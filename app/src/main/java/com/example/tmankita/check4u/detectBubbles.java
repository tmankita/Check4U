package com.example.tmankita.check4u;

import android.graphics.Bitmap;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class detectBubbles {
    int numberOfAnswers;
    public detectBubbles(int numberOfAnswers){
        this.numberOfAnswers = numberOfAnswers;
    }
    public void detect(Mat paper, Bitmap bitmap) {
//# apply a four point perspective transform to both the
//# original image and grayscale image to obtain a top-down
//# birds eye view of the paper
//        paper = four_point_transform(image, docCnt.reshape(4, 2))
//        warped = four_point_transform(gray, docCnt.reshape(4, 2))
        int height = Double.valueOf(paper.size().height).intValue();
        int width = Double.valueOf(paper.size().width).intValue();
        Size size = new Size(width, height);
        Mat warped = new Mat(size, CvType.CV_8UC1);
        Imgproc.cvtColor(paper, warped, Imgproc.COLOR_RGB2GRAY, 4);


//# apply Otsu's thresholding method to binarize the warped
//# piece of paper
// thresh = cv2.threshold(warped, 0, 255,
//           cv2.THRESH_BINARY_INV | cv2.THRESH_OTSU)[1]

        Mat thresh = new Mat(size, CvType.CV_8UC1);
        Imgproc.threshold(warped, thresh, 0, 255, Imgproc.THRESH_BINARY_INV | Imgproc.THRESH_OTSU);
        Mat copyTresh = new Mat(size, CvType.CV_8UC1);
        thresh.copyTo(copyTresh);
//        # find contours in the thresholded image, then initialize
//# the list of contours that correspond to questions
//        cnts = cv2.findContours(thresh.copy(), cv2.RETR_EXTERNAL,
//                cv2.CHAIN_APPROX_SIMPLE)
//        cnts = imutils.grab_contours(cnts)
//        questionCnts = []

        ArrayList<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(thresh, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        hierarchy.release();

//        Collections.sort(contours, new Comparator<MatOfPoint>() {
//            @Override
//            public int compare(MatOfPoint lhs, MatOfPoint rhs) {
//                return Double.valueOf(Imgproc.contourArea(rhs)).compareTo(Imgproc.contourArea(lhs));
//            }
//        });


//  # loop over the contours
//  for c in cnts:
//	# compute the bounding box of the contour, then use the
//	# bounding box to derive the aspect ratio
//      (x, y, w, h) = cv2.boundingRect(c)
//       ar = w / float(h)
//
//	# in order to label the contour as a question, region
//	# should be sufficiently wide, sufficiently tall, and
//	# have an aspect ratio approximately equal to 1
//      if w >= 20 and h >= 20 and ar >= 0.9 and ar <= 1.1:
//          questionCnts.append(c)
        ArrayList<MatOfPoint> questionCnts = new ArrayList<>();
        for ( MatOfPoint c: contours ) {
            Rect rect = Imgproc.boundingRect(c);
            float ar = rect.width/rect.height;
            if(rect.width>=20 & rect.height>=20 & ar>=0.9 & ar<= 1.1)
                questionCnts.add(c);
        }

//# sort the question contours top-to-bottom, then initialize
//# the total number of correct answers
//        questionCnts = contours.sort_contours(questionCnts,
//                method="top-to-bottom")[0]
//        correct = 0

        Collections.sort(questionCnts, new Comparator<MatOfPoint>() {
            @Override
            public int compare(MatOfPoint lhs, MatOfPoint rhs) {
                return Double.valueOf(Imgproc.moments(rhs).m01/Imgproc.moments(rhs).m00).compareTo(Imgproc.moments(lhs).m01/Imgproc.moments(lhs).m00);
            }
        });
        Collections.reverse(questionCnts);
        int correct = 0;

//        each question has 5 possible answers, to loop over the
//# question in batches of 5
//        for (q, i) in enumerate(np.arange(0, len(questionCnts), 5)):
//	# sort the contours for the current question from
//	# left to right, then initialize the index of the
//	# bubbled answer
//        cnts = contours.sort_contours(questionCnts[i:i + 5])[0]
//        bubbled = None
        ArrayList<MatOfPoint> sortedquestionCnts = new ArrayList<>();
        ArrayList<MatOfPoint> temp = new ArrayList<>();
        for (int i = 0; i < questionCnts.size() ; i=i+numberOfAnswers) {
            for(int j=i ; j<i+numberOfAnswers; i++){
                temp.add(questionCnts.get(j));
            }
            Collections.sort(temp, new Comparator<MatOfPoint>() {
                @Override
                public int compare(MatOfPoint lhs, MatOfPoint rhs) {
                    return Double.valueOf(Imgproc.moments(rhs).m10/Imgproc.moments(rhs).m00).compareTo(Imgproc.moments(lhs).m10/Imgproc.moments(lhs).m00);
                }
            });
            Collections.reverse(temp);
            sortedquestionCnts.addAll(temp);
//	# loop over the sorted contours
//        for (j, c) in enumerate(cnts):
//		# construct a mask that reveals only the current
//		# "bubble" for the question
//        mask = np.zeros(thresh.shape, dtype="uint8")
//        cv2.drawContours(mask, [c], -1, 255, -1)
            ArrayList<MatOfPoint> answer = new ArrayList<>();
            Mat bitwise = new Mat(size,CvType.CV_8UC1);
            Mat mask = Mat.zeros(size,CvType.CV_8UC1);
            int total=0;
            for(int j=0; j<temp.size();j++){
                answer.add(temp.get(j));
                Imgproc.drawContours(mask,answer,-1,new Scalar(255,255,255),4);
                answer.remove(temp.get(j));
//		# apply the mask to the thresholded image, then
//		# count the number of non-zero pixels in the
//		# bubble area
//        mask = cv2.bitwise_and(thresh, thresh, mask=mask)
//        total = cv2.countNonZero(mask)
                Core.bitwise_and(copyTresh,copyTresh,bitwise,mask);
                total = Core.countNonZero(bitwise);

//		# if the current total has a larger number of total
//		# non-zero pixels, then we are examining the currently
//		# bubbled-in answer
//        if bubbled is None or total > bubbled[0]:
//        bubbled = (total, j)

            }

        }
        questionCnts.clear();


//
//		# if the current total has a larger number of total
//		# non-zero pixels, then we are examining the currently
//		# bubbled-in answer
//        if bubbled is None or total > bubbled[0]:
//        bubbled = (total, j)
//
//	# initialize the contour color and the index of the
//	# *correct* answer
//        color = (0, 0, 255)
//        k = ANSWER_KEY[q]
//
//	# check to see if the bubbled answer is correct
//        if k == bubbled[1]:
//        color = (0, 255, 0)
//        correct += 1
//
//	# draw the outline of the correct answer on the test
//        cv2.drawContours(paper, [cnts[k]], -1, color, 3)


//        List<MatOfPoint> lcs = new ArrayList<>();
//        for ( MatOfPoint c: contours ) {
//            MatOfPoint2f c2f = new MatOfPoint2f(c.toArray());
//            double perimeter = Imgproc.arcLength(c2f, true);
//            MatOfPoint2f approx = new MatOfPoint2f();
//            Imgproc.approxPolyDP(c2f, approx, 0.01 * perimeter, true);


        //Point[] points = approx.toArray();
//            Point[] foundPoints = sortPoints(points);

//            if(Imgproc.contourArea(approx)<0.70*size.width*size.height){
//                lcs.add(c);
//                Imgproc.drawContours(img,lcs,-1, new Scalar(0, 255, 0, 150), 5);


//                break;
//            }
//        }


    }


}
