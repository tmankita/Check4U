package com.example.tmankita.check4u.Database;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class StudentDataBase extends SQLiteOpenHelper {

    public static String DB_FILEPATH = "/data/data/com.example.tmankita.check4u/databases/studentDatabase.db";
    public static final String DATABASE_NAME = "studentDatabase.db";
    public static final String TABLE_NAME = "students_table";
    public static final String COL_1 = "ID";
    public static final String COL_N = "GRADE";
    public int numberOfQuestions;


    public StudentDataBase(Context context, int numberOfQuestions ){ // @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {
        super(context, DATABASE_NAME, null, 1);
        this.numberOfQuestions = numberOfQuestions;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String create_table_query = generateCreateQuery();
        db.execSQL(create_table_query);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS "+TABLE_NAME);
        onCreate(db);
    }
    public double insertRaw(int id, int[] answers ,int[] correctnesFlags , int score){
        int grade=0;
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_1,id);
        for (int i = 1; i < numberOfQuestions+1; i++) {
            contentValues.put("QUESTION_"+(i),answers[i]);
            grade = grade +score*correctnesFlags[i];
        }
        contentValues.put(COL_N,grade);

        long result = db.insert(TABLE_NAME,null,contentValues);
        if(result == -1)
            return -1;
        else
            return grade;
    }

    
    private String generateCreateQuery(){
        int i;
        int j;
        String result = "create table "+ TABLE_NAME + "(ID INTEGER PRIMARY KEY , ";

        if(numberOfQuestions == 1)
            result = result + "QUESTION_"+ 1 +" INTEGER , GRADE INTEGER)";
        else {
            for (i = 0; i < numberOfQuestions - 1; i++) {
                j=i + 1;
                result = result + "QUESTION_" + j + " INTEGER , ";

            }
            j=i + 1;
            result = result + "QUESTION_" + j + " INTEGER , GRADE INTEGER)";
        }

        return result;
    }
    public String getFilePath (){
        return DB_FILEPATH;
    }

}
