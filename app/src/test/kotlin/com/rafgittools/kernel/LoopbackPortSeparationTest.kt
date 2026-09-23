package com.rafgittools.kernel

import com.google.common.truth.Truth.assertThat
import com.rafgittools.bridge.RafBridgePrefs
import org.junit.Test

class LoopbackPortSeparationTest {
    @Test
    fun rafBridgeAndTermuxHealthUseDifferentPorts() {
        assertThat(RafBridgePrefs.BRIDGE_PORT).isEqualTo(8765)
        assertThat(TermuxHealthProbe.DEFAULT_PORT).isEqualTo(8766)
        assertThat(TermuxHealthProbe.DEFAULT_PORT).isNotEqualTo(RafBridgePrefs.BRIDGE_PORT)
    }

    @Test
    fun termuxDefaultEndpointUsesDedicatedHealthPort() {
        assertThat(TermuxHealthProbe.DEFAULT_ENDPOINT)
            .isEqualTo("http://127.0.0.1:8766/health")
    }
}
