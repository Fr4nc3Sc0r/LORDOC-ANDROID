package com.lordoc.android;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Locale;

/** Colori, misure e piccole funzioni per creare i componenti grafici nello stile di Lordoc. */
final class Ui {

    static final int HEADER = 0xFF1F3A5F;
    static final int SUB = 0xFFE8EEF5;
    static final int ACCENT = 0xFF2E6DB4;
    static final int OK = 0xFF2E7D32;
    static final int OK_BG = 0xFFE3F4E4;
    static final int KO = 0xFFC62828;
    static final int KO_BG = 0xFFFBE4E4;
    static final int WARN = 0xFFE69500;
    static final int TEXT = 0xFF1B1B1B;
    static final int MUTED = 0xFF5A6B7D;
    static final int EXHIBIT_BG = 0xFF1E1E1E;
    static final int EXHIBIT_FG = 0xFFE6E6E6;
    static final int FEEDBACK_BG = 0xFFF6F8FB;

    private Ui() { }

    static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    static TextView text(Context c, String s, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) {
            t.setTypeface(Typeface.DEFAULT_BOLD);
        }
        return t;
    }

    /** Titolo di sezione blu con un po' di spazio sopra. */
    static TextView section(Context c, String s) {
        TextView t = text(c, s, 18, HEADER, true);
        t.setPadding(0, dp(c, 18), 0, dp(c, 6));
        return t;
    }

    static Button button(Context c, String s, View.OnClickListener l) {
        Button b = new Button(c);
        b.setText(s);
        b.setAllCaps(false);
        b.setOnClickListener(l);
        return b;
    }

    static LinearLayout vertical(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    static LinearLayout horizontal(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    /** Riquadro bianco con angoli arrotondati e margine interno. */
    static LinearLayout card(Context c) {
        LinearLayout l = vertical(c);
        l.setBackground(rounded(c, 0xFFFFFFFF));
        int p = dp(c, 14);
        l.setPadding(p, p, p, p);
        return l;
    }

    static GradientDrawable rounded(Context c, int color) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(c, 10));
        return g;
    }

    /** Parametri di layout: tutta la larghezza, altezza del contenuto, margine inferiore. */
    static LinearLayout.LayoutParams full(Context c, int bottomMarginDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(c, bottomMarginDp);
        return lp;
    }

    /** Parametri di layout per dividere una riga in parti proporzionali. */
    static LinearLayout.LayoutParams weight(float w) {
        return new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, w);
    }

    /** Pagina scorrevole con margini laterali, usata da quasi tutte le schermate. */
    static ScrollView page(Context c, LinearLayout content) {
        ScrollView sv = new ScrollView(c);
        int p = dp(c, 14);
        content.setPadding(p, p, p, dp(c, 28));
        sv.addView(content);
        return sv;
    }

    /** Barra di avanzamento colorata (0-100). */
    static ProgressBar bar(Context c, double percent, int color) {
        ProgressBar pb = new ProgressBar(c, null, android.R.attr.progressBarStyleHorizontal);
        pb.setMax(100);
        pb.setProgress((int) Math.round(percent));
        pb.setProgressTintList(ColorStateList.valueOf(color));
        return pb;
    }

    /** Verde ≥ 82,5%, arancio ≥ 60%, rosso sotto. */
    static int colorFor(double percent) {
        if (percent >= 82.5) {
            return OK;
        }
        return percent >= 60 ? WARN : KO;
    }

    static String pct(double v) {
        return String.format(Locale.ITALY, "%.0f%%", v);
    }
}
