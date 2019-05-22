package com.example.tmankita.check4u.Detectors;


import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;


public class detectBlackSquare {

    public detectBlackSquare (){}

    public Point detect (Mat paper){
//        int height = Double.valueOf(paper.size().height).intValue();
//        int width = Double.valueOf(paper.size().width).intValue();
//        Size size = new Size(width, height);

        int squareHeight = 1;
        int squareWidth = 1;
        Point left_upper = new Point(-1,-1);
        Point left_bottom = new Point(-1,-1);
        Point right_upper = new Point(-1,-1);

        for (int i = 0; i < paper.rows() ; i++) {
            for (int j = 0; j < paper.cols() ; j++) {
                if(squareWidth%100==0){
                    if(left_upper.y == i){
                        right_upper.x = j;
                        right_upper.y = i;
                    }
                    break;
                }
                if(squareWidth==2){
                    left_upper.x = j;
                    left_upper.y = i;
                }
                if(paper.get(i,j)[0]<5){
                    squareWidth ++;
                }else{
                    squareHeight = 1;
                    squareWidth = 1;
                    left_upper.x = -1;
                    left_upper.y = -1;
                    right_upper.x = -1;
                    right_upper.y = -1;
                    left_bottom.x = -1;
                    left_bottom.y = -1;

                }
            }
            if(squareWidth%100==0){
                squareHeight++;
            }
            if(squareHeight%100==0){
                left_bottom.y = i;
                left_bottom.x = left_upper.x;
                break;
            }

        }

        return new Point(left_upper.x+(right_upper.x - left_upper.x)/2, left_upper.y + (left_bottom.y-left_upper.y)/2);
    }
}
