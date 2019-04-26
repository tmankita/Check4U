package com.example.tmankita.check4u;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import android.widget.RelativeLayout;
import android.widget.TableLayout;

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
import org.opencv.core.Size;

import java.io.File;
import java.util.HashMap;



public class NewTemplateActivity extends AppCompatActivity {
    //Data Structures
        private HashMap<String,Mark> marks;
        private HashMap<String,Point> marksLocation;
        private HashMap<String,Size> marksSize;
        private HashMap<String,Mark> marksTogether;
        private HashMap<String,View> marksViews;
        private String[][] questionTable;

    //Views
        private ImageView image;
        private RelativeLayout relativeLayout;
        private RelativeLayout screenLayout;
        private ZoomLayout zoomLayout;
        private ZoomEngine engine;
        private Button copy;
        private Button exitCopyMode;
        private TableLayout layoutSetQuestionID;
        private TableLayout layoutGetInfo;
        private EditText questionIDEdit;
        private EditText answerIDEdit;
        private EditText questionNumberEdit;
        private EditText answerNumberEdit;
        private ImageView loading;
        private Sprite fadingCircle;


    //Template Matrix
        private Mat paper;
        private Matrix M;

    // Create a string for the View label
        private static final String VIEW_WRONG_TAG = "WrongAnswerMarker";
        private static final String VIEW_RIGHT_TAG = "RightAnswerMarker";
//        private static final String VIEW_QUESTION_TAG = "QuestionMarker";
        private static final String VIEW_BARCODE_TAG = "BarcodeMarker";

    //Counters
        private int counterWrong = 1;
        private int numberOfOptions = 4;
        private int numberOfQuestion = 10;
        private int counterQuestion = 1;

    //Markers
        private ImageView wrongMark;
        private ImageView barcodeMark;

    //Helpers
        private RelativeLayout markToUpdate;
        private int threshold;
        private String questionToInsertToTheTable;
        private int lastUserUpdateQuestion = 1 ;
        private int lastUserUpdateAnswer = 0 ;

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

