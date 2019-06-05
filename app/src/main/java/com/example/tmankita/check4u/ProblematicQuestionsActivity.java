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
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.text.BoringLayout;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TextView;

import com.example.tmankita.check4u.Database.Answer;
import com.otaliastudios.zoom.ZoomEngine;
import com.otaliastudios.zoom.ZoomLayout;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.HashMap;

public class ProblematicQuestionsActivity extends AppCompatActivity {
    private Mat paper;
    private ArrayList<Answer> problematicAnswers;
    private Matrix M;
    private ImageView image;
    private RelativeLayout layout;
    private ZoomLayout zoomLayout;
    private TextView questionID;
    private TextView answerID;
    private TableLayout layoutSetQuestionID;
    private TableLayout dialog;
    private ZoomEngine engine;
    private Button finish;
//    private ImageView markToUpdate;
//    private HashMap<String,Point> marksLocation;
    private HashMap<String,RelativeLayout> marksImageViews;
    private int[] toFix;
    private int numberOfQuestions;
    private String questionToInsertToFix;
    Boolean finished;
    int screenWidth;

    //helpers
    private int UserUpdateAnswer;
    private int UserUpdateQuestion;
    private Mat imageForTest;






    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    paper = new Mat();
                    imageForTest = new Mat();

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
        image               = (ImageView) findViewById(R.id.NewPicture_p);
        layout              = (RelativeLayout) findViewById(R.id.Layout_p);
        questionID      = (TextView) findViewById(R.id.questionID);
        answerID        = (TextView) findViewById(R.id.answerID);
        layoutSetQuestionID = (TableLayout) findViewById(R.id.LayoutSetID);
        dialog              = (TableLayout) findViewById(R.id.table_dialog);
        engine              = (ZoomEngine) zoomLayout.getEngine();
        finish              = (Button) findViewById(R.id.create);
        marksImageViews     =  new HashMap<>();


        ViewGroup.LayoutParams params1 =  layout.getLayoutParams();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        params1.height = displayMetrics.heightPixels;
        params1.width = displayMetrics.widthPixels;
        screenWidth = displayMetrics.widthPixels;;

        Bundle extras = getIntent().getExtras();
        String imagePath = (String) extras.getString("sheet");
        problematicAnswers = (ArrayList<Answer>) extras.getSerializable("problematicAnswers");
        numberOfQuestions = extras.getInt("numberOfQuestions");
        toFix = new int[numberOfQuestions+1];
        for (int i = 1; i < numberOfQuestions+1; i++) {
            toFix[i] = 0;
        }


        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap = BitmapFactory.decodeFile(imagePath, options);
        // to sum the black level in matrix
        Bitmap bmp = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(bmp, paper);
        paper.copyTo(imageForTest);

        // set paper on display
//        Display display = getWindowManager().getDefaultDisplay();
//        Point size = new Point();
//        display.getSize(size);
//        final int width = size.x;
//        final int height = size.y;
//        engine.zoomTo(1,false);
//        image.setImageBitmap(bitmap);
//        M = image.getImageMatrix();
//        RectF drawableRect = new RectF(0, 0, paper.cols(), paper.rows());
//        RectF viewRect = new RectF(0, 0, width, height);
//        M.setRectToRect(drawableRect, viewRect, Matrix.ScaleToFit.CENTER);
//        image.setImageMatrix(M);
//        image.invalidate();
        zoomLayout.setVisibility(View.VISIBLE);
//        finished=false;
        M = new Matrix();
        image.setImageBitmap(bitmap);

        image.post(new Runnable() {
            @Override
            public void run() {
                M = image.getImageMatrix();
//                finished = true;

            }
        });

        image.invalidate();

