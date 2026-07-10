package com.rafgittools.bridge;

import android.content.Context;
import android.content.SharedPreferences;

import java.security.SecureRandom;

/**
 * Small, direct preference store for the local Raf Bridge.
 *
 * The bridge never listens outside loopback and never stores conversation bodies.
 */
public final class RafBridgePrefs {
    public static final String DEFAULT_MODEL_ENDPOINT =
            "http://127.0.0.1:8080/v1/chat/completions";
    public static final String DEFAULT_MODEL_NAME = "llama-rafaelia";
    public static final int BRIDGE_PORT = 8765;

    private static final String PREFS = "raf_bridge_private";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_ENDPOINT = "model_endpoint";
    private static final String KEY_MODEL = "model_name";
    private static final String KEY_ALLOW_SENSITIVE = "allow_sensitive";

    private RafBridgePrefs() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean isEnabled(Context context) {
        return prefs(context).getBoolean(KEY_ENABLED, false);
    }

    public static void setEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply();
    }

    public static boolean allowSensitive(Context context) {
        return prefs(context).getBoolean(KEY_ALLOW_SENSITIVE, false);
    }

    public static void setAllowSensitive(Context context, boolean allow) {
        prefs(context).edit().putBoolean(KEY_ALLOW_SENSITIVE, allow).apply();
    }

    public static String getToken(Context context) {
        String token = prefs(context).getString(KEY_TOKEN, "");
        if (token == null || token.length() < 32) {
            token = newToken();
            prefs(context).edit().putString(KEY_TOKEN, token).commit();
        }
        return token;
    }

    public static String rotateToken(Context context) {
        String token = newToken();
        prefs(context).edit().putString(KEY_TOKEN, token).commit();
        return token;
    }

    public static String getModelEndpoint(Context context) {
        String value = prefs(context).getString(KEY_ENDPOINT, DEFAULT_MODEL_ENDPOINT);
        return value == null ? DEFAULT_MODEL_ENDPOINT : value;
    }

    public static void setModelEndpoint(Context context, String endpoint) {
        if (!isLoopbackEndpoint(endpoint)) {
            throw new IllegalArgumentException("O endpoint do modelo deve usar 127.0.0.1 ou localhost.");
        }
        prefs(context).edit().putString(KEY_ENDPOINT, endpoint.trim()).apply();
    }

    public static String getModelName(Context context) {
        String value = prefs(context).getString(KEY_MODEL, DEFAULT_MODEL_NAME);
        return value == null || value.trim().isEmpty() ? DEFAULT_MODEL_NAME : value.trim();
    }

    public static void setModelName(Context context, String model) {
        String value = model == null ? "" : model.trim();
        prefs(context).edit().putString(KEY_MODEL,
                value.isEmpty() ? DEFAULT_MODEL_NAME : value).apply();
    }

    public static boolean isLoopbackEndpoint(String endpoint) {
        if (endpoint == null) {
            return false;
        }
        String value = endpoint.trim().toLowerCase();
        return value.startsWith("http://127.0.0.1:")
                || value.startsWith("http://localhost:");
    }

    private static String newToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        StringBuilder out = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            out.append(String.format("%02x", value & 0xff));
        }
        return out.toString();
    }
}
