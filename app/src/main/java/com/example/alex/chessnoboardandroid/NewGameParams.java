package com.example.alex.chessnoboardandroid;

import android.util.Log;

import com.github.bhlangonijr.chesslib.PieceType;

enum AllowViewBoardMode {
    None(0),
    AllowViewTextBoard(1),
    AllowViewGraphicsBoard(2);

    private int index;

    AllowViewBoardMode(int index) {
        this.index = index;
    }

    public static AllowViewBoardMode fromId(int id) {
        for (AllowViewBoardMode type : values()) {
            if (type.index == id) {
                return type;
            }
        }
        return None;
    }

    public int getIndex() {
        return index;
    }

}

public class NewGameParams {

    private static final int maxLevel = 20;

    // Можно ли подсматривать на доску
    //private AllowViewBoardMode allowViewBoardMode = AllowViewBoardMode.None;

    private int compStrength = 3;

    public PieceType compFavoriteFigure = PieceType.NONE;
    public double favoritePrc = 0;

    //public int analItem = 4;

    //private boolean addFiguresSign = true;

    public static int getMaxLevel() {
        return maxLevel;
    }

    public  void ChooseFavorite(){
        while(compFavoriteFigure == PieceType.NONE)
            compFavoriteFigure = PieceType.values()[MainApp.rndFromRange(0, PieceType.values().length-1)];

        favoritePrc = MainApp.rand().nextDouble();
    }

    public boolean isMaxStrength() {
        return getCompStrength() >= getMaxLevel();
    }

    public int getCompStrength() {
        return compStrength;
    }

//    public AllowViewBoardMode getAllowViewBoardMode() {
//        return allowViewBoardMode;
//    }
//
//    public void setAllowViewBoardMode(AllowViewBoardMode allowViewBoardMode) {
//        this.allowViewBoardMode = allowViewBoardMode;
//    }

    public void setCompStrength(int compStrength) {
        this.compStrength = compStrength;
    }

    //public boolean isAddFiguresSign() {
    //    return addFiguresSign;
    //}

//    public void setAddFiguresSign(boolean addFiguresSign) {
//        this.addFiguresSign = addFiguresSign;
//    }
}
