package com.example.tmankita.check4u.Detectors;

import android.graphics.Bitmap;
import android.graphics.Matrix;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.example.tmankita.check4u.Detectors.detectDocument.sortPoints;


public class TemplateMatching {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
        System.loadLibrary("opencv_java4");
    }

    public native void templateMatcher(long im, long imReference, long ps);

    public TemplateMatching(){}


    public Mat match2 (Mat template, Mat image){

        Mat grayTemplate = new Mat(template.size(), CvType.CV_8UC1);
        Mat grayImage = new Mat(template.size(), CvType.CV_8UC1);

        Mat resizeImage = new Mat(grayTemplate.size(), CvType.CV_8UC1);
        Imgproc.resize(image,resizeImage,grayTemplate.size());

        resizeImage.copyTo(grayImage);
        Imgproc.cvtColor(template, grayTemplate, Imgproc.COLOR_RGB2GRAY, 4);


        MatOfPoint2f Ps = new MatOfPoint2f();
        Mat Ps1 = Ps;
        templateMatcher(grayImage.nativeObj, grayTemplate.nativeObj, Ps1.nativeObj);


        Point[] temp = ((MatOfPoint2f)Ps1).toArray();
        Point[] sorted = sortPoints((Point[])temp);



        Imgproc.line(image,sorted[0],sorted[1],new Scalar(0, 0, 255, 150), 9);
        Imgproc.line(image,sorted[1],sorted[2],new Scalar(0, 0, 255, 150), 9);
        Imgproc.line(image,sorted[2],sorted[3],new Scalar(0, 0, 255, 150), 9);
        Imgproc.line(image,sorted[3],sorted[0],new Scalar(0, 0, 255, 150), 9);



        Mat match = fourPointTransform_match(resizeImage,sorted);


        Bitmap bmpPaper11 = Bitmap.createBitmap(image.cols(), image.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(image, bmpPaper11);

        Bitmap bmpPaper = Bitmap.createBitmap(match.cols(), match.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(match, bmpPaper);

        return match;
    }




    public Mat normalization (Mat input){
        Size size = new Size(input.size().width,input.size().height);
        Mat canneyImage = new Mat(size, CvType.CV_8UC1);
        Mat bilateralFilterImage = new Mat(size, CvType.CV_8UC1);
        Mat adaptiveThresholdImage = new Mat(size, CvType.CV_8UC1);
        Mat medianBlurImage = new Mat(size, CvType.CV_8UC1);
        // Bilateral filter preserve edges
        Imgproc.bilateralFilter(input ,bilateralFilterImage, 9, 75, 75);
        // Create black and white image based on adaptive threshold
        Imgproc.adaptiveThreshold(bilateralFilterImage,adaptiveThresholdImage, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 115, 4);
        // Median filter clears small details
        Imgproc.medianBlur(adaptiveThresholdImage,medianBlurImage, 11);
        Imgproc.Canny(medianBlurImage, canneyImage, 250, 200);



        bilateralFilterImage.release();
        adaptiveThresholdImage.release();
        medianBlurImage.release();

        return canneyImage;
    }


    public Point[] match (Mat template, Mat image, String mode){

        Size sizeTemplate = new Size(template.cols(),template.rows());
        Size sizeImage = new Size(image.cols(),image.rows());


        Mat grayTemplate = new Mat(sizeTemplate, CvType.CV_8UC1);
        Mat grayImage = new Mat(sizeImage, CvType.CV_8UC1);



        if(mode.equals("template"))
            Imgproc.cvtColor(image, grayImage, Imgproc.COLOR_RGB2GRAY, 4);
        else if(mode.equals("image"))
            image.copyTo(grayImage);


        Bitmap bmpPaper13 = Bitmap.createBitmap(template.cols(), template.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(template, bmpPaper13);

        Imgproc.cvtColor(template, grayTemplate, Imgproc.COLOR_RGB2GRAY, 4);

        Bitmap bmpPaper12 = Bitmap.createBitmap(grayTemplate.cols(), grayTemplate.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(grayTemplate, bmpPaper12);

//        Mat resizeTemplate = new Mat(resizeTemplateSize, CvType.CV_8UC1);
//        Imgproc.resize(grayTemplate,resizeTemplate,resizeTemplateSize);

//        double scale = 1;
//        Mat resizeImage = new Mat(sizeImage, CvType.CV_8UC1);
//        while((resizeImage.cols() - grayTemplate.cols()) < 0 || (resizeImage.rows() - grayTemplate.rows()) < 0){
//            double height = scale * resizeImage.size().height;
//            double width = scale * resizeImage.size().width;
//            Size ds = new Size(width,height);
//            resizeImage.release();
//            resizeImage = new Mat(ds, CvType.CV_8UC1);
//            Imgproc.resize( grayImage , resizeImage, ds);
//            scale += 0.00001;
//        }
//
//        scale -= 0.00001;

//        Mat norTemplate = normalization(grayTemplate);
//        Mat norImage = normalization(resizeImage);



        Mat Template = new Mat(sizeTemplate, CvType.CV_8UC1);
        Mat Image = new Mat(grayImage.size(), CvType.CV_8UC1);
        grayImage.copyTo(Image);
        grayTemplate.copyTo(Template);




//        Mat padingImage = new Mat(sizeTemplate, CvType.CV_8UC1);
//        Imgproc.resize(image,padingImage,sizeTemplate);

//        Bitmap bmpPaper1 = Bitmap.createBitmap(template.cols(), template.rows(), Bitmap.Config.ARGB_8888);
//        Utils.matToBitmap(template, bmpPaper1);
//        Bitmap bmp_template = Bitmap.createBitmap(bmpPaper1, 0, 0, bmpPaper1.getWidth(), bmpPaper1.getHeight(), new Matrix(), true);
//
//        Bitmap bmpPaper2 = Bitmap.createBitmap(image.cols(), image.rows(), Bitmap.Config.ARGB_8888);
//        Utils.matToBitmap(image, bmpPaper2);
//        Bitmap bmp_image = Bitmap.createBitmap(bmpPaper2, 0, 0, bmpPaper2.getWidth(), bmpPaper2.getHeight(), new Matrix(), true);

//        Bitmap bmpPaper3 = Bitmap.createBitmap(image.cols(), image.rows(), Bitmap.Config.ARGB_8888);
//        Utils.matToBitmap(image, bmpPaper3);
//        Bitmap bmp_paddingImage = Bitmap.createBitmap(bmpPaper3, 0, 0, bmpPaper3.getWidth(), bmpPaper3.getHeight(), new Matrix(), true);


        // / Create the result matrix
        int result_cols = Image.cols() - Template.cols() + 1;
        int result_rows = Image.rows() - Template.rows() + 1;
        Mat result = new Mat(result_rows, result_cols, CvType.CV_32FC1);

        int match_method = Imgproc.TM_CCOEFF_NORMED; //CV_TM_SQDIFF, CV_TM_SQDIFF_NORMED, CV_TM_CCORR, CV_TM_CCORR_NORMED, CV_TM_CCOEFF, CV_TM_CCOEFF_NORMED

        // / Do the Matching and Normalize
        Imgproc.matchTemplate(Image, Template, result, match_method);
        Core.normalize(result, result, 0, 1, Core.NORM_MINMAX, -1, new Mat());

        // / Localizing the best match with minMaxLoc
        Core.MinMaxLocResult mmr = Core.minMaxLoc(result);

        Point matchLoc;
        if (match_method == Imgproc.TM_SQDIFF || match_method == Imgproc.TM_SQDIFF_NORMED) {
            matchLoc = mmr.minLoc;
        } else {
            matchLoc = mmr.maxLoc;
        }

        Point[] ps = new Point[]{
                new Point(matchLoc.x,matchLoc.y),
                new Point(matchLoc.x+template.cols(),matchLoc.y),
                new Point(matchLoc.x, matchLoc.y +Template.rows()),
                new Point(matchLoc.x+Template.cols(),matchLoc.y +Template.rows() )
        };
        Point[] sorted = sortPoints(ps);


        Imgproc.line(image,sorted[0],sorted[1],new Scalar(0, 0, 255, 150), 4);
        Imgproc.line(image,sorted[1],sorted[2],new Scalar(0, 0, 255, 150), 4);
        Imgproc.line(image,sorted[2],sorted[3],new Scalar(0, 0, 255, 150), 4);
        Imgproc.line(image,sorted[3],sorted[0],new Scalar(0, 0, 255, 150), 4);


//        Mat match = fourPointTransform_match(resizeImage,sorted);
//
//        Size resultPSize = new Size (match.size().width/scale,match.size().height/scale);
//        Mat resultP = new Mat(resultPSize, CvType.CV_8UC1);
//        Imgproc.resize(match,resultP,resultPSize);

        Bitmap bmpPaper11 = Bitmap.createBitmap(image.cols(), image.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(image, bmpPaper11);

//        Bitmap bmpPaper = Bitmap.createBitmap(resultP.cols(), resultP.rows(), Bitmap.Config.ARGB_8888);
//        Utils.matToBitmap(resultP, bmpPaper);
//        Bitmap bOutput = Bitmap.createBitmap(bmpPaper, 0, 0, bmpPaper.getWidth(), bmpPaper.getHeight(), new Matrix(), true);

        return sorted;

        // / Show me what you got
//        Core.rectangle(img, matchLoc, new Point(matchLoc.x + templ.cols(),
//                matchLoc.y + templ.rows()), new Scalar(0, 255, 0));



    }
    public static Mat fourPointTransform_match( Mat src , Point[] pts ) {

        Point tl = pts[0];
        Point tr = pts[1];
        Point br = pts[2];
        Point bl = pts[3];

        double widthA = Math.sqrt(Math.pow(br.x - bl.x, 2) + Math.pow(br.y - bl.y, 2));
        double widthB = Math.sqrt(Math.pow(tr.x - tl.x, 2) + Math.pow(tr.y - tl.y, 2));

        double dw = Math.max(widthA, widthB);
        int maxWidth = Double.valueOf(dw).intValue();


        double heightA = Math.sqrt(Math.pow(tr.x - br.x, 2) + Math.pow(tr.y - br.y, 2));
        double heightB = Math.sqrt(Math.pow(tl.x - bl.x, 2) + Math.pow(tl.y - bl.y, 2));

        double dh = Math.max(heightA, heightB);
        int maxHeight = Double.valueOf(dh).intValue();

        Mat doc = new Mat(maxHeight, maxWidth, CvType.CV_8UC4);

        Mat src_mat = new Mat(4, 1, CvType.CV_32FC2);
        Mat dst_mat = new Mat(4, 1, CvType.CV_32FC2);

//        src_mat.put(0, 0, tl.x*ratio, tl.y*ratio, tr.x*ratio, tr.y*ratio, br.x*ratio, br.y*ratio, bl.x*ratio, bl.y*ratio);

        src_mat.put(0, 0, tl.x, tl.y, tr.x, tr.y, br.x, br.y, bl.x, bl.y);
        dst_mat.put(0, 0, 0.0, 0.0, dw, 0.0, dw, dh, 0.0, dh);

        Mat m = Imgproc.getPerspectiveTransform(src_mat, dst_mat);

        Imgproc.warpPerspective(src, doc, m, doc.size());

        return doc;
    }

}
