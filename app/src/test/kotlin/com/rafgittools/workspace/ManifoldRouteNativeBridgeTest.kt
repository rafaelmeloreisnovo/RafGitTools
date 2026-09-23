package com.rafgittools.workspace

import com.google.common.truth.Truth.assertThat
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertThrows
import org.junit.Test

class ManifoldRouteNativeBridgeTest {
    @Test
    fun encodeRequest_matches_32_byte_little_endian_contract() {
        val frame = ManifoldRouteNativeBridge.encodeRequest(
            triggerCode = 4,
            sourceIdentity = intArrayOf(1, 2, 3, 4),
            ambiguous = false,
            expansionRequired = true
        )

        assertThat(frame.remaining()).isEqualTo(32)
        assertThat(frame.int).isEqualTo(ManifoldRouteNativeBridge.REQUEST_MAGIC)
        assertThat(frame.short.toInt() and 0xffff).isEqualTo(1)
        assertThat(frame.short.toInt() and 0xffff).isEqualTo(4)
        assertThat(frame.int).isEqualTo(
            ManifoldRouteNativeBridge.FLAG_SOURCE_BOUND or
                ManifoldRouteNativeBridge.FLAG_EXPANSION_REQUIRED
        )
        assertThat(frame.int).isEqualTo(1)
        assertThat(frame.int).isEqualTo(2)
        assertThat(frame.int).isEqualTo(3)
        assertThat(frame.int).isEqualTo(4)
        assertThat(frame.int).isEqualTo(0)
    }

    @Test
    fun decodeReceipt_promotes_only_successful_v1_receipt_to_binding() {
        val receipt = receipt(
            status = 0,
            errorMask = 0,
            trigger = 7,
            route = 7,
            tag = 0x1234abcd,
            sourceFold = 0
        )

        val result = ManifoldRouteNativeBridge.decodeReceipt(
            receipt,
            "Mapa:data/manifold/routes_omega_v1.jsonl"
        )

        val resolved = result as NativeRouteResult.Resolved
        assertThat(resolved.binding.routeId).isEqualTo("R0007")
        assertThat(resolved.binding.routeTagHex).isEqualTo("1234abcd")
        assertThat(resolved.sourceFoldHex).isEqualTo("00000000")
    }

    @Test
    fun decodeReceipt_preserves_native_rejection() {
        val receipt = receipt(
            status = 1,
            errorMask = 0x10,
            trigger = 7,
            route = 0,
            tag = 0,
            sourceFold = 0
        )

        val result = ManifoldRouteNativeBridge.decodeReceipt(
            receipt,
            "Mapa:data/manifold/routes_omega_v1.jsonl"
        )

        val rejected = result as NativeRouteResult.Rejected
        assertThat(rejected.errorMask).isEqualTo(0x10)
        assertThat(rejected.triggerCode).isEqualTo(7)
    }

    @Test
    fun decodeReceipt_rejects_mapping_drift_as_protocol_error() {
        val receipt = receipt(
            status = 0,
            errorMask = 0,
            trigger = 4,
            route = 5,
            tag = 0x01020304,
            sourceFold = 0x11111111
        )

        val result = ManifoldRouteNativeBridge.decodeReceipt(
            receipt,
            "Mapa:data/manifold/routes_omega_v1.jsonl"
        )

        assertThat(result).isInstanceOf(NativeRouteResult.ProtocolError::class.java)
    }

    @Test
    fun encodeRequest_requires_exact_source_identity_width() {
        assertThrows(IllegalArgumentException::class.java) {
            ManifoldRouteNativeBridge.encodeRequest(
                triggerCode = 4,
                sourceIdentity = intArrayOf(1, 2, 3),
                ambiguous = false,
                expansionRequired = false
            )
        }
    }

    @Test
    fun availability_query_never_claims_device_execution() {
        val result = runCatching { ManifoldRouteNativeBridge.isAvailable() }
        assertThat(result.isSuccess).isTrue()
    }

    private fun receipt(
        status: Int,
        errorMask: Int,
        trigger: Int,
        route: Int,
        tag: Int,
        sourceFold: Int
    ): ByteBuffer = ByteBuffer.allocateDirect(32)
        .order(ByteOrder.LITTLE_ENDIAN)
        .apply {
            putInt(ManifoldRouteNativeBridge.RECEIPT_MAGIC)
            putShort(1)
            putShort(status.toShort())
            putInt(errorMask)
            putInt(trigger)
            putInt(route)
            putInt(tag)
            putInt(0)
            putInt(sourceFold)
            flip()
        }
}
