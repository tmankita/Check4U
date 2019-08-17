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

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
        System.loadLibrary("opencv_java4");
    }

    private static final int  CAMERA_CONTINUES_REQUEST_CODE            = 1;
    private static final int  FILE_PICKER__CONTINUES_REQUEST_CODE      = 2;
    private ImageView icon;
    private Button test;
    String pathAppResult;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        icon = findViewById(R.id.check_icon);
        test = findViewById(R.id.TestTemplate);

        Bundle bundle = getIntent().getExtras();
        if(bundle!=null)
            pathAppResult = bundle.getString("test");

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

                            String pathCSV1 = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath() + "/Check4U_DB/TestCSV/1.csv";
                            String pathCSV2= Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath() + "/Check4U_DB/TestCSV/2.csv";
                            Compare2CSVTables c = new Compare2CSVTables(pathCSV1,pathCSV2);
                            c.compare();

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

        if (requestCode == CAMERA_CONTINUES_REQUEST_CODE) {
            if(resultCode == Activity.RESULT_OK){
                String[] imagesPath = data.getStringArrayExtra("sheets");
                Intent nextIntent = new Intent(getApplicationContext(), NewTemplateActivity.class);
                nextIntent.putExtra("sheets", imagesPath);
                startActivity(nextIntent);
            }
            if (resultCode == Activity.RESULT_CANCELED) {}

        } else if(requestCode == FILE_PICKER__CONTINUES_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Uri uri = data.getData();
                String p = uri.getPath();
                String[] parts = p.split(":");
                Intent nextIntent = new Intent(getApplicationContext(), oneByOneOrSeries.class);
                nextIntent.putExtra("dbPath", parts[1] );
                startActivity(nextIntent);
            }
            if (resultCode == Activity.RESULT_CANCELED) {}
        }

    }

    /**
     * Button for import Template from device
     * @param view is import button
     * @return None
     */
    public   void  ImportTemplate(View view) {
        Uri uri = Uri.parse(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath()
                + "/Check4U_DB/ZIP/");
        Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT,uri);
        chooseFile.setType("application/zip");
        chooseFile = Intent.createChooser(chooseFile, "Choose a file");
        startActivityForResult(chooseFile, FILE_PICKER__CONTINUES_REQUEST_CODE);
    }

    /**
     * Button for create new Template
     * @param view is new template button
     * @return None
     */
    public   void  NewTemplate(View view) {

//        Intent NewTemplate = new Intent(getApplicationContext(),TouchActivity.class);
//        NewTemplate.putExtra("caller","MainActivity");
//        startActivityForResult(NewTemplate,CAMERA_CONTINUES_REQUEST_CODE);

        Intent nextIntent = new Intent(getApplicationContext(), NewTemplateActivity.class);
        nextIntent.putExtra("sheets", "");
        startActivity(nextIntent);
    }
    /**
     * Button for testing the application  correctness
     * @param view is test template button
     * @return None
     */
    public   void  TestTemplate(View view) {
        File exportDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Check4U_DB");
        File csvDir = new File(exportDir.getPath(),"CSV");
        String path_test_table = csvDir.getAbsolutePath()+"/test.csv";
        File testM = new File(path_test_table);
        if(pathAppResult != null && testM.exists()){
            Compare2CSVTables test_table = new Compare2CSVTables(pathAppResult,path_test_table);
            test_table.compare();
        }
    }

    public native void align(long im, long imReference, long aligned);

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
