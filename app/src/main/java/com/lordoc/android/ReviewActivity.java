package com.lordoc.android;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.lordoc.model.ExamResult;
import com.lordoc.model.ExamSession;

/** Revisione dopo l'esame: domanda, risposta data, risposta corretta e spiegazione. */
public final class ReviewActivity extends Activity {

    static final String EXTRA_INDEXES = "indexes";
    static final String EXTRA_START = "start";

    private ExamSession session;
    private int[] indexes;
    private int pos;
    private TextView counter;
    private LinearLayout box;
    private ScrollView scroll;
    private Button prev;
    private Button next;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ExamResult result = LordocApp.get(this).getLastResult();
        indexes = getIntent().getIntArrayExtra(EXTRA_INDEXES);
        if (result == null || result.getSession() == null || indexes == null || indexes.length == 0) {
            finish();
            return;
        }
        session = result.getSession();
        pos = savedInstanceState != null ? savedInstanceState.getInt(EXTRA_START)
            : getIntent().getIntExtra(EXTRA_START, 0);
        setTitle("Revisione");

        LinearLayout root = Ui.vertical(this);
        counter = Ui.text(this, "", 15, 0xFFFFFFFF, true);
        counter.setBackgroundColor(Ui.HEADER);
        int p = Ui.dp(this, 12);
        counter.setPadding(p, Ui.dp(this, 8), p, Ui.dp(this, 8));
        root.addView(counter);
        scroll = new ScrollView(this);
        box = Ui.vertical(this);
        box.setPadding(p, p, p, Ui.dp(this, 24));
        scroll.addView(box);
        root.addView(scroll, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
        LinearLayout nav = Ui.horizontal(this);
        prev = Ui.button(this, "◀ Precedente", v -> show(pos - 1));
        next = Ui.button(this, "Successiva ▶", v -> show(pos + 1));
        nav.addView(prev, Ui.weight(1));
        nav.addView(next, Ui.weight(1));
        root.addView(nav);
        setContentView(root);
        show(pos);
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt(EXTRA_START, pos);
    }

    private void show(int p) {
        if (p < 0 || p >= indexes.length) {
            return;
        }
        pos = p;
        int idx = indexes[p];
        QuestionRenderer.render(this, box, session.getQuestion(idx), session.getResponse(idx), false, true, () -> { });
        counter.setText("Domanda " + (idx + 1) + " dell'esame · " + (p + 1) + " di " + indexes.length
            + (session.isMarked(idx) ? " · ⚑ marcata" : ""));
        prev.setEnabled(p > 0);
        next.setEnabled(p < indexes.length - 1);
        scroll.post(() -> scroll.scrollTo(0, 0));
    }
}
