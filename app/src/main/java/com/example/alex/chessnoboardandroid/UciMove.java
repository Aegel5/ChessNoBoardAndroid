package com.example.alex.chessnoboardandroid;

public class UciMove {
    private String move;
    private int score = Integer.MIN_VALUE;

    // Мат через n ходов
    private int mateIn = 0;

    public boolean isValid() {
        return getMove() != null && (score != Integer.MIN_VALUE || mateIn != 0);
    }

    public String getMove() {
        return move;
    }

    public void setMove(String move) {
        this.move = move;
    }

    /*
    public int getScore() {
        return score;
    }
    */

    public void setScore(int score) {
        this.score = score;
    }

    public int getMateIn() {
        return mateIn;
    }

    public void setMateIn(int mateIn) {
        this.mateIn = mateIn;
    }

    // score с учетом мата в n ходов
    public int getUniversalScore() {
        if (mateIn == 0) {
            if (score == Integer.MIN_VALUE)
                throw new RuntimeException("bad state");
            return score;
        }
        else if (mateIn == 1)
            return 1600; // 16 пешек
        else if (mateIn == 2)
            return 1200;
        else if (mateIn == 3)
            return 800;
        else
            return 700;

    }
}

