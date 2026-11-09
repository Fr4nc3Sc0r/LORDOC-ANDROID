package com.lordoc.model;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;

/** Esito di un esame concluso. */
public final class ExamResult {

    /** Punteggio minimo (scala 300-1000) considerato di superamento. */
    public static final int PASSING_SCORE = 825;
    public static final int MIN_SCORE = 300;
    public static final int MAX_SCORE = 1000;

    private final String title;
    private final ExamMode mode;
    private final LocalDateTime date;
    private final int correct;
    private final int total;
    private final int durationSeconds;
    private final Map<Domain, DomainScore> byDomain;
    /** La sessione è disponibile solo per l'esame appena svolto (non viene salvata). */
    private final ExamSession session;

    public ExamResult(String title, ExamMode mode, LocalDateTime date, int correct, int total,
                      int durationSeconds, Map<Domain, DomainScore> byDomain, ExamSession session) {
        this.title = title;
        this.mode = mode;
        this.date = date;
        this.correct = correct;
        this.total = total;
        this.durationSeconds = durationSeconds;
        this.byDomain = new EnumMap<>(Domain.class);
        this.byDomain.putAll(byDomain);
        this.session = session;
    }

    public String getTitle() { return title; }
    public ExamMode getMode() { return mode; }
    public LocalDateTime getDate() { return date; }
    public int getCorrect() { return correct; }
    public int getTotal() { return total; }
    public int getDurationSeconds() { return durationSeconds; }
    public Map<Domain, DomainScore> getByDomain() { return byDomain; }
    public ExamSession getSession() { return session; }

    public double getPercent() { return total == 0 ? 0 : 100.0 * correct / total; }

    /** Punteggio scalato stimato su scala 300-1000. */
    public int getScaledScore() {
        if (total == 0) {
            return MIN_SCORE;
        }
        return (int) Math.round(MIN_SCORE + (MAX_SCORE - MIN_SCORE) * ((double) correct / total));
    }

    public boolean isPassed() { return getScaledScore() >= PASSING_SCORE; }
}
