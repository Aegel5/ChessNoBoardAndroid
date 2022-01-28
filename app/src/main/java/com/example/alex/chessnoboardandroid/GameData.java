package com.example.alex.chessnoboardandroid;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.MoveBackup;
import com.github.bhlangonijr.chesslib.move.Move;
import com.google.gson.Gson;

import java.util.Map;
import java.util.TreeMap;

/*
    Данные игры которые сериализуются при паузе
*/
class GameData {
    public NewGameParams parm;
    public String curMove = "";
    public Map<String, Integer> scoreCache = new TreeMap<>();
    public GameState lastGameState;
    public transient Board board = null;
    public boolean compEnabled = false;
    public String[] moves;
    public int seldelt;

    public String Serialize() {

        if (!board.getBackup().isEmpty()) {
            int index = 0;
            moves = new String[board.getBackup().size()];
            for (MoveBackup mb : board.getBackup()) {
                moves[index++] = mb.getMove().toString();
            }
        }

        return (new Gson()).toJson(this);
    }

    public static GameData Deserialize(String str) {
        GameData data = (new Gson()).fromJson(str, GameData.class);
        data.board = new Board();
        if (data.moves != null) {

            for (String mv : data.moves) {
                data.board.doMove(new Move(mv, data.board.getSideToMove()), false);
            }
        }
        return data;
    }

}
