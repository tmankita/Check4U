package com.example.tmankita.check4u;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.example.tmankita.check4u.Camera.TouchActivity;
import com.example.tmankita.check4u.Utils.Compare2CSVTables;

import java.io.File;


public class MainActivity extends AppCompatActivity {

    private static final int  MY_CAMERA_REQUEST_CODE            = 100;
    private ImageView icon;
    private Button test;
    String pathTest;

    /**
     * Button for import Template from device
     */
    public   void  ImportTemplate(View view) {
        Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
        chooseFile.setType("application/zip");
        chooseFile = Intent.createChooser(chooseFile, "Choose a file");
        startActivityForResult(chooseFile, 2);
    }

    /**
     * Button for create new Template
     */
    public   void  NewTemplate(View view) {
        Intent NewTemplate = new Intent(getApplicationContext(),TouchActivity.class);
        NewTemplate.putExtra("caller","MainActivity");
        startActivityForResult(NewTemplate,1);
    }

    /**
     * Button for testing the application  correctness
     */
    public   void  TestTemplate(View view) {
        File exportDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Check4U_DB");
        File csvDir = new File(exportDir.getPath(),"CSV");
        String path_manual_table = csvDir.getAbsolutePath()+"/test.csv";
        File testM = new File(path_manual_table);
        if(pathTest != null && testM.exists()){
            Compare2CSVTables test_table = new Compare2CSVTables(pathTest,path_manual_table);
            test_table.compare();
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        icon = findViewById(R.id.check_icon);
        test = findViewById(R.id.TestTemplate);

        Bundle bundle = getIntent().getExtras();
        pathTest = bundle.getString("test");

        icon.setOnTouchListener(new View.OnTouchListener() {

            private long lastClickTime = 0;
            private static final long DOUBLE_CLICK_TIME_DELTA = 300;//milliseconds
            private long clickTime;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        clickTime = System.currentTimeMillis();
                        //double tapping mode
                        if (clickTime - lastClickTime < DOUBLE_CLICK_TIME_DELTA){
                            Log.i("Mark", "Double tap!! ");
                            test.setVisibility(View.INVISIBLE);

                        }else{
                            lastClickTime = clickTime;
                        }

                }
                return true;
            }
        });

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
            if (resultCode == Activity.RESULT_CANCELED) {}

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
        }

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
