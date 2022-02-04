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

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainApp.MainTag + MainActivity.class.getSimpleName();

    private static final int PICK_NEW_GAME_REQUEST = 0;
    private static final String APP_PREFERENCES = "mysettings";
    private static final String APP_PREFERENCES_STATE = "state"; // имя кота
    private static final String STATE_GAMESTATE = "gameState";

    private UCIWrapper uci = new UCIWrapper();
    private UCIWrapper uci2 = new UCIWrapper();
    private Button[] buttonsLetters = null;
    private ToggleButton btnIsCompEnabled = null;

    private Button btnShowBoard;
    private PopupWindow popupWindowBoard;
    private TextView boardView;
    private final Handler handler = new Handler();
    private final Handler h2 = new Handler();
    private CompMoveWaiter compMoveWaiter = null;
    private GameData st;
    private boolean activityStarted = false;
    private SharedPreferences mSettings;
    private TextView textMoves;
    private RecyclerView mRecyclerView;
    private DataAdapterMoves mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private List<DisplayMoveItem> lstMoveItemsDisplay = new ArrayList<>();


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

    public void doMove(String move) {
        st.board.doMove(new Move(move, st.board.getSideToMove()));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.d(TAG, "onCreate in thread " + Thread.currentThread().getId());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {

            //initPopup();
            initUci();
            initGui();

            mSettings = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);
            String stateString = mSettings.getString(APP_PREFERENCES_STATE, null);
            loadPrevState(stateString);
            if (st == null) {
                st = startNewGame(new NewGameParams());
            }

            updateCompOnButtonText();

            h2.postDelayed(new Runnable() {
                @Override
                public void run() {
                    analyzer();
                    h2.postDelayed(this, 1000);
                }
            }, 1000);

        } catch (Exception e) {
            Log.d(TAG, Utils.printException(e));
            throw e;
        }
    }

    List<String> curAnal = new ArrayList<>();
    boolean stoped = true;
    String curFen;
    int anCnt;


    HashMap<String, analitem> cur_hash = new HashMap<>();

    void stop2(){
        try {
            if (!stoped) {
                Log.d(TAG, "stop uci2");
                uci2.send("stop");
            }
            stoped = true;
        }
        catch (Exception e) {
            Log.d(TAG, Utils.printException(e));
        }
    }

    void analyzer(){

        //Log.d(TAG, "analize");

        try {
            if(st.seldelt <= 0){

                curAnal.clear();
                cur_hash.clear();
                curFen = null;



                stop2();
                uci2.clearOutput();

                return;
            }else{

                int tot = st.board.getBackup().size();
                int curm = 0;
                Board tmpBoard = new Board();
                Move myMove = null;
                for (MoveBackup moveBackup : st.board.getBackup()) {
                    Move curMove = moveBackup.getMove();
                    if(curm == tot-st.seldelt) {
                        myMove = curMove;
                        break;
                    }
                    curm++;

                    tmpBoard.doMove(curMove);
                }

                String fen = tmpBoard.getFen();

                //Log.d(TAG, fen);
                //Log.d(TAG, curFen);

                if(!fen.equals(curFen)){
                    cur_hash.clear();
                    curAnal.clear();
                    stoped = false;
                    anCnt = 0;
                    curFen = fen;
                    uci2.send("stop");
                    uci2.clearOutput();
                    Log.d(TAG, "start uci2 with new position");
                    uci2.send(String.format("position fen %s", fen));
                    uci2.send("go infinite");

                }else{
                    if(anCnt >= 30){
                        stop2();
                        uci2.clearOutput();
                        return; // max 10 sec analize
                    }
                    anCnt++;
                    if(anCnt == 1) {
                        //uci2.clearOutput();
                        //return; // зделать нормальный timeout для очистки старых ходов либо проверку корректности хода.
                    }

                    int cnt = 0;

                    List<String> ln = uci2.takeList();
                    for (int i = 0; i < ln.size(); i++) { // маленькая вероятно, но все же могут параллельно добавиться, но нам пофиг, но используем индекс.
                        String s = ln.get(i);
                        analitem item = new analitem();
                        item.Parse(s, tmpBoard, true);
                        if(item.move != null){
                            if(tmpBoard.getSideToMove() == Side.BLACK) {
                                item.mateIn = -item.mateIn;
                                item.cp = -item.cp;
                            }
                            cur_hash.put(item.move, item);
                        }
                    }

                    Log.d(TAG, "hash has");

                    List<analitem> sorted = new ArrayList<>();

                    for (Map.Entry<String, analitem> entry: cur_hash.entrySet()) {
                        sorted.add(entry.getValue());
                    }

                    boolean neg = tmpBoard.getSideToMove() == Side.BLACK;

                    // todo use multipv instead of sort
                    Collections.sort(sorted, new Comparator<analitem>() {
                        @Override
                        public int compare(analitem u1, analitem u2) {
                            return neg ? u1.cp.compareTo(u2.cp) : u2.cp.compareTo(u1.cp);
                        }
                    });

                    int curnumb = 1;
                    for (analitem entry: sorted) {
                        entry.number = curnumb++;
                    }

                    curAnal.clear();
                    for (analitem cur: sorted) {
                        String cpres;
                        if(cur.mateIn != 0){
                            cpres = "#"+ cur.mateIn;
                        }else{
                            cpres = String.format("%.2f", cur.cp/100.0);
                        }
                        double dcp = ((double)cur.cp)/100.0;
                        if(myMove != null && cur.move.equals(myMove.toString())){
                            curAnal.add(String.format("(%d) %s %s", cur.number, cpres, cur.NiceCont(tmpBoard)));
                        }
                        else if (curAnal.size() < 7 ) {
                            curAnal.add(String.format("%s %s", cpres, cur.NiceCont(tmpBoard)));
                        }
                    }
                    //uci2.clearOutput(); // можем удалить те, которые еще не прочитали, но пофиг.
                    printAllMoves_noscr();
                }
            }
        } catch (Exception e) {
            Log.d(TAG, Utils.printException(e));
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
        // todo del  analizer
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


        (findViewById(R.id.btnBS)).setOnLongClickListener(v -> {
            revertMove();
            return true;
        });

        (findViewById(R.id.btnLeft)).setOnLongClickListener(v -> {
            st.seldelt = 99999;
            printAllMoves_noscr();
            scr_up();
            return true;
        });

        (findViewById(R.id.btnRight)).setOnLongClickListener(v -> {
            st.seldelt = 0;
            printAllMoves_noscr();
            scr_end();
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
            String stockFileName = "stockfish_exe_arm";

            if (found_x86)
                stockFileName = "stockfish_exe_x86";
            String path = Utils.unzipExeFromAsset(stockFileName, this);

            //var path = getApplicationInfo().nativeLibraryDir + "/stockfish.so";

            uci.init(path);
            uci2.init(path);

            // показываем 50 возможных ходов и их score
            uci.send("setoption name MultiPV value 50");
            uci2.send("setoption name MultiPV value 50");

            uci.clearOutput();
            uci2.clearOutput();

        } catch (Exception e) {
            Log.d(TAG, Utils.printException(e));
        }
    }

    public void updateGui() {
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
        var capt = String.format("C(%d)", st.parm.getCompStrength());
        btnIsCompEnabled.setTextOn(capt);
        btnIsCompEnabled.setTextOff(capt);
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
            curAnal.clear();

            updateCompOnButtonText();
            updateGui();

            Log.d(TAG, "Game state created with level " + level);

            return st;
        } catch (Exception e) {
            Log.d(TAG, Utils.printException(e));
            return null;
        }
    }

    private void doIntMove(Move mv) {
        st.board.doMove(mv);
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

                    var mv = CompMoveChooser.DoMoveChoose(bestMoveLine, st, uci);
                    doIntMove(new Move(mv, st.board.getSideToMove()));
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
            int timeToMove = 300;
            uci.send(String.format("go movetime %d", timeToMove));

            compMoveWaiter = new CompMoveWaiter(timeToMove);


        } catch (Exception e) {
            Log.d(TAG, Utils.printException(e));
        }
    }

    private void printAllMoves() {
        printAllMoves_int(st.parm.isAddFiguresSign(), true);
    }

    private void printAllMoves_noscr() {
        printAllMoves_int(st.parm.isAddFiguresSign(), false);
    }

    class EntryProcess {
        String moveStr = "";
        boolean endgame;
    }

    private List<EntryProcess> getHistory() {

        List<EntryProcess> moveToPrint = new ArrayList<>();
        Board tmpBoard = new Board();
        for (MoveBackup moveBackup : st.board.getBackup()) {
            Move curMove = moveBackup.getMove();
            EntryProcess entry = new EntryProcess();
            entry.moveStr = Utils.PretifyAndMove(curMove.toString(), tmpBoard);
            moveToPrint.add(entry);
        }

        EntryProcess entry = new EntryProcess();
        if (st.lastGameState == GameState.InProcess) {
            if(st.curMove.length() >= 4){
                entry.moveStr = "err";
            }else
                entry.moveStr = Utils.PretifyAndMove(st.curMove, st.board);
        }else{
            entry.endgame = true;
        }
        moveToPrint.add(entry);

        return moveToPrint;
    }

    private List<DisplayMoveItem> generateDisplayMoveLst(boolean fShowFigures){
        List<DisplayMoveItem> lstMoves = new ArrayList<>();

        int moveCounter = 1;
        boolean isWhite = true;

        List<EntryProcess> moveToPrint = getHistory();
        if(st.seldelt > moveToPrint.size()-1)
            st.seldelt = moveToPrint.size()-1;
        else if(st.seldelt < 0)
            st.seldelt = 0;
        int sel = moveToPrint.size()-1-st.seldelt;

        DisplayMoveItem curItem = new DisplayMoveItem();
        for (int i = 0; i < moveToPrint.size(); i++) {

            boolean isLastEntry = i == moveToPrint.size() - 1;

            EntryProcess entry = moveToPrint.get(i);

            if(entry.endgame) {
                String endString = "err";
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

                DisplayMoveItem item = new DisplayMoveItem();
                lstMoves.add(item);
                item.simpleString = endString;
                item.issel = i == sel;
                break;
            }

            if (isWhite) {
                curItem = new DisplayMoveItem();
                lstMoves.add(curItem);
                curItem.moveNum = moveCounter;
            } else {
                moveCounter += 1;
            }

            boolean isSelected = i == sel;

            if (isWhite) {
                curItem.whiteMove = entry.moveStr;
                curItem.whiteSel = isSelected;
            }
            else {
                curItem.blackMove = entry.moveStr;
                curItem.blackSel = isSelected;
            }

            if(isSelected && st.seldelt > 0) {
                for (String s : curAnal) {
                    DisplayMoveItem item = new DisplayMoveItem();
                    item.simpleString = s;
                    lstMoves.add((item));
                }
            }

            isWhite = !isWhite;
        }

        return lstMoves;

    }

    void scr_end(){
        mRecyclerView.post(new Runnable() {
            @Override
            public void run() {
                // Call smooth scroll
                mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount() - 1);
            }
        });
    }

    void scr_up(){
        mRecyclerView.post(new Runnable() {
            @Override
            public void run() {
                // Call smooth scroll
                mRecyclerView.smoothScrollToPosition(0);
            }
        });
    }

    private void printAllMoves_int(boolean fShowFigures, boolean scroll) {


        lstMoveItemsDisplay = generateDisplayMoveLst(fShowFigures);



        mAdapter.setLst(lstMoveItemsDisplay);
        mAdapter.notifyDataSetChanged();

        if(scroll) {
            scr_end();
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

            st.seldelt = 0; // сбрасываем анализ.
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


    public void onLeft(View view) {
        if(compMoveWaiter != null)
            return;
        curAnal.clear();
        st.seldelt++;
        printAllMoves_noscr();
    }

    public void onRight(View view) {
        if(compMoveWaiter != null)
            return;
        curAnal.clear();
        st.seldelt--;
        printAllMoves_noscr();

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
