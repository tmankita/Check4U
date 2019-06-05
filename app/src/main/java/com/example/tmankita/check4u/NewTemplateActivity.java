package com.example.tmankita.check4u;

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
import android.os.Environment;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SizeF;
import android.util.TypedValue;
import android.view.Display;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.tmankita.check4u.Database.Template;
import com.example.tmankita.check4u.Dropbox.UserDropBoxActivity;
import com.github.ybq.android.spinkit.sprite.Sprite;
import com.github.ybq.android.spinkit.style.FadingCircle;
import com.otaliastudios.zoom.ZoomEngine;
import com.otaliastudios.zoom.ZoomLayout;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;



public class NewTemplateActivity extends AppCompatActivity {
    //Data Structures
        private HashMap<String,Mark> marks;
        private HashMap<String, PointF> marksLocation;
        private HashMap<String, SizeF> marksSize;
        private HashMap<String,Mark> marksTogether;
        private HashMap<String,View> marksViews;
        private String[][] questionTable;

    //Views
        private ImageView image;
        private RelativeLayout relativeLayout;
        private RelativeLayout screenLayout;
        private ZoomLayout zoomLayout; //comment
        private ZoomEngine engine; //comment
        private Button copy;
        private Button exitCopyMode;
        private TableLayout layoutSetQuestionID;
        private TableLayout layoutGetInfo;
        private EditText questionIDEdit;
        private EditText answerIDEdit;
        private EditText questionsNumberEdit;
        private EditText answersNumberEdit;



    //Template Matrix
        private Mat paper;
        private Matrix M;
        private float realA4Width = 4960;//(float)796.8;
        private float realA4Height = 7016;//(float)1123.2;

    // Create a string for the View label
        private static final String VIEW_WRONG_TAG = "WrongAnswerMarker";
        private static final String VIEW_RIGHT_TAG = "RightAnswerMarker";
//        private static final String VIEW_QUESTION_TAG = "QuestionMarker";
        private static final String VIEW_BARCODE_TAG = "BarcodeMarker";

    //Counters
        private int counterWrong = 1;
        private int numberOfOptions = 4;
        private int numberOfQuestions = 10;
        private int counterQuestion = 1;

    //Markers
        private ImageView wrongMark;
        private ImageView rightMark;
        private ImageView barcodeMark;

    //Helpers
        private RelativeLayout markToUpdate;
        private String questionToInsertToTheTable;
        private int lastUserUpdateQuestion = 1 ;
        private int lastUserUpdateAnswer = 0 ;
        private Bitmap bitmap;
        private String imagePath;
        private boolean keyboardOff;
        private float widthDisplay;
        private float heightDisplay;



    //Copy Mode
        private String idHelper;
        private  boolean copyModeFlag = false;
        private View viewHelper;
        private Template db;

    public class Mark {
        public RelativeLayout _mark;
        public Button _plusButton;
        public Button _minusButton;
        public Button _closeButton;
        public TextView _viewQ;

        public Mark (RelativeLayout mark, Button plusButton, Button minusButton, Button closeButton, TextView viewQ){
            _mark = mark;
            _plusButton = plusButton;
            _minusButton = minusButton;
            _closeButton = closeButton;
            _viewQ = viewQ;

        }

    }

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

