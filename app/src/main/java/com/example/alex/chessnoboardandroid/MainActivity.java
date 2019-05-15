package com.example.alex.chessnoboardandroid;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.Selection;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.MoveBackup;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;
import com.github.bhlangonijr.chesslib.move.MoveGenerator;
import com.github.bhlangonijr.chesslib.move.MoveGeneratorException;
import com.github.bhlangonijr.chesslib.move.MoveList;
import com.google.gson.Gson;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;


public class MainActivity extends AppCompatActivity {

    static final int PICK_NEW_GAME_REQUEST = 0;
    private static final String TAG = MainApp.MainTag + UCIWrapper.class.getSimpleName();
    UCIWrapper uci = new UCIWrapper();
    Button[] buttonsLetters = null;
    ToggleButton btnIsCompEnabled = null;
    BoardViewListener boardViewListener;
    Button btnBoard;
    PopupWindow popupWindow;
    TextView boardView;
    final Handler handler = new Handler();
    CompMoveWaiter compMoveWaiter = null;
    GameData st;
    static final String STATE_GAMESTATE = "gameState";
    boolean activityStarted = false;
    public static final String APP_PREFERENCES = "mysettings";
    public static final String APP_PREFERENCES_STATE = "state"; // имя кота
    SharedPreferences mSettings;
    TextView textMoves;
    private RecyclerView mRecyclerView;
    private DataAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    List<DisplayMoveItem> lstMoveItemsDisplay = new ArrayList<>();

    class GameData{
        NewGameParams parm;
        String curMove = "";
        Map<String, Integer> scoreCache = new TreeMap<>();
        GameState lastGameState;
        transient Board board = null;
        boolean compEnabled = false;
        String[] moves;

        String Serialize(){

            if(!board.getBackup().isEmpty()) {
                int index = 0;
                moves = new String[board.getBackup().size()];
                for (MoveBackup mb : board.getBackup()) {
                    moves[index++] = mb.getMove().toString();
                }
            }

            return (new Gson()).toJson(this);

        }

    }

    GameData Deserialize(String str){
        GameData data = (new Gson()).fromJson(str, GameData.class);
        data.board = new Board();
        if(data.moves != null) {

            for (String mv : data.moves) {
                data.board.doMove(new Move(mv, data.board.getSideToMove()), false);
            }
        }
        return data;
    }

