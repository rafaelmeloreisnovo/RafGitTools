package com.rafgittools.data.auth

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class OAuthDeviceFlowManagerValidationTest {

    @Test
    fun `isConfiguredClientId rejects blank values`() {
        assertThat(isConfiguredClientId("")).isFalse()
        assertThat(isConfiguredClientId("   ")).isFalse()
    }

    @Test
    fun `isConfiguredClientId rejects local prefix`() {
        assertThat(isConfiguredClientId("local-dev-client-id")).isFalse()
        assertThat(isConfiguredClientId("LOCAL-custom-client")).isFalse()
    }

    @Test
    fun `isConfiguredClientId rejects known placeholders`() {
        assertThat(isConfiguredClientId("your-client-id")).isFalse()
        assertThat(isConfiguredClientId("your_github_client_id")).isFalse()
        assertThat(isConfiguredClientId("changeme")).isFalse()
    }

    @Test
    fun `isConfiguredClientId accepts configured value`() {
        assertThat(isConfiguredClientId("Iv1.1234567890abcdef")).isTrue()
    }
}
