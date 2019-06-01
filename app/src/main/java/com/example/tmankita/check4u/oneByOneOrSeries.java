package com.example.tmankita.check4u;

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
import android.os.StrictMode;
import android.util.Log;
import android.util.SparseArray;
import android.view.Display;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.core.v2.files.FileMetadata;
import com.example.tmankita.check4u.Camera.TouchActivity;
import com.example.tmankita.check4u.Database.Answer;
import com.example.tmankita.check4u.Database.StudentDataBase;
import com.example.tmankita.check4u.Database.Template;
import com.example.tmankita.check4u.Detectors.detectDocument;
import com.example.tmankita.check4u.Dropbox.UserDropBoxActivity;
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

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import static android.database.sqlite.SQLiteDatabase.OPEN_READONLY;
import static android.widget.Toast.LENGTH_SHORT;

public class oneByOneOrSeries extends AppCompatActivity {

    //image - student test
    private Mat paper;
    private String currentImagePath;

    //DB
    private Answer[][] allanswers;
    private double score;
    private  int numberOfQuestions;
    private int numberOfAnswers;
    private StudentDataBase students_db;
    private int[] studentAnswers;
    private int[] correctAnswers;
    int id;
    private String dbPath;
    private String templatePath;

    //Views
    private TableLayout need_to_continue;
    private Button series;
    private Button oneByOne;
//    private TableLayout send_via_email;
//    private Button send;
//    private EditText email;
    private ImageView test_align;
    private Button ok_align_button;
    private Button realign_button;
    private RelativeLayout test_align_Layout;
    private TextView info_examsCounter;
    private TextView info_lastGrade;
    private TextView info_average;

    //To counters
    private int totalExamsThatProcessedUntilNow;
    private double lastGrade;
    private double averageUntilNow;

    //helpers
    private Bitmap bmpMarks;
    private float realA4Width = 4960;//(float)796.8;
    private float realA4Height = 7016;//(float)1123.2;



    // CSV
    private File file;

    // for tests
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
        setContentView(R.layout.activity_onebyone_or_series);

        if (!OpenCVLoader.initDebug()) {
            Log.d("LOAD OPENCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d("LOAD OPENCV", "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        final Context c = this;

        Bundle  extras = getIntent().getExtras();

        need_to_continue  = findViewById(R.id.Layout_if_need_to_continue);
//        send_via_email    = findViewById(R.id.Layout_send_via_email);
        info_examsCounter = (TextView) findViewById(R.id.info_count_tests_num);
        info_lastGrade    = (TextView) findViewById(R.id.info_last_grade_num);
        info_average      = (TextView) findViewById(R.id.info_average_num);
//        email             = (EditText) findViewById(R.id.edit_email);
        series            = (Button) findViewById(R.id.series_button);
        oneByOne          = (Button) findViewById(R.id.oneByOne_button);
//        send              = (Button) findViewById(R.id.send_button);
        test_align        = (ImageView) findViewById(R.id.test_align);
        ok_align_button   = (Button) findViewById(R.id.ok_align);
        realign_button    = (Button) findViewById(R.id.realign);
        test_align_Layout = (RelativeLayout) findViewById(R.id.test_align_layout);

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
            protected Void doInBackground( final Void ... params ) {
                // something you know that will take a few seconds
                Log.e("CHECKL4U","DO IN BACKGROUND");
                String targetDir =  Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath()+ "/Check4U_DB/UNZIP/"+generateDirName()+"/";
                ZipManager zipManager = new ZipManager();
                zipManager.unzip(dbPath,targetDir);

                String templateName = null;
                String dbName = null;
                File yourDir = new File(targetDir);
                for (File f : yourDir.listFiles()) {
                    if (f.isFile() && f.getName().toLowerCase().endsWith("jpg"))
                        templateName = f.getName();
                    else if(f.isFile() && f.getName().toLowerCase().endsWith("db"))
                        dbName = f.getName();
                }


                templatePath = targetDir+"/"+templateName;

                SQLiteDatabase db_template = SQLiteDatabase.openDatabase(targetDir+"/"+dbName, null, OPEN_READONLY);


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
            protected void onPostExecute( final Void result ) {
                pd.dismiss();

            }
        }.execute();

    }

    private static String generateDirName(){

        // Create a Directory name
        String timeStamp ="db_"+new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());


        return timeStamp;
    }

    public void oneByOne (View view){
        series.setVisibility(View.INVISIBLE);
        oneByOne.setVisibility(View.INVISIBLE);
        Intent takePicture = new Intent(getApplicationContext(), TouchActivity.class);
        takePicture.putExtra("templatePath",templatePath);
        takePicture.putExtra("caller","oneByOne");
        startActivityForResult(takePicture,1);
    }

