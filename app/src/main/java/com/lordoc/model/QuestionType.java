package com.lordoc.model;

/** Tipologie di domanda supportate (come all'esame reale). */
public enum QuestionType {
    /** Scelta multipla con una sola risposta corretta. */
    SINGLE("Risposta singola"),
    /** Scelta multipla con più risposte corrette ("Choose two"). */
    MULTI("Risposta multipla"),
    /** Abbinamento elementi → categorie (equivalente del drag and drop). */
    DRAG("Drag and drop"),
    /** Risposta libera (es. calcolo di subnetting). */
    FILL("Risposta aperta");

    private final String label;

    QuestionType(String label) { this.label = label; }

    public String getLabel() { return label; }
}
