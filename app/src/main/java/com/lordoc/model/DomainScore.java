package com.lordoc.model;

/** Risultato parziale per un dominio. */
public final class DomainScore {
    private int correct;
    private int total;

    public DomainScore() { }

    public DomainScore(int correct, int total) {
        this.correct = correct;
        this.total = total;
    }

    public void add(boolean ok) {
        total++;
        if (ok) {
            correct++;
        }
    }

    public void add(DomainScore other) {
        correct += other.correct;
        total += other.total;
    }

    public int getCorrect() { return correct; }
    public int getTotal() { return total; }

    public double getPercent() { return total == 0 ? 0 : 100.0 * correct / total; }
}
