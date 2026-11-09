package com.lordoc.engine;

import java.util.List;

import com.lordoc.util.Text;

/** Un esame predefinito (insieme fisso di domande), come "Esame A", "Esame B"... */
public final class PresetExam {
    private final String name;
    private final List<String> questionIds;

    public PresetExam(String name, List<String> questionIds) {
        this.name = name;
        this.questionIds = Text.copyOf(questionIds);
    }

    public String getName() { return name; }
    public List<String> getQuestionIds() { return questionIds; }
    public int size() { return questionIds.size(); }
}
