package com.lordoc.android;

import android.app.Application;
import android.content.Context;
import android.os.SystemClock;

import com.lordoc.data.QuestionBank;
import com.lordoc.data.UserDataStore;
import com.lordoc.engine.ExamFactory;
import com.lordoc.model.ExamResult;
import com.lordoc.model.ExamSession;

/**
 * Oggetto unico dell'applicazione: carica domande e dati utente all'avvio e conserva
 * l'esame in corso e l'ultimo risultato mentre si passa da una schermata all'altra.
 */
public final class LordocApp extends Application {

    public static final String VERSION = "1.0.0";

    private UserDataStore store;
    private QuestionBank bank;
    private ExamFactory factory;
    private ExamSession currentSession;
    private long sessionStartRealtime;
    private ExamResult lastResult;

    @Override
    public void onCreate() {
        super.onCreate();
        store = new UserDataStore(getFilesDir().toPath());
        store.load();
        reloadQuestions();
    }

    /** Ricarica la banca domande (all'avvio e dopo un'importazione). */
    public void reloadQuestions() {
        bank = QuestionBank.load(path -> getAssets().open(path), store.getUserQuestionsDirectory());
        factory = new ExamFactory(bank, store);
    }

    public static LordocApp get(Context c) {
        return (LordocApp) c.getApplicationContext();
    }

    public UserDataStore getStore() { return store; }
    public QuestionBank getBank() { return bank; }
    public ExamFactory getFactory() { return factory; }
    public ExamSession getCurrentSession() { return currentSession; }
    public ExamResult getLastResult() { return lastResult; }
    public void setLastResult(ExamResult r) { lastResult = r; }

    /** Avvia un nuovo esame e fa partire il cronometro. */
    public void startSession(ExamSession s) {
        currentSession = s;
        sessionStartRealtime = SystemClock.elapsedRealtime();
    }

    public void clearSession() {
        currentSession = null;
    }

    /**
     * Allinea il tempo trascorso dell'esame all'orologio reale: il tempo continua a scorrere
     * anche se l'app va in secondo piano, come in un esame vero.
     */
    public void syncClock() {
        if (currentSession == null) {
            return;
        }
        long seconds = (SystemClock.elapsedRealtime() - sessionStartRealtime) / 1000;
        while (currentSession.getElapsedSeconds() < seconds) {
            currentSession.tick();
        }
    }
}
