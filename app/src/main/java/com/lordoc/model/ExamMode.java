package com.lordoc.model;

/** Modalità d'esame. */
public enum ExamMode {
    /** Condizioni d'esame reale: tempo limite, nessuna spiegazione fino alla fine. */
    SIMULATION("Simulazione"),
    /** Senza limite di tempo, con risposta e spiegazione visibili su richiesta. */
    STUDY("Studio");

    private final String label;

    ExamMode(String label) { this.label = label; }

    public String getLabel() { return label; }

    @Override
    public String toString() { return label; }
}
