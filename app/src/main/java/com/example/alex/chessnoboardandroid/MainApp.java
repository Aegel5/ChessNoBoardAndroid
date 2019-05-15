package com.example.alex.chessnoboardandroid;

import android.util.Log;

import java.util.Random;

public class MainApp {
    private static final String TAG = MainApp.MainTag + MainApp.class.getSimpleName();
    public  static final String MainTag = "jetflame_";
    private static Random rnd = new Random();

    static {
    }

    public  static Random rand()    {
        return  rnd;
    }
    public static boolean isprob(double val){
        return rnd.nextDouble() < val;
    }
    public  static int rndFromRange(int min, int max){
        int result = rnd.nextInt((max - min) + 1) + min;
        Log.d(TAG, String.format("rand %s from range (%s %s)", result, min, max));
        return result;
    }

}
