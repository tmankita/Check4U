package com.example.tmankita.check4u.Detectors;

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
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.Features2d;
import org.opencv.features2d.ORB;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.example.tmankita.check4u.Camera.TouchActivity.fourPointTransform_touch;

public class alignToTemplate {

    int MAX_FEATURES = 2500;
    double GOOD_MATCH_PERCENT = 0.1;

    public alignToTemplate (){}

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

    public Mat transformationBasedPerspective (Mat img,Mat template, Point[] imgP, Point[] templateP, Bitmap bitmap){


        MatOfPoint imagePoints = new MatOfPoint(imgP);
        MatOfPoint templatePoints = new MatOfPoint(templateP);
        Mat M = Imgproc.getPerspectiveTransform(imagePoints,templatePoints);

        Mat doc = new Mat(template.size() , CvType.CV_8UC4);

        Imgproc.warpPerspective(img,doc, M,doc.size());

        Bitmap bmpBarcode23 = bitmap.createBitmap(doc.cols(), doc.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(doc, bmpBarcode23);

        return doc;
    }

    public Mat transformationBasedHomography (Mat img,Mat template, Point[] imgP, Point[] templateP, Bitmap bitmap){

        MatOfPoint2f templateMat = new MatOfPoint2f(templateP), imageMat = new MatOfPoint2f(imgP);

        //RANSAC
        Mat H = Calib3d.findHomography( templateMat,imageMat, Calib3d.RANSAC);

        //Get the corners from the image_1 ( the object to be "detected" )
        Mat objCorners = new Mat(4, 1, CvType.CV_32FC2), sceneCorners = new Mat();
        float[] objCornersData = new float[(int) (objCorners.total() * objCorners.channels())];
        objCorners.get(0, 0, objCornersData);
        objCornersData[0] = 0;
        objCornersData[1] = 0;
        objCornersData[2] = template.cols();
        objCornersData[3] = 0;
        objCornersData[4] = template.cols();
        objCornersData[5] = template.rows();
        objCornersData[6] = 0;
        objCornersData[7] = template.rows();
        objCorners.put(0, 0, objCornersData);

        Core.perspectiveTransform(objCorners, sceneCorners, H);

        float[] sceneCornersData = new float[(int) (sceneCorners.total() * sceneCorners.channels())];
        sceneCorners.get(0, 0, sceneCornersData);
        //-- Draw lines between the corners (the mapped object in the scene - image_2 )
        Imgproc.line(img, new Point(sceneCornersData[0], sceneCornersData[1]),
                new Point(sceneCornersData[2] , sceneCornersData[3]), new Scalar(0, 255, 0), 7);

        Imgproc.line(img, new Point(sceneCornersData[2] , sceneCornersData[3]),
                new Point(sceneCornersData[4] , sceneCornersData[5]), new Scalar(0, 255, 0), 7);

        Imgproc.line(img, new Point(sceneCornersData[4] , sceneCornersData[5]),
                new Point(sceneCornersData[6], sceneCornersData[7]), new Scalar(0, 255, 0), 7);

        Imgproc.line(img, new Point(sceneCornersData[6] , sceneCornersData[7]),
                new Point(sceneCornersData[0] , sceneCornersData[1]), new Scalar(0, 255, 0), 7);

        Point[] detect = new Point[]{
                new Point(sceneCornersData[0] , sceneCornersData[1]),
                new Point(sceneCornersData[2] , sceneCornersData[3]),
                new Point(sceneCornersData[4] , sceneCornersData[5]),
                new Point(sceneCornersData[6] , sceneCornersData[7])

        };
        Point[] sorted_2 = detectDocument.sortPoints(detect);
        Mat croped = fourPointTransform_touch(img,sorted_2);
        Bitmap bmpBarcode23 = bitmap.createBitmap(img.cols(), img.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(img, bmpBarcode23);
        Bitmap bmpBarcode26 = bitmap.createBitmap(croped.cols(), croped.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(croped, bmpBarcode26);
        return croped;
    }
    public Mat transformationBasedAffine (Mat img,Mat template){return null;}
    public Mat transformationBasedEstimateRigid (Mat img,Mat template){return null;}

    public Point[] alignHelper (Mat img,Mat template ,Bitmap bitmap,String mode){
        //Convert images to grayscale
        Size sizeTemplate = new Size(template.cols(),template.rows());
        Size sizeImage = new Size(img.cols(),img.rows());
        Mat grayTemplate = new Mat(sizeTemplate, CvType.CV_8UC1);
        Mat grayImage = new Mat(sizeImage, CvType.CV_8UC1);

        if(mode.equals("template"))
            Imgproc.cvtColor(img, grayImage, Imgproc.COLOR_RGB2GRAY, 4);
        else if(mode.equals("image"))
            img.copyTo(grayImage);


        Imgproc.cvtColor(template, grayTemplate, Imgproc.COLOR_RGB2GRAY, 4);

        Imgproc.adaptiveThreshold(grayTemplate, grayTemplate, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 15, 15);
        Imgproc.adaptiveThreshold(grayImage, grayImage, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 15, 15);


        Bitmap bmpBarcode24 = bitmap.createBitmap(grayTemplate.cols(), grayTemplate.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(grayTemplate, bmpBarcode24);


        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

//        Mat norTemplate = normalization(grayTemplate);
//        Mat norImage = normalization(grayImage);

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        Mat Template = new Mat(sizeTemplate, CvType.CV_8UC1);
        Mat Image = new Mat(sizeImage, CvType.CV_8UC1);
        grayImage.copyTo(Image);
        grayTemplate.copyTo(Template);

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        //Detect ORB features and compute descriptors.
        ORB orb = ORB.create(MAX_FEATURES);
        MatOfKeyPoint keyPointT_b = new MatOfKeyPoint();
        MatOfKeyPoint keyPointI_b = new MatOfKeyPoint();
        Mat descriptorsT = new Mat();
        Mat descriptorsI = new Mat();
        orb.detectAndCompute(Template,new Mat(),keyPointT_b,descriptorsT);
        orb.detectAndCompute(Image,new Mat(),keyPointI_b,descriptorsI);
        KeyPoint[] keyPointT = keyPointT_b.toArray();
        KeyPoint[] keyPointI = keyPointI_b.toArray();
        // Match features.
        MatOfDMatch matches = new MatOfDMatch();
        //BRUTEFORCE_HAMMING
        DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
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
        Features2d.drawMatches( Template, keyPointT_b,Image, keyPointI_b, goodMatches, imgMatches, Scalar.all(-1),
                Scalar.all(-1), new MatOfByte(), Features2d.DrawMatchesFlags_DEFAULT);

        Bitmap bmpBarcode25 = bitmap.createBitmap(imgMatches.cols(), imgMatches.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(imgMatches, bmpBarcode25);

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
        //RANSAC
        Mat H = Calib3d.findHomography( templateMat,imageMat, Calib3d.RANSAC);

        //Get the corners from the image_1 ( the object to be "detected" )
        Mat objCorners = new Mat(4, 1, CvType.CV_32FC2), sceneCorners = new Mat();
        float[] objCornersData = new float[(int) (objCorners.total() * objCorners.channels())];
        objCorners.get(0, 0, objCornersData);
        objCornersData[0] = 0;
        objCornersData[1] = 0;
        objCornersData[2] = Template.cols();
        objCornersData[3] = 0;
        objCornersData[4] = Template.cols();
        objCornersData[5] = Template.rows();
        objCornersData[6] = 0;
        objCornersData[7] = Template.rows();
        objCorners.put(0, 0, objCornersData);

        Core.perspectiveTransform(objCorners, sceneCorners, H);

        float[] sceneCornersData = new float[(int) (sceneCorners.total() * sceneCorners.channels())];
        sceneCorners.get(0, 0, sceneCornersData);
        //-- Draw lines between the corners (the mapped object in the scene - image_2 )
        Imgproc.line(imgMatches, new Point(sceneCornersData[0] + Template.cols(), sceneCornersData[1]),
                new Point(sceneCornersData[2] + Template.cols(), sceneCornersData[3]), new Scalar(0, 255, 0), 7);

        Imgproc.line(imgMatches, new Point(sceneCornersData[2] + Template.cols(), sceneCornersData[3]),
                new Point(sceneCornersData[4] + Template.cols(), sceneCornersData[5]), new Scalar(0, 255, 0), 7);

        Imgproc.line(imgMatches, new Point(sceneCornersData[4] + Template.cols(), sceneCornersData[5]),
                new Point(sceneCornersData[6] + Template.cols(), sceneCornersData[7]), new Scalar(0, 255, 0), 7);

        Imgproc.line(imgMatches, new Point(sceneCornersData[6] + Template.cols(), sceneCornersData[7]),
                new Point(sceneCornersData[0] + Template.cols(), sceneCornersData[1]), new Scalar(0, 255, 0), 7);

        Point[] detect = new Point[]{
                new Point(sceneCornersData[0] , sceneCornersData[1]),
                new Point(sceneCornersData[2] , sceneCornersData[3]),
                new Point(sceneCornersData[4] , sceneCornersData[5]),
                new Point(sceneCornersData[6] , sceneCornersData[7])

        };
        Point[] sorted_2 = detectDocument.sortPoints(detect);
//        Mat croped = fourPointTransform_touch(grayImage,sorted_2);
        Bitmap bmpBarcode23 = bitmap.createBitmap(imgMatches.cols(), imgMatches.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(imgMatches, bmpBarcode23);
//        Bitmap bmpBarcode26 = bitmap.createBitmap(croped.cols(), croped.rows(), Bitmap.Config.ARGB_8888);
//        Utils.matToBitmap(croped, bmpBarcode26);
        return sorted_2;
    }

    public Mat align ( Mat img,Mat template,Mat icon,Bitmap bitmap) {

//        Point[] iconTemplate = alignHelper(template,icon, bitmap,"template");
//        Point[] iconimage = alignHelper(template,icon,bitmap,"image");
//        Mat res = transformationBasedHomography(img, template, iconimage, iconTemplate, bitmap);


        return align1(img,template,bitmap);
    }

    public Mat align1 ( Mat img,Mat template,Bitmap bitmap) {
        //Convert images to grayscale
        Size sizeTemplate = new Size(template.cols(),template.rows());
        Size sizeImage = new Size(img.cols(),img.rows());
        Mat grayTemplate = new Mat(sizeTemplate, CvType.CV_8UC1);
        Mat grayImage = new Mat(sizeImage, CvType.CV_8UC1);
        img.copyTo(grayImage);
//        Imgproc.cvtColor(img, grayImage, Imgproc.COLOR_RGB2GRAY, 4);
        Imgproc.cvtColor(template, grayTemplate, Imgproc.COLOR_RGB2GRAY, 4);

        Imgproc.adaptiveThreshold(grayTemplate, grayTemplate, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 15, 15);
        Imgproc.adaptiveThreshold(grayImage, grayImage, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 15, 15);

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

//        Mat norTemplate = normalization(grayTemplate);
//        Mat norImage = normalization(grayImage);

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        Mat Template = new Mat(sizeTemplate, CvType.CV_8UC1);
        Mat Image = new Mat(sizeImage, CvType.CV_8UC1);
        grayImage.copyTo(Image);
        grayTemplate.copyTo(Template);

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        //Detect ORB features and compute descriptors.
        ORB orb = ORB.create(MAX_FEATURES);
        MatOfKeyPoint keyPointT_b = new MatOfKeyPoint();
        MatOfKeyPoint keyPointI_b = new MatOfKeyPoint();
        Mat descriptorsT = new Mat();
        Mat descriptorsI = new Mat();
        orb.detectAndCompute(Template,new Mat(),keyPointT_b,descriptorsT);
        orb.detectAndCompute(Image,new Mat(),keyPointI_b,descriptorsI);
        KeyPoint[] keyPointT = keyPointT_b.toArray();
        KeyPoint[] keyPointI = keyPointI_b.toArray();
        // Match features.
        MatOfDMatch matches = new MatOfDMatch();
        //BRUTEFORCE_HAMMING
        DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
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
        Features2d.drawMatches( Template, keyPointT_b,Image, keyPointI_b, goodMatches, imgMatches, Scalar.all(-1),
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
        //RANSAC
        Mat H = Calib3d.findHomography( templateMat,imageMat, Calib3d.RANSAC);

        //Get the corners from the image_1 ( the object to be "detected" )
        Mat objCorners = new Mat(4, 1, CvType.CV_32FC2), sceneCorners = new Mat();
        float[] objCornersData = new float[(int) (objCorners.total() * objCorners.channels())];
        objCorners.get(0, 0, objCornersData);
        objCornersData[0] = 0;
        objCornersData[1] = 0;
        objCornersData[2] = Template.cols();
        objCornersData[3] = 0;
        objCornersData[4] = Template.cols();
        objCornersData[5] = Template.rows();
        objCornersData[6] = 0;
        objCornersData[7] = Template.rows();
        objCorners.put(0, 0, objCornersData);

        Core.perspectiveTransform(objCorners, sceneCorners, H);

        float[] sceneCornersData = new float[(int) (sceneCorners.total() * sceneCorners.channels())];
        sceneCorners.get(0, 0, sceneCornersData);
        //-- Draw lines between the corners (the mapped object in the scene - image_2 )
        Imgproc.line(imgMatches, new Point(sceneCornersData[0] + Template.cols(), sceneCornersData[1]),
                new Point(sceneCornersData[2] + Template.cols(), sceneCornersData[3]), new Scalar(0, 255, 0), 7);

        Imgproc.line(imgMatches, new Point(sceneCornersData[2] + Template.cols(), sceneCornersData[3]),
                new Point(sceneCornersData[4] + Template.cols(), sceneCornersData[5]), new Scalar(0, 255, 0), 7);

        Imgproc.line(imgMatches, new Point(sceneCornersData[4] + Template.cols(), sceneCornersData[5]),
                new Point(sceneCornersData[6] + Template.cols(), sceneCornersData[7]), new Scalar(0, 255, 0), 7);

        Imgproc.line(imgMatches, new Point(sceneCornersData[6] + Template.cols(), sceneCornersData[7]),
                new Point(sceneCornersData[0] + Template.cols(), sceneCornersData[1]), new Scalar(0, 255, 0), 7);

        Point[] detect = new Point[]{
                new Point(sceneCornersData[0] , sceneCornersData[1]),
                new Point(sceneCornersData[2] , sceneCornersData[3]),
                new Point(sceneCornersData[4] , sceneCornersData[5]),
                new Point(sceneCornersData[6] , sceneCornersData[7])

        };
        Point[] sorted_2 = detectDocument.sortPoints(detect);
        Mat croped = fourPointTransform_touch(grayImage,sorted_2);
        Bitmap bmpBarcode23 = bitmap.createBitmap(imgMatches.cols(), imgMatches.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(imgMatches, bmpBarcode23);
        Bitmap bmpBarcode26 = bitmap.createBitmap(croped.cols(), croped.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(croped, bmpBarcode26);
        return croped;
    }
}
