package com.rafgittools.bridge;

import org.json.JSONObject;

import java.util.Locale;

/**
 * Moral and operational gate for Kiwi -> APK -> local model messages.
 *
 * Only conversation is accepted. No shell, git write, file write, automation,
 * credential forwarding, or hidden background action is part of this protocol.
 */
public final class RafBridgeContract {
    public static final int MAX_MESSAGE_CHARS = 32768;

    private RafBridgeContract() {
    }

    public static Result validate(JSONObject request, boolean allowSensitive) {
        String requestId = request.optString("request_id", "").trim();
        String action = request.optString("action", "").trim();
        String intent = request.optString("intent", "").trim();
        String dataClass = request.optString("data_class", "").trim().toLowerCase(Locale.ROOT);
        String source = request.optString("source", "").trim();
        String message = request.optString("message", "");
        boolean consent = request.optBoolean("consent", false);

        if (requestId.isEmpty()) {
            return Result.reject("request_id ausente");
        }
        if (!"chat".equals(action)) {
            return Result.reject("Apenas action=chat é permitida");
        }
        if (intent.isEmpty()) {
            return Result.reject("intent ausente");
        }
        if (!consent) {
            return Result.reject("consent=true é obrigatório para cada envio");
        }
        if (!"kiwi-extension".equals(source)) {
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

        return Result.allow(requestId, intent, dataClass, message);
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
        public final String intent;
        public final String dataClass;
        public final String message;

        private Result(
                boolean allowed,
                String error,
                String requestId,
                String intent,
                String dataClass,
                String message
        ) {
            this.allowed = allowed;
            this.error = error;
            this.requestId = requestId;
            this.intent = intent;
            this.dataClass = dataClass;
            this.message = message;
        }

        static Result reject(String error) {
            return new Result(false, error, "", "", "", "");
        }

        static Result allow(String requestId, String intent, String dataClass, String message) {
            return new Result(true, "", requestId, intent, dataClass, message);
        }
    }
}