        image.setVisibility(View.INVISIBLE);
        finish.setVisibility(View.INVISIBLE);
        dialog.setVisibility(View.VISIBLE);
        dialog.bringToFront();

//        generateMarks();

    }

    private void generateMarks (){



        for (Answer answer: problematicAnswers) {
            String newTag = answer.getQuestionNumber() + "_" +answer.getAnswerNumber();
            RelativeLayout mark = createMark(newTag,answer);
            layout.addView(mark,1);
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
    public void mistakeClick (View view){
        layoutSetQuestionID.setVisibility(View.VISIBLE);
        RelativeLayout mark_to_Highlight_Green =  marksImageViews.get(questionToInsertToFix);//marks.get(questionToInsertToTheTable)._mark ;
        mark_to_Highlight_Green.setBackgroundColor(Color.parseColor("#FF0000"));
    }
    public void ok_dialog (View view){
        dialog.setVisibility(View.INVISIBLE);
        finish.setVisibility(View.VISIBLE);
        image.setVisibility(View.VISIBLE);
        generateMarks();
    }
    public void correctAnswer ( View view ){
//        int UserUpdateQuestion = Integer.parseInt(questionIDEdit.getText().toString());
//        int UserUpdateAnswer = Integer.parseInt(answerIDEdit.getText().toString());
        toFix[UserUpdateQuestion] = UserUpdateAnswer;
        RelativeLayout mark_to_Highlight_Green =  marksImageViews.get(questionToInsertToFix);//marks.get(questionToInsertToTheTable)._mark ;
        mark_to_Highlight_Green.setBackgroundColor(Color.parseColor("#00FF00"));
        layoutSetQuestionID.setVisibility(View.INVISIBLE);
//        InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
//        imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);

    }
    public void finish (View view){
        Intent returnIntent = new Intent();
        returnIntent.putExtra("toFix",toFix);
        setResult(Activity.RESULT_OK,returnIntent);
        finish();

    }

    private RelativeLayout createMark( String id,Answer answer ) {
        Matrix scaleToImageSize = new Matrix();
        RectF drawableRect = new RectF(0, 0, 4960, 7016);
        RectF viewRect = new RectF(0, 0, paper.cols(), paper.rows());
        boolean success = scaleToImageSize.setRectToRect(drawableRect, viewRect, Matrix.ScaleToFit.FILL);
        float[] p_to_imageViewSize = new float[]{answer.getLocationX(),(answer.getLocationY())};

        float[][] points = new float[][]{
                {answer.getLocationX(),(answer.getLocationY())},
                {(answer.getLocationX()+answer.getWidth()),(answer.getLocationY())},
                {answer.getLocationX(),(answer.getLocationY()+answer.getHeight())},
                {(answer.getLocationX()+answer.getWidth()),(answer.getLocationY()+answer.getHeight())}
        };

        PointF[] ps_imageView = new PointF[4];
        org.opencv.core.Point[] ps_forTest = new org.opencv.core.Point[4];

        for (int i = 0; i < 4; i++) {
            scaleToImageSize.mapPoints(points[i]);
            ps_forTest[i] =new org.opencv.core.Point((double)points[i][0],(double)points[i][1]);
        }

        scaleToImageSize.mapPoints(p_to_imageViewSize);


//        Imgproc.line(imageForTest,ps_forTest[0],ps_forTest[1],new Scalar(0, 0, 255, 150), 4);
//        Imgproc.line(imageForTest,ps_forTest[1],ps_forTest[2],new Scalar(0, 0, 255, 150), 4);
//        Imgproc.line(imageForTest,ps_forTest[2],ps_forTest[3],new Scalar(0, 0, 255, 150), 4);
//        Imgproc.line(imageForTest,ps_forTest[3],ps_forTest[0],new Scalar(0, 0, 255, 150), 4);
//
//
//        Bitmap bmpForTest = Bitmap.createBitmap(imageForTest.cols(), imageForTest.rows(), Bitmap.Config.ARGB_8888);
//        Utils.matToBitmap(imageForTest, bmpForTest);


        for (int i = 0; i < 4; i++) {
            M.mapPoints(points[i]);
            ps_imageView[i] = new PointF(points[i][0],points[i][1]);
        }
        M.mapPoints(p_to_imageViewSize);

        float height =  Math.abs(ps_imageView[3].y - ps_imageView[1].y);
        float width = Math.abs(ps_imageView[1].x - ps_imageView[0].x);

        // new relativeLayout
        final RelativeLayout markLayout= new RelativeLayout(this);
        markLayout.setTag(id);
        RelativeLayout.LayoutParams markParam = new RelativeLayout.LayoutParams(  // set the layout params for mark
                (int)height,
                (int)width);

        markParam.addRule(RelativeLayout.CENTER_VERTICAL);
        markParam.addRule(RelativeLayout.CENTER_HORIZONTAL);
        markLayout.setLayoutParams(markParam);
        markLayout.setBackgroundColor(Color.parseColor("#FF0000"));


        // set the layout params for ImageView
        RelativeLayout.LayoutParams imageViewParam = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.FILL_PARENT,
                RelativeLayout.LayoutParams.FILL_PARENT);

        // create a new ImageView
        ImageView imageView = new ImageView(this);
        imageView.setTag(id);
        imageViewParam.addRule(RelativeLayout.CENTER_HORIZONTAL); // align ImageView in the center
        imageView.setLayoutParams(imageViewParam); // set defined layout params to ImageView
        imageView.setImageResource(R.drawable.square_question);
        // add ImageView in RelativeLayout
        markLayout.addView(imageView);

        markLayout.setOnTouchListener(new View.OnTouchListener() {
            private long lastClickTime = 0;
            private static final long DOUBLE_CLICK_TIME_DELTA = 300;//milliseconds
            private long clickTime;
            //onTouch code
            @Override
            public boolean onTouch(View v, MotionEvent event) {


                questionToInsertToFix = (String) v.getTag();

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        RelativeLayout mark = marksImageViews.get(questionToInsertToFix);
                        clickTime = System.currentTimeMillis();
                        //double tapping mode
                        if (clickTime - lastClickTime < DOUBLE_CLICK_TIME_DELTA){
                            Log.i("Mark", "Double tap!! ");
                            layoutSetQuestionID.setVisibility(View.VISIBLE);
                            layoutSetQuestionID.bringToFront();
                            String[] parts =  questionToInsertToFix.split("_");
                            UserUpdateAnswer = Integer.parseInt(parts[1]);
                            UserUpdateQuestion = Integer.parseInt(parts[0]);
                            questionID.setText(parts[0]);
                            answerID.setText(parts[1]);
                            RelativeLayout mark_to_highlight = mark;
                            // Set highlight to the drag mark
                            mark_to_highlight.setBackgroundColor(Color.parseColor("#515DA7F1"));
                        }else{
                            lastClickTime = clickTime;
                        }

                }
                return true;
            }
        });

        final float[] p = new float[]{p_to_imageViewSize[0],p_to_imageViewSize[1]};
        markLayout.post(new Runnable() {
            @Override
            public void run() {
                markLayout.setX(p[0]);
                markLayout.setY(p[1]);
            }
        });


//
//        // create a new ImageView
//        ImageView markImageView = new ImageView(ProblematicQuestionsActivity.this);
//        markImageView.setMinimumHeight((int)height);
//        markImageView.setMinimumWidth((int)width);
//        markImageView.setMaxHeight((int)height);
//        markImageView.setMaxWidth((int)width);
//        markImageView.setTag(id);
//        // set resource in ImageView
//        markImageView.setImageResource(R.drawable.square_question);
//        markImageView.setX(p_to_imageViewSize[0]);
//        markImageView.setY(p_to_imageViewSize[1]);
//        markImageView.setBackgroundColor(Color.parseColor("#FF0000"));
//        markImageView.setVisibility(View.VISIBLE);
//        markImageView.bringToFront();
//
//        markImageView.setOnTouchListener(new View.OnTouchListener() {
//            private long lastClickTime = 0;
//            private static final long DOUBLE_CLICK_TIME_DELTA = 300;//milliseconds
//            private long clickTime;
//            //onTouch code
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//
//
//                questionToInsertToFix = (String) v.getTag();
//
//                switch (event.getAction()) {
//                    case MotionEvent.ACTION_DOWN:
//                        ImageView mark = marksImageViews.get(questionToInsertToFix);
//                        clickTime = System.currentTimeMillis();
//                        //double tapping mode
//                        if (clickTime - lastClickTime < DOUBLE_CLICK_TIME_DELTA){
//                            Log.i("Mark", "Double tap!! ");
//                            layoutSetQuestionID.setVisibility(View.VISIBLE);
//                            layoutSetQuestionID.bringToFront();
//                            String[] parts =  questionToInsertToFix.split("_");
//                            UserUpdateAnswer = Integer.parseInt(parts[1]);
//                            UserUpdateQuestion = Integer.parseInt(parts[0]);
//                            questionID.setText(parts[0]);
//                            answerID.setText(parts[1]);
//                            ImageView mark_to_highlight = mark;
//                            // Set highlight to the drag mark
//                            mark_to_highlight.setBackgroundColor(Color.parseColor("#515DA7F1"));
//                        }else{
//                            lastClickTime = clickTime;
//                        }
//
//                }
//                return true;
//            }
//        });

        marksImageViews.put(id,markLayout);

        return markLayout;
    }

}
