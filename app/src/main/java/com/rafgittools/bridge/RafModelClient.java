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
    public static final int MAX_CONTEXT_JSON_CHARS = 65_536;
    public static final int MAX_EXAM_JSON_CHARS = 16_384;

    private static final String MORAL_SYSTEM_PROMPT =
            "Você é a ponte local RAFAELIA. Responda de modo natural, claro e humano, "
                    + "sem linguagem robótica. Preserve a intenção do usuário e a moral do contrato: "
                    + "consentimento explícito, privacidade, verdade sobre limites e ausência de ações ocultas. "
                    + "Você conversa e orienta; não afirma ter executado shell, git, arquivos, compras, envios "
                    + "ou mudanças externas. Não revele nem solicite senhas, tokens ou chaves privadas.";

    private static final String READ_ONLY_CONTEXT_SYSTEM_PROMPT =
            "O contexto anexado é somente leitura e veio do ContextBroker após seleção explícita. "
                    + "O Semantic Context Exam já foi executado. Preserve SOURCE != ARTIFACT != EXECUTION "
                    + "!= EVIDENCE != CLAIM e TOKEN_VAZIO != 0. Não invente unidade, transformação, "
                    + "autoridade, evidência ou ação externa. Cite limites e lacunas quando existirem. "
                    + "Este canal não autoriza shell, escrita em arquivos, git, publicação ou claim promotion.";

    public String chat(String endpoint, String model, String intent, String dataClass, String message)
            throws IOException {
        byte[] payload = buildPayload(model, intent, dataClass, message)
                .getBytes(StandardCharsets.UTF_8);
        return post(endpoint, payload);
    }

    public String chatExaminedContext(
            String endpoint,
            String model,
            String intent,
            String dataClass,
            String message,
            String contextBundleJson,
            String semanticExamResultJson
    ) throws IOException {
        if (contextBundleJson == null || contextBundleJson.isEmpty()) {
            throw new IOException("ContextBundle V2 ausente");
        }
        if (semanticExamResultJson == null || semanticExamResultJson.isEmpty()) {
            throw new IOException("Semantic Context Exam result ausente");
        }
        if (contextBundleJson.length() > MAX_CONTEXT_JSON_CHARS) {
            throw new IOException(
                    "ContextBundle V2 excede " + MAX_CONTEXT_JSON_CHARS + " caracteres"
            );
        }
        if (semanticExamResultJson.length() > MAX_EXAM_JSON_CHARS) {
            throw new IOException(
                    "Semantic Context Exam result excede " + MAX_EXAM_JSON_CHARS + " caracteres"
            );
        }
        if (RafBridgeContract.looksLikeCredential(contextBundleJson)
                || RafBridgeContract.looksLikeCredential(message)) {
            throw new IOException(
                    "Possível credencial detectada no contexto selecionado ou na mensagem"
            );
        }

        byte[] payload = buildExaminedPayload(
                model,
                intent,
                dataClass,
                message,
                contextBundleJson,
                semanticExamResultJson
        ).getBytes(StandardCharsets.UTF_8);
        return post(endpoint, payload);
    }

    private static String buildPayload(
            String model,
            String intent,
            String dataClass,
            String message
    ) throws IOException {
        try {
            JSONObject body = baseBody(model);
            JSONArray messages = new JSONArray();
            messages.put(system(MORAL_SYSTEM_PROMPT));
            messages.put(system(
                    "Intenção declarada: " + intent + ". Classe de dados: " + dataClass + "."
            ));
            messages.put(user(message));
            body.put("messages", messages);
            return body.toString();
        } catch (Exception error) {
            throw new IOException("Falha ao montar requisição do modelo", error);
        }
    }

    private static String buildExaminedPayload(
            String model,
            String intent,
            String dataClass,
            String message,
            String contextBundleJson,
            String semanticExamResultJson
    ) throws IOException {
        try {
            JSONObject body = baseBody(model);
            JSONArray messages = new JSONArray();
            messages.put(system(MORAL_SYSTEM_PROMPT));
            messages.put(system(READ_ONLY_CONTEXT_SYSTEM_PROMPT));
            messages.put(system(
                    "Intenção declarada: " + intent + ". Classe de dados: " + dataClass + "."
            ));
            messages.put(system("SEMANTIC_CONTEXT_EXAM_RESULT\n" + semanticExamResultJson));
            messages.put(system("CONTEXT_BUNDLE_V2\n" + contextBundleJson));
            messages.put(user(message));
            body.put("messages", messages);
            return body.toString();
        } catch (Exception error) {
            throw new IOException("Falha ao montar requisição examinada do modelo", error);
        }
    }

    private static JSONObject baseBody(String model) throws Exception {
        JSONObject body = new JSONObject();
        body.put("model", model);
        body.put("stream", false);
        body.put("temperature", 0.2);
        return body;
    }

    private static JSONObject system(String content) throws Exception {
        return new JSONObject().put("role", "system").put("content", content);
    }

    private static JSONObject user(String content) throws Exception {
        return new JSONObject().put("role", "user").put("content", content);
    }

    private static String post(String endpoint, byte[] payload) throws IOException {
        if (!RafBridgePrefs.isLoopbackEndpoint(endpoint)) {
            throw new IOException("Endpoint externo recusado; use apenas localhost/127.0.0.1");
        }

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
        return extractContent(response);
    }

    private static String extractContent(String response) throws IOException {
        try {
            JSONObject parsed = new JSONObject(response);
            JSONArray choices = parsed.optJSONArray("choices");
            if (choices == null || choices.length() == 0) {
                throw new IOException("Resposta do modelo sem choices");
            }
            JSONObject choice = choices.optJSONObject(0);
            JSONObject responseMessage = choice == null ? null : choice.optJSONObject("message");
            String content = responseMessage == null
                    ? ""
                    : responseMessage.optString("content", "").trim();
            if (content.isEmpty()) {
                throw new IOException("Resposta do modelo sem conteúdo");
            }
            return content;
        } catch (IOException error) {
            throw error;
        } catch (Exception error) {
            throw new IOException("Resposta JSON inválida do modelo local", error);
        }
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
