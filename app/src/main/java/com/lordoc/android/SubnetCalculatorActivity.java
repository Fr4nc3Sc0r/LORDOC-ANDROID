package com.lordoc.android;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.lordoc.util.IpUtils;

/** Calcolatore IPv4: rete, broadcast, range host, maschera, wildcard, magic number, binario. */
public final class SubnetCalculatorActivity extends Activity {

    private EditText input;
    private TextView error;
    private LinearLayout results;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Calcolatore subnet");
        LinearLayout root = Ui.vertical(this);
        setContentView(Ui.page(this, root));

        root.addView(Ui.text(this, "Indirizzo (es. 10.1.2.3/20 oppure 10.1.2.3 255.255.240.0)", 14, Ui.TEXT, true));
        input = new EditText(this);
        input.setText("192.168.10.77/26");
        input.setSingleLine(true);
        input.setTypeface(Typeface.MONOSPACE);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);
        input.setOnEditorActionListener((v, a, e) -> {
            calculate();
            return false;
        });
        root.addView(input, Ui.full(this, 4));
        root.addView(Ui.button(this, "Calcola", v -> calculate()), Ui.full(this, 4));
        error = Ui.text(this, "", 14, Ui.KO, true);
        root.addView(error, Ui.full(this, 6));
        results = Ui.card(this);
        root.addView(results, Ui.full(this, 8));
        root.addView(Ui.text(this, "All'esame non hai calcolatrici: usa questo strumento solo per verificare i "
            + "calcoli fatti a mano con il metodo del magic number.", 13, Ui.MUTED, false));
        calculate();
    }

    private void calculate() {
        results.removeAllViews();
        error.setText("");
        try {
            String s = input.getText().toString().trim();
            String ipPart;
            int prefix;
            if (s.contains("/")) {
                String[] p = s.split("/");
                ipPart = p[0];
                prefix = Integer.parseInt(p[1].trim());
            } else {
                String[] p = s.split("\\s+");
                if (p.length != 2) {
                    throw new IllegalArgumentException("Specifica prefisso (/24) o maschera");
                }
                ipPart = p[0];
                prefix = IpUtils.prefixFromMask(p[1]);
            }
            long ip = IpUtils.toLong(ipPart);
            long net = IpUtils.network(ip, prefix);
            long bc = IpUtils.broadcast(ip, prefix);
            add("Indirizzo", IpUtils.toIp(ip));
            add("Prefisso / maschera", "/" + prefix + " = " + IpUtils.maskString(prefix));
            add("Wildcard mask", IpUtils.wildcardString(prefix));
            add("Indirizzo di rete", IpUtils.toIp(net));
            add("Broadcast", IpUtils.toIp(bc));
            if (prefix <= 30) {
                add("Primo host", IpUtils.toIp(net + 1));
                add("Ultimo host", IpUtils.toIp(bc - 1));
            } else if (prefix == 31) {
                add("Host (RFC 3021)", IpUtils.toIp(net) + " – " + IpUtils.toIp(bc));
            }
            add("Host utilizzabili", String.valueOf(IpUtils.usableHosts(prefix)));
            add("Indirizzi totali", String.valueOf(1L << (32 - prefix)));
            if (prefix > 0 && prefix % 8 != 0) {
                int oct = (prefix - 1) / 8;
                int maskOct = (int) ((IpUtils.mask(prefix) >> (24 - 8 * oct)) & 255);
                add("Magic number", (256 - maskOct) + " nel " + (oct + 1) + "° ottetto");
            }
            add("Classe (classful)", String.valueOf(IpUtils.ipClass(ip)));
            add("Privato RFC 1918", IpUtils.isPrivate(ip) ? "sì" : "no");
            add("Binario indirizzo", IpUtils.toBinary(ip));
            add("Binario maschera", IpUtils.toBinary(IpUtils.mask(prefix)));
        } catch (RuntimeException ex) {
            error.setText(ex.getMessage() == null ? "Input non valido" : ex.getMessage());
        }
    }

    private void add(String label, String value) {
        results.addView(Ui.text(this, label, 13, Ui.MUTED, true));
        TextView v = Ui.text(this, value, 16, Ui.TEXT, false);
        v.setTypeface(Typeface.MONOSPACE);
        v.setTextIsSelectable(true);
        results.addView(v, Ui.full(this, 8));
    }
}
