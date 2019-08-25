package com.example.tmankita.check4u.Activities;


import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

import android.graphics.RectF;
import android.hardware.Camera;
import android.os.Environment;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.example.tmankita.check4u.Camera.CameraPreviewFocus;
import com.example.tmankita.check4u.Camera.DrawingView;
import com.example.tmankita.check4u.Camera.PreviewSurfaceView;
import com.example.tmankita.check4u.Detectors.alignToTemplate;
import com.example.tmankita.check4u.R;
import com.example.tmankita.check4u.Detectors.detectDocument;


import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;

public class TouchActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_RECAPTURE = 1;

    private  static final String TAG= "TouchActivity";
    private PreviewSurfaceView camView;
    private CameraPreviewFocus cameraPreview;
    private DrawingView drawingView;
    private detectDocument.document paper_obj;
    private Mat orig;
    private Button edit_button;
    private Button ok_button;
    private Button set_button;
    private Button recapture_button;
    private ImageButton capture_button;
    private ImageView test;
    private LinearLayout buttonsLayout;
    private LinearLayout setButtonsLayout;

    private String VIEW_EDIT_TAG = "coronerMark";
    private int counter_coroner = 1;
    private HashMap<String,RelativeLayout> marksCoroners;
    private HashMap<String,Point> coronersLoaction;
    private RelativeLayout testLayout;
    private Bitmap origin_bitmap;
    private String idHelper;
    private static  float height;
    private static  float width;
    private Mat origcopy1;
    private Mat rotated_no_draw;
    private Matrix M1;
    private String caller;
    private String templatePath;
    private Bitmap bmp;

    // Used to load the 'native-lib' library on application startup.
//    static {
//        System.loadLibrary("native-lib");
//        System.loadLibrary("opencv_java4");
//    }

//    public native void align(long im, long imReference, long aligned);


    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_touch);

        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        testLayout = (RelativeLayout) findViewById(R.id.testlayout);
        buttonsLayout = (LinearLayout)  findViewById(R.id.buttonsLayout);
        setButtonsLayout = (LinearLayout)  findViewById(R.id.setButtonsLayout);
        ok_button = (Button) findViewById(R.id.ok_test);
        edit_button = (Button) findViewById(R.id.edit_test);
        set_button = (Button) findViewById(R.id.set_test);
        recapture_button = (Button) findViewById(R.id.take_agian);
        capture_button = (ImageButton) findViewById(R.id.capture_touch_activity);
        orig = new Mat();
        test = (ImageView) findViewById(R.id.test);
        marksCoroners = new HashMap<>();
        coronersLoaction = new HashMap<>();


        Bundle extras = getIntent().getExtras();
        caller = extras.getString("caller");
        if(caller.equals("oneByOne"))
            templatePath = extras.getString("templatePath");


        Display display = getWindowManager().getDefaultDisplay();
        android.graphics.Point size = new android.graphics.Point();
        display.getSize(size);
        width = size.x;
        height = size.y;


        camView = (PreviewSurfaceView) findViewById(R.id.preview_surface);
        SurfaceHolder camHolder = camView.getHolder();

//        cameraPreview = new CameraPreviewFocus(previewWidth, previewHeight);
        cameraPreview = new CameraPreviewFocus();
        camHolder.addCallback(cameraPreview);
        camHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        camView.setListener(cameraPreview);
        //cameraPreview.changeExposureComp(-currentAlphaAngle);
        drawingView = (DrawingView) findViewById(R.id.drawing_surface);
        camView.setDrawingView(drawingView);

        // Add a listener to the Capture button
        final ImageButton captureButton = (ImageButton) findViewById(R.id.capture_touch_activity);

        captureButton. setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        captureButton.setVisibility(View.INVISIBLE);
                        // get an image from the camera
                        cameraPreview.mCamera.takePicture(null, null, mPicture);
                    }
                }
        );

        test.setOnDragListener(new View.OnDragListener(){
            private String draggedImageTag;
            private  float x_cord ,y_cord;
            @Override
            public boolean onDrag(View v, DragEvent event) {
                switch(event.getAction()) {
                    case DragEvent.ACTION_DRAG_STARTED:
                        draggedImageTag = idHelper;
                        v.invalidate();
                        Log.i("Coroner", "Action is DragEvent.ACTION_DRAG_STARTED ");
                        break;

                    case DragEvent.ACTION_DRAG_ENTERED:
                        v.invalidate();
                        Log.i("Coroner", "Action is DragEvent.ACTION_DRAG_ENTERED");
                        break;

                    case DragEvent.ACTION_DRAG_EXITED:
                        v.invalidate();
                        Log.i("Coroner", "Action is DragEvent.ACTION_DRAG_EXITED");
                        break;

                    case DragEvent.ACTION_DRAG_LOCATION:
                        Log.i("Coroner", "TAG: "+ draggedImageTag+" Action is DragEvent.ACTION_DRAG_LOCATION");
                        Log.i("Coroner", "TAG: "+ draggedImageTag+" x:" + x_cord +" y:"+y_cord);

                        x_cord =  event.getX();
                        y_cord =  event.getY();

                        RelativeLayout coroner_selected= marksCoroners.get(idHelper);
                        updateDropAction(draggedImageTag,(int)(y_cord - (250/2)),(int)(x_cord - (250/2)),coroner_selected);
                        break;

                    case DragEvent.ACTION_DRAG_ENDED:
                        v.invalidate();
                        break;

                    case DragEvent.ACTION_DROP:
                        v.invalidate();
                        Log.i("Coroner", "ACTION_DROP event");
                        break;

                    default: break;
                }
                return true;
            }
        });
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");

                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };


    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

