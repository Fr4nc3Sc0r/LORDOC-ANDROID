package com.lordoc.data;

import com.lordoc.model.Domain;
import com.lordoc.model.Question;
import com.lordoc.model.QuestionType;
import com.lordoc.util.Text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser del formato testuale delle domande (vedi README, sezione "Formato delle domande").
 *
 * <pre>
 * [Q]
 * id: NF-001
 * domain: 1
 * type: SINGLE
 * text:
 * Testo della domanda...
 * exhibit:
 * R1#show ip route
 * options:
 * A) prima opzione
 * B) seconda opzione
 * answer: B
 * explanation:
 * Spiegazione...
 * reference: Cisco IOS Configuration Guide
 * [/Q]
 * </pre>
 *
 * Le chiavi devono stare a inizio riga (colonna 0). Le righe che iniziano con '#'
 * fuori da un blocco [Q] sono commenti.
 */
public final class QuestionParser {

    private static final Pattern KEY = Pattern.compile(
        "^(id|domain|type|text|exhibit|options|answer|explanation|reference|items|targets|shuffle)\\s*:\\s?(.*)$");
    private static final Pattern OPTION = Pattern.compile("^\\s*([A-Za-z])[).]\\s+(.*)$");

    private QuestionParser() { }

    /**
     * Legge tutte le domande dal reader. Le domande non valide vengono saltate e
     * il relativo errore aggiunto a {@code errors}.
     */
    public static List<Question> parse(Reader reader, String source, List<String> errors) throws IOException {
        List<Question> result = new ArrayList<>();
        BufferedReader br = new BufferedReader(reader);
        String line;
        int lineNo = 0;
        int blockStart = 0;
        Map<String, StringBuilder> fields = null;
        String currentKey = null;

        while ((line = br.readLine()) != null) {
            lineNo++;
            String trimmed = line.trim();
            if (fields == null) {
                if (trimmed.equals("[Q]")) {
                    fields = new LinkedHashMap<>();
                    currentKey = null;
                    blockStart = lineNo;
                }
                continue; // commenti e righe fuori blocco ignorati
            }
            if (trimmed.equals("[/Q]")) {
                try {
                    result.add(build(fields, source));
                } catch (QuestionParseException | RuntimeException e) {
                    errors.add(source + " (blocco alla riga " + blockStart + "): " + e.getMessage());
                }
                fields = null;
                continue;
            }
            Matcher m = KEY.matcher(line);
            if (m.matches()) {
                currentKey = m.group(1);
                StringBuilder sb = new StringBuilder();
                if (!Text.isBlank(m.group(2))) {
                    sb.append(m.group(2));
                }
                fields.put(currentKey, sb);
            } else if (currentKey != null) {
                StringBuilder sb = fields.get(currentKey);
                if (sb.length() > 0) {
                    sb.append('\n');
                }
                sb.append(line);
            }
        }
        if (fields != null) {
            errors.add(source + ": blocco [Q] alla riga " + blockStart + " non chiuso con [/Q]");
        }
        return result;
    }

    private static Question build(Map<String, StringBuilder> f, String source) throws QuestionParseException {
        Question.Builder b = new Question.Builder().source(source);
        String id = value(f, "id");
        if (id.isEmpty()) {
            throw new QuestionParseException("campo 'id' mancante");
        }
        b.id(id);
        try {
            b.domain(Domain.parse(value(f, "domain")));
        } catch (IllegalArgumentException e) {
            throw new QuestionParseException(id + ": " + e.getMessage());
        }
        QuestionType type;
        try {
            type = QuestionType.valueOf(value(f, "type").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new QuestionParseException(id + ": tipo non valido '" + value(f, "type") + "'");
        }
        b.type(type);
        b.text(Text.trimBlock(raw(f, "text")));
        b.exhibit(Text.trimBlock(raw(f, "exhibit")));
        b.explanation(Text.trimBlock(raw(f, "explanation")));
        b.reference(value(f, "reference"));
        String shuffle = value(f, "shuffle").toLowerCase(Locale.ROOT);
        b.shuffleAllowed(!(shuffle.equals("no") || shuffle.equals("false")));

        String answer = value(f, "answer");
        switch (type) {
            case SINGLE:
            case MULTI:
                parseOptions(raw(f, "options"), b);
                for (String a : answer.split("[,\\s]+")) {
                    if (a.isEmpty()) {
                        continue;
                    }
                    char c = Character.toUpperCase(a.charAt(0));
                    if (a.length() != 1 || c < 'A' || c > 'Z') {
                        throw new QuestionParseException(id + ": risposta non valida '" + a + "'");
                    }
                    b.correct(c - 'A');
                }
                break;
            case FILL:
                for (String a : answer.split("\\|")) {
                    if (!Text.isBlank(a)) {
                        b.accept(a.trim());
                    }
                }
                break;
            case DRAG:
                for (String l : raw(f, "items").split("\n")) {
                    if (Text.isBlank(l)) {
                        continue;
                    }
                    int idx = l.indexOf("=>");
                    if (idx < 0) {
                        throw new QuestionParseException(id + ": riga 'items' senza '=>': " + l);
                    }
                    b.dragPair(l.substring(0, idx).trim(), l.substring(idx + 2).trim());
                }
                for (String t : value(f, "targets").split("\\|")) {
                    if (!Text.isBlank(t)) {
                        b.extraTarget(t.trim());
                    }
                }
                break;
            default:
                break;
        }
        try {
            return b.build();
        } catch (IllegalStateException e) {
            throw new QuestionParseException(e.getMessage());
        }
    }

    private static void parseOptions(String block, Question.Builder b) {
        StringBuilder current = null;
        List<String> opts = new ArrayList<>();
        for (String l : block.split("\n")) {
            Matcher m = OPTION.matcher(l);
            if (m.matches()) {
                if (current != null) {
                    opts.add(current.toString().trim());
                }
                current = new StringBuilder(m.group(2));
            } else if (current != null && !Text.isBlank(l)) {
                current.append(' ').append(l.trim());
            }
        }
        if (current != null) {
            opts.add(current.toString().trim());
        }
        opts.forEach(b::option);
    }

    private static String raw(Map<String, StringBuilder> f, String key) {
        StringBuilder sb = f.get(key);
        return sb == null ? "" : sb.toString();
    }

    private static String value(Map<String, StringBuilder> f, String key) {
        return raw(f, key).trim();
    }
}
