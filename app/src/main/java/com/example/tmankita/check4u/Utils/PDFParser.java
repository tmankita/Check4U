package com.example.tmankita.check4u.Utils;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;


import com.shockwave.pdfium.PdfDocument;
import com.shockwave.pdfium.PdfiumCore;
import com.tom_roush.pdfbox.cos.COSName;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageTree;
import com.tom_roush.pdfbox.pdmodel.PDResources;
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;
import com.tom_roush.pdfbox.pdmodel.graphics.PDXObject;
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject;
import com.tom_roush.pdfbox.rendering.PDFRenderer;
import com.tom_roush.pdfbox.rendering.PageDrawer;
import com.tom_roush.pdfbox.rendering.PageDrawerParameters;

import org.spongycastle.jcajce.provider.asymmetric.util.ExtendedInvalidKeySpecException;
import org.vudroid.core.DecodeServiceBase;
import org.vudroid.pdfdroid.codec.PdfContext;
import org.vudroid.pdfdroid.codec.PdfPage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import livroandroid.lib.utils.AndroidUtils;


public class PDFParser {
    Context mContext;
    String mOutputDir;
    ContentResolver contentResolver;

    public PDFParser(Context c, String desDir, ContentResolver r){
        mContext = c;
        mOutputDir = desDir;
        contentResolver = r;

    }

//
//    PDDocument document = PDDocument.load(new File(pdfFilename));
//    PDFRenderer pdfRenderer = new PDFRenderer(document);
//    int pageCounter = 0;
//for (PDPage page : document.getPages())
//    {
//        // note that the page number parameter is zero based
//        BufferedImage bim = pdfRenderer.renderImageWithDPI(pageCounter, 300, ImageType.RGB);
//
//        // suffix in filename will be used as the file format
//        ImageIOUtil.writeImage(bim, pdfFilename + "-" + (pageCounter++) + ".png", 300);
//    }
//document.close();


    public ArrayList<String> PDF2Jpeg3(Uri pdfUri){
        PdfiumCore pdfiumCore = new PdfiumCore(mContext);
        ArrayList<String> res = new ArrayList<>();
        try {
            ParcelFileDescriptor fd = contentResolver.openFileDescriptor(pdfUri, "r");
            PdfDocument pdfDocument = pdfiumCore.newDocument(fd);

            int PageIndex=0;
            while(pdfDocument.hasPage(PageIndex)){
                res.add(generateImageFromPdf(pdfiumCore,pdfDocument,PageIndex));
                PageIndex++;
            }

        }catch (Exception e){
            Log.e("pdf", e.getMessage());
        }
        return null;
    }






    //PdfiumAndroid (https://github.com/barteksc/PdfiumAndroid)
//https://github.com/barteksc/AndroidPdfViewer/issues/49

    String generateImageFromPdf(PdfiumCore pdfiumCore, PdfDocument pdfDocument , int pageNumber) {
        String res="";
        try {
            //http://www.programcreek.com/java-api-examples/index.php?api=android.os.ParcelFileDescriptor


            int width = pdfiumCore.getPageWidthPoint(pdfDocument, pageNumber);
            int height = pdfiumCore.getPageHeightPoint(pdfDocument, pageNumber);
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            pdfiumCore.renderPageBitmap(pdfDocument, bmp, pageNumber, 0, 0, width, height);
            res = saveImage(bmp,pageNumber);
            pdfiumCore.closeDocument(pdfDocument); // important!
        } catch(Exception e) {
            //todo with exception
        }
        return res;
    }

    private String saveImage(Bitmap bmp,int page) {
        FileOutputStream out = null;
        String res="";
        try {
            File folder = new File(mOutputDir);
            if(!folder.exists())
                folder.mkdirs();
            File file = new File(folder, "Test_+" +page+".png");
            out = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
            res = file.getAbsolutePath();

        } catch (Exception e) {
            //todo with exception
        } finally {
            try {
                if (out != null)
                    out.close();
            } catch (Exception e) {
                //todo with exception
            }
            return res;
        }
    }






    public ArrayList<String> PDF2Jpeg2(String pdfPath){

        ArrayList<String> res = new ArrayList<>();
        File sourceFile =  new File(pdfPath);
        try{

            PDDocument document = PDDocument.load(sourceFile);

            PDFRenderer renderer = new PDFRenderer(document);


            int NumberOfTests = document.getDocumentCatalog().getPages().getCount();

            FileOutputStream fileOut = null;
            int pageIndex = 0;
            while(pageIndex < NumberOfTests) {
                PDPageTree list = document.getPages();
                for (PDPage page : list) {
                    PDResources pdResources = page.getResources();
                    int i = 1;
                    for (COSName name : pdResources.getXObjectNames()) {
                        PDXObject o = pdResources.getXObject(name);
                        if (o instanceof PDImageXObject) {
                            PDImageXObject image = (PDImageXObject) o;
                            Bitmap bim = image.getImage();
                            File outputfile = new File(mOutputDir , "Test_" +(pageIndex+ 1)+ ".jpg");
                            fileOut = new FileOutputStream(outputfile);
                            bim.compress(Bitmap.CompressFormat.JPEG, 100, fileOut);
                            res.add(outputfile.getPath());

                        }
                    }
                }



//                = renderer.renderImageWithDPI(pageIndex, 300f);
//                File outputfile = new File(mOutputDir , "Test_" +(pageIndex+ 1)+ ".jpg");
//                fileOut = new FileOutputStream(outputfile);
//                bim.compress(Bitmap.CompressFormat.JPEG, 100, fileOut);

                pageIndex++;

            }
            fileOut.close();
            document.close();
        }catch (Exception e){
            Log.e("PDF",e.getMessage());
        }
        return res;

    }
//https://stackoverflow.com/questions/5570343/android-compatible-library-for-creating-image-from-pdf-file
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

//             do a fit center to 1920x1080
            double scaleBy = Math.min(1080 / (double) page.getWidth(), //
                    1920 / (double) page.getHeight());
//
            int width = (int) (page.getWidth()*scaleBy);
            int height = (int) (page.getHeight()*scaleBy);

            // you can change these values as you to zoom in/out
            // and even distort (scale without maintaining the aspect ratio)
            // the resulting images

            // Long running
            Bitmap bitmap = page.renderBitmap(width, height, rectF);

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
