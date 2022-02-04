package com.example.alex.chessnoboardandroid;

import android.content.Context;
import android.util.Log;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

public class Utils {
    static public String unzipExeFromAsset(String filename, Context context) throws IOException {
        File f = new File(context.getCacheDir() + "/" + filename);
        if (!f.exists()) {

            Log.d("unzipExeFromAsset", "not exist try create");

            InputStream is = context.getAssets().open(filename);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            FileOutputStream fos = new FileOutputStream(f);
            fos.write(buffer);
            fos.close();
        }
        f.setExecutable(true);
        return f.getPath();
    }

    static public String printException(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        String sStackTrace = sw.toString(); // stack trace as a string
        return e.toString() + "\n" + sStackTrace;
    }

    static private String getUnicodeFromPiece(Piece piece) {
        if (piece == Piece.WHITE_KING) return (Character.toString((char) 0x2654));
        else if (piece == Piece.WHITE_QUEEN) return (Character.toString((char) 0x2655));
        else if (piece == Piece.WHITE_ROOK) return (Character.toString((char) 0x2656));
        else if (piece == Piece.WHITE_BISHOP) return (Character.toString((char) 0x2657));
        else if (piece == Piece.WHITE_KNIGHT) return (Character.toString((char) 0x2658));
        else if (piece == Piece.WHITE_PAWN) return (Character.toString((char) 0x2659));
        else if (piece == Piece.BLACK_KING) return (Character.toString((char) 0x265A));
        else if (piece == Piece.BLACK_QUEEN) return (Character.toString((char) 0x265B));
        else if (piece == Piece.BLACK_ROOK) return (Character.toString((char) 0x265C));
        else if (piece == Piece.BLACK_BISHOP) return (Character.toString((char) 0x265D));
        else if (piece == Piece.BLACK_KNIGHT) return (Character.toString((char) 0x265E));
        else if (piece == Piece.BLACK_PAWN) return (Character.toString((char) 0x265F));
        return "";
    }

    static private Square parseSquare(String sq) {
        try {
            return Square.fromValue(sq.toUpperCase());
        } catch (Exception e) {
            return Square.NONE;
        }
    }

    static  public String PretifyMove(String mv, Board brd){

        StringBuilder curMv = new StringBuilder();

        if (mv.length() >= 2) {

            var cur = mv.substring(0,2);
            curMv.append(getUnicodeFromPiece(brd.getPiece(parseSquare(cur))));
            curMv.append(cur);

            if (mv.length() >= 4) {
                cur = mv.substring(2,4);
                curMv.append(getUnicodeFromPiece(brd.getPiece(parseSquare(cur))));
                curMv.append(cur);

                if(mv.length() == 5){
                    curMv.append(mv.charAt(4));
                }else if(mv.length() > 5){
                    curMv.append("ERR");
                }

                if(brd.isMated()) {
                    curMv.append("#");
                }else if(brd.isKingAttacked()){
                    curMv.append("+");
                }

            }else if(mv.length() == 3){
                curMv.append(mv.charAt(2));
            }

        }else if(mv.length() == 1){
            curMv.append(mv.charAt(0));
        }

        return curMv.toString();
    }
}