    class BoardViewListener implements View.OnTouchListener {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if(st.parm.boardMode == BoardMode.Hardcore)
                return false;
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if(st.parm.boardMode == BoardMode.AllowViewFigures)
                        printAllMoves(true);
                    if(st.parm.boardMode.getIndex() >= BoardMode.AllowViewTextBoard.getIndex()){
                        showBoardPopup();
                    }
                    break;
                case MotionEvent.ACTION_UP: // отпускание
                case MotionEvent.ACTION_CANCEL:
                    if(st.parm.boardMode == BoardMode.AllowViewFigures)
                        printAllMoves(false);
                    if(st.parm.boardMode.getIndex() >= BoardMode.AllowViewTextBoard.getIndex()){
                        popupWindow.dismiss();
                    }
                    break;
            }
            return true;
        }
    }

    void fillButtonsLetter() {
        buttonsLetters = new Button[8];
        buttonsLetters[0] = findViewById(R.id.l1);
        buttonsLetters[1] = findViewById(R.id.l2);
        buttonsLetters[2] = findViewById(R.id.l3);
        buttonsLetters[3] = findViewById(R.id.l4);
        buttonsLetters[4] = findViewById(R.id.l5);
        buttonsLetters[5] = findViewById(R.id.l6);
        buttonsLetters[6] = findViewById(R.id.l7);
        buttonsLetters[7] = findViewById(R.id.l8);
    }

    void initPopup() {
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.layout_board, null);

        // create the popup window
        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        boolean focusable = true; // lets taps outside the popup also dismiss it
        popupWindow = new PopupWindow(popupView, width, height, focusable);
        boardView = popupView.findViewById(R.id.boardView);
    }


    String getUnicodeFromPiece(Piece piece){
        if(     piece == Piece.WHITE_KING  ) return(Character.toString((char)0x2654));
        else if(piece == Piece.WHITE_QUEEN ) return(Character.toString((char)0x2655));
        else if(piece == Piece.WHITE_ROOK  ) return(Character.toString((char)0x2656));
        else if(piece == Piece.WHITE_BISHOP) return(Character.toString((char)0x2657));
        else if(piece == Piece.WHITE_KNIGHT) return(Character.toString((char)0x2658));
        else if(piece == Piece.WHITE_PAWN  ) return(Character.toString((char)0x2659));
        else if(piece == Piece.BLACK_KING  ) return(Character.toString((char)0x265A));
        else if(piece == Piece.BLACK_QUEEN ) return(Character.toString((char)0x265B));
        else if(piece == Piece.BLACK_ROOK  ) return(Character.toString((char)0x265C));
        else if(piece == Piece.BLACK_BISHOP) return(Character.toString((char)0x265D));
        else if(piece == Piece.BLACK_KNIGHT) return(Character.toString((char)0x265E));
        else if(piece == Piece.BLACK_PAWN  ) return(Character.toString((char)0x265F));
        return "";
    }

    public void showBoardPopup() {
        try {
            Log.d(TAG, "showBoardPopup");

            String boardStr = st.board.toString();
            String text = boardStr.substring(0, boardStr.indexOf("Side:") - 1);
            StringBuilder result = new StringBuilder();
            for (char c : text.toCharArray()) {
                if (c == ' ') result.append('.');
                else result.append(c);
            }

            boardView.setText(result.toString());
            popupWindow.showAtLocation(btnBoard, Gravity.CENTER, 0, 0);

        } catch (Exception e) {
            Log.d(TAG, "Exception showBoardPopup: " + e.toString());
        }
    }

    void doMove(String move) {
        st.board.doMove(new Move(move, st.board.getSideToMove()));
    }

    void testProm() {
        try {
            doMove("e2e4");
            doMove("a7a6");
            doMove("f1a6");
            doMove("a8a6");
            doMove("a2a4");
            doMove("a6b6");
            doMove("a4a5");
            doMove("b6c6");
            doMove("a5a6");
            doMove("c6d6");
            doMove("a6a7");
            doMove("d6e6");
            updateGui();
        } catch (Exception e) {
            Log.d(TAG, "Exception testProm: " + e.toString());
        }
    }

    void testMate() {
        try {
            st.board = new Board();
            doMove("e2e4");
            doMove("a7a6");
            doMove("f1c4");
            doMove("a6a5");
            doMove("d1h5");
            doMove("a5a4");
            //doMove("h5f7");

            updateGui();
        } catch (Exception e) {
            Log.d(TAG, "Exception testProm: " + e.toString());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.d(TAG, "onCreate in thread " + Thread.currentThread().getId());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {

            initPopup();
            initUci();
            initGui();

            mSettings = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);
            String stateString = mSettings.getString(APP_PREFERENCES_STATE, null);
            loadPrevState(stateString);
            if (st == null) {
                st = startNewGame(new NewGameParams());
            }

            //testProm();
            //testMate();

        } catch (Exception e){
            Log.d(TAG, Utils.printException(e));
            throw e;
        }
    }

    @Override
    protected void onRestart() {
        Log.d(TAG, "onRestart");
        super.onRestart();



    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();

        if(st == null)
            return;

        try {
            SharedPreferences.Editor editor = mSettings.edit();
            String stateString = st.Serialize();
            Log.d(TAG, "state is\n" + stateString);
            editor.putString(APP_PREFERENCES_STATE, stateString);
            editor.apply();
        } catch (Exception e){
            Log.d(TAG, Utils.printException(e));
        }


    }


    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();



        activityStarted = true;
    }


