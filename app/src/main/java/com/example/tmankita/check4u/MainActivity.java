package com.example.tmankita.check4u;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.example.tmankita.check4u.Camera.TouchActivity;

public class MainActivity extends AppCompatActivity {

    private static final int  MY_CAMERA_REQUEST_CODE            = 100;


    public   void  ImportTemplate(View view) {
//        Intent Import = new Intent(getApplicationContext(), TouchActivity.class);
//        startActivity(Import);
    }
    public   void  NewTemplate(View view) {
        //TouchActivity.class
        //NewTemplateActivity.class

        Intent NewTemplate = new Intent(getApplicationContext(),TouchActivity.class);
        startActivityForResult(NewTemplate,1);
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


    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == 1) {
            if(resultCode == Activity.RESULT_OK){
                String path=data.getStringExtra("sheet");
                Intent nextIntent = new Intent(getApplicationContext(), NewTemplateActivity.class);
                nextIntent.putExtra("sheet", path);
                startActivity(nextIntent);
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                //Write your code if there's no result
            }
        }
    }//onActivityResult

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
