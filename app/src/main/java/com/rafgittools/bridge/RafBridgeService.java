package com.rafgittools.bridge;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Minimal local HTTP bridge for Kiwi Browser extensions.
 *
 * Bind: 127.0.0.1:8765 only.
 * Routes: GET /health and POST /v1/chat.
 * There is deliberately no command, shell, git-write, or filesystem route.
 */
public final class RafBridgeService extends Service {
    private static final int MAX_HEADER_BYTES = 16 * 1024;
    private static final int MAX_BODY_BYTES = 128 * 1024;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final ExecutorService clients = Executors.newFixedThreadPool(2);
    private final RafModelClient modelClient = new RafModelClient();

    private ServerSocket serverSocket;
    private Thread serverThread;

    @Override
    public void onCreate() {
        super.onCreate();
        if (RafBridgePrefs.isEnabled(this)) {
            startServer();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!RafBridgePrefs.isEnabled(this)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        startServer();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        stopServer();
        clients.shutdownNow();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private synchronized void startServer() {
        if (running.get()) {
            return;
        }
        running.set(true);
        serverThread = new Thread(this::acceptLoop, "raf-bridge-loopback");
        serverThread.start();
    }

    private void acceptLoop() {
        try {
            serverSocket = new ServerSocket();
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(
                    InetAddress.getByName("127.0.0.1"),
                    RafBridgePrefs.BRIDGE_PORT
            ));

            while (running.get()) {
                Socket socket = serverSocket.accept();
                socket.setSoTimeout(130000);
                clients.execute(() -> handleClient(socket));
            }
        } catch (IOException ignored) {
            // Closing the socket is the normal shutdown path.
        } finally {
            running.set(false);
            closeServerSocket();
        }
    }

    private void handleClient(Socket socket) {
        try (Socket client = socket;
             BufferedInputStream input = new BufferedInputStream(client.getInputStream());
             BufferedOutputStream output = new BufferedOutputStream(client.getOutputStream())) {

            HttpRequest request = HttpRequest.read(input);
            if (request == null) {
                writeJson(output, 400, error("invalid_request", "Requisição HTTP inválida"));
                return;
            }

            if ("OPTIONS".equals(request.method)) {
                writeEmpty(output, 204);
                return;
            }

            if ("GET".equals(request.method) && "/health".equals(request.path)) {
                JSONObject health = new JSONObject();
                health.put("ok", true);
                health.put("service", "raf-bridge");
                health.put("port", RafBridgePrefs.BRIDGE_PORT);
                health.put("model", RafBridgePrefs.getModelName(this));
                writeJson(output, 200, health);
                return;
            }

            if (!"POST".equals(request.method) || !"/v1/chat".equals(request.path)) {
                writeJson(output, 404, error("not_found", "Rota inexistente"));
                return;
            }

            String suppliedToken = request.headers.getOrDefault("x-raf-token", "");
            if (!constantTimeEquals(suppliedToken, RafBridgePrefs.getToken(this))) {
                writeJson(output, 401, error("unauthorized", "Token local inválido"));
                return;
            }

            JSONObject json = new JSONObject(request.body);
            RafBridgeContract.Result contract = RafBridgeContract.validate(
                    json,
                    RafBridgePrefs.allowSensitive(this)
            );
            if (!contract.allowed) {
                writeJson(output, 422, error("contract_rejected", contract.error));
                return;
            }

            String response = modelClient.chat(
                    RafBridgePrefs.getModelEndpoint(this),
                    RafBridgePrefs.getModelName(this),
                    contract.intent,
                    contract.dataClass,
                    contract.message
            );

            JSONObject result = new JSONObject();
            result.put("ok", true);
            result.put("request_id", contract.requestId);
            result.put("reply", response);
            result.put("executed_external_action", false);
            result.put("retained_message", false);
            writeJson(output, 200, result);
        } catch (Exception error) {
            try {
                BufferedOutputStream output = new BufferedOutputStream(socket.getOutputStream());
                writeJson(output, 502, error(
                        "bridge_error",
                        error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage()
                ));
            } catch (Exception ignored) {
                // Connection already closed.
            }
        }
    }

    private synchronized void stopServer() {
        running.set(false);
        closeServerSocket();
        if (serverThread != null) {
            serverThread.interrupt();
            serverThread = null;
        }
    }

    private void closeServerSocket() {
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
            }
            serverSocket = null;
        }
    }

    private static boolean constantTimeEquals(String left, String right) {
        return MessageDigest.isEqual(
                left.getBytes(StandardCharsets.UTF_8),
                right.getBytes(StandardCharsets.UTF_8)
        );
    }

    private static JSONObject error(String code, String message) {
        JSONObject json = new JSONObject();
        try {
            json.put("ok", false);
            json.put("error", code);
            json.put("message", message == null ? "Erro sem mensagem" : message);
        } catch (Exception ignored) {
        }
        return json;
    }

    private static void writeJson(BufferedOutputStream output, int status, JSONObject json)
            throws IOException {
        byte[] body = json.toString().getBytes(StandardCharsets.UTF_8);
        writeHeaders(output, status, "application/json; charset=utf-8", body.length);
        output.write(body);
        output.flush();
    }

    private static void writeEmpty(BufferedOutputStream output, int status) throws IOException {
        writeHeaders(output, status, "text/plain; charset=utf-8", 0);
        output.flush();
    }

    private static void writeHeaders(
            BufferedOutputStream output,
            int status,
            String contentType,
            int length
    ) throws IOException {
        String reason;
        switch (status) {
            case 200: reason = "OK"; break;
            case 204: reason = "No Content"; break;
            case 400: reason = "Bad Request"; break;
            case 401: reason = "Unauthorized"; break;
            case 404: reason = "Not Found"; break;
            case 422: reason = "Unprocessable Entity"; break;
            default: reason = "Bad Gateway"; break;
        }
        String headers = "HTTP/1.1 " + status + " " + reason + "\r\n"
                + "Content-Type: " + contentType + "\r\n"
                + "Content-Length: " + length + "\r\n"
                + "Access-Control-Allow-Origin: *\r\n"
                + "Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n"
                + "Access-Control-Allow-Headers: Content-Type, X-Raf-Token\r\n"
                + "Cache-Control: no-store\r\n"
                + "Connection: close\r\n\r\n";
        output.write(headers.getBytes(StandardCharsets.US_ASCII));
    }

    private static final class HttpRequest {
        final String method;
        final String path;
        final Map<String, String> headers;
        final String body;

        private HttpRequest(String method, String path, Map<String, String> headers, String body) {
            this.method = method;
            this.path = path;
            this.headers = headers;
            this.body = body;
        }

        static HttpRequest read(BufferedInputStream input) throws IOException {
            byte[] headerBytes = readUntilHeaderEnd(input);
            if (headerBytes == null) {
                return null;
            }
            String headerText = new String(headerBytes, StandardCharsets.US_ASCII);
            String[] lines = headerText.split("\\r?\\n");
            if (lines.length == 0) {
                return null;
            }
            String[] requestLine = lines[0].split(" ");
            if (requestLine.length < 2) {
                return null;
            }

            Map<String, String> headers = new HashMap<>();
            for (int i = 1; i < lines.length; i++) {
                int colon = lines[i].indexOf(':');
                if (colon > 0) {
                    headers.put(
                            lines[i].substring(0, colon).trim().toLowerCase(Locale.ROOT),
                            lines[i].substring(colon + 1).trim()
                    );
                }
            }

            int contentLength = 0;
            String value = headers.get("content-length");
            if (value != null && !value.isEmpty()) {
                try {
                    contentLength = Integer.parseInt(value);
                } catch (NumberFormatException error) {
                    return null;
                }
            }
            if (contentLength < 0 || contentLength > MAX_BODY_BYTES) {
                return null;
            }

            byte[] body = new byte[contentLength];
            int offset = 0;
            while (offset < contentLength) {
                int read = input.read(body, offset, contentLength - offset);
                if (read < 0) {
                    return null;
                }
                offset += read;
            }

            return new HttpRequest(
                    requestLine[0].toUpperCase(Locale.ROOT),
                    requestLine[1],
                    headers,
                    new String(body, StandardCharsets.UTF_8)
            );
        }

        private static byte[] readUntilHeaderEnd(BufferedInputStream input) throws IOException {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int state = 0;
            while (out.size() < MAX_HEADER_BYTES) {
                int value = input.read();
                if (value < 0) {
                    return null;
                }
                out.write(value);
                if ((state == 0 || state == 2) && value == '\r') {
                    state++;
                } else if ((state == 1 || state == 3) && value == '\n') {
                    state++;
                    if (state == 4) {
                        return out.toByteArray();
                    }
                } else {
                    state = 0;
                }
            }
            return null;
        }
    }
}
