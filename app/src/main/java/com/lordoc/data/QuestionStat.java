package com.lordoc.data;

/** Statistiche persistenti di una singola domanda. */
public final class QuestionStat {
    private int seen;
    private int correct;
    private boolean lastCorrect;
    private boolean flagged;

    public QuestionStat() { }

    QuestionStat(int seen, int correct, boolean lastCorrect, boolean flagged) {
        this.seen = seen;
        this.correct = correct;
        this.lastCorrect = lastCorrect;
        this.flagged = flagged;
    }

    public void record(boolean ok) {
        seen++;
        if (ok) {
            correct++;
        }
        lastCorrect = ok;
    }

    public int getSeen() { return seen; }
    public int getCorrect() { return correct; }
    public int getWrong() { return seen - correct; }
    public boolean isLastCorrect() { return lastCorrect; }
    public boolean isFlagged() { return flagged; }
    public void setFlagged(boolean flagged) { this.flagged = flagged; }
}
