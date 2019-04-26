package com.example.tmankita.check4u;

import androidx.appcompat.app.AppCompatActivity;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.Bundle;
import android.util.Log;

import com.example.tmankita.check4u.Database.Answer;
import com.example.tmankita.check4u.Database.Template;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Size;

import java.sql.SQLException;
import java.util.ArrayList;

import static android.database.sqlite.SQLiteDatabase.OPEN_READONLY;

public class OneByOneActivity extends AppCompatActivity {
    Mat paper;
    double score;

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
        setContentView(R.layout.activity_one_by_one);

        if (!OpenCVLoader.initDebug()) {
            Log.d("LOAD OPENCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d("LOAD OPENCV", "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }

        Bundle extras = getIntent().getExtras();
        String imagePath = extras.getString("sheet");

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap = BitmapFactory.decodeFile(imagePath, options);
        // to sum the black level in matrix
        Bitmap bmp = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(bmp, paper);

        SQLiteDatabase db_template = SQLiteDatabase.openDatabase(Template.DB_FILEPATH, null, OPEN_READONLY);
        ArrayList<Answer> allanswers = extractAllDbTemplateInfo(db_template);
        db_template.close();
        



    }

    public ArrayList<Answer> extractAllDbTemplateInfo(SQLiteDatabase db_template) {

        ArrayList<Answer> allAnswers = new ArrayList();
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
                answer.setLocationX(cursor.getInt(1));
                answer.setLocationY(cursor.getInt(2));
                answer.setHeight(cursor.getInt(3));
                answer.setWidth(cursor.getInt(4));
                answer.setSum_of_black(cursor.getInt(5));
                answer.setFlagCorrect(cursor.getInt(6));

                // add answer to list
                allAnswers.add(answer);

            } while (cursor.moveToNext());
        }
        return allAnswers;
    }

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

}