    public void series (View view){
        series.setVisibility(View.INVISIBLE);
        oneByOne.setVisibility(View.INVISIBLE);
    }

    public void continue_check(View view){
        Intent takePicture = new Intent(getApplicationContext(), TouchActivity.class);
        takePicture.putExtra("templatePath",templatePath);
        takePicture.putExtra("caller","oneByOne");
        startActivityForResult(takePicture,1);
    }

    public void finish_check(View view){
    //send via mail
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
//            send_via_email.setVisibility(View.VISIBLE);

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

//    public void sendEmail( View view ){
//
//        Uri path = Uri.fromFile(file);
//
//        Intent emailIntent = new Intent(Intent.ACTION_SEND);
//// set the type to 'email'
//        emailIntent .setType("text/plain");
//        String address = email.getText().toString();
//
//        String to[] = {address};
//
//        emailIntent .putExtra(Intent.EXTRA_EMAIL, to);
//// the attachment
//        emailIntent .putExtra(Intent.EXTRA_STREAM, path);
//// the mail subject
//        emailIntent .putExtra(Intent.EXTRA_SUBJECT, "Check4U - Grades DataBase");
//
//        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
//        StrictMode.setVmPolicy(builder.build());
//
//        startActivityForResult(Intent.createChooser(emailIntent , "Send email..."),3);
//
//    }

    public void ok_align(View view){
        test_align_Layout.setVisibility(View.INVISIBLE);
        test_align.setVisibility(View.INVISIBLE);
        realign_button.setVisibility(View.INVISIBLE);
        ok_align_button.setVisibility(View.INVISIBLE);
        totalExamsThatProcessedUntilNow++;
        final Context c = this;


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
            protected Void doInBackground(Void... voids) {
                boolean res = insertStudent(currentImagePath);
                if(!res) {
                    totalExamsThatProcessedUntilNow--;
                    Toast.makeText(c, "Faild: not success to check this test, because there is no barcode in this test", Toast.LENGTH_LONG).show();
                    Log.e("BARCODE", "Faild: not success to check this test, because there is no barcode in this test");
                }
                return null;
            }

            @Override
            protected void onPostExecute( final Void result ) {
                pd.dismiss();
                checkIfFinalSheetOrContinue();


            }
        }.execute();



    }

    public void again_align(View view){
        File old_pic = new File(currentImagePath);
        if(old_pic.exists())
            old_pic.delete();
        series.setVisibility(View.INVISIBLE);
        oneByOne.setVisibility(View.INVISIBLE);
        test_align_Layout.setVisibility(View.INVISIBLE);
        test_align.setVisibility(View.INVISIBLE);
        realign_button.setVisibility(View.INVISIBLE);
        ok_align_button.setVisibility(View.INVISIBLE);
        Intent takePicture = new Intent(getApplicationContext(), TouchActivity.class);
        takePicture.putExtra("templatePath",templatePath);
        takePicture.putExtra("caller","oneByOne");
        startActivityForResult(takePicture,1);
    }

