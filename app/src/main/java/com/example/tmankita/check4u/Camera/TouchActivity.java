package com.example.tmankita.check4u.Camera;


import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.os.Environment;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.example.tmankita.check4u.NewTemplateActivity;
import com.example.tmankita.check4u.R;
import com.example.tmankita.check4u.detectDocument;
import com.github.ybq.android.spinkit.sprite.Sprite;
import com.github.ybq.android.spinkit.style.FadingCircle;


import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;

public class TouchActivity extends AppCompatActivity {
    private  static final String TAG= "TouchActivity";
    private PreviewSurfaceView camView;
    private CameraPreviewFocus cameraPreview;
    private DrawingView drawingView;
    private Sprite fadingCircle;
    private ImageView imageView;

    private int previewWidth = 1280;
    private int previewHeight = 720;

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        setContentView(R.layout.activity_touch);

        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }

        imageView = findViewById(R.id.imageView);
        fadingCircle = new FadingCircle();
        fadingCircle.setColor(Color.WHITE);
        imageView.setImageDrawable(fadingCircle);
        imageView.setVisibility(View.INVISIBLE);


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
            imageView.setVisibility(View.VISIBLE);
            imageView.bringToFront();
            fadingCircle.start();

            //get the camera parameters
            Camera.Parameters parameters = camera.getParameters();
            int width = parameters.getPreviewSize().width;
            int height = parameters.getPreviewSize().height;

            //convert the byte[] to Bitmap;
            Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);

            //convert Bitmap to Mat; note the bitmap config ARGB_8888 conversion that
            //allows you to use other image processing methods and still save at the end
            Mat orig = new Mat();
            bmp = bmp.copy(Bitmap.Config.ARGB_8888, true);
            Utils.bitmapToMat(bmp, orig);
//            ArrayList<Point[]> Rps1;
//            Rps1 = detectDocument.findDocument(orig);
            Mat paper = detectDocument.findDocument(orig);

//            for (Point[] ps : Rps1)
//                {
//                    Imgproc.line(orig,ps[0],ps[1],new Scalar(0, 255, 0, 150), 4);
//                    Imgproc.line(orig,ps[1],ps[2],new Scalar(0, 255, 0, 150), 4);
//                    Imgproc.line(orig,ps[2],ps[3],new Scalar(0, 255, 0, 150), 4);
//                    Imgproc.line(orig,ps[3],ps[0],new Scalar(0, 255, 0, 150), 4);
//
//
//                }

//            Imgproc.circle(orig, ps[0], 10, new Scalar(0, 255, 0, 150), 4);
//            Imgproc.circle(orig, ps[1], 10, new Scalar(0, 255, 0, 150), 4);
//            Imgproc.circle(orig, ps[2], 10, new Scalar(0, 255, 0, 150), 4);
//            Imgproc.circle(orig, ps[3], 10, new Scalar(0, 255, 0, 150), 4);




            //srcMat.release();
            Bitmap bmpPaper = Bitmap.createBitmap(paper.cols(), paper.rows(), Bitmap.Config.ARGB_8888);


            Utils.matToBitmap(paper, bmpPaper);
            float degrees = 90;//rotation degree
            Matrix matrix = new Matrix();
            matrix.setRotate(degrees);
            Bitmap bOutput = Bitmap.createBitmap(bmpPaper, 0, 0, bmpPaper.getWidth(), bmpPaper.getHeight(), matrix, true);


//            FrameLayout cameraPr = (FrameLayout) findViewById(R.id.preview_surface);
//            cameraPr.setVisibility(View.INVISIBLE);
//            ImageView image = (ImageView) findViewById(R.id.test);
//            image.setVisibility(View.VISIBLE);
//
//            image.setImageBitmap(bOutput);

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bOutput.compress(Bitmap.CompressFormat.JPEG, 70, stream);
            byte[] paperData = stream.toByteArray();

            File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
            if (pictureFile == null){
                Log.d(TAG, "Error creating media file, check storage permissions");
                return;
            }

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(paperData);
                fos.close();

                String path = pictureFile.getAbsolutePath();

                Intent CreateTemplate = new Intent(getApplicationContext(), NewTemplateActivity.class);

                CreateTemplate.putExtra("sheet", path);
                imageView.setVisibility(View.INVISIBLE);
                fadingCircle.stop();
                startActivity(CreateTemplate);

            } catch (FileNotFoundException e) {
                Log.d(TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(TAG, "Error accessing file: " + e.getMessage());
            }
        }
    };


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
