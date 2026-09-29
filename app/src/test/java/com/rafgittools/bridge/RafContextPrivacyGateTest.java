package com.rafgittools.bridge;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public final class RafContextPrivacyGateTest {
    @Test
    public void public_context_allows_all_declared_classes() {
        assertThat(RafContextPrivacyGate.allows("public", "PUBLIC")).isTrue();
        assertThat(RafContextPrivacyGate.allows("private", "PUBLIC")).isTrue();
        assertThat(RafContextPrivacyGate.allows("sensitive", "PUBLIC")).isTrue();
    }

    @Test
    public void private_context_cannot_be_downgraded_to_public() {
        assertThat(RafContextPrivacyGate.allows("public", "PRIVATE")).isFalse();
        assertThat(RafContextPrivacyGate.allows("private", "PRIVATE")).isTrue();
        assertThat(RafContextPrivacyGate.allows("sensitive", "PRIVATE")).isTrue();
    }

    @Test
    public void sensitive_context_requires_sensitive_class() {
        assertThat(RafContextPrivacyGate.allows("public", "SENSITIVE")).isFalse();
        assertThat(RafContextPrivacyGate.allows("private", "SENSITIVE")).isFalse();
        assertThat(RafContextPrivacyGate.allows("sensitive", "SENSITIVE")).isTrue();
    }

    @Test
    public void token_vazio_privacy_always_blocks() {
        assertThat(RafContextPrivacyGate.allows("sensitive", "TOKEN_VAZIO")).isFalse();
    }

    @Test
    public void unknown_classes_fail_closed() {
        assertThat(RafContextPrivacyGate.allows("unknown", "PUBLIC")).isFalse();
        assertThat(RafContextPrivacyGate.allows("private", "UNKNOWN")).isFalse();
    }
}
