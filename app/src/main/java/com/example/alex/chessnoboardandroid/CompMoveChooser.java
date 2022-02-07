package com.example.alex.chessnoboardandroid;

import android.util.Log;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.MoveBackup;
import com.github.bhlangonijr.chesslib.PieceType;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
Алгоритм выбора компьютерного хода
 */
public class CompMoveChooser {

    private static final String TAG = MainApp.MainTag + CompMoveChooser.class.getSimpleName();

    static public double getMinDeltaScoreForLevel(int level) {
        return floatDeltaScore(level);
    }

    // 0 -> 0
    // x_max -> y_max
    static double exp_map(double xmax, double ymax, double x){
        double res = Math.pow(ymax+1, x/xmax);
        return res-1;
    }
    // koeff < 1 - кривая стремится к прямой
    // koeff > 1 - кривая стремится к углу.
    static double exp_map(double xmax, double ymax, double koeff, double x){
        var res = exp_map(xmax, ymax*koeff, x);
        return res/koeff;
    }

    /*
    Вычислим score в пределах которого компьютер может ходить (сколько пешек может залить за ход)
     */
    static private double strickDeltaScore(int level) {

        if(level == 0)
            return 1000000; // полный рандом

        level = NewGameParams.getMaxLevel() - level;

        // exponent dependence. 1-level=14, max-level=0.
        var res = exp_map(NewGameParams.getMaxLevel() - 1, 14, 1.1, level);
        return res;
    }

    /*
    Для большего рандома компьютер может перейти на более низкий уровень на один ход с небольшой вероятностью
    Эмуляция зевка для компьютера.
     */
    static private double floatDeltaScore(int level) {
        // с более высокого есть шанс перейти на более низкий (зевок)
        double prob = (NewGameParams.getMaxLevel() - level) * 0.005;
        if (MainApp.isprob(prob)) {
            int newLevel = MainApp.rndFromRange(0, level);
            Log.d(TAG, String.format("Tag decress with prob %f from level %d to %d", prob, level, newLevel));
            level = newLevel;

        }
        return strickDeltaScore(level);
    }

    static double  dist(String s1, String s2){
        double x = s1.charAt(0)  - s2.charAt(0);
        double y = s1.charAt(1) - s2.charAt(1);
        return Math.sqrt(x*x+y*y);
    }

    static double MateToCp(int mateIn){

        var absMate = Math.abs(mateIn);
        var sgn = Integer.signum(mateIn);

        if (absMate == 1)
            return sgn * 7.0; // приравнивается к чистому ферзю. todo конем считать меньше (типо труднее заметить)
        if (absMate == 2)
            return sgn * 6.0;
        if(absMate == 3)
            return sgn * 5.0;

        return sgn * 4.0;
    }

    static  double CpByMaterial(Board brd) {

        double total = 0;
        for (Square square : Square.values()) {
            var cur = brd.getPiece(square);
            var type = cur.getPieceType();

            if(type == PieceType.NONE)
                continue;

            double curcp = 0;

            if (type == PieceType.PAWN) {
                curcp = 1;
            } else if (type == PieceType.BISHOP || type == PieceType.KNIGHT) {
                curcp = 3;
            } else if (type == PieceType.QUEEN ) {
                curcp = 9;
            }else if (type == PieceType.ROOK ) {
                curcp = 5;
            }

            if(cur.getPieceSide() == Side.BLACK){
                curcp = -curcp;
            }

            total += curcp;
        }

        if(brd.getSideToMove() == Side.BLACK){
            total = -total;
        }

        return total;
    }

    public static String DoMoveChoose(
            String lineBestMove,
            GameData st,
            UCIWrapper uci) {

        var scanbest = new SimpleTokScanner(lineBestMove);
        scanbest.skip("bestmove");
        String bestMoveString = scanbest.getNext();

        Log.d(TAG, "comp move: " + bestMoveString);
        if (bestMoveString == null || bestMoveString.length() < 4)
            throw new RuntimeException("bad move string: " + bestMoveString);

        if (st.parm.isMaxStrength()) {
            return bestMoveString;
        }

        var lines = uci.curLines();
        var parsed = analitem.Parse(lines);

        if (parsed.isEmpty())
            throw new RuntimeException("moves is empty, but best move exists");

        if(st.parm.getCompStrength() == 0){
            return parsed.get(MainApp.rndFromRange(0, parsed.size() - 1)).move; // todo use generateLegalMoves instead
        }

        Collections.sort(parsed, (u1, u2) -> u2.cp.compareTo(u1.cp));

        int cntGet = 0;
        double sumGet = 0;
        for (var item : parsed) {
            if (item.mateIn == 0) {
                cntGet++;
                sumGet += item.cp;
                if (cntGet >= 5)
                    break;
            }
        }

        // оценка позиции без учета мата - среднее 5 лучших ходов + оценка по материалу
        double etalon = cntGet == 0 ? 0 : sumGet / cntGet;
        etalon += 2*CpByMaterial(st.board);
        etalon /= 3;

        // пропатчим все матовые ходы согласно эталону
        for (var item : parsed) {
            if (item.mateIn != 0) {
                item.cp = etalon + MateToCp(item.mateIn);
            }
        }

        // снова сортируем
        Collections.sort(parsed, (u1, u2) -> u2.cp.compareTo(u1.cp));

        //уменьшим разброс, чтобы добавить больше энтропии в принципиальных позициях
        var avr = parsed.stream().mapToDouble((u) -> u.cp).average().getAsDouble();
        for (var item : parsed) {
            var newcp = avr + Math.abs(item.cp - avr) * 0.9 * Math.signum(item.cp-avr);
            item.cp = newcp;
        }

        // Теперь у нас все готово для выбора хода

        var minDeltaScore = getMinDeltaScoreForLevel(st.parm.getCompStrength());
        var bestScore = parsed.get(0).cp;

        var minPossibleScore = bestScore - minDeltaScore;

        var previusScore = Double.MAX_VALUE;
        LinkedList<MoveBackup> backup = st.board.getBackup();
        Move prevComp = null;
        if (!backup.isEmpty()) {

            if (backup.size() >= 2) {
                Iterator lit = backup.descendingIterator();
                lit.next();
                prevComp = ((MoveBackup) lit.next()).getMove();
            }

            Move mv = backup.getLast().getMove();
            st.board.undoMove(); // откатываем ход пользователя
            String fen = st.board.getFen();
            st.board.doMove(mv);
            //Log.d(TAG,"try find for fen "+ fen);
            previusScore = st.scoreCache.getOrDefault(fen, previusScore);
            if (previusScore != Double.MAX_VALUE) {
                Log.d(TAG, "found previous score " + previusScore);
            }
        }

        var origMinPossibleScore = minPossibleScore;
        if (minPossibleScore > previusScore) {
            // Пользователь зевнул, на низких уровнях не будем принимать его зевок
            double total = minPossibleScore - previusScore;
            minPossibleScore = previusScore + exp_map(NewGameParams.getMaxLevel(), total, st.parm.getCompStrength());
            Log.d(TAG, MessageFormat.format("уменьшаем minPossibleScore {0} -> {1}", origMinPossibleScore, minPossibleScore));

        }

        Log.d(TAG, MessageFormat.format("best score={0}, minpossiblescore={1}({2}), prevScore={3}",
                bestScore, minPossibleScore, origMinPossibleScore, previusScore));

        List<analitem> possibleMoves = new ArrayList<>();

        for (var item : parsed) {
            if (item.cp >= minPossibleScore) {
                possibleMoves.add(item);
            } else
                break; // sorted
        }

        if (possibleMoves.isEmpty())
            throw new RuntimeException("possible moves empty");

        // добавим веса, чтобы у лучших ходов было немного больше вероятности быть вытянутыми
        var min = possibleMoves.get(possibleMoves.size()-1).cp;
        var max = possibleMoves.get(0).cp;
        var len = max - min;
        if(len > 0) {
            for (var m : possibleMoves) {
                m.rnd += (m.cp - min) / len * 8;
            }
        }

        // применим любимую фигуру компа
        if(st.parm.compFavoriteFigure != PieceType.NONE){
            for (var m : possibleMoves) {
                var sq = Square.fromValue(m.move.substring(0,2).toUpperCase());
                if(st.board.getPiece(sq).getPieceType() == st.parm.compFavoriteFigure){
                    m.rnd += 5 + st.parm.favoritePrc * 15;
                }
            }
        }

        // probability by distance from prev move. prev-to <-> cur-from
        // в таком случае фигура которой ходили в предыдущий раз (если такая есть в этой выборке, получит мак значение)
        // weight = 20 - dist
        String prevto = null;
        if (prevComp != null) {
            prevto = prevComp.getTo().value().toLowerCase();
        }
        double sumd = 0;
        for (var m : possibleMoves) {
            m.rnd += 20;
            if (prevto != null) {
                double dist = dist(prevto, m.move.substring(0, 2));
                m.rnd -= dist;
            }
            sumd += m.rnd;
        }

        double r = MainApp.rand().nextDouble();
        r *= sumd;

        double cursum = 0;
        int numb = 0;
        analitem candMv = null;
        for (var m : possibleMoves) {
            cursum += m.rnd;
            numb++;
            if (r <= cursum) {
                candMv = m;
                break;
            }
        }

        Log.d(TAG, MessageFormat.format("choose move {0} from {1}", numb, possibleMoves.size()));

        if (candMv == null) {
            candMv = possibleMoves.get(MainApp.rndFromRange(0, possibleMoves.size() - 1)); // fallback
        }

        // запомним best score для этой позиции
        st.board.doMove(new Move(candMv.move, st.board.getSideToMove()));
        st.scoreCache.put(st.board.getFen(), candMv.cp);
        st.board.undoMove();

        return candMv.move;
    }

}