//    @Override
//    protected void onSaveInstanceState(Bundle outState) {
//        super.onSaveInstanceState(outState);
//        Log.d(TAG, "onSaveInstanceState");
//        if(st == null)
//            return;
//        try {
//            String stateString = st.Serialize();
//            Log.d(TAG, "state is\n" + stateString);
//            outState.putString(STATE_GAMESTATE, stateString);
//        } catch (Exception e){
//            Log.d(TAG, Utils.printException(e));
//        }
//    }

    void loadPrevState(String stateString){
        Log.d(TAG, "loadPrevState");

        if(stateString == null)
            return;

        try {
            st = Deserialize(stateString);
            updateCompOnButtonText();
            updateGui();
        } catch (Exception e){
            Log.d(TAG, Utils.printException(e));
        }
    }

//    @Override
//    protected void onRestoreInstanceState(Bundle savedInstanceState) {
//
//        Log.d(TAG, "onRestoreInstanceState");
//
//        super.onRestoreInstanceState(savedInstanceState);
//        String stateString = savedInstanceState.getString(STATE_GAMESTATE);
//        loadPrevState(stateString);
//    }

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart");
        super.onStart();


    }


    private void initGui() {
        textMoves = findViewById(R.id.textMoves);
        //textMoves.setMovementMethod(new ScrollingMovementMethod());
        fillButtonsLetter();
        btnIsCompEnabled = findViewById(R.id.isCompEnabled);
        btnIsCompEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(compMoveWaiter != null)
                return;
            if(activityStarted == false)
                return;
            st.compEnabled = isChecked;
            if (isChecked) {
                st.curMove = "";
                doCompMove();
                updateGui();
            }
        });
        btnBoard = findViewById(R.id.btnBoard);
        boardViewListener = new BoardViewListener();
        btnBoard.setOnTouchListener(boardViewListener);

        (findViewById(R.id.btnBS)).setOnLongClickListener(v -> {
            revertMove();
            return true;
        });


        mRecyclerView = (RecyclerView) findViewById(R.id.my_recycler_view);
        //mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mAdapter = new DataAdapter(this);
        mRecyclerView.setAdapter(mAdapter);
    }

    void revertMove() {
        try {
            if(compMoveWaiter != null)
                return;
            Log.d(TAG, "revertMove");
            st.curMove = "";
            if (!st.board.getBackup().isEmpty()) {
                st.board.undoMove();
                st.lastGameState = getGameState();
            }
            updateGui();
        } catch (Exception e) {
            Log.d(TAG, "Exception revertMove: " + e.toString());
        }
    }


    private void initUci() {
        try {
            boolean found_x86 = false;
            for (String item : Build.SUPPORTED_32_BIT_ABIS) {
                if (item.indexOf("x86") != -1) {
                    found_x86 = true;
                }
                Log.d(TAG, item);
            }
            String stockFileName = "stockfish_exe_arm7";
            //String stockFileName = "stockfish_exe_arm7_rand";

            if (found_x86)
                stockFileName = "stockfish_exe_x86";
            String path = Utils.unzipExeFromAsset(stockFileName, this);
            uci.init(path);
            //uci.send("uci");
            //uci.wait("uciok");

            // показываем 50 возможных ходов и их score
            uci.send("setoption name MultiPV value 50");

            uci.clearOutput();

        } catch (Exception e) {
            Log.d(TAG, Utils.printException(e));
        }
    }

    void updateGui() {
        printAllMoves();
        updateButtonTexts();
    }

    void updateButtonTexts() {
        boolean fLetters = true;
        boolean fPromotion = false;
        if (st.curMove.length() == 4)
            fPromotion = true;
        else if (st.curMove.length() % 2 == 1)
            fLetters = false;

        if (fPromotion) {
            buttonsLetters[0].setText("n");
            buttonsLetters[1].setText("b");
            buttonsLetters[2].setText("r");
            buttonsLetters[3].setText("q");
            for (int i = 4; i < 8; i++) {
                buttonsLetters[i].setText("");
            }
        } else {
            for (int i = 0; i < 8; i++) {
                Button bt = buttonsLetters[i];
                if (fLetters)
                    bt.setText(Character.toString((char) ('a' + i)));
                else
                    bt.setText(Integer.toString(i + 1));
            }
        }


    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(false);
    }

    void updateCompOnButtonText(){
        btnIsCompEnabled.setChecked(st.compEnabled);
        btnIsCompEnabled.setTextOn(String.format("on(%d)", st.parm.compStrength));
        btnIsCompEnabled.setTextOff(String.format("off"));
    }

    GameData startNewGame(NewGameParams parms) {

        try {
            st = new GameData();

            int level = parms.compStrength;
            if (level < 0 || level > 20)
                throw new RuntimeException("wrong level " + level);
            st.parm = parms;


            uci.send("ucinewgame");

            //uci.send(String.format("setoption name Skill Level value %d", level));

            st.board = new Board();
            st.lastGameState = GameState.InProcess;

            updateCompOnButtonText();
            updateGui();

            Log.d(TAG, "Game state created with level " + level);

            return st;
        } catch (Exception e) {
            Log.d(TAG, Utils.printException(e));
            return null;
        }
    }

    void doIntMove(Move move) {
        st.board.doMove(move);
        st.lastGameState = getGameState();
    }



    class CompMoveWaiter{
        public  CompMoveWaiter(int time) {
            scheduleWait(time);
        }
        int lastCheckedIndex = 0;
        void scheduleWait(int time) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    checkCompDoMove();
                }
            }, time);
        }
        void checkCompDoMove() {
            Log.d(TAG, "thread is " + Thread.currentThread().getId());
            List<String> lines = uci.curLines();
            String result = null;
            if (!lines.isEmpty()) {
                for (; lastCheckedIndex < lines.size(); lastCheckedIndex++) {
                    String line = lines.get(lastCheckedIndex);
                    if (line.startsWith("bestmove")) {
                        result = line;
                        break;
                    }
                }
            }
            if (result != null) {
                compMoveWaiter = null;
                actualCompDoMove(result);
                printAllMoves();
            } else {
                scheduleWait(10);
            }

        }
    }



    void actualCompDoMove(String lineBestMove) {
        try {

            Pattern pattern = Pattern.compile("bestmove (\\w*)");
            Matcher match = pattern.matcher(lineBestMove);
            if (!match.find())
                new RuntimeException("not found move for: " + lineBestMove);

            String bestMoveString = match.group(1).trim();
            Log.d(TAG, "comp move: " + bestMoveString);
            if (bestMoveString.length() < 4)
                throw new RuntimeException("bad move string: " + bestMoveString);

            Move bestmove = moveFromString(bestMoveString);

//            if (isMoveLegal(bestmove, false) != MoveLegalResult.AllOk)
//                throw new RuntimeException("illegal comp move: " + bestMoveString);

            Move moveTodo = bestmove;
            int moveScoreTodo = Integer.MIN_VALUE;

            if (!st.parm.isMaxStrength()) {
                //boolean isWhite = board.getSideToMove() == Side.WHITE;
                // получим все ходы компа.
                List<UciMove> possibleMoves = new ArrayList<>();
                Map<String, UciMove> allMoves = uci.getCurrentMovesScore();
                if (allMoves.isEmpty())
                    throw new RuntimeException("moves is empty, buy best move exists");
                int minDeltaScore = StrengthRules.getMinDeltaScoreForLevel(st.parm.compStrength);
                int bestScore = allMoves.get(bestMoveString).score;

                int minPossibleScore = bestScore - minDeltaScore;

                int previusScore = Integer.MAX_VALUE;
                LinkedList<MoveBackup> backup = st.board.getBackup();
                if(!backup.isEmpty()){
                    Move mv = backup.getLast().getMove();
                    st.board.undoMove();
                    String fen = st.board.getFen();
                    st.board.doMove(mv);
                    //Log.d(TAG,"try find for fen "+ fen);
                    previusScore = st.scoreCache.getOrDefault(fen, previusScore);
                    if(previusScore != Integer.MAX_VALUE){
                        Log.d(TAG, "found previous score " + previusScore);
                    }


                }

                int origMinPossibleScore = minPossibleScore;
                if(minPossibleScore > previusScore) {
                    // Пользователь зевнул, на низких уровнях не будем принимать его зевок
                    double total = minPossibleScore - previusScore;
                    double step = total / NewGameParams.maxLevel;
                    double addFor = step * st.parm.compStrength;
                    minPossibleScore = previusScore + (int)Math.round(addFor);
                    Log.d(TAG, String.format("уменьшаем minPossibleScore %d -> %d ", origMinPossibleScore, minPossibleScore));

                }

                Log.d(TAG, String.format("best score=%d, minpossiblescore=%d(%d), prevScore=%d",
                        bestScore, minPossibleScore, origMinPossibleScore, previusScore));
                for (UciMove move : allMoves.values()) {
                    if (move.score >= minPossibleScore)
                        possibleMoves.add(move);
                }
                if (possibleMoves.isEmpty())
                    throw new RuntimeException("possible moves empty");

                UciMove moveUci = possibleMoves.get(MainApp.rndFromRange(0, possibleMoves.size() - 1));
                moveTodo = moveFromString(moveUci.move);

//                if (isMoveLegal(moveTodo, false) != MoveLegalResult.AllOk) {
//                    throw new RuntimeException("comp move not legal " + moveUci.move);
//                }

                StringBuilder allmv = new StringBuilder();
                for(int i = 0; i<possibleMoves.size();i++)
                {
                    allmv.append(possibleMoves.get(i).move);
                    if(i != possibleMoves.size()-1)
                        allmv.append(",");
                }
                moveScoreTodo = moveUci.score;
                Log.d(TAG, String.format("side=%s, choose move=%s(%d) from %d(%s)(all=%d), bestmove=%s(%d), level=%d",
                        st.board.getSideToMove().toString(),
                        moveUci.move, moveUci.score,
                        possibleMoves.size(), allmv.toString(), allMoves.size(),
                        bestMoveString, bestScore, st.parm.compStrength));

            }

            doIntMove(moveTodo);

            // запомним best score для этой позиции
            if(moveScoreTodo != Integer.MIN_VALUE) {
                st.scoreCache.put(st.board.getFen(), moveScoreTodo);
                //Log.d(TAG, "safe score for fen " + board.getFen());
            }

        } catch (Exception e) {
            Log.d(TAG, Utils.printException(e));
        } finally {
        }
    }


    void doCompMove() {
        try {

            if (compMoveWaiter != null)
                throw new RuntimeException("already waited");

            String fen = st.board.getFen();
            uci.send(String.format("position fen %s", fen));
            uci.clearOutput();
            int timeToMove = 200;
            uci.send(String.format("go movetime %d", timeToMove));

            compMoveWaiter = new CompMoveWaiter(timeToMove);


        } catch (Exception e) {
            Log.d(TAG, Utils.printException(e));
        }
    }

    void printAllMoves() {
        boolean fShowFigures = false;
        if(st.parm.boardMode.getIndex() >= BoardMode.AlwaysViewFigures.getIndex()){
            fShowFigures = true;
        }

        printAllMoves(fShowFigures);
    }

    List<DisplayMoveItem> generateLst(boolean fShowFigures)
    {
        List<DisplayMoveItem> lstMoves =new ArrayList<>();

        class EntryProcess{
            String moveStr = "";
            Piece piece = Piece.NONE;
            Piece pieceTo = Piece.NONE;
            boolean isCheck = false;
        }

        int moveCounter = 1;
        boolean isWhite = true;

        List<EntryProcess> moveToPrint = new ArrayList<>();
        Board tmpBoard = new Board();
        for (MoveBackup moveBackup : st.board.getBackup()) {
            Move curMove = moveBackup.getMove();
            tmpBoard.doMove(curMove);
            EntryProcess entry = new EntryProcess();
            entry.moveStr = curMove.toString();
            entry.piece = moveBackup.getMovingPiece();
            entry.pieceTo = moveBackup.getCapturedPiece();
            if(tmpBoard.isKingAttacked()){
                entry.isCheck = true;
            }
            moveToPrint.add(entry);
        }

        if (st.lastGameState == GameState.InProcess) {
            EntryProcess entry = new EntryProcess();
            entry.moveStr = st.curMove;
            entry.piece = Piece.NONE;
            if (st.curMove.length() >= 2) {
                entry.piece = st.board.getPiece(parseSquare(st.curMove.substring(0, 2)));
            }
            if (st.curMove.length() >= 4) {
                entry.pieceTo = st.board.getPiece(parseSquare(st.curMove.substring(2, 4)));
            }
            moveToPrint.add(entry);
        }

        DisplayMoveItem curItem = new DisplayMoveItem();
        for (int i = 0; i < moveToPrint.size(); i++) {

            boolean isLastEntry = i == moveToPrint.size() - 1;

            EntryProcess entry = moveToPrint.get(i);
            String moveString = entry.moveStr;


            if (isWhite) {
                curItem = new DisplayMoveItem();
                lstMoves.add(curItem);
                curItem.moveNum = moveCounter;
            } else {
                moveCounter += 1;
            }

            StringBuilder curMv = new StringBuilder();

            if (fShowFigures) {
                curMv.append(getUnicodeFromPiece(entry.piece));
            }
            if(moveString.length() > 0)
                curMv.append(moveString.substring(0, Math.min(2, moveString.length())));
            if (fShowFigures) {
                curMv.append(getUnicodeFromPiece(entry.pieceTo));
            }
            if(moveString.length() > 2)
                curMv.append(moveString.substring(2, moveString.length()));
            if(entry.isCheck) {
                if(isLastEntry && st.lastGameState == GameState.Win)
                    curMv.append("#");
                else
                    curMv.append("+");
            }

            if(isWhite)
                curItem.whitemove = curMv.toString();
            else
                curItem.blackmove = curMv.toString();


            isWhite = !isWhite;
        }

        return lstMoves;

    }

    void printAllMoves(boolean fShowFigures) {

        lstMoveItemsDisplay = generateLst(fShowFigures);
        String endString = null;
        if (st.lastGameState == GameState.Win) {
            endString = String.format("%s win", st.board.getSideToMove() == Side.WHITE ? "Black" : "White");
        } else if (st.lastGameState == GameState.Draw) {
            String reason = "3 fold rep";
            if (st.board.isStaleMate())
                reason = "stalemate";
            else if (st.board.isInsufficientMaterial())
                reason = "insuficient material";
            else if (st.board.getHalfMoveCounter() >= 100)
                reason = "50th move rule";
            endString = String.format("Draw: %s", reason);
        }
        if(endString != null){
            DisplayMoveItem item =  new DisplayMoveItem();
            item.simpleString = endString;
            lstMoveItemsDisplay.add((item));
        }
        mAdapter.setLst(lstMoveItemsDisplay);
        mAdapter.notifyDataSetChanged();

        mRecyclerView.post(new Runnable() {
            @Override
            public void run() {
                // Call smooth scroll
                mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount() - 1);
            }
        });