    private int detectBarcode (Mat sheet,Bitmap bitmapSheet){
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

        Imgproc.line(imageForTest,sorted_2[0],sorted_2[1],new Scalar(0, 255, 0, 150), 4);
        Imgproc.line(imageForTest,sorted_2[1],sorted_2[2],new Scalar(0, 255, 0, 150), 4);
        Imgproc.line(imageForTest,sorted_2[2],sorted_2[3],new Scalar(0, 255, 0, 150), 4);
        Imgproc.line(imageForTest,sorted_2[3],sorted_2[0],new Scalar(0, 255, 0, 150), 4);

        Bitmap bmpBarcodeForTest = bitmapSheet.createBitmap(imageForTest.cols(), imageForTest.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(imageForTest, bmpBarcodeForTest);

        Bitmap bmpBarcode = bitmapSheet.createBitmap(barcodeCropped.cols(), barcodeCropped.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(barcodeCropped, bmpBarcode);

        int id = 0;
        String  LOG_TAG = "BARCODE";
        Frame frame = new Frame.Builder().setBitmap(bmpBarcode).build();
        BarcodeDetector barcodeDetector = new BarcodeDetector.Builder(getApplicationContext())
                .build();

        if(barcodeDetector.isOperational()){
            SparseArray<Barcode> sparseArray = barcodeDetector.detect(frame);
            if(sparseArray != null && sparseArray.size() > 0){
                for (int i = 0; i < sparseArray.size(); i++){
                    try{
                        Log.d(LOG_TAG, "Value_" + sparseArray.valueAt(i).rawValue + "_" + sparseArray.valueAt(i).displayValue);
                        id = Integer.parseInt(sparseArray.valueAt(i).rawValue);
                    }catch(Exception e){
                        barcodeCropped.release();
//                        Toast.makeText(this,"Didn't catch the barcode, take the picture again",LENGTH_SHORT).show();
                        File old_pic = new File(currentImagePath);
                        if(old_pic.exists())
                            old_pic.delete();
                        series.setVisibility(View.INVISIBLE);
                        oneByOne.setVisibility(View.INVISIBLE);
                        test_align_Layout.setVisibility(View.INVISIBLE);
                        test_align.setVisibility(View.INVISIBLE);
                        realign_button.setVisibility(View.INVISIBLE);
                        ok_align_button.setVisibility(View.INVISIBLE);
                        Intent takePicture = new Intent(getApplicationContext(), TouchActivity.class);
                        takePicture.putExtra("templatePath",templatePath);
                        takePicture.putExtra("caller","oneByOne");
                        startActivityForResult(takePicture,1);
                    }
                    break;

                }
            }else {
                barcodeCropped.release();
                Toast.makeText(this, "The Barcode is not clear, take the picture again ",Toast.LENGTH_LONG).show();
                Log.e(LOG_TAG,"SparseArray null or empty");
                File old_pic = new File(currentImagePath);
                if(old_pic.exists())
                    old_pic.delete();
                series.setVisibility(View.INVISIBLE);
                oneByOne.setVisibility(View.INVISIBLE);
                test_align_Layout.setVisibility(View.INVISIBLE);
                test_align.setVisibility(View.INVISIBLE);
                realign_button.setVisibility(View.INVISIBLE);
                ok_align_button.setVisibility(View.INVISIBLE);
                Intent takePicture = new Intent(getApplicationContext(), TouchActivity.class);
                takePicture.putExtra("templatePath",templatePath);
                takePicture.putExtra("caller","oneByOne");
                startActivityForResult(takePicture,1);

            }

        }else{
            Log.e(LOG_TAG, "Detector dependencies are not yet downloaded");
        }
    barcodeCropped.release();
    return id;
    }

    private boolean insertStudent (String Path) {
        //import the image from path
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap = BitmapFactory.decodeFile(Path, options);
        // to sum the black level in matrix
        Bitmap bmpBarcode = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        bmpMarks = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(bmpBarcode, paper);
        paper.copyTo(imageForTest);

        // calculate barcode -> student id
        int id = detectBarcode(paper,bmpBarcode);
        if(id == 0){
            return false;

        }
        //-------------------------------
        //
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
                    sumOfBlacks[i][j] = calculateBlackLevel(paper, question[j]);
                    if (correct == 1)
                        correctAnswers[i] = j + 1;
                }

            }
        Bitmap bmpForTest = bmpMarks.createBitmap(imageForTest.cols(), imageForTest.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(imageForTest, bmpForTest);

            //find question with two answers marked or more
            //get all of them -> show the user all the marks that marked let him to choose the right one
            studentAnswers = new int[numberOfQuestions+1];
            int numberOfAnswerThatChoosed = 0;
            for (i = 1; i < sumOfBlacks.length; i++) {
                int minBlack = Integer.MAX_VALUE;
                for (j = 0; j < sumOfBlacks[i].length; j++) {
                    if (minBlack > sumOfBlacks[i][j]) {
                        minBlack = sumOfBlacks[i][j];
                        numberOfAnswerThatChoosed = j + 1;
                    }
                }
                boolean needToAddtheChoosedOne = false;
                // make array of answers -> answers[i] = j : i is question number, j is answer number
                studentAnswers[i] = numberOfAnswerThatChoosed;
                for (j = 0; j < sumOfBlacks[i].length; j++) {
                    // record all the answers of the i question that marked
                    // the conditions are:
                    // - j+1 < allanswers[i].length
                    // - at least sum of black pixels like the min sum of black pixels + 30 .
                    // - and j+1 is not the answer with the max sum of black pixels .
                    if ((j + 1) < allanswers[i].length && (j + 1) != numberOfAnswerThatChoosed && (minBlack + 10000) > sumOfBlacks[i][j]) {
                        flagNeedToCorrectSomeAnswers = true;
                        needToAddtheChoosedOne = true;
                        AnotherAnswersThatChoosed.add(allanswers[i][j]);

                    }

                }
                if(needToAddtheChoosedOne)
                    AnotherAnswersThatChoosed.add(allanswers[i][numberOfAnswerThatChoosed-1]);

            }


            if (flagNeedToCorrectSomeAnswers) {
                Intent intent = new Intent(oneByOneOrSeries.this, ProblematicQuestionsActivity.class);
                intent.putExtra("problematicAnswers", AnotherAnswersThatChoosed);
                intent.putExtra("sheet", Path);//numberOfQuestions
                intent.putExtra("numberOfQuestions", numberOfQuestions);
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
                lastGrade = students_db.insertRaw(id, studentAnswers, binaryCorrectFlag, (int)score);

            }
         return true;
        }

    }

