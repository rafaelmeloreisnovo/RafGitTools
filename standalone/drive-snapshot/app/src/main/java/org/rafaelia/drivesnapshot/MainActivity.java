package org.rafaelia.drivesnapshot;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.gms.auth.api.identity.AuthorizationClient;
import com.google.android.gms.auth.api.identity.AuthorizationRequest;
import com.google.android.gms.auth.api.identity.AuthorizationResult;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.common.api.Scope;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Standalone app: no EstudioAudio/RafGitTools runtime dependency. */
public final class MainActivity extends Activity {
    private static final int AUTH_REQUEST = 4881;
    private static final String METADATA_SCOPE = "https://www.googleapis.com/auth/drive.metadata.readonly";
    private static final String WRITE_SCOPE = "https://www.googleapis.com/auth/drive.file";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<SnapshotDb.Node> visible = new ArrayList<>();
    private final ArrayList<String> stack = new ArrayList<>();
    private String bearer;
    private String folderId = "";
    private int offset = 0;
    private AuthorizationClient auth;
    private TextView state, folderLabel;
    private ListView tree;
    private Button scan, resume, explore, more, up;
    private SnapshotDb db;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new SnapshotDb(this);
        auth = Identity.getAuthorizationClient(this);
        render();
        handler.postDelayed(new Runnable() {
            @Override public void run() { updateStatus(); handler.postDelayed(this, 1500); }
        }, 1000);
    }

    private Button button(String caption, LinearLayout parent, View.OnClickListener click) {
        Button b = new Button(this);
        b.setText(caption); b.setOnClickListener(click);
        parent.addView(b, new LinearLayout.LayoutParams(-1, -2));
        return b;
    }

    private void render() {
        LinearLayout screen = new LinearLayout(this);
        screen.setPadding(16, 14, 16, 8);
        screen.setOrientation(LinearLayout.VERTICAL);
        TextView title = new TextView(this);
        title.setText("RAFAELIA Ω · DRIVE SNAPSHOT");
        title.setTextSize(19); title.setGravity(Gravity.CENTER_HORIZONTAL);
        screen.addView(title);
        state = new TextView(this);
        state.setText("Desconectado. Nenhuma operação automática.");
        state.setTextSize(13);
        screen.addView(state);
        button("1 · Conectar Google Drive", screen, v -> connect());
        scan = button("2 · Nova fotografia estrutural", screen, v -> start(ScanService.START));
        resume = button("Retomar fotografia interrompida", screen, v -> start(ScanService.RESUME));
        button("Pausar sem perder checkpoint", screen, v -> {
            Intent i = new Intent(this, ScanService.class); i.setAction(ScanService.PAUSE);
            startService(i);
        });
        explore = button("Explorar TREE local", screen, v -> {
            SharedPreferences p = getSharedPreferences(ScanService.PREF, MODE_PRIVATE);
            folderId = p.getString("root_id", "");
            stack.clear(); offset = 0; showFolder();
        });
        folderLabel = new TextView(this);
        folderLabel.setText("TREE: aguarda snapshot");
        screen.addView(folderLabel);
        up = button("↑ Pasta anterior", screen, v -> {
            if (!stack.isEmpty()) {
                folderId = stack.remove(stack.size() - 1); offset = 0; showFolder();
            }
        });
        tree = new ListView(this);
        screen.addView(tree, new LinearLayout.LayoutParams(-1, 0, 1));
        tree.setOnItemClickListener((parent, view, position, id) -> {
            if (position < 0 || position >= visible.size()) return;
            SnapshotDb.Node item = visible.get(position);
            if (!item.isFolder()) return;
            stack.add(folderId); folderId = item.id; offset = 0; showFolder();
        });
        more = button("Mostrar mais 100 itens", screen, v -> { offset += 100; showFolder(); });
        setContentView(screen);
        scan.setEnabled(false); resume.setEnabled(false);
    }

    private void connect() {
        state.setText("Solicitando consentimento Google...");
        AuthorizationRequest request = AuthorizationRequest.builder().setRequestedScopes(Arrays.asList(
                new Scope(METADATA_SCOPE), new Scope(WRITE_SCOPE))).build();
        auth.authorize(request).addOnSuccessListener(result -> {
            if (result.hasResolution()) {
                try {
                    startIntentSenderForResult(result.getPendingIntent().getIntentSender(), AUTH_REQUEST,
                            null, 0, 0, 0);
                } catch (Exception ex) { state.setText("Autorização bloqueada: " + ex.getClass().getSimpleName()); }
            } else accept(result);
        }).addOnFailureListener(e -> state.setText("Falha OAuth: " + e.getClass().getSimpleName()));
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != AUTH_REQUEST) return;
        if (resultCode != RESULT_OK || data == null) { state.setText("Consentimento cancelado"); return; }
        try { accept(auth.getAuthorizationResultFromIntent(data)); }
        catch (Exception e) { state.setText("Falha na autorização: " + e.getClass().getSimpleName()); }
    }

    private void accept(AuthorizationResult result) {
        List<String> granted = result.getGrantedScopes();
        if (!granted.contains(METADATA_SCOPE) || !granted.contains(WRITE_SCOPE)) {
            state.setText("BLOCKED: faltam escopos Drive metadata.readonly + drive.file");
            return;
        }
        bearer = result.getAccessToken();
        if (bearer == null || bearer.isEmpty()) { state.setText("TOKEN_VAZIO_AUTH"); return; }
        scan.setEnabled(true); resume.setEnabled(true);
        state.setText("Conectado. Toque em Nova fotografia ou Retomar. Nenhum scan foi iniciado.");
    }

    private void start(String action) {
        if (bearer == null) { state.setText("Conecte primeiro"); return; }
        Intent i = new Intent(this, ScanService.class);
        i.setAction(action);
        i.putExtra(ScanService.TOKEN, bearer);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(i);
        else startService(i);
        state.setText("Serviço iniciado; processamento em páginas de 50 itens.");
    }

    private void updateStatus() {
        SharedPreferences p = getSharedPreferences(ScanService.PREF, MODE_PRIVATE);
        String st = p.getString("status", "NOT_STARTED");
        int pages = p.getInt("page", 0);
        state.setText("Estado: " + st + "\nPáginas confirmadas: " + pages +
                " · arquivo(s): " + p.getInt("count", 0) +
                " · pastas: " + p.getInt("dirs", 0) +
                " · ZIPs: " + p.getInt("zips", 0) +
                "\n" + db.count());
    }

    private void showFolder() {
        if (folderId.isEmpty()) { folderLabel.setText("TREE: root desconhecida"); return; }
        visible.clear();
        visible.addAll(db.children(folderId, 100, offset));
        List<String> display = new ArrayList<>();
        for (SnapshotDb.Node n : visible) display.add(n.label());
        tree.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, display));
        folderLabel.setText("TREE / diretório " + folderId + " / offset " + offset +
                " / itens mostrados " + visible.size());
        more.setEnabled(visible.size() == 100);
        up.setEnabled(!stack.isEmpty());
    }

    @Override protected void onDestroy() {
        bearer = null;
        db.close();
        super.onDestroy();
    }
}
