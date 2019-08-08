#include <jni.h>
#include <string>
#include <opencv2/core.hpp>
#include <opencv2/opencv_modules.hpp>
#include <opencv2/imgproc.hpp>
#include <opencv2/features2d.hpp>
#include <opencv2/calib3d.hpp>
#include <opencv2/video/tracking.hpp>
#include <opencv2/highgui/highgui.hpp>
#include <iostream>



using namespace std;
using namespace cv;




const int MAX_FEATURES = 500;
const float GOOD_MATCH_PERCENT = 0.15f;




void alignFetureBased(Mat &im1, Mat &im2, Mat &imMatches, Mat &h) {



    // Convert images to grayscale
    Mat im1Gray, im2Gray;
    cvtColor(im1, im1Gray, COLOR_BGR2GRAY);
    cvtColor(im2, im2Gray, COLOR_BGR2GRAY);

    // Variables to store keypoints and descriptors
    std::vector<KeyPoint> keypoints1, keypoints2;
    Mat descriptors1, descriptors2;

    // Detect ORB features and compute descriptors.
    Ptr<Feature2D> orb = ORB::create(MAX_FEATURES);
    orb->detectAndCompute(im1Gray, Mat(), keypoints1, descriptors1);
    orb->detectAndCompute(im2Gray, Mat(), keypoints2, descriptors2);

    // Match features.
    std::vector<DMatch> matches;
    Ptr<DescriptorMatcher> matcher = DescriptorMatcher::create("BruteForce-Hamming");
    matcher->match(descriptors1, descriptors2, matches, Mat());

    // Sort matches by score
    std::sort(matches.begin(), matches.end());

    // Remove not so good matches
    const int numGoodMatches = matches.size() * GOOD_MATCH_PERCENT;
    matches.erase(matches.begin()+numGoodMatches, matches.end());


    // Draw top matches
//     Mat imMatches;
    drawMatches(im1, keypoints1, im2, keypoints2, matches, imMatches, Scalar::all(-1), Scalar::all(-1),
            vector<char>(), DrawMatchesFlags::NOT_DRAW_SINGLE_POINTS);
//    imwrite("matches.jpg", imMatches);


    // Extract location of good matches
    std::vector<Point2f> points1, points2;

    for( size_t i = 0; i < matches.size(); i++ )
    {
        points1.push_back( keypoints1[ matches[i].queryIdx ].pt );
        points2.push_back( keypoints2[ matches[i].trainIdx ].pt );
    }

    // Find homography
    h = findHomography( points1, points2, RANSAC );


    std::vector<Point2f> obj_corners(4);
    obj_corners[0] = Point(0,0); obj_corners[1] = Point( im2Gray.cols, 0 );
    obj_corners[2] = Point( im2Gray.cols, im2Gray.rows ); obj_corners[3] = Point( 0, im2Gray.rows );
    std::vector<Point2f> scene_corners(4);


    // Use homography to warp image

    perspectiveTransform( obj_corners, scene_corners, h);


    //-- Draw lines between the corners (the mapped object in the scene - image_2 )
    line( imMatches, scene_corners[0] + Point2f( im2Gray.cols, 0), scene_corners[1] + Point2f( im2Gray.cols, 0), Scalar(0, 255, 0), 4 );
    line( imMatches, scene_corners[1] + Point2f( im2Gray.cols, 0), scene_corners[2] + Point2f( im2Gray.cols, 0), Scalar( 0, 255, 0), 4 );
    line( imMatches, scene_corners[2] + Point2f( im2Gray.cols, 0), scene_corners[3] + Point2f( im2Gray.cols, 0), Scalar( 0, 255, 0), 4 );
    line( imMatches, scene_corners[3] + Point2f( im2Gray.cols, 0), scene_corners[0] + Point2f( im2Gray.cols, 0), Scalar( 0, 255, 0), 4 );

//    if(imMatches.empty()){
//        int i =0;
//    }

//    namedWindow( "Good Matches & Object detection", WINDOW_AUTOSIZE );// Create a window for display.
//    imshow( "Good Matches & Object detection", imMatches );
//    waitKey(0);

    //warpPerspective(im1, im1Reg, h, im2.size());

}

//Mat GetGradient(Mat src_gray)
//{
//    Mat grad_x, grad_y;
//    Mat abs_grad_x, abs_grad_y;
//
//    int scale = 1;
//    int delta = 0;
//    int ddepth = CV_32FC1; ;
//
//    // Calculate the x and y gradients using Sobel operator
//
//    Sobel( src_gray, grad_x, ddepth, 1, 0, 3, scale, delta, BORDER_DEFAULT );
//    convertScaleAbs( grad_x, abs_grad_x );
//
//    Sobel( src_gray, grad_y, ddepth, 0, 1, 3, scale, delta, BORDER_DEFAULT );
//    convertScaleAbs( grad_y, abs_grad_y );
//
//    // Combine the two gradients
//    Mat grad;
//    addWeighted( abs_grad_x, 0.5, abs_grad_y, 0.5, 0, grad );
//
//    return grad;
//
//}



void directAlign( Mat &temp, Mat &img,  Mat &im_aligned )
{

    // Read 8-bit color image.
    // This is an image in which the three channels are
    // concatenated vertically.
   // Mat im =  imread("images/cathedral.jpg", IMREAD_GRAYSCALE);



    // Define motion model
    const int warp_mode = MOTION_AFFINE;

    // Set space for warp matrix.
    Mat warp_matrix;

    // Set the warp matrix to identity.
    if ( warp_mode == MOTION_HOMOGRAPHY )
        warp_matrix = Mat::eye(3, 3, CV_32F);
    else
        warp_matrix = Mat::eye(2, 3, CV_32F);

    // Set the stopping criteria for the algorithm.
    int number_of_iterations = 5000;
    double termination_eps = 1e-10;

    TermCriteria criteria(TermCriteria::COUNT+TermCriteria::EPS,
                          number_of_iterations, termination_eps);


    // Warp the blue and green channels to the red channel
//    for ( int i = 0; i < 2; i++)
//    {

        double cc = findTransformECC (
                temp,
                img,
                warp_matrix,
                warp_mode,
                criteria
        );

//        cout << "warp_matrix : " << warp_matrix << endl;
//        cout << "CC " << cc << endl;
//        if (cc == -1)
//        {
//            cerr << "The execution was interrupted. The correlation value is going to be minimized." << endl;
//            cerr << "Check the warp initialization and/or the size of images." << endl << flush;
//        }


        if (warp_mode == MOTION_HOMOGRAPHY)
            // Use Perspective warp when the transformation is a Homography
            warpPerspective (img, im_aligned, warp_matrix, temp.size(), INTER_LINEAR + WARP_INVERSE_MAP);
        else
            // Use Affine warp when the transformation is not a Homography
            warpAffine(img, im_aligned, warp_matrix, temp.size(), INTER_LINEAR + WARP_INVERSE_MAP);

//    }



}

void align(Mat &im, Mat &imReference, Mat &aligned)
{


    Mat h;
    alignFetureBased(im, imReference, aligned, h);
    directAlign(imReference,im,aligned);


}


extern "C"
JNIEXPORT void JNICALL
Java_com_example_tmankita_check4u_Camera_TouchActivity_align(
        JNIEnv *env,
        jobject ,
        jlong addrImage,
        jlong addrTemplate,
        jlong addrAligned
){
    Mat& im = *(Mat*)addrImage;
    Mat& tmplate = *(Mat*)addrTemplate;
    Mat& aligned = *(Mat*)addrAligned;

    align(im,tmplate,aligned);

}


