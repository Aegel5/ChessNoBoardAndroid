//package com.example.alex.chessnoboardandroid;
//
//import com.github.bhlangonijr.chesslib.Board;
//import com.github.bhlangonijr.chesslib.Piece;
//import com.github.bhlangonijr.chesslib.Side;
//import com.github.bhlangonijr.chesslib.move.Move;
//
//import java.util.ArrayList;
//import java.util.List;
//
//enum UciLineType{
//    no,
//    info,
//    best
//}
//
//public class UciLine {
//
//    UciLineType type = UciLineType.no;
//
//    public String move;
//    public Integer cp;
//    public List<String> cont = new ArrayList<>();
//    public  int number;
//    public  int mateIn;
//    String nice;
//
//    public String NiceCont(Board brd){
//        if(nice != null)
//            return nice;
//        Board tmp = new Board();
//        tmp.loadFromFen(brd.getFen());
//        StringBuilder curMv = new StringBuilder();
//        //curMv.append("• ");
//        for(String s:cont){
//            Move mv = new Move(s, tmp.getSideToMove());
//            curMv.append(getUnicodeFromPiece(tmp.getPiece(mv.getFrom())));
//            curMv.append(s.substring(0, 2));
//            curMv.append(getUnicodeFromPiece(tmp.getPiece(mv.getTo())));
//            curMv.append(s.substring(2, 4));
//
//            try {
//                tmp.doMove(mv);
//            }catch (Exception ex){
//                return "Err";
//            }
//
//            if(tmp.isMated()) {
//                curMv.append("#");
//            }else if(tmp.isKingAttacked()){
//                curMv.append("+");
//            }
//
//            curMv.append(' ');
//        }
//        if(curMv.length()>0)
//            curMv.deleteCharAt(curMv.length()-1);
//        nice = curMv.toString();
//        return nice;
//    }
//    public  void Parse(String s, Board tmpBoard){
//        SimpleTokScanner a = new SimpleTokScanner(s);
//        String pp = a.getNext();
//        if(!pp.equals("info"))
//            return;
//
//        boolean nextpv = false;
//        String first = null;
//        while(true){
//            String cur = a.getNext();
//            if(cur == null)
//                break;
//            if(nextpv){
//                if(first == null) {
//                    //if(!tmpBoard.isMoveLegal(cur, false))
//                    //  break;
//                    Move mv = new Move(cur, tmpBoard.getSideToMove());
//                    Piece pc = tmpBoard.getPiece(mv.getFrom());
//                    if(pc == Piece.NONE)
//                        break;
//                    if(tmpBoard.getSideToMove() == Side.WHITE) {
//                        if (pc == Piece.BLACK_BISHOP || pc == Piece.BLACK_KING || pc == Piece.BLACK_PAWN || pc == Piece.BLACK_ROOK
//                                || pc == Piece.BLACK_KNIGHT || pc == Piece.BLACK_QUEEN)
//                            break;
//                    }else{
//                        if (pc == Piece.WHITE_BISHOP || pc == Piece.WHITE_KING || pc == Piece.WHITE_PAWN || pc == Piece.WHITE_ROOK
//                                || pc == Piece.WHITE_KNIGHT || pc == Piece.WHITE_QUEEN)
//                            break;
//                    }
//
//                    try {
//                        if (!tmpBoard.isMoveLegal(mv, false))
//                            break;
//                    }
//                    catch (Exception ex){
//                        break;
//                    }
//                    first = cur;
//                }
//                cont.add(cur);
//                if(cont.size() >= 4)
//                    break;
//                continue;
//            }
//            if(cur.equals("mate")){
//                cur = a.getNext();
//                if (cur == null)
//                    break;
//                mateIn = Integer.parseInt(cur);
//                if(tmpBoard.getSideToMove() == Side.BLACK)
//                    mateIn =-mateIn;
//                cp = 10000000 * Integer.signum(mateIn) - mateIn;
//                continue;
//            }
//            if(cur.equals("cp")) {
//                cur = a.getNext();
//                if (cur == null)
//                    break;
//                cp = Integer.parseInt(cur);
//                if(tmpBoard.getSideToMove() == Side.BLACK)
//                    cp =-cp;
//                continue;
//            }
//            if(cur.equals("pv")){
//                nextpv = true;
//                continue;
//            }
//        }
//        if(first != null){
//            move = first;
//        }
//    }
//}
