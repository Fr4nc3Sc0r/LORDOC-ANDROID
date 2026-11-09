package com.lordoc.engine;

import com.lordoc.model.Domain;
import com.lordoc.model.ExamMode;

import java.util.EnumSet;
import java.util.Set;

/** Parametri per la creazione di un esame personalizzato. */
public final class ExamConfig {
    private String title = "Esame personalizzato";
    private ExamMode mode = ExamMode.STUDY;
    private Set<Domain> domains = EnumSet.allOf(Domain.class);
    private int questionCount = 30;
    private QuestionFilter filter = QuestionFilter.ALL;
    private int subnetQuestions;
    private int timeMinutes;
    private boolean allowBack = true;
    private boolean weighted = true;

    public String getTitle() { return title; }
    public ExamConfig title(String v) { this.title = v; return this; }
    public ExamMode getMode() { return mode; }
    public ExamConfig mode(ExamMode v) { this.mode = v; return this; }
    public Set<Domain> getDomains() { return domains; }
    public ExamConfig domains(Set<Domain> v) { this.domains = EnumSet.copyOf(v); return this; }
    public int getQuestionCount() { return questionCount; }
    public ExamConfig questionCount(int v) { this.questionCount = v; return this; }
    public QuestionFilter getFilter() { return filter; }
    public ExamConfig filter(QuestionFilter v) { this.filter = v; return this; }
    public int getSubnetQuestions() { return subnetQuestions; }
    public ExamConfig subnetQuestions(int v) { this.subnetQuestions = v; return this; }
    /** 0 = senza limite di tempo. */
    public int getTimeMinutes() { return timeMinutes; }
    public ExamConfig timeMinutes(int v) { this.timeMinutes = v; return this; }
    public boolean isAllowBack() { return allowBack; }
    public ExamConfig allowBack(boolean v) { this.allowBack = v; return this; }
    /** Se true le domande vengono distribuite secondo i pesi dei domini del blueprint. */
    public boolean isWeighted() { return weighted; }
    public ExamConfig weighted(boolean v) { this.weighted = v; return this; }
}
