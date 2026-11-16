package com.lordoc.android;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.widget.FrameLayout;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Tutta la pubblicità dell'app in un solo posto (Google AdMob).
 *
 * <ul>
 *   <li><b>Consenso</b>: prima di chiedere annunci si interroga l'UMP (User Messaging Platform) di
 *       Google, che mostra all'utente europeo il messaggio di consenso GDPR configurato nella
 *       console AdMob. Solo se {@code canRequestAds()} è vero si inizializza l'SDK.</li>
 *   <li><b>Banner</b>: adattivo, in fondo a Menu, Storico e Report. Mai durante l'esame.</li>
 *   <li><b>Interstitial</b>: a schermo intero, solo dopo la consegna di un esame e al massimo una
 *       volta ogni {@link #INTERSTITIAL_GAP_MS}.</li>
 * </ul>
 *
 * Gli ID degli annunci arrivano da {@code BuildConfig}: nella build di debug sono gli ID di prova
 * di Google, in quella di release quelli veri (letti da release.properties, che non va su Git).
 */
final class Ads {

    /** Tempo minimo tra due interstitial (3 minuti). */
    static final long INTERSTITIAL_GAP_MS = 3 * 60 * 1000L;

    private static final AtomicBoolean SDK_STARTED = new AtomicBoolean(false);
    private static InterstitialAd interstitial;
    private static boolean interstitialLoading;
    private static long lastInterstitialAt = -INTERSTITIAL_GAP_MS;

    private Ads() { }

    /** Nei test automatici (Robolectric) e se gli ID mancano la pubblicità è spenta. */
    static boolean disabled() {
        return "robolectric".equals(Build.FINGERPRINT) || BuildConfig.ADMOB_BANNER_ID.isEmpty();
    }

    /**
     * Da chiamare all'avvio (MainActivity): aggiorna lo stato del consenso, se serve mostra il
     * modulo e poi avvia l'SDK. {@code onDone} viene eseguito alla fine (per aggiornare il menu).
     */
    static void requestConsentAndStart(Activity a, Runnable onDone) {
        if (disabled()) {
            return;
        }
        ConsentInformation info = UserMessagingPlatform.getConsentInformation(a);
        ConsentRequestParameters params = new ConsentRequestParameters.Builder().build();
        info.requestConsentInfoUpdate(a, params,
            () -> UserMessagingPlatform.loadAndShowConsentFormIfRequired(a, formError -> {
                if (info.canRequestAds()) {
                    startSdk(a);
                }
                onDone.run();
            }),
            requestError -> onDone.run());
        // Se il consenso era già stato dato in una sessione precedente si parte subito.
        if (info.canRequestAds()) {
            startSdk(a);
        }
    }

    private static void startSdk(Context c) {
        if (SDK_STARTED.getAndSet(true)) {
            return;
        }
        Context app = c.getApplicationContext();
        new Thread(() -> MobileAds.initialize(app, status -> { }), "ads-init").start();
    }

    static boolean canRequestAds(Context c) {
        return !disabled() && UserMessagingPlatform.getConsentInformation(c).canRequestAds();
    }

    /** true se l'utente deve poter rivedere le scelte privacy (utenti UE/UK). */
    static boolean privacyOptionsRequired(Context c) {
        return !disabled() && UserMessagingPlatform.getConsentInformation(c).getPrivacyOptionsRequirementStatus()
            == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED;
    }

    /** Riapre il modulo del consenso (voce "Preferenze privacy e pubblicità"). */
    static void showPrivacyOptions(Activity a, Runnable onDone) {
        UserMessagingPlatform.showPrivacyOptionsForm(a, formError -> {
            if (canRequestAds(a)) {
                startSdk(a);
            }
            onDone.run();
        });
    }

    // ------------------------------------------------------------------ banner

    /** Inserisce un banner adattivo nell'area in basso della schermata; restituisce il banner o null. */
    static AdView attachBanner(BaseActivity a) {
        FrameLayout slot = a.getBottomSlot();
        if (slot == null || !canRequestAds(a)) {
            return null;
        }
        startSdk(a);
        DisplayMetrics dm = a.getResources().getDisplayMetrics();
        int widthDp = (int) (dm.widthPixels / dm.density);
        AdView view = new AdView(a);
        view.setAdUnitId(BuildConfig.ADMOB_BANNER_ID);
        view.setAdSize(AdSize.getLargeAnchoredAdaptiveBannerAdSize(a, widthDp));
        slot.removeAllViews();
        slot.addView(view, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
            android.view.Gravity.CENTER_HORIZONTAL));
        view.loadAd(new AdRequest.Builder().build());
        return view;
    }

    // ------------------------------------------------------------------ interstitial

    /** Prepara in anticipo l'interstitial (chiamato all'inizio di un esame). */
    static void preloadInterstitial(Context c) {
        if (interstitial != null || interstitialLoading || !canRequestAds(c)) {
            return;
        }
        startSdk(c);
        interstitialLoading = true;
        InterstitialAd.load(c.getApplicationContext(), BuildConfig.ADMOB_INTERSTITIAL_ID,
            new AdRequest.Builder().build(), new InterstitialAdLoadCallback() {
                @Override
                public void onAdLoaded(InterstitialAd ad) {
                    interstitial = ad;
                    interstitialLoading = false;
                }

                @Override
                public void onAdFailedToLoad(LoadAdError error) {
                    interstitialLoading = false;
                }
            });
    }

    /** Mostra l'interstitial se è pronto e se è passato abbastanza tempo dall'ultimo. */
    static void maybeShowInterstitial(Activity a) {
        long now = SystemClock.elapsedRealtime();
        if (interstitial == null || now - lastInterstitialAt < INTERSTITIAL_GAP_MS) {
            return;
        }
        InterstitialAd ad = interstitial;
        interstitial = null;
        lastInterstitialAt = now;
        ad.setFullScreenContentCallback(new FullScreenContentCallback() { });
        ad.show(a);
    }
}
