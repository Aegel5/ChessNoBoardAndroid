package com.example.alex.chessnoboardandroid;

import android.util.Log;

public class StrengthRules {
    private static final String TAG = MainApp.MainTag + MainApp.class.getSimpleName();

    static public int getMinDeltaScoreForLevel(int level) {
        return floatDeltaScore(level);
    }

    static private int strickDeltaScore(int level) {

        int result = 0;

        if (level == 0)
            // полный рандом
            result = 100000;
        else if (level == 1)
            // может пожертвовать ферзя
            result = 900;
        else if (level == 2)
            // может пожертвовать ладью
            result = 500;
        else if (level == 3)
            result = 400;
        else if (level == 4)
            result = 300;
        // дальше равномерно
        else {
            double count = NewGameParams.maxLevel - 5;
            double step = 200.0 /count;
            result = (int) ((NewGameParams.maxLevel - level) * step);
        }
        Log.d(TAG, String.format("delta score for level %d is %d", level, result));
        return result;
    }

    static private int floatDeltaScore(int level){
        // с более высокого есть шанс перейти на более низкий (зевок)
        double prob = (NewGameParams.maxLevel - level) * 0.005;
        if(MainApp.isprob(prob)){
            int newlevel = MainApp.rndFromRange(0, level);
            Log.d(TAG, String.format("Tag decress with prob %f from level %d to %d", prob, level, newlevel));
            level = newlevel;

        }
        return strickDeltaScore(level);
    }
}
