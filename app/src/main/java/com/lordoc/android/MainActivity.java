package com.lordoc.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.lordoc.data.QuestionStat;
import com.lordoc.engine.ExamConfig;
import com.lordoc.engine.ExamFactory;
import com.lordoc.engine.PresetExam;
import com.lordoc.engine.QuestionFilter;
import com.lordoc.model.Domain;
import com.lordoc.model.ExamMode;
import com.lordoc.model.ExamResult;
import com.lordoc.model.ExamSession;
import com.lordoc.model.Question;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

/** Menu principale: esami predefiniti, altre modalità, strumenti e copertura per dominio. */
public final class MainActivity extends Activity {

    private static final int REQ_IMPORT = 1;
    private static final String PREFS = "lordoc";
    private static boolean errorsShown;

    private LordocApp app;
    private LinearLayout root;
    private List<PresetExam> presets;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = LordocApp.get(this);
        root = Ui.vertical(this);
        setContentView(Ui.page(this, root));
        if (!errorsShown && !app.getBank().getErrors().isEmpty()) {
            errorsShown = true;
            showLoadErrors();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        build();
    }

    private SharedPreferences prefs() {
        return getSharedPreferences(PREFS, MODE_PRIVATE);
    }

    // ------------------------------------------------------------------ costruzione della pagina

    private void build() {
        root.removeAllViews();
        presets = app.getFactory().presetExams();

        LinearLayout head = Ui.card(this);
        head.setBackground(Ui.rounded(this, Ui.HEADER));
        head.addView(Ui.text(this, "Lordoc", 28, 0xFFFFFFFF, true));
        head.addView(Ui.text(this, "Simulatore d'esame Cisco CCNA 200-301\n" + app.getBank().size()
            + " domande in banca + subnetting illimitato", 14, 0xFFC9D6E6, false));
        root.addView(head, Ui.full(this, 6));

        // --- esami predefiniti
        root.addView(Ui.section(this, "Esami predefiniti"));
        root.addView(Ui.text(this, "Domande fisse, distribuite secondo i pesi del blueprint. Prima in Studio, "
            + "poi in Simulazione per misurarti.", 14, Ui.MUTED, false), Ui.full(this, 8));
        for (PresetExam p : presets) {
            root.addView(presetCard(p), Ui.full(this, 10));
        }
        CheckBox back = new CheckBox(this);
        back.setText("In Simulazione consenti di tornare alle domande precedenti (l'esame Cisco reale NON lo consente)");
        back.setChecked(prefs().getBoolean("allowBack", true));
        back.setOnCheckedChangeListener((b, v) -> prefs().edit().putBoolean("allowBack", v).apply());
        root.addView(back, Ui.full(this, 4));

        // --- altre modalità
        root.addView(Ui.section(this, "Altre modalità"));
        addMode("Esame completo casuale", "100 domande pesate come all'esame reale, a tempo.", v -> startFullRandom());
        addMode("Esame personalizzato…", "Scegli domini, filtri, numero di domande, tempo e modalità.",
            v -> startActivity(new Intent(this, CustomExamActivity.class)));
        addMode("Drill di subnetting", "20 esercizi generati al momento, con spiegazione passo-passo.",
            v -> startSubnetDrill());
        addMode("Ripassa gli errori", "Le domande sbagliate l'ultima volta, in modalità Studio.",
            v -> startFiltered(QuestionFilter.LAST_WRONG, "Ripasso errori"));
        addMode("Ripassa le marcate", "Le domande che hai marcato durante gli esami.",
            v -> startFiltered(QuestionFilter.FLAGGED, "Ripasso domande marcate"));

        // --- strumenti
        root.addView(Ui.section(this, "Strumenti"));
        addMode("Storico e progressi", "Esami svolti, grafico dei punteggi, accuratezza per dominio.",
            v -> startActivity(new Intent(this, HistoryActivity.class)));
        addMode("Cheat sheet", "Schede di ripasso: subnetting, OSPF, STP, porte, comandi IOS…",
            v -> startActivity(new Intent(this, ReferenceActivity.class)));
        addMode("Calcolatore subnet", "Rete, broadcast, host, wildcard e magic number.",
            v -> startActivity(new Intent(this, SubnetCalculatorActivity.class)));
        addMode("Importa domande (.txt)", "Aggiungi file di domande scritti nel formato di Lordoc.",
            v -> pickQuestionFiles());

        // --- copertura per dominio
        root.addView(Ui.section(this, "Copertura e accuratezza per dominio"));
        LinearLayout cov = Ui.card(this);
        for (Domain d : Domain.values()) {
            cov.addView(domainRow(d), Ui.full(this, 8));
        }
        root.addView(cov, Ui.full(this, 10));

        Button about = Ui.button(this, "Informazioni", v -> showAbout());
        root.addView(about, Ui.full(this, 0));
    }