//            Log.d(TAG, "in On Picture Taken, data length: "+ data.length);

            //get the camera parameters
//            Camera.Parameters parameters = camera.getParameters();

            //convert the byte[] to Bitmap;
            bmp = BitmapFactory.decodeByteArray(data, 0, data.length);

            //convert Bitmap to Mat; note the bitmap config ARGB_8888 conversion that
            //allows you to use other image processing methods and still save at the end
            bmp = bmp.copy(Bitmap.Config.ARGB_8888, true);
            Utils.bitmapToMat(bmp, orig);
            Mat origCopy_before_rotate = new Mat();
            orig.copyTo(origCopy_before_rotate);


            paper_obj = detectDocument.findDocument(orig);


            ArrayList<Point[]> Rps1;
            Rps1 = paper_obj.allpoints_original;
            for (Point[] ps : Rps1)
            {

                Imgproc.line(origCopy_before_rotate,ps[0],ps[1],new Scalar(255, 255, 0, 0), 10);
                Imgproc.line(origCopy_before_rotate,ps[1],ps[2],new Scalar(255, 255, 0, 0), 10);
                Imgproc.line(origCopy_before_rotate,ps[2],ps[3],new Scalar(255, 255, 0, 0), 10);
                Imgproc.line(origCopy_before_rotate,ps[3],ps[0],new Scalar(255, 255, 0, 0), 10);

            }
            Mat origCopy_rotatetd_with_draw = new Mat();
            rotated_no_draw = new Mat();
            Core.rotate(orig, rotated_no_draw, Core.ROTATE_90_CLOCKWISE);
            Core.rotate(origCopy_before_rotate, origCopy_rotatetd_with_draw, Core.ROTATE_90_CLOCKWISE);
            Core.rotate(paper_obj.doc_resized,paper_obj.doc_resized,Core.ROTATE_90_CLOCKWISE);
            origcopy1 = new Mat();

            rotated_no_draw.copyTo(origcopy1);

            Bitmap bmpPaper_UNTOUCH = Bitmap.createBitmap(rotated_no_draw.cols(), rotated_no_draw.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(rotated_no_draw, bmpPaper_UNTOUCH);
            Matrix matrix1 = new Matrix();
            origin_bitmap = Bitmap.createBitmap(bmpPaper_UNTOUCH, 0, 0, bmpPaper_UNTOUCH.getWidth(), bmpPaper_UNTOUCH.getHeight(), matrix1, true);


            //srcMat.release();
            Bitmap bmpPaper = Bitmap.createBitmap(origCopy_rotatetd_with_draw.cols(), origCopy_rotatetd_with_draw.rows(), Bitmap.Config.ARGB_8888);


            Utils.matToBitmap(origCopy_rotatetd_with_draw, bmpPaper);
            Matrix matrix2 = new Matrix();
            Bitmap bOutput = Bitmap.createBitmap(bmpPaper, 0, 0, bmpPaper.getWidth(), bmpPaper.getHeight(), matrix2, true);


            PreviewSurfaceView cameraPr = (PreviewSurfaceView) findViewById(R.id.preview_surface);
            cameraPr.setVisibility(View.INVISIBLE);

            testLayout.setVisibility(View.VISIBLE);
            test.setVisibility(View.VISIBLE);
            setButtonsLayout.setVisibility(View.INVISIBLE);
            capture_button.setVisibility(View.INVISIBLE);

            test.setImageBitmap(bOutput);
            Matrix M = test.getImageMatrix();
            RectF drawableRect = new RectF(0, 0, origCopy_rotatetd_with_draw.cols(), origCopy_before_rotate.rows());
            RectF viewRect = new RectF(0, 0, width, height);
            M.setRectToRect(drawableRect, viewRect, Matrix.ScaleToFit.CENTER);
            test.setImageMatrix(M);
            test.invalidate();


            buttonsLayout.setVisibility(View.VISIBLE);
            buttonsLayout.bringToFront();

        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        /**
         * Continues from take picture again
         */
        if (requestCode == REQUEST_CODE_RECAPTURE) {
            if (resultCode == Activity.RESULT_OK) {
                String[] imagesPathes = data.getStringArrayExtra("sheets");
                Intent returnIntent = new Intent();
                returnIntent.putExtra("sheets",imagesPathes);
                setResult(Activity.RESULT_OK,returnIntent);
                finish();
            }  if (resultCode == Activity.RESULT_CANCELED) {}
        }
    }

    /**
     * Button for confirm that the automate edges detecting success.
     * @param view is "OK" button
     * @return None
     */
    public void ok_test (View view){
        send_image(paper_obj.doc_resized);
    }

    /**
     * Button for editing manually the edge detecting of the template.
     * @param view is "Edit" Button
     * @return None
     */
    public void edit_test(View view){
        buttonsLayout.setVisibility(View.INVISIBLE);
        setButtonsLayout.setVisibility(View.VISIBLE);
        setButtonsLayout.bringToFront();

        test.setImageBitmap(origin_bitmap);
        M1 = test.getImageMatrix();
        RectF drawableRect = new RectF(0, 0, origcopy1.cols(), origcopy1.rows());
        RectF viewRect = new RectF(0, 0, width, height);
        M1.setRectToRect(drawableRect, viewRect, Matrix.ScaleToFit.CENTER);
        test.setImageMatrix(M1);


        // make programmatically 4 green marks with padding for touch.
        for (Point p: paper_obj.allpoints_original.get(0)) {
            build_coroner((int)p.x,(int)p.y);
        }
        // make drag surface on test image-view
        // make drag builder and touch- event on 4 marks
        // save 4 new marks location
        // do sortPoints -> fourPointTransform ->  enhanceDocument
        // save new image to paper_obj.doc
        // use send image function

    }

    /**
     * Button for setting the manual coroners of the template.
     * @param view is "Set" button
     * @return None
     */
    public void set_test (View view) {

        Point[] points_2 = new Point[4];
        int i=0;
        for (Point p: coronersLoaction.values()) {
            points_2[i]=p;
            i++;
        }

        Point[] sorted_2 = detectDocument.sortPoints(points_2);

        for (RelativeLayout mark: marksCoroners.values()) {
            mark.setVisibility(View.INVISIBLE);
        }
        set_button.setVisibility(View.INVISIBLE);

        // calculate inverse matrix
        Matrix inverse = new Matrix();
        M1.invert(inverse);

        float[][] image_point_2 = new float[][]{
                {(float)sorted_2[0].x+250/2,(float)sorted_2[0].y+250/2-50},
                {(float)sorted_2[1].x+250/2,(float)sorted_2[1].y+250/2-50},
                {(float)sorted_2[2].x+250/2,(float)sorted_2[2].y+250/2-50},
                {(float)sorted_2[3].x+250/2,(float)sorted_2[3].y+250/2-50}
        };
        Point[] final2= new Point[4];
        i=0;
        for (float[] p: image_point_2) {
            inverse.mapPoints(p);
            final2[i]= new Point(p[0],p[1]);
            i++;
        }

        Mat croped = fourPointTransform_touch(origcopy1,final2);
        Mat cropedGray = new Mat(croped.size(),CvType.CV_8UC1);
        Imgproc.cvtColor(croped,cropedGray,Imgproc.COLOR_RGBA2GRAY);
        croped.release();

        send_image(cropedGray);
    }

    /**
     * Button for taking a new picture of template.
     * @param view is 'Recapture" button
     * @return None
     */
    public void recapture (View view){
        Intent takePicture = new Intent(getApplicationContext(), TouchActivity.class);
        takePicture.putExtra("templatePath", templatePath);
        takePicture.putExtra("caller", "oneByOne");
        startActivityForResult(takePicture, REQUEST_CODE_RECAPTURE);
    }

    /**
     * Function the transform from src to the pts on the src
     * @param src is the image we want apply the four points transformation
     * @param pts is the four points for the transformation
     * @return new image after transform
     */
    public static Mat fourPointTransform_touch( Mat src , Point[] pts ) {

        Point tl = pts[0];
        Point tr = pts[1];
        Point br = pts[2];
        Point bl = pts[3];

        double widthA = Math.sqrt(Math.pow(br.x - bl.x, 2) + Math.pow(br.y - bl.y, 2));
        double widthB = Math.sqrt(Math.pow(tr.x - tl.x, 2) + Math.pow(tr.y - tl.y, 2));

        double dw = Math.max(widthA, widthB);
        int maxWidth = Double.valueOf(dw).intValue();


        double heightA = Math.sqrt(Math.pow(tr.x - br.x, 2) + Math.pow(tr.y - br.y, 2));
        double heightB = Math.sqrt(Math.pow(tl.x - bl.x, 2) + Math.pow(tl.y - bl.y, 2));

        double dh = Math.max(heightA, heightB);
        int maxHeight = Double.valueOf(dh).intValue();

        Mat doc = new Mat(maxHeight, maxWidth, CvType.CV_8UC4);

        Mat src_mat = new Mat(4, 1, CvType.CV_32FC2);
        Mat dst_mat = new Mat(4, 1, CvType.CV_32FC2);

//        src_mat.put(0, 0, tl.x*ratio, tl.y*ratio, tr.x*ratio, tr.y*ratio, br.x*ratio, br.y*ratio, bl.x*ratio, bl.y*ratio);

        src_mat.put(0, 0, tl.x, tl.y, tr.x, tr.y, br.x, br.y, bl.x, bl.y);
        dst_mat.put(0, 0, 0.0, 0.0, dw, 0.0, dw, dh, 0.0, dh);

        Mat m = Imgproc.getPerspectiveTransform(src_mat, dst_mat);

        Imgproc.warpPerspective(src, doc, m, doc.size());

        return doc;
    }

    /**
     * update the location of v to x_cord and y_cord on the display.
     * @param CoronerTag is
     * @param y_cord is
     * @param x_cord is
     * @param v is
     * @return None
     */
    public void  updateDropAction(String CoronerTag, float y_cord, float x_cord, View v ){

        View view = v;
        view.setVisibility(View.VISIBLE);
        view.setX(x_cord);
        view.setY(y_cord);
        view.invalidate();
        Point p = new Point(x_cord,y_cord);
        if(coronersLoaction.containsKey(CoronerTag))
            coronersLoaction.remove(CoronerTag);
        coronersLoaction.put(CoronerTag, p );

    }

    /**
     * function build mark layout for edge detect editing
     * @param x is
     * @param y is
     * @return None
     */
    public void build_coroner(int x,int y){
        String newTag = VIEW_EDIT_TAG +"_" + counter_coroner;
        counter_coroner++;
        RelativeLayout markLayout = new RelativeLayout(this);
        markLayout.setTag(newTag);
        RelativeLayout.LayoutParams markParam = new RelativeLayout.LayoutParams(  // set the layout params for mark
                250,
                250);
        coronersLoaction.put(newTag,new Point(x,y));

        markParam.addRule(RelativeLayout.CENTER_HORIZONTAL);
        markParam.addRule(RelativeLayout.CENTER_VERTICAL);
//        markParam.setMargins(0,y,X-x,0);
        markLayout.setLayoutParams(markParam); // set defined layout params to mark layout

        // on touch event!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        markLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                idHelper = (String) v.getTag();
                RelativeLayout layout = marksCoroners.get(idHelper);
                if (layout != null) {
                    ClipData data = ClipData.newPlainText("", "");
                    View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(layout);
                    layout.startDrag(data, shadowBuilder,v, View.DRAG_FLAG_OPAQUE);
                    layout.setVisibility(View.INVISIBLE);
                    return true;
                }
                return false;
            }
        });

        // set the layout params for ImageView
        RelativeLayout.LayoutParams imageViewParam = new RelativeLayout.LayoutParams(
               150,
                150);

        // create a new ImageView
        ImageView imageView = new ImageView(this);
        imageView.setTag(newTag);
        imageViewParam.addRule(RelativeLayout.CENTER_HORIZONTAL); // align ImageView in the center
        imageView.setLayoutParams(imageViewParam); // set defined layout params to ImageView

        // set resource in ImageView
        imageView.setImageResource(R.drawable.square_wrong_answer);
        // add ImageView in RelativeLayout
        markLayout.addView(imageView);

        testLayout.addView(markLayout,1);
        marksCoroners.put(newTag,markLayout);

    }


    /**
     * run the alignment algorithem on paper and template
     * @param paper
     * @return alignToTemplate Object that contain align image and strong marks align image
     */
    private alignToTemplate alignBeforeSend (Mat paper){
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmapT = BitmapFactory.decodeFile(templatePath, options);
        Bitmap bmpT = bitmapT.copy(Bitmap.Config.ARGB_8888, true);
        Mat template = new Mat();
        Utils.bitmapToMat(bmpT, template);

        alignToTemplate align_to_template = new alignToTemplate();
        align_to_template.align1(paper,template,bmp,"OneByOne"); //coment

//        TemplateMatching template_matching = new TemplateMatching();
//        Mat match = template_matching.match2(template, paper);

//                Mat aligned = new Mat(new Size(orig.cols(),orig.rows()), CvType.CV_8UC1);
//                align(orig.nativeObj,template.nativeObj,aligned.nativeObj);


//        Bitmap bmpBarcode23 = Bitmap.createBitmap(align_to_template.align.cols(), align_to_template.align.rows(), Bitmap.Config.ARGB_8888);
//        Utils.matToBitmap(align_to_template.align, bmpBarcode23);
//
//        Mat dcRotateA = new Mat(align_to_template.align.size(), align_to_template.align.type());
//        Core.rotate(align_to_template.align, dcRotateA, Core.ROTATE_90_CLOCKWISE);
//        Mat dcRotateS = new Mat(align_to_template.strongMarks.size(), align_to_template.strongMarks.type());
//        Core.rotate(align_to_template.strongMarks, dcRotateS, Core.ROTATE_90_CLOCKWISE);
//
//        Bitmap bmpBarcode24 = Bitmap.createBitmap(dcRotateA.cols(), dcRotateA.rows(), Bitmap.Config.ARGB_8888);
//        Utils.matToBitmap(dcRotateA, bmpBarcode24);
//        Bitmap bmpBarcode25 = Bitmap.createBitmap(dcRotateS.cols(), dcRotateS.rows(), Bitmap.Config.ARGB_8888);
//        Utils.matToBitmap(dcRotateS, bmpBarcode25);
//        align_to_template.align.release();
//        align_to_template.strongMarks.release();
//        align_to_template.align = dcRotateA;
//        align_to_template.strongMarks = dcRotateS;

        return align_to_template;
    }

    /**
     * create jpg file from img
     * @param img is image
     * @param adding is some extras for the name of the jpg file.
     * @return Path of jpg file of the img on the disk.
     */
    static public String send_imageHelper (Mat img, String adding){
        Matrix matrix = new Matrix();
//        matrix.setRotate(90);
        Bitmap bmpPaper = Bitmap.createBitmap(img.cols(), img.rows(), Bitmap.Config.ARGB_8888);

        Utils.matToBitmap(img, bmpPaper);
        Bitmap bOutput = Bitmap.createBitmap(bmpPaper, 0, 0, bmpPaper.getWidth(), bmpPaper.getHeight(), matrix, true);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bOutput.compress(Bitmap.CompressFormat.JPEG, 70, stream);
        byte[] paperData = stream.toByteArray();

        File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE, adding);
        if (pictureFile == null) {
            Log.d(TAG, "Error creating media file, check storage permissions");
            return "";
        }

        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            fos.write(paperData);
            fos.close();
            return pictureFile.getAbsolutePath();

        } catch (FileNotFoundException e) {
            Log.d(TAG, "File not found: " + e.getMessage());
            return "";
        } catch (IOException e) {
            Log.d(TAG, "Error accessing file: " + e.getMessage());
            return "";
        }
    }

    /**
     * send paper to the next activity
     * @param paper is the image
     * @return None
     */
    private void send_image (Mat paper){
        //srcMat.release();
        String[] res = new String[2];
//        img = new Mat();
//        paper.copyTo(img);
        if(caller.equals("oneByOne")){
            alignToTemplate align_to_template = alignBeforeSend (paper);
            res[0] = send_imageHelper(align_to_template.align,"a");
            res[1] = send_imageHelper(align_to_template.strongMarks,"m");
        }else {
            res[0] = send_imageHelper(paper,"");
        }


        Intent returnIntent = new Intent();
        returnIntent.putExtra("sheets",res);
        setResult(Activity.RESULT_OK,returnIntent);
        finish();

    }

    /**
     * generate File Object with type <type> .
     * @param type is the type you want
     * @param adding is some extras for the name of the file.
     * @return File with the type and with adding in his name
     */
    private static File getOutputMediaFile(int type,String adding){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        String Path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath() + "/Check4U_DB/DCIM";
        File mediaStorageDir = new File(Path);
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d("Check4U", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date())+adding;
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_"+ timeStamp + ".jpg");
        } else if(type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_"+ timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }

}
