package com.example.tmankita.check4u.Dropbox;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;

import android.os.Environment;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.core.android.Auth;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.users.FullAccount;
import com.example.tmankita.check4u.Database.StudentDataBase;
import com.example.tmankita.check4u.MainActivity;
import com.example.tmankita.check4u.R;
import com.example.tmankita.check4u.Utils.ZipManager;
import com.example.tmankita.check4u.oneByOneOrSeries;
import com.github.ybq.android.spinkit.sprite.Sprite;
import com.github.ybq.android.spinkit.style.FadingCircle;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.database.sqlite.SQLiteDatabase.OPEN_READONLY;


/**
 * Activity that shows information about the currently logged in user
 */


public class UserDropBoxActivity extends DropBoxActivity{
    private String path;
    private String templatePath;
    private String inputPath;
    private String caller;
    private TableLayout dialog_delete;
    private LinearLayout shareMenu;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Context c = this;
        setContentView(R.layout.activity_dropbox_user);

        dialog_delete = (TableLayout)findViewById(R.id.table_dialog_delete);
        shareMenu     = (LinearLayout) findViewById(R.id.share_menu);
        Button loginButton = (Button) findViewById(R.id.login_button);


        Bundle bundle = getIntent().getExtras();
        caller = bundle.getString("caller");


        if(caller.equals("oneByOne")){
            inputPath = bundle.getString("CSV");
        }
        else if(caller.equals("newTemplate")) {
            path = bundle.getString("TemplateDataBase");
            templatePath = bundle.getString("templatePath");
            if (!createZipFile(path, templatePath)) {
                Toast.makeText(this, "can't make zip file!!!", Toast.LENGTH_LONG).show();
            }
        }

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Auth.startOAuth2Authentication(UserDropBoxActivity.this, getString(R.string.app_key));
            }
        });

        Button filesButton = (Button)findViewById(R.id.files_button);
        Button anotherShareButton = (Button)findViewById(R.id.another_share_button);
        Button noShareButton = (Button)findViewById(R.id.no_share);

        noShareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(caller.equals("newTemplate")){
                    Intent oneByOneOrSeries = new Intent(c, oneByOneOrSeries.class);
                    oneByOneOrSeries.putExtra("dbPath",inputPath);
                    startActivity(oneByOneOrSeries);
                }else if(caller.equals("oneByOne")) {
                    checkIfWantToDelete();
                }
            }
        });

        anotherShareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                File file = new File(inputPath);
                Uri path = Uri.fromFile(file);

                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent .setType("text/plain");

                shareIntent .putExtra(Intent.EXTRA_EMAIL, "");
// the attachment
                shareIntent.putExtra(Intent.EXTRA_STREAM, path);
// the mail subject
                shareIntent .putExtra(Intent.EXTRA_SUBJECT, "Check4U - Grades DataBase");

                StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
                StrictMode.setVmPolicy(builder.build());

                startActivityForResult(Intent.createChooser(shareIntent , "Share"),1);
            }
        });

        filesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                new UploadFileTask(c, DropboxClientFactory.getClient(), new UploadFileTask.Callback() {
                    @Override
                    public void onUploadComplete(FileMetadata result) {
                        if(caller.equals("newTemplate")){
                            Toast.makeText(c,"Template file uploaded succesfully",Toast.LENGTH_SHORT).show();
                            Intent oneByOneOrSeries = new Intent(c, oneByOneOrSeries.class);
                            oneByOneOrSeries.putExtra("dbPath",inputPath);
                            startActivity(oneByOneOrSeries);
                        }else if(caller.equals("oneByOne")) {
                            Toast.makeText(c,"CSV file uploaded succesfully",Toast.LENGTH_LONG).show();
                            checkIfWantToDelete();
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(c,"Error occurred while trying to upload to dropbox",Toast.LENGTH_SHORT).show();

                    }
                }).execute(inputPath,caller);
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

    public void yes_dialog(View view){
        final ProgressDialog pd = new ProgressDialog(this);
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                pd.setTitle("Delete all the images and unzip directories");
                pd.setMessage("Please wait");
                pd.setCancelable(false);
                pd.setIndeterminate(true);
                pd.show();
            }
            @Override
            protected Void doInBackground( final Void ... params ) {
                //erase all the pictures and unzip directories
                // the db not to erase
                removeImagesAndUnzipDirectories();
                return null;
            }

            @Override
            protected void onPostExecute( final Void result ) {
                pd.dismiss();
                startActivity(new Intent(getApplicationContext(), MainActivity.class));

            }
        }.execute();
    }

    public void no_dialog(View view){
        startActivity(new Intent(getApplicationContext(), MainActivity.class));
    }

    private boolean createZipFile(String... params){
        String localUriDB = params[0];
        String localUriTemplate = params[1];


        String[] s = new String[]{localUriDB,localUriTemplate};
        inputPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath()+ "/Check4U_DB/ZIP/"+getOutputName();

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            checkIfWantToDelete();
        }
    }

    private void checkIfWantToDelete(){
        shareMenu.setVisibility(View.INVISIBLE);
        dialog_delete.setVisibility(View.VISIBLE);

    }

    private void removeImagesAndUnzipDirectories(){
        String DCIM_Path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath()+ "/Check4U_DB/DCIM/";
        String UNZIP_Directory_Path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath()+ "/Check4U_DB/UNZIP/";

        File DCIM_Dir = new File(DCIM_Path);
        File UNZIP_Dir = new File(UNZIP_Directory_Path);
        for (File f : DCIM_Dir.listFiles()) {
            if (f.isFile())
                f.delete();

        }
        for (File d : UNZIP_Dir.listFiles()) {
            if (d.isDirectory())
                d.delete();
        }

    }
}
