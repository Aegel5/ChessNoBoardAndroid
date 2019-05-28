package com.example.alex.chessnoboardandroid;

import android.util.Log;

public class StrengthRules {
    private static final String TAG = MainApp.MainTag + StrengthRules.class.getSimpleName();

    static public int getMinDeltaScoreForLevel(int level) {
        return floatDeltaScore(level);
    }

    /*
    Вычислим score в пределах которого компьютер может ходить
     */
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
            double count = NewGameParams.getMaxLevel() - 5;
            double step = 200.0 / count;
            result = (int) ((NewGameParams.getMaxLevel() - level) * step);
        }
        Log.d(TAG, String.format("delta score for level %d is %d", level, result));
        return result;
    }

    /*
    Для большего рандома компьютер может перейти на более низкий уровень на один ход с небольшой вероятностью
    Эмуляция зевка для компьютера.
     */
    static private int floatDeltaScore(int level) {
        // с более высокого есть шанс перейти на более низкий (зевок)
        double prob = (NewGameParams.getMaxLevel() - level) * 0.005;
        if (MainApp.isprob(prob)) {
            int newLevel = MainApp.rndFromRange(0, level);
            Log.d(TAG, String.format("Tag decress with prob %f from level %d to %d", prob, level, newLevel));
            level = newLevel;

        }
        return strickDeltaScore(level);
    }
}
