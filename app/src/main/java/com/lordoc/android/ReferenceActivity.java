package com.lordoc.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Elenco delle schede del cheat sheet (lette da assets/reference/index.txt). */
public final class ReferenceActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Cheat sheet");
        LinearLayout root = Ui.vertical(this);
        setContentView(Ui.page(this, root));
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                getAssets().open("reference/index.txt"), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                int sep = line.indexOf('|');
                if (line.trim().isEmpty() || line.startsWith("#") || sep < 0) {
                    continue;
                }
                String file = line.substring(0, sep).trim();
                String title = line.substring(sep + 1).trim();
                LinearLayout card = Ui.card(this);
                card.setClickable(true);
                card.addView(Ui.text(this, title, 16, Ui.ACCENT, true));
                card.setOnClickListener(v -> {
                    Intent i = new Intent(this, ReferencePageActivity.class);
                    i.putExtra(ReferencePageActivity.EXTRA_FILE, file);
                    i.putExtra(ReferencePageActivity.EXTRA_TITLE, title);
                    startActivity(i);
                });
                root.addView(card, Ui.full(this, 8));
            }
        } catch (IOException e) {
            root.addView(Ui.text(this, "Errore: " + e.getMessage(), 14, Ui.KO, false));
        }
    }
}
