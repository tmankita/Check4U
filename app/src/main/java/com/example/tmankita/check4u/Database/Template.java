package com.example.tmankita.check4u.Database;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class Template extends SQLiteOpenHelper {
    public static String DB_FILEPATH = "/data/data/com.example.tmankita.check4u/databases/template.db";
    public static final String DATABASE_NAME = "template.db";
    public static final String TABLE_NAME = "template_table";
    public static final String COL_1 = "ID";
    public static final String COL_2 = "LOCATION_X";
    public static final String COL_3 = "LOCATION_Y";
    public static final String COL_4 = "HEIGHT";
    public static final String COL_5 = "WIDTH";
    public static final String COL_6 = "SUM_OF_BLACK";
    public static final String COL_7 = "FLAG_CORRECT";



    public Template( Context context){ // @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table "+ TABLE_NAME + "(ID INTEGER PRIMARY KEY , LOCATION_X INTEGER , LOCATION_Y INTEGER, " +
                "HEIGHT INTEGER, WIDTH INTEGER, SUM_OF_BLACK INTEGER, FLAG_CORRECT INTEGER )");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        db.execSQL("DROP TABLE IF EXISTS "+TABLE_NAME);
        onCreate(db);
    }

    public boolean insertData(int id, int location_x, int location_y, int height, int width, int sum_of_black, int correct){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_1,id);
        contentValues.put(COL_2,location_x);
        contentValues.put(COL_3,location_y);
        contentValues.put(COL_4,height);
        contentValues.put(COL_5,width);
        contentValues.put(COL_6,sum_of_black);
        contentValues.put(COL_7,correct);
        long result = db.insert(TABLE_NAME,null,contentValues);
        if(result == -1)
            return false;
        else
            return true;
    }
    //https://stackoverflow.com/questions/6540906/simple-export-and-import-of-a-sqlite-database-on-android
    /**
     * Copies the database file at the specified location over the current
     * internal application database.
     * */
    public boolean importDatabase(String dbPath) throws IOException {

        // Close the SQLiteOpenHelper so it will commit the created empty
        // database to internal storage.
        close();
        File newDb = new File(dbPath);
        File oldDb = new File(DB_FILEPATH);
        if (newDb.exists()) {
            FileUtils.copyFile(new FileInputStream(newDb), new FileOutputStream(oldDb));
            // Access the copied database so SQLiteHelper will cache it and mark
            // it as created.
            getWritableDatabase().close();
            return true;
        }
        return false;
    }

    public String getFilePath (){
        return DB_FILEPATH;
    }

}
