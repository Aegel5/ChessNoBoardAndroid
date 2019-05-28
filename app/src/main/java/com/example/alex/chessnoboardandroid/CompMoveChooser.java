package com.example.alex.chessnoboardandroid;

import android.util.Log;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.MoveBackup;
import com.github.bhlangonijr.chesslib.move.Move;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
Алгоритм выбора компьютерного хода
 */
public class CompMoveChooser {

    private static final String TAG = MainApp.MainTag + CompMoveChooser.class.getSimpleName();

    public static Move DoMoveChoose(
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

        Move bestmove = ChessLibWrapper.moveFromString(bestMoveString, st.board);

        Move moveTodo = bestmove;
        int moveScoreTodo = Integer.MIN_VALUE;

        if (!st.parm.isMaxStrength()) {
            //boolean isWhite = board.getSideToMove() == Side.WHITE;
            // получим все ходы компа.
            List<UciMove> possibleMoves = new ArrayList<>();
            Map<String, UciMove> allMoves = uci.getCurrentMovesScore();
            if (allMoves.isEmpty())
                throw new RuntimeException("moves is empty, buy best move exists");
            int minDeltaScore = StrengthRules.getMinDeltaScoreForLevel(st.parm.getCompStrength());
            int bestScore = allMoves.get(bestMoveString).getUniversalScore();

            int minPossibleScore = bestScore - minDeltaScore;

            int previusScore = Integer.MAX_VALUE;
            LinkedList<MoveBackup> backup = st.board.getBackup();
            if (!backup.isEmpty()) {
                Move mv = backup.getLast().getMove();
                st.board.undoMove();
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
            for (UciMove move : allMoves.values()) {
                if (move.getUniversalScore() >= minPossibleScore)
                    possibleMoves.add(move);
            }
            if (possibleMoves.isEmpty())
                throw new RuntimeException("possible moves empty");

            UciMove moveUci = possibleMoves.get(MainApp.rndFromRange(0, possibleMoves.size() - 1));
            moveTodo = ChessLibWrapper.moveFromString(moveUci.getMove(), st.board);

//                if (isMoveLegal(moveTodo, false) != MoveLegalResult.AllOk) {
//                    throw new RuntimeException("comp move not legal " + moveUci.move);
//                }

            StringBuilder allmv = new StringBuilder();
            for (int i = 0; i < possibleMoves.size(); i++) {
                allmv.append(possibleMoves.get(i).getMove());
                if (i != possibleMoves.size() - 1)
                    allmv.append(",");
            }
            moveScoreTodo = moveUci.getUniversalScore();
            Log.d(TAG, String.format("side=%s, choose move=%s(%d) from %d(%s)(all=%d), bestmove=%s(%d), level=%d",
                    st.board.getSideToMove().toString(),
                    moveUci.getMove(), moveUci.getUniversalScore(),
                    possibleMoves.size(), allmv.toString(), allMoves.size(),
                    bestMoveString, bestScore, st.parm.getCompStrength()));

        }

        // запомним best score для этой позиции
        if (moveScoreTodo != Integer.MIN_VALUE) {
            st.scoreCache.put(st.board.getFen(), moveScoreTodo);
            //Log.d(TAG, "safe score for fen " + board.getFen());
        }

        return moveTodo;

    }

}
