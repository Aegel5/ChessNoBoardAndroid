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
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.MoveBackup;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;
import com.google.gson.Gson;

import java.util.List;
import java.util.ArrayList;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainApp.MainTag + MainActivity.class.getSimpleName();

    private static final int PICK_NEW_GAME_REQUEST = 0;
    private static final String APP_PREFERENCES = "mysettings";
    private static final String APP_PREFERENCES_STATE = "state"; // имя кота
    private static final String STATE_GAMESTATE = "gameState";

    private UCIWrapper uci = new UCIWrapper();
    private Button[] buttonsLetters = null;
    private ToggleButton btnIsCompEnabled = null;
    private BoardViewListener boardViewListener;
    private Button btnShowBoard;
    private PopupWindow popupWindowBoard;
    private TextView boardView;
    private final Handler handler = new Handler();
    private CompMoveWaiter compMoveWaiter = null;
    private GameData st;
    private boolean activityStarted = false;
    private SharedPreferences mSettings;
    private TextView textMoves;
    private RecyclerView mRecyclerView;
    private DataAdapterMoves mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private List<DisplayMoveItem> lstMoveItemsDisplay = new ArrayList<>();



    /*
    Коллбек для показа доски
     */
    private class BoardViewListener implements View.OnTouchListener {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (st.parm.getAllowViewBoardMode() == AllowViewBoardMode.None)
                return false;
            boolean allowShowBoard = st.parm.getAllowViewBoardMode().getIndex() >= AllowViewBoardMode.AllowViewTextBoard.getIndex();
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (allowShowBoard) {
                        showBoardPopup();
                    }
                    break;
                case MotionEvent.ACTION_UP: // отпускание
                case MotionEvent.ACTION_CANCEL:
                    if (popupWindowBoard != null) {
                        popupWindowBoard.dismiss();
                    }
                    break;
            }
            return true;
        }
    }

    private void fillButtonsLetter() {
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

    private void initPopup() {

        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.layout_board, null);

        // create the popup window
        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        boolean focusable = true; // lets taps outside the popup also dismiss it
        popupWindowBoard = new PopupWindow(popupView, width, height, focusable);
        boardView = popupView.findViewById(R.id.boardView);
    }


    private String getUnicodeFromPiece(Piece piece) {
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

    private void showBoardPopup() {
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
            popupWindowBoard.showAtLocation(btnShowBoard, Gravity.CENTER, 0, 0);

        } catch (Exception e) {
            Log.d(TAG, "Exception showBoardPopup: " + e.toString());
        }
    }

    public void doMove(String move) {
        st.board.doMove(new Move(move, st.board.getSideToMove()));
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

        } catch (Exception e) {
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

        if (st == null)
            return;

        try {
            SharedPreferences.Editor editor = mSettings.edit();
            String stateString = st.Serialize();
            Log.d(TAG, "state is\n" + stateString);
            editor.putString(APP_PREFERENCES_STATE, stateString);
            editor.apply();
        } catch (Exception e) {
            Log.d(TAG, Utils.printException(e));
        }


    }


    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
        activityStarted = true;
    }

    private void loadPrevState(String stateString) {
        Log.d(TAG, "loadPrevState");

        if (stateString == null)
            return;

        try {
            st = GameData.Deserialize(stateString);
            updateCompOnButtonText();
            updateGui();
        } catch (Exception e) {
            Log.d(TAG, Utils.printException(e));
        }
    }


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
            if (compMoveWaiter != null)
                return;
            if (activityStarted == false)
                return;
            st.compEnabled = isChecked;
            if (isChecked) {
                st.curMove = "";
                doCompMove();
                updateGui();
            }
        });
        btnShowBoard = findViewById(R.id.btnBoard);
        boardViewListener = new BoardViewListener();
        btnShowBoard.setOnTouchListener(boardViewListener);

        (findViewById(R.id.btnBS)).setOnLongClickListener(v -> {
            revertMove();
            return true;
        });


        mRecyclerView = findViewById(R.id.my_recycler_view);
        //mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mAdapter = new DataAdapterMoves(this);
        mRecyclerView.setAdapter(mAdapter);
    }

    private GameState getGameState()
    {
        return ChessLibWrapper.getGameState(st.board);
    }

    private void revertMove() {
        try {
            if (compMoveWaiter != null)
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

    private void updateGui() {
        printAllMoves();
        updateButtonTexts();
    }

    private void updateButtonTexts() {
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

    private void updateCompOnButtonText() {
        btnIsCompEnabled.setChecked(st.compEnabled);
        btnIsCompEnabled.setTextOn(String.format("on(%d)", st.parm.getCompStrength()));
        btnIsCompEnabled.setTextOff(String.format("off"));
    }

    private GameData startNewGame(NewGameParams parms) {

        try {
            st = new GameData();

            int level = parms.getCompStrength();
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

    private void doIntMove(Move move) {
        st.board.doMove(move);
        st.lastGameState = getGameState();
    }


    class CompMoveWaiter {
        public CompMoveWaiter(int time) {
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

            try {


                List<String> lines = uci.curLines();
                String bestMoveLine = null;
                if (!lines.isEmpty()) {
                    for (; lastCheckedIndex < lines.size(); lastCheckedIndex++) {
                        String line = lines.get(lastCheckedIndex);
                        if (line.startsWith("bestmove")) {
                            bestMoveLine = line;
                            break;
                        }
                    }
                }
                if (bestMoveLine != null) {
                    compMoveWaiter = null;

                    doIntMove(CompMoveChooser.DoMoveChoose(bestMoveLine, st, uci));
                    printAllMoves();
                } else {
                    scheduleWait(10);
                }
            } catch (Exception e) {
                Log.d(TAG, Utils.printException(e));
            }
        }
    }

    private void doCompMove() {
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

    private void printAllMoves() {
        printAllMoves(st.parm.isAddFiguresSign());
    }

    private List<DisplayMoveItem> generateDisplayMoveLst(boolean fShowFigures) {
        List<DisplayMoveItem> lstMoves = new ArrayList<>();

        class EntryProcess {
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
            if (tmpBoard.isKingAttacked()) {
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
            if (moveString.length() > 0)
                curMv.append(moveString.substring(0, Math.min(2, moveString.length())));
            if (fShowFigures) {
                curMv.append(getUnicodeFromPiece(entry.pieceTo));
            }
            if (moveString.length() > 2)
                curMv.append(moveString.substring(2, moveString.length()));
            if (entry.isCheck) {
                if (isLastEntry && st.lastGameState == GameState.Win)
                    curMv.append("#");
                else
                    curMv.append("+");
            }

            if (isWhite)
                curItem.whiteMove = curMv.toString();
            else
                curItem.blackMove = curMv.toString();


            isWhite = !isWhite;
        }

        return lstMoves;

    }

    private void printAllMoves(boolean fShowFigures) {

        lstMoveItemsDisplay = generateDisplayMoveLst(fShowFigures);
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
        if (endString != null) {
            DisplayMoveItem item = new DisplayMoveItem();
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

    }

    private Square parseSquare(String sq) {
        try {
            return Square.fromValue(sq.toUpperCase());
        } catch (Exception e) {
            return Square.NONE;
        }
    }

    private Move moveFromString(String moveString) {
        try {
            return ChessLibWrapper.moveFromString(moveString, st.board);
        } catch (Exception e) {
            return null;
        }
    }

    public void onInputMove(View view) {
        try {

            Log.d(TAG, "onInputMove");

            if (activityStarted == false)
                return;

            if (st.lastGameState != GameState.InProcess)
                return;

            if (compMoveWaiter != null)
                return;

            Button button = (Button) view;
            String text = button.getText().toString();


            st.curMove += text;

            if (st.curMove.length() >= 4) {

                Move move = moveFromString(st.curMove);
                MoveLegalResult res = ChessLibWrapper.isMoveLegal(move, st.board, true);

                if (res == MoveLegalResult.AllOk) {
                    Log.d(TAG, "do user move " + st.curMove);
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
        if (compMoveWaiter != null)
            return;
        if (!st.curMove.isEmpty())
            st.curMove = st.curMove.substring(0, st.curMove.length() - 1);
        updateGui();
    }

    public void onNewGame(View view) {
        if (compMoveWaiter != null)
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
