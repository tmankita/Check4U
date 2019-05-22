package com.example.tmankita.check4u.Database;

import java.io.Serializable;

public class Answer implements Serializable {
    private int questionNumber;
    private int answerNumber;
    private float locationX;
    private float locationY;
    private float height;
    private float width;
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

    public float getLocationX() {
        return locationX;
    }

    public void setLocationX(float locationX) {
        this.locationX = locationX;
    }

    public float getLocationY() {
        return locationY;
    }

    public void setLocationY(float locationY) {
        this.locationY = locationY;
    }

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public float getWidth() {
        return width;
    }

    public void setWidth(float width) {
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
