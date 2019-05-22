package com.example.tmankita.check4u.Detectors;


import android.util.Log;

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

public class detectDocument {
// https://github.com/ctodobom/OpenNoteScanner/blob/master/app/src/main/java/com/todobom/opennotescanner/ImageProcessor.java
    /**
     *  Object that encapsulates the contour and 4 points that makes the larger
     *  rectangle on the image
     */
    private static boolean colorMode=false;
    private static boolean filterMode=true;
    private static double colorGain = 1.5;       // contrast
    private static double colorBias = 0;         // bright
    private static int colorThresh = 110;        // threshold

    public static class  document{
        public Mat doc_resized;
        public Mat doc_origin;
        public ArrayList<Point[]> allpoints_original;

        public document(Mat doc_origin,Mat doc_resized, ArrayList<Point[]> allpoints_original){
            this.doc_origin = doc_origin;
            this.doc_resized = doc_resized;
            this.allpoints_original = allpoints_original;
        }
    }

    public static class Quadrilateral {
        public MatOfPoint contour;
        public Point[] points;

        public Quadrilateral(MatOfPoint contour, Point[] points) {
            this.contour = contour;
            this.points = points;
        }
    }


    public static document findDocument( Mat inputRgba ) {
        Mat doc;
        ArrayList<MatOfPoint> contours = findContours(inputRgba);
        Quadrilateral quad = getQuadrilateral(contours);
        double[] offset = {-5.0,-5.0};
        contourOffset(quad,offset);
        ArrayList<Point[]> allpoints_original = new ArrayList<>();
            ArrayList<Point> points_original = new ArrayList<>();
            for (Point p : quad.points)
                points_original.add(new Point(p.x * (inputRgba.size().height / 800), p.y * inputRgba.size().height / 800));
            MatOfPoint sPoints_temp =  new  MatOfPoint();
            sPoints_temp.fromList( points_original);
            allpoints_original.add(sPoints_temp.toArray());
        doc = fourPointTransform (inputRgba , quad.points);
//        Imgproc.cvtColor(doc,doc,Imgproc.COLOR_RGBA2GRAY);
        enhanceDocument(doc);
        return new document(inputRgba,doc,allpoints_original);


        //Recalculate to original scale - start Points


//
//            ArrayList<Point> points_original = new ArrayList<>();
//            MatOfPoint sPoints_temp =  new  MatOfPoint();
//            for (Point p : quad.points)
//                points_original.add(new Point(p.x * (inputRgba.size().height / 800), p.y * inputRgba.size().height / 800));
//
//        sPoints_temp.fromList( points_original);
//        Point[] sPoints = sPoints_temp.toArray();
//        ArrayList<Point[]>res = new ArrayList<>();
//        res.add(sPoints);

//        return allpoints_original;

    }


    private static void contourOffset( Quadrilateral contour, double[] offset) {
      //Offset contour, by 5px border
        MatOfPoint points = new  MatOfPoint();
        ArrayList<Point> points_res = new ArrayList<>();
                 for( Point p:  contour.points ){
                     double y = p.y + offset[0];
                     double x = p.x + offset[1];
                     if(x<0.0)
                         x=0.0;
                     if(y<0.0)
                         y=0.0;
                     points_res.add(new Point(x,y));
                 }
        points.fromList(points_res);
        contour.points = points.toArray();

    }