    final Handler handler = new Handler();
    Runnable mLongPressed = new Runnable() {
        public void run() {
            String[] parts = idHelper.split("_");
            if (!parts[0].equals("BarcodeMarker")) {
                if (!copyModeFlag) {
                    copyModeFlag = true;
                        Mark mark_to_highlight = marks.get(idHelper);
                        RelativeLayout mark_to_highlight_layout = mark_to_highlight._mark;
                        mark_to_highlight_layout.setBackgroundColor(Color.parseColor("#515DA7F1"));
                        if(!marksTogether.containsKey(idHelper)) {
                            marksTogether.put(idHelper, mark_to_highlight);
                            marksViews.put(idHelper,viewHelper);
                        }
                    copy.setVisibility(View.VISIBLE);
                    exitCopyMode.setVisibility(View.VISIBLE);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_template);

        if (!OpenCVLoader.initDebug()) {
            Log.d("LOAD OPENCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d("LOAD OPENCV", "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        keyboardOff         = false;
        marksLocation       = new HashMap<>();
        marksSize           = new HashMap<>();
        marks               = new HashMap<>();
        marksTogether       = new HashMap<>();
        marksViews          = new HashMap<>();
        relativeLayout      = (RelativeLayout) findViewById(R.id.Layout);
        wrongMark           = (ImageView) findViewById(R.id.wrongAns);
        rightMark           = (ImageView) findViewById(R.id.rightAns);
        barcodeMark         = (ImageView) findViewById(R.id.barcode_mark);
        zoomLayout          = findViewById(R.id.zoom_layout); //comment
        screenLayout        =  findViewById(R.id.Layout1);
        copy                = findViewById(R.id.copy);
        exitCopyMode        = findViewById(R.id.exitCopyMode);
        layoutSetQuestionID = findViewById(R.id.LayoutSetID);
        layoutGetInfo       = findViewById(R.id.numberOfQuestionsAndAnswers);
        questionIDEdit      = findViewById(R.id.questionID);
        answerIDEdit        = findViewById(R.id.answerID);
        questionsNumberEdit  = findViewById(R.id.questionsNumber);
        answersNumberEdit    = findViewById(R.id.answersNumber);
        engine              = zoomLayout.getEngine(); //comment


        layoutGetInfo.setVisibility(View.VISIBLE);
        layoutGetInfo.bringToFront();

        ViewGroup.LayoutParams params1 =  relativeLayout.getLayoutParams();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        params1.height = displayMetrics.heightPixels;
        params1.width = displayMetrics.widthPixels;

//        relativeLayout.setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener() {
//            @Override
//            public void onChildViewAdded(View parent, View child) {
//                    String tag = (String) child.getTag();
//                    marksViews.put(tag,child);
//
//            }
//
//            @Override
//            public void onChildViewRemoved(View parent, View child) {
//
//            }
//        });

        zoomLayout.setVisibility(View.VISIBLE); //comment

        Bundle extras = getIntent().getExtras();
        imagePath = extras.getString("sheet");

        image = (ImageView) findViewById(R.id.NewPicture);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        bitmap = BitmapFactory.decodeFile(imagePath, options);
        // to sum the black level in matrix
        Bitmap bmp = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(bmp, paper);



        // set paper on display
//        widthDisplay = displayMetrics.widthPixels;
//        heightDisplay = displayMetrics.heightPixels;




        M = new Matrix();
        image.setImageBitmap(bitmap);
        image.post(new Runnable() {
            @Override
            public void run() {
                M = image.getImageMatrix();


            }
        });
        image.invalidate();


        wrongMark.setOnTouchListener(new View.OnTouchListener() {

            //onTouch code
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                viewHelper = v;
                final RelativeLayout markLayout;
                String newTag = VIEW_WRONG_TAG +"_" + counterQuestion + counterWrong;
                counterWrong++;
                markLayout = createMark("WrongAnswerMarker", newTag,wrongMark.getHeight(),wrongMark.getWidth());
                markLayout.setVisibility(View.VISIBLE);
                markToUpdate = markLayout;
                relativeLayout.addView(markLayout,1);
                relativeLayout.invalidate();
                updateLocation();
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                    // basic mode
                        markLayout.post(new Runnable() {
                            @Override
                            public void run() {
                                if (markLayout != null) {
                                    ClipData data = ClipData.newPlainText("", "");
                                    View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(markLayout);
                                    markLayout.startDrag(data, shadowBuilder, markLayout, View.DRAG_FLAG_OPAQUE);
                                    markLayout.setVisibility(View.INVISIBLE);

                                }
                            }
                        });


                }

                return false;
            }
        });

        rightMark.setOnTouchListener(new View.OnTouchListener() {

                        //onTouch code
                        @Override
                        public boolean onTouch(View v, MotionEvent event) {
                            viewHelper = v;
                            final RelativeLayout markLayout;
                            String newTag = VIEW_RIGHT_TAG + "_" + counterQuestion + counterWrong;
                            counterWrong++;
                            markLayout = createMark("RightAnswerMarker", newTag,wrongMark.getHeight(),wrongMark.getWidth());
                            markToUpdate = markLayout;
                            relativeLayout.addView(markLayout,1);
                            markLayout.setVisibility(View.VISIBLE);
                            relativeLayout.invalidate();
                            updateLocation();
                            switch (event.getAction()) {
                                case MotionEvent.ACTION_DOWN:
                                    // basic mode
                                    markLayout.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (markLayout != null) {
                                                ClipData data = ClipData.newPlainText("", "");
                                                View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(markLayout);
                                                markLayout.startDrag(data, shadowBuilder, markLayout, View.DRAG_FLAG_OPAQUE);
                                                markLayout.setVisibility(View.INVISIBLE);

                                            }
                                        }
                                    });

                            }
                            return false;
                         }
                    });

        barcodeMark.setOnTouchListener(new View.OnTouchListener() {
            //onTouch code
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                idHelper = (String) v.getTag();
                viewHelper = v;
                final RelativeLayout markLayout;
                String newTag = VIEW_BARCODE_TAG;
                markLayout = createMark("barcode", newTag,wrongMark.getHeight(),wrongMark.getWidth());
                markToUpdate = markLayout;
                relativeLayout.addView(markLayout,1);
                markLayout.setVisibility(View.VISIBLE);
                relativeLayout.invalidate();
                updateLocation();
                barcodeMark.setVisibility(View.INVISIBLE);
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        markLayout.post(new Runnable() {
                            @Override
                            public void run() {
                                if (markLayout != null) {
                                    ClipData data = ClipData.newPlainText("", "");
                                    View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(markLayout);
                                    markLayout.startDrag(data, shadowBuilder, markLayout, View.DRAG_FLAG_OPAQUE);
                                    markLayout.setVisibility(View.INVISIBLE);

                                }
                            }
                        });

                }
                return false;
            }
        });


        screenLayout.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                  Rect r = new Rect();
                        screenLayout.getWindowVisibleDisplayFrame(r);
                        int screenHeight = screenLayout.getRootView().getHeight();

                        // r.bottom is the position above soft keypad or device button.
                        // if keypad is shown, the r.bottom is smaller than that before.
                        int keypadHeight = screenHeight - r.bottom;

                        Log.d("KEYBOARD", "keypadHeight = " + keypadHeight);

                        if (keypadHeight > screenHeight * 0.15) { // 0.15 ratio is perhaps enough to determine keypad height.
                            // keyboard is opened
                            if(keyboardOff){
                                keyboardOff= false;
                            InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
                            imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
                            }
                        }
                        else {
                            // keyboard is closed
                        }
                    }
                });

        // Set the drag surface
        image.setOnDragListener(new View.OnDragListener() {
            private String draggedImageTag;
            private  float x_cord ,y_cord;
            private float zoomScale, pan_y,pan_x; //comment
            @Override
            public boolean onDrag(View v, DragEvent event) {

                switch(event.getAction()) {
                    case DragEvent.ACTION_DRAG_STARTED:
                        View view =(View) event.getLocalState();
                        draggedImageTag = (String) view.getTag();
                        pan_x = zoomLayout.getPanX(); //comment
                        pan_y = zoomLayout.getPanY(); //comment
                        zoomScale = zoomLayout.getRealZoom(); //comment
                        engine.zoomTo(0.8f,false); //comment
                        v.invalidate();
                        Log.i("Mark", "Action is DragEvent.ACTION_DRAG_STARTED ");
                        break;

                    case DragEvent.ACTION_DRAG_ENTERED:
                        v.invalidate();
                        Log.i("Mark", "Action is DragEvent.ACTION_DRAG_ENTERED");
                        break;

                    case DragEvent.ACTION_DRAG_EXITED:
                        v.invalidate();
                        Log.i("Mark", "Action is DragEvent.ACTION_DRAG_EXITED");
                        break;

                    case DragEvent.ACTION_DRAG_LOCATION:
                        Log.i("Mark", "TAG: "+ draggedImageTag+" Action is DragEvent.ACTION_DRAG_LOCATION");

                        x_cord =  event.getX();
                        y_cord =  event.getY();
                        Log.i("Mark", "TAG: "+ draggedImageTag+" X: "+x_cord+" Y: "+y_cord);

                        SizeF size_mark_prev;

                            size_mark_prev = marksSize.get(draggedImageTag);
                            PointF mark_prev_location = marksLocation.get(draggedImageTag);
                            if(copyModeFlag){
                                float delta_x = (x_cord - (size_mark_prev.getWidth()/2))  - mark_prev_location.x;
                                float delta_y = (y_cord - (size_mark_prev.getHeight()/2)) - mark_prev_location.y ;
                                View s = (View) event.getLocalState();
                                updateDropAction(draggedImageTag,(y_cord - (size_mark_prev.getHeight()/2)),(x_cord - (size_mark_prev.getWidth()/2)), s);
                                for (View mark_selected: marksViews.values() ) {
                                    String MarkTag = (String) mark_selected.getTag(); //(String)mark_selected._mark.getTag();
                                    if(!MarkTag.equals(draggedImageTag)){
                                        PointF curr_mark_prev_location = marksLocation.get(MarkTag);
                                        float new_x_cord = (curr_mark_prev_location.x + delta_x);
                                        float new_y_cord = (curr_mark_prev_location.y + delta_y);
                                        updateDropAction(MarkTag, new_y_cord, new_x_cord,mark_selected);
                                    }
                                }
                                engine.realZoomTo(zoomScale,false); //comment
                                engine.panTo(pan_x,pan_y,false); //comment


                            }
                            else{
                                View s = (View) event.getLocalState();
                                updateDropAction(draggedImageTag,(y_cord - (size_mark_prev.getHeight()/2)),(x_cord - (size_mark_prev.getWidth()/2)),s);
                                engine.realZoomTo(zoomScale,false); //comment
                                engine.panTo(pan_x,pan_y,false); //comment
                            }

                        break;

                    case DragEvent.ACTION_DRAG_ENDED:
                        v.invalidate();
                        handler.removeCallbacks(mLongPressed);
                        break;

                    case DragEvent.ACTION_DROP:
                        v.invalidate();
                        handler.removeCallbacks(mLongPressed);
                        Log.i("Answer_Mark", "ACTION_DROP event");
                        break;

                    default: break;
                }
                return true;
            }
        });

    }

    public void  updateDropAction(String ImageTag, float y_cord, float x_cord, View v){
        Mark mark = marks.get(ImageTag);
        RelativeLayout mark_select = mark._mark;
        View view = v;
        view.setVisibility(View.VISIBLE);
        view.setX(x_cord);
        view.setY(y_cord);
        markToUpdate = mark_select;
        v.invalidate();
        String tag = (String) markToUpdate.getTag();
        PointF p = new PointF(x_cord,y_cord);
        if(marksLocation.containsKey(tag)) {
            marksLocation.remove(tag);
        }
        marksLocation.put( tag ,p);

    }

    public void copy (View v ){
        float pan_x = zoomLayout.getPanX(); //comment
        float pan_y = zoomLayout.getPanY(); //comment
        float zoomScale = zoomLayout.getRealZoom(); //comment
        engine.zoomTo(0.8f,false); //comment
//        ArrayList<Mark> newMarksTogther = new ArrayList<>();


        for ( Mark mark: marksTogether.values() ) {
            String tag = (String) mark._mark.getTag();
            String[] parts =  tag.split("_");
            PointF location = marksLocation.get(tag);
            int X = (int) relativeLayout.getWidth();
            RelativeLayout newMark = null;
            String newTag;
            switch(parts[0]){
                case "WrongAnswerMarker":
                    newTag = parts[0]+"_"+counterQuestion+counterWrong;
                    counterWrong++;
                    newMark = createMark("WrongAnswerMarker",newTag,(int)marksSize.get(tag).getHeight(),(int)marksSize.get(tag).getWidth());
                    break;
                case "RightAnswerMarker":
                    newTag = parts[0]+"_"+counterQuestion+counterWrong;
                    counterWrong++;
                    newMark = createMark("RightAnswerMarker",newTag,(int)marksSize.get(tag).getHeight(),(int)marksSize.get(tag).getWidth());
                    break;
            }
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) newMark.getLayoutParams();
//              Set the circle location in the a beside the source mark
            params.removeRule(RelativeLayout.CENTER_HORIZONTAL);
            params.removeRule(RelativeLayout.CENTER_VERTICAL);
            params.topMargin = (int)(location.y + 50) ;
            params.rightMargin = (int)(X - location.x);
            newMark.setVisibility(View.VISIBLE);

            markToUpdate = newMark;
            relativeLayout.addView(newMark,2);
            updateLocation();
            RelativeLayout mark_to_UnHighlight_layout =  mark._mark ;
            mark_to_UnHighlight_layout.setBackgroundColor(Color.parseColor("#00FFFFFF"));
        }
        copyModeFlag = false;
        copy.setVisibility(View.INVISIBLE);
        marksTogether.clear();
        marksViews.clear();
        exitCopyMode.setVisibility(View.INVISIBLE);


        engine.realZoomTo(zoomScale,false); //comment
        engine.panTo(pan_x,pan_y,false); //comment
    }

    public void exitCopyMode (View v) {
        for ( Mark mark: marksTogether.values() ) {
            RelativeLayout mark_to_UnHighlight_layout =  mark._mark ;
            mark_to_UnHighlight_layout.setBackgroundColor(Color.parseColor("#00FFFFFF"));
        }
        copyModeFlag = false;
        copy.setVisibility(View.INVISIBLE);
        marksTogether.clear();
        marksViews.clear();
        exitCopyMode.setVisibility(View.INVISIBLE);

    }

    public void createDataBase( View v ){
//        Bitmap bmpBarcode23 = bitmap.createBitmap(paper.cols(), paper.rows(), Bitmap.Config.ARGB_8888);

        if(!marksLocation.containsKey("BarcodeMarker") || !marksSize.containsKey("BarcodeMarker")){
            Log.i("createDataBase", "Most mark barcode!!! ");
            Toast.makeText(getApplicationContext(),"Most mark barcode!!!",Toast.LENGTH_LONG).show();
        }
        else {
            for (String[] q:questionTable) {
                for (int i = 0; i < q.length ; i++) {
                    if(q[i]==null){
                        Toast.makeText(this,"Need to associate  all the  marks to the relevant answers ",Toast.LENGTH_LONG).show();
                        return;
                    }
                }

            }

            String filePath = Template.DB_FILEPATH;
            File file = new File(filePath);
            if (file.exists())
                file.delete();
            db = new Template(this);
            // calculate inverse matrix
            final Matrix inverse = new Matrix();

            M.invert(inverse);

//            Imgproc.circle(paper, new Point(rP[0],rP[1]), 10, new Scalar(0, 0, 255, 150), 4);
//            Bitmap bmpBarcode23 = bitmap.createBitmap(paper.cols(), paper.rows(), Bitmap.Config.ARGB_8888);
//            Utils.matToBitmap(paper, bmpBarcode23);
            float[] pForSize1 = new float[2];
            float[] p1 = new float[2];
            PointF[] ps1 = new PointF[4];
            float[] point = new float[2];
            PointF[] scalePoint = new PointF[4];
            for (int i = 0; i < numberOfQuestions; ++i) {
                for (int j = 0; j < numberOfOptions; ++j) {
                    String tag = questionTable[i][j];
                    PointF location;
                    SizeF size;
                    int sumOfBlack=0;
                    int id;
                    String[] parts = (tag).split("_");

                    location = marksLocation.get(tag);
                    size = marksSize.get(tag);
                    pForSize1[0]= (location.x+size.getWidth());
                    pForSize1[1] = (location.y+size.getHeight());
                    p1[0] =  location.x;
                    p1[1] =  location.y;

                    inverse.mapPoints(p1);
                    inverse.mapPoints(pForSize1);

                    ps1[0] = new PointF(p1[0],p1[1]);
                    ps1[1] = new PointF(pForSize1[0] ,p1[1]);
                    ps1[2] = new PointF(p1[0],pForSize1[1]);
                    ps1[3] = new PointF(pForSize1[0],pForSize1[1]);
                    PointF[] sorted_2 = sortPoints_newTemplate(ps1);

//                    FOR DEBUG:
//                    Point[] ps2 = new Point[4];
//                    ps2[0] = new Point(p1[0],p1[1]);
//                    ps2[1] = new Point(pForSize1[0] ,p1[1]);
//                    ps2[2] = new Point(p1[0],pForSize1[1]);
//                    ps2[3] = new Point(pForSize1[0],pForSize1[1]);
//
//
//
//                Imgproc.line(paper,ps2[0],ps2[1],new Scalar(0, 255, 0, 150), 4);
//                Imgproc.line(paper,ps2[1],ps2[2],new Scalar(0, 255, 0, 150), 4);
//                Imgproc.line(paper,ps2[2],ps2[3],new Scalar(0, 255, 0, 150), 4);
//                Imgproc.line(paper,ps2[3],ps2[0],new Scalar(0, 255, 0, 150), 4);
//                Utils.matToBitmap(paper, bmpBarcode23);

                    Matrix scaleToRealSize = new Matrix();
                    RectF drawableRect = new RectF(0, 0, paper.cols(), paper.rows());
                    RectF viewRect = new RectF(0, 0, realA4Width, realA4Height);
                    scaleToRealSize.setRectToRect(drawableRect, viewRect, Matrix.ScaleToFit.FILL);

                    for (int k = 0; k < 4; k++) {
                        point[0] =(float)sorted_2[k].x; point[1] = (float)sorted_2[k].y;
                        scaleToRealSize.mapPoints(point);
                        scalePoint[k] = new PointF(point[0],point[1]);
                    }
                    SizeF newSize = new SizeF(Math.abs(scalePoint[0].x-scalePoint[1].x),Math.abs(scalePoint[0].y-scalePoint[3].y));


                    PointF transfer_point = new PointF(scalePoint[0].x, scalePoint[0].y);
                    id = (i + 1) * 10 + (j + 1);
                    switch (parts[0]) {
                        case VIEW_WRONG_TAG:
                            db.insertData(id, transfer_point.x,  transfer_point.y,  newSize.getHeight(),  newSize.getWidth(), sumOfBlack, 0,0,0);
                            break;
                        case VIEW_RIGHT_TAG:
                            db.insertData(id,  transfer_point.x,  transfer_point.y,  newSize.getHeight(),  newSize.getWidth(), sumOfBlack, 1,0,0);
                            break;
                    }
                }
            }
            if (marks.containsKey(VIEW_BARCODE_TAG)) {
                PointF location;
                SizeF size;
                location = marksLocation.get(VIEW_BARCODE_TAG);
                size = marksSize.get(VIEW_BARCODE_TAG);
                pForSize1[0]=  (location.x+size.getWidth());
                pForSize1[1] = (location.y+size.getHeight());
                p1[0] = location.x;
                p1[1] = location.y;

                inverse.mapPoints(p1);
                inverse.mapPoints(pForSize1);

                ps1[0] = new PointF(p1[0],p1[1]);
                ps1[1] = new PointF(pForSize1[0] ,p1[1]);
                ps1[2] = new PointF(p1[0],pForSize1[1]);
                ps1[3] = new PointF(pForSize1[0],pForSize1[1]);

                PointF[] sorted_2 = sortPoints_newTemplate(ps1);

//                Point[] ps2 = new Point[4];
//                ps2[0] = new Point(p1[0],p1[1]);
//                ps2[1] = new Point(pForSize1[0] ,p1[1]);
//                ps2[2] = new Point(p1[0],pForSize1[1]);
//                ps2[3] = new Point(pForSize1[0],pForSize1[1]);
//
//                Imgproc.line(paper,ps2[0],ps2[1],new Scalar(0, 255, 0, 150), 4);
//                Imgproc.line(paper,ps2[1],ps2[2],new Scalar(0, 255, 0, 150), 4);
//                Imgproc.line(paper,ps2[2],ps2[3],new Scalar(0, 255, 0, 150), 4);
//                Imgproc.line(paper,ps2[3],ps2[0],new Scalar(0, 255, 0, 150), 4);
//                 Utils.matToBitmap(paper, bmpBarcode23);


                Matrix scaleToRealSize = new Matrix();
//                scaleToRealSize.postScale((realA4Width/paper.cols()),(realA4Height/paper.rows()));
                RectF drawableRect = new RectF(0, 0, paper.cols(), paper.rows());
                RectF viewRect = new RectF(0, 0, realA4Width, realA4Height);
                scaleToRealSize.setRectToRect(drawableRect, viewRect, Matrix.ScaleToFit.FILL);
                int i=0;
                for (PointF p: sorted_2) {
                    point[0] =p.x; point[1] = p.y;
                    scaleToRealSize.mapPoints(point);
                    scalePoint[i] = new PointF(point[0],point[1]);
                    i++;
                }
                SizeF newSize = new SizeF(Math.abs(scalePoint[0].x-scalePoint[1].x),Math.abs(scalePoint[0].y-scalePoint[3].y));


                PointF transfer_barcode = new PointF(scalePoint[0].x, scalePoint[0].y);
                db.insertData(0,  transfer_barcode.x,  transfer_barcode.y,  newSize.getHeight(),  newSize.getWidth(), 0, 0,numberOfQuestions,numberOfOptions);
            }


            Intent uploadTemplate = new Intent(getApplicationContext(), UserDropBoxActivity.class);
            Bundle bundle = new Bundle();
            bundle.putString("caller", "newTemplate");
            bundle.putString("templatePath", imagePath);
            bundle.putString("TemplateDataBase", db.getFilePath());
            uploadTemplate.putExtras(bundle);
            startActivity(uploadTemplate);
        }
//        for (Mark mark : marks.values()) {
//            String tag = (String)mark._mark.getTag();
//            Point location;
//            Size size;
//            int sumOfBlack;
//            String[] parts = (tag).split("_");
//            switch(parts[0]) {
//                case VIEW_WRONG_TAG:
//                   location = marksLocation.get(tag);
//                   size = marksSize.get(tag);
//                   sumOfBlack = calculateBlackLevel(paper,location,size);
//                   db.insertData(Integer.parseInt(parts[1]),(int)location.x,(int)location.y,(int)size.height,(int)size.width,sumOfBlack,0);
//                   break;
//                case VIEW_RIGHT_TAG:
//                    location = marksLocation.get(tag);
//                    size = marksSize.get(tag);
//                    sumOfBlack = calculateBlackLevel(paper,location,size);
//                    db.insertData(Integer.parseInt(parts[1]),(int)location.x,(int)location.y,(int)size.height,(int)size.width,sumOfBlack,1);
//                    break;
//            }
//        }
    }

    public void setIdQuestionOrAnswer ( View view ){
        keyboardOff = true;
        lastUserUpdateQuestion = Integer.parseInt(questionIDEdit.getText().toString());
        lastUserUpdateAnswer = Integer.parseInt(answerIDEdit.getText().toString());
        questionTable[lastUserUpdateQuestion-1][lastUserUpdateAnswer-1] = questionToInsertToTheTable;
        RelativeLayout mark_to_UnHighlight_layout =  marks.get(questionToInsertToTheTable)._mark ;
        String tag = (String) mark_to_UnHighlight_layout.getTag();
        mark_to_UnHighlight_layout.setBackgroundColor(Color.parseColor("#00FFFFFF"));
        layoutSetQuestionID.setVisibility(View.INVISIBLE);
        marks.get(tag)._viewQ.setText(lastUserUpdateAnswer+", "+lastUserUpdateQuestion);


    }

    public void setNumbersQuestionAndAnswer ( View view ){
        numberOfQuestions = (Integer.parseInt(questionsNumberEdit.getText().toString()));
        numberOfOptions = (Integer.parseInt(answersNumberEdit.getText().toString()));
        questionTable       = new String[numberOfQuestions][numberOfOptions];
        layoutGetInfo.setVisibility(View.INVISIBLE);

        InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);

    }

    private RelativeLayout createMark( String tag, String id, int height, int width ) {
        // Create mark layout
        final RelativeLayout.LayoutParams markParam;
        RelativeLayout markLayout;
        if(tag.equals("barcode")){
            markLayout= new RelativeLayout(this);
            markLayout.setTag(id);
            markParam = new RelativeLayout.LayoutParams(  // set the layout params for mark
                    barcodeMark.getWidth(),
                    barcodeMark.getHeight()
                    );
            marksSize.put(id,new SizeF(barcodeMark.getWidth(),barcodeMark.getHeight()));
        }
        else {
            markLayout= new RelativeLayout(this);
            markLayout.setTag(id);
            //wrongMark.getHeight()
            //wrongMark.getWidth()
            markParam = new RelativeLayout.LayoutParams(  // set the layout params for mark
                    height,
                    width);

            marksSize.put(id,new SizeF(width,height));
//            visibleView = new Rect();
        }


//        screenLayout.getChildVisibleRect(relativeLayout,visibleView,null);

        markParam.addRule(RelativeLayout.CENTER_HORIZONTAL);
        markParam.addRule(RelativeLayout.CENTER_VERTICAL);
        markLayout.setLayoutParams(markParam); // set defined layout params to mark layout


        markLayout.setOnTouchListener(new View.OnTouchListener() {
            private long lastClickTime = 0;
            private static final long DOUBLE_CLICK_TIME_DELTA = 300;//milliseconds
            private long clickTime;
            //onTouch code
            @Override
            public boolean onTouch(View v, MotionEvent event) {


                idHelper = (String) v.getTag();
                viewHelper = v;

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        Mark mark = marks.get(idHelper);
                        clickTime = System.currentTimeMillis();
                        //double tapping mode
                        if (!(((String)idHelper).equals(VIEW_BARCODE_TAG)) && clickTime - lastClickTime < DOUBLE_CLICK_TIME_DELTA){
                            Log.i("Mark", "Double tap!! ");
                            layoutSetQuestionID.setVisibility(View.VISIBLE);
                            layoutSetQuestionID.bringToFront();
                            questionToInsertToTheTable = idHelper;
                             calculateNextQuestionId();
                             calculateNextAnswerId();
                            questionIDEdit.setText(Integer.toString(lastUserUpdateQuestion));
                            answerIDEdit.setText(Integer.toString(lastUserUpdateAnswer));
                            Mark mark_to_highlight = mark;
                            RelativeLayout mark_to_highlight_layout = mark_to_highlight._mark;
                            // Set highlight to the drag mark
                            mark_to_highlight_layout.setBackgroundColor(Color.parseColor("#515DA7F1"));
                        }
                        // copy mode
                        else if(copyModeFlag) {
                            Mark mark_to_highlight = mark;
                            RelativeLayout mark_to_highlight_layout = mark_to_highlight._mark;
                            // Set highlight to the drag mark
                            mark_to_highlight_layout.setBackgroundColor(Color.parseColor("#515DA7F1"));
                            if (!marksTogether.containsKey(idHelper)){
                                marksTogether.put(idHelper, mark_to_highlight);
                                marksViews.put(idHelper,v);
                            }

                            copy.setVisibility(View.VISIBLE);
                            RelativeLayout mark_to_build_shadow_layout = mark._mark;

                            ClipData data = ClipData.newPlainText("", "");
                            View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(mark_to_build_shadow_layout);
                            mark_to_build_shadow_layout.startDrag(data, shadowBuilder, mark_to_build_shadow_layout, View.DRAG_FLAG_OPAQUE);
                            mark_to_build_shadow_layout.setVisibility(View.INVISIBLE);
                            return true;
                        }
                        // basic mode
                        else {
                            lastClickTime = clickTime;
                            RelativeLayout layout = mark._mark;
                            handler.postDelayed(mLongPressed, 2000);
                            if (layout != null) {
                                ClipData data = ClipData.newPlainText("", "");
                                View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(layout);
                                layout.startDrag(data, shadowBuilder, layout, View.DRAG_FLAG_OPAQUE);
                                layout.setVisibility(View.INVISIBLE);
                                return true;
                            }
                        }

                        return false;
                }
                return true;
            }
        });

        // set the layout params for ImageView
        RelativeLayout.LayoutParams imageViewParam = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.FILL_PARENT,
                RelativeLayout.LayoutParams.FILL_PARENT);

        // create a new ImageView
        ImageView imageView = new ImageView(NewTemplateActivity.this);
        imageView.setTag(id);
        imageViewParam.addRule(RelativeLayout.CENTER_HORIZONTAL); // align ImageView in the center
        imageView.setLayoutParams(imageViewParam); // set defined layout params to ImageView



        // Create close button
        // set the layout params for Button
        RelativeLayout.LayoutParams closeButtonParam = new RelativeLayout.LayoutParams(
                (int)(height*0.4),
                (int)(width*0.4));
        Button closeButton = new Button(this);  // create a new Button
        closeButton.setTag(id);  // set Button's id
        closeButtonParam.addRule(RelativeLayout.ALIGN_PARENT_TOP); // set Button to the UPPER of ImageView
        closeButtonParam.addRule(RelativeLayout.ALIGN_PARENT_RIGHT); // set Button to the LEFT of ImageView
        closeButton.setLayoutParams(closeButtonParam); // set defined layout params to Button
        closeButton.setBackgroundResource(android.R.drawable.btn_dialog);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String id = (String) view.getTag();
                String[] parts = (id).split("_");
                RelativeLayout layout = marks.get(id)._mark;
                if(layout != null){
                    if(copyModeFlag){
                        marksTogether.remove(id);
                        marksViews.remove(id);
                    }
                        marks.remove(id);
                        marksLocation.remove(id);
                        relativeLayout.removeView(layout);
                        if(parts[0].equals(VIEW_BARCODE_TAG))
                            barcodeMark.setVisibility(View.VISIBLE);

                    for (int i = 0; i < questionTable.length ; i++) {
                        for (int j = 0; j < questionTable[i].length ; j++) {
                            if(questionTable[i][j] != null && questionTable[i][j].equals(id)){
                                questionTable[i][j]=null;
                            }
                        }
                    }
                }
                return;
            }

        });

        // Create resize buttons
        // plus button
        RelativeLayout.LayoutParams plusButtonParam = new RelativeLayout.LayoutParams(
                (int)(height*0.5),
                (int)(width*0.5));
        Button plusButton = new Button(this);  // create a new Button
        plusButton.setTag(id);  // set Button's id
        plusButtonParam.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 1); // set Button to the Bottom of ImageView
        plusButtonParam.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 1); // set Button to the LEFT of ImageView
        plusButton.setLayoutParams(plusButtonParam); // set defined layout params to Button
        plusButton.setBackgroundResource(android.R.drawable.btn_plus);
        // minus button
        RelativeLayout.LayoutParams minusButtonParam = new RelativeLayout.LayoutParams(
                (int)(height*0.5),
                (int)(width*0.5));
        Button minusButton = new Button(this);  // create a new Button
        minusButton.setTag(id);  // set Button's id
        minusButtonParam.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 1); // set Button to the Bottom of ImageView
        minusButtonParam.addRule(RelativeLayout.ALIGN_PARENT_LEFT, 1); // set Button to the LEFT of ImageView
        minusButton.setLayoutParams(minusButtonParam); // set defined layout params to Button
        minusButton.setBackgroundResource(android.R.drawable.btn_minus);

        plusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                float pan_x = zoomLayout.getPanX(); //comment
                float pan_y = zoomLayout.getPanY(); //comment
                float zoomScale = zoomLayout.getRealZoom(); //comment
                engine.zoomTo(0.8f,false); //comment

                double supremum = 140*4;
                String buttonTag = (String) view.getTag();
                Mark markSelected= marks.get(buttonTag);
                RelativeLayout markSelectedLayout = markSelected._mark;
                Button plusButton = markSelected._plusButton;
                Button minusButton = markSelected._minusButton;
                Button closeButton = markSelected._closeButton;
                final TextView viewQ = markSelected._viewQ;
                // Get his layout params
                RelativeLayout.LayoutParams lParams = (RelativeLayout.LayoutParams) markSelectedLayout.getLayoutParams();
                RelativeLayout.LayoutParams pParams = (RelativeLayout.LayoutParams) plusButton.getLayoutParams();
                RelativeLayout.LayoutParams mParams = (RelativeLayout.LayoutParams) minusButton.getLayoutParams();
                RelativeLayout.LayoutParams cParams = (RelativeLayout.LayoutParams) closeButton.getLayoutParams();
                RelativeLayout.LayoutParams vParams = (RelativeLayout.LayoutParams) viewQ.getLayoutParams();
                // get current mark size
                SizeF size = marksSize.get(buttonTag);
                float new_height = size.getHeight() + 15;
                float new_width = size.getWidth() + 15;
                // resize layout
                if(supremum>new_height){
                    lParams.height = (int) new_height;
                    lParams.width = (int) new_width;
                    mParams.height = (int) (0.5*new_height);
                    mParams.width = (int) (0.5*new_width);
                    pParams.height = (int) (0.5*new_height);
                    pParams.width = (int) (0.5*new_width);
                    cParams.height = (int) (0.3*new_height);
                    cParams.width = (int) (0.3*new_width);
                    vParams.height = (int) (1*new_height);
                    vParams.width = (int) (1*new_width);
                    // set the changes
                    plusButton.setLayoutParams(pParams);
                    minusButton.setLayoutParams(mParams);
                    closeButton.setLayoutParams(cParams);
                    viewQ.setLayoutParams(vParams);
                    viewQ.bringToFront();
                    markSelectedLayout.setLayoutParams(lParams);
                    markSelectedLayout.setVisibility(View.VISIBLE);
                    marks.remove(buttonTag);
                    marksSize.remove(buttonTag);
                    marks.put(buttonTag,new Mark(markSelectedLayout,plusButton,minusButton,closeButton,viewQ));
                    marksSize.put(buttonTag,new SizeF (new_width,new_height));
                    markToUpdate =  markSelectedLayout;
                    viewQ.setTextSize(TypedValue.COMPLEX_UNIT_PX,(int)(0.5*new_width));

                    updateLocation();

                }
                engine.realZoomTo(zoomScale,false); //comment
                engine.panTo(pan_x,pan_y,false); //comment
            }
        });
        minusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                float pan_x = zoomLayout.getPanX(); //comment
                float pan_y = zoomLayout.getPanY(); //comment
                float zoomScale = zoomLayout.getRealZoom(); //comment
                engine.zoomTo(0.8f,false); //comment
                double infimum = 140/8;

                String buttonTag = (String) view.getTag();
                Mark markSelected= marks.get(buttonTag);
                RelativeLayout markSelectedLayout = markSelected._mark;
                Button plusButton = markSelected._plusButton;
                Button minusButton = markSelected._minusButton;
                Button closeButton = markSelected._closeButton;
                final TextView viewQ = markSelected._viewQ;

                // Get his layout params
                RelativeLayout.LayoutParams lParams = (RelativeLayout.LayoutParams) markSelectedLayout.getLayoutParams();
                RelativeLayout.LayoutParams pParams = (RelativeLayout.LayoutParams) plusButton.getLayoutParams();
                RelativeLayout.LayoutParams mParams = (RelativeLayout.LayoutParams) minusButton.getLayoutParams();
                RelativeLayout.LayoutParams cParams = (RelativeLayout.LayoutParams) closeButton.getLayoutParams();
                RelativeLayout.LayoutParams vParams = (RelativeLayout.LayoutParams) viewQ.getLayoutParams();

                // get current mark size
                SizeF size = marksSize.get(buttonTag);
                float new_height = size.getHeight() - 15;
                float new_width = size.getWidth() - 15;
                // resize layout
                if(new_height >infimum){
                    lParams.height = (int) new_height;
                    lParams.width = (int) new_width;
                    mParams.height = (int) (0.5*new_height);
                    mParams.width = (int) (0.5*new_width);
                    pParams.height = (int) (0.5*new_height);
                    pParams.width = (int) (0.5*new_width);
                    cParams.height = (int) (0.3*new_height);
                    cParams.width = (int) (0.3*new_width);
                    vParams.height = (int) (1*new_height);
                    vParams.width = (int) (1*new_width);
                // set the changes
                    plusButton.setLayoutParams(pParams);
                    minusButton.setLayoutParams(mParams);
                    closeButton.setLayoutParams(cParams);
                    viewQ.setLayoutParams(vParams);
                    viewQ.bringToFront();
                    markSelectedLayout.setLayoutParams(lParams);
                    markSelectedLayout.setVisibility(View.VISIBLE);
                    marks.remove(buttonTag);
                    marksSize.remove(buttonTag);
                    marks.put(buttonTag,new Mark(markSelectedLayout,plusButton,minusButton,closeButton,viewQ));
                    marksSize.put(buttonTag,new SizeF (new_width,new_height));
                    viewQ.setTextSize(TypedValue.COMPLEX_UNIT_PX,(int)(0.5*new_width));

                    markToUpdate =  markSelectedLayout;
                    updateLocation();

                }
                engine.realZoomTo(zoomScale,false); //comment
                engine.panTo(pan_x,pan_y,false); //comment
            }
        });

        // Create view for number question and answer if defined
        RelativeLayout.LayoutParams viewQParam = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.FILL_PARENT,
                RelativeLayout.LayoutParams.FILL_PARENT);
        final TextView viewQ = new TextView(this);
        viewQ.setTag(id);
        viewQParam.addRule(RelativeLayout.ALIGN_PARENT_TOP); //
        viewQParam.addRule(RelativeLayout.ALIGN_PARENT_RIGHT); //
        viewQ.setLayoutParams(viewQParam); //
        viewQ.setTextSize(TypedValue.COMPLEX_UNIT_PX,(int)(0.5*height));
        viewQ.bringToFront();

        switch (tag){

            case "WrongAnswerMarker":
                // Set id for mark
                markLayout.setId(R.id.answer_mark);
                // set resource in ImageView
                imageView.setImageResource(R.drawable.square_wrong_answer);
                // add ImageView in RelativeLayout
                markLayout.addView(imageView);
                // add close Button in RelativeLayout
                markLayout.addView(closeButton);
                // add resize Button in RelativeLayout
                markLayout.addView(plusButton);
                markLayout.addView(minusButton);
                markLayout.addView(viewQ);

                break;

            case "barcode":
                // Set id for mark
                markLayout.setId(R.id.barcode_mark);
                // set resource in ImageView
                imageView.setImageResource(R.drawable.square_id_digits);
                // add ImageView in RelativeLayout
                markLayout.addView(imageView);
                // add close Button in RelativeLayout
                markLayout.addView(closeButton);
                // add resize Button in RelativeLayout
                markLayout.addView(plusButton);
                markLayout.addView(minusButton);
                break;


//            case "QuestionMarker":
//                // Set id for mark
//                markLayout.setId(R.id.question_mark);
//                // set resource in ImageView
//                imageView.setImageResource(R.drawable.square_question);
//                // add ImageView in RelativeLayout
//                markLayout.addView(imageView);
//                // add close Button in RelativeLayout
//                markLayout.addView(closeButton);
//                // add resize Button in RelativeLayout
//                markLayout.addView(plusButton);
//                markLayout.addView(minusButton);
//                break;

            case "RightAnswerMarker":
                // Set id for mark
                markLayout.setId(R.id.right_answer_mark);
                // set resource in ImageView
                imageView.setImageResource(R.drawable.square_right_answer);
                // add ImageView in RelativeLayout
                markLayout.addView(imageView);
                // add close Button in RelativeLayout
                markLayout.addView(closeButton);
                // add resize Button in RelativeLayout
                markLayout.addView(plusButton);
                markLayout.addView(minusButton);

                markLayout.addView(viewQ);

                break;
            default: break;
        }
    marks.put(id,new Mark(markLayout, plusButton, minusButton, closeButton, viewQ));
    return markLayout;
    }


    private void updateLocation () {
                String tag = (String) markToUpdate.getTag();
                float [] location = new float[2];
                location[0]=markToUpdate.getX();
                location[1]=markToUpdate.getY();
        PointF p = new PointF(location[0],location[1]);
                if(marksLocation.containsKey(tag)) {
                    marksLocation.remove(tag);
                }
                marksLocation.put( tag ,p);
    }

