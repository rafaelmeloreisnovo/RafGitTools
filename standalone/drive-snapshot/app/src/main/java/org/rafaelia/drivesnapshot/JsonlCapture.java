package org.rafaelia.drivesnapshot;

import android.database.sqlite.SQLiteDatabase;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.JsonWriter;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;

/** Decode HTTP response incrementally; do not hold an array of files in memory. */
final class JsonlCapture {
    static final int MAX_RECORD_CHARS = 512 * 1024;
    static final class Result {
        int items, folders, zips, skippedInternal;
        String next = "";
        boolean incomplete;
    }

    static Result capture(HttpURLConnection response, File outFile, SnapshotDb index, String runId) throws Exception {
        Result result = new Result();
        SQLiteDatabase db = index.getWritableDatabase();
        db.beginTransaction();
        try (JsonReader input = new JsonReader(new InputStreamReader(response.getInputStream(), StandardCharsets.UTF_8));
             FileOutputStream file = new FileOutputStream(outFile);
             GZIPOutputStream gzip = new GZIPOutputStream(file, 8192);
             OutputStreamWriter writer = new OutputStreamWriter(gzip, StandardCharsets.UTF_8)) {
            input.beginObject();
            while (input.hasNext()) {
                String key = input.nextName();
                if ("nextPageToken".equals(key)) result.next = input.nextString();
                else if ("incompleteSearch".equals(key)) result.incomplete = input.nextBoolean();
                else if ("files".equals(key)) {
                    input.beginArray();
                    while (input.hasNext()) {
                        // Limited to one metadata record at a time, not 50 JSON objects.
                        BoundedWriter storage = new BoundedWriter(MAX_RECORD_CHARS);
                        JsonWriter output = new JsonWriter(storage);
                        copyJson(input, output, 0);
                        output.flush();
                        JSONObject f = new JSONObject(storage.toString());
                        JSONObject props = f.optJSONObject("appProperties");
                        if (props != null && "RAFAELIA".equals(props.optString("snapshot_owner"))) {
                            result.skippedInternal++;
                            continue;
                        }
                        JSONObject record = new JSONObject();
                        record.put("schema", "rafaelia.drive.file.v1");
                        record.put("run_id", runId);
                        record.put("source", "google_drive_v3_files_list");
                        record.put("object", f);
                        writer.write(record.toString());
                        writer.write('\n');
                        index.add(f);
                        result.items++;
                        if ("application/vnd.google-apps.folder".equals(f.optString("mimeType"))) result.folders++;
                        String name = f.optString("name", "").toLowerCase(java.util.Locale.ROOT);
                        if (name.endsWith(".zip") || name.endsWith(".zipraf")) result.zips++;
                    }
                    input.endArray();
                } else input.skipValue();
            }
            input.endObject();
            writer.flush();
            gzip.finish();
            file.getFD().sync();
            db.setTransactionSuccessful();
            return result;
        } finally {
            db.endTransaction();
            response.disconnect();
        }
    }

    private static void copyJson(JsonReader in, JsonWriter out, int depth) throws IOException {
        if (depth > 32) throw new IOException("metadata too deeply nested");
        JsonToken kind = in.peek();
        switch (kind) {
            case BEGIN_OBJECT:
                in.beginObject(); out.beginObject();
                while (in.hasNext()) { out.name(in.nextName()); copyJson(in, out, depth + 1); }
                in.endObject(); out.endObject(); break;
            case BEGIN_ARRAY:
                in.beginArray(); out.beginArray();
                while (in.hasNext()) copyJson(in, out, depth + 1);
                in.endArray(); out.endArray(); break;
            case STRING: out.value(in.nextString()); break;
            case NUMBER: out.value(new BigDecimal(in.nextString())); break;
            case BOOLEAN: out.value(in.nextBoolean()); break;
            case NULL: in.nextNull(); out.nullValue(); break;
            default: throw new IOException("unsupported metadata JSON token " + kind);
        }
    }

    private static final class BoundedWriter extends java.io.Writer {
        private final int max;
        private final StringBuilder buf = new StringBuilder(2048);
        BoundedWriter(int max) { this.max = max; }
        @Override public void write(char[] c, int off, int len) throws IOException {
            if (len < 0 || buf.length() > max - len) throw new IOException("metadata object exceeds per-file budget");
            buf.append(c, off, len);
        }
        @Override public void write(int c) throws IOException {
            if (buf.length() >= max) throw new IOException("metadata object exceeds per-file budget");
            buf.append((char)c);
        }
        @Override public void write(String str, int off, int len) throws IOException {
            if (len < 0 || buf.length() > max - len) throw new IOException("metadata object exceeds per-file budget");
            buf.append(str, off, off + len);
        }
        @Override public void flush() { }
        @Override public void close() { }
        @Override public String toString() { return buf.toString(); }
    }
}
