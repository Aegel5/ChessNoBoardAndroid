package com.example.alex.chessnoboardandroid;

public class UciMove {
    public double rnd;
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

        var absMate = Math.abs(mateIn);

        if (mateIn == 0) {
            if (score == Integer.MIN_VALUE)
                throw new RuntimeException("bad state");
            return score;
        }

        else if (absMate == 1)
            return mateIn * 900; // приравнивается к чистому ферзю.
        else if (absMate == 2)
            return mateIn * 700;
        else if(absMate == 3)
            return mateIn * 600;
        else
            return mateIn * 500;
    }
}