    private View presetCard(PresetExam p) {
        LinearLayout card = Ui.card(this);
        int attempts = 0;
        int best = -1;
        int last = -1;
        for (ExamResult res : app.getStore().getHistory()) {
            if (res.getTitle().equals(p.getName())) {
                attempts++;
                best = Math.max(best, res.getScaledScore());
                last = res.getScaledScore();
            }
        }
        card.addView(Ui.text(this, p.getName(), 18, Ui.TEXT, true));
        String info = p.size() + " domande · " + ExamFactory.suggestedMinutes(p.size()) + " min · tentativi: " + attempts;
        if (best >= 0) {
            info += "\nmigliore: " + best + (best >= ExamResult.PASSING_SCORE ? " ✔" : "") + " · ultimo: " + last;
        }
        card.addView(Ui.text(this, info, 14, Ui.MUTED, false), Ui.full(this, 6));
        LinearLayout row = Ui.horizontal(this);
        row.addView(Ui.button(this, "▶ Simulazione", v -> startPreset(p, ExamMode.SIMULATION)), Ui.weight(1));
        row.addView(Ui.button(this, "Studio", v -> startPreset(p, ExamMode.STUDY)), Ui.weight(1));
        card.addView(row);
        return card;
    }

    private void addMode(String title, String desc, View.OnClickListener action) {
        LinearLayout card = Ui.card(this);
        card.setClickable(true);
        card.setOnClickListener(action);
        card.addView(Ui.text(this, title, 16, Ui.ACCENT, true));
        card.addView(Ui.text(this, desc, 14, Ui.MUTED, false));
        root.addView(card, Ui.full(this, 8));
    }

    private View domainRow(Domain d) {
        List<Question> qs = app.getBank().byDomain(d);
        int seen = 0;
        int answers = 0;
        int correct = 0;
        for (Question q : qs) {
            QuestionStat s = app.getStore().peekStat(q.getId());
            if (s != null && s.getSeen() > 0) {
                seen++;
                answers += s.getSeen();
                correct += s.getCorrect();
            }
        }
        LinearLayout box = Ui.vertical(this);
        box.addView(Ui.text(this, d.getLabel() + "  (" + d.getWeight() + "%)", 14, Ui.TEXT, true));
        String acc = answers == 0 ? "—" : Ui.pct(100.0 * correct / answers);
        box.addView(Ui.text(this, qs.size() + " domande · viste " + seen + " · accuratezza " + acc,
            13, Ui.MUTED, false));
        if (answers > 0) {
            double p = 100.0 * correct / answers;
            box.addView(Ui.bar(this, p, Ui.colorFor(p)));
        }
        return box;
    }

    // ------------------------------------------------------------------ avvio degli esami

    private void startExam(ExamSession s) {
        if (s == null) {
            Toast.makeText(this, "Nessuna domanda corrisponde ai criteri scelti.", Toast.LENGTH_LONG).show();
            return;
        }
        app.startSession(s);
        startActivity(new Intent(this, ExamActivity.class));
    }

    private void startPreset(PresetExam p, ExamMode mode) {
        boolean back = mode == ExamMode.STUDY || prefs().getBoolean("allowBack", true);
        if (mode == ExamMode.SIMULATION) {
            new AlertDialog.Builder(this)
                .setTitle("Avvio simulazione")
                .setMessage(p.getName() + ": " + p.size() + " domande in "
                    + ExamFactory.suggestedMinutes(p.size()) + " minuti.\nIl cronometro parte subito. Pronto?")
                .setPositiveButton("Inizia", (d, w) -> startExam(app.getFactory().fromPreset(p, mode, back)))
                .setNegativeButton("Annulla", null)
                .show();
        } else {
            startExam(app.getFactory().fromPreset(p, mode, back));
        }
    }

    private void startFullRandom() {
        int n = Math.min(100, app.getBank().size());
        startExam(app.getFactory().custom(new ExamConfig()
            .title("Esame completo casuale")
            .mode(ExamMode.SIMULATION)
            .questionCount(n)
            .timeMinutes(ExamFactory.suggestedMinutes(n))
            .allowBack(prefs().getBoolean("allowBack", true))));
    }

