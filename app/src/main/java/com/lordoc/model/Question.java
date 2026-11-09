package com.lordoc.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

import com.lordoc.util.Text;

/** Una domanda d'esame, immutabile. Si costruisce con {@link Builder}. */
public final class Question {

    private static final String[] NUMBER_WORDS = {
        "zero", "one", "two", "three", "four", "five", "six"
    };

    private final String id;
    private final Domain domain;
    private final QuestionType type;
    private final String text;
    private final String exhibit;
    private final List<String> options;
    private final Set<Integer> correctOptions;
    private final List<String> acceptedAnswers;
    private final List<String> dragItems;
    private final List<String> dragTargets;
    private final List<String> dragSolution;
    private final String explanation;
    private final String reference;
    private final boolean generated;
    private final boolean shuffleAllowed;
    private final String source;

    private Question(Builder b) {
        this.id = Objects.requireNonNull(b.id, "id");
        this.domain = Objects.requireNonNull(b.domain, "domain");
        this.type = Objects.requireNonNull(b.type, "type");
        this.text = b.text == null ? "" : b.text;
        this.exhibit = b.exhibit == null ? "" : b.exhibit;
        this.options = Text.copyOf(b.options);
        this.correctOptions = Collections.unmodifiableSet(new java.util.TreeSet<>(b.correctOptions));
        this.acceptedAnswers = Text.copyOf(b.acceptedAnswers);
        this.dragItems = Text.copyOf(b.dragItems);
        this.dragSolution = Text.copyOf(b.dragSolution);
        List<String> targets = new ArrayList<>();
        for (String s : b.dragSolution) {
            if (!targets.contains(s)) {
                targets.add(s);
            }
        }
        for (String s : b.extraTargets) {
            if (!targets.contains(s)) {
                targets.add(s);
            }
        }
        this.dragTargets = Text.copyOf(targets);
        this.explanation = b.explanation == null ? "" : b.explanation;
        this.reference = b.reference == null ? "" : b.reference;
        this.generated = b.generated;
        this.shuffleAllowed = b.shuffleAllowed;
        this.source = b.source == null ? "" : b.source;
    }

    public String getId() { return id; }
    public Domain getDomain() { return domain; }
    public QuestionType getType() { return type; }
    public String getText() { return text; }
    public String getExhibit() { return exhibit; }
    public boolean hasExhibit() { return !Text.isBlank(exhibit); }
    public List<String> getOptions() { return options; }
    public Set<Integer> getCorrectOptions() { return correctOptions; }
    public List<String> getAcceptedAnswers() { return acceptedAnswers; }
    public List<String> getDragItems() { return dragItems; }
    public List<String> getDragTargets() { return dragTargets; }
    public List<String> getDragSolution() { return dragSolution; }
    public String getExplanation() { return explanation; }
    public String getReference() { return reference; }
    public boolean isGenerated() { return generated; }
    /** false se l'ordine delle opzioni è significativo (campo "shuffle: no"). */
    public boolean isShuffleAllowed() { return shuffleAllowed; }
    public String getSource() { return source; }

    /** Numero di opzioni da selezionare. */
    public int requiredSelections() { return correctOptions.size(); }

    /** Testo da visualizzare: per le MULTI aggiunge "(Choose N.)" se manca. */
    public String getDisplayText() {
        if (type == QuestionType.MULTI && !text.toLowerCase(Locale.ROOT).contains("(choose")) {
            int n = requiredSelections();
            String word = n < NUMBER_WORDS.length ? NUMBER_WORDS[n] : String.valueOf(n);
            return text + " (Choose " + word + ".)";
        }
        return text;
    }

    /** true se l'utente ha fornito una risposta completa. */
    public boolean isAnswered(Response r) {
        if (r == null) {
            return false;
        }
        switch (type) {
            case SINGLE:
                return r.getSelected().size() == 1;
            case MULTI:
                return r.getSelected().size() == requiredSelections();
            case FILL:
                return !Text.isBlank(r.getFillText());
            case DRAG:
                return r.dragAnswerCount() == dragItems.size();
            default:
                return false;
        }
    }

    /** Correzione "tutto o niente", come all'esame Cisco. */
    public boolean isCorrect(Response r) {
        if (r == null) {
            return false;
        }
        switch (type) {
            case SINGLE:
            case MULTI:
                return r.getSelected().equals(correctOptions);
            case FILL:
                String given = normalize(r.getFillText());
                for (String a : acceptedAnswers) {
                    if (normalize(a).equals(given)) {
                        return true;
                    }
                }
                return false;
            case DRAG:
                for (int i = 0; i < dragItems.size(); i++) {
                    if (!Objects.equals(r.getDragAnswer(i), dragSolution.get(i))) {
                        return false;
                    }
                }
                return true;
            default:
                return false;
        }
    }

