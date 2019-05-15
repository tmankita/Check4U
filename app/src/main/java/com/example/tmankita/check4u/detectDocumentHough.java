package com.example.tmankita.check4u;

import android.graphics.Bitmap;
import android.util.Log;
import android.widget.ImageView;

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


public class detectDocumentHough {



    public detectDocumentHough() {

    }

    public detectDocument.document detect(Mat img, Bitmap bitmap) {
        //gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
        int height = Double.valueOf(img.size().height).intValue();
        int width = Double.valueOf(img.size().width).intValue();
        Size size = new Size(width, height);
//        Mat gray = new Mat(size, CvType.CV_8UC1);
//        Imgproc.cvtColor(img, gray, Imgproc.COLOR_RGB2GRAY, 4);

        Mat LabImage = new Mat(size, CvType.CV_8UC3);
        List<Mat> l = new ArrayList<>();
        Imgproc.cvtColor(img, LabImage, Imgproc.COLOR_RGB2Lab, 4);
        Core.split(LabImage, l);
//        Bitmap bmpBarcode21 = bitmap.createBitmap(l.get(0).cols(), l.get(0).rows(), Bitmap.Config.ARGB_8888);
//        Utils.matToBitmap(l.get(0), bmpBarcode21);
//        Bitmap bmpBarcode22 = bitmap.createBitmap(l.get(1).cols(), l.get(1).rows(), Bitmap.Config.ARGB_8888);
//        Utils.matToBitmap(l.get(1), bmpBarcode22);
//        Bitmap bmpBarcode23 = bitmap.createBitmap(l.get(2).cols(), l.get(2).rows(), Bitmap.Config.ARGB_8888);
//        Utils.matToBitmap(l.get(2), bmpBarcode23);

        Mat lImage = new Mat(size, CvType.CV_8UC1);
        l.get(0).copyTo(lImage);


        Mat bilateral = new Mat(size, CvType.CV_8UC1);
        Imgproc.bilateralFilter(lImage, bilateral, 9, 75, 75);

        // Create black and white image based on adaptive threshold
        Mat adaptive = new Mat(size, CvType.CV_8UC1);
        Imgproc.adaptiveThreshold(bilateral, adaptive, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 115, 4);


        Mat median = new Mat(size, CvType.CV_8UC1);
        Imgproc.medianBlur(adaptive, median, 11);

        Mat canny = new Mat(size, CvType.CV_8UC1);
        Imgproc.Canny(median, canny, 50, 150, 3);


//        Mat gradX = new Mat(size,CvType.CV_8UC1);
//        Mat gradY = new Mat(size,CvType.CV_8UC1);
//        Imgproc.Sobel(median,gradX,CV_32F,1,0,-1);
//        Imgproc.Sobel(median,gradY,CV_32F,0,1,-1);

        //# subtract the y-gradient from the x-gradient
        //////////gradient = cv2.subtract(gradX, gradY)
        //////////gradient = cv2.convertScaleAbs(gradient)
//        Mat subtract = new Mat(size,CvType.CV_8UC1);
//        Mat subtractAbs = new Mat(size,CvType.CV_8UC1);
//        Core.subtract(gradY,gradX,subtract);
//        Core.convertScaleAbs(subtract,subtractAbs);
        Mat lines = new Mat(size, CvType.CV_8UC1);
        Mat toDetect = Mat.zeros(size, CvType.CV_8UC1);

        int threshold = 300;

        Imgproc.HoughLines(canny, lines, 1, Math.PI / 180, threshold);
        int row;
        double[] line;
        ArrayList<Point[]> verticals = new ArrayList<>();
        ArrayList<Point[]> horizontals = new ArrayList<>();
        //
        for (row = 0; row < lines.rows(); row++) {
            line = lines.get(row, 0);
            double rho = line[0];
            double theta = line[1];
            double a = Math.cos(theta);
            double b = Math.sin(theta);
            double x0 = a * rho;
            double y0 = b * rho;
            int x1 = (int) (x0 + 1000 * (-b));
            int y1 = (int) (y0 + 1000 * (a));
            int x2 = (int) (x0 - 1000 * (-b));
            int y2 = (int) (y0 - 1000 * (a));
            Log.i("theta", "theta is: " + Math.toDegrees(theta));

            if (80 <= Math.toDegrees(theta) && Math.toDegrees(theta) < 100) {
                if (x1 < x2)
                    horizontals.add(new Point[]{new Point(0, y1), new Point(size.width - 1, y2)});
                else {
                    horizontals.add(new Point[]{new Point(0, y2), new Point(size.width - 1, y1)});

                }


            } else if (160 <= Math.toDegrees(theta) && Math.toDegrees(theta) <= 200) {
                if (y2 > y1)
                    verticals.add(new Point[]{new Point(x1, 0), new Point(x2, size.height - 1)});
                else {
                    verticals.add(new Point[]{new Point(x2, 0), new Point(x1, size.height - 1)});

                }
            }

        }
        for (Point[] lineV : verticals
        ) {
            Imgproc.line(toDetect, lineV[0], lineV[1], new Scalar(255, 255, 255), 70);

        }
        for (Point[] lineH : horizontals
        ) {
            Imgproc.line(toDetect, lineH[0], lineH[1], new Scalar(255, 255, 255), 70);

        }
        Point[] imgEdge = new Point[]{
                new Point(0, 0),
                new Point(size.width, 0),
                new Point(0, size.height),
                new Point(size.width, size.height)
        };
        Point[] sortsImagePoint = sortPoints(imgEdge);
        Imgproc.line(toDetect, sortsImagePoint[0], sortsImagePoint[1], new Scalar(255, 255, 255), 70);
        Imgproc.line(toDetect, sortsImagePoint[1], sortsImagePoint[2], new Scalar(255, 255, 255), 70);
        Imgproc.line(toDetect, sortsImagePoint[2], sortsImagePoint[3], new Scalar(255, 255, 255), 70);
        Imgproc.line(toDetect, sortsImagePoint[3], sortsImagePoint[0], new Scalar(255, 255, 255), 70);

        Bitmap bmpBarcode23 = bitmap.createBitmap(toDetect.cols(), toDetect.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(toDetect, bmpBarcode23);

        ArrayList<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(toDetect, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
        hierarchy.release();
        Collections.sort(contours, new Comparator<MatOfPoint>() {

            @Override
            public int compare(MatOfPoint lhs, MatOfPoint rhs) {
                return Double.valueOf(Imgproc.contourArea(rhs)).compareTo(Imgproc.contourArea(lhs));
            }
        });
        Point[] foundPoints = null;
        MatOfPoint c = contours.get(1);
        MatOfPoint2f c2f = new MatOfPoint2f(c.toArray());
        double perimeter = Imgproc.arcLength(c2f, true);
        MatOfPoint2f approx = new MatOfPoint2f();
        Imgproc.approxPolyDP(c2f, approx, 0.01 * perimeter, true);


        Point[] points = approx.toArray();
        foundPoints = sortPoints(points);

//
//        Imgproc.line(img,foundPoints[0],foundPoints[1],new Scalar(0, 255, 0, 150), 4);
//        Imgproc.line(img,foundPoints[1],foundPoints[2],new Scalar(0, 255, 0, 150), 4);
//        Imgproc.line(img,foundPoints[2],foundPoints[3],new Scalar(0, 255, 0, 150), 4);
//        Imgproc.line(img,foundPoints[3],foundPoints[0],new Scalar(0, 255, 0, 150), 4);

        Mat croped = fourPointTransform_touch(img,foundPoints);
        enhanceDocument(croped);


        Bitmap bmpBarcode22 = bitmap.createBitmap(croped.cols(), croped.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(croped, bmpBarcode22);



        lines.release();
        canny.release();
        median.release();
        adaptive.release();
        bilateral.release();
        lImage.release();
        LabImage.release();

        ArrayList<Point[]> final_points = new ArrayList<>();
        final_points.add(foundPoints);
        return new detectDocument.document(img,croped,final_points);


    }

    public static Point[] sortPoints(Point[] src) {

        ArrayList<Point> srcPoints = new ArrayList<>(Arrays.asList(src));

        Point[] result = {null, null, null, null};

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

    public static Mat fourPointTransform_touch(Mat src, Point[] pts) {

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

    public static void enhanceDocument(Mat src) {
        Imgproc.cvtColor(src, src, Imgproc.COLOR_RGBA2GRAY);
        Imgproc.adaptiveThreshold(src, src, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 15, 15);

    }
}