    private void startSubnetDrill() {
        startExam(app.getFactory().custom(new ExamConfig()
            .title("Drill di subnetting")
            .mode(ExamMode.STUDY)
            .domains(EnumSet.noneOf(Domain.class))
            .questionCount(0)
            .subnetQuestions(20)));
    }

    private void startFiltered(QuestionFilter f, String title) {
        ExamSession s = app.getFactory().custom(new ExamConfig().title(title).mode(ExamMode.STUDY)
            .filter(f).questionCount(Integer.MAX_VALUE).weighted(false));
        if (s == null) {
            Toast.makeText(this, "Niente da ripassare per ora: svolgi qualche esame.", Toast.LENGTH_LONG).show();
            return;
        }
        startExam(s);
    }

    // ------------------------------------------------------------------ importazione domande

    private void pickQuestionFiles() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        i.putExtra(Intent.EXTRA_MIME_TYPES, new String[] {"text/plain", "application/octet-stream"});
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(i, REQ_IMPORT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_IMPORT || resultCode != RESULT_OK || data == null) {
            return;
        }
        int copied = 0;
        StringBuilder problems = new StringBuilder();
        if (data.getClipData() != null) {
            for (int k = 0; k < data.getClipData().getItemCount(); k++) {
                copied += importOne(data.getClipData().getItemAt(k).getUri(), problems);
            }
        } else if (data.getData() != null) {
            copied += importOne(data.getData(), problems);
        }
        int before = app.getBank().size();
        app.reloadQuestions();
        int added = app.getBank().size() - before;
        String msg = String.format(Locale.ITALY, "File importati: %d\nNuove domande caricate: %d", copied, added);
        if (problems.length() > 0) {
            msg += "\n\n" + problems;
        }
        if (!app.getBank().getErrors().isEmpty()) {
            msg += "\n\nAvvisi:\n" + String.join("\n", app.getBank().getErrors());
        }
        new AlertDialog.Builder(this).setTitle("Importazione domande").setMessage(msg)
            .setPositiveButton("OK", null).show();
        build();
    }

    private int importOne(Uri uri, StringBuilder problems) {
        String name = displayName(uri);
        name = name.replaceAll("[^A-Za-z0-9._-]", "_");
        if (!name.toLowerCase(Locale.ROOT).endsWith(".txt")) {
            name += ".txt";
        }
        Path target = app.getStore().getUserQuestionsDirectory().resolve(name);
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            if (in == null) {
                throw new IOException("file non leggibile");
            }
            Files.createDirectories(target.getParent());
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            return 1;
        } catch (IOException e) {
            problems.append(name).append(": ").append(e.getMessage()).append('\n');
            return 0;
        }
    }

    private String displayName(Uri uri) {
        try (Cursor c = getContentResolver().query(uri, new String[] {OpenableColumns.DISPLAY_NAME},
                null, null, null)) {
            if (c != null && c.moveToFirst()) {
                String n = c.getString(0);
                if (n != null && !n.isEmpty()) {
                    return n;
                }
            }
        }
        return "domande_" + System.currentTimeMillis() + ".txt";
    }

    // ------------------------------------------------------------------ messaggi

    private void showLoadErrors() {
        StringBuilder sb = new StringBuilder("Alcune domande non sono state caricate:\n\n");
        List<String> errs = app.getBank().getErrors();
        for (int i = 0; i < Math.min(15, errs.size()); i++) {
            sb.append("• ").append(errs.get(i)).append('\n');
        }
        new AlertDialog.Builder(this).setTitle("Avvisi caricamento domande").setMessage(sb.toString())
            .setPositiveButton("OK", null).show();
    }

    private void showAbout() {
        TextView t = Ui.text(this, "Lordoc " + LordocApp.VERSION + "\n\n"
            + "Simulatore d'esame per la preparazione alla certificazione Cisco CCNA 200-301.\n"
            + "Domande nella banca: " + app.getBank().size() + "\n\n"
            + "Realizzato in collaborazione con Anthropic Claude.\n\n"
            + "Progetto indipendente, non affiliato a Cisco Systems. "
            + "CCNA è un marchio registrato di Cisco Systems, Inc.", 15, Ui.TEXT, false);
        int p = Ui.dp(this, 20);
        t.setPadding(p, p, p, 0);
        new AlertDialog.Builder(this).setTitle("Informazioni").setView(t).setPositiveButton("OK", null).show();
    }
}
