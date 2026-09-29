package com.rafgittools.bridge;

/** Non-promoting privacy compatibility gate for ContextBundle -> local model handoff. */
public final class RafContextPrivacyGate {
    private RafContextPrivacyGate() {
    }

    public static boolean allows(String dataClass, String privacyClass) {
        if ("TOKEN_VAZIO".equals(privacyClass)) {
            return false;
        }

        int declaredRank;
        if ("sensitive".equals(dataClass)) {
            declaredRank = 2;
        } else if ("private".equals(dataClass)) {
            declaredRank = 1;
        } else if ("public".equals(dataClass)) {
            declaredRank = 0;
        } else {
            return false;
        }

        int requiredRank;
        if ("SENSITIVE".equals(privacyClass)) {
            requiredRank = 2;
        } else if ("PRIVATE".equals(privacyClass) || "INTERNAL".equals(privacyClass)) {
            requiredRank = 1;
        } else if ("PUBLIC".equals(privacyClass)) {
            requiredRank = 0;
        } else {
            return false;
        }

        return declaredRank >= requiredRank;
    }
}
