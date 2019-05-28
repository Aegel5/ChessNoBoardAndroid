package com.example.alex.chessnoboardandroid;

public class UciMove {
    private String move;
    private int score = Integer.MIN_VALUE;

    // Мат через n ходов
    private int mateIn = 0;

    public boolean isValid() {
        return getMove() != null && (getScore() != Integer.MIN_VALUE || mateIn != 0);
    }

    public String getMove() {
        return move;
    }

    public void setMove(String move) {
        this.move = move;
    }

    public int getScore() {
        return score;
    }

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
        if (mateIn == 0)
            return score;
        else if (mateIn == 1)
            return 16;
        else if (mateIn == 2)
            return 12;
        else if (mateIn == 3)
            return 8;
        else
            return 7;

    }
}

