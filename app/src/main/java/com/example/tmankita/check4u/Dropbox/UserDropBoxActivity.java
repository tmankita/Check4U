package com.example.tmankita.check4u.Dropbox;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;

import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.core.android.Auth;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.users.FullAccount;
import com.example.tmankita.check4u.R;
import com.example.tmankita.check4u.Utils.ZipManager;
import com.example.tmankita.check4u.oneByOneOrSeries;
import com.github.ybq.android.spinkit.sprite.Sprite;
import com.github.ybq.android.spinkit.style.FadingCircle;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * Activity that shows information about the currently logged in user
 */


public class UserDropBoxActivity extends DropBoxActivity{
    private String path;
    private String templatePath;
    private String inputPath;
    private ImageView loading;
    private Sprite fadingCircle;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_dropbox_user);
        loading = findViewById(R.id.Loading1);
        fadingCircle = new FadingCircle();
        fadingCircle.setColor(Color.WHITE);
        loading.setImageDrawable(fadingCircle);
        loading.setVisibility(View.INVISIBLE);
//        final String[] params = new String[2];
        Bundle bundle = getIntent().getExtras();
        path = bundle.getString("TemplateDataBase");
        templatePath = bundle.getString("templatePath");
        Button loginButton = (Button)findViewById(R.id.login_button);
        if(!createZipFile(path,templatePath)){
            Toast.makeText(this,"can't make zip file!!!", Toast.LENGTH_LONG).show();
        }

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Auth.startOAuth2Authentication(UserDropBoxActivity.this, getString(R.string.app_key));
            }
        });

        Button filesButton = (Button)findViewById(R.id.files_button);

        filesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loading.setVisibility(View.VISIBLE);
                loading.bringToFront();
                fadingCircle.start();
                fadingCircle.obtainAnimation();

                new UploadFileTask(getApplicationContext(), DropboxClientFactory.getClient(), new UploadFileTask.Callback() {
                    @Override
                    public void onUploadComplete(FileMetadata result) {
                        Toast.makeText(getApplicationContext(),"Template file uploaded succesfully",Toast.LENGTH_SHORT).show();
                        Intent oneByOneOrSeries = new Intent(getApplicationContext(), oneByOneOrSeries.class);
                        oneByOneOrSeries.putExtra("dbPath",inputPath);
                        startActivity(oneByOneOrSeries);
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(getApplicationContext(),"Error occurred while trying to upload to dropbox",Toast.LENGTH_SHORT).show();

                    }
                }).execute(inputPath);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (hasToken()) {
            findViewById(R.id.login_button).setVisibility(View.GONE);
            findViewById(R.id.email_text).setVisibility(View.VISIBLE);
            findViewById(R.id.name_text).setVisibility(View.VISIBLE);
            findViewById(R.id.type_text).setVisibility(View.VISIBLE);
            findViewById(R.id.files_button).setEnabled(true);
        } else {
            findViewById(R.id.login_button).setVisibility(View.VISIBLE);
            findViewById(R.id.email_text).setVisibility(View.GONE);
            findViewById(R.id.name_text).setVisibility(View.GONE);
            findViewById(R.id.type_text).setVisibility(View.GONE);
            findViewById(R.id.files_button).setEnabled(false);
        }
    }

    @Override
    protected void loadData() {
        new GetCurrentAccountTask(DropboxClientFactory.getClient(), new GetCurrentAccountTask.Callback() {
            @Override
            public void onComplete(FullAccount result) {
                ((TextView) findViewById(R.id.email_text)).setText(result.getEmail());
                ((TextView) findViewById(R.id.name_text)).setText(result.getName().getDisplayName());
                ((TextView) findViewById(R.id.type_text)).setText(result.getAccountType().name());
            }

            @Override
            public void onError(Exception e) {
                Log.e(getClass().getName(), "Failed to get account details.", e);
            }
        }).execute();
    }

    private boolean createZipFile(String... params){
        String localUriDB = params[0];
        String localUriTemplate = params[1];


        String[] s = new String[]{localUriDB,localUriTemplate};
        inputPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath()+ "/Check4U_DB/ZIP/"+getOutputName();

//        File StorageDir = new File(Environment.getExternalStorageDirectory(), "Check4U_DB");
//
//        // Create the storage directory if it does not exist
//        if (! StorageDir.exists()){
//            if (! StorageDir.mkdirs()){
//                Log.d("Check4U", "failed to create directory");
//                return false;
//            }
//        }

        ZipManager zipManager = new ZipManager();
        zipManager.zip(s, inputPath);
        File f = new File(inputPath);
        f.setReadable(true);
        return true;
    }
    private static String getOutputName(){

        // Create a Directory name
        String timeStamp ="Check4U_db_"+new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());


        return timeStamp+".zip";
    }
}
