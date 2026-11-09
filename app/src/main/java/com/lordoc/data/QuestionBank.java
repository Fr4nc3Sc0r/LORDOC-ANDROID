package com.lordoc.data;

import com.lordoc.model.Domain;
import com.lordoc.model.Question;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Archivio delle domande. Carica le domande incluse nell'app (file elencati in
 * {@code questions/index.txt} negli assets) e quelle importate dall'utente nella cartella
 * {@code questions/} dei dati dell'app.
 */
public final class QuestionBank {

    private static final String DIR = "questions/";
    private static final String INDEX = DIR + "index.txt";

    private final Map<String, Question> byId = new LinkedHashMap<>();
    private final List<String> errors = new ArrayList<>();

    /**
     * @param opener           apre i file inclusi nell'app
     * @param userQuestionsDir cartella con le domande dell'utente (può essere null)
     */
    public static QuestionBank load(ResourceOpener opener, Path userQuestionsDir) {
        QuestionBank bank = new QuestionBank();
        bank.loadBuiltIn(opener);
        if (userQuestionsDir != null && Files.isDirectory(userQuestionsDir)) {
            bank.loadDirectory(userQuestionsDir);
        }
        return bank;
    }

    private void loadBuiltIn(ResourceOpener opener) {
        List<String> files = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(opener.open(INDEX), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    files.add(line);
                }
            }
        } catch (IOException e) {
            errors.add("Indice delle domande non trovato: " + INDEX);
            return;
        }
        for (String name : files) {
            try (InputStream in = opener.open(DIR + name);
                 Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                addAll(QuestionParser.parse(r, name, errors));
            } catch (IOException e) {
                errors.add("File mancante o illeggibile: " + DIR + name);
            }
        }
    }

    private void loadDirectory(Path dir) {
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, "*.txt")) {
            for (Path p : ds) {
                try (Reader r = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
                    addAll(QuestionParser.parse(r, p.getFileName().toString(), errors));
                } catch (IOException e) {
                    errors.add(p.getFileName() + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            errors.add(dir + ": " + e.getMessage());
        }
    }

    private void addAll(List<Question> list) {
        for (Question q : list) {
            if (byId.containsKey(q.getId())) {
                errors.add("ID duplicato ignorato: " + q.getId() + " (" + q.getSource() + ")");
            } else {
                byId.put(q.getId(), q);
            }
        }
    }

    public List<Question> all() { return new ArrayList<>(byId.values()); }

    public int size() { return byId.size(); }

    public Question get(String id) { return byId.get(id); }

    public List<String> getErrors() { return Collections.unmodifiableList(errors); }

    public List<Question> byDomain(Domain d) {
        List<Question> list = new ArrayList<>();
        for (Question q : byId.values()) {
            if (q.getDomain() == d) {
                list.add(q);
            }
        }
        return list;
    }

    public Map<Domain, Integer> countByDomain() {
        Map<Domain, Integer> m = new EnumMap<>(Domain.class);
        for (Domain d : Domain.values()) {
            m.put(d, 0);
        }
        for (Question q : byId.values()) {
            m.merge(q.getDomain(), 1, Integer::sum);
        }
        return m;
    }

}
