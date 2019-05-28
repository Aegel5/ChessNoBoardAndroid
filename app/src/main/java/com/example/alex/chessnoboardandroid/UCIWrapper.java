package com.example.alex.chessnoboardandroid;

import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeoutException;


public class UCIWrapper {

    private static final String TAG = MainApp.MainTag + UCIWrapper.class.getSimpleName();
    private Process process = null;
    private Thread threadReader = null;
    private List<String> currentOutput = Collections.synchronizedList(new ArrayList<>());
    private BufferedWriter out = null;


    public void init(String path) throws IOException {
        Log.d(TAG, "Init for path: " + path);
        cleanup();
        ProcessBuilder processBuilder = new ProcessBuilder(path);
        process = processBuilder.start();
        out = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));

        threadReader = new Thread(() ->
        {
            Log.d(TAG, "start reader");
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                while (true) {
                    //Log.d(TAG, "before read line");
                    String line = reader.readLine();
                    if (line == null) {
                        Log.d(TAG, "line == null");
                        return;
                    } else {
                        Log.d("UCI_READ_LINE", "line is " + line);
                        //Log.d(TAG, "UCI=" + line);
                    }

                    currentOutput.add(line.toLowerCase());
                }
            } catch (Exception e) {
                Log.d(TAG, "Exception in threadReader: " + e.toString());
            }
        });
        threadReader.start();

    }

    public void cleanup() {

        Log.d(TAG, "Cleanup started...");

        try {

            clearOutput();

            if (process != null) {
                process.destroy();
                process.waitFor();
                process = null;
            }

            if (threadReader != null) {
                threadReader.join();
                threadReader = null;
            }
        } catch (Exception e) {
            Log.d(TAG, "Exception in cleanup: " + e.toString());
        }

        Log.d(TAG, "Cleanup done");
    }

    public void send(String cmd) throws IOException {
        Log.d(TAG, "send: " + cmd);

        out.write(cmd);
        out.newLine();
        out.flush();
    }

    /*
     Дождаться определенного сообщения
     */
    public String wait(String output) throws InterruptedException, TimeoutException {
        long timeout = 5000;
        long start = System.currentTimeMillis();
        int lastIndex = 0;
        while (true) {
            if (System.currentTimeMillis() - start > timeout) {
                Log.d(TAG, "timeout for " + output);
                throw new TimeoutException();
            }
            for (; lastIndex < currentOutput.size(); lastIndex++) {
                String line = currentOutput.get(lastIndex);
                if (line.indexOf(output) != -1) {
                    //Log.d(TAG, "found string " + line);
                    return line;
                }
            }
            Thread.sleep(10);
        }
    }

    public Map<String, UciMove> getCurrentMovesScore() {
        Map<String, UciMove> moves = new TreeMap<>();
        for (String line : currentOutput) {
            SimpleTokScanner scanner = new SimpleTokScanner(line);
            UciMove uciMove = new UciMove();
            for (String tok = scanner.getNext(); tok != null; tok = scanner.getNext()) {
                if (scanner.getNumTokReturned() == 1 && !tok.equals("info")) {
                    break;
                }
                if (tok.equals("score")) {
                    tok = scanner.getNext();
                    if (tok == null)
                        break;
                    if (tok.equals("cp")) {
                        tok = scanner.getNext();
                        if (tok == null)
                            break;
                        uciMove.setScore(Integer.valueOf(tok));
                    } else if (tok.equals("mate")) {
                        tok = scanner.getNext();
                        if (tok == null)
                            break;
                        uciMove.setMateIn(Integer.valueOf(tok));
                    }


                }
                if (tok.equals("pv")) {
                    tok = scanner.getNext();
                    if (tok == null)
                        break;
                    uciMove.setMove(tok);
                }
                if (uciMove.isValid()) {
                    moves.put(uciMove.getMove(), uciMove);
                    break;
                }
            }
        }
        return moves;
    }

    public void clearOutput() {
        currentOutput.clear();
    }

    /*
        НЕ ДЕЛАЕТ копию списка (для оптимизации). Коллекция thread safe.
     */
    public List<String> curLines() {
        return currentOutput;
    }

}