//    private int calculateBlackLevel( Mat img, Point location, Size size ){
//        double blackLevel=0.0;
////        double[] currentPixel;
//        for (int raw = (int)location.y ; raw < (location.y + size.height) ; ++raw){
//            for (int col = (int)location.x ; col < (location.x + size.width) ; ++col){
//                blackLevel = blackLevel+ img.get(raw,col)[0];
//            }
//        }
//        return (int)blackLevel;
//    }

    private void calculateNextQuestionId () {
        if(lastUserUpdateAnswer >= numberOfOptions){
             lastUserUpdateQuestion++;
        }
    }

    private int calculateNextAnswerId () {
        if(lastUserUpdateAnswer >= numberOfOptions)
            return lastUserUpdateAnswer = 1;
        else
            return lastUserUpdateAnswer++;
    }

//    private Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
//        int width = bm.getWidth();
//        int height = bm.getHeight();
//        float scaleWidth = ((float) newWidth) / width;
//        float scaleHeight = ((float) newHeight) / height;
//        // CREATE A MATRIX FOR THE MANIPULATION
//        Matrix matrix = new Matrix();
//        // RESIZE THE BIT MAP
//        matrix.postScale(scaleWidth, scaleHeight);
//        // "RECREATE" THE NEW BITMAP
//        Bitmap resizedBitmap = Bitmap.createBitmap(
//                bm, 0, 0, width, height, matrix, false);
//        bm.recycle();
//        return resizedBitmap;
//    }

    public static PointF[] sortPoints_newTemplate( PointF[] src ) {

        ArrayList<PointF> srcPoints = new ArrayList<>(Arrays.asList(src));

        PointF[] result = { null , null , null , null };

        Comparator<PointF> sumComparator = new Comparator<PointF>() {
            @Override
            public int compare(PointF lhs, PointF rhs) {
                return Float.valueOf(lhs.y + lhs.x).compareTo(rhs.y + rhs.x);
            }
        };

        Comparator<PointF> diffComparator = new Comparator<PointF>() {

            @Override
            public int compare(PointF lhs, PointF rhs) {
                return Float.valueOf(lhs.y - lhs.x).compareTo(rhs.y - rhs.x);
            }
        };

        // top-left corner = minimal sum
        result[0] = Collections.min(srcPoints, sumComparator);

        // bottom-right corner = maximal sum
        result[2] = Collections.max(srcPoints, sumComparator);

        // top-right corner = minimal difference
        result[1] = Collections.min(srcPoints, diffComparator);

        // bottom-left corner = maximal difference
        result[3] = Collections.max(srcPoints, diffComparator);

        return result;
    }


}
