package com.example.alex.chessnoboardandroid;

public class SimpleTokScanner {
    private static final String TAG = MainApp.MainTag + SimpleTokScanner.class.getSimpleName();
    
    private String text;
    private int curPos = 0;
    private int numTokReturned = 0;

    public SimpleTokScanner(String text) {
        this.text = text;
        //Log.d(TAG, "create scanner=" + text);
    }

    public int getNumTokReturned() {
        return numTokReturned;
    }

    public String getNext() {

        //Log.d(TAG, "getnext");

        // найдем начало
        int indexFrom = -1;
        for (; curPos < text.length(); curPos++) {
            if (!Character.isWhitespace(text.charAt(curPos))) {
                indexFrom = curPos;
                break;
            }
        }

        if (indexFrom == -1)
            return null;

        // найдем конец (должен быть в любом случае, так как есть начало)
        for (; curPos < text.length(); curPos++) {
            if (Character.isWhitespace(text.charAt(curPos))) {
                break;
            }
        }
        int indexTo = curPos;

        numTokReturned += 1;
        String result = text.substring(indexFrom, indexTo);
        //Log.d(TAG, String.format("cur token=/%s/", result));
        return result;

    }
}