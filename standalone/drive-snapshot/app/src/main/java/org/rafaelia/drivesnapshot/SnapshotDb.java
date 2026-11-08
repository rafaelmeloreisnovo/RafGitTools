package org.rafaelia.drivesnapshot;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Minimal on-device tree cache. The authoritative snapshot is remote JSONL. */
final class SnapshotDb extends SQLiteOpenHelper {
    private static final String FOLDER = "application/vnd.google-apps.folder";
    SnapshotDb(Context context) { super(context, "drive_snapshot_cache_v1.db", null, 1); }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE nodes(id TEXT PRIMARY KEY, parent_id TEXT, name TEXT NOT NULL," +
                "mime TEXT NOT NULL, size_bytes INTEGER, size_known INTEGER NOT NULL," +
                "modified TEXT, trashed INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX nodes_parent ON nodes(parent_id,name)");
    }
    @Override public void onUpgrade(SQLiteDatabase db, int from, int to) {
        throw new IllegalStateException("Explicit cache migration required");
    }
    void clear() { getWritableDatabase().delete("nodes", null, null); }

    void add(JSONObject f) {
        final String id = f.optString("id", "");
        if (id.isEmpty()) return;
        JSONArray parents = f.optJSONArray("parents");
        String parent = parents != null && parents.length() > 0 ? parents.optString(0, "") : "";
        String mime = f.optString("mimeType", "application/octet-stream");
        boolean known = f.has("size") && !f.isNull("size");
        ContentValues row = new ContentValues();
        row.put("id", id);
        row.put("parent_id", parent);
        row.put("name", f.optString("name", ""));
        row.put("mime", mime);
        if (known) {
            try { row.put("size_bytes", Long.parseLong(f.getString("size"))); }
            catch (Exception e) { row.putNull("size_bytes"); known = false; }
        } else row.putNull("size_bytes");
        row.put("size_known", known ? 1 : 0);
        row.put("modified", f.optString("modifiedTime", ""));
        row.put("trashed", f.optBoolean("trashed", false) ? 1 : 0);
        getWritableDatabase().insertWithOnConflict("nodes", null, row, SQLiteDatabase.CONFLICT_REPLACE);
    }

    static final class Node {
        String id, parent, name, mime;
        long size;
        boolean known;
        boolean isFolder() { return FOLDER.equals(mime); }
        String label() {
            String prefix = isFolder() ? "📁 " : "📄 ";
            return prefix + name + "   [" + (known ? human(size) : "size: TOKEN_VAZIO") + "]";
        }
    }
    static String human(long n) {
        if (n < 1024) return n + " B";
        final String[] units = {"KiB", "MiB", "GiB", "TiB", "PiB"};
        double value = n;
        int idx = -1;
        do { value /= 1024.; idx++; } while (value >= 1024 && idx + 1 < units.length);
        return String.format(java.util.Locale.ROOT, "%.1f %s", value, units[idx]);
    }

    List<Node> children(String parent, int limit, int offset) {
        List<Node> out = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT id,parent_id,name,mime,size_bytes,size_known FROM nodes " +
                "WHERE parent_id=? AND trashed=0 ORDER BY (mime=?) DESC, name COLLATE NOCASE LIMIT ? OFFSET ?",
                new String[] {parent, FOLDER, String.valueOf(limit), String.valueOf(offset)})) {
            while (c.moveToNext()) {
                Node n = new Node();
                n.id = c.getString(0); n.parent = c.getString(1); n.name = c.getString(2);
                n.mime = c.getString(3); n.known = c.getInt(5) == 1;
                if (n.known) n.size = c.getLong(4);
                out.add(n);
            }
        }
        return out;
    }

    String count() {
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT COUNT(*), SUM(CASE WHEN mime=? THEN 1 ELSE 0 END), " +
                "SUM(CASE WHEN size_known=1 THEN size_bytes ELSE 0 END), " +
                "SUM(CASE WHEN size_known=0 THEN 1 ELSE 0 END) FROM nodes", new String[]{FOLDER})) {
            if (!c.moveToFirst()) return "0 arquivos";
            return c.getLong(0) + " itens / " + c.getLong(1) + " diretórios / " +
                    human(c.getLong(2)) + " conhecidos / " + c.getLong(3) + " tamanho pendente";
        }
    }
}
