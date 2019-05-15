package com.example.alex.chessnoboardandroid;

public class UciMove{
    String move;
    int score = Integer.MIN_VALUE;
    public boolean isValid(){
        return move != null && score != Integer.MIN_VALUE;
    }

}

