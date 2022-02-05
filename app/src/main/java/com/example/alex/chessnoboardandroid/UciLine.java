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
    public Double cp;
    public List<String> cont = new ArrayList<>();
    public  int number;
    public  int mateIn;
    public double rnd;
    String nice;

    public String NiceCont(Board brd){

        if(nice != null)
            return nice;

        Board tmp = new Board();
        tmp.loadFromFen(brd.getFen());
        StringBuilder curMv = new StringBuilder();
        //curMv.append("• ");
        for(String s:cont){


            try {
                Utils.PretifyAndMove(curMv, s, tmp, true);
                curMv.append(' ');
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

    public void Parse(String s, Board tmpBoard, boolean getCont) {

        SimpleTokScanner a = new SimpleTokScanner(s);

        if (!"info".equals(a.getNext()))
            return;

        a.skip("score");
        String cur = a.getNext();
        if (cur == null)
            return;
        else if (cur.equals("mate")) {
            cur = a.getNext();
            if (cur == null)
                return;
            mateIn = Integer.parseInt(cur);
            cp = 10000000.0 * Integer.signum(mateIn) - mateIn;
        } else if (cur.equals("cp")) {
            cur = a.getNext();
            if (cur == null)
                return;
            cp = Integer.parseInt(cur)/100.0;
        }

        a.skip("pv");

        while (true) {

            cur = a.getNext();
            if (cur == null)
                break;

            if (move == null) {

                if (tmpBoard != null) {
                    Move mv = new Move(cur, tmpBoard.getSideToMove());
                    Piece pc = tmpBoard.getPiece(mv.getFrom());

                    if (pc == Piece.NONE)
                        break;
                    if (tmpBoard.getSideToMove() != pc.getPieceSide()) {
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
                if (!getCont)
                    break;
            }
            cont.add(cur);
            if (cont.size() >= 8)
                break;
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

