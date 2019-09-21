package com.example.tmankita.check4u.Activities;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Process;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.artifex.mupdf.fitz.*;
import com.artifex.mupdf.fitz.android.AndroidDrawDevice;
import com.example.tmankita.check4u.Database.Answer;
import com.example.tmankita.check4u.Database.StudentDataBase;
import com.example.tmankita.check4u.Database.Template;
import com.example.tmankita.check4u.Detectors.alignToTemplate;
import com.example.tmankita.check4u.Detectors.detectDocument;
import com.example.tmankita.check4u.Dropbox.UserDropBoxActivity;
import com.example.tmankita.check4u.R;
import com.example.tmankita.check4u.Utils.CSVWriter;
import com.example.tmankita.check4u.Utils.ZipManager;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import static android.database.sqlite.SQLiteDatabase.OPEN_READONLY;
import static android.os.Process.THREAD_PRIORITY_DEFAULT;
import static android.os.Process.THREAD_PRIORITY_MORE_FAVORABLE;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;
import static com.example.tmankita.check4u.Utils.PDFUtils.getRealPath;

public class oneByOneOrSeries extends AppCompatActivity {

    private  static final String TAG= "oneOrSeries_Activity";
    private static final int AFTER_ALIGNMENT_CONTINUES_REQUEST_CODE = 1;
    private static final int PROBLEMATICֹֹ_QUESTIONSֹ_CONTINUES = 2;
    private static final int FILE_PICKER__CONTINUES_REQUEST_CODE = 3;

    //image - student test
    private Mat paper;
    private Mat barcodePaper;
    private Mat paperMarks;
    private String currentImagePath;
    private String currentImageMarks;
    private String currentImageBarcode;

    //DB
    private Answer[][] allanswers;
    private double score;
    private int numberOfQuestions;
    private int numberOfAnswers;
    private StudentDataBase students_db;
    private int[] studentAnswers;
    private int[] correctAnswers;
    private String dbPath;
    private String templatePath;
    private String barcodePath;

    //Views
    private TableLayout need_to_continue;
    private TableLayout need_to_continue1;
    private TableLayout Layout_begin_series;
    private Button series;
    private Button oneByOne;
//    private ImageView test_align;
//    private Button ok_align_button;
//    private Button realign_button;
//    private RelativeLayout test_align_Layout;
    private TextView info_examsCounter;
    private TextView info_lastGrade;
    private TextView info_average;
    private TextView wait_dialog;
    private Button finish_check_button;

    //To counters
    private int totalExamsThatProcessedUntilNow;
    private double lastGrade;
    private double averageUntilNow;

    //helpers
    private float realA4Width = 4960;
    private float realA4Height = 7016;
    private File testsDir;
    private int numberOfTest;
    private int countSeries;
    private Intent dataFilePickerSeries;

    // CSV
    private File file;