        public Mark (RelativeLayout mark, Button plusButton, Button minusButton, Button closeButton){
            _mark = mark;
            _plusButton = plusButton;
            _minusButton = minusButton;
            _closeButton = closeButton;
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
            if (!copyModeFlag) {
                copyModeFlag = true;
                String[] parts = idHelper.split("_");
                if (!parts[0].equals("BarcodeMarker")) {
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

        marksLocation       = new HashMap<>();
        marksSize           = new HashMap<>();
        marks               = new HashMap<>();
        marksTogether       = new HashMap<>();
        marksViews          = new HashMap<>();
        relativeLayout      = findViewById(R.id.Layout);
        wrongMark           = findViewById(R.id.wrongAns);
        barcodeMark         = findViewById(R.id.barcode_mark);
        threshold           = 140;
        zoomLayout          = findViewById(R.id.zoom_layout);
        screenLayout        = findViewById(R.id.Layout1);
        copy                = findViewById(R.id.copy);
        exitCopyMode        = findViewById(R.id.exitCopyMode);
        layoutSetQuestionID = findViewById(R.id.LayoutSetID);
        layoutGetInfo       = findViewById(R.id.numberOfQuestionsAndAnswers);
        questionIDEdit      = findViewById(R.id.questionID);
        answerIDEdit        = findViewById(R.id.answerID);
        questionNumberEdit  = findViewById(R.id.questionNumber);
        answerNumberEdit    = findViewById(R.id.answerNumber);
        engine              = zoomLayout.getEngine();
        loading = findViewById(R.id.Loading);
        fadingCircle = new FadingCircle();
        fadingCircle.setColor(Color.WHITE);
        loading.setImageDrawable(fadingCircle);
        loading.setVisibility(View.INVISIBLE);

        layoutGetInfo.setVisibility(View.VISIBLE);
        layoutGetInfo.bringToFront();
        ViewGroup.LayoutParams params1 =  relativeLayout.getLayoutParams();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        params1.height = displayMetrics.heightPixels;
        params1.width = displayMetrics.widthPixels;
        zoomLayout.setVisibility(View.VISIBLE);


        Bundle extras = getIntent().getExtras();
        String imagePath = extras.getString("sheet");

//      /storage/emulated/0/Pictures/Check4U/IMG_20190226_220922.jpg
//        String imagePath = "/storage/emulated/0/Pictures/Check4U/IMG_20190226_220922.jpg";

        image = (ImageView) findViewById(R.id.NewPicture);
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
        image.setImageBitmap(bitmap);
        M = image.getImageMatrix();
        RectF drawableRect = new RectF(0, 0, width, height);
        RectF viewRect = new RectF(0, 0, image.getWidth(), image.getHeight());
        M.setRectToRect(drawableRect, viewRect, Matrix.ScaleToFit.CENTER);
        image.setImageMatrix(M);
        image.invalidate();

        // Set the drag surface
        image.setOnDragListener(new View.OnDragListener() {
            private String draggedImageTag;
            private  float x_cord ,y_cord;
            private float zoomScale, pan_y,pan_x;

            @Override
            public boolean onDrag(View v, DragEvent event) {

                switch(event.getAction()) {
                    case DragEvent.ACTION_DRAG_STARTED:
                        View view =(View) event.getLocalState();
                        draggedImageTag = (String) view.getTag();
                        pan_x = zoomLayout.getPanX();
                        pan_y = zoomLayout.getPanY();
                        zoomScale = zoomLayout.getRealZoom();
                        engine.zoomTo(1,false);
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
                        Size size_mark_prev = marksSize.get(draggedImageTag);
                        Point mark_prev_location = marksLocation.get(draggedImageTag);
                        if(copyModeFlag){
                            double delta_x = (x_cord - (size_mark_prev.width/2))  - mark_prev_location.x ;
                            double delta_y = (y_cord - (size_mark_prev.height/2)) - mark_prev_location.y ;
                            View s = (View) event.getLocalState();
                            updateDropAction(draggedImageTag,(int)(y_cord - (size_mark_prev.height/2)),(int)(x_cord - (size_mark_prev.width/2)), s);
                            for (View mark_selected: marksViews.values() ) {
                                String MarkTag = (String) mark_selected.getTag(); //(String)mark_selected._mark.getTag();
                                if(!MarkTag.equals(draggedImageTag)){
                                    Point curr_mark_prev_location = marksLocation.get(MarkTag);
                                    int new_x_cord = (int)(curr_mark_prev_location.x + delta_x);
                                    int new_y_cord = (int)(curr_mark_prev_location.y + delta_y);
                                    updateDropAction(MarkTag, new_y_cord, new_x_cord, mark_selected);
                                }
                            }
                            engine.realZoomTo(zoomScale,false);
                            engine.panTo(pan_x,pan_y,false);


                        }
                        else{
                            View s = (View) event.getLocalState();
                            updateDropAction(draggedImageTag,(int)(y_cord - (size_mark_prev.height/2)),(int)(x_cord - (size_mark_prev.width/2)),s);
                            engine.realZoomTo(zoomScale,false);
                            engine.panTo(pan_x,pan_y,false);

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

    public void  updateDropAction(String ImageTag, float y_cord, float x_cord, View v ){
        Mark mark = marks.get(ImageTag);
        RelativeLayout mark_select = mark._mark;
        View view = v;
        view.setVisibility(View.VISIBLE);
        view.setX(x_cord);
        view.setY(y_cord);
        markToUpdate = mark_select;
        v.invalidate();
        updateLocation();

    }

    public void createWrongMark ( View v ) {
        String newTag = VIEW_WRONG_TAG +"_" + counterQuestion + counterWrong;
        counterWrong++;
        RelativeLayout markLayout = createMark("WrongAnswerMarker", newTag);
        markToUpdate = markLayout;
        relativeLayout.addView(markLayout,1);
        updateLocation();

    }

    public void createRightMark ( View v ) {
        String newTag = VIEW_RIGHT_TAG + "_" + counterQuestion + counterWrong;
        counterWrong++;
        RelativeLayout markLayout = createMark("RightAnswerMarker", newTag);
        markToUpdate = markLayout;
        relativeLayout.addView(markLayout,1);
        updateLocation();

    }

//    public void createQuestionMark ( View v ) {
//        String newTag = VIEW_QUESTION_TAG + "_" + counterQuestion;
//        counterQuestion++;
//        RelativeLayout markLayout = createMark("QuestionMarker",newTag);
//        markToUpdate = markLayout;
//        relativeLayout.addView(markLayout,1);
//        updateLocation();
//    }

    public void createBarcodeMark ( View v ) {
        String newTag = VIEW_BARCODE_TAG;
        RelativeLayout markLayout = createMark("barcode", newTag);
        markToUpdate = markLayout;
        relativeLayout.addView(markLayout,1);
        updateLocation();
        barcodeMark.setVisibility(View.INVISIBLE);

    }

    public void copy (View v ){
        for ( Mark mark: marksTogether.values() ) {
            String tag = (String) mark._mark.getTag();
            String[] parts =  tag.split("_");
            Point loction = marksLocation.get(tag);
            int X = (int) relativeLayout.getWidth();
            RelativeLayout newMark = null;
            String newTag;
            switch(parts[0]){
                case "WrongAnswerMarker":
                    newTag = parts[0]+"_"+counterQuestion+counterWrong;
                    counterWrong++;
                    newMark = createMark("WrongAnswerMarker",newTag);
                    break;
                case "RightAnswerMarker":
                    newTag = parts[0]+"_"+counterQuestion+counterWrong;
                    counterWrong++;
                    newMark = createMark("RightAnswerMarker",newTag);
                    break;
//                case "QuestionMarker":
//                    newTag = parts[0]+"_"+counterQuestion;
//                    counterQuestion++;
//                    newMark = createMark("QuestionMarker",newTag);
//                    break;
            }
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) newMark.getLayoutParams();
//              Set the circle location in the a beside the source mark
            params.removeRule(RelativeLayout.CENTER_HORIZONTAL);
            params.removeRule(RelativeLayout.CENTER_VERTICAL);
            params.topMargin = loction.y + 100 ;
            params.rightMargin = X - loction.x - 100;
            newMark.setVisibility(View.VISIBLE);
            markToUpdate = newMark;
            relativeLayout.addView(newMark,2);
            updateLocation();
            RelativeLayout mark_to_UnHighlight_layout =  mark._mark ;
            mark_to_UnHighlight_layout.setBackgroundColor(Color.parseColor("#00FFFFFF"));
        }
        copyModeFlag = false;
        copy.setVisibility(View.INVISIBLE);
        exitCopyMode.setVisibility(View.INVISIBLE);
        marksTogether.clear();
        marksViews.clear();
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
        double score = 100/numberOfQuestion;
        loading.setVisibility(View.VISIBLE);
        loading.bringToFront();
        fadingCircle.start();
        String filePath = Template.DB_FILEPATH;
        File file = new File(filePath);
        if(file.exists())
            file.delete();
        db = new Template(this);
        // calculate inverse matrix
        Matrix inverse = new Matrix();
        image.getImageMatrix().invert(inverse);

        for (int i = 0 ; i < numberOfQuestion ; ++i){
            for(int j = 0 ; j < numberOfOptions ; ++j){
                String tag = questionTable[i][j];
                Point location;
                Size size;
                int sumOfBlack;
                int id;
                String[] parts = (tag).split("_");
                location = marksLocation.get(tag);
                size = marksSize.get(tag);
                float[] p = new float[]{(float)location.x,(float)location.y};
                inverse.mapPoints(p);
                Point transfer_point = new Point(Math.round(p[0]),Math.round(p[1]));
                sumOfBlack = calculateBlackLevel(paper,transfer_point,size);
                id = (i+1)*10+(j+1);
                switch(parts[0]) {
                    case VIEW_WRONG_TAG:
                        db.insertData(id,(int)transfer_point.x,(int)transfer_point.y,(int)size.height,(int)size.width,sumOfBlack,0);
                        break;
                    case VIEW_RIGHT_TAG:
                        db.insertData(id,(int)transfer_point.x,(int)transfer_point.y,(int)size.height,(int)size.width,sumOfBlack,1);
                        break;
                }
            }
        }
        if(marks.containsKey(VIEW_BARCODE_TAG)){
            Point location;
            Size size;
            location = marksLocation.get(VIEW_BARCODE_TAG);
            size = marksSize.get(VIEW_BARCODE_TAG);
            float[] p = new float[]{(float)location.x,(float)location.y};
            inverse.mapPoints(p);
            Point transfer_barcode = new Point(Math.round(p[0]),Math.round(p[1]));
            db.insertData(0,(int)transfer_barcode.x,(int)transfer_barcode.y,(int)size.height,(int)size.width,0,0);
        }
        loading.setVisibility(View.INVISIBLE);
        fadingCircle.stop();
        Intent uploadTemplate = new Intent(getApplicationContext(), UserDropBoxActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("TemplateDataBase",db.getFilePath());
        bundle.putDouble("score",score);
        uploadTemplate.putExtras(bundle);
        startActivity(uploadTemplate);
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
        lastUserUpdateQuestion = Integer.parseInt(questionIDEdit.getText().toString());
        lastUserUpdateAnswer = Integer.parseInt(answerIDEdit.getText().toString());
        questionTable[lastUserUpdateQuestion-1][lastUserUpdateAnswer-1] = questionToInsertToTheTable;
        RelativeLayout mark_to_UnHighlight_layout =  marks.get(questionToInsertToTheTable)._mark ;
        mark_to_UnHighlight_layout.setBackgroundColor(Color.parseColor("#00FFFFFF"));
        layoutSetQuestionID.setVisibility(View.INVISIBLE);
        InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);

    }

    public void setNumbersQuestionAndAnswer ( View view ){
        numberOfQuestion = (Integer.parseInt(questionNumberEdit.getText().toString()));
        numberOfOptions = (Integer.parseInt(answerNumberEdit.getText().toString()));
        questionTable       = new String[numberOfQuestion][numberOfOptions];
        layoutGetInfo.setVisibility(View.INVISIBLE);

        InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);

    }

    private RelativeLayout createMark( String tag, String id ) {
        // Create mark layout
        Rect visibleView;
        final RelativeLayout.LayoutParams markParam;
        RelativeLayout markLayout;
        if(tag.equals("barcode")){
            markLayout= new RelativeLayout(this);
            markLayout.setTag(id);
            markParam = new RelativeLayout.LayoutParams(  // set the layout params for mark
                    barcodeMark.getWidth(),
                    barcodeMark.getHeight()
                    );
            marksSize.put(id,new Size(barcodeMark.getWidth(),barcodeMark.getHeight()));
            visibleView = new Rect();
        }
        else {
            markLayout= new RelativeLayout(this);
            markLayout.setTag(id);
            markParam = new RelativeLayout.LayoutParams(  // set the layout params for mark
                    wrongMark.getHeight(),
                    wrongMark.getWidth());
            marksSize.put(id,new Size(wrongMark.getHeight(),wrongMark.getWidth()));
            visibleView = new Rect();
        }


        screenLayout.getChildVisibleRect(relativeLayout,visibleView,null);

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
                        if (clickTime - lastClickTime < DOUBLE_CLICK_TIME_DELTA){
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
                (int)(wrongMark.getHeight()*0.4),
                (int)(wrongMark.getWidth()*0.4));
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
                        return;
                    }

            }
        });

        // Create resize buttons
        // plus button
        RelativeLayout.LayoutParams plusButtonParam = new RelativeLayout.LayoutParams(
                (int)(wrongMark.getHeight()*0.5),
                (int)(wrongMark.getWidth()*0.5));
        Button plusButton = new Button(this);  // create a new Button
        plusButton.setTag(id);  // set Button's id
        plusButtonParam.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 1); // set Button to the Bottom of ImageView
        plusButtonParam.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 1); // set Button to the LEFT of ImageView
        plusButton.setLayoutParams(plusButtonParam); // set defined layout params to Button
        plusButton.setBackgroundResource(android.R.drawable.btn_plus);
        // minus button
        RelativeLayout.LayoutParams minusButtonParam = new RelativeLayout.LayoutParams(
                (int)(wrongMark.getHeight()*0.5),
                (int)(wrongMark.getWidth()*0.5));
        Button minusButton = new Button(this);  // create a new Button
        minusButton.setTag(id);  // set Button's id
        minusButtonParam.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 1); // set Button to the Bottom of ImageView
        minusButtonParam.addRule(RelativeLayout.ALIGN_PARENT_LEFT, 1); // set Button to the LEFT of ImageView
        minusButton.setLayoutParams(minusButtonParam); // set defined layout params to Button
        minusButton.setBackgroundResource(android.R.drawable.btn_minus);

        plusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                float pan_x = zoomLayout.getPanX();
                float pan_y = zoomLayout.getPanY();
                float zoomScale = zoomLayout.getRealZoom();
                engine.zoomTo(1,false);

                double supremum = 140*4;
                String buttonTag = (String) view.getTag();
                Mark markSelected= marks.get(buttonTag);
                RelativeLayout markSelectedLayout = markSelected._mark;
                Button plusButton = markSelected._plusButton;
                Button minusButton = markSelected._minusButton;
                Button closeButton = markSelected._closeButton;
                // Get his layout params
                RelativeLayout.LayoutParams lParams = (RelativeLayout.LayoutParams) markSelectedLayout.getLayoutParams();
                RelativeLayout.LayoutParams pParams = (RelativeLayout.LayoutParams) plusButton.getLayoutParams();
                RelativeLayout.LayoutParams mParams = (RelativeLayout.LayoutParams) minusButton.getLayoutParams();
                RelativeLayout.LayoutParams cParams = (RelativeLayout.LayoutParams) closeButton.getLayoutParams();
                // get current mark size
                Size size = marksSize.get(buttonTag);
                double new_height = size.height + 15;
                double new_width = size.width + 15;
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
                    // set the changes
                    plusButton.setLayoutParams(pParams);
                    minusButton.setLayoutParams(mParams);
                    closeButton.setLayoutParams(cParams);
                    markSelectedLayout.setLayoutParams(lParams);
                    markSelectedLayout.setVisibility(View.VISIBLE);
                    marks.remove(buttonTag);
                    marksSize.remove(buttonTag);
                    marks.put(buttonTag,new Mark(markSelectedLayout,plusButton,minusButton,closeButton));
                    marksSize.put(buttonTag,new Size (new_width,new_height ));
                    markToUpdate =  markSelectedLayout;