//
//        String val = result.toString();
//        textMoves.setText(val);
//        ScrollView scroll = findViewById(R.id.scrollView);
//        scroll.post(new Runnable() {
//            @Override
//            public void run() {
//                scroll.fullScroll(ScrollView.FOCUS_DOWN);
//            }
//        });
        //scroll.fullScroll(View.FOCUS_DOWN);
        //textMoves.setScrollY(textMoves.getBottom());
        //textMoves.scrollTo(0, 999999);
        //textMoves.sro(val, val.length());




    }

    Square parseSquare(String sq) {
        try {
            return Square.fromValue(sq.toUpperCase());
        } catch (Exception e) {
            return Square.NONE;
        }
    }

    Move moveFromString(String moveString) {
        try {
            return new Move(moveString, st.board.getSideToMove());
        } catch (Exception e) {
            return null;
        }
    }

    enum MoveLegalResult {
        AllOk,
        NeedPromotion,
        Bad
    }

    MoveLegalResult isMoveLegal(Move move, boolean checkNeedPromotion)  {
        if (move == null)
            return MoveLegalResult.Bad;
        if (move.getFrom() == Square.NONE || move.getTo() == Square.NONE)
            return MoveLegalResult.Bad;

        MoveList moves;
        try {
            moves = MoveGenerator.generateLegalMoves(st.board);
        } catch (MoveGeneratorException e) {
            Log.d(TAG, Utils.printException(e));
            return MoveLegalResult.Bad;
        }

        MoveLegalResult res = MoveLegalResult.Bad;
        Move mvToCheck = null;

        for (Move mv : moves) {
            //Log.d(TAG, "possible "+ mv.toString());
            if(move.getTo() == mv.getTo() && move.getFrom() == mv.getFrom()){

                if(move.getPromotion() == Piece.NONE && mv.getPromotion() != Piece.NONE)
                    res = MoveLegalResult.NeedPromotion;
                else
                    res = MoveLegalResult.AllOk;
                mvToCheck = mv;
                break;
            }
        }

        if(mvToCheck != null){
            if (st.board.isMoveLegal(mvToCheck, true))
                return res;
            else
                return MoveLegalResult.Bad;
        }


//        try {
//            if (board.isMoveLegal(move, true)) {
//                Log.d(TAG, String.format("TRUE is move legal from %s to %s", move.getFrom().toString(), move.getTo().toString()));
//                return MoveLegalResult.AllOk;
//            }
//            if (checkNeedPromotion && move.getPromotion() == Piece.NONE) {
//                move = new Move(move.getFrom(), move.getTo(), board.getSideToMove() == Side.WHITE ? Piece.WHITE_QUEEN : Piece.BLACK_QUEEN);
//                if (board.isMoveLegal(move, true))
//                    return MoveLegalResult.NeedPromotion;
//            }
//        } catch (Exception e) {
//            return MoveLegalResult.Bad;
//        }

        return MoveLegalResult.Bad;

    }

    enum GameState {
        InProcess,
        Draw,
        Win
    }

    GameState getGameState() {
        if (st.board.isMated())
            return GameState.Win;
        if (st.board.isDraw())
            return GameState.Draw;
        return GameState.InProcess;
    }

