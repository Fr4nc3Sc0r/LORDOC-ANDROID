package com.lordoc.engine;

/** Filtro sulle domande da includere in un esame personalizzato. */
public enum QuestionFilter {
    ALL("Tutte le domande"),
    UNSEEN("Solo domande mai viste"),
    MISSED("Solo domande sbagliate almeno una volta"),
    LAST_WRONG("Solo domande sbagliate l'ultima volta"),
    FLAGGED("Solo domande marcate");

    private final String label;

    QuestionFilter(String label) { this.label = label; }

    @Override
    public String toString() { return label; }
}
