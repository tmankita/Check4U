package com.example.tmankita.check4u;

import android.graphics.Bitmap;

import org.opencv.android.Utils;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.BFMatcher;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.Features2d;
import org.opencv.features2d.ORB;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.example.tmankita.check4u.Camera.TouchActivity.fourPointTransform_touch;
import static org.opencv.core.Core.NORM_HAMMING;

public class alignToTemplate {

    int MAX_FEATURES = 500;
    double GOOD_MATCH_PERCENT = 0.15;

    public alignToTemplate (){}

    public Mat align ( Mat img,Mat template,Bitmap bitmap) {

        //Convert images to grayscale
        Size sizeTemplate = new Size(template.cols(),template.rows());
        Size sizeImage = new Size(img.cols(),img.rows());
        Mat grayTemplate = new Mat(sizeTemplate, CvType.CV_8UC1);
        Mat grayImage = new Mat(sizeImage, CvType.CV_8UC1);
//        Mat grayTemplate = new Mat(sizeTemplate, CvType.CV_8UC1);
        Imgproc.cvtColor(img, grayImage, Imgproc.COLOR_RGB2GRAY, 4);
        Imgproc.cvtColor(template, grayTemplate, Imgproc.COLOR_RGB2GRAY, 4);

//        Imgproc.adaptiveThreshold(grayImage, grayImage, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 15, 15);



        //Detect ORB features and compute descriptors.
        ORB orb = ORB.create(MAX_FEATURES);
        MatOfKeyPoint keyPointT_b = new MatOfKeyPoint();
        MatOfKeyPoint keyPointI_b = new MatOfKeyPoint();
        Mat descriptorsT = new Mat();
        Mat descriptorsI = new Mat();
        orb.detectAndCompute(grayTemplate,new Mat(),keyPointT_b,descriptorsT);
        orb.detectAndCompute(grayImage,new Mat(),keyPointI_b,descriptorsI);

        KeyPoint[] keyPointT = keyPointT_b.toArray();
        KeyPoint[] keyPointI = keyPointI_b.toArray();

        // Match features.
        MatOfDMatch matches = new MatOfDMatch();
        DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
//        BFMatcher bf = new BFMatcher(NORM_HAMMING, true);
        matcher.match(descriptorsT,descriptorsI,matches);

        // Sort matches by score
        List<DMatch> matchesList = matches.toList();
        Collections.sort(matchesList,new Comparator<DMatch>() {
            @Override
            public int compare(DMatch lhs, DMatch rhs) {
                return Float.valueOf(lhs.distance).compareTo(rhs.distance);
            }
        });

        // Remove not so good matches
        int numGoodMatches = (int)(matchesList.size() * GOOD_MATCH_PERCENT);
        ArrayList<DMatch> minDistanceList = new ArrayList<>();
        int i=0;
        for (DMatch dmatch : matchesList) {
            if(i == numGoodMatches)
                break;
            minDistanceList.add(dmatch);
            i++;
        }
        MatOfDMatch goodMatches = new MatOfDMatch();
        goodMatches.fromList(minDistanceList);




        // Draw top matches

        Mat imgMatches = new Mat();
        Features2d.drawMatches( grayTemplate, keyPointT_b,grayImage, keyPointI_b, goodMatches, imgMatches, Scalar.all(-1),
                Scalar.all(-1), new MatOfByte(), Features2d.DrawMatchesFlags_DEFAULT);

        // Extract location of good matches



        List<Point> pointsI = new ArrayList<>();
        List<Point> pointsT = new ArrayList<>();

        for (DMatch dmatch : minDistanceList) {
            pointsT.add(keyPointT[dmatch.queryIdx].pt);
            pointsI.add(keyPointI[dmatch.trainIdx].pt);
        }

        MatOfPoint2f templateMat = new MatOfPoint2f(), imageMat = new MatOfPoint2f();
        imageMat.fromList(pointsI);
        templateMat.fromList(pointsT);
        Mat H = Calib3d.findHomography(  templateMat,imageMat, Calib3d.RANSAC);


        //-- Get the corners from the image_1 ( the object to be "detected" )
        Mat objCorners = new Mat(4, 1, CvType.CV_32FC2), sceneCorners = new Mat();
        float[] objCornersData = new float[(int) (objCorners.total() * objCorners.channels())];
        objCorners.get(0, 0, objCornersData);
        objCornersData[0] = 0;
        objCornersData[1] = 0;
        objCornersData[2] = grayTemplate.cols();
        objCornersData[3] = 0;
        objCornersData[4] = grayTemplate.cols();
        objCornersData[5] = grayTemplate.rows();
        objCornersData[6] = 0;
        objCornersData[7] = grayTemplate.rows();
        objCorners.put(0, 0, objCornersData);

        Core.perspectiveTransform(objCorners, sceneCorners, H);

        float[] sceneCornersData = new float[(int) (sceneCorners.total() * sceneCorners.channels())];
        sceneCorners.get(0, 0, sceneCornersData);
        //-- Draw lines between the corners (the mapped object in the scene - image_2 )
        Imgproc.line(imgMatches, new Point(sceneCornersData[0] + grayTemplate.cols(), sceneCornersData[1]),
                new Point(sceneCornersData[2] + grayTemplate.cols(), sceneCornersData[3]), new Scalar(0, 255, 0), 4);

        Imgproc.line(imgMatches, new Point(sceneCornersData[2] + grayTemplate.cols(), sceneCornersData[3]),
                new Point(sceneCornersData[4] + grayTemplate.cols(), sceneCornersData[5]), new Scalar(0, 255, 0), 4);

        Imgproc.line(imgMatches, new Point(sceneCornersData[4] + grayTemplate.cols(), sceneCornersData[5]),
                new Point(sceneCornersData[6] + grayTemplate.cols(), sceneCornersData[7]), new Scalar(0, 255, 0), 4);

        Imgproc.line(imgMatches, new Point(sceneCornersData[6] + grayTemplate.cols(), sceneCornersData[7]),
                new Point(sceneCornersData[0] + grayTemplate.cols(), sceneCornersData[1]), new Scalar(0, 255, 0), 4);


        Point[] detect = new Point[]{
                new Point(sceneCornersData[0] , sceneCornersData[1]),
                new Point(sceneCornersData[2] , sceneCornersData[3]),
                new Point(sceneCornersData[4] , sceneCornersData[5]),
                new Point(sceneCornersData[6] , sceneCornersData[7])

        };
        Point[] sorted_2 = detectDocument.sortPoints(detect);

        Mat croped = fourPointTransform_touch(grayImage,sorted_2);


//        Bitmap bmpBarcode23 = bitmap.createBitmap(imgMatches.cols(), imgMatches.rows(), Bitmap.Config.ARGB_8888);
//        Utils.matToBitmap(imgMatches, bmpBarcode23);
//        Bitmap bmpBarcode26 = bitmap.createBitmap(croped.cols(), croped.rows(), Bitmap.Config.ARGB_8888);
//        Utils.matToBitmap(croped, bmpBarcode26);
        return croped;
    }
}
