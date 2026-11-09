package com.lordoc.android;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import com.lordoc.model.Question;
import com.lordoc.model.QuestionType;
import com.lordoc.model.Response;

import java.util.ArrayList;
import java.util.List;

/**
 * Disegna una domanda dentro un contenitore: testo, exhibit, area di risposta adatta al tipo
 * e, in revisione, il riquadro con correzione e spiegazione.
 */
final class QuestionRenderer {

    private QuestionRenderer() { }

    /**
     * @param editable se l'utente può modificare la risposta
     * @param reveal   se mostrare correzione e spiegazione
     * @param onChange azione da eseguire a ogni modifica della risposta
     */
    static void render(Context c, LinearLayout box, Question q, Response r,
                       boolean editable, boolean reveal, Runnable onChange) {
        box.removeAllViews();

        TextView text = Ui.text(c, q.getDisplayText(), 17, Ui.TEXT, false);
        text.setLineSpacing(0, 1.15f);
        box.addView(text, Ui.full(c, 10));

        if (q.hasExhibit()) {
            box.addView(Ui.text(c, "Exhibit", 13, Ui.ACCENT, true), Ui.full(c, 4));
            box.addView(exhibit(c, q.getExhibit()), Ui.full(c, 12));
        }

        switch (q.getType()) {
            case SINGLE:
            case MULTI:
                buildChoices(c, box, q, r, editable, reveal, onChange);
                break;
            case DRAG:
                buildDrag(c, box, q, r, editable, reveal, onChange);
                break;
            case FILL:
                buildFill(c, box, q, r, editable, reveal, onChange);
                break;
            default:
                break;
        }

        if (reveal) {
            box.addView(feedback(c, q, r), Ui.full(c, 12));
        }
    }

    // ------------------------------------------------------------------ exhibit

    private static View exhibit(Context c, String s) {
        HorizontalScrollView hs = new HorizontalScrollView(c);
        hs.setBackground(Ui.rounded(c, Ui.EXHIBIT_BG));
        TextView t = Ui.text(c, s, 12.5f, Ui.EXHIBIT_FG, false);
        t.setTypeface(Typeface.MONOSPACE);
        int p = Ui.dp(c, 10);
        t.setPadding(p, p, p, p);
        hs.addView(t);
        return hs;
    }

    // ------------------------------------------------------------------ scelta singola / multipla

    private static void buildChoices(Context c, LinearLayout box, Question q, Response r,
                                     boolean editable, boolean reveal, Runnable onChange) {
        boolean single = q.getType() == QuestionType.SINGLE;
        LinearLayout group = single ? new RadioGroup(c) : Ui.vertical(c);
        List<String> opts = q.getOptions();
        for (int i = 0; i < opts.size(); i++) {
            final int idx = i;
            CompoundButton btn = single ? new RadioButton(c) : new CheckBox(c);
            btn.setId(View.generateViewId());
            String label = Question.letter(i) + ".  " + opts.get(i);
            boolean correct = q.getCorrectOptions().contains(i);
            boolean chosen = r.getSelected().contains(i);
            if (reveal && correct) {
                label += chosen ? "   ✔ corretta (scelta)" : "   ✔ corretta";
            } else if (reveal && chosen) {
                label += "   ✘ scelta errata";
            }
            btn.setText(label);
            btn.setTextSize(16);
            btn.setTextColor(Ui.TEXT);
            int p = Ui.dp(c, 6);
            btn.setPadding(p, p, p, p);
            group.addView(btn, Ui.full(c, 6));
            btn.setChecked(chosen);
            btn.setEnabled(editable);
            if (reveal && correct) {
                btn.setBackground(Ui.rounded(c, Ui.OK_BG));
            } else if (reveal && chosen) {
                btn.setBackground(Ui.rounded(c, Ui.KO_BG));
            }
            btn.setOnCheckedChangeListener((b, checked) -> {
                if (checked) {
                    r.select(idx, single);
                } else {
                    r.deselect(idx);
                }
                onChange.run();
            });
        }
        box.addView(group, Ui.full(c, 6));
        if (!single && editable) {
            box.addView(Ui.text(c, "Seleziona " + q.requiredSelections() + " risposte.", 13, Ui.MUTED, false),
                Ui.full(c, 8));
        }
    }

    // ------------------------------------------------------------------ abbinamento (drag and drop)

    private static void buildDrag(Context c, LinearLayout box, Question q, Response r,
                                  boolean editable, boolean reveal, Runnable onChange) {
        box.addView(Ui.text(c, "Abbina ogni elemento alla categoria corretta.", 13, Ui.MUTED, false),
            Ui.full(c, 8));
        List<String> labels = new ArrayList<>();
        labels.add("— scegli —");
        labels.addAll(q.getDragTargets());
        List<String> items = q.getDragItems();
        for (int i = 0; i < items.size(); i++) {
            final int idx = i;
            LinearLayout row = Ui.vertical(c);
            int p = Ui.dp(c, 8);
            row.setPadding(p, p, p, p);
            row.addView(Ui.text(c, items.get(i), 15, Ui.TEXT, true));

            Spinner sp = new Spinner(c);
            ArrayAdapter<String> ad = new ArrayAdapter<>(c, android.R.layout.simple_spinner_item, labels);
            ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            sp.setAdapter(ad);
            String cur = r.getDragAnswer(i);
            int sel = cur == null ? 0 : Math.max(0, labels.indexOf(cur));
            sp.setSelection(sel, false);
            sp.setEnabled(editable);
            sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                    String chosen = pos == 0 ? null : labels.get(pos);
                    String before = r.getDragAnswer(idx);
                    if (chosen == null ? before != null : !chosen.equals(before)) {
                        r.setDragAnswer(idx, chosen);
                        onChange.run();
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) { }
            });
            row.addView(sp);

            if (reveal) {
                String expected = q.getDragSolution().get(i);
                boolean ok = expected.equals(cur);
                row.setBackground(Ui.rounded(c, ok ? Ui.OK_BG : Ui.KO_BG));
                row.addView(Ui.text(c, ok ? "✔ corretto" : "✘ corretto: " + expected, 14,
                    ok ? Ui.OK : Ui.KO, true));
            }
            box.addView(row, Ui.full(c, 8));
        }
    }

    // ------------------------------------------------------------------ risposta aperta

    private static void buildFill(Context c, LinearLayout box, Question q, Response r,
                                  boolean editable, boolean reveal, Runnable onChange) {
        box.addView(Ui.text(c, "Risposta:", 15, Ui.TEXT, true));
        EditText field = new EditText(c);
        field.setSingleLine(true);
        field.setTypeface(Typeface.MONOSPACE);
        field.setTextSize(17);
        field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        field.setText(r.getFillText());
        field.setEnabled(editable);
        field.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int d) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int d) { }

            @Override
            public void afterTextChanged(Editable s) {
                r.setFillText(s.toString());
                onChange.run();
            }
        });
        if (reveal) {
            field.setBackgroundColor(q.isCorrect(r) ? Ui.OK_BG : Ui.KO_BG);
        }
        box.addView(field, Ui.full(c, 10));
    }

    // ------------------------------------------------------------------ correzione

    private static View feedback(Context c, Question q, Response r) {
        boolean ok = q.isCorrect(r);
        LinearLayout p = Ui.vertical(c);
        p.setBackground(Ui.rounded(c, Ui.FEEDBACK_BG));
        int pad = Ui.dp(c, 12);
        p.setPadding(pad, pad, pad, pad);

        String verdict;
        if (ok) {
            verdict = "✔ Risposta corretta";
        } else if (!q.isAnswered(r)) {
            verdict = "✘ Risposta mancante o incompleta";
        } else {
            verdict = "✘ Risposta errata";
        }
        p.addView(Ui.text(c, verdict, 18, ok ? Ui.OK : Ui.KO, true), Ui.full(c, 6));
        p.addView(Ui.text(c, "Risposta corretta: " + q.getCorrectAnswerText(), 15, Ui.TEXT, true), Ui.full(c, 8));
        if (!q.getExplanation().isEmpty()) {
            p.addView(Ui.text(c, "Spiegazione", 15, Ui.ACCENT, true), Ui.full(c, 4));
            TextView ex = Ui.text(c, plain(q.getExplanation()), 15, Ui.TEXT, false);
            ex.setLineSpacing(0, 1.15f);
            p.addView(ex, Ui.full(c, 8));
        }
        if (!q.getReference().isEmpty()) {
            TextView ref = Ui.text(c, "Riferimento: " + q.getReference(), 13, Ui.MUTED, false);
            ref.setTypeface(Typeface.defaultFromStyle(Typeface.ITALIC));
            p.addView(ref, Ui.full(c, 6));
        }
        p.addView(Ui.text(c, "ID " + q.getId() + "  ·  " + q.getDomain().getLabel() + "  ·  "
            + q.getType().getLabel(), 12, Ui.MUTED, false));
        return p;
    }

    /** Toglie la mini-sintassi `codice` e **grassetto** dalla spiegazione. */
    static String plain(String s) {
        return s.replace("`", "").replace("**", "");
    }
}
