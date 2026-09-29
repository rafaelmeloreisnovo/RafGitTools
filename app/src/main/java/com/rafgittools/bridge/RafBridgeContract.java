package com.rafgittools.bridge;

import org.json.JSONObject;

import java.util.Locale;

/**
 * Moral and operational gate for browser client -> APK -> local model messages.
 *
 * Only conversation is accepted. No shell, git write, file write, automation,
 * credential forwarding, or hidden background action is part of this protocol.
 */
public final class RafBridgeContract {
    public static final int MAX_MESSAGE_CHARS = 32768;

    private RafBridgeContract() {
    }

    public static Result validate(JSONObject request, boolean allowSensitive) {
        String schema = request.optString("schema", "").trim();
        String requestId = request.optString("request_id", "").trim();
        String action = request.optString("action", "").trim();
        String intent = request.optString("intent", "").trim();
        String dataClass = request.optString("data_class", "").trim().toLowerCase(Locale.ROOT);
        String source = request.optString("source", "").trim();
        String message = request.optString("message", "");
        boolean consent = request.optBoolean("consent", false);

        // Empty schema preserves compatibility with the existing Kiwi v0.1 client.
        if (!schema.isEmpty() && !"raf.client.envelope.v1".equals(schema)) {
            return Result.reject("schema de cliente não suportado");
        }
        if (requestId.isEmpty()) {
            return Result.reject("request_id ausente");
        }
        if (!"chat".equals(action) && !"context_chat".equals(action)) {
            return Result.reject("Apenas action=chat ou action=context_chat são permitidas");
        }
        if (intent.isEmpty()) {
            return Result.reject("intent ausente");
        }
        if (!consent) {
            return Result.reject("consent=true é obrigatório para cada envio");
        }
        if (!isAllowedSource(source)) {
            return Result.reject("source não autorizado");
        }
        if (!("public".equals(dataClass)
                || "private".equals(dataClass)
                || "sensitive".equals(dataClass))) {
            return Result.reject("data_class deve ser public, private ou sensitive");
        }
        if ("sensitive".equals(dataClass) && !allowSensitive) {
            return Result.reject("Conteúdo sensível está bloqueado nas configurações do APK");
        }
        if (message.trim().isEmpty()) {
            return Result.reject("message vazio");
        }
        if (message.length() > MAX_MESSAGE_CHARS) {
            return Result.reject("message excede " + MAX_MESSAGE_CHARS + " caracteres");
        }
        if (looksLikeCredential(message)) {
            return Result.reject("Possível credencial detectada; remova tokens, senhas ou chaves privadas");
        }

        String semanticExamJson = null;
        if ("context_chat".equals(action)) {
            JSONObject semanticExam = request.optJSONObject("semantic_exam");
            if (semanticExam == null) {
                return Result.reject("semantic_exam é obrigatório para action=context_chat");
            }
            semanticExamJson = semanticExam.toString();
        }

        return Result.allow(requestId, action, intent, dataClass, message, semanticExamJson);
    }

    private static boolean isAllowedSource(String source) {
        return "kiwi-extension".equals(source)
                || "tampermonkey-userscript".equals(source);
    }

    private static boolean looksLikeCredential(String message) {
        String lower = message.toLowerCase(Locale.ROOT);
        return message.contains("ghp_")
                || message.contains("github_pat_")
                || message.contains("-----BEGIN PRIVATE KEY-----")
                || message.contains("-----BEGIN OPENSSH PRIVATE KEY-----")
                || lower.contains("password=")
                || lower.contains("senha=")
                || lower.contains("authorization: bearer ");
    }

    public static final class Result {
        public final boolean allowed;
        public final String error;
        public final String requestId;
        public final String action;
        public final String intent;
        public final String dataClass;
        public final String message;
        public final String semanticExamJson;

        private Result(
                boolean allowed,
                String error,
                String requestId,
                String action,
                String intent,
                String dataClass,
                String message,
                String semanticExamJson
        ) {
            this.allowed = allowed;
            this.error = error;
            this.requestId = requestId;
            this.action = action;
            this.intent = intent;
            this.dataClass = dataClass;
            this.message = message;
            this.semanticExamJson = semanticExamJson;
        }

        static Result reject(String error) {
            return new Result(false, error, "", "", "", "", null);
        }

        static Result allow(
                String requestId,
                String action,
                String intent,
                String dataClass,
                String message,
                String semanticExamJson
        ) {
            return new Result(
                    true,
                    "",
                    requestId,
                    action,
                    intent,
                    dataClass,
                    message,
                    semanticExamJson
            );
        }
    }
}