//    boolean isNeedPromotionInput()
//    {
//        if(curMove.length() != 4)
//            return  false;
//
//    }

    public void onInputMove(View view) {
        try {

            Log.d(TAG, "onInputMove");

            if(activityStarted == false)
                return;

            if (st.lastGameState != GameState.InProcess)
                return;

            if(compMoveWaiter != null)
                return;

            Button button = (Button) view;
            String text = button.getText().toString();


            st.curMove += text;

            if (st.curMove.length() >= 4) {

                Move move = moveFromString(st.curMove);
                MoveLegalResult res = isMoveLegal(move, true);

                if (res == MoveLegalResult.AllOk) {
                    Log.d(TAG,"do user move " + st.curMove);
                    st.curMove = "";
                    doIntMove(move);
                    if (st.lastGameState == GameState.InProcess && st.compEnabled) {
                        doCompMove();
                    }
                } else if (res == MoveLegalResult.NeedPromotion) {
                    // wait promotion input
                } else {
                    st.curMove = "";
                }
            }

            updateGui();
        } catch (Exception e) {
            Log.d(TAG, Utils.printException(e));
        }
    }

    public void onBackspace(View view) {
        if(compMoveWaiter != null)
            return;
        if (!st.curMove.isEmpty())
            st.curMove = st.curMove.substring(0, st.curMove.length() - 1);
        updateGui();
    }

    public void onNewGame(View view) {
        if(compMoveWaiter != null)
            return;
        Intent intent = new Intent(this, NewGameActivity.class);
        intent.putExtra("prevParm", (new Gson()).toJson(st.parm));
        startActivityForResult(intent, PICK_NEW_GAME_REQUEST);
        //startNewGame();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_NEW_GAME_REQUEST) {
            if (resultCode == RESULT_OK) {
                String jsonObj = data.getStringExtra("newGameParm");
                NewGameParams parms = (new Gson()).fromJson(jsonObj, NewGameParams.class);
                st = startNewGame(parms);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        uci.cleanup();
    }
}
