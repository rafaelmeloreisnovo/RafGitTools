package org.rafaelia.drivesnapshot;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Google Drive v3 transport. Only Google API HTTPS hosts; token stays in RAM. */
final class DriveClient {
    private static final String API = "https://www.googleapis.com/drive/v3/";
    private static final String UPLOAD = "https://www.googleapis.com/upload/drive/v3/files";
    private final String bearer;

    DriveClient(String bearer) {
        if (bearer == null || bearer.length() < 12) throw new IllegalArgumentException("missing auth");
        this.bearer = bearer;
    }

    static String enc(String s) throws IOException {
        return URLEncoder.encode(s, "UTF-8");
    }

    private HttpURLConnection connection(String url, String method) throws IOException {
        if (!url.startsWith("https://www.googleapis.com/")) throw new SecurityException("host not allowed");
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setInstanceFollowRedirects(false);
        c.setConnectTimeout(20000);
        c.setReadTimeout(90000);
        c.setRequestMethod(method);
        c.setRequestProperty("Authorization", "Bearer " + bearer);
        c.setRequestProperty("Accept", "application/json");
        c.setRequestProperty("User-Agent", "RAFAELIA-DriveSnapshot/0.1");
        return c;
    }

    static void ensureOk(HttpURLConnection c) throws IOException {
        int status = c.getResponseCode();
        if (status >= 200 && status < 300) return;
        String reason;
        try (InputStream in = c.getErrorStream()) {
            byte[] buf = new byte[1024];
            int n = in == null ? -1 : in.read(buf);
            reason = n < 1 ? "" : new String(buf, 0, n, StandardCharsets.UTF_8);
        }
        throw new IOException("DRIVE_HTTP_" + status + " " + reason.replaceAll("(?i)Bearer\\s+[A-Za-z0-9._-]+", "Bearer [redacted]"));
    }

    private JSONObject get(String path) throws Exception {
        HttpURLConnection c = connection(API + path, "GET");
        try {
            ensureOk(c);
            return readJson(c.getInputStream(), 2 * 1024 * 1024);
        } finally { c.disconnect(); }
    }

    private static JSONObject readJson(InputStream stream, int limit) throws Exception {
        try (InputStream in = stream; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int total = 0, n;
            while ((n = in.read(buffer)) != -1) {
                total += n;
                if (total > limit) throw new IOException("metadata response too large");
                out.write(buffer, 0, n);
            }
            return new JSONObject(out.toString("UTF-8"));
        }
    }

    /** Caller parses one file at a time and always closes/disconnects. */
    HttpURLConnection listPage(String token) throws Exception {
        // files(*) captures every File field Google returns; page remains small.
        String url = API + "files?pageSize=50&corpora=user&spaces=drive" +
                "&supportsAllDrives=true&includeItemsFromAllDrives=true" +
                "&fields=" + enc("nextPageToken,incompleteSearch,files(*)");
        if (token != null && !token.isEmpty()) url += "&pageToken=" + enc(token);
        HttpURLConnection c = connection(url, "GET");
        try { ensureOk(c); return c; }
        catch (Exception e) { c.disconnect(); throw e; }
    }

    String rootId() throws Exception {
        return get("files/root?fields=id").getString("id");
    }

    private static String quoteDrive(String value) {
        return value.replace("\\", "\\\\").replace("'", "\\'");
    }

    /** A matching child is only reused when type and app ownership are safe. */
    private JSONObject findChild(String parentId, String name, String mime) throws Exception {
        String q = "'" + quoteDrive(parentId) + "' in parents and name = '" +
                quoteDrive(name) + "' and trashed = false";
        String token = "";
        do {
            String path = "files?q=" + enc(q) + "&pageSize=100&fields=" +
                    enc("nextPageToken,files(id,name,mimeType,md5Checksum,appProperties)");
            if (!token.isEmpty()) path += "&pageToken=" + enc(token);
            JSONObject result = get(path);
            JSONArray arr = result.getJSONArray("files");
            for (int i = 0; i < arr.length(); i++) {
                JSONObject file = arr.getJSONObject(i);
                if (mime.equals(file.optString("mimeType"))) return file;
            }
            token = result.optString("nextPageToken", "");
        } while (!token.isEmpty());
        return null;
    }

    String ensureFolder(String parentId, String name) throws Exception {
        String mime = "application/vnd.google-apps.folder";
        JSONObject existing = findChild(parentId, name, mime);
        if (existing != null) return existing.getString("id");
        JSONObject body = new JSONObject();
        body.put("name", name);
        body.put("mimeType", mime);
        body.put("parents", new JSONArray().put(parentId));
        body.put("appProperties", new JSONObject().put("snapshot_owner", "RAFAELIA"));
        HttpURLConnection c = connection(API + "files?fields=id,name,mimeType", "POST");
        try {
            byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            c.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream out = c.getOutputStream()) { out.write(bytes); }
            ensureOk(c);
            return readJson(c.getInputStream(), 32768).getString("id");
        } finally { c.disconnect(); }
    }

    static String hash(File file, String algorithm) throws Exception {
        MessageDigest digest = MessageDigest.getInstance(algorithm);
        try (FileInputStream in = new FileInputStream(file)) {
            byte[] b = new byte[32768];
            int n;
            while ((n = in.read(b)) != -1) digest.update(b, 0, n);
        }
        StringBuilder out = new StringBuilder();
        for (byte x : digest.digest()) out.append(String.format(java.util.Locale.ROOT, "%02x", x & 255));
        return out.toString();
    }

    /** Return existing verified shard on retry, otherwise create atomically as a new Drive file. */
    String uploadShard(String parentId, String name, String runId, int page, File data) throws Exception {
        String localMd5 = hash(data, "MD5");
        JSONObject existing = findChild(parentId, name, "application/gzip");
        if (existing != null) {
            String remoteMd5 = existing.optString("md5Checksum", "");
            if (!localMd5.equalsIgnoreCase(remoteMd5)) throw new IOException("REMOTE_SHARD_COLLISION:" + name);
            return existing.getString("id");
        }
        JSONObject meta = new JSONObject();
        meta.put("name", name);
        meta.put("parents", new JSONArray().put(parentId));
        meta.put("mimeType", "application/gzip");
        meta.put("appProperties", new JSONObject()
                .put("snapshot_owner", "RAFAELIA")
                .put("snapshot_run", runId)
                .put("snapshot_page", String.valueOf(page)));
        String boundary = "RAFAELIA_SNAPSHOT_BOUNDARY_01";
        byte[] pre = ("--" + boundary + "\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n" +
                meta.toString() + "\r\n--" + boundary + "\r\nContent-Type: application/gzip\r\n\r\n")
                .getBytes(StandardCharsets.UTF_8);
        byte[] post = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);
        HttpURLConnection c = connection(UPLOAD + "?uploadType=multipart&fields=id,name,md5Checksum", "POST");
        try {
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", "multipart/related; boundary=" + boundary);
            c.setFixedLengthStreamingMode((long) pre.length + data.length() + post.length);
            try (OutputStream out = c.getOutputStream(); FileInputStream in = new FileInputStream(data)) {
                out.write(pre);
                byte[] b = new byte[32768];
                int n;
                while ((n = in.read(b)) != -1) out.write(b, 0, n);
                out.write(post);
            }
            ensureOk(c);
            JSONObject response = readJson(c.getInputStream(), 32768);
            if (!localMd5.equalsIgnoreCase(response.optString("md5Checksum", ""))) {
                throw new IOException("UPLOAD_MD5_NOT_CONFIRMED:" + name);
            }
            return response.getString("id");
        } finally { c.disconnect(); }
    }
}
