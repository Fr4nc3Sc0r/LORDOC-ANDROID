package com.lordoc.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.lordoc.engine.Scorer;
import com.lordoc.model.ExamMode;
import com.lordoc.model.ExamResult;
import com.lordoc.model.ExamSession;
import com.lordoc.model.Question;
import com.lordoc.util.Text;

/** Svolgimento dell'esame: una domanda alla volta, con cronometro, marcatura e consegna. */
public final class ExamActivity extends Activity {

    private LordocApp app;
    private ExamSession session;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean finished;

    private TextView counter;
    private TextView timer;
    private TextView info;
    private TextView progressText;
    private CheckBox markBox;
    private ProgressBar progress;
    private ScrollView scroll;
    private LinearLayout questionBox;
    private Button prevBtn;
    private Button nextBtn;
    private Button revealBtn;

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            tick();
            if (!finished) {
                handler.postDelayed(this, 1000);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = LordocApp.get(this);
        session = app.getCurrentSession();
        if (session == null) {
            // ad esempio se Android ha chiuso l'app mentre era in secondo piano
            finish();
            return;
        }
        setTitle(session.getTitle() + " — " + session.getMode().getLabel());
        setContentView(buildLayout());
        show(session.getCurrent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (session != null && !finished) {
            handler.post(ticker);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(ticker);
    }

    // ------------------------------------------------------------------ interfaccia

    private LinearLayout buildLayout() {
        LinearLayout root = Ui.vertical(this);

        LinearLayout top = Ui.horizontal(this);
        top.setBackgroundColor(Ui.HEADER);
        int p = Ui.dp(this, 12);
        top.setPadding(p, Ui.dp(this, 8), p, Ui.dp(this, 8));
        counter = Ui.text(this, "", 16, 0xFFFFFFFF, true);
        timer = Ui.text(this, "", 18, 0xFFFFFFFF, true);
        timer.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        top.addView(counter, Ui.weight(1));
        top.addView(timer);
        root.addView(top);

        LinearLayout sub = Ui.vertical(this);
        sub.setBackgroundColor(Ui.SUB);
        sub.setPadding(p, Ui.dp(this, 4), p, Ui.dp(this, 6));
        LinearLayout subRow = Ui.horizontal(this);
        markBox = new CheckBox(this);
        markBox.setText("Marca");
        subRow.addView(markBox);
        info = Ui.text(this, "", 12, Ui.MUTED, false);
        info.setGravity(android.view.Gravity.END);
        subRow.addView(info, Ui.weight(1));
        sub.addView(subRow);
        progress = Ui.bar(this, 0, Ui.ACCENT);
        progress.setMax(session.size());
        sub.addView(progress);
        progressText = Ui.text(this, "", 12, Ui.MUTED, false);
        sub.addView(progressText);
        root.addView(sub);

        scroll = new ScrollView(this);
        questionBox = Ui.vertical(this);
        int q = Ui.dp(this, 14);
        questionBox.setPadding(q, q, q, Ui.dp(this, 24));
        scroll.addView(questionBox);
        root.addView(scroll, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        LinearLayout bottom = Ui.vertical(this);
        bottom.setPadding(Ui.dp(this, 6), 0, Ui.dp(this, 6), Ui.dp(this, 4));
        LinearLayout row1 = Ui.horizontal(this);
        Button endBtn = Ui.button(this, "Termina", v -> confirmEnd());
        endBtn.setTextColor(Ui.KO);
        row1.addView(endBtn, Ui.weight(1));
        Button reviewBtn = Ui.button(this, "Riepilogo", v -> openItemReview());
        row1.addView(reviewBtn, Ui.weight(1));
        revealBtn = Ui.button(this, "Risposta", v -> revealCurrent());
        if (session.getMode() == ExamMode.STUDY) {
            row1.addView(revealBtn, Ui.weight(1));
        }
        LinearLayout row2 = Ui.horizontal(this);
        prevBtn = Ui.button(this, "◀ Precedente", v -> show(session.getCurrent() - 1));
        nextBtn = Ui.button(this, "Successiva ▶", v -> next());
        row2.addView(prevBtn, Ui.weight(1));
        row2.addView(nextBtn, Ui.weight(1));
        bottom.addView(row1);
        bottom.addView(row2);
        root.addView(bottom);

        if (!session.isAllowBack()) {
            prevBtn.setVisibility(android.view.View.GONE);
            reviewBtn.setVisibility(android.view.View.GONE);
        }
        return root;
    }

    // ------------------------------------------------------------------ navigazione

    private void show(int index) {
        if (index < 0 || index >= session.size()) {
            return;
        }
        session.setCurrent(index);
        Question q = session.getQuestion(index);
        boolean revealed = session.isRevealed(index);
        QuestionRenderer.render(this, questionBox, q, session.getResponse(index), !revealed, revealed,
            this::refreshStatus);
        markBox.setOnCheckedChangeListener(null);
        markBox.setChecked(session.isMarked(index));
        markBox.setOnCheckedChangeListener((b, v) -> session.setMarked(session.getCurrent(), v));
        counter.setText("Domanda " + (index + 1) + " di " + session.size());
        prevBtn.setEnabled(index > 0);
        nextBtn.setText(index == session.size() - 1 ? "Fine ▶" : "Successiva ▶");
        revealBtn.setEnabled(!revealed);
        scroll.post(() -> scroll.scrollTo(0, 0));
        refreshStatus();
    }

    private void next() {
        int cur = session.getCurrent();
        if (!session.isAllowBack() && !session.isAnswered(cur)) {
            new AlertDialog.Builder(this)
                .setTitle("Domanda senza risposta")
                .setMessage("Non hai risposto completamente.\nIn questa modalità non potrai tornare indietro. Proseguire?")
                .setPositiveButton("Prosegui", (d, w) -> advance(cur))
                .setNegativeButton("Resta", null)
                .show();
        } else {
            advance(cur);
        }
    }

    private void advance(int cur) {
        if (cur == session.size() - 1) {
            confirmEnd();
        } else {
            show(cur + 1);
        }
    }

    private void revealCurrent() {
        int cur = session.getCurrent();
        if (session.getMode() != ExamMode.STUDY || session.isRevealed(cur)) {
            return;
        }
        session.setRevealed(cur, true);
        show(cur);
    }

    private void refreshStatus() {
        int answered = session.countAnswered();
        progress.setProgress(answered);
        progressText.setText(answered + " / " + session.size() + " risposte");
        Question q = session.getQuestion(session.getCurrent());
        String s = q.getType().getLabel();
        if (session.getMode() == ExamMode.STUDY) {
            s = q.getDomain().getCode() + " " + q.getDomain().getTitle() + "\n" + s;
        }
        info.setText(s);
    }

    private void openItemReview() {
        String[] items = new String[session.size()];
        for (int i = 0; i < session.size(); i++) {
            items[i] = (i + 1) + "  ·  " + (session.isAnswered(i) ? "risposta data" : "INCOMPLETA")
                + (session.isMarked(i) ? "  ·  ⚑ marcata" : "");
        }
        new AlertDialog.Builder(this)
            .setTitle("Riepilogo: " + session.countAnswered() + "/" + session.size() + " risposte, "
                + session.countMarked() + " marcate")
            .setItems(items, (d, which) -> show(which))
            .setNeutralButton("Termina esame", (d, w) -> confirmEnd())
            .setNegativeButton("Chiudi", null)
            .show();
    }

    // ------------------------------------------------------------------ tempo e consegna

    private void tick() {
        app.syncClock();
        if (session.isTimed()) {
            int rem = session.getRemainingSeconds();
            timer.setText("⏱ " + Text.formatDuration(rem));
            timer.setTextColor(rem <= 300 ? 0xFFFFB4A9 : 0xFFFFFFFF);
            if (rem == 0 && !finished) {
                finished = true;
                new AlertDialog.Builder(this)
                    .setTitle("Tempo scaduto")
                    .setMessage("L'esame viene consegnato.")
                    .setCancelable(false)
                    .setPositiveButton("Vedi risultati", (d, w) -> deliver())
                    .show();
            }
        } else {
            timer.setText("⏱ " + Text.formatDuration(session.getElapsedSeconds()));
        }
    }

    private void confirmEnd() {
        int unanswered = session.size() - session.countAnswered();
        String msg = "Vuoi terminare e consegnare l'esame?";
        if (unanswered > 0) {
            msg = "Ci sono ancora " + unanswered + " domande senza risposta completa.\n" + msg;
        }
        if (session.countMarked() > 0) {
            msg = "Hai " + session.countMarked() + " domande marcate per revisione.\n" + msg;
        }
        new AlertDialog.Builder(this)
            .setTitle("Termina esame")
            .setMessage(msg)
            .setPositiveButton("Consegna", (d, w) -> {
                finished = true;
                deliver();
            })
            .setNegativeButton("Annulla", null)
            .show();
    }

    /** Corregge, salva e mostra il report. */
    private void deliver() {
        handler.removeCallbacks(ticker);
        app.syncClock();
        ExamResult result = Scorer.score(session);
        app.getStore().recordResult(result);
        app.setLastResult(result);
        app.clearSession();
        startActivity(new Intent(this, ResultsActivity.class));
        finish();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        new AlertDialog.Builder(this)
            .setTitle("Abbandonare l'esame?")
            .setMessage("Le risposte date andranno perse.")
            .setPositiveButton("Abbandona", (d, w) -> {
                finished = true;
                app.clearSession();
                finish();
            })
            .setNegativeButton("Continua l'esame", null)
            .show();
    }
}