    // for tests
    private Mat imageForTest;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    paper = new Mat();
                    barcodePaper = new Mat();
                    imageForTest = new Mat();
                    paperMarks = new Mat();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onebyone_or_series);

        if (!OpenCVLoader.initDebug()) {
            Log.d("LOAD OPENCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d("LOAD OPENCV", "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        final Context c = this;


        Bundle extras = getIntent().getExtras();

        //Views
        need_to_continue        = (TableLayout) findViewById(R.id.Layout_if_need_to_continue);
        need_to_continue1        = (TableLayout) findViewById(R.id.Layout_if_need_to_continue1);
        Layout_begin_series     = (TableLayout) findViewById(R.id.Layout_begin_series);
        info_examsCounter       = (TextView) findViewById(R.id.info_count_tests_num);
        info_lastGrade          = (TextView) findViewById(R.id.info_last_grade_num);
        info_average            = (TextView) findViewById(R.id.info_average_num);
        series                  = (Button) findViewById(R.id.series_button);
        oneByOne                = (Button) findViewById(R.id.oneByOne_button);
//        test_align              = (ImageView) findViewById(R.id.test_align);
//        ok_align_button         = (Button) findViewById(R.id.ok_align);
//        realign_button          = (Button) findViewById(R.id.realign);
//        test_align_Layout       = (RelativeLayout) findViewById(R.id.test_align_layout);
        wait_dialog             = (TextView) findViewById(R.id.wait_dialog);
        finish_check_button     = (Button) findViewById(R.id.finish_check);

        wait_dialog.setVisibility(View.INVISIBLE);

        //Variables for status of checking tests
        totalExamsThatProcessedUntilNow = 0;
        lastGrade = 0;
        averageUntilNow = 0;

        dbPath = extras.getString("dbPath");
        final ProgressDialog pd = new ProgressDialog(c);

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();


                pd.setTitle("Prepare template data base");
                pd.setMessage("Please wait");
                pd.setCancelable(false);
                pd.setIndeterminate(true);
                pd.show();
            }

            @Override
            protected Void doInBackground(final Void... params) {
                Process.setThreadPriority(THREAD_PRIORITY_DEFAULT + THREAD_PRIORITY_MORE_FAVORABLE);
                // something you know that will take a few seconds
                Log.e("CHECKL4U", "DO IN BACKGROUND");
                String targetDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath() + "/Check4U_DB/UNZIP/" + generateDirName() + "/";
                ZipManager zipManager = new ZipManager();
                zipManager.unzip(dbPath, targetDir);

                String templateName = null;
                String barcodeName = null;
                String dbName = null;
                File yourDir = new File(targetDir);
                for (File f : yourDir.listFiles()) {
                    String name  = f.getName();
                    if(f.isFile() && ((name.charAt(name.length() - 5)) == 'B') && f.getName().toLowerCase().endsWith("jpg"))
                        barcodeName = f.getName();
                    else if (f.isFile() && ((name.charAt(name.length() - 5)) == 'A') && f.getName().toLowerCase().endsWith("jpg"))
                        templateName = f.getName();
                    else if (f.isFile() && f.getName().toLowerCase().endsWith("db"))
                        dbName = f.getName();
                }


                templatePath = targetDir + "/" + templateName;
                barcodePath = targetDir + "/" + barcodeName;

                SQLiteDatabase db_template = SQLiteDatabase.openDatabase(targetDir + "/" + dbName, null, OPEN_READONLY);


                allanswers = extractAllDbTemplateInfo(db_template);
                db_template.close();

                String filePath = StudentDataBase.DB_FILEPATH;
                File file = new File(filePath);
                if (file.exists())
                    file.delete();
                students_db = new StudentDataBase(getApplicationContext(), numberOfQuestions);
                return null;
            }

            @Override
            protected void onPostExecute(final Void result) {
                pd.dismiss();

            }
        }.execute();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        /**
         * Continues from alignment
         */
        if (requestCode == AFTER_ALIGNMENT_CONTINUES_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                try {
                    String[] imagesPathes = data.getStringArrayExtra("finalResult");
                    currentImageBarcode = imagesPathes[0];
                    currentImagePath    =  imagesPathes[2];
                    currentImageMarks   = imagesPathes[3];

                    //update the status variable
                    totalExamsThatProcessedUntilNow++;
                    //insert the student to DB
                    boolean res = insertStudent(null ,"OneByOne");
                    if(!res) {
                        totalExamsThatProcessedUntilNow--;
                        Toast.makeText(this, "Faild: not success to check this test, because there is no barcode in this test", Toast.LENGTH_LONG).show();
                        Log.e("BARCODE", "Faild: not success to check this test, because there is no barcode in this test");
                    }

//                    Mat align = new Mat();
//                    BitmapFactory.Options options = new BitmapFactory.Options();
//                    options.inPreferredConfig = Bitmap.Config.ARGB_8888;
//
//                    Bitmap bitmap = BitmapFactory.decodeFile(currentImagePath, options);
//                    Bitmap bmpAlign = bitmap.copy(Bitmap.Config.ARGB_8888, true);
//                    Utils.bitmapToMat(bmpAlign, align);
//
//                    Bitmap bmpPaper = Bitmap.createBitmap(align.cols(), align.rows(), Bitmap.Config.ARGB_8888);
//                    Utils.matToBitmap(align, bmpPaper);
//                    Matrix matrix2 = new Matrix();
//                    Bitmap bOutput = Bitmap.createBitmap(bmpPaper, 0, 0, bmpPaper.getWidth(), bmpPaper.getHeight(), matrix2, true);
//
//                    Display display = getWindowManager().getDefaultDisplay();
//                    android.graphics.Point size = new android.graphics.Point();
//                    display.getSize(size);
//                    int width = size.x;
//                    int height = size.y;
//
//                    test_align.setImageBitmap(bOutput);
//                    Matrix M = test_align.getImageMatrix();
//                    RectF drawableRect = new RectF(0, 0, align.cols(), align.rows());
//                    RectF viewRect = new RectF(0, 0, width, height);
//                    M.setRectToRect(drawableRect, viewRect, Matrix.ScaleToFit.CENTER);
//                    test_align.setImageMatrix(M);
//                    test_align.invalidate();
//
//                    test_align_Layout.setVisibility(View.VISIBLE);
//                    test_align.setVisibility(View.VISIBLE);
//                    realign_button.setVisibility(View.VISIBLE);
//                    ok_align_button.setVisibility(View.VISIBLE);
                } catch (Exception e) {
                    Toast.makeText(this, "The alignment procces go wrong, try again", Toast.LENGTH_LONG).show();
                    File old_pic = new File(currentImagePath);
                    File old_barcode = new File(currentImageBarcode);
                    if (old_pic.exists())
                        old_pic.delete();
                    if (old_barcode.exists())
                        old_barcode.delete();
                    series.setVisibility(View.INVISIBLE);
                    oneByOne.setVisibility(View.INVISIBLE);
//                    test_align_Layout.setVisibility(View.INVISIBLE);
//                    test_align.setVisibility(View.INVISIBLE);
//                    realign_button.setVisibility(View.INVISIBLE);
//                    ok_align_button.setVisibility(View.INVISIBLE);
                    Intent takePicture = new Intent(getApplicationContext(), TouchActivity.class);
                    takePicture.putExtra("templatePath",templatePath);
                    takePicture.putExtra("barcodeTemplatePath",barcodePath);
                    takePicture.putExtra("status","Barcode");
                    takePicture.putExtra("caller","oneByOne");
                    startActivityForResult(takePicture, 1);
                }
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(this, "The alignment procces go wrong, try again", Toast.LENGTH_LONG).show();
                File old_pic = new File(currentImagePath);
                File old_barcode = new File(currentImageBarcode);
                if (old_pic.exists())
                    old_pic.delete();
                if (old_barcode.exists())
                    old_barcode.delete();
                series.setVisibility(View.INVISIBLE);
                oneByOne.setVisibility(View.INVISIBLE);
//                test_align_Layout.setVisibility(View.INVISIBLE);
//                test_align.setVisibility(View.INVISIBLE);
//                realign_button.setVisibility(View.INVISIBLE);
//                ok_align_button.setVisibility(View.INVISIBLE);
                Intent takePicture = new Intent(getApplicationContext(), TouchActivity.class);
                takePicture.putExtra("templatePath",templatePath);
                takePicture.putExtra("barcodeTemplatePath",barcodePath);
                takePicture.putExtra("status","Barcode");
                takePicture.putExtra("caller", "oneByOne");
                startActivityForResult(takePicture, 1);
            }
        }
        /**
         * Continues from problematic questions
         */
        else if (requestCode == PROBLEMATICֹֹ_QUESTIONSֹ_CONTINUES) {
            int i;
            if (resultCode == Activity.RESULT_OK) {
                int currId = data.getIntExtra("id",-1);
                int[] toFix = data.getIntArrayExtra("toFix");
                for (i = 1; i < toFix.length; i++) {
                    if(toFix[i] == -1){
                        studentAnswers[i] = 0;
                    }
                    else if (toFix[i] != 0) {
                        studentAnswers[i] = toFix[i];
                    }

                }
                // make binary array for questions -> 1 - right  , 0 - wrong
                int[] binaryCorrectFlag = new int[numberOfQuestions + 1];
                for (i = 1; i < numberOfQuestions + 1; i++) {
                    if (correctAnswers[i] == studentAnswers[i])
                        binaryCorrectFlag[i] = 1;
                    else
                        binaryCorrectFlag[i] = 0;
                }
                double currGrade = students_db.insertRaw(currId, studentAnswers, binaryCorrectFlag, (int) score);
                countSeries++;

                if(currGrade!= -1)
                    lastGrade = currGrade;
                else
                    totalExamsThatProcessedUntilNow--;

                String callee = data.getStringExtra("callee");

                if(callee.equals("OneByOne")){

                    checkIfFinalSheetOrContinue();
                }
                else if(callee.equals("Series")){
//                    Mat template = new Mat();
//                    BitmapFactory.Options options = new BitmapFactory.Options();
//                    options.inPreferredConfig = Bitmap.Config.ARGB_8888;
//                    Bitmap bmp1 = BitmapFactory.decodeFile(templatePath, options);
//                    Utils.bitmapToMat(bmp1, template);
//
//                    while(iterTests.hasNext()) {
//                        currentImagePath = iterTests.next();
//                        Mat img = new Mat();
//                        Bitmap bmp = BitmapFactory.decodeFile(currentImagePath, options);
//                        Utils.bitmapToMat(bmp, img);
//                        alignToTemplate align_to_template = new alignToTemplate();
//                        Mat align = align_to_template.align1(img, template, bmp,"Series"); //series_align
//                        if(align.empty()){
////                        write to the log each test we skip
//                            continue;
//                        }
//                        currentImagePath = saveJpeg(align,testsDir.getAbsolutePath());
//
//                        totalExamsThatProcessedUntilNow++;
//                        boolean res = insertStudent(currentImagePath, "Series");
//                        if (!res) {
//                            //write to log file which test not checked
//                            totalExamsThatProcessedUntilNow--;
//                            Toast.makeText(this, "Faild: not success to check this test, because there is no barcode in this test", Toast.LENGTH_LONG).show();
//                            Log.e("BARCODE", "Faild: not success to check this test, because there is no barcode in this test");
//                        }
//                    }
                    finishSeries();

                }



            }
            else if (resultCode == Activity.RESULT_CANCELED) {
            }
            /**
             * Continues from FILE PICKER multiply series mode
             */
        }
        /**
         * Continues after pick the pdf with all tests
         */
        else if (requestCode == FILE_PICKER__CONTINUES_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                dataFilePickerSeries = data;
                oneByOne.setVisibility(View.INVISIBLE);
                series.setVisibility(View.INVISIBLE);
                Layout_begin_series.setVisibility(View.VISIBLE);

            }
        }

    }

    /**
     * function start "ONE-BY-0NE" option.
     * @param view is "ONE-BY-0NE" button
     * @return None
     */
    public void oneByOne (View view){
        series.setVisibility(View.INVISIBLE);
        oneByOne.setVisibility(View.INVISIBLE);
        Intent takePicture = new Intent(getApplicationContext(), TouchActivity.class);
        takePicture.putExtra("templatePath",templatePath);
        takePicture.putExtra("barcodeTemplatePath",barcodePath);
        takePicture.putExtra("status","Barcode");
        takePicture.putExtra("caller","oneByOne");
        startActivityForResult(takePicture,1);
    }

    /**
     * function start "Butch" option.
     * @param view is "Butch" button
     * @return None
     */
    public void series (View view){
        //picker files view - choose all the tests
        countSeries = 0;
        series.setVisibility(View.INVISIBLE);
        oneByOne.setVisibility(View.INVISIBLE);


        Uri uri = Uri.parse(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath());
        Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT,uri);
        chooseFile.setType("application/pdf");
        chooseFile = Intent.createChooser(chooseFile, "Choose a zip file");
        startActivityForResult(chooseFile, FILE_PICKER__CONTINUES_REQUEST_CODE);




    }

    /**
     * start to check another test in "ONE-BY-0NE" option.
     * @param view is the "yes" button int the statistics dialog
     * @return
     */
    public void continue_check(View view){
        Intent takePicture = new Intent(getApplicationContext(), TouchActivity.class);
        takePicture.putExtra("templatePath",templatePath);
        takePicture.putExtra("barcodeTemplatePath",barcodePath);
        takePicture.putExtra("status","Barcode");
        takePicture.putExtra("caller","oneByOne");
        takePicture.putExtra("caller","oneByOne");
        startActivityForResult(takePicture,AFTER_ALIGNMENT_CONTINUES_REQUEST_CODE);
    }

    /**
     * finish to check in "ONE-BY-0NE" option.
     * @param view is the "finish" button
     * @return
     */
    public void finish_check(View view){
        createCSV();
    }

    /**
     * start to check another test in "Butch" option.
     * @param view is the "yes" button in the "Butch" display
     * @return
     */
    public void start_check_series(View view){
        Layout_begin_series.setVisibility(View.INVISIBLE);
        wait_dialog.setVisibility(View.VISIBLE);
        wait_dialog.bringToFront();

        wait_dialog.post(new Runnable() {
            @Override
            public void run() {
//
                File StorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Check4U_DB");
                File SeriesDir = new File(StorageDir.getPath(), "Series");
                if (!SeriesDir.exists()) {
                    if (!SeriesDir.mkdirs()) {
                        Log.d("Check4U", "failed to create directory SeriesDir");
                    }
                }
                testsDir = new File(SeriesDir.getPath(), "tests_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()));
                if (!testsDir.exists()) {
                    if (!testsDir.mkdirs()) {
                        Log.d("Check4U", "failed to create directory testsDir");
                    }
                }


                final ArrayList<Mat> tests = new ArrayList<>();
                Uri uri = dataFilePickerSeries.getData();
                currentImagePath = getRealPathFromURI(uri);


                Document doc = Document.openDocument(currentImagePath);
                int pageCount = doc.countPages();
                int pageIndex=0;

                com.artifex.mupdf.fitz.Matrix ctm;
//                Link[] links;
//                Quad[] hits;



//                links = page.getLinks();
//                if (links != null)
//                    for (Link link : links)
//                        link.bounds.transform(ctm);
//                if (zoom != 1)
//                    ctm.scale(zoom);

//                File targetDir = new File(testsDir,"PDF2JPEG");
//                if (!targetDir.exists()) {
//                    if (!targetDir.mkdirs()) {
//                        Log.d("Check4U", "failed to create directory targetDir");
//                    }
//                }

                Mat template = new Mat();
                Mat barcodeTemplate = new Mat();
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                Bitmap bmp1 = BitmapFactory.decodeFile(templatePath, options);
                Bitmap bmp2 = BitmapFactory.decodeFile(barcodePath, options);
                Utils.bitmapToMat(bmp1, template);
                Utils.bitmapToMat(bmp2, barcodeTemplate);

                alignToTemplate align_to_template = new alignToTemplate();
                numberOfTest = (pageCount)/2;
                while(pageIndex<pageCount){
                    Page page = doc.loadPage(pageIndex);
                    ctm = AndroidDrawDevice.fitPage(page, (int)template.size().width, (int)template.size().height);
                    Bitmap bitmap = AndroidDrawDevice.drawPage(page,ctm);
                    Mat image = new Mat();
                    Utils.bitmapToMat(bitmap, image);
                    if(pageIndex % 2 == 0){
                        align_to_template.align1(image, barcodeTemplate, null,"Series");
                        barcodePaper = align_to_template.align;
                        pageIndex++;
                        continue;
                    }
                    else{
                        align_to_template.align1(image, template, null,"Series");
                    }

                    if(align_to_template.align.empty() || barcodePaper.empty()){
                        countSeries++;
                        continue;
                    }
                    totalExamsThatProcessedUntilNow++;
                    boolean res = insertStudent(align_to_template ,"Series");
                    if (!res) {
                        //write to the log each test we skip
                        totalExamsThatProcessedUntilNow--;
//                        Toast.makeText(this, "Faild: not success to check this test, because there is no barcode in this test", Toast.LENGTH_LONG).show();
                        Log.e("BARCODE", "Faild: not success to check this test, because there is no barcode in this test");
                    }
                    pageIndex++;
                    Log.d("series", "test: " + countSeries + " finish");

                }

                //for each test:
                //      align to the template- no option for realign
                //      check the align image and insert it to the student db
                //finish_check

                finishSeries();

            }
        });



    }

    /**
     * function that get the real path from <uri>.
     * @param uri is uri of file
     * @return path in the disk
     */
    public String getRealPathFromURI( Uri uri) {
        String fullPath;
        try{
        fullPath = getRealPath(getApplicationContext(), uri);
            return fullPath;
        }
        catch(Exception e){
            Log.d(TAG, "pdf: " + e.getMessage());
            return "";
        }

    }

    /**
     * function that create the csv file if all the tests are checked.
     * @return None
     */
    public void finishSeries(){
        if(countSeries == numberOfTest)
            createCSV();
    }

    /**
     * crete jpg file of <img>  in <dirpath> with the name <name> on disk.
     * @param img
     * @param dirPath
     * @param name
     * @return path of the jpg file
     */
    private String saveJpeg(Mat img, String dirPath, String name) {
        String path="";
        Matrix matrix = new Matrix();
        Bitmap bmpPaper = Bitmap.createBitmap(img.cols(), img.rows(), Bitmap.Config.ARGB_8888);

        Utils.matToBitmap(img, bmpPaper);
        Bitmap bOutput = Bitmap.createBitmap(bmpPaper, 0, 0, bmpPaper.getWidth(), bmpPaper.getHeight(), matrix, true);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bOutput.compress(Bitmap.CompressFormat.JPEG, 70, stream);
        byte[] paperData = stream.toByteArray();

        File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE,dirPath,name);
        if (pictureFile == null) {
            Log.d(TAG, "Error creating media file, check storage permissions");
            return "";
        }

        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            fos.write(paperData);
            fos.close();

            path = pictureFile.getAbsolutePath();


        } catch (FileNotFoundException e) {
            Log.d(TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d(TAG, "Error accessing file: " + e.getMessage());
        }
        return path;
    }


    private static File getOutputMediaFile(int type, String dirPath, String name){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        String Path = dirPath;          //Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath() + "/Check4U_DB/DCIM";
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
        //String timeStamp = //new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                     name + ".jpg");
        } else if(type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    name + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }


    private static String generateDirName(){
        // Create a Directory name
        String timeStamp ="db_"+new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

        return timeStamp;
    }


    public void createCSV(){
        File exportDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Check4U_DB");
        File csvDir = new File(exportDir.getPath(),"CSV");

        file = new File(csvDir, "Check4U_StudentDataBase"+new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date())+".csv");
        try
        {
            file.createNewFile();
            CSVWriter csvWrite = new CSVWriter(new FileWriter(file));
            SQLiteDatabase db = students_db.getReadableDatabase();
            Cursor curCSV = db.rawQuery("SELECT * FROM students_table",null);
            csvWrite.writeNext(curCSV.getColumnNames());
            String[] rowStr = new String[2+numberOfQuestions];
            while(curCSV.moveToNext())
            {
                for(int i=0; i<numberOfQuestions+2 ;i++){
                    rowStr[i] = String.valueOf(curCSV.getInt(i));
                }

                csvWrite.writeNext(rowStr);
            }
            csvWrite.close();
            curCSV.close();
            oneByOne.setVisibility(View.INVISIBLE);
            series.setVisibility(View.INVISIBLE);
            need_to_continue.setVisibility(View.INVISIBLE);
            need_to_continue1.setVisibility(View.INVISIBLE);
            finish_check_button.setVisibility(View.INVISIBLE);

            Intent uploadCSV = new Intent(getApplicationContext(), UserDropBoxActivity.class);
            Bundle bundle = new Bundle();
            bundle.putString("caller", "oneByOne");
            bundle.putString("CSV", file.getPath());
            uploadCSV.putExtras(bundle);
            startActivity(uploadCSV);

        }
        catch(Exception sqlEx)
        {
            Log.e("SQL_to_CSV", sqlEx.getMessage(), sqlEx);
        }
    }

    /**
     *
     * @param sheet is a image with barcode that need to detect
     * @return the number that coded in the barcode
     */
    private int detectBarcode (Mat sheet){
        Answer barcodeInfo = allanswers[0][0];

        Matrix scaleToImageSize = new Matrix();
//        scaleToImageSize.postScale((sheet.cols()/realA4Width),sheet.rows()/(realA4Height));
        RectF drawableRect = new RectF(0, 0, realA4Width, realA4Height);
        RectF viewRect = new RectF(0, 0, sheet.cols(), sheet.rows());
        boolean success = scaleToImageSize.setRectToRect(drawableRect, viewRect, Matrix.ScaleToFit.FILL);

        if(!success){Log.i("scaling","not success!!!!!");}

        float[][] points = new float[][]{
                {barcodeInfo.getLocationX(),barcodeInfo.getLocationY()},
                {(barcodeInfo.getLocationX()+barcodeInfo.getWidth()),(barcodeInfo.getLocationY())},
                {(barcodeInfo.getLocationX()),(barcodeInfo.getLocationY()+barcodeInfo.getHeight())},
                {(barcodeInfo.getLocationX()+barcodeInfo.getWidth()),(barcodeInfo.getLocationY()+barcodeInfo.getHeight())}
        };
        Point[] ps = new Point[4];
        for (int i = 0; i < 4; i++) {
            scaleToImageSize.mapPoints(points[i]);
            ps[i] = new Point(points[i][0],points[i][1]);
        }


        Point[] sorted_2 = detectDocument.sortPoints(ps);
        Mat barcodeCropped = TouchActivity.fourPointTransform_touch(sheet,sorted_2);

        Imgproc.line(sheet,sorted_2[0],sorted_2[1],new Scalar(0, 255, 0, 150), 4);
        Imgproc.line(sheet,sorted_2[1],sorted_2[2],new Scalar(0, 255, 0, 150), 4);
        Imgproc.line(sheet,sorted_2[2],sorted_2[3],new Scalar(0, 255, 0, 150), 4);
        Imgproc.line(sheet,sorted_2[3],sorted_2[0],new Scalar(0, 255, 0, 150), 4);
//
        Bitmap bmpBarcodeForTest = Bitmap.createBitmap(sheet.cols(), sheet.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(sheet, bmpBarcodeForTest);
//
        Bitmap bmpBarcode = Bitmap.createBitmap(barcodeCropped.cols(), barcodeCropped.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(barcodeCropped, bmpBarcode);

        int id = 0;
        String  LOG_TAG = "BARCODE";
        Frame frame = new Frame.Builder().setBitmap(bmpBarcode).build();
        BarcodeDetector barcodeDetector = new BarcodeDetector.Builder(getApplicationContext())
                .setBarcodeFormats(Barcode.ALL_FORMATS)
                .build();

        if(barcodeDetector.isOperational()){
            SparseArray<Barcode> sparseArray = barcodeDetector.detect(frame);
            if(sparseArray != null && sparseArray.size() > 0){
                for (int i = 0; i < sparseArray.size(); i++){
                    try{
                        Log.d(LOG_TAG, "Value_" + sparseArray.valueAt(i).rawValue + "_" + sparseArray.valueAt(i).displayValue);
                        id = Integer.parseInt(sparseArray.valueAt(i).rawValue);
                    }catch(Exception e){
                        if(i < sparseArray.size())
                            continue;
                        barcodeCropped.release();
//                        Toast.makeText(this,"Didn't catch the barcode, take the picture again",LENGTH_SHORT).show();
                        File old_pic = new File(currentImagePath);
//                        File old_barcode = new File(currentImageBarcode);
                        File old_pic_marks = new File(currentImageMarks);
                        if(old_pic.exists())
                            old_pic.delete();
//                        if(old_barcode.exists())
//                            old_barcode.delete();
                        if(old_pic_marks.exists())
                            old_pic_marks.delete();
                        series.setVisibility(View.INVISIBLE);
                        oneByOne.setVisibility(View.INVISIBLE);
//                        test_align_Layout.setVisibility(View.INVISIBLE);
//                        test_align.setVisibility(View.INVISIBLE);
//                        realign_button.setVisibility(View.INVISIBLE);
//                        ok_align_button.setVisibility(View.INVISIBLE);
                        Intent takePicture = new Intent(getApplicationContext(), TouchActivity.class);
                        takePicture.putExtra("templatePath",templatePath);
                        takePicture.putExtra("barcodeTemplatePath",barcodePath);
                        takePicture.putExtra("caller","oneByOne");
                        takePicture.putExtra("status","Barcode");
                        startActivityForResult(takePicture,1);
                    }
                    break;

                }
            }else {
                barcodeCropped.release();
                Toast.makeText(this, "The Barcode is not clear, take the picture again ",Toast.LENGTH_LONG).show();
                Log.e(LOG_TAG,"SparseArray null or empty");
                File old_pic = new File(currentImagePath);
                File old_pic_marks = new File(currentImageMarks);
//                File old_barcode = new File(currentImageBarcode);
                if(old_pic.exists())
                    old_pic.delete();
//                if(old_barcode.exists())
//                    old_barcode.delete();
                if(old_pic_marks.exists())
                    old_pic_marks.delete();
                series.setVisibility(View.INVISIBLE);
                oneByOne.setVisibility(View.INVISIBLE);
//                test_align_Layout.setVisibility(View.INVISIBLE);
//                test_align.setVisibility(View.INVISIBLE);
//                realign_button.setVisibility(View.INVISIBLE);
//                ok_align_button.setVisibility(View.INVISIBLE);
                Intent takePicture = new Intent(getApplicationContext(), TouchActivity.class);
                takePicture.putExtra("templatePath",templatePath);
                takePicture.putExtra("barcodeTemplatePath",barcodePath);
                takePicture.putExtra("caller", "oneByOne");
                takePicture.putExtra("status","Barcode");
                startActivityForResult(takePicture,1);

            }

        }else{
            Log.e(LOG_TAG, "Detector dependencies are not yet downloaded");
        }
    barcodeCropped.release();
    return id;
    }

    /**
     * function that insert a row in student db - contain all the answers that the student chose and his grade.
     * @param align_to_template is alignToTemplate Object that contain the align image and the strong marks image.
     * @param callee is who option calls the  insertStudent function
     * @return true if success to insert student to student db.
     */
    private boolean insertStudent (alignToTemplate align_to_template, String callee) {
        double threshold=0;
        if(callee.equals("OneByOne")){
//            threshold=16;
            //import the image from path
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap bitmap = BitmapFactory.decodeFile(currentImagePath, options);
            Bitmap bitmap1 = BitmapFactory.decodeFile(currentImageMarks, options);
            Bitmap bitmap2 = BitmapFactory.decodeFile(currentImageBarcode, options);
            // to sum the black level in matrix
            Bitmap bmpAnswers = bitmap.copy(Bitmap.Config.ARGB_8888, true);
            Bitmap bmpBarcode = bitmap2.copy(Bitmap.Config.ARGB_8888, true);
            Bitmap bmpStrongMarks = bitmap1.copy(Bitmap.Config.ARGB_8888, true);
            Utils.bitmapToMat(bmpAnswers, paper);
            Utils.bitmapToMat(bmpBarcode, barcodePaper);
            Utils.bitmapToMat(bmpStrongMarks, paperMarks);
            paper.copyTo(imageForTest);
        }
        else if(callee.equals("Series")){
//            threshold=50;
            align_to_template.align.copyTo(paper);
            align_to_template.strongMarks.copyTo(paperMarks);
            paper.copyTo(imageForTest);
        }

        // calculate barcode -> student id
        int id = detectBarcode(barcodePaper);
        if(id == 0) { return false; }
        else {
            int i;
            int j;
            int correct = 0;
            correctAnswers = new int[numberOfQuestions+1];
            int[][] sumOfBlacks = new int[numberOfQuestions+1][numberOfAnswers];
            ArrayList<Answer> AnotherAnswersThatChoosed = new ArrayList<>();
            boolean flagNeedToCorrectSomeAnswers = false;
            for (i = 1; i < allanswers.length; i++) {
                Answer[] question = allanswers[i];
                for (j = 0; j < question.length; j++){
                    correct = question[j].getFlagCorrect();
                    sumOfBlacks[i][j] = calculateBlackLevel(paperMarks, question[j]);
                    if (correct == 1)
                        correctAnswers[i] = j + 1;
                }
            }

        Bitmap bmpForTest = Bitmap.createBitmap(imageForTest.cols(), imageForTest.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(imageForTest, bmpForTest);

            //find question with two answers marked or more
            //get all of them -> show the user all the marks that marked let him to choose the right one
            studentAnswers = new int[numberOfQuestions+1];
            int numberOfAnswerThatChoosed = 0;
            for (i = 1; i < sumOfBlacks.length; i++) {
                int minBlack = Integer.MAX_VALUE;
                int maxBlack = 0;
                for (j = 0; j < sumOfBlacks[i].length; j++) {
                    if(maxBlack <= sumOfBlacks[i][j]){
                        maxBlack = sumOfBlacks[i][j];
                    }
                    if (minBlack >= sumOfBlacks[i][j]) {
                        minBlack = sumOfBlacks[i][j];
                        numberOfAnswerThatChoosed = j + 1;
                    }
                }
                double epsilon=0;
                boolean needToAddtheChoosedOne = false;
                if(callee.equals("OneByOne")){
//                    threshold = (maxBlack-minBlack)*0.5;
//                    epsilon = minBlack + threshold;
                    threshold = (maxBlack-minBlack)*0.7;
                    epsilon = maxBlack - threshold;
                }else if(callee.equals("Series")){
                    threshold = 50;
                    epsilon = minBlack + threshold;
                }
                // make array of answers -> answers[i] = j : i is question number, j is answer number
                studentAnswers[i] = numberOfAnswerThatChoosed;
                for (j = 0; j < sumOfBlacks[i].length; j++) {
                    // record all the answers of the i question that marked
                    // the conditions are:
                    // - j < allanswers[i].length
                    // - at least sum of black pixels like the min sum of black pixels + 30 .
                    // - and j+1 is not the answer with the max sum of black pixels .
                    if ((j) < allanswers[i].length && (j + 1) != numberOfAnswerThatChoosed && epsilon > sumOfBlacks[i][j]) {
                        flagNeedToCorrectSomeAnswers = true;
                        needToAddtheChoosedOne = true;
                        AnotherAnswersThatChoosed.add(allanswers[i][j]);
                    }
                }
                if(needToAddtheChoosedOne)
                    if(!AnotherAnswersThatChoosed.contains(allanswers[i][numberOfAnswerThatChoosed-1]))
                        AnotherAnswersThatChoosed.add(allanswers[i][numberOfAnswerThatChoosed-1]);
            }
            int l=0;
            if (flagNeedToCorrectSomeAnswers) {
                Intent intent = new Intent(oneByOneOrSeries.this, ProblematicQuestionsActivity.class);
                intent.putExtra("problematicAnswers", AnotherAnswersThatChoosed);
                if(callee.equals("Series"))
                    currentImagePath = saveJpeg(align_to_template.align,testsDir.getAbsolutePath(),"test_"+countSeries+"a");

                intent.putExtra("sheet", currentImagePath);
                intent.putExtra("numberOfQuestions", numberOfQuestions);
                intent.putExtra("numberOfAnswers", numberOfAnswers);
                intent.putExtra("callee", callee);
                intent.putExtra("id", id);
                startActivityForResult(intent, 2);
            } else {
                // make binary array for questions -> 1 - right  , 0 - wrong
                int[] binaryCorrectFlag = new int[numberOfQuestions+1];
                for (i = 1; i < numberOfQuestions+1; i++) {
                    if (correctAnswers[i] == studentAnswers[i])
                        binaryCorrectFlag[i] = 1;
                    else
                        binaryCorrectFlag[i] = 0;
                }
                double currGrade = students_db.insertRaw(id, studentAnswers, binaryCorrectFlag, (int)score);
                if(currGrade!= -1)
                    lastGrade = currGrade;
                else
                    totalExamsThatProcessedUntilNow--;
                if(callee.equals("Series")){
                    countSeries++;
                    return true;
                }
                else if(callee.equals("OneByOne"))
                    checkIfFinalSheetOrContinue();
            }
         return true;
        }
    }

    /**
     * update all the statistics data.
     * @return None
     */
    private void checkIfFinalSheetOrContinue(){

        if(totalExamsThatProcessedUntilNow == 1)
            averageUntilNow = lastGrade;
        else
            averageUntilNow = ((averageUntilNow * (totalExamsThatProcessedUntilNow-1)) +lastGrade)/totalExamsThatProcessedUntilNow;

        int integer =(int) Math.floor(averageUntilNow);
        double fraction = (Math.floor((averageUntilNow)*100) - (integer*100))/100;
        double averageWith2DigitAfterThePoint = integer+fraction;


        // print number of exams that processed already
        info_examsCounter.setText(String.valueOf(totalExamsThatProcessedUntilNow));
        // print the last grade that processed
        info_lastGrade.setText(String.valueOf(lastGrade));
        // print the average so far
        info_average.setText(String.valueOf(averageWith2DigitAfterThePoint));

        finish_check_button.setVisibility(View.VISIBLE);
        need_to_continue.setVisibility(View.VISIBLE);
        need_to_continue1.setVisibility(View.VISIBLE);

    }

    /**
     * calculate brightness in the image.
     * @param img is the image we calculate the brightness
     * @param answer is the coordinates where to calculate
     * @return brightness of the <answer> in <img>
     */
    private int calculateBlackLevel(Mat img, Answer answer ){
        double blackLevel=0.0;

        Matrix scaleToImageSize = new Matrix();
        RectF viewRect = new RectF(0, 0, img.cols(), img.rows());
        RectF drawableRect = new RectF(0, 0, realA4Width, realA4Height);
        scaleToImageSize.setRectToRect(drawableRect, viewRect, Matrix.ScaleToFit.FILL);


        float[][] pointsF = new float[][]{
                {answer.getLocationX(),(answer.getLocationY())},
                {(answer.getLocationX()+answer.getWidth()),(answer.getLocationY())},
                {answer.getLocationX(),(answer.getLocationY()+answer.getHeight())},
                {(answer.getLocationX()+answer.getWidth()),(answer.getLocationY()+answer.getHeight())}
        };

        Point[] ps2 = new Point[4];


        for (int i = 0; i < 4; i++) {
            scaleToImageSize.mapPoints(pointsF[i]);
            ps2[i] = new Point(pointsF[i][0],pointsF[i][1]);
        }


        Imgproc.line(imageForTest,ps2[0],ps2[1],new Scalar(0, 0, 255, 150), 4);
        Imgproc.line(imageForTest,ps2[1],ps2[2],new Scalar(0, 0, 255, 150), 4);
        Imgproc.line(imageForTest,ps2[2],ps2[3],new Scalar(0, 0, 255, 150), 4);
        Imgproc.line(imageForTest,ps2[3],ps2[0],new Scalar(0, 0, 255, 150), 4);

        int d = img.channels();
        int row=0;
        int col=0;
        for(row = (int)ps2[1].y; row<ps2[2].y ; row++){
            for(col = (int)ps2[0].x ; col<ps2[1].x ; col++ ){
                for (int j = 0; j < d ; j++) {
                    blackLevel = blackLevel+ img.get(row,col)[j];
                }
            }
        }


        return (int)(Math.floor(blackLevel/(d * ((row - (int)ps2[1].y) * (col - (int)ps2[0].x)))));
    }

    /**
     * function that extract all the data from the <db_template> to 2-d array.
     * @param db_template is the db that contain all the data.
     * @return 2-d array with all the coordinates of the answers for each question
     */
    private Answer[][] extractAllDbTemplateInfo(SQLiteDatabase db_template) {

        Cursor c = db_template.query(Template.TABLE_NAME,new String[]{"ID","NUMBER_QUESTIONS","NUMBER_ANSWERS"},"ID = 0",null,null,null,"ID");
        if (c.moveToFirst()) {
            numberOfQuestions = c.getInt(1);
            numberOfAnswers = c.getInt(2);
            score = 100/numberOfQuestions;
        }


        Answer[][] allAnswers = new Answer[numberOfQuestions+1][numberOfAnswers];
        Cursor cursor = db_template.query(Template.TABLE_NAME,null,null,null,null,null,"ID");


        //if TABLE has rows
        if (cursor.moveToFirst()) {
            //Loop through the table rows
            do {
                Answer answer = new Answer();
                int id = (cursor.getInt(0));
                int answerNumber = id%10; //LSB of the number represent the number of answer
                int questionNumber = id/10; //all digits number part the last digit represent the question number

                answer.setAnswerNumber(answerNumber);
                answer.setQuestionNumber(questionNumber);
                answer.setLocationX(cursor.getFloat(1));
                answer.setLocationY(cursor.getFloat(2));
                answer.setHeight(cursor.getFloat(3));
                answer.setWidth(cursor.getFloat(4));
                answer.setSum_of_black(cursor.getInt(5));
                answer.setFlagCorrect(cursor.getInt(6));
                if(answerNumber==0 && questionNumber==0)
                    allAnswers[0][0] = answer;
                    // add answer to list
                else
                    allAnswers[questionNumber][answerNumber-1] = answer;

            } while (cursor.moveToNext());
        }
        return allAnswers;
    }



//    for debug alignment algorithm
//    /**
//     *
//     * @param
//     * @return
//     */
//    public void ok_align(View view){
//        //Update the view Contents
//        test_align_Layout.setVisibility(View.INVISIBLE);
//        test_align.setVisibility(View.INVISIBLE);
//        realign_button.setVisibility(View.INVISIBLE);
//        ok_align_button.setVisibility(View.INVISIBLE);
//        //update the status variable
//        totalExamsThatProcessedUntilNow++;
//        //insert the student to DB
//        boolean res = insertStudent(null ,"OneByOne");
//        if(!res) {
//            totalExamsThatProcessedUntilNow--;
//            Toast.makeText(this, "Faild: not success to check this test, because there is no barcode in this test", Toast.LENGTH_LONG).show();
//            Log.e("BARCODE", "Faild: not success to check this test, because there is no barcode in this test");
//        }
//
//    }

//    /**
//     *
//     * @param
//     * @return
//     */
//    public void again_align(View view){
//        File old_pic = new File(currentImagePath);
//        if(old_pic.exists())
//            old_pic.delete();
//        series.setVisibility(View.INVISIBLE);
//        oneByOne.setVisibility(View.INVISIBLE);
//        test_align_Layout.setVisibility(View.INVISIBLE);
//        test_align.setVisibility(View.INVISIBLE);
//        realign_button.setVisibility(View.INVISIBLE);
//        ok_align_button.setVisibility(View.INVISIBLE);
//        Intent takePicture = new Intent(getApplicationContext(), TouchActivity.class);
//        takePicture.putExtra("templatePath",templatePath);
//        takePicture.putExtra("caller","oneByOne");
//        startActivityForResult(takePicture,1);
//    }
}