    /** Descrizione testuale della risposta corretta. */
    public String getCorrectAnswerText() {
        StringBuilder sb = new StringBuilder();
        switch (type) {
            case SINGLE:
            case MULTI:
                for (int i : correctOptions) {
                    if (sb.length() > 0) {
                        sb.append(", ");
                    }
                    sb.append(letter(i));
                }
                break;
            case FILL:
                sb.append(String.join("  oppure  ", acceptedAnswers));
                break;
            case DRAG:
                for (int i = 0; i < dragItems.size(); i++) {
                    sb.append(dragItems.get(i)).append(" → ").append(dragSolution.get(i)).append('\n');
                }
                break;
            default:
                break;
        }
        return sb.toString().trim();
    }

    public static char letter(int index) {
        return (char) ('A' + index);
    }

    /** Normalizza una risposta libera: minuscolo, spazi compattati. */
    public static String normalize(String s) {
        if (s == null) {
            return "";
        }
        return s.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    /**
     * Restituisce una copia della domanda con le opzioni (SINGLE/MULTI) in ordine casuale,
     * così la posizione della risposta corretta non si può memorizzare. Le altre tipologie,
     * o le domande con "shuffle: no", vengono restituite invariate.
     */
    public Question shuffled(Random random) {
        if ((type != QuestionType.SINGLE && type != QuestionType.MULTI) || !shuffleAllowed) {
            return this;
        }
        List<Integer> perm = new ArrayList<>();
        for (int i = 0; i < options.size(); i++) {
            perm.add(i);
        }
        Collections.shuffle(perm, random);
        Builder b = new Builder()
            .id(id).domain(domain).type(type).text(text).exhibit(exhibit)
            .explanation(explanation).reference(reference).generated(generated)
            .shuffleAllowed(shuffleAllowed).source(source);
        for (int newIdx = 0; newIdx < perm.size(); newIdx++) {
            int oldIdx = perm.get(newIdx);
            b.option(options.get(oldIdx));
            if (correctOptions.contains(oldIdx)) {
                b.correct(newIdx);
            }
        }
        return b.build();
    }

    @Override
    public String toString() { return id; }

    /** Builder mutabile usato dal parser e dal generatore. */
    public static final class Builder {
        private String id;
        private Domain domain;
        private QuestionType type;
        private String text;
        private String exhibit;
        private final List<String> options = new ArrayList<>();
        private final Set<Integer> correctOptions = new LinkedHashSet<>();
        private final List<String> acceptedAnswers = new ArrayList<>();
        private final List<String> dragItems = new ArrayList<>();
        private final List<String> dragSolution = new ArrayList<>();
        private final List<String> extraTargets = new ArrayList<>();
        private String explanation;
        private String reference;
        private boolean generated;
        private boolean shuffleAllowed = true;
        private String source;

        public Builder id(String v) { this.id = v; return this; }
        public Builder domain(Domain v) { this.domain = v; return this; }
        public Builder type(QuestionType v) { this.type = v; return this; }
        public Builder text(String v) { this.text = v; return this; }
        public Builder exhibit(String v) { this.exhibit = v; return this; }
        public Builder option(String v) { this.options.add(v); return this; }
        public Builder correct(int index) { this.correctOptions.add(index); return this; }
        public Builder accept(String v) { this.acceptedAnswers.add(v); return this; }
        public Builder dragPair(String item, String target) {
            this.dragItems.add(item);
            this.dragSolution.add(target);
            return this;
        }
        public Builder extraTarget(String v) { this.extraTargets.add(v); return this; }
        public Builder explanation(String v) { this.explanation = v; return this; }
        public Builder reference(String v) { this.reference = v; return this; }
        public Builder generated(boolean v) { this.generated = v; return this; }
        public Builder shuffleAllowed(boolean v) { this.shuffleAllowed = v; return this; }
        public Builder source(String v) { this.source = v; return this; }

        public String getId() { return id; }

        /** Valida la coerenza della domanda e la costruisce. */
        public Question build() {
            if (Text.isBlank(id)) {
                throw new IllegalStateException("id mancante");
            }
            if (domain == null) {
                throw new IllegalStateException(id + ": domain mancante");
            }
            if (type == null) {
                throw new IllegalStateException(id + ": type mancante");
            }
            if (Text.isBlank(text)) {
                throw new IllegalStateException(id + ": text mancante");
            }
            switch (type) {
                case SINGLE:
                case MULTI:
                    if (options.size() < 2) {
                        throw new IllegalStateException(id + ": servono almeno 2 opzioni");
                    }
                    for (int c : correctOptions) {
                        if (c < 0 || c >= options.size()) {
                            throw new IllegalStateException(id + ": risposta fuori dalle opzioni");
                        }
                    }
                    if (type == QuestionType.SINGLE && correctOptions.size() != 1) {
                        throw new IllegalStateException(id + ": SINGLE richiede esattamente 1 risposta");
                    }
                    if (type == QuestionType.MULTI && correctOptions.size() < 2) {
                        throw new IllegalStateException(id + ": MULTI richiede almeno 2 risposte");
                    }
                    break;
                case FILL:
                    if (acceptedAnswers.isEmpty()) {
                        throw new IllegalStateException(id + ": FILL richiede almeno una risposta");
                    }
                    break;
                case DRAG:
                    if (dragItems.size() < 2) {
                        throw new IllegalStateException(id + ": DRAG richiede almeno 2 elementi");
                    }
                    break;
                default:
                    break;
            }
            return new Question(this);
        }
    }
}
