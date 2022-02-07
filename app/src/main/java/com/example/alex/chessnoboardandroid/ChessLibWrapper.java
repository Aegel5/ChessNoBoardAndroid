package com.example.alex.chessnoboardandroid;

import android.util.Log;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;
import com.github.bhlangonijr.chesslib.move.MoveGenerator;
import com.github.bhlangonijr.chesslib.move.MoveGeneratorException;
import com.github.bhlangonijr.chesslib.move.MoveList;

import java.util.List;

/*
Дополнительный функционал поверх com.github.bhlangonijr.chesslib
*/

enum MoveLegalResult {
    AllOk,
    NeedPromotion,
    Bad
}

enum GameState {
    InProcess,
    Draw,
    Win
}

public class ChessLibWrapper {

    private static final String TAG = MainApp.MainTag + MainActivity.class.getSimpleName();

    public static Move moveFromString(String moveString, Board board) {
        return new Move(moveString, board.getSideToMove());
    }

    public static GameState getGameState(Board board) {
        if (board.isMated())
            return GameState.Win;
        if (board.isDraw())
            return GameState.Draw;
        return GameState.InProcess;
    }

//    public  static Square SqFromChars(char a, char b){
//        if(a <= 'h')
//            a -= 'a';
//        else
//            a -= 'A';
//
//        b -= '1';
//
//        int val = a*8+b;
//
//        return (Square)val;
//
//    }
//
//    public  static Move MvFromStringQuick(String mv){
//
//
//    }

    public static MoveLegalResult isMoveLegal(Move move, Board board, boolean checkNeedPromotion) {
        if (move == null)
            return MoveLegalResult.Bad;
        if (move.getFrom() == Square.NONE || move.getTo() == Square.NONE)
            return MoveLegalResult.Bad;

        List<Move> moves;
        try {
            moves = MoveGenerator.generateLegalMoves(board);
        } catch (MoveGeneratorException e) {
            Log.d(TAG, Utils.printException(e));
            return MoveLegalResult.Bad;
        }

        MoveLegalResult res = MoveLegalResult.Bad;
        Move mvToCheck = null;

        for (Move mv : moves) {
            //Log.d(TAG, "possible "+ mv.toString());
            if (move.getTo() == mv.getTo() && move.getFrom() == mv.getFrom()) {

                if (move.getPromotion() == Piece.NONE && mv.getPromotion() != Piece.NONE)
                    res = MoveLegalResult.NeedPromotion;
                else
                    res = MoveLegalResult.AllOk;
                mvToCheck = mv;
                break;
            }
        }

        if (mvToCheck != null) {
            if (board.isMoveLegal(mvToCheck, true))
                return res;
            else
                return MoveLegalResult.Bad;
        }

        return MoveLegalResult.Bad;

    }
}
