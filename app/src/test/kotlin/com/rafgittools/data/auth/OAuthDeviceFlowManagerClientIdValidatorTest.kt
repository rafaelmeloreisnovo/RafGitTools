package com.rafgittools.data.auth

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class OAuthDeviceFlowManagerClientIdValidatorTest {

    @Test
    fun isConfiguredClientId_rejectsBlankValues() {
        assertThat(isConfiguredClientId("")).isFalse()
        assertThat(isConfiguredClientId("   ")).isFalse()
    }

    @Test
    fun isConfiguredClientId_rejectsLocalPrefixValues() {
        assertThat(isConfiguredClientId("local-dev-client-id")).isFalse()
        assertThat(isConfiguredClientId("LOCAL-custom-id")).isFalse()
    }

    @Test
    fun isConfiguredClientId_rejectsKnownPlaceholders() {
        assertThat(isConfiguredClientId("your-client-id")).isFalse()
        assertThat(isConfiguredClientId("your_github_client_id")).isFalse()
        assertThat(isConfiguredClientId("placeholder")).isFalse()
        assertThat(isConfiguredClientId("changeme")).isFalse()
        assertThat(isConfiguredClientId("replace-me")).isFalse()
    }

    @Test
    fun isConfiguredClientId_acceptsConfiguredValue() {
        assertThat(isConfiguredClientId("Ov23liKzArealClientId")).isTrue()
    }
}
