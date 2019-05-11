package com.example.tmankita.check4u.Camera;


import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;

import android.graphics.RectF;
import android.hardware.Camera;
import android.os.Environment;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.example.tmankita.check4u.detectDocumentHough;
import com.example.tmankita.check4u.R;
import com.example.tmankita.check4u.detectDocument;
import com.github.ybq.android.spinkit.sprite.Sprite;
import com.github.ybq.android.spinkit.style.FadingCircle;


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
import static android.widget.ImageView.ScaleType.FIT_START;

public class TouchActivity extends AppCompatActivity {
    private  static final String TAG= "TouchActivity";
    private PreviewSurfaceView camView;
    private CameraPreviewFocus cameraPreview;
    private DrawingView drawingView;
    private Sprite fadingCircle;
    private ImageView imageView;
    private detectDocument.document paper_obj;
    private Mat orig;
    private Button edit_button;
    private Button ok_button;
    private Button set_button;
    private ImageButton capture_button;
    private ImageView test;

    private int previewWidth = 1280;
    private int previewHeight = 720;

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
    private Point[] right;



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
        imageView = (ImageView) findViewById(R.id.imageView);
        fadingCircle = new FadingCircle();
        fadingCircle.setColor(Color.WHITE);
        imageView.setImageDrawable(fadingCircle);
        imageView.setVisibility(View.INVISIBLE);
        ok_button = (Button) findViewById(R.id.ok_test);
        edit_button = (Button) findViewById(R.id.edit_test);
        set_button = (Button) findViewById(R.id.set_test);
        capture_button = (ImageButton) findViewById(R.id.capture_touch_activity);
        orig = new Mat();
        test = (ImageView) findViewById(R.id.test);
        marksCoroners = new HashMap<>();
        coronersLoaction = new HashMap<>();

        DisplayMetrics metrics = this.getResources().getDisplayMetrics();
        height= (float)metrics.heightPixels;
        width = (float)metrics.widthPixels;
//        ratio = (height/ width);


        camView = (PreviewSurfaceView) findViewById(R.id.preview_surface);
        SurfaceHolder camHolder = camView.getHolder();

        cameraPreview = new CameraPreviewFocus(previewWidth, previewHeight);
        camHolder.addCallback(cameraPreview);
        camHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        camView.setListener(cameraPreview);
        //cameraPreview.changeExposureComp(-currentAlphaAngle);
        drawingView = (DrawingView) findViewById(R.id.drawing_surface);
        camView.setDrawingView(drawingView);

        // Add a listener to the Capture button
        ImageButton captureButton = (ImageButton) findViewById(R.id.capture_touch_activity);

        captureButton. setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
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

    public void ok_test (View view){
        send_image(paper_obj.doc_resized);
    }
    public void edit_test(View view){
        set_button.setVisibility(View.VISIBLE);
        edit_button.setVisibility(View.INVISIBLE);
        ok_button.setVisibility(View.INVISIBLE);
        test.setImageBitmap(origin_bitmap);
        test.setScaleType(FIT_START);
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
        test.getImageMatrix().invert(inverse);

        float[][] image_point_2 = new float[][]{
                {(float)sorted_2[0].x,(float)sorted_2[0].y},
                {(float)sorted_2[1].x,(float)sorted_2[1].y},
                {(float)sorted_2[2].x,(float)sorted_2[2].y},
                {(float)sorted_2[3].x,(float)sorted_2[3].y}
        };
        Point[] final2= new Point[4];
        i=0;
        for (float[] p: image_point_2) {
            inverse.mapPoints(p);
            final2[i]= new Point(p[0]+250/2-100,p[1]-250/2);
            i++;
        }

        Mat croped = fourPointTransform_touch(origcopy1,final2);
//        Imgproc.cvtColor(croped,croped,Imgproc.COLOR_RGBA2GRAY);
        detectDocument.enhanceDocument(croped);
//
//        Imgproc.circle(origcopy1, right[2], 20, new Scalar(255, 0, 0, 150), 4);

//        Imgproc.circle(origcopy1, final2[0], 20, new Scalar(0, 0, 255, 150), 4);
//        Imgproc.circle(origcopy1, final2[1], 20, new Scalar(0, 0, 255, 150), 4);
//        Imgproc.circle(origcopy1, final2[2], 20, new Scalar(0, 0, 255, 150), 4);
//        Imgproc.circle(origcopy1, final2[3], 20, new Scalar(0, 0, 255, 150), 4);
//        Bitmap bmpPaper1 = Bitmap.createBitmap(origcopy1.cols(), origcopy1.rows(), Bitmap.Config.ARGB_8888);
//        Utils.matToBitmap(origcopy1, bmpPaper1);
//        Matrix matrix21 = new Matrix();
//        float degrees = 90;//rotation degree
//        matrix21.setRotate(degrees);
//        Bitmap bOutput1 = Bitmap.createBitmap(bmpPaper1, 0, 0, bmpPaper1.getWidth(), bmpPaper1.getHeight(), matrix21, true);
//        test.setImageBitmap(bOutput1);

        send_image(croped);
    }
//    public static Point[] rotateTransform(Point[] points, Point center, int angle_degree){
//        double angle = Math.toRadians(angle_degree);
//        Point[] result = new Point[points.length];
//        for (int i=0; i<points.length; i++) {
//            double x1 = points[i].x - center.x;
//            double y1 = points[i].y - center.y;
//
//            double x2 = x1 * Math.cos(angle) - y1 * Math.sin(angle);
//            double y2 = x1 * Math.sin(angle) + y1 * Math.cos(angle);
//
//            result[i] =new Point( x2 + center.x, y2 + center.y);
//        }
//
//        return result;
//    }
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

