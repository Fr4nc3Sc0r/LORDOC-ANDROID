package com.lordoc.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import com.lordoc.engine.ExamConfig;
import com.lordoc.engine.ExamFactory;
import com.lordoc.engine.QuestionFilter;
import com.lordoc.model.Domain;
import com.lordoc.model.ExamMode;
import com.lordoc.model.ExamSession;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/** Configurazione di un esame personalizzato. */
public final class CustomExamActivity extends Activity {

    private final Map<Domain, CheckBox> domainBoxes = new EnumMap<>(Domain.class);
    private EditText title;
    private RadioButton study;
    private Spinner filter;
    private EditText count;
    private EditText subnet;
    private EditText minutes;
    private CheckBox weighted;
    private CheckBox allowBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Esame personalizzato");
        LinearLayout root = Ui.vertical(this);
        setContentView(Ui.page(this, root));

        root.addView(Ui.text(this, "Titolo", 14, Ui.TEXT, true));
        title = new EditText(this);
        title.setText("Esame personalizzato");
        title.setSingleLine(true);
        root.addView(title, Ui.full(this, 8));

        root.addView(Ui.text(this, "Modalità", 14, Ui.TEXT, true));
        RadioGroup mode = new RadioGroup(this);
        study = new RadioButton(this);
        study.setId(android.view.View.generateViewId());
        study.setText("Studio (senza tempo, con spiegazioni)");
        RadioButton sim = new RadioButton(this);
        sim.setId(android.view.View.generateViewId());
        sim.setText("Simulazione (a tempo)");
        mode.addView(study);
        mode.addView(sim);
        study.setChecked(true);
        mode.setOnCheckedChangeListener((g, id) -> updateMinutes());
        root.addView(mode, Ui.full(this, 8));

        root.addView(Ui.section(this, "Domini"));
        for (Domain d : Domain.values()) {
            CheckBox cb = new CheckBox(this);
            cb.setText(d.getLabel() + " (" + d.getWeight() + "%)");
            cb.setChecked(true);
            domainBoxes.put(d, cb);
            root.addView(cb);
        }
        LinearLayout sel = Ui.horizontal(this);
        sel.addView(Ui.button(this, "Tutti", v -> setAll(true)), Ui.weight(1));
        sel.addView(Ui.button(this, "Nessuno", v -> setAll(false)), Ui.weight(1));
        root.addView(sel, Ui.full(this, 8));

        root.addView(Ui.section(this, "Opzioni"));
        root.addView(Ui.text(this, "Filtro domande", 14, Ui.TEXT, true));
        filter = new Spinner(this);
        ArrayAdapter<QuestionFilter> fa = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
            QuestionFilter.values());
        fa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        filter.setAdapter(fa);
        root.addView(filter, Ui.full(this, 8));

        count = number(root, "Numero di domande", 30);
        subnet = number(root, "Domande di subnetting generate", 0);
        minutes = number(root, "Tempo in minuti (0 = illimitato)", 0);
        TextWatcher w = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) { updateMinutes(); }
        };
        count.addTextChangedListener(w);
        subnet.addTextChangedListener(w);

        weighted = new CheckBox(this);
        weighted.setText("Distribuisci secondo i pesi del blueprint");
        weighted.setChecked(true);
        root.addView(weighted);
        allowBack = new CheckBox(this);
        allowBack.setText("Consenti di tornare indietro");
        allowBack.setChecked(true);
        root.addView(allowBack, Ui.full(this, 12));

        root.addView(Ui.button(this, "Crea esame", v -> confirm()));
    }

    private EditText number(LinearLayout root, String label, int value) {
        root.addView(Ui.text(this, label, 14, Ui.TEXT, true));
        EditText e = new EditText(this);
        e.setInputType(InputType.TYPE_CLASS_NUMBER);
        e.setText(String.valueOf(value));
        root.addView(e, Ui.full(this, 8));
        return e;
    }

    private static int intOf(EditText e, int max) {
        try {
            return Math.max(0, Math.min(max, Integer.parseInt(e.getText().toString().trim())));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private void setAll(boolean v) {
        for (CheckBox cb : domainBoxes.values()) {
            cb.setChecked(v);
        }
    }

    private void updateMinutes() {
        if (minutes == null || study == null) {
            return;
        }
        int m = study.isChecked() ? 0 : ExamFactory.suggestedMinutes(intOf(count, 500) + intOf(subnet, 100));
        minutes.setText(String.valueOf(m));
    }

    private void confirm() {
        Set<Domain> sel = EnumSet.noneOf(Domain.class);
        for (Map.Entry<Domain, CheckBox> e : domainBoxes.entrySet()) {
            if (e.getValue().isChecked()) {
                sel.add(e.getKey());
            }
        }
        int n = intOf(count, 500);
        int sub = intOf(subnet, 100);
        if (sel.isEmpty() && sub == 0) {
            Toast.makeText(this, "Seleziona almeno un dominio o delle domande di subnetting.", Toast.LENGTH_LONG).show();
            return;
        }
        String t = title.getText().toString().trim();
        ExamConfig cfg = new ExamConfig()
            .title(t.isEmpty() ? "Esame personalizzato" : t)
            .mode(study.isChecked() ? ExamMode.STUDY : ExamMode.SIMULATION)
            .domains(sel)
            .filter((QuestionFilter) filter.getSelectedItem())
            .questionCount(n)
            .subnetQuestions(sub)
            .timeMinutes(intOf(minutes, 600))
            .weighted(weighted.isChecked())
            .allowBack(allowBack.isChecked());
        LordocApp app = LordocApp.get(this);
        ExamSession s = app.getFactory().custom(cfg);
        if (s == null) {
            Toast.makeText(this, "Nessuna domanda corrisponde ai criteri scelti.", Toast.LENGTH_LONG).show();
            return;
        }
        app.startSession(s);
        startActivity(new Intent(this, ExamActivity.class));
        finish();
    }
}
