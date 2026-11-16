package com.lordoc.android;

import android.app.Activity;
import android.graphics.Insets;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.ads.AdView;

/**
 * Base comune a tutte le schermate. Disegna la barra del titolo (al posto della ActionBar di
 * sistema), un'area in basso per il banner pubblicitario e gestisce gli "insets": da Android 15
 * l'app occupa tutto lo schermo (edge-to-edge) e deve lasciare spazio da sola alla barra di stato,
 * alla barra di navigazione, al notch e alla tastiera.
 */
abstract class BaseActivity extends Activity {

    private TextView titleView;
    private LinearLayout header;
    private FrameLayout bottomSlot;
    private AdView banner;

    /** true per le schermate che mostrano un banner pubblicitario in basso (Menu, Storico, Report). */
    protected boolean showBanner() {
        return false;
    }

    /** true per le schermate secondarie: mostra la freccia "indietro" nella barra. */
    protected boolean showBackArrow() {
        return true;
    }

    /** Cosa fa la freccia nella barra del titolo; ExamActivity chiede conferma. */
    protected void onHeaderBack() {
        finish();
    }

    /** Area in fondo allo schermo dove {@link Ads} può inserire un banner. */
    protected FrameLayout getBottomSlot() {
        return bottomSlot;
    }

    @Override
    public void setContentView(View content) {
        LinearLayout root = Ui.vertical(this);
        root.setBackgroundColor(Ui.BACKGROUND);

        header = Ui.horizontal(this);
        header.setBackgroundColor(Ui.HEADER);
        int p = Ui.dp(this, 12);
        header.setPadding(p, p, p, p);
        if (showBackArrow()) {
            TextView back = Ui.text(this, "←", 22, 0xFFFFFFFF, true);
            back.setContentDescription("Indietro");
            back.setPadding(0, 0, Ui.dp(this, 14), 0);
            back.setOnClickListener(v -> onHeaderBack());
            header.addView(back);
        }
        titleView = Ui.text(this, String.valueOf(getTitle()), 19, 0xFFFFFFFF, true);
        titleView.setSingleLine(true);
        titleView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        header.addView(titleView, Ui.weight(1));
        root.addView(header, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        root.addView(content, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        bottomSlot = new FrameLayout(this);
        root.addView(bottomSlot, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        bottomSlot.setForegroundGravity(Gravity.CENTER_HORIZONTAL);

        applyInsets(root);
        super.setContentView(root);
        refreshBanner();
    }

    /** (Ri)crea il banner, ad esempio dopo che l'utente ha dato il consenso. */
    protected void refreshBanner() {
        if (showBanner() && banner == null) {
            banner = Ads.attachBanner(this);
        }
    }

    @Override
    protected void onPause() {
        if (banner != null) {
            banner.pause();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (banner != null) {
            banner.resume();
        }
    }

    @Override
    protected void onDestroy() {
        if (banner != null) {
            banner.destroy();
            banner = null;
        }
        super.onDestroy();
    }

    @Override
    protected void onTitleChanged(CharSequence title, int color) {
        super.onTitleChanged(title, color);
        if (titleView != null) {
            titleView.setText(title);
        }
    }

    /**
     * Da Android 15 (API 35) le app che puntano ad Android 15/16 sono sempre edge-to-edge:
     * spostiamo la barra del titolo sotto la barra di stato e lasciamo in fondo lo spazio per la
     * barra di navigazione o per la tastiera (che non ridimensiona più la finestra da sola).
     * Sulle versioni precedenti è il sistema a farlo, quindi non serve nulla.
     */
    private void applyInsets(LinearLayout root) {
        if (Build.VERSION.SDK_INT < 35) {
            return;
        }
        final int headerPad = header.getPaddingTop();
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            Insets ime = insets.getInsets(WindowInsets.Type.ime());
            header.setPadding(headerPad + bars.left, headerPad + bars.top, headerPad + bars.right, headerPad);
            v.setPadding(bars.left, 0, bars.right, Math.max(bars.bottom, ime.bottom));
            return WindowInsets.CONSUMED;
        });
    }
}
