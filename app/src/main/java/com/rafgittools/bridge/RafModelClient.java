package com.rafgittools.bridge;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** Direct OpenAI-compatible client for a local llama.cpp/llamaRafaelia server. */
public final class RafModelClient {
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 120000;

    private static final String MORAL_SYSTEM_PROMPT =
            "Você é a ponte local RAFAELIA. Responda de modo natural, claro e humano, "
                    + "sem linguagem robótica. Preserve a intenção do usuário e a moral do contrato: "
                    + "consentimento explícito, privacidade, verdade sobre limites e ausência de ações ocultas. "
                    + "Você conversa e orienta; não afirma ter executado shell, git, arquivos, compras, envios "
                    + "ou mudanças externas. Não revele nem solicite senhas, tokens ou chaves privadas.";

    public String chat(String endpoint, String model, String intent, String dataClass, String message)
            throws IOException {
        if (!RafBridgePrefs.isLoopbackEndpoint(endpoint)) {
            throw new IOException("Endpoint externo recusado; use apenas localhost/127.0.0.1");
        }

        JSONObject body = new JSONObject();
        body.put("model", model);
        body.put("stream", false);
        body.put("temperature", 0.7);

        JSONArray messages = new JSONArray();
        messages.put(new JSONObject()
                .put("role", "system")
                .put("content", MORAL_SYSTEM_PROMPT));
        messages.put(new JSONObject()
                .put("role", "system")
                .put("content", "Intenção declarada: " + intent
                        + ". Classe de dados: " + dataClass + "."));
        messages.put(new JSONObject()
                .put("role", "user")
                .put("content", message));
        body.put("messages", messages);

        byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);
        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        connection.setRequestProperty("Accept", "application/json");
        connection.setFixedLengthStreamingMode(payload.length);

        try (OutputStream output = connection.getOutputStream()) {
            output.write(payload);
        }

        int status = connection.getResponseCode();
        InputStream stream = status >= 200 && status < 300
                ? connection.getInputStream()
                : connection.getErrorStream();
        String response = readAll(stream);
        connection.disconnect();

        if (status < 200 || status >= 300) {
            throw new IOException("Modelo local respondeu HTTP " + status + ": " + response);
        }

        JSONObject parsed = new JSONObject(response);
        JSONArray choices = parsed.optJSONArray("choices");
        if (choices == null || choices.length() == 0) {
            throw new IOException("Resposta do modelo sem choices");
        }
        JSONObject choice = choices.optJSONObject(0);
        JSONObject responseMessage = choice == null ? null : choice.optJSONObject("message");
        String content = responseMessage == null ? "" : responseMessage.optString("content", "").trim();
        if (content.isEmpty()) {
            throw new IOException("Resposta do modelo sem conteúdo");
        }
        return content;
    }

    private static String readAll(InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                out.append(line).append('\n');
            }
        }
        return out.toString().trim();
    }
}