    public void  updateDropAction(String CoronerTag, float y_cord, float x_cord, View v ){

//        RelativeLayout coroner_select = marksCoroners.get(CoronerTag);
        View view = v;
        view.setVisibility(View.VISIBLE);
        view.setX(x_cord);
        view.setY(y_cord);
        view.invalidate();
        Point p = new Point(x_cord+(250/2),y_cord+(250/2));
        if(coronersLoaction.containsKey(CoronerTag))
            coronersLoaction.remove(CoronerTag);
        coronersLoaction.put(CoronerTag, p );

    }

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
//        markLayout.setX(x);
//        markLayout.setY(y);
        testLayout.addView(markLayout,2);
        marksCoroners.put(newTag,markLayout);

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
            imageView.setVisibility(View.INVISIBLE); //loading animation
//            imageView.bringToFront();
//            fadingCircle.start();
            Log.d(TAG, "in On Picture Taken, data length: "+ data.length);

            //get the camera parameters
            Camera.Parameters parameters = camera.getParameters();

            //convert the byte[] to Bitmap;
            Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);

            //convert Bitmap to Mat; note the bitmap config ARGB_8888 conversion that
            //allows you to use other image processing methods and still save at the end
            bmp = bmp.copy(Bitmap.Config.ARGB_8888, true);
            Utils.bitmapToMat(bmp, orig);
            Mat origCopy_before_rotate = new Mat();
            orig.copyTo(origCopy_before_rotate);

            detectDocumentHough detect = new detectDocumentHough();

            paper_obj = detect.detect(orig,bmp);

