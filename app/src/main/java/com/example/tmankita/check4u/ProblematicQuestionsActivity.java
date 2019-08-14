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
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.BoringLayout;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

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

import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.HashMap;

public class ProblematicQuestionsActivity extends AppCompatActivity {
    //Image
    private Mat paper;
    private Matrix M;

    //Views
    private ImageView image;
    private RelativeLayout layout;
    private ZoomLayout zoomLayout;
    private TextView questionID;
    private EditText answerID;
    private TableLayout layoutSetQuestionID;
    private TableLayout dialog;
    private RelativeLayout choose_dialog;
    private ZoomEngine engine;
    private Button finish;
    private RelativeLayout layoutMain;

    //Data Structures
    private ArrayList<Answer> problematicAnswers;
    private HashMap<String,RelativeLayout> marksImageViews;
    private HashMap<String,RelativeLayout> options;
    private int[] toFix;


    //helpers
    private int numberOfAnswers;
    private int numberOfQuestions;
    private String questionToInsertToFix;
    int screenWidth;
    private int UserUpdateAnswer;
    private int UserUpdateQuestion;
    private Mat imageForTest;
    private String callee;
    private int id;


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
        layoutMain          = (RelativeLayout) findViewById(R.id.Layout1);
        questionID          = (TextView) findViewById(R.id.questionID);
        answerID            = (EditText) findViewById(R.id.answerID);
        layoutSetQuestionID = (TableLayout) findViewById(R.id.LayoutSetID);
        dialog              = (TableLayout) findViewById(R.id.table_dialog);
        engine              = (ZoomEngine) zoomLayout.getEngine();
        finish              = (Button) findViewById(R.id.create);
        marksImageViews     =  new HashMap<>();


        Bundle extras = getIntent().getExtras();

        callee = extras.getString("callee");
        id = extras.getInt("id");
        numberOfAnswers = extras.getInt("numberOfAnswers");



        ViewGroup.LayoutParams params1 =  layout.getLayoutParams();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        params1.height = displayMetrics.heightPixels;
        params1.width = displayMetrics.widthPixels;
        screenWidth = displayMetrics.widthPixels;;

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
        zoomLayout.setVisibility(View.VISIBLE);
        M = new Matrix();
        image.setImageBitmap(bitmap);

        image.post(new Runnable() {
            @Override
            public void run() {
                M = image.getImageMatrix();
            }
        });

        image.invalidate();

