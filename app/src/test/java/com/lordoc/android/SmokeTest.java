package com.lordoc.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;

import com.lordoc.model.ExamMode;
import com.lordoc.model.ExamResult;
import com.lordoc.model.ExamSession;
import com.lordoc.model.Question;
import com.lordoc.model.QuestionType;
import com.lordoc.model.Response;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowDialog;
import org.robolectric.shadows.ShadowLooper;

import java.util.ArrayList;
import java.util.List;

/**
 * Test di prova: avviano ogni schermata con il vero codice di Android (tramite Robolectric)
 * e verificano che le domande si carichino e che un esame si possa svolgere e consegnare.
 */
@RunWith(RobolectricTestRunner.class)
public class SmokeTest {

    private LordocApp app() {
        return (LordocApp) ApplicationProvider.getApplicationContext();
    }

    @Test
    public void laBancaDomandeSiCaricaSenzaErrori() {
        assertTrue("domande caricate: " + app().getBank().size(), app().getBank().size() >= 290);
        assertTrue(app().getBank().getErrors().toString(), app().getBank().getErrors().isEmpty());
        assertEquals(4, app().getFactory().presetExams().size());
    }

    @Test
    public void ogniDomandaSiDisegnaInTutteLeModalita() {
        LinearLayout box = new LinearLayout(app());
        for (Question q : app().getBank().all()) {
            Response r = new Response();
            QuestionRenderer.render(app(), box, q, r, true, false, () -> { });
            QuestionRenderer.render(app(), box, q.shuffled(new java.util.Random(1)), r, false, true, () -> { });
            assertTrue(q.getId(), box.getChildCount() > 0);
        }
    }

    @Test
    public void menuPrincipaleEStrumenti() {
        assertNotNull(Robolectric.buildActivity(MainActivity.class).setup().get());
        assertNotNull(Robolectric.buildActivity(CustomExamActivity.class).setup().get());
        assertNotNull(Robolectric.buildActivity(HistoryActivity.class).setup().get());
        assertNotNull(Robolectric.buildActivity(ReferenceActivity.class).setup().get());
        Intent page = new Intent(app(), ReferencePageActivity.class)
            .putExtra(ReferencePageActivity.EXTRA_FILE, "subnetting.html")
            .putExtra(ReferencePageActivity.EXTRA_TITLE, "Subnetting");
        assertNotNull(Robolectric.buildActivity(ReferencePageActivity.class, page).setup().get());
        SubnetCalculatorActivity calc = Robolectric.buildActivity(SubnetCalculatorActivity.class).setup().get();
        assertTrue(textOf(calc.getWindow().getDecorView()).contains("192.168.10.64"));
    }

    @Test
    public void esameStudioCompletoFinoAlReport() {
        ExamSession s = app().getFactory().fromPreset(app().getFactory().presetExams().get(0), ExamMode.STUDY, true);
        // rispondiamo correttamente alla prima metà delle domande
        for (int i = 0; i < s.size() / 2; i++) {
            answerCorrectly(s.getQuestion(i), s.getResponse(i));
        }
        app().startSession(s);
        ActivityController<ExamActivity> c = Robolectric.buildActivity(ExamActivity.class).setup();
        ExamActivity exam = c.get();

        // mostra la risposta della prima domanda e scorre tutte le domande con "Successiva"
        click(exam, "Risposta");
        for (int i = 0; i < s.size() - 1; i++) {
            click(exam, "Successiva ▶");
        }
        click(exam, "Fine ▶");
        AlertDialog dialog = (AlertDialog) ShadowDialog.getLatestDialog();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
        ShadowLooper.idleMainLooper();

        ExamResult result = app().getLastResult();
        assertNotNull(result);
        assertEquals(s.size() / 2, result.getCorrect());
        assertEquals(ResultsActivity.class.getName(),
            shadowOf(exam).getNextStartedActivity().getComponent().getClassName());
        assertEquals(1, app().getStore().getHistory().size());

        ResultsActivity res = Robolectric.buildActivity(ResultsActivity.class).setup().get();
        assertTrue(textOf(res.getWindow().getDecorView()).contains(String.valueOf(result.getScaledScore())));

        Intent review = new Intent(app(), ReviewActivity.class)
            .putExtra(ReviewActivity.EXTRA_INDEXES, new int[] {0, 1, 2})
            .putExtra(ReviewActivity.EXTRA_START, 0);
        assertNotNull(Robolectric.buildActivity(ReviewActivity.class, review).setup().get());
        assertNotNull(Robolectric.buildActivity(HistoryActivity.class).setup().get());
    }

    @Test
    public void simulazioneATempoSiConsegnaAllaScadenza() {
        ExamSession s = new ExamSession("Prova", ExamMode.SIMULATION,
            app().getBank().byDomain(com.lordoc.model.Domain.IP_SERVICES).subList(0, 3), 2, false);
        app().startSession(s);
        ActivityController<ExamActivity> c = Robolectric.buildActivity(ExamActivity.class).setup();
        // fa avanzare l'orologio simulato di 3 secondi (il limite è 2)
        shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(3));
        AlertDialog dialog = (AlertDialog) ShadowDialog.getLatestDialog();
        assertNotNull("deve comparire l'avviso di tempo scaduto (trascorsi: " + s.getElapsedSeconds() + " s)", dialog);
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
        ShadowLooper.idleMainLooper();
        assertNotNull(app().getLastResult());
        assertEquals(0, app().getLastResult().getCorrect());
        assertEquals(ResultsActivity.class.getName(),
            shadowOf(c.get()).getNextStartedActivity().getComponent().getClassName());
        c.pause().stop().destroy();
    }

    // ------------------------------------------------------------------ utilità

    private static void answerCorrectly(Question q, Response r) {
        if (q.getType() == QuestionType.SINGLE || q.getType() == QuestionType.MULTI) {
            for (int i : q.getCorrectOptions()) {
                r.select(i, q.getType() == QuestionType.SINGLE);
            }
        } else if (q.getType() == QuestionType.FILL) {
            r.setFillText(q.getAcceptedAnswers().get(0));
        } else {
            for (int i = 0; i < q.getDragItems().size(); i++) {
                r.setDragAnswer(i, q.getDragSolution().get(i));
            }
        }
    }

    private static void click(android.app.Activity a, String label) {
        List<Button> found = new ArrayList<>();
        collect(a.getWindow().getDecorView(), label, found);
        assertTrue("pulsante non trovato: " + label, !found.isEmpty());
        found.get(0).performClick();
        ShadowLooper.idleMainLooper();
    }

    private static void collect(View v, String label, List<Button> out) {
        if (v instanceof Button && label.contentEquals(((Button) v).getText()) && v.isShown()) {
            out.add((Button) v);
        }
        if (v instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) v).getChildCount(); i++) {
                collect(((ViewGroup) v).getChildAt(i), label, out);
            }
        }
    }

    private static String textOf(View v) {
        StringBuilder sb = new StringBuilder();
        if (v instanceof TextView) {
            sb.append(((TextView) v).getText()).append('\n');
        }
        if (v instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) v).getChildCount(); i++) {
                sb.append(textOf(((ViewGroup) v).getChildAt(i)));
            }
        }
        return sb.toString();
    }
}
