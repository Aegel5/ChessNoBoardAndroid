package com.example.alex.chessnoboardandroid;

import android.util.Log;

import com.github.bhlangonijr.chesslib.Board;

public class TestMoves {

    private static final String TAG = MainApp.MainTag + TestMoves.class.getSimpleName();

    private MainActivity mainActivity;
    public TestMoves(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }
    void testPromotion() {
        try {
            mainActivity.doMove("e2e4");
            mainActivity.doMove("a7a6");
            mainActivity.doMove("f1a6");
            mainActivity.doMove("a8a6");
            mainActivity.doMove("a2a4");
            mainActivity.doMove("a6b6");
            mainActivity.doMove("a4a5");
            mainActivity.doMove("b6c6");
            mainActivity.doMove("a5a6");
            mainActivity.doMove("c6d6");
            mainActivity.doMove("a6a7");
            mainActivity.doMove("d6e6");

            mainActivity.updateGui();
        } catch (Exception e) {
            Log.d(TAG, Utils.printException(e));
        }
    }

    void testMate() {
        try {
            mainActivity.doMove("e2e4");
            mainActivity.doMove("a7a6");
            mainActivity.doMove("f1c4");
            mainActivity.doMove("a6a5");
            mainActivity.doMove("d1h5");
            mainActivity.doMove("a5a4");
            //doMove("h5f7");

            mainActivity.updateGui();
        } catch (Exception e) {
            Log.d(TAG, Utils.printException(e));
        }
    }
}