//            paper_obj = detectDocument.findDocument(orig);


            ArrayList<Point[]> Rps1;
            Rps1 = paper_obj.allpoints_original;
            right = new Point[4];
            for (Point[] ps : Rps1)
            {

                Imgproc.line(origCopy_before_rotate,ps[0],ps[1],new Scalar(0, 255, 0, 150), 4);
                Imgproc.line(origCopy_before_rotate,ps[1],ps[2],new Scalar(0, 255, 0, 150), 4);
                Imgproc.line(origCopy_before_rotate,ps[2],ps[3],new Scalar(0, 255, 0, 150), 4);
                Imgproc.line(origCopy_before_rotate,ps[3],ps[0],new Scalar(0, 255, 0, 150), 4);

                right[0]=ps[0];
                right[1]=ps[1];
                right[2]=ps[2];
                right[3]=ps[3];
                Log.i("the right!", "x: "+ ps[0].x +" y: "+ps[0].y);
                Log.i("the right!", "x: "+ ps[1].x +" y: "+ps[1].y);
                Log.i("the right!", "x: "+ ps[2].x +" y: "+ps[2].y);
                Log.i("the right!", "x: "+ ps[3].x +" y: "+ps[3].y);



            }
            Mat origCopy = new Mat();
            Mat rotated = new Mat();
            Core.rotate(orig, rotated, Core.ROTATE_90_CLOCKWISE);
            Core.rotate(origCopy_before_rotate, origCopy, Core.ROTATE_90_CLOCKWISE);

            origcopy1 = new Mat();

            rotated.copyTo(origcopy1);





            Bitmap bmpPaper_UNTOUCH = Bitmap.createBitmap(origCopy.cols(), origCopy.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(origCopy, bmpPaper_UNTOUCH);
//            float degrees1 = 90;//rotation degree
            Matrix matrix1 = new Matrix();
//            matrix1.setRotate(degrees1);
            origin_bitmap = Bitmap.createBitmap(bmpPaper_UNTOUCH, 0, 0, bmpPaper_UNTOUCH.getWidth(), bmpPaper_UNTOUCH.getHeight(), matrix1, true);




            //srcMat.release();
            Bitmap bmpPaper = Bitmap.createBitmap(origCopy.cols(), origCopy.rows(), Bitmap.Config.ARGB_8888);


            Utils.matToBitmap(origCopy, bmpPaper);
//            float degrees2 = 90;//rotation degree
            Matrix matrix2 = new Matrix();
//            matrix2.setRotate(degrees2);
            Bitmap bOutput = Bitmap.createBitmap(bmpPaper, 0, 0, bmpPaper.getWidth(), bmpPaper.getHeight(), matrix2, true);


            PreviewSurfaceView cameraPr = (PreviewSurfaceView) findViewById(R.id.preview_surface);
            cameraPr.setVisibility(View.INVISIBLE);

            testLayout.setVisibility(View.VISIBLE);
            test.setVisibility(View.VISIBLE);
            edit_button.setVisibility(View.VISIBLE);
            ok_button.setVisibility(View.VISIBLE);
            imageView.setVisibility(View.INVISIBLE);
            capture_button.setVisibility(View.INVISIBLE);

            test.setImageBitmap(bOutput);
            Matrix m = test.getImageMatrix();
            RectF drawableRect = new RectF(0, 0, width, height);
            RectF viewRect = new RectF(0, 0, test.getWidth(), test.getHeight());
            m.setRectToRect(drawableRect, viewRect, Matrix.ScaleToFit.CENTER);
            test.setImageMatrix(m);
            test.invalidate();

        }
    };

    private void send_image (Mat paper){
        //srcMat.release();

        Matrix matrix = new Matrix();
        matrix.setRotate(90);
        Bitmap bmpPaper = Bitmap.createBitmap(paper.cols(), paper.rows(), Bitmap.Config.ARGB_8888);

        Utils.matToBitmap(paper, bmpPaper);
        Bitmap bOutput = Bitmap.createBitmap(bmpPaper, 0, 0, bmpPaper.getWidth(), bmpPaper.getHeight(), matrix, true);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bOutput.compress(Bitmap.CompressFormat.JPEG, 70, stream);
        byte[] paperData = stream.toByteArray();

        File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
        if (pictureFile == null) {
            Log.d(TAG, "Error creating media file, check storage permissions");
            return;
        }

        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            fos.write(paperData);
            fos.close();

            String path = pictureFile.getAbsolutePath();
            imageView.setVisibility(View.INVISIBLE);
            fadingCircle.stop();
            Intent returnIntent = new Intent();
            returnIntent.putExtra("sheet",path);
            setResult(Activity.RESULT_OK,returnIntent);
            finish();

//            if(mode.equals("newTemplate")){
//                returnIntent.putExtra("sheet",path);
//                setResult(Activity.RESULT_OK,returnIntent);
//                finish();
//                nextIntent = new Intent(getApplicationContext(), NewTemplateActivity.class);
//                nextIntent.putExtra("sheet", path);
//            }else if (mode.equals("oneByOne")){
//                returnIntent.putExtra("sheet",path);
//                setResult(Activity.RESULT_OK,returnIntent);
//                finish();
//                nextIntent = new Intent(getApplicationContext(), ProblematicQuestionsActivity.class);
//                nextIntent.putExtra("sheet", path);
//            }


//            startActivity(nextIntent);

        } catch (FileNotFoundException e) {
            Log.d(TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d(TAG, "Error accessing file: " + e.getMessage());
        }

    }
    private static File getOutputMediaFile(int type){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "Check4U");
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
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
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
