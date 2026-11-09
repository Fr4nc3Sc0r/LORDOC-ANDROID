package com.lordoc.android;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Mostra una scheda HTML del cheat sheet, adattata allo schermo del telefono. */
public final class ReferencePageActivity extends Activity {

    static final String EXTRA_FILE = "file";
    static final String EXTRA_TITLE = "title";

    /** Stile delle schede: stessi colori dell'app, tabelle scorrevoli in orizzontale. */
    private static final String HEAD = "<meta name='viewport' content='width=device-width, initial-scale=1'>"
        + "<style>"
        + "body{font-family:sans-serif;font-size:15px;margin:14px;color:#1b1b1b;line-height:1.4}"
        + "h1{color:#1F3A5F;font-size:22px}h2{color:#2E6DB4;font-size:18px;margin-top:18px}"
        + "table{border-collapse:collapse;width:100%;display:block;overflow-x:auto;font-size:14px}"
        + "th{background:#1F3A5F;color:#fff;padding:6px;text-align:left}"
        + "td{padding:6px;border-bottom:1px solid #ddd;vertical-align:top}"
        + "code,pre{font-family:monospace;background:#f1f3f6;font-size:13px}"
        + "pre{padding:8px;overflow-x:auto}"
        + ".tip{background:#fff6dc;padding:8px;border-left:4px solid #e69500}"
        + "</style>";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String file = getIntent().getStringExtra(EXTRA_FILE);
        setTitle(getIntent().getStringExtra(EXTRA_TITLE));
        WebView web = new WebView(this);
        setContentView(web);
        String html;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                getAssets().open("reference/" + file), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder(HEAD);
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }
            html = sb.toString();
        } catch (IOException e) {
            html = HEAD + "<p>Pagina non trovata: " + file + "</p>";
        }
        web.loadDataWithBaseURL("file:///android_asset/reference/", html, "text/html", "UTF-8", null);
    }
}
