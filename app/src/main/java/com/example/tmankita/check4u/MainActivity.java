package com.example.tmankita.check4u;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.example.tmankita.check4u.Camera.TouchActivity;

public class MainActivity extends AppCompatActivity {

    private static final int  MY_CAMERA_REQUEST_CODE            = 100;


    public   void  ImportTemplate(View view) {
        Intent Import = new Intent(getApplicationContext(), TouchActivity.class);
        startActivity(Import);
    }
    public   void  NewTemplate(View view) {

        Intent NewTemplate = new Intent(getApplicationContext(),NewTemplateActivity.class);
        startActivity(NewTemplate);
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        int PERMISSION_ALL = 1;
        String[] PERMISSIONS = {
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.CAMERA
        };

        if(!hasPermissions(this, PERMISSIONS)){
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }

//        if (checkSelfPermission(Manifest.permission.CAMERA)
//                != PackageManager.PERMISSION_GRANTED) {
//            requestPermissions(new String[]{Manifest.permission.CAMERA},
//                    MY_CAMERA_REQUEST_CODE);
//        }
//
//        if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
//                != PackageManager.PERMISSION_GRANTED) {
//            requestPermissions( new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},100);
//        }
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }
}
