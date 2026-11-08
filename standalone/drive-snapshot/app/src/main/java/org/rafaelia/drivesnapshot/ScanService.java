package org.rafaelia.drivesnapshot;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;

import java.io.File;
import java.net.HttpURLConnection;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/** Foreground dataSync scan: bounded one-page spool and crash-safe remote sharding. */
public final class ScanService extends Service {
    static final String PREF = "scan_checkpoint_v1";
    static final String START = "org.rafaelia.drivesnapshot.START";
    static final String RESUME = "org.rafaelia.drivesnapshot.RESUME";
    static final String PAUSE = "org.rafaelia.drivesnapshot.PAUSE";
    static final String TOKEN = "access_token";
    private static final String CHANNEL = "snapshot_ongoing";
    private static final int NOTIFICATION = 4301;
    private final AtomicBoolean pause = new AtomicBoolean(false);
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final ExecutorService worker = Executors.newSingleThreadExecutor();

    @Override public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(CHANNEL, "Drive Snapshot", NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(channel);
        }
    }

    @Override public int onStartCommand(Intent command, int flags, int startId) {
        if (command == null) return START_NOT_STICKY;
        String action = command.getAction();
        if (PAUSE.equals(action)) {
            pause.set(true);
            status("PAUSE_REQUESTED");
            return START_NOT_STICKY;
        }
        String token = command.getStringExtra(TOKEN);
        if (token == null || token.length() < 12) {
            status("AUTH_REQUIRED");
            stopSelf();
            return START_NOT_STICKY;
        }
        try { startForeground(NOTIFICATION, notification("Fotografia estrutural em andamento")); }
        catch (Exception ex) { status("FOREGROUND_NOT_ALLOWED"); stopSelf(); return START_NOT_STICKY; }
        if (!running.compareAndSet(false, true)) return START_NOT_STICKY;
        pause.set(false);
        boolean fresh = START.equals(action);
        worker.execute(() -> {
            try { scan(token, fresh); }
            catch (Exception e) { status("BLOCKED: " + safe(e.getMessage())); }
            finally {
                running.set(false);
                stopForeground(true);
                stopSelf();
            }
        });
        return START_NOT_STICKY;
    }

    private Notification notification(String title) {
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ?
                new Notification.Builder(this, CHANNEL) : new Notification.Builder(this);
        return b.setContentTitle("RAFAELIA · Drive Snapshot")
                .setContentText(title).setSmallIcon(android.R.drawable.stat_notify_sync)
                .setOngoing(true).build();
    }

    private static String safe(String error) {
        if (error == null) return "UNKNOWN";
        // Never put OAuth tokens or full raw provider responses into a UI/receipt.
        String cleaned = error.replaceAll("(?i)Bearer\\s+[A-Za-z0-9._-]+", "Bearer [redacted]")
                .replaceAll("https://[^\\s]+", "[url]");
        return cleaned.substring(0, Math.min(cleaned.length(), 160));
    }

    private void status(String state) {
        getSharedPreferences(PREF, MODE_PRIVATE).edit().putString("status", state).apply();
        NotificationManager mgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (mgr != null && running.get()) mgr.notify(NOTIFICATION, notification(state));
    }

    private void scan(String token, boolean fresh) throws Exception {
        SharedPreferences prefs = getSharedPreferences(PREF, MODE_PRIVATE);
        DriveClient drive = new DriveClient(token);
        SnapshotDb index = new SnapshotDb(this);
        if (fresh) {
            // This deletes ONLY the local derived SQLite cache; never a Drive file.
            index.clear();
            prefs.edit().clear().putString("status", "INITIALIZING").commit();
        }
        String runId = prefs.getString("run_id", "");
        if (runId.isEmpty()) {
            runId = UUID.randomUUID().toString();
            prefs.edit().putString("run_id", runId).putInt("page", 0).putString("token", "")
                    .putBoolean("completed", false).commit();
        }
        if (prefs.getBoolean("completed", false)) { status("COMPLETE_ALREADY"); return; }
        status("AUTH_OK · CHECKING_DESTINATION");
        String root = drive.rootId();
        prefs.edit().putString("root_id", root).commit();
        String organizer = drive.ensureFolder(root, "__ORGANIZANDO");
        String snapshots = drive.ensureFolder(organizer, "010_SNAPSHOTS");
        String folder = drive.ensureFolder(snapshots, "SNAP_" + runId);
        prefs.edit().putString("folder_id", folder).commit();

        while (!pause.get()) {
            int page = prefs.getInt("page", 0);
            String next = prefs.getString("token", "");
            File spool = new File(getCacheDir(), "snapshot_" + runId + "_" + page + ".jsonl.gz");
            if (!prefs.getBoolean("pending", false) || !spool.isFile()) {
                if (prefs.getBoolean("pending", false) && !spool.isFile())
                    prefs.edit().putBoolean("pending", false).commit();
                status("READING_PAGE_" + page);
                HttpURLConnection response = drive.listPage(next);
                JsonlCapture.Result result;
                try {
                    result = JsonlCapture.capture(response, spool, index, runId);
                } catch (Exception e) {
                    if (spool.exists()) spool.delete();
                    throw e;
                }
                // A durable local journal precedes the remote upload.
                boolean saved = prefs.edit().putBoolean("pending", true)
                        .putString("pending_next", result.next)
                        .putInt("pending_items", result.items)
                        .putInt("pending_dirs", result.folders)
                        .putInt("pending_zips", result.zips)
                        .putBoolean("pending_incomplete", result.incomplete)
                        .commit();
                if (!saved) throw new IllegalStateException("checkpoint storage failed");
            }
            if (pause.get()) break;
            status("UPLOADING_PAGE_" + page);
            String fileName = String.format(java.util.Locale.ROOT, "files_%08d.jsonl.gz", page);
            drive.uploadShard(folder, fileName, runId, page, spool);
            // Commit cursor ONLY after the matching shard is verified remotely.
            boolean incomplete = prefs.getBoolean("incomplete", false) ||
                    prefs.getBoolean("pending_incomplete", false);
            String nextToken = prefs.getString("pending_next", "");
            int count = prefs.getInt("count", 0) + prefs.getInt("pending_items", 0);
            int dirs = prefs.getInt("dirs", 0) + prefs.getInt("pending_dirs", 0);
            int zips = prefs.getInt("zips", 0) + prefs.getInt("pending_zips", 0);
            boolean done = nextToken.isEmpty();
            boolean checkpointOk = prefs.edit()
                    .putInt("page", page + 1).putString("token", nextToken)
                    .putInt("count", count).putInt("dirs", dirs).putInt("zips", zips)
                    .putBoolean("incomplete", incomplete).putBoolean("completed", done)
                    .putBoolean("pending", false).remove("pending_next")
                    .remove("pending_items").remove("pending_dirs").remove("pending_zips")
                    .remove("pending_incomplete").commit();
            if (!checkpointOk) throw new IllegalStateException("post-upload checkpoint failed");
            spool.delete();
            status((done ? (incomplete ? "INCOMPLETE_SEARCH" : "LIST_PAGINATION_COMPLETE") : "SCANNING") +
                    " · " + count + " itens / " + dirs + " pastas / " + zips + " ZIPs");
            if (done) break;
        }
        if (pause.get()) status("PAUSED · SAFE_AT_PAGE_BOUNDARY");
    }

    @Override public void onDestroy() {
        pause.set(true);
        worker.shutdown();
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
