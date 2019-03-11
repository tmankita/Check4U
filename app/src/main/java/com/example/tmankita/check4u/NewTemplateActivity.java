package com.example.tmankita.check4u;

import android.content.ClipData;
import android.content.ClipDescription;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.DragEvent;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageView;

import android.widget.RelativeLayout;

import com.otaliastudios.zoom.ZoomLayout;

import java.util.HashMap;



public class NewTemplateActivity extends AppCompatActivity {

    private ImageView image;
    private RelativeLayout relativeLayout;
    private HashMap<String,Mark> marks;
    private HashMap<String,Point> marksLocation;
    private ZoomLayout zoomLayout;

    // Create a string for the View label
        private static final String VIEW_WRONG_TAG = "WrongAnswerMarker";
        private static final String VIEW_RIGHT_TAG = "RightAnswerMarker";
        private static final String VIEW_QUESTION_TAG = "QuestionMarker";
        private static final String VIEW_BARCODE_TAG = "BarcodeMarker";

    //Counters for each Mark
        private int counterWrong = 0;
        private int counterRight = 0;
        private int counterQuestion = 0;

    //Markers
        private ImageView wrongMark;
        private ImageView rightMark;
        private ImageView questionMark;
        private ImageView barcodeMark;


    //Helpers
        private RelativeLayout markToUpdate;
        int threshold;


    public class Mark {
       public RelativeLayout _mark;
       public Button _resizeButton;
       public Button _closeButton;

        public Mark (RelativeLayout mark, Button resizeButton, Button closeButton){
            _mark = mark;
            _resizeButton = resizeButton;
            _closeButton = closeButton;
        }

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_template);

        marksLocation       = new HashMap<>();
        marks               = new HashMap<>();
        relativeLayout      = (RelativeLayout) findViewById(R.id.Layout);
        wrongMark           = (ImageView) findViewById(R.id.wrongAns);
        rightMark           = (ImageView) findViewById(R.id.rightAns);
        questionMark        = (ImageView) findViewById(R.id.question);
        barcodeMark         = (ImageView) findViewById(R.id.ID_digit);
        threshold           = 140;
        zoomLayout          = findViewById(R.id.zoom_layout);


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
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;
        image.setImageBitmap(getResizedBitmap(bitmap,width,height));



