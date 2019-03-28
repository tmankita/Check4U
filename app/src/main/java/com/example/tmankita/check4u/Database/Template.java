package com.example.tmankita.check4u.Database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.Nullable;

public class Template extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "template.db";
    public static final String TABLE_NAME = "template_table";
    public static final String COL_1 = "ID";
    public static final String COL_2 = "ANSWER_ID";
    public static final String COL_3 = "LOCATION_X";
    public static final String COL_4 = "LOCATION_Y";
    public static final String COL_5 = "HEIGHT";
    public static final String COL_6 = "WIDTH";
    public static final String COL_7 = "SUM_OF_BLACK_PIXELS";
    public static final String COL_8 = "FLAG_CORRECT";



    public Template(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {

    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }
}
