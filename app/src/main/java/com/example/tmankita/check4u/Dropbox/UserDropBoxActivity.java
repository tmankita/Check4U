package com.example.tmankita.check4u.Dropbox;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.core.android.Auth;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.users.FullAccount;
import com.example.tmankita.check4u.R;
import com.example.tmankita.check4u.oneByOneOrSeries;


/**
 * Activity that shows information about the currently logged in user
 */


public class UserDropBoxActivity extends DropBoxActivity{
    private String path;
    private double score;
    private int numberOfQuestions;
    private int numberOfAnswers;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_dropbox_user);
//        final String[] params = new String[2];
        Bundle bundle = getIntent().getExtras();
        path = bundle.getString("TemplateDataBase");
        score = bundle.getDouble("score");
        numberOfQuestions = bundle.getInt("numberOfQuestions");
        Button loginButton = (Button)findViewById(R.id.login_button);

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
                new UploadFileTask(getApplicationContext(), DropboxClientFactory.getClient(), new UploadFileTask.Callback() {
                    @Override
                    public void onUploadComplete(FileMetadata result) {
                        Toast.makeText(getApplicationContext(),"Template file uploaded succesfully",Toast.LENGTH_SHORT).show();
                        Intent oneByOneOrSeries = new Intent(getApplicationContext(), oneByOneOrSeries.class);
                        oneByOneOrSeries.putExtra("score",score);
                        oneByOneOrSeries.putExtra("numberOfQuestions",numberOfQuestions);
                        oneByOneOrSeries.putExtra("numberOfAnswers",numberOfAnswers);

                        startActivity(oneByOneOrSeries);
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(getApplicationContext(),"Error occurred while trying to upload to dropbox",Toast.LENGTH_SHORT).show();

                    }
                }).execute(path);
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
}