                    updateLocation();

                }
                engine.realZoomTo(zoomScale,false);
                engine.panTo(pan_x,pan_y,false);
            }
        });
        minusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                float pan_x = zoomLayout.getPanX();
                float pan_y = zoomLayout.getPanY();
                float zoomScale = zoomLayout.getRealZoom();
                engine.zoomTo(1,false);
                double infimum = 140/5;

                String buttonTag = (String) view.getTag();
                Mark markSelected= marks.get(buttonTag);
                RelativeLayout markSelectedLayout = markSelected._mark;
                Button plusButton = markSelected._plusButton;
                Button minusButton = markSelected._minusButton;
                Button closeButton = markSelected._closeButton;
                // Get his layout params
                RelativeLayout.LayoutParams lParams = (RelativeLayout.LayoutParams) markSelectedLayout.getLayoutParams();
                RelativeLayout.LayoutParams pParams = (RelativeLayout.LayoutParams) plusButton.getLayoutParams();
                RelativeLayout.LayoutParams mParams = (RelativeLayout.LayoutParams) minusButton.getLayoutParams();
                RelativeLayout.LayoutParams cParams = (RelativeLayout.LayoutParams) closeButton.getLayoutParams();
                // get current mark size
                Size size = marksSize.get(buttonTag);
                double new_height = size.height - 15;
                double new_width = size.width - 15;
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
                // set the changes
                    plusButton.setLayoutParams(pParams);
                    minusButton.setLayoutParams(mParams);
                    closeButton.setLayoutParams(cParams);
                    markSelectedLayout.setLayoutParams(lParams);
                    markSelectedLayout.setVisibility(View.VISIBLE);
                    marks.remove(buttonTag);
                    marksSize.remove(buttonTag);
                    marks.put(buttonTag,new Mark(markSelectedLayout,plusButton,minusButton,closeButton));
                    marksSize.put(buttonTag,new Size (new_width,new_height));

                    markToUpdate =  markSelectedLayout;
                    updateLocation();

                }
                engine.realZoomTo(zoomScale,false);
                engine.panTo(pan_x,pan_y,false);
            }
        });



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
                break;
            default: break;
        }
    marks.put(id,new Mark(markLayout, plusButton, minusButton, closeButton));
    return markLayout;
    }

    private Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);
        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false);
        bm.recycle();
        return resizedBitmap;
    }

    private void updateLocation () {
                String tag = (String) markToUpdate.getTag();
                int [] location = new int[2];
                location[0]=(int)markToUpdate.getX();
                location[1]=(int)markToUpdate.getY();
                if(marksLocation.containsKey(tag)) {
                    marksLocation.remove(tag);
                }
                marksLocation.put( tag ,new Point(location[0],location[1]));
    }

    private int calculateBlackLevel( Mat img, Point location, Size size ){
        double blackLevel=0.0;
//        double[] currentPixel;
        for (int col = (int)location.x ; col < (location.x + size.width) ; ++col){
            for (int raw = (int)location.y ; raw < (location.y + size.height) ; ++raw){
                blackLevel = blackLevel+ img.get(raw,col)[0];
            }
        }
        return (int)blackLevel;
    }

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

}
