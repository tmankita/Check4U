package com.example.tmankita.check4u.Dropbox;


import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.WriteMode;
import com.example.tmankita.check4u.Utils.ZipManager;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;

class UploadFileTask extends AsyncTask<String, Void, FileMetadata> {

    private final Context mContext;
    private final DbxClientV2 mDbxClient;
    private final Callback mCallback;
    private Exception mException;
    private static final int BUFFER = 80000;

    public interface Callback {
        void onUploadComplete(FileMetadata result);
        void onError(Exception e);
    }

    UploadFileTask(Context context, DbxClientV2 dbxClient, Callback callback) {
        mContext = context;
        mDbxClient = dbxClient;
        mCallback = callback;
    }
    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        ProgressDialog pd = new ProgressDialog(this.mContext);
        pd.setTitle("Uploading Data");
        pd.setMessage("Please wait, data is sending");
        pd.setCancelable(false);
        pd.setIndeterminate(true);
        pd.show();
    }

    @Override
    protected void onPostExecute(FileMetadata result) {
        super.onPostExecute(result);
        Log.e("MYAPP", "here");
        if (mException != null) {
            mCallback.onError(mException);
        } else if (result == null) {
            mCallback.onError(null);
        } else {
            mCallback.onUploadComplete(result);
        }
    }

    @Override
    protected FileMetadata doInBackground(String... params) {

        String localUri = params[0];
        String caller = params[1];

        File localZip = UriHelpers.getFileForUri(mContext, Uri.fromFile(new File(localUri)));

        if (localZip != null && caller!=null) {
            String remoteFolderPath=null;

            if(caller.equals("newTemplate")) {
                remoteFolderPath = "/databases/";
            }else if(caller.equals("oneByOne")){
                remoteFolderPath = "/CSV/";
            }


            // Note - this is not ensuring the name is a valid dropbox file name
            String remoteFileZipName = localZip.getName();

            try (InputStream inputStream = new FileInputStream(localZip)) {
                return mDbxClient.files().uploadBuilder(remoteFolderPath + remoteFileZipName)
                        .withMode(WriteMode.OVERWRITE)
                        .uploadAndFinish(inputStream);
            } catch (DbxException | IOException e) {
                mException = e;
            }
        }

        return null;
    }



}


