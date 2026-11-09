package com.lordoc.engine;

import com.lordoc.data.QuestionBank;
import com.lordoc.data.QuestionStat;
import com.lordoc.data.SubnetQuestionGenerator;
import com.lordoc.data.UserDataStore;
import com.lordoc.model.Domain;
import com.lordoc.model.ExamMode;
import com.lordoc.model.ExamSession;
import com.lordoc.model.Question;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/** Costruisce gli esami: predefiniti, casuali e personalizzati. */
public final class ExamFactory {

    /** Dimensione indicativa di un esame predefinito. */
    public static final int PRESET_SIZE = 70;
    /** Minuti per domanda (l'esame reale: ~120 minuti per ~100 domande). */
    public static final double MINUTES_PER_QUESTION = 1.2;
    private static final long PRESET_SEED = 200301L;

    private final QuestionBank bank;
    private final UserDataStore store;
    private final Random random = new Random();

    public ExamFactory(QuestionBank bank, UserDataStore store) {
        this.bank = bank;
        this.store = store;
    }

    public static int suggestedMinutes(int questions) {
        return (int) Math.ceil(questions * MINUTES_PER_QUESTION);
    }

    /**
     * Suddivide in modo deterministico la banca domande in esami disgiunti, ognuno con
     * una distribuzione per dominio simile a quella dell'esame reale.
     */
    public List<PresetExam> presetExams() {
        int n = Math.max(1, bank.size() / PRESET_SIZE);
        List<List<String>> buckets = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            buckets.add(new ArrayList<>());
        }
        Random r = new Random(PRESET_SEED);
        int next = 0;
        for (Domain d : Domain.values()) {
            List<Question> qs = bank.byDomain(d);
            Collections.shuffle(qs, r);
            for (Question q : qs) {
                buckets.get(next % n).add(q.getId());
                next++;
            }
        }
        List<PresetExam> exams = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            List<String> ids = buckets.get(i);
            Collections.shuffle(ids, r);
            exams.add(new PresetExam("Esame " + (char) ('A' + i), ids));
        }
        return exams;
    }

    public ExamSession fromPreset(PresetExam preset, ExamMode mode, boolean allowBack) {
        List<Question> qs = new ArrayList<>();
        for (String id : preset.getQuestionIds()) {
            Question q = bank.get(id);
            if (q != null) {
                qs.add(q.shuffled(random));
            }
        }
        int time = mode == ExamMode.SIMULATION ? suggestedMinutes(qs.size()) * 60 : 0;
        return new ExamSession(preset.getName(), mode, qs, time, allowBack);
    }

    /** Esame personalizzato. Restituisce null se nessuna domanda soddisfa i criteri. */
    public ExamSession custom(ExamConfig cfg) {
        List<Question> pool = new ArrayList<>();
        for (Question q : bank.all()) {
            if (cfg.getDomains().contains(q.getDomain()) && matches(q, cfg.getFilter())) {
                pool.add(q);
            }
        }
        List<Question> chosen = cfg.isWeighted()
            ? weightedPick(pool, cfg.getDomains(), cfg.getQuestionCount())
            : randomPick(pool, cfg.getQuestionCount());
        chosen.replaceAll(q -> q.shuffled(random));
        if (cfg.getSubnetQuestions() > 0) {
            chosen.addAll(new SubnetQuestionGenerator(random).generate(cfg.getSubnetQuestions()));
        }
        if (chosen.isEmpty()) {
            return null;
        }
        Collections.shuffle(chosen, random);
        return new ExamSession(cfg.getTitle(), cfg.getMode(), chosen, cfg.getTimeMinutes() * 60, cfg.isAllowBack());
    }

    /** Esame costruito su una lista precisa di domande (es. "ripeti gli errori"). */
    public ExamSession fromQuestions(String title, ExamMode mode, List<Question> questions) {
        List<Question> qs = new ArrayList<>();
        for (Question q : questions) {
            qs.add(q.shuffled(random));
        }
        Collections.shuffle(qs, random);
        int time = mode == ExamMode.SIMULATION ? suggestedMinutes(qs.size()) * 60 : 0;
        return new ExamSession(title, mode, qs, time, true);
    }

    private boolean matches(Question q, QuestionFilter f) {
        QuestionStat s = store.peekStat(q.getId());
        switch (f) {
            case UNSEEN: return s == null || s.getSeen() == 0;
            case MISSED: return s != null && s.getWrong() > 0;
            case LAST_WRONG: return s != null && s.getSeen() > 0 && !s.isLastCorrect();
            case FLAGGED: return s != null && s.isFlagged();
            default: return true;
        }
    }

    private List<Question> randomPick(List<Question> pool, int count) {
        List<Question> copy = new ArrayList<>(pool);
        Collections.shuffle(copy, random);
        return new ArrayList<>(copy.subList(0, Math.min(count, copy.size())));
    }

    /** Selezione proporzionale ai pesi dei domini; i posti non coperti vengono riempiti a caso. */
    private List<Question> weightedPick(List<Question> pool, Set<Domain> domains, int count) {
        Map<Domain, List<Question>> byDomain = new EnumMap<>(Domain.class);
        for (Question q : pool) {
            byDomain.computeIfAbsent(q.getDomain(), k -> new ArrayList<>()).add(q);
        }
        int totalWeight = 0;
        for (Domain d : domains) {
            totalWeight += d.getWeight();
        }
        List<Question> chosen = new ArrayList<>();
        List<Question> leftovers = new ArrayList<>();
        for (Domain d : domains) {
            List<Question> qs = byDomain.getOrDefault(d, new ArrayList<>());
            Collections.shuffle(qs, random);
            int quota = totalWeight == 0 ? 0 : (int) Math.round((double) count * d.getWeight() / totalWeight);
            int take = Math.min(quota, qs.size());
            chosen.addAll(qs.subList(0, take));
            leftovers.addAll(qs.subList(take, qs.size()));
        }
        Collections.shuffle(leftovers, random);
        for (Question q : leftovers) {
            if (chosen.size() >= count) {
                break;
            }
            chosen.add(q);
        }
        while (chosen.size() > count) {
            chosen.remove(chosen.size() - 1);
        }
        return chosen;
    }
}
