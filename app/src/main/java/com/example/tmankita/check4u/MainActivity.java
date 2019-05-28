package com.example.tmankita.check4u;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;

import com.example.tmankita.check4u.Camera.TouchActivity;

import java.io.File;



public class MainActivity extends AppCompatActivity {

    private static final int  MY_CAMERA_REQUEST_CODE            = 100;


    public   void  ImportTemplate(View view) {
        Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
        chooseFile.setType("application/zip");
        chooseFile = Intent.createChooser(chooseFile, "Choose a file");
        startActivityForResult(chooseFile, 2);

    }
    public   void  NewTemplate(View view) {
        Intent NewTemplate = new Intent(getApplicationContext(),TouchActivity.class);
        NewTemplate.putExtra("caller","MainActivity");
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

        File StorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Check4U_DB");


        if (! StorageDir.exists()){
            if (! StorageDir.mkdirs()){
                Log.d("Check4U", "failed to create directory StorageDir");
            }
        }

        File zipDir = new File(StorageDir.getPath(),"ZIP");
        File unzipDir = new File(StorageDir.getPath(),"UNZIP");
        File csvDir = new File(StorageDir.getPath(),"CSV");
        File imagesDir = new File(StorageDir.getPath(),"DCIM");
        if (! zipDir.exists()){
            if (! zipDir.mkdirs()){
                Log.d("Check4U", "failed to create directory zipDir");
            }
        }
        if (! unzipDir.exists()){
            if (! unzipDir.mkdirs()){
                Log.d("Check4U", "failed to create directory unzipDir");
            }
        }
        if (! csvDir.exists()){
            if (! csvDir.mkdirs()){
                Log.d("Check4U", "failed to create directory csvDir");
            }
        }
        if (! imagesDir.exists()){
            if (! imagesDir.mkdirs()){
                Log.d("Check4U", "failed to create directory imagesDir");
            }
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
        } else if(requestCode == 2) {
            if (resultCode == RESULT_OK) {
                Log.e("CHECKL4U","RESULT PICK FILE");
                Uri uri = data.getData();
                String p = uri.getPath();
                String[] parts = p.split(":");
                Intent nextIntent = new Intent(getApplicationContext(), oneByOneOrSeries.class);
                nextIntent.putExtra("dbPath", parts[1] );
                startActivity(nextIntent);

            }


//            if(data!= null) {
//
//                ArrayList<MediaFile> file = data.getParcelableArrayListExtra(FilePickerActivity.MEDIA_FILES);
//
//                //Do something with files
//                String pathImportDB = file.get(0).getPath();
//                Intent nextIntent = new Intent(getApplicationContext(), oneByOneOrSeries.class);
//                nextIntent.putExtra("dbPath", pathImportDB);
//                startActivity(nextIntent);
//            }

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
