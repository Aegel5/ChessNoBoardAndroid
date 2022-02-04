package com.example.alex.chessnoboardandroid;

import com.example.alex.chessnoboardandroid.Utils;
import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.move.Move;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

class analitem{
    public String move;
    public Integer cp;
    public List<String> cont = new ArrayList<>();
    public  int number;
    public  int mateIn;
    String nice;

    public String NiceCont(Board brd){

        if(nice != null)
            return nice;

        Board tmp = new Board();
        tmp.loadFromFen(brd.getFen());
        StringBuilder curMv = new StringBuilder();
        //curMv.append("• ");
        for(String s:cont){

            curMv.append(Utils.PretifyMove(s, tmp));
            curMv.append(' ');

            try {
                Move mv = new Move(s, tmp.getSideToMove());
                tmp.doMove(mv);
            }catch (Exception ex){
                curMv.append("Err");
                return curMv.toString();
            }
        }

        if(curMv.length()>0)
            curMv.deleteCharAt(curMv.length()-1);
        nice = curMv.toString();

        return nice;
    }

    public void Parse(String s, Board tmpBoard, boolean getCont){
        SimpleTokScanner a = new SimpleTokScanner(s);
        String pp = a.getNext();
        if(!pp.equals("info"))
            return;
        boolean nextpv = false;
        while(true){
            String cur = a.getNext();
            if(cur == null)
                break;
            if(nextpv){
                if(move == null) {
                    //if(!tmpBoard.isMoveLegal(cur, false))
                    //  break;
                    if(tmpBoard != null) {
                        Move mv = new Move(cur, tmpBoard.getSideToMove());
                        Piece pc = tmpBoard.getPiece(mv.getFrom());
                        if (pc == Piece.NONE)
                            return;
                        if (tmpBoard.getSideToMove() == Side.WHITE) {
                            if (pc == Piece.BLACK_BISHOP || pc == Piece.BLACK_KING || pc == Piece.BLACK_PAWN || pc == Piece.BLACK_ROOK
                                    || pc == Piece.BLACK_KNIGHT || pc == Piece.BLACK_QUEEN)
                                break;
                        } else {
                            if (pc == Piece.WHITE_BISHOP || pc == Piece.WHITE_KING || pc == Piece.WHITE_PAWN || pc == Piece.WHITE_ROOK
                                    || pc == Piece.WHITE_KNIGHT || pc == Piece.WHITE_QUEEN)
                                break;
                        }

                        try {
                            if (!tmpBoard.isMoveLegal(mv, false))
                                break;
                        } catch (Exception ex) {
                            break;
                        }
                    }
                    move = cur;
                    if(!getCont)
                        break;
                }
                cont.add(cur);
                if(cont.size() >= 8)
                    break;
                continue;
            }
            if(cur.equals("mate")){
                cur = a.getNext();
                if (cur == null)
                    break;
                mateIn = Integer.parseInt(cur);
                cp = 10000000 * Integer.signum(mateIn) - mateIn;
                continue;
            }
            if(cur.equals("cp")) {
                cur = a.getNext();
                if (cur == null)
                    break;
                cp = Integer.parseInt(cur);
                continue;
            }
            if(cur.equals("pv")){
                nextpv = true;
                continue;
            }
        }
    }

    static public List<analitem> Parse(List<String> lst){

        HashMap<String, analitem> items = new HashMap<>();

        for (String s : lst) {
            var cur = new analitem();
            cur.Parse(s, null, false);
            if(cur.move != null)
                items.put(cur.move, cur);
        }

        return new ArrayList<>(items.values());
    }
}

