package com.lordoc.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.lordoc.model.Domain;
import com.lordoc.model.DomainScore;
import com.lordoc.model.ExamMode;
import com.lordoc.model.ExamResult;
import com.lordoc.model.ExamSession;
import com.lordoc.model.Question;
import com.lordoc.util.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Report finale: punteggio, risultato per dominio, elenco domande e revisione. */
public final class ResultsActivity extends Activity {

    private static final String[] FILTERS = {"Tutte", "Solo errate / incomplete", "Solo corrette", "Solo marcate"};

    private LordocApp app;
    private ExamResult result;
    private ExamSession session;
    private LinearLayout list;
    private final List<Integer> visible = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = LordocApp.get(this);
        result = app.getLastResult();
        if (result == null || result.getSession() == null) {
            finish();
            return;
        }
        session = result.getSession();
        setTitle("Risultati");
        LinearLayout root = Ui.vertical(this);
        setContentView(Ui.page(this, root));

        // --- intestazione
        boolean pass = result.isPassed();
        LinearLayout head = Ui.card(this);
        head.addView(Ui.text(this, result.getTitle() + " — " + result.getMode().getLabel(), 17, Ui.HEADER, true));
        head.addView(Ui.text(this, pass ? "SUPERATO" : "NON SUPERATO", 22, pass ? Ui.OK : Ui.KO, true));
        TextView score = Ui.text(this, result.getScaledScore() + " / " + ExamResult.MAX_SCORE, 40,
            pass ? Ui.OK : Ui.KO, true);
        head.addView(score);
        head.addView(new ScoreBarView(this, result.getScaledScore()), Ui.full(this, 6));
        head.addView(Ui.text(this, String.format(Locale.ITALY, "%d corrette su %d (%.1f%%) · tempo %s",
            result.getCorrect(), result.getTotal(), result.getPercent(),
            Text.formatDuration(result.getDurationSeconds())), 14, Ui.TEXT, false), Ui.full(this, 6));
        head.addView(Ui.text(this, "Punteggio stimato su scala 300-1000. Cisco non pubblica la soglia ufficiale: "
            + ExamResult.PASSING_SCORE + " è un obiettivo prudente.", 12, Ui.MUTED, false));
        root.addView(head, Ui.full(this, 6));

        // --- domini
        root.addView(Ui.section(this, "Risultato per dominio"));
        LinearLayout dom = Ui.card(this);
        for (Map.Entry<Domain, DomainScore> e : result.getByDomain().entrySet()) {
            DomainScore ds = e.getValue();
            dom.addView(Ui.text(this, e.getKey().getLabel() + " (" + e.getKey().getWeight() + "%)", 14, Ui.TEXT, true));
            dom.addView(Ui.bar(this, ds.getPercent(), Ui.colorFor(ds.getPercent())));
            dom.addView(Ui.text(this, ds.getCorrect() + " / " + ds.getTotal() + "  (" + Ui.pct(ds.getPercent()) + ")",
                13, Ui.MUTED, false), Ui.full(this, 8));
        }
        dom.addView(Ui.text(this, "Verde ≥ 82,5% · Arancio ≥ 60% · Rosso < 60%", 12, Ui.MUTED, false));
        root.addView(dom, Ui.full(this, 6));

        // --- azioni
        LinearLayout actions = Ui.horizontal(this);
        actions.addView(Ui.button(this, "Ripassa errori", v -> retryWrong()), Ui.weight(1));
        actions.addView(Ui.button(this, "Menu principale", v -> finish()), Ui.weight(1));
        root.addView(actions, Ui.full(this, 4));

        // --- domande
        root.addView(Ui.section(this, "Domande (tocca per rivedere)"));
        Spinner filter = new Spinner(this);
        ArrayAdapter<String> ad = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, FILTERS);
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        filter.setAdapter(ad);
        root.addView(filter, Ui.full(this, 6));
        list = Ui.vertical(this);
        root.addView(list);
        filter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                applyFilter(pos);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
        applyFilter(0);
    }

    private void applyFilter(int f) {
        visible.clear();
        list.removeAllViews();
        for (int i = 0; i < session.size(); i++) {
            Question q = session.getQuestion(i);
            boolean ok = q.isCorrect(session.getResponse(i));
            boolean include = f == 0 || (f == 1 && !ok) || (f == 2 && ok) || (f == 3 && session.isMarked(i));
            if (include) {
                visible.add(i);
            }
        }
        for (int pos = 0; pos < visible.size(); pos++) {
            int idx = visible.get(pos);
            Question q = session.getQuestion(idx);
            boolean ok = q.isCorrect(session.getResponse(idx));
            String esito = ok ? "✔ Corretta" : q.isAnswered(session.getResponse(idx)) ? "✘ Errata" : "✘ Mancante";
            LinearLayout row = Ui.card(this);
            row.setClickable(true);
            final int start = pos;
            row.setOnClickListener(v -> openReview(start));
            row.addView(Ui.text(this, (idx + 1) + ".  " + esito + (session.isMarked(idx) ? "   ⚑" : ""), 15,
                ok ? Ui.OK : Ui.KO, true));
            row.addView(Ui.text(this, q.getId() + " · " + q.getDomain().getLabel() + " · " + q.getType().getLabel(),
                12, Ui.MUTED, false));
            list.addView(row, Ui.full(this, 6));
        }
        if (visible.isEmpty()) {
            list.addView(Ui.text(this, "Nessuna domanda per questo filtro.", 14, Ui.MUTED, false));
        }
    }

    private void openReview(int startPos) {
        int[] idx = new int[visible.size()];
        for (int i = 0; i < idx.length; i++) {
            idx[i] = visible.get(i);
        }
        Intent i = new Intent(this, ReviewActivity.class);
        i.putExtra(ReviewActivity.EXTRA_INDEXES, idx);
        i.putExtra(ReviewActivity.EXTRA_START, startPos);
        startActivity(i);
    }

    private void retryWrong() {
        List<Question> wrong = new ArrayList<>();
        for (int i = 0; i < session.size(); i++) {
            if (!session.getQuestion(i).isCorrect(session.getResponse(i))) {
                wrong.add(session.getQuestion(i));
            }
        }
        if (wrong.isEmpty()) {
            Toast.makeText(this, "Nessun errore: complimenti!", Toast.LENGTH_LONG).show();
            return;
        }
        app.startSession(app.getFactory().fromQuestions("Ripasso errori", ExamMode.STUDY, wrong));
        startActivity(new Intent(this, ExamActivity.class));
        finish();
    }
}
