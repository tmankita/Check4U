package com.example.tmankita.check4u.Detectors;

import android.graphics.Bitmap;
import android.graphics.Matrix;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class TemplateMatching {

    public TemplateMatching(){}

    public Mat match (Mat template, Mat image){
        Size sizeTemplate = new Size(template.cols(),template.rows());
        Mat grayTemplate = new Mat(sizeTemplate, CvType.CV_8UC1);
        Imgproc.cvtColor(template, grayTemplate, Imgproc.COLOR_RGB2GRAY, 4);
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
        int result_cols = image.cols() - template.cols() + 1;
        int result_rows = image.rows() - template.rows() + 1;
        Mat result = new Mat(result_rows, result_cols, CvType.CV_32FC1);

        int match_method = Imgproc.TM_CCOEFF_NORMED; //CV_TM_SQDIFF, CV_TM_SQDIFF_NORMED, CV_TM_CCORR, CV_TM_CCORR_NORMED, CV_TM_CCOEFF, CV_TM_CCOEFF_NORMED

        // / Do the Matching and Normalize
        Imgproc.matchTemplate(image, grayTemplate, result, match_method);
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
                new Point(matchLoc.x, matchLoc.y +grayTemplate.rows()),
                new Point(matchLoc.x+grayTemplate.cols(),matchLoc.y +grayTemplate.rows() )
        };
        Point[] sorted = detectDocument.sortPoints(ps);
        Mat match = fourPointTransform_match(image,sorted);

        Bitmap bmpPaper = Bitmap.createBitmap(match.cols(), match.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(match, bmpPaper);
        Bitmap bOutput = Bitmap.createBitmap(bmpPaper, 0, 0, bmpPaper.getWidth(), bmpPaper.getHeight(), new Matrix(), true);

        return match;

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
