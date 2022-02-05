package com.example.alex.chessnoboardandroid;

public class SimpleTokScanner {

    private String text;
    private int curPos = 0;
    StringBuilder res = new StringBuilder();

    public SimpleTokScanner(String text) {
        this.text = text;
    }

    public String getNext() {

        res.setLength(0);

        for (; curPos < text.length(); curPos++) {
            var cur = text.charAt(curPos);
            if(Character.isWhitespace(cur)){
                if(res.length() > 0)
                    return res.toString();
            }else{
                res.append(cur);
            }
        }

        if(res.length() > 0)
            return res.toString(); // last

        return null; // the end
    }

    public void skip(String tok){

        int i = 0;
        for (; curPos < text.length(); curPos++) {
            var cur = text.charAt(curPos);
            if(Character.isWhitespace(cur)){
                if(i == tok.length())
                    return;
                else
                    i = 0;
            }else{
                if(i < tok.length()){
                    if(tok.charAt(i) == cur){
                        i++;
                    }else{
                        i = Integer.MAX_VALUE;
                    }
                }
            }
        }
    }
}