package com.example.tmankita.check4u;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TableLayout;

import com.example.tmankita.check4u.Database.Answer;
import com.otaliastudios.zoom.ZoomEngine;
import com.otaliastudios.zoom.ZoomLayout;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.HashMap;

public class ProblematicQuestionsActivity extends AppCompatActivity {
    private Mat paper;
    private ArrayList<Answer> problematicAnswers;
    private Matrix M;
    private ImageView image;
    private ConstraintLayout layout;
    private ZoomLayout zoomLayout;
    private EditText questionIDEdit;
    private EditText answerIDEdit;
    private TableLayout layoutSetQuestionID;
    private ZoomEngine engine;
//    private ImageView markToUpdate;
//    private HashMap<String,Point> marksLocation;
    private HashMap<String,ImageView> marksImageViews;
    private int[] toFix;
    private int numberOfQuestions;
    private String questionToInsertToFix;





    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    paper = new Mat();

                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_problematic_questions);

        if (!OpenCVLoader.initDebug()) {
            Log.d("LOAD OPENCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d("LOAD OPENCV", "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }

        zoomLayout          = (ZoomLayout) findViewById(R.id.zoom_layout1);
        image               = (ImageView) findViewById(R.id.NewPicture);
        layout              = (ConstraintLayout) findViewById(R.id.Layout_image);
        questionIDEdit      = (EditText) findViewById(R.id.questionID);
        answerIDEdit        = (EditText) findViewById(R.id.answerID);
        layoutSetQuestionID = (TableLayout) findViewById(R.id.LayoutSetID);
        engine              = (ZoomEngine) zoomLayout.getEngine();
        marksImageViews     =  new HashMap<String,ImageView>();


        ViewGroup.LayoutParams params1 =  layout.getLayoutParams();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        params1.height = displayMetrics.heightPixels;
        params1.width = displayMetrics.widthPixels;

        Bundle extras = getIntent().getExtras();
        String imagePath = (String) extras.getString("sheet");
        problematicAnswers = (ArrayList<Answer>) extras.getSerializable("problematicAnswers");
        numberOfQuestions = extras.getInt("numberOfQuestions");
        toFix = new int[numberOfQuestions+1];
        for (int i = 0; i < numberOfQuestions; i++) {
            toFix[i] = 0;
        }


        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap = BitmapFactory.decodeFile(imagePath, options);
        // to sum the black level in matrix
        Bitmap bmp = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(bmp, paper);
        // set paper on display
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        final int width = size.x;
        final int height = size.y;
        engine.zoomTo(1,false);
        image.setImageBitmap(bitmap);
        M = image.getImageMatrix();
        RectF drawableRect = new RectF(0, 0, paper.cols(), paper.rows());
        RectF viewRect = new RectF(0, 0, width, height);
        M.setRectToRect(drawableRect, viewRect, Matrix.ScaleToFit.CENTER);
        image.setImageMatrix(M);
        image.invalidate();
        zoomLayout.setVisibility(View.VISIBLE);

        generateMarks();

    }

    private void generateMarks (){



        for (Answer answer: problematicAnswers) {
            String newTag = answer.getQuestionNumber() + "_" +answer.getAnswerNumber();
            ImageView mark = createMark(newTag,answer);
            layout.addView(mark,1);
//            updateLocation();
        }

    }
    //for debug
//    private void updateLocation () {
//        String tag = (String) markToUpdate.getTag();
//        int [] location = new int[2];
//        location[0]=(int)markToUpdate.getX();
//        location[1]=(int)markToUpdate.getY();
//        if(marksLocation.containsKey(tag)) {
//            marksLocation.remove(tag);
//        }
//        marksLocation.put( tag ,new Point(location[0],location[1]));
//    }
    //----------
    public void correctAnswer ( View view ){
        int UserUpdateQuestion = Integer.parseInt(questionIDEdit.getText().toString());
        int UserUpdateAnswer = Integer.parseInt(answerIDEdit.getText().toString());
        toFix[UserUpdateQuestion-1] = UserUpdateAnswer;
        ImageView mark_to_Highlight_Green =  marksImageViews.get(questionToInsertToFix);//marks.get(questionToInsertToTheTable)._mark ;
        mark_to_Highlight_Green.setBackgroundColor(Color.parseColor("#00FF00"));
        layoutSetQuestionID.setVisibility(View.INVISIBLE);
        InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);

    }
    public void finish (View view){
        Intent returnIntent = new Intent();
        returnIntent.putExtra("toFix",toFix);
        setResult(Activity.RESULT_OK,returnIntent);
        finish();

    }

    private ImageView createMark( String id,Answer answer ) {
        Matrix scaleToImageSize = new Matrix();
        RectF viewRect = new RectF(0, 0, paper.cols(), paper.rows());
        RectF drawableRect = new RectF(0, 0, 4960, 7016);
        boolean success = scaleToImageSize.setRectToRect(drawableRect, viewRect, Matrix.ScaleToFit.FILL);
        float[] p_to_imageViewSize = new float[]{answer.getLocationX(),answer.getLocationY()};
        float[][] points = new float[][]{
                {answer.getLocationX(),answer.getLocationY()},
                {(answer.getLocationX()+answer.getWidth()),(answer.getLocationY())},
                {(answer.getLocationX()),(answer.getLocationY()+answer.getHeight())},
                {(answer.getLocationX()+answer.getWidth()),(answer.getLocationY()+answer.getHeight())}
        };

        PointF[] ps_imageView = new PointF[4];


        for (int i = 0; i < 4; i++) {
            scaleToImageSize.mapPoints(points[i]);
        }
        scaleToImageSize.mapPoints(p_to_imageViewSize);

        for (int i = 0; i < 4; i++) {
            M.mapPoints(points[i]);
            ps_imageView[i] = new PointF(points[i][0],points[i][1]);
        }
        M.mapPoints(p_to_imageViewSize);

        float height =  Math.abs(ps_imageView[3].y - ps_imageView[1].y);
        float width = Math.abs(ps_imageView[1].x - ps_imageView[0].x);
        // create a new ImageView
        ImageView markImageView = new ImageView(ProblematicQuestionsActivity.this);
        markImageView.setMinimumHeight((int)height);
        markImageView.setMinimumWidth((int)width);
        markImageView.setMaxHeight((int)height);
        markImageView.setMaxWidth((int)width);
        markImageView.setTag(id);
        // set resource in ImageView
        markImageView.setImageResource(R.drawable.square_question);
        markImageView.setX(p_to_imageViewSize[0]);
        markImageView.setY(p_to_imageViewSize[1]);
        markImageView.setBackgroundColor(Color.parseColor("#FF0000"));

        markImageView.setOnTouchListener(new View.OnTouchListener() {
            private long lastClickTime = 0;
            private static final long DOUBLE_CLICK_TIME_DELTA = 300;//milliseconds
            private long clickTime;
            //onTouch code
            @Override
            public boolean onTouch(View v, MotionEvent event) {


                questionToInsertToFix = (String) v.getTag();

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        ImageView mark = marksImageViews.get(questionToInsertToFix);
                        clickTime = System.currentTimeMillis();
                        //double tapping mode
                        if (clickTime - lastClickTime < DOUBLE_CLICK_TIME_DELTA){
                            Log.i("Mark", "Double tap!! ");
                            layoutSetQuestionID.setVisibility(View.VISIBLE);
                            layoutSetQuestionID.bringToFront();
                            String[] parts =  questionToInsertToFix.split("_");
                            questionIDEdit.setText(parts[0]);
                            answerIDEdit.setText(parts[1]);
                            ImageView mark_to_highlight = mark;
                            // Set highlight to the drag mark
                            mark_to_highlight.setBackgroundColor(Color.parseColor("#515DA7F1"));
                        }else{
                            lastClickTime = clickTime;
                        }

                }
                return true;
            }
        });

        marksImageViews.put(id,markImageView);

        return markImageView;
    }

}
