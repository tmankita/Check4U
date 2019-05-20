package com.example.tmankita.check4u.Database;

import java.io.Serializable;

public class Answer implements Serializable {
    private int questionNumber;
    private int answerNumber;
    private double locationX;
    private double locationY;
    private double height;
    private double width;
    private int sum_of_black;
    private int flagCorrect;

    public Answer() {
    }
    public int getAnswerNumber() {
        return answerNumber;
    }

    public void setAnswerNumber(int answerNumber) {
        this.answerNumber = answerNumber;
    }

    public int getQuestionNumber() {
        return questionNumber;
    }

    public void setQuestionNumber(int questionNumber) {
        this.questionNumber = questionNumber;
    }

    public double getLocationX() {
        return locationX;
    }

    public void setLocationX(int locationX) {
        this.locationX = locationX;
    }

    public double getLocationY() {
        return locationY;
    }

    public void setLocationY(int locationY) {
        this.locationY = locationY;
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public double getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getSum_of_black() {
        return sum_of_black;
    }

    public void setSum_of_black(int sum_of_black) {
        this.sum_of_black = sum_of_black;
    }

    public int getFlagCorrect() {
        return flagCorrect;
    }

    public void setFlagCorrect(int flagCorrect) {
        this.flagCorrect = flagCorrect;
    }
}