    private void checkIfFinalSheetOrContinue(){

        if(totalExamsThatProcessedUntilNow == 1)
            averageUntilNow = lastGrade;
        else
            averageUntilNow = ((averageUntilNow * (totalExamsThatProcessedUntilNow-1)) +lastGrade)/totalExamsThatProcessedUntilNow;

        // print number of exams that processed already
        info_examsCounter.setText(String.valueOf(totalExamsThatProcessedUntilNow));
        // print the last grade that processed
        info_lastGrade.setText(String.valueOf(lastGrade));
        // print the average so far
        info_average.setText(String.valueOf(averageUntilNow));

        need_to_continue.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i("onActivityResult","welcome back!");
        if (requestCode == 1) {
            if(resultCode == Activity.RESULT_OK){//come back from align detector
                try {
                    currentImagePath = data.getStringExtra("sheet");

                    Mat align = new Mat();
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inPreferredConfig = Bitmap.Config.ARGB_8888;

                    Bitmap bitmap = BitmapFactory.decodeFile(currentImagePath, options);
                    Bitmap bmpAlign = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                    Utils.bitmapToMat(bmpAlign, align);

                    Bitmap bmpPaper = Bitmap.createBitmap(align.cols(), align.rows(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(align, bmpPaper);
                    Matrix matrix2 = new Matrix();
                    Bitmap bOutput = Bitmap.createBitmap(bmpPaper, 0, 0, bmpPaper.getWidth(), bmpPaper.getHeight(), matrix2, true);

                    Display display = getWindowManager().getDefaultDisplay();
                    android.graphics.Point size = new android.graphics.Point();
                    display.getSize(size);
                    int width = size.x;
                    int height = size.y;

                    test_align.setImageBitmap(bOutput);
                    Matrix M = test_align.getImageMatrix();
                    RectF drawableRect = new RectF(0, 0, align.cols(), align.rows());
                    RectF viewRect = new RectF(0, 0, width, height);
                    M.setRectToRect(drawableRect, viewRect, Matrix.ScaleToFit.CENTER);
                    test_align.setImageMatrix(M);
                    test_align.invalidate();

                    test_align_Layout.setVisibility(View.VISIBLE);
                    test_align.setVisibility(View.VISIBLE);
                    realign_button.setVisibility(View.VISIBLE);
                    ok_align_button.setVisibility(View.VISIBLE);
                } catch(Exception e){
                    Toast.makeText(this, "The alignment procces go wrong, try again", Toast.LENGTH_LONG).show();
                    File old_pic = new File(currentImagePath);
                    if(old_pic.exists())
                        old_pic.delete();
                    series.setVisibility(View.INVISIBLE);
                    oneByOne.setVisibility(View.INVISIBLE);
                    test_align_Layout.setVisibility(View.INVISIBLE);
                    test_align.setVisibility(View.INVISIBLE);
                    realign_button.setVisibility(View.INVISIBLE);
                    ok_align_button.setVisibility(View.INVISIBLE);
                    Intent takePicture = new Intent(getApplicationContext(), TouchActivity.class);
                    takePicture.putExtra("templatePath",templatePath);
                    takePicture.putExtra("caller","oneByOne");
                    startActivityForResult(takePicture,1);
                }
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                //Write your code if there's no result
            }
        } else if(requestCode == 2){//come back from problematic questions
            int i;
            if(resultCode == Activity.RESULT_OK) {
                int[] toFix = data.getIntArrayExtra("toFix");
                for (i = 0; i < toFix.length; i++) {
                    if (toFix[i] != 0) {
                        studentAnswers[i] = toFix[i];
                    }
                }
                // make binary array for questions -> 1 - right  , 0 - wrong
                int[] binaryCorrectFlag = new int[numberOfQuestions];
                for (i = 0; i < numberOfQuestions; i++) {
                    if (correctAnswers[i] == studentAnswers[i])
                        binaryCorrectFlag[i] = 1;
                    else
                        binaryCorrectFlag[i] = 0;
                }
                lastGrade = students_db.insertRaw(id, studentAnswers, binaryCorrectFlag, (int)score);

                checkIfFinalSheetOrContinue();
            }if (resultCode == Activity.RESULT_CANCELED) {
                    //Write your code if there's no result
                }

            }
//        else if(requestCode == 3){// come back from share action
//            // think about erase all the pictures, the db not erase
//                startActivity(new Intent(getApplicationContext(),MainActivity.class));
//            }

    }//onActivityResult

    private int calculateBlackLevel(Mat img, Answer answer ){
        double blackLevel=0.0;

        Matrix scaleToImageSize = new Matrix();
        RectF viewRect = new RectF(0, 0, img.cols(), img.rows());
        RectF drawableRect = new RectF(0, 0, realA4Width, realA4Height);
        boolean success = scaleToImageSize.setRectToRect(drawableRect, viewRect, Matrix.ScaleToFit.FILL);

//        if(!success){Log.i("scaling","not success!!!!!");}

//        float offset = (float)(0.5 * Math.abs(answer.getLocationY()-(answer.getLocationY()+answer.getHeight()))+10);
        float[][] pointsF = new float[][]{
                {answer.getLocationX(),(answer.getLocationY())},
                {(answer.getLocationX()+answer.getWidth()),(answer.getLocationY())},
                {answer.getLocationX(),(answer.getLocationY()+answer.getHeight())},
                {(answer.getLocationX()+answer.getWidth()),(answer.getLocationY()+answer.getHeight())}
        };
//        float[][] points = new float[][]{
//                {answer.getLocationX(),(answer.getLocationY()-offset)},
//                {(answer.getLocationX()+answer.getWidth()),(answer.getLocationY()-offset)},
//                {answer.getLocationX(),(answer.getLocationY()-answer.getHeight()-offset)},
//                {(answer.getLocationX()+answer.getWidth()),(answer.getLocationY()-answer.getHeight()-offset)}
//        };
//        float[][] points_1 = new float[][]{
//                {answer.getLocationX(),(answer.getLocationY()-offset)},
//                {(answer.getLocationX()+answer.getWidth()),(answer.getLocationY()-offset)},
//                {answer.getLocationX(),(answer.getLocationY()+answer.getHeight()-offset)},
//                {(answer.getLocationX()+answer.getWidth()),(answer.getLocationY()+answer.getHeight()-offset)}
//        };

//        Point[] ps = new Point[4];
//        Point[] ps1 = new Point[4];
        Point[] ps2 = new Point[4];


        for (int i = 0; i < 4; i++) {
//            scaleToImageSize.mapPoints(points[i]);
//            ps[i] = new Point(points[i][0],points[i][1]);
//            scaleToImageSize.mapPoints(points_1[i]);
//            ps1[i] = new Point(points_1[i][0],points_1[i][1]);
            scaleToImageSize.mapPoints(pointsF[i]);
            ps2[i] = new Point(pointsF[i][0],pointsF[i][1]);
        }



//        Imgproc.line(imageForTest,ps[0],ps[1],new Scalar(0, 255, 0, 150), 4);
//        Imgproc.line(imageForTest,ps[1],ps[2],new Scalar(0, 255, 0, 150), 4);
//        Imgproc.line(imageForTest,ps[2],ps[3],new Scalar(0, 255, 0, 150), 4);
//        Imgproc.line(imageForTest,ps[3],ps[0],new Scalar(0, 255, 0, 150), 4);
//        Imgproc.line(imageForTest,ps1[0],ps1[1],new Scalar(255, 0, 0, 150), 4);
//        Imgproc.line(imageForTest,ps1[1],ps1[2],new Scalar(255, 0, 0, 150), 4);
//        Imgproc.line(imageForTest,ps1[2],ps1[3],new Scalar(255, 0, 0, 150), 4);
//        Imgproc.line(imageForTest,ps1[3],ps1[0],new Scalar(255, 0, 0, 150), 4);
        Imgproc.line(imageForTest,ps2[0],ps2[1],new Scalar(0, 0, 255, 150), 4);
        Imgproc.line(imageForTest,ps2[1],ps2[2],new Scalar(0, 0, 255, 150), 4);
        Imgproc.line(imageForTest,ps2[2],ps2[3],new Scalar(0, 0, 255, 150), 4);
        Imgproc.line(imageForTest,ps2[3],ps2[0],new Scalar(0, 0, 255, 150), 4);


        for(int row = (int)ps2[1].y; row<ps2[2].y ; row++){
            for(int col = (int)ps2[0].x ; col<ps2[1].x ; col++ ){
                blackLevel = blackLevel+ img.get(row,col)[0];
            }
        }


        return (int)blackLevel;
    }

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

}
