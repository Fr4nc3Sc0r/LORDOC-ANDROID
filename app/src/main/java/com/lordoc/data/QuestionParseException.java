package com.lordoc.data;

/** Errore di formato in un file di domande. */
public class QuestionParseException extends Exception {
    private static final long serialVersionUID = 1L;

    public QuestionParseException(String message) {
        super(message);
    }
}
