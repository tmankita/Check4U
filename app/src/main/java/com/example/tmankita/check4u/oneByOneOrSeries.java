package com.example.tmankita.check4u;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.example.tmankita.check4u.Camera.TouchActivity;
import com.example.tmankita.check4u.Database.Answer;
import com.example.tmankita.check4u.Database.StudentDataBase;
import com.example.tmankita.check4u.Database.Template;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Size;

import java.util.ArrayList;

import static android.database.sqlite.SQLiteDatabase.OPEN_READONLY;

public class oneByOneOrSeries extends AppCompatActivity {

    private Mat paper;
    private Answer[][] allanswers;
    private double score;
    private  int numberOfQuestions;
    private int numberOfAnswers;
    private StudentDataBase students_db;
    private int[] studentAnswers;
    private int[] correctAnswers;
    int id;

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

        Bundle extras = getIntent().getExtras();
        score = extras.getDouble("score");
        numberOfQuestions = extras.getInt("numberOfQuestions");
        numberOfAnswers   = extras.getInt("numberOfAnswers");


        SQLiteDatabase db_template = SQLiteDatabase.openDatabase(Template.DB_FILEPATH, null, OPEN_READONLY);
        allanswers = extractAllDbTemplateInfo(db_template);
        db_template.close();

        students_db = new StudentDataBase(getApplicationContext(),numberOfQuestions);
    }


    public void oneByOne (View view){


        Intent takePicture = new Intent(getApplicationContext(), TouchActivity.class);
        startActivityForResult(takePicture,1);
    }
    public void series (View view){}

    public void insertStudent (String Path) {
        //calculate the image in path

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap = BitmapFactory.decodeFile(Path, options);
        // to sum the black level in matrix
        Bitmap bmp = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(bmp, paper);

        // calculate barcode -> student id

        //-------------------------------
        //
        int i=0;
        int j=0;
        int correct = 0;
        correctAnswers = new int[numberOfQuestions];
        int[][] sumOfBlacks = new int[numberOfQuestions][numberOfAnswers];
        ArrayList<Answer> AnotherAnswersThatChoosed = new ArrayList<>();
        boolean flagNeedToCorrectSomeAnswers = false;
       for(i = 0 ; i < allanswers.length ; i++ ){
           Answer[] question = allanswers[i];
           for(j = 0 ; j < question.length ; j++ )
            correct = question[j].getFlagCorrect();
            sumOfBlacks[i][j] = calculateBlackLevel(paper, new Point(question[j].getLocationX(), question[j].getLocationY()), new Size(question[j].getWidth(), question[j].getHeight()));
            if (correct == 1)
                correctAnswers[i] = j+1;
        }

        //find question with two answers marked or more
            //get all of them -> show the user all the marks that marked let him to choose the right one
        studentAnswers = new int[numberOfQuestions];
        int numberOfAnswerThatChoosed=0;
        for (i=0 ; i<sumOfBlacks.length ; i++) {
            int maxBlack = 0;
            for( j=0; j<sumOfBlacks[i].length ; j++) {
                if (maxBlack < sumOfBlacks[i][j]) {
                    maxBlack = sumOfBlacks[i][j];
                    numberOfAnswerThatChoosed = j + 1;
                }
            }
            // make array of answers -> answers[i] = j : i is question number, j is answer number
            studentAnswers[i]=numberOfAnswerThatChoosed;
            for(j=0; j<sumOfBlacks[i].length ; j++) {
                // record all the answers of the i question that marked
                // the conditions are:
                // - at least sum of black pixels like the max sum of black pixels - 30 .
                // - and j+1 is not the answer with the max sum of black pixels .
                if ((j + 1) != numberOfAnswerThatChoosed && (maxBlack - 30) < sumOfBlacks[i][j]) {
                    flagNeedToCorrectSomeAnswers = true;
                    AnotherAnswersThatChoosed.add(allanswers[i][j]);

                }

            }
        }


        if(flagNeedToCorrectSomeAnswers){
            // this end:
            //Intent intent = new Intent(RegisterActivity1.this, RegisterActivity2.class);
            //intent.putExtra("problematicAnswers", AnotherAnswersThatChoosed);
            //startActivity(intent);
            //another end:
//            ArrayList<Answer> problematicAnswers = (ArrayList<Answer>) intent.getSerializable("problematicAnswers");

        }
        else{
            // make binary array for questions -> 1 - right  , 0 - wrong
            int[] binaryCorrectFlag = new int[numberOfQuestions];
            for (i =0 ; i<numberOfQuestions ; i++){
                if(correctAnswers[i] == studentAnswers[i])
                    binaryCorrectFlag[i]=1;
                else
                    binaryCorrectFlag[i]=0;
            }
            students_db.insertRaw(id,studentAnswers,binaryCorrectFlag,score);
        }



        //need to ask if the last one!!!!!!!!!!!!!!!!!!
        //checkIfFinalSheetOrContinue();

        Intent takePicture = new Intent(getApplicationContext(), TouchActivity.class);
        startActivityForResult(takePicture,1);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == 1) {
            if(resultCode == Activity.RESULT_OK){
                String path=data.getStringExtra("sheet");
                insertStudent(path);
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                //Write your code if there's no result
            }
        } else if(requestCode == 2){
            int i=0;
            if(resultCode == Activity.RESULT_OK){
                int[] toFix = data.getIntArrayExtra("toFix");
                for (i=0; i<toFix.length ;i++){
                    if(toFix[i]!=0){
                        studentAnswers[i]=toFix[i];
                    }
                }
                // make binary array for questions -> 1 - right  , 0 - wrong
                int[] binaryCorrectFlag = new int[numberOfQuestions];
                for (i =0 ; i<numberOfQuestions ; i++){
                    if(correctAnswers[i] == studentAnswers[i])
                        binaryCorrectFlag[i]=1;
                    else
                        binaryCorrectFlag[i]=0;
                }
                students_db.insertRaw(id,studentAnswers,binaryCorrectFlag,score);

                //checkIfFinalSheetOrContinue();


            }
            if (resultCode == Activity.RESULT_CANCELED) {
                //Write your code if there's no result
            }
        }
    }//onActivityResult




    private int calculateBlackLevel(Mat img, Point location, Size size ){
        double blackLevel=0.0;
//        double[] currentPixel;
        for (int col = (int)location.x ; col < (location.x + size.width) ; ++col){
            for (int raw = (int)location.y ; raw < (location.y + size.height) ; ++raw){
                blackLevel = blackLevel+ img.get(raw,col)[0];
            }
        }
        return (int)blackLevel;
    }


    public Answer[][] extractAllDbTemplateInfo(SQLiteDatabase db_template) {

        Answer[][] allAnswers = new Answer[numberOfQuestions][numberOfAnswers];
        Cursor cursor = db_template.query(Template.TABLE_NAME,null,null,null,null,null,"ID");

        //if TABLE has rows
        if (cursor.moveToFirst()) {
            //Loop through the table rows
            do {
                Answer answer = new Answer();
                int id = (cursor.getInt(0));
                int answerNumber = id%10; //LSB of the number represent the number of answer
                int questionNumber = id/10; //all digits number part the last digit represent the question number
//                answer.setAnswerNumber(answerNumber);
//                answer.setQuestionNumber(questionNumber);
                answer.setLocationX(cursor.getInt(1));
                answer.setLocationY(cursor.getInt(2));
                answer.setHeight(cursor.getInt(3));
                answer.setWidth(cursor.getInt(4));
                answer.setSum_of_black(cursor.getInt(5));
                answer.setFlagCorrect(cursor.getInt(6));

                // add answer to list
                allAnswers[questionNumber-1][answerNumber-1] = answer;

            } while (cursor.moveToNext());
        }
        return allAnswers;
    }

}