        image.setOnDragListener(new View.OnDragListener() {
            private String draggedImageTag;
            private int X;
            private  int x_cord ,y_cord;
            boolean dropped;

            @Override
            public boolean onDrag(View v, DragEvent event) {
                X = (int) relativeLayout.getWidth();

                switch(event.getAction()) {
                    case DragEvent.ACTION_DRAG_STARTED:
                        View view = (View) event.getLocalState();
                        draggedImageTag = (String) view.getTag();
                        Log.i("Mark", "Action is DragEvent.ACTION_DRAG_STARTED ");
                        break;

                    case DragEvent.ACTION_DRAG_ENTERED:
                        Log.i("Mark", "Action is DragEvent.ACTION_DRAG_ENTERED");
                        break;

                    case DragEvent.ACTION_DRAG_EXITED:
                        Log.i("Mark", "Action is DragEvent.ACTION_DRAG_EXITED");
                        break;

                    case DragEvent.ACTION_DRAG_LOCATION:
                        Log.i("Mark", "Action is DragEvent.ACTION_DRAG_LOCATION");
                        x_cord = (int) event.getX();
                        y_cord = (int) event.getY();


                        // Find drag square
                        Mark mark = marks.get(draggedImageTag);
                        RelativeLayout mark_select = mark._mark;
                        int heigh = mark_select.getHeight();
                        int width = mark_select.getWidth();

                        Log.i("Answer_Mark", "x "+ x_cord + " y " + y_cord );
                        // Get his layout params
                        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mark_select.getLayoutParams();
                        // Set the square location in the drop location
                        params.removeRule(relativeLayout.CENTER_HORIZONTAL);
                        params.removeRule(relativeLayout.CENTER_VERTICAL);

                        if (threshold == width) {
                            params.topMargin = y_cord + (int) (0.5 * heigh);
                            params.rightMargin = X - x_cord - (int) (0.5 * width);
                        }
                        else if (threshold/3 > width) {
                            Log.i("Answer_Mark", "heigh "+ heigh + " width " + width );
                            params.topMargin = y_cord + (int) (3.5 * heigh);
                            params.rightMargin = X - x_cord - (int) (0.5 * width);
                        }
                        else if (threshold > width) {
                            Log.i("Answer_Mark", "heigh "+ heigh + " width " + width );
                            params.topMargin = y_cord + (int) (2.5 * heigh);
                            params.rightMargin = X - x_cord - (int) (0.5 * width);
                        }
                        else
                        {
                            params.topMargin = y_cord + (int) (0.1 * heigh);
                            params.rightMargin = X - x_cord - (int) (0.6789 * width);
                        }

                        mark_select.setVisibility(View.VISIBLE);
                        markToUpdate = mark_select;
                        relativeLayout.updateViewLayout(mark_select,params);
                        marks.remove(draggedImageTag);
                        marks.put(draggedImageTag,new Mark (mark_select,mark._resizeButton,mark._closeButton));

                        break;

                    case DragEvent.ACTION_DRAG_ENDED:
                        if(dropped)
                            updateLocation();
                        break;

                    case DragEvent.ACTION_DROP:
                        Log.i("Answer_Mark", "ACTION_DROP event");
                        dropped=true;

                        break;

                    default: break;
                }
                return true;
            }
        });
    }



    public void createWrongMark ( View v ) {
        String newTag = VIEW_WRONG_TAG + "_" + counterWrong;
        counterWrong++;
        RelativeLayout markLayout = createMark("answer", newTag);
        markToUpdate = markLayout;
        relativeLayout.addView(markLayout,2);
        updateLocation();

    }
    public void createRightMark ( View v ) {
        String newTag = VIEW_RIGHT_TAG + "_" + counterRight;
        counterRight++;
        RelativeLayout markLayout = createMark("right_answer", newTag);
        markToUpdate = markLayout;
        relativeLayout.addView(markLayout,2);
        updateLocation();

    }
    public void createQuestionMark ( View v ) {
        String newTag = VIEW_QUESTION_TAG + "_" + counterQuestion;
        counterQuestion++;
        RelativeLayout markLayout = createMark("question",newTag);
        markToUpdate = markLayout;
        relativeLayout.addView(markLayout,2);
        updateLocation();
    }

    public void createBarcodeMark ( View v ) {
        String newTag = VIEW_BARCODE_TAG;
        RelativeLayout markLayout = createMark("barcode", newTag);
        markToUpdate = markLayout;
        relativeLayout.addView(markLayout,2);
        updateLocation();

    }




    //TODO detect barcode of Student ID
    //TODO create Markers of the right answers and the wrong ones
    //TODO create sqlite file that store all the data about that template (use room tech)




    private RelativeLayout createMark( String tag, String id ) {

        // Create mark layout
        RelativeLayout markLayout= new RelativeLayout(this);
        markLayout.setTag(id);
        RelativeLayout.LayoutParams markParam = new RelativeLayout.LayoutParams(  // set the layout params for mark
                wrongMark.getHeight(),
                wrongMark.getWidth());

        markParam.addRule(relativeLayout.CENTER_HORIZONTAL);
        markParam.addRule(relativeLayout.CENTER_VERTICAL);
        markLayout.setLayoutParams(markParam); // set defined layout params to mark layout

       final GestureDetector gd = new GestureDetector(getApplicationContext(),new GestureDetector.SimpleOnGestureListener(){

            @Override
            public boolean onDoubleTap(MotionEvent e) {

                //your action here for double tap e.g.
                //Log.d("OnDoubleTapListener", "onDoubleTap");

                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                super.onLongPress(e);

            }

            @Override
            public boolean onDoubleTapEvent(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

        });



        markLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                String id = (String) v.getTag();
                RelativeLayout layout = marks.get(id)._mark;
                    if(layout != null){
                        if (event.getAction() == MotionEvent.ACTION_DOWN) {
                            ClipData data = ClipData.newPlainText("", "");
                            View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(layout);
                            layout.startDrag(data, shadowBuilder, layout, 0);
                            layout.setVisibility(View.INVISIBLE);
                            return true;
                        } else {
                            return gd.onTouchEvent(event);
                        }
                    }
                return false;
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
                RelativeLayout layout = marks.get(id)._mark;
                if(layout != null){
                        marks.remove(id);
                        relativeLayout.removeView(layout);
                        return;
                    }

            }
        });

        // Create resize button
        RelativeLayout.LayoutParams resizeButtonParam = new RelativeLayout.LayoutParams(
                (int)(wrongMark.getHeight()*0.4),
                (int)(wrongMark.getWidth()*0.4));
        Button resizeButton = new Button(this);  // create a new Button
        resizeButton.setTag(id);  // set Button's id
        resizeButtonParam.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 1); // set Button to the Bottom of ImageView
        resizeButtonParam.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 1); // set Button to the LEFT of ImageView
        resizeButton.setLayoutParams(resizeButtonParam); // set defined layout params to Button
        resizeButton.setBackgroundResource(R.drawable.resize);
        resizeButton.setOnTouchListener(new View.OnTouchListener() {
            int oldHeight;
            int oldWidth;
            boolean update;
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                int x = (int) motionEvent.getRawX() ;
                int y = (int) motionEvent.getRawY() ;

                String buttonTag = (String) view.getTag();
                Mark markSelected= marks.get(buttonTag);
                RelativeLayout markSelectedLayout = markSelected._mark;
                Button resizeButton = markSelected._resizeButton;
                Button closeButton = markSelected._closeButton;

                int permanentX = marksLocation.get(buttonTag).x;
                int permanentY = marksLocation.get(buttonTag).y;

                switch (motionEvent.getAction()) {

                    case MotionEvent.ACTION_DOWN:
                        Log.i("TAG", "touched down");
                        oldHeight = (int) motionEvent.getRawX() ;
                        oldHeight = (int) motionEvent.getRawY() ;
                        update=false;
                        break;

                    case MotionEvent.ACTION_MOVE:
                         Log.i("TAG", "moving: (" + x + ", " + y + ")");
                        double new_height=0;
                        double new_width=0;

                        if(x > oldHeight) {
                             new_height = x - permanentX;
                             new_width = x - permanentX;
                         }
                         else if(y > oldWidth)  {
                            new_height = y - permanentY;
                            new_width = y - permanentY;
                        }

                            if (new_height < 0 || new_width < 0){
                                 new_height = 0;
                                new_width = 0;
                            }
                            // Get his layout params
                            RelativeLayout.LayoutParams lParams = (RelativeLayout.LayoutParams) markSelectedLayout.getLayoutParams();
                            RelativeLayout.LayoutParams rParams = (RelativeLayout.LayoutParams) resizeButton.getLayoutParams();
                            RelativeLayout.LayoutParams cParams = (RelativeLayout.LayoutParams) closeButton.getLayoutParams();
                            if(new_height!=0){
                                update=true;
                                // resize layout
                                lParams.height = (int) new_height;
                                lParams.width = (int) new_width;
                                // resize buttons
                                rParams.height = (int) (0.3*new_height);
                                rParams.width = (int) (0.3*new_width);
                                cParams.height = (int) (0.3*new_height);
                                cParams.width = (int) (0.3*new_width);
                                // set the changes
                                resizeButton.setLayoutParams(rParams);
                                closeButton.setLayoutParams(cParams);
                                markSelectedLayout.setLayoutParams(lParams);
                                markSelectedLayout.setVisibility(View.VISIBLE);
                                marks.remove(buttonTag);
                                marks.put(buttonTag,new Mark(markSelectedLayout,resizeButton,closeButton));
                                updateLocation();
                            }

                        break;

                    case MotionEvent.ACTION_UP:
                        Log.i("TAG", "touched up");


                        break;
                    case MotionEvent.ACTION_CANCEL:
                        Log.i("TAG", "touched CANCEL");
                        break;
                }
                return false;
            }
        });

        switch (tag){

            case "answer":
                // Set id for mark
                markLayout.setId(R.id.answer_mark);
                // set resource in ImageView
                imageView.setImageResource(R.drawable.square_wrong_answer);
                // add ImageView in RelativeLayout
                markLayout.addView(imageView);
                // add close Button in RelativeLayout
                markLayout.addView(closeButton);
                // add resize Button in RelativeLayout
                markLayout.addView(resizeButton);

//                markLayout.setOnLongClickListener(new View.OnLongClickListener() {
//                    @Override
//                    public boolean onLongClick(View v) {
//                        ClipData.Item item = new ClipData.Item((CharSequence)v.getTag());
//                        String[] mimeTypes = {ClipDescription.MIMETYPE_TEXT_PLAIN};
//
//                        ClipData dragData = new ClipData(v.getTag().toString(),mimeTypes, item);
//                        View.DragShadowBuilder myShadow = new View.DragShadowBuilder();
//
//                        v.startDrag(dragData,myShadow,null,0);
//                        return true;
//                    }
//                });

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
                markLayout.addView(resizeButton);



            case "question":
                // Set id for mark
                markLayout.setId(R.id.question_mark);
                // set resource in ImageView
                imageView.setImageResource(R.drawable.square_question);
                // add ImageView in RelativeLayout
                markLayout.addView(imageView);
                // add close Button in RelativeLayout
                markLayout.addView(closeButton);
                // add resize Button in RelativeLayout
                markLayout.addView(resizeButton);

                break;

            case "right_answer":
                // Set id for mark
                markLayout.setId(R.id.right_answer_mark);
                // set resource in ImageView
                imageView.setImageResource(R.drawable.square_right_answer);
                // add ImageView in RelativeLayout
                markLayout.addView(imageView);
                // add close Button in RelativeLayout
                markLayout.addView(closeButton);
                // add resize Button in RelativeLayout
                markLayout.addView(resizeButton);

                break;
            default: break;
        }

        marks.put(id,new Mark(markLayout, resizeButton, closeButton));
    return markLayout;
    }

    public Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
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

    public void updateLocation () {
        // Set listener on the view tree to update locations
        ViewTreeObserver vto = relativeLayout.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener(){
            @Override public void onGlobalLayout(){
                String tag = (String) markToUpdate.getTag();
                int [] location = new int[2];
                markToUpdate.getLocationOnScreen(location);

                if(marksLocation.containsKey(tag))
                    marksLocation.remove(tag);

                marksLocation.put( tag ,new Point(location[0],location[1]));
                relativeLayout.getViewTreeObserver().removeGlobalOnLayoutListener(this);

            }
        });
    }
}
