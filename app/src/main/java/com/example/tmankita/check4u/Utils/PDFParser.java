package com.example.tmankita.check4u.Utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.net.Uri;


import org.vudroid.core.DecodeServiceBase;
import org.vudroid.pdfdroid.codec.PdfContext;
import org.vudroid.pdfdroid.codec.PdfPage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class PDFParser {
    Context mContext;
    String mOutputDir;

    public PDFParser(Context c, String desDir){
        mContext = c;
        mOutputDir = desDir;
    }

    public ArrayList<String> PDF2Jpeg(String pdfPath) {

        DecodeServiceBase decodeService = new DecodeServiceBase(new PdfContext());
        decodeService.setContentResolver(mContext.getContentResolver());

        // a bit long running
        decodeService.open(Uri.fromFile(new File(pdfPath)));
        ArrayList<String> testImagesPath = new ArrayList<>();
        int pageCount = decodeService.getPageCount();
        for (int i = 0; i < pageCount; i++) {
            PdfPage page =(PdfPage) decodeService.getPage(i);
            RectF rectF = new RectF(0, 0, 1, 1);

            // do a fit center to 1920x1080
//            double scaleBy = Math.min(AndroidUtils.PHOTO_WIDTH_PIXELS / (double) page.getWidth(), //
//                    AndroidUtils.PHOTO_HEIGHT_PIXELS / (double) page.getHeight());

            int with = (int) (page.getWidth());
            int height = (int) (page.getHeight());

            // you can change these values as you to zoom in/out
            // and even distort (scale without maintaining the aspect ratio)
            // the resulting images

            // Long running
            Bitmap bitmap = page.renderBitmap(with, height, rectF);

            try {

                File outputFile = new File(mOutputDir, System.currentTimeMillis() + ".jpg");
                FileOutputStream outputStream = new FileOutputStream(outputFile);

                // a bit long running
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);

                outputStream.close();

                testImagesPath.add(outputFile.getPath());

            } catch (IOException e) {

            }
        }
        return testImagesPath;
    }
}
