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
    public static final String COL_2 = "GRADE";
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
    public boolean insertRaw(int id, int[] answers ,int[] correctnesFlags , double score){
        double grade=0;
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_1,id);
        for (int i = 0; i < numberOfQuestions; i++) {
            contentValues.put("QUESTION_"+(i+1),answers[i]);
            grade = grade +score*correctnesFlags[i];
        }
        contentValues.put(COL_2,grade);

        long result = db.insert(TABLE_NAME,null,contentValues);
        if(result == -1)
            return false;
        else
            return true;
    }

    private String generateCreateQuery(){
        int i;
        String result = "create table "+ TABLE_NAME + "(ID INTEGER PRIMARY KEY , GRADE INTEGER , ";
        for (i = 0; i < numberOfQuestions-1 ; i++) {
            result = result + "QUESTION_"+ i+1 +" INTEGER , ";

        }
        result = result + "QUESTION_"+ i+1 +"INTEGER )";

        return result;
    }
    public String getFilePath (){
        return DB_FILEPATH;
    }

}
