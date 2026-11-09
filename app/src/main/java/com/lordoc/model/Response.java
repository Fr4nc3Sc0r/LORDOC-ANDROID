package com.lordoc.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** La risposta data dall'utente a una singola domanda. */
public final class Response {

    private final Set<Integer> selected = new TreeSet<>();
    private final Map<Integer, String> dragAnswers = new HashMap<>();
    private String fillText = "";

    public Set<Integer> getSelected() { return Collections.unmodifiableSet(selected); }

    public void select(int option, boolean single) {
        if (single) {
            selected.clear();
        }
        selected.add(option);
    }

    public void deselect(int option) { selected.remove(option); }

    public String getFillText() { return fillText; }

    public void setFillText(String text) { this.fillText = text == null ? "" : text; }

    public String getDragAnswer(int item) { return dragAnswers.get(item); }

    public void setDragAnswer(int item, String target) {
        if (target == null || target.isEmpty()) {
            dragAnswers.remove(item);
        } else {
            dragAnswers.put(item, target);
        }
    }

    public int dragAnswerCount() { return dragAnswers.size(); }
}
