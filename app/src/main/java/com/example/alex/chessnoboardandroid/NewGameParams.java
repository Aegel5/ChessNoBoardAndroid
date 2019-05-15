package com.example.alex.chessnoboardandroid;


import com.github.bhlangonijr.chesslib.Square;

enum BoardMode
{
    Hardcore(0),
    AllowViewFigures(1),
    AlwaysViewFigures(2),
    AllowViewTextBoard(3),
    AllowViewGraphicsBoard(4),
    ClassicalGame(5);

    private int index;

    private BoardMode(int index) {
        this.index = index;
    }

    public static BoardMode fromId(int id) {
        for (BoardMode type : values()) {
            if (type.index == id) {
                return type;
            }
        }
        return AlwaysViewFigures;
    }

    int getIndex(){
        return index;
    }

}
public class NewGameParams {

    public BoardMode boardMode = BoardMode.AlwaysViewFigures;
    public int compStrength = 10;
    public boolean isMaxStrength(){
        return compStrength >= maxLevel;
    }
    static final int maxLevel = 20;
}
