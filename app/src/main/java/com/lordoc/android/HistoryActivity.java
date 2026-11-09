package com.lordoc.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;

import com.lordoc.model.Domain;
import com.lordoc.model.DomainScore;
import com.lordoc.model.ExamResult;
import com.lordoc.util.Text;

import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Storico degli esami, grafico dei punteggi e accuratezza complessiva per dominio. */
public final class HistoryActivity extends Activity {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private LinearLayout root;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Storico e progressi");
        root = Ui.vertical(this);
        setContentView(Ui.page(this, root));
        build();
    }

    private void build() {
        root.removeAllViews();
        LordocApp app = LordocApp.get(this);
        List<ExamResult> history = app.getStore().getHistory();

        root.addView(Ui.section(this, "Andamento dei punteggi"));
        LinearLayout chartCard = Ui.card(this);
        chartCard.addView(new ScoreChartView(this, history));
        root.addView(chartCard, Ui.full(this, 6));

        root.addView(Ui.section(this, "Accuratezza complessiva per dominio"));
        Map<Domain, DomainScore> agg = new EnumMap<>(Domain.class);
        for (ExamResult r : history) {
            for (Map.Entry<Domain, DomainScore> e : r.getByDomain().entrySet()) {
                agg.computeIfAbsent(e.getKey(), k -> new DomainScore()).add(e.getValue());
            }
        }
        LinearLayout dom = Ui.card(this);
        for (Domain d : Domain.values()) {
            DomainScore s = agg.get(d);
            dom.addView(Ui.text(this, d.getLabel(), 14, Ui.TEXT, true));
            if (s == null || s.getTotal() == 0) {
                dom.addView(Ui.text(this, "nessun dato", 13, Ui.MUTED, false), Ui.full(this, 8));
            } else {
                dom.addView(Ui.bar(this, s.getPercent(), Ui.colorFor(s.getPercent())));
                dom.addView(Ui.text(this, s.getCorrect() + " / " + s.getTotal() + "  (" + Ui.pct(s.getPercent()) + ")",
                    13, Ui.MUTED, false), Ui.full(this, 8));
            }
        }
        root.addView(dom, Ui.full(this, 6));

        root.addView(Ui.section(this, "Esami svolti (" + history.size() + ")"));
        if (history.isEmpty()) {
            root.addView(Ui.text(this, "Ancora nessun esame consegnato.", 14, Ui.MUTED, false));
        }
        for (int i = history.size() - 1; i >= 0; i--) {
            ExamResult r = history.get(i);
            LinearLayout row = Ui.card(this);
            row.addView(Ui.text(this, r.getTitle() + " · " + r.getMode().getLabel(), 15, Ui.TEXT, true));
            row.addView(Ui.text(this, r.getDate().format(FMT) + " · " + r.getCorrect() + "/" + r.getTotal()
                + " · durata " + Text.formatDuration(r.getDurationSeconds()), 13, Ui.MUTED, false));
            row.addView(Ui.text(this, r.getScaledScore() + "  " + (r.isPassed() ? "✔ Superato" : "✘ Non superato"),
                15, r.isPassed() ? Ui.OK : Ui.KO, true));
            root.addView(row, Ui.full(this, 6));
        }

        Button clear = Ui.button(this, "Azzera storico e statistiche", v -> new AlertDialog.Builder(this)
            .setTitle("Conferma")
            .setMessage("Cancellare definitivamente storico esami e statistiche delle domande?")
            .setPositiveButton("Cancella", (d, w) -> {
                app.getStore().clearHistory();
                build();
            })
            .setNegativeButton("Annulla", null)
            .show());
        clear.setTextColor(Ui.KO);
        root.addView(clear, Ui.full(this, 0));
    }
}