        image.setVisibility(View.INVISIBLE);
        finish.setVisibility(View.INVISIBLE);
        dialog.setVisibility(View.VISIBLE);
        dialog.bringToFront();
    }

    private void createChooseDialog(){
        choose_dialog = new RelativeLayout(this);

        choose_dialog.setTag("choose_dialog");
        RelativeLayout.LayoutParams dialogParam = new RelativeLayout.LayoutParams(  // set the layout params for mark
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        dialogParam.addRule(RelativeLayout.ALIGN_END, R.id.marks_row);
        choose_dialog.setLayoutParams(dialogParam);
        choose_dialog.setBackgroundColor(Color.parseColor("#8B8B8F"));

        TableLayout table = new TableLayout(this);
        TableLayout.LayoutParams tableParam = new TableLayout.LayoutParams(  // set the layout params for mark
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        table.setLayoutParams(tableParam);
        table.setShrinkAllColumns(true);


        TableRow titleRow = new TableRow(this);
        TableRow.LayoutParams titleViewParam = new TableRow.LayoutParams(  // set the layout params for mark
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        titleRow.setLayoutParams(titleViewParam);


        for (int i = -1; i <=numberOfAnswers ; i++) {
            if(i==numberOfAnswers){
                TextView title = new TextView(this);
                title.setText("Choose the right answer:");
                title.setTypeface(Typeface.DEFAULT_BOLD);
                titleRow.addView(title);

            }else{
                TextView title = new TextView(this);
                title.setText(" ");
                titleRow.addView(title);
            }
        }
        table.addView(titleRow);

        TableRow optionsToChooseRow = new TableRow(this);
        ViewGroup.LayoutParams optionsParam = new TableRow.LayoutParams(  // set the layout params for mark
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        optionsToChooseRow.setLayoutParams(optionsParam);



        options = new HashMap<>();

        for (int i = numberOfAnswers; i >=-1 ; i--) {
            String choose_id ="choose_"+(i+1);
            Button option_i = new Button(this);
            option_i.setTag(choose_id);

            if(i>-1 && i<numberOfAnswers){
                option_i.setText(String.valueOf(i+1));
                option_i.setTextSize(50);
                option_i.setBackgroundResource(R.drawable.square_question);
                option_i.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String tag =(String)v.getTag();
                        UserUpdateAnswer = Integer.parseInt(tag.split("_")[1]);
                        toFix[UserUpdateQuestion] = UserUpdateAnswer;
                        RelativeLayout mark_to_Highlight_Green = marksImageViews.get(questionToInsertToFix);//marks.get(questionToInsertToTheTable)._mark ;
                        mark_to_Highlight_Green.setBackgroundColor(Color.parseColor("#00FF00"));
                        choose_dialog.setVisibility(View.INVISIBLE);
                    }
                });
            }else{
                option_i.setBackgroundColor(Color.parseColor("#8B8B8F"));
            }

            optionsToChooseRow.addView(option_i);
        }


        table.addView(optionsToChooseRow);
        choose_dialog.addView(table);
        layoutMain.addView(choose_dialog);
        choose_dialog.setVisibility(View.INVISIBLE);
    }
    /**
     *
     */
    private void generateMarks (){
        for (Answer answer: problematicAnswers) {
            String newTag = answer.getQuestionNumber() + "_" +answer.getAnswerNumber();
            RelativeLayout mark = createMark(newTag,answer);
            layout.addView(mark,1);
        }

    }
    /**
     *
     */
    public void mistakeClick (View view){
        layoutSetQuestionID.setVisibility(View.VISIBLE);
        RelativeLayout mark_to_Highlight_Green =  marksImageViews.get(questionToInsertToFix);//marks.get(questionToInsertToTheTable)._mark ;
        mark_to_Highlight_Green.setBackgroundColor(Color.parseColor("#FF0000"));
        layoutSetQuestionID.setVisibility(View.INVISIBLE);
    }
    /**
     *
     */
    public void ok_dialog (View view){
        dialog.setVisibility(View.INVISIBLE);
        finish.setVisibility(View.VISIBLE);
        image.setVisibility(View.VISIBLE);
        generateMarks();
        createChooseDialog();
    }
    /**
     *
     */
    public void correctAnswer ( View view ) {
        int curAnswerId = Integer.parseInt(answerID.getText().toString());
        if (curAnswerId > 0 && curAnswerId <= numberOfAnswers){
            UserUpdateAnswer = curAnswerId;
            toFix[UserUpdateQuestion] = UserUpdateAnswer;
            RelativeLayout mark_to_Highlight_Green = marksImageViews.get(questionToInsertToFix);//marks.get(questionToInsertToTheTable)._mark ;
            mark_to_Highlight_Green.setBackgroundColor(Color.parseColor("#00FF00"));
            layoutSetQuestionID.setVisibility(View.INVISIBLE);
        }else {
            Toast.makeText(this,"Insert answer number between range 1 - "+numberOfAnswers, Toast.LENGTH_SHORT).show();
        }


    }
    /**
     *
     */
    public void finish (View view){
        Intent returnIntent = new Intent();
        returnIntent.putExtra("toFix",toFix);
        returnIntent.putExtra("callee",callee);
        returnIntent.putExtra("id",id);
        setResult(Activity.RESULT_OK,returnIntent);
        finish();

    }
    /**
     *
     */
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
//                            layoutSetQuestionID.setVisibility(View.VISIBLE);
//                            layoutSetQuestionID.bringToFront();
                            choose_dialog.setVisibility(View.VISIBLE);
                            choose_dialog.bringToFront();
                            String[] parts =  questionToInsertToFix.split("_");
                            UserUpdateAnswer = Integer.parseInt(parts[1]);
                            UserUpdateQuestion = Integer.parseInt(parts[0]);
//                            questionID.setText(parts[0]);
//                            answerID.setText(parts[1]);
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


        marksImageViews.put(id,markLayout);

        return markLayout;
    }


}
