package com.example.alex.chessnoboardandroid;

import android.util.Log;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.MoveBackup;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.move.Move;

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

    static public int getMinDeltaScoreForLevel(int level) {
        return floatDeltaScore(level);
    }

    /*
    Вычислим score в пределах которого компьютер может ходить (сколько пешек может залить за ход)
     */
    static private int strickDeltaScore(int level) {

        int result = 0;

        if (level == 0)
            // полный рандом
            result = 1000000;
        else if (level == 1)
            result = 800;
        else if (level == 2)
            result = 700;
        else if (level == 3)
            result = 600;
        else if (level == 4)
            result = 550;
        else if (level == 5)
            result = 500;
        else if (level == 6)
            result = 450;
            // дальше равномерно
        else {
            int last_lvl = 6;
            double rem_pwn = 450;

            var cur = level-last_lvl;
            var prc = cur / (double)(NewGameParams.getMaxLevel()-last_lvl);
            prc = 1-prc;

            result = (int) (prc * rem_pwn);
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

    static double  dist(String s1, String s2){
        double x = (s1.charAt(0) - 'a') - (s2.charAt(0) - 'a');
        double y = (s1.charAt(1) - '1') - (s2.charAt(1) - '1');
        return Math.sqrt(x*x+y*y);
    }

    static int MateToCp(int mateIn){

        var absMate = Math.abs(mateIn);
        var sgn = Integer.signum(mateIn);

        if (absMate == 1)
            return sgn * 700; // приравнивается к чистому ферзю. todo конем считать меньше (типо труднее заметить)
        if (absMate == 2)
            return sgn * 600;
        if(absMate == 3)
            return sgn * 500;

        return sgn * 400;
    }

    public static class CandidatMove {
        public double rnd;
        private String move;
        public  int cp;
    }

    public static String DoMoveChoose(
            String lineBestMove,
            GameData st,
            UCIWrapper uci) {

        Pattern pattern = Pattern.compile("bestmove (\\w*)");
        Matcher match = pattern.matcher(lineBestMove);
        if (!match.find())
            new RuntimeException("not found move for: " + lineBestMove);

        String bestMoveString = match.group(1).trim();
        Log.d(TAG, "comp move: " + bestMoveString);
        if (bestMoveString.length() < 4)
            throw new RuntimeException("bad move string: " + bestMoveString);

        if (st.parm.isMaxStrength()) {
            return bestMoveString;
        }

        var lines = uci.curLines();
        if (lines.isEmpty())
            throw new RuntimeException("moves is empty, but best move exists");

        var parsed = analitem.Parse(lines);

        Collections.sort(parsed, (u1, u2) -> u2.cp.compareTo(u1.cp));


        int cntGet = 0;
        int sumGet = 0;
        for (var item : parsed) {
            if(item.mateIn == 0) {
                cntGet++;
                sumGet += item.cp;
                if(cntGet >= 10)
                    break;
            }
        }

        // оценка позиции без учета мата - среднее 10 лучших ходов
        int etalon = cntGet == 0 ? 0 : (int) ((double)sumGet / cntGet);

        // пропатчим все матовые ходы согласно эталону
        for (var item : parsed) {
            if(item.mateIn != 0) {
                item.cp = etalon + MateToCp(item.mateIn);
            }
        }

        // снова сортируем
        Collections.sort(parsed, (u1, u2) -> u2.cp.compareTo(u1.cp));

        // Теперь у нас все готово для выбора хода

        int minDeltaScore = getMinDeltaScoreForLevel(st.parm.getCompStrength());
        int bestScore = parsed.get(0).cp;

        int minPossibleScore = bestScore - minDeltaScore;

        int previusScore = Integer.MAX_VALUE;
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
            if (previusScore != Integer.MAX_VALUE) {
                Log.d(TAG, "found previous score " + previusScore);
            }
        }

        int origMinPossibleScore = minPossibleScore;
        if (minPossibleScore > previusScore) {
            // Пользователь зевнул, на низких уровнях не будем принимать его зевок
            double total = minPossibleScore - previusScore;
            double step = total / NewGameParams.getMaxLevel();
            double addFor = step * st.parm.getCompStrength();
            minPossibleScore = previusScore + (int) Math.round(addFor);
            Log.d(TAG, String.format("уменьшаем minPossibleScore %d -> %d ", origMinPossibleScore, minPossibleScore));

        }

        Log.d(TAG, String.format("best score=%d, minpossiblescore=%d(%d), prevScore=%d",
                bestScore, minPossibleScore, origMinPossibleScore, previusScore));

        List<CandidatMove> possibleMoves = new ArrayList<>();

        for (var item : parsed) {
            if (item.cp >= minPossibleScore) {
                CandidatMove mv = new CandidatMove();
                mv.move = item.move;
                mv.cp = item.cp;
                possibleMoves.add(mv);
            }
            else
                break; // sorted
        }

        if (possibleMoves.isEmpty())
            throw new RuntimeException("possible moves empty");

        String prevto = null;
        if (prevComp != null) {
            prevto = prevComp.getTo().value().toLowerCase();
        }

        // todo probability by distance from prev move. prev-to <-> cur-from
        // в таком случае фигура которой ходили в предыдущий раз (если такая есть в этой выборке, получит мак значение)
        // weight = 20 - dist
        for (var m : possibleMoves) {
            m.rnd = 20;
            if (prevto != null) {
                double dist = dist(prevto, m.move.substring(0, 2));
                m.rnd -= dist;
            }
        }

        double sumd = 0;
        for (var m : possibleMoves) {
            sumd += m.rnd;
        }

        double r = MainApp.rand().nextDouble();
        r *= sumd;

        double cursum = 0;
        CandidatMove candMv = null;
        for (var m : possibleMoves) {
            cursum += m.rnd;
            if (r <= cursum) {
                candMv = m;
                break;
            }

        }

        if (candMv == null) {
            candMv = possibleMoves.get(MainApp.rndFromRange(0, possibleMoves.size() - 1)); // fallback
        }

        StringBuilder allmv = new StringBuilder();
        for (int i = 0; i < possibleMoves.size(); i++) {
            allmv.append(possibleMoves.get(i).move);
            if (i != possibleMoves.size() - 1)
                allmv.append(",");
        }

        var moveScoreTodo = candMv.cp;
        Log.d(TAG, String.format("side=%s, choose move=%s(%d) from %d(%s)(all=%d), bestmove=%s(%d), level=%d",
                st.board.getSideToMove().toString(),
                candMv.move, candMv.cp,
                possibleMoves.size(), allmv.toString(), parsed.size(),
                bestMoveString, bestScore, st.parm.getCompStrength()));


        // запомним best score для этой позиции
        if (moveScoreTodo != Integer.MIN_VALUE) {
            st.board.doMove(new Move(candMv.move, st.board.getSideToMove()));
            st.scoreCache.put(st.board.getFen(), moveScoreTodo);
            st.board.undoMove();
            //Log.d(TAG, "safe score for fen " + board.getFen());
        }

        return candMv.move;
    }

}
