package com.rafgittools.workspace

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class ManifoldRouteNativeBridgeInstrumentedTest {

    @Test
    fun native_route_library_loads_and_resolves_code_runtime() {
        assertThat(ManifoldRouteNativeBridge.isAvailable()).isTrue()

        val result = ManifoldRouteNativeBridge.resolve(
            triggerCode = 4,
            sourceIdentity = intArrayOf(
                0x11111111,
                0x22222222,
                0x44444444,
                0x88888888.toInt()
            ),
            authorityRef = "Mapa:data/manifold/routes_omega_v1.jsonl"
        )

        val resolved = result as NativeRouteResult.Resolved
        assertThat(resolved.binding.routeId).isEqualTo("R0004")
        assertThat(resolved.binding.routeCode).isEqualTo(4)
        assertThat(resolved.binding.triggerCode).isEqualTo(4)
        assertThat(resolved.binding.routeTagHex).hasLength(8)
    }

    @Test
    fun native_route_library_rejects_ambiguous_trigger_fail_closed() {
        assertThat(ManifoldRouteNativeBridge.isAvailable()).isTrue()

        val result = ManifoldRouteNativeBridge.resolve(
            triggerCode = 7,
            sourceIdentity = intArrayOf(
                0x10203040,
                0x50607080,
                0x90a0b0c0.toInt(),
                0xd0e0f001.toInt()
            ),
            authorityRef = "Mapa:data/manifold/routes_omega_v1.jsonl",
            ambiguous = true
        )

        val rejected = result as NativeRouteResult.Rejected
        assertThat(rejected.triggerCode).isEqualTo(7)
        assertThat(rejected.errorMask and 0x10).isNotEqualTo(0)
    }
}
