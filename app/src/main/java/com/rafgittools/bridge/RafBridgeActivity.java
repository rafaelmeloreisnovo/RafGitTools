package com.rafgittools.bridge;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** Plain Java control panel: direct controls, no DI and no UI abstraction layer. */
public final class RafBridgeActivity extends Activity {
    private EditText endpointInput;
    private EditText modelInput;
    private CheckBox sensitiveCheck;
    private TextView tokenView;
    private TextView statusView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Raf Bridge");
        setContentView(buildUi());
        refreshState();
    }

    private ScrollView buildUi() {
        int pad = dp(20);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);

        TextView title = text("Kiwi ↔ RafGitTools ↔ llamaRafaelia", 22, true);
        root.addView(title);
        root.addView(text(
                "Ponte local em Java direto. Escuta somente 127.0.0.1, exige token e aceita apenas conversa. "
                        + "Não executa shell, commit, push, arquivos ou ações ocultas.",
                15,
                false
        ));

        root.addView(space());
        root.addView(text("Endpoint local do modelo", 15, true));
        endpointInput = new EditText(this);
        endpointInput.setSingleLine(true);
        endpointInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        root.addView(endpointInput, matchWrap());

        root.addView(text("Nome do modelo", 15, true));
        modelInput = new EditText(this);
        modelInput.setSingleLine(true);
        root.addView(modelInput, matchWrap());

        sensitiveCheck = new CheckBox(this);
        sensitiveCheck.setText("Permitir conteúdo marcado como sensível");
        root.addView(sensitiveCheck, matchWrap());

        root.addView(space());
        root.addView(text("Token de pareamento local", 15, true));
        tokenView = text("", 13, false);
        tokenView.setTypeface(Typeface.MONOSPACE);
        tokenView.setTextIsSelectable(true);
        root.addView(tokenView, matchWrap());

        Button copy = button("Copiar token");
        copy.setOnClickListener(view -> copyToken());
        root.addView(copy, matchWrap());

        Button rotate = button("Gerar novo token");
        rotate.setOnClickListener(view -> {
            RafBridgePrefs.rotateToken(this);
            refreshState();
            toast("Token anterior invalidado");
        });
        root.addView(rotate, matchWrap());

        root.addView(space());
        Button start = button("Salvar e iniciar ponte");
        start.setOnClickListener(view -> startBridge());
        root.addView(start, matchWrap());

        Button stop = button("Parar ponte");
        stop.setOnClickListener(view -> stopBridge());
        root.addView(stop, matchWrap());

        statusView = text("", 15, true);
        root.addView(statusView, matchWrap());

        root.addView(space());
        root.addView(text(
                "Endereço da extensão: http://127.0.0.1:8765\n"
                        + "Backend recomendado: llamaRafaelia servido por llama.cpp em modo OpenAI-compatible.\n"
                        + "NanoGPT permanece útil para experimentos de treino, mas não é o runtime principal desta ponte.",
                14,
                false
        ));

        ScrollView scroll = new ScrollView(this);
        scroll.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        return scroll;
    }

    private void startBridge() {
        String endpoint = endpointInput.getText().toString().trim();
        String model = modelInput.getText().toString().trim();
        try {
            RafBridgePrefs.setModelEndpoint(this, endpoint);
            RafBridgePrefs.setModelName(this, model);
            RafBridgePrefs.setAllowSensitive(this, sensitiveCheck.isChecked());
            RafBridgePrefs.setEnabled(this, true);
            startService(new Intent(this, RafBridgeService.class));
            refreshState();
            toast("Ponte local iniciada");
        } catch (IllegalArgumentException error) {
            toast(error.getMessage());
        }
    }

    private void stopBridge() {
        RafBridgePrefs.setEnabled(this, false);
        stopService(new Intent(this, RafBridgeService.class));
        refreshState();
        toast("Ponte parada");
    }

    private void copyToken() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText(
                "Raf Bridge token",
                RafBridgePrefs.getToken(this)
        ));
        toast("Token copiado");
    }

    private void refreshState() {
        endpointInput.setText(RafBridgePrefs.getModelEndpoint(this));
        modelInput.setText(RafBridgePrefs.getModelName(this));
        sensitiveCheck.setChecked(RafBridgePrefs.allowSensitive(this));
        tokenView.setText(RafBridgePrefs.getToken(this));
        statusView.setText(RafBridgePrefs.isEnabled(this)
                ? "Estado: ATIVA em 127.0.0.1:" + RafBridgePrefs.BRIDGE_PORT
                : "Estado: PARADA");
    }

    private TextView text(String value, int sp, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        if (bold) {
            view.setTypeface(Typeface.DEFAULT_BOLD);
        }
        view.setPadding(0, dp(6), 0, dp(6));
        return view;
    }

    private Button button(String label) {
        Button button = new Button(this);
        button.setText(label);
        return button;
    }

    private TextView space() {
        TextView view = new TextView(this);
        view.setHeight(dp(12));
        return view;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void toast(String message) {
        Toast.makeText(this, message == null ? "Erro" : message, Toast.LENGTH_LONG).show();
    }
}