    private static ArrayList<MatOfPoint> findContours(Mat src) {

        double ratio = src.size().height / 800;
        int height = Double.valueOf(src.size().height / ratio).intValue();
        int width = Double.valueOf(src.size().width / ratio).intValue();
        Size size = new Size(width,height);

        Mat resizedImage = new Mat(size, CvType.CV_8UC4);
        Mat grayImage = new Mat(size, CvType.CV_8UC4);
//        Mat LabImage = new Mat(size, CvType.CV_8UC3);
//        Mat LImage = new Mat(size, CvType.CV_8UC1);
        Mat canneyImage = new Mat(size, CvType.CV_8UC1);
        Mat bilateralFilterImage = new Mat(size, CvType.CV_8UC1);
        Mat adaptiveThresholdImage = new Mat(size, CvType.CV_8UC1);
        Mat medianBlurImage = new Mat(size, CvType.CV_8UC1);
        Mat BorderImage =  new Mat(size, CvType.CV_8UC1);

        // Resize and convert to grayscale
        Imgproc.resize(src,resizedImage,size);
        Imgproc.cvtColor(resizedImage, grayImage, Imgproc.COLOR_RGB2GRAY, 4);


        // Bilateral filter preserve edges
        Imgproc.bilateralFilter(grayImage ,bilateralFilterImage, 9, 75, 75);
        // Create black and white image based on adaptive threshold
        Imgproc.adaptiveThreshold(bilateralFilterImage,adaptiveThresholdImage, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 115, 4);
        // Median filter clears small details
        Imgproc.medianBlur(adaptiveThresholdImage,medianBlurImage, 11);
        // Add black border in case that page is touching an image border
        double[] value= {0.0,0.0,0.0};
        Core.copyMakeBorder(medianBlurImage, BorderImage, 5, 5, 5, 5, Core.BORDER_CONSTANT,  new Scalar(value));

        Imgproc.Canny(BorderImage, canneyImage, 250, 200);

        ArrayList<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();

        Imgproc.findContours(canneyImage, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        hierarchy.release();

        Collections.sort(contours, new Comparator<MatOfPoint>() {

            @Override
            public int compare(MatOfPoint lhs, MatOfPoint rhs) {
                return Double.valueOf(Imgproc.contourArea(rhs)).compareTo(Imgproc.contourArea(lhs));
            }
        });

        resizedImage.release();
        grayImage.release();
        canneyImage.release();
        bilateralFilterImage.release();
        adaptiveThresholdImage.release();
        medianBlurImage.release();
        BorderImage.release();

        return contours;
    }


    private static Quadrilateral getQuadrilateral(ArrayList<MatOfPoint> contours) {

//        ArrayList<Quadrilateral> allpoints = new ArrayList<>();
        for ( MatOfPoint c: contours ) {
            MatOfPoint2f c2f = new MatOfPoint2f(c.toArray());
            double perimeter = Imgproc.arcLength(c2f, true);
            MatOfPoint2f approx = new MatOfPoint2f();
            Imgproc.approxPolyDP(c2f, approx, 0.03 * perimeter, true);


            Point[] points = approx.toArray();
            Point[] foundPoints = sortPoints(points);


            // select biggest 4\5 angles polygon
            if (points.length == 4){
//                allpoints.add(new Quadrilateral(c, foundPoints));
                return new Quadrilateral(c, foundPoints);
            }


            else if(points.length == 5){
                double m_1 = (foundPoints[3].y-foundPoints[2].y)/(foundPoints[3].x-foundPoints[2].x);
                double n_1 = (-1) * m_1 * foundPoints[1].x + foundPoints[1].y;
                double m_2 = (foundPoints[2].y-foundPoints[1].y)/(foundPoints[2].x-foundPoints[1].x);
                double n_2 = (-1) * m_2 * foundPoints[3].x + foundPoints[3].y;
                Point Intersection = new Point(Math.floor((n_2-n_1)/(m_1-m_2)),Math.floor((-m_2*n_1+n_2*m_1)/(m_1-m_2)));

                foundPoints[0]= Intersection;
//                allpoints.add(new Quadrilateral(c, foundPoints));
                return new Quadrilateral(c, foundPoints);
            }
        }

        return null;
    }


    public static Point[] sortPoints( Point[] src ) {

        ArrayList<Point> srcPoints = new ArrayList<>(Arrays.asList(src));

        Point[] result = { null , null , null , null };

        Comparator<Point> sumComparator = new Comparator<Point>() {
            @Override
            public int compare(Point lhs, Point rhs) {
                return Double.valueOf(lhs.y + lhs.x).compareTo(rhs.y + rhs.x);
            }
        };

        Comparator<Point> diffComparator = new Comparator<Point>() {

            @Override
            public int compare(Point lhs, Point rhs) {
                return Double.valueOf(lhs.y - lhs.x).compareTo(rhs.y - rhs.x);
            }
        };

        // top-left corner = minimal sum
        result[0] = Collections.min(srcPoints, sumComparator);

        // bottom-right corner = maximal sum
        result[2] = Collections.max(srcPoints, sumComparator);

        // top-right corner = minimal difference
        result[1] = Collections.min(srcPoints, diffComparator);

        // bottom-left corner = maximal difference
        result[3] = Collections.max(srcPoints, diffComparator);

        return result;
    }

    public static Mat fourPointTransform( Mat src , Point[] pts ) {

        double ratio = src.size().height / 800;

        Point tl = pts[0];
        Point tr = pts[1];
        Point br = pts[2];
        Point bl = pts[3];
        Log.i("fourPointTransform", "tl.x = "+tl.x*ratio+" tl.y = "+ tl.y*ratio+" tr.x = "+ tr.x*ratio+" tr.y = "+tr.y*ratio+" br.x = "+br.x*ratio+" br.y = "+br.y*ratio+" bl.x = "+bl.x*ratio+" bl.y = "+bl.y*ratio);


        double widthA = Math.sqrt(Math.pow(br.x - bl.x, 2) + Math.pow(br.y - bl.y, 2));
        double widthB = Math.sqrt(Math.pow(tr.x - tl.x, 2) + Math.pow(tr.y - tl.y, 2));

        double dw = Math.max(widthA, widthB)*ratio;
        int maxWidth = Double.valueOf(dw).intValue();


        double heightA = Math.sqrt(Math.pow(tr.x - br.x, 2) + Math.pow(tr.y - br.y, 2));
        double heightB = Math.sqrt(Math.pow(tl.x - bl.x, 2) + Math.pow(tl.y - bl.y, 2));

        double dh = Math.max(heightA, heightB)*ratio;
        int maxHeight = Double.valueOf(dh).intValue();

        Mat doc = new Mat(maxHeight, maxWidth, CvType.CV_8UC4);

        Mat src_mat = new Mat(4, 1, CvType.CV_32FC2);
        Mat dst_mat = new Mat(4, 1, CvType.CV_32FC2);

        src_mat.put(0, 0, tl.x*ratio, tl.y*ratio, tr.x*ratio, tr.y*ratio, br.x*ratio, br.y*ratio, bl.x*ratio, bl.y*ratio);
        dst_mat.put(0, 0, 0.0, 0.0, dw, 0.0, dw, dh, 0.0, dh);

        Mat m = Imgproc.getPerspectiveTransform(src_mat, dst_mat);

        Imgproc.warpPerspective(src, doc, m, doc.size());

        return doc;
    }
    public static void enhanceDocument( Mat src ) {
        if (colorMode && filterMode) {
            src.convertTo(src,-1, colorGain , colorBias);
            Mat mask = new Mat(src.size(), CvType.CV_8UC1);
            Imgproc.cvtColor(src,mask,Imgproc.COLOR_RGBA2GRAY);

            Mat copy = new Mat(src.size(), CvType.CV_8UC3);
            src.copyTo(copy);

            Imgproc.adaptiveThreshold(mask,mask,255,Imgproc.ADAPTIVE_THRESH_MEAN_C,Imgproc.THRESH_BINARY_INV,15,15);

            src.setTo(new Scalar(255,255,255));
            copy.copyTo(src,mask);

            copy.release();
            mask.release();

            // special color threshold algorithm
            colorThresh(src,colorThresh);
        } else if (!colorMode) {
            Imgproc.cvtColor(src,src,Imgproc.COLOR_RGBA2GRAY);
            if (filterMode) {
//                Imgproc.adaptiveThreshold(src, src, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 15, 15);
//                Imgproc.threshold(src,src,150,255,Imgproc.THRESH_BINARY);

            }
        }
    }

    /**
     * When a pixel have any of its three elements above the threshold
     * value and the average of the three values are less than 80% of the
     * higher one, brings all three values to the max possible keeping
     * the relation between them, any absolute white keeps the value, all
     * others go to absolute black.
     *
     * src must be a 3 channel image with 8 bits per channel
     *
     * @param src
     * @param threshold
     */
    public static void colorThresh(Mat src, int threshold) {
        Size srcSize = src.size();
        int size = (int) (srcSize.height * srcSize.width)*3;
        byte[] d = new byte[size];
        src.get(0,0,d);

        for (int i=0; i < size; i+=3) {

            // the "& 0xff" operations are needed to convert the signed byte to double

            // avoid unneeded work
            if ( (double) (d[i] & 0xff) == 255 ) {
                continue;
            }

            double max = Math.max(Math.max((double) (d[i] & 0xff), (double) (d[i + 1] & 0xff)),
                    (double) (d[i + 2] & 0xff));
            double mean = ((double) (d[i] & 0xff) + (double) (d[i + 1] & 0xff)
                    + (double) (d[i + 2] & 0xff)) / 3;

            if (max > threshold && mean < max * 0.8) {
                d[i] = (byte) ((double) (d[i] & 0xff) * 255 / max);
                d[i + 1] = (byte) ((double) (d[i + 1] & 0xff) * 255 / max);
                d[i + 2] = (byte) ((double) (d[i + 2] & 0xff) * 255 / max);
            } else {
                d[i] = d[i + 1] = d[i + 2] = 0;
            }
        }
        src.put(0,0,d);
    }


}