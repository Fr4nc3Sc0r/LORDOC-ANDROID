package com.lordoc.data;

import com.lordoc.model.Domain;
import com.lordoc.model.DomainScore;
import com.lordoc.model.ExamMode;
import com.lordoc.model.ExamResult;
import com.lordoc.model.ExamSession;
import com.lordoc.model.Question;
import com.lordoc.util.Text;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dati persistenti dell'utente, salvati come file di testo (TSV) nella cartella privata dell'app:
 * <ul>
 *   <li>{@code history.tsv} – storico degli esami svolti</li>
 *   <li>{@code stats.tsv} – statistiche per domanda (viste, corrette, marcate)</li>
 *   <li>{@code questions/} – cartella per domande aggiuntive dell'utente</li>
 * </ul>
 */
public final class UserDataStore {

    private final Path dir;
    private final Path historyFile;
    private final Path statsFile;
    private final List<ExamResult> history = new ArrayList<>();
    private final Map<String, QuestionStat> stats = new HashMap<>();

    public UserDataStore(Path dir) {
        this.dir = dir;
        this.historyFile = dir.resolve("history.tsv");
        this.statsFile = dir.resolve("stats.tsv");
    }

    public Path getDirectory() { return dir; }

    public Path getUserQuestionsDirectory() { return dir.resolve("questions"); }

    public void load() {
        try {
            Files.createDirectories(getUserQuestionsDirectory());
        } catch (IOException e) {
            System.err.println("Impossibile creare " + dir + ": " + e.getMessage());
        }
        loadHistory();
        loadStats();
    }

    // ------------------------------------------------------------------ storico

    public List<ExamResult> getHistory() { return Collections.unmodifiableList(history); }

    private void loadHistory() {
        history.clear();
        if (!Files.exists(historyFile)) {
            return;
        }
        try {
            for (String line : Files.readAllLines(historyFile, StandardCharsets.UTF_8)) {
                if (Text.isBlank(line) || line.startsWith("#")) {
                    continue;
                }
                try {
                    history.add(parseHistoryLine(line));
                } catch (RuntimeException e) {
                    System.err.println("Riga di storico ignorata: " + line);
                }
            }
        } catch (IOException e) {
            System.err.println("Errore lettura storico: " + e.getMessage());
        }
    }

    private static ExamResult parseHistoryLine(String line) {
        String[] p = line.split("\t", -1);
        LocalDateTime date = LocalDateTime.parse(p[0]);
        String title = p[1];
        ExamMode mode = ExamMode.valueOf(p[2]);
        int correct = Integer.parseInt(p[3]);
        int total = Integer.parseInt(p[4]);
        int duration = Integer.parseInt(p[5]);
        Map<Domain, DomainScore> byDomain = new EnumMap<>(Domain.class);
        if (p.length > 6 && !p[6].isEmpty()) {
            for (String part : p[6].split(";")) {
                String[] kv = part.split("[:/]");
                byDomain.put(Domain.valueOf(kv[0]),
                    new DomainScore(Integer.parseInt(kv[1]), Integer.parseInt(kv[2])));
            }
        }
        return new ExamResult(title, mode, date, correct, total, duration, byDomain, null);
    }

    private static String toHistoryLine(ExamResult r) {
        StringBuilder d = new StringBuilder();
        for (Map.Entry<Domain, DomainScore> e : r.getByDomain().entrySet()) {
            if (d.length() > 0) {
                d.append(';');
            }
            d.append(e.getKey().name()).append(':').append(e.getValue().getCorrect())
             .append('/').append(e.getValue().getTotal());
        }
        return String.join("\t",
            r.getDate().withNano(0).toString(),
            r.getTitle().replace('\t', ' '),
            r.getMode().name(),
            String.valueOf(r.getCorrect()),
            String.valueOf(r.getTotal()),
            String.valueOf(r.getDurationSeconds()),
            d.toString());
    }

    /** Registra il risultato nello storico e aggiorna le statistiche delle domande. */
    public void recordResult(ExamResult result) {
        history.add(result);
        try {
            Files.createDirectories(dir);
            byte[] line = (toHistoryLine(result) + "\n").getBytes(StandardCharsets.UTF_8);
            Files.write(historyFile, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("Errore salvataggio storico: " + e.getMessage());
        }
        ExamSession s = result.getSession();
        if (s != null) {
            for (int i = 0; i < s.size(); i++) {
                Question q = s.getQuestion(i);
                if (q.isGenerated()) {
                    continue;
                }
                QuestionStat st = stat(q.getId());
                st.record(q.isCorrect(s.getResponse(i)));
                if (s.isMarked(i)) {
                    st.setFlagged(true);
                }
            }
            saveStats();
        }
    }

    public void clearHistory() {
        history.clear();
        stats.clear();
        try {
            Files.deleteIfExists(historyFile);
            Files.deleteIfExists(statsFile);
        } catch (IOException e) {
            System.err.println("Errore cancellazione dati: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------- statistiche

    public QuestionStat stat(String id) { return stats.computeIfAbsent(id, k -> new QuestionStat()); }

    public QuestionStat peekStat(String id) { return stats.get(id); }

    public void setFlagged(String id, boolean flagged) {
        stat(id).setFlagged(flagged);
        saveStats();
    }

    private void loadStats() {
        stats.clear();
        if (!Files.exists(statsFile)) {
            return;
        }
        try {
            for (String line : Files.readAllLines(statsFile, StandardCharsets.UTF_8)) {
                String[] p = line.split("\t");
                if (p.length < 5 || line.startsWith("#")) {
                    continue;
                }
                try {
                    stats.put(p[0], new QuestionStat(Integer.parseInt(p[1]), Integer.parseInt(p[2]),
                        Boolean.parseBoolean(p[3]), Boolean.parseBoolean(p[4])));
                } catch (NumberFormatException e) {
                    System.err.println("Riga di statistiche ignorata: " + line);
                }
            }
        } catch (IOException e) {
            System.err.println("Errore lettura statistiche: " + e.getMessage());
        }
    }

    private void saveStats() {
        try {
            Files.createDirectories(dir);
            try (BufferedWriter w = Files.newBufferedWriter(statsFile, StandardCharsets.UTF_8)) {
                w.write("# id\tseen\tcorrect\tlastCorrect\tflagged");
                w.newLine();
                for (Map.Entry<String, QuestionStat> e : stats.entrySet()) {
                    QuestionStat s = e.getValue();
                    w.write(e.getKey() + "\t" + s.getSeen() + "\t" + s.getCorrect() + "\t"
                        + s.isLastCorrect() + "\t" + s.isFlagged());
                    w.newLine();
                }
            }
        } catch (IOException e) {
            System.err.println("Errore salvataggio statistiche: " + e.getMessage());
        }
    }
}
