package com.lordoc.model;

import java.util.List;

import com.lordoc.util.Text;

/** Stato di un esame in corso. */
public final class ExamSession {

    private final String title;
    private final ExamMode mode;
    private final List<Question> questions;
    private final Response[] responses;
    private final boolean[] marked;
    private final boolean[] revealed;
    private final int timeLimitSeconds;
    private final boolean allowBack;
    private int elapsedSeconds;
    private int current;

    /**
     * @param timeLimitSeconds 0 = nessun limite
     * @param allowBack se false non si può tornare alla domanda precedente (come all'esame Cisco)
     */
    public ExamSession(String title, ExamMode mode, List<Question> questions,
                       int timeLimitSeconds, boolean allowBack) {
        if (questions.isEmpty()) {
            throw new IllegalArgumentException("L'esame non contiene domande");
        }
        this.title = title;
        this.mode = mode;
        this.questions = Text.copyOf(questions);
        this.responses = new Response[questions.size()];
        for (int i = 0; i < responses.length; i++) {
            responses[i] = new Response();
        }
        this.marked = new boolean[questions.size()];
        this.revealed = new boolean[questions.size()];
        this.timeLimitSeconds = timeLimitSeconds;
        this.allowBack = allowBack;
    }

    public String getTitle() { return title; }
    public ExamMode getMode() { return mode; }
    public List<Question> getQuestions() { return questions; }
    public int size() { return questions.size(); }
    public Question getQuestion(int i) { return questions.get(i); }
    public Response getResponse(int i) { return responses[i]; }
    public boolean isMarked(int i) { return marked[i]; }
    public void setMarked(int i, boolean v) { marked[i] = v; }
    public boolean isRevealed(int i) { return revealed[i]; }
    public void setRevealed(int i, boolean v) { revealed[i] = v; }
    public int getTimeLimitSeconds() { return timeLimitSeconds; }
    public boolean isTimed() { return timeLimitSeconds > 0; }
    public boolean isAllowBack() { return allowBack; }
    public int getElapsedSeconds() { return elapsedSeconds; }
    public void tick() { elapsedSeconds++; }
    public int getRemainingSeconds() { return Math.max(0, timeLimitSeconds - elapsedSeconds); }
    public int getCurrent() { return current; }

    public void setCurrent(int i) {
        if (i >= 0 && i < questions.size()) {
            current = i;
        }
    }

    public boolean isAnswered(int i) { return questions.get(i).isAnswered(responses[i]); }

    public int countAnswered() {
        int n = 0;
        for (int i = 0; i < size(); i++) {
            if (isAnswered(i)) {
                n++;
            }
        }
        return n;
    }

    public int countMarked() {
        int n = 0;
        for (boolean m : marked) {
            if (m) {
                n++;
            }
        }
        return n;
    }
}
