package com.rafgittools.workspace

import java.nio.ByteBuffer
import java.nio.ByteOrder

sealed class NativeRouteResult {
    data class Resolved(
        val binding: ManifoldRouteBinding,
        val sourceFoldHex: String
    ) : NativeRouteResult()

    data class Rejected(
        val errorMask: Int,
        val triggerCode: Int
    ) : NativeRouteResult()

    data class Unavailable(val reason: String) : NativeRouteResult()

    data class ProtocolError(val reason: String) : NativeRouteResult()
}

/**
 * Hosted JNI boundary for the independently audited freestanding rafcode_route_v1 core.
 *
 * This wrapper owns byte packing only. It does not parse user text, select authority,
 * read sources, access providers or promote claims.
 */
object ManifoldRouteNativeBridge {
    internal const val REQUEST_MAGIC = 0x31515252
    internal const val RECEIPT_MAGIC = 0x31505252
    internal const val VERSION = 1
    internal const val FRAME_BYTES = 32

    internal const val FLAG_SOURCE_BOUND = 0x01
    internal const val FLAG_AMBIGUOUS = 0x02
    internal const val FLAG_EXPANSION_REQUIRED = 0x04

    private val loaded: Boolean = runCatching {
        System.loadLibrary("rafroute")
        true
    }.getOrDefault(false)

    fun isAvailable(): Boolean = loaded

    fun resolve(
        triggerCode: Int,
        sourceIdentity: IntArray,
        authorityRef: String,
        ambiguous: Boolean = false,
        expansionRequired: Boolean = false
    ): NativeRouteResult {
        if (!loaded) {
            return NativeRouteResult.Unavailable("librafroute unavailable")
        }

        val request = encodeRequest(
            triggerCode = triggerCode,
            sourceIdentity = sourceIdentity,
            ambiguous = ambiguous,
            expansionRequired = expansionRequired
        )
        val receipt = ByteBuffer.allocateDirect(FRAME_BYTES).order(ByteOrder.LITTLE_ENDIAN)

        val nativeStatus = runCatching {
            resolveNative(request, receipt)
        }.getOrElse {
            return NativeRouteResult.Unavailable("native invocation failed")
        }

        if (nativeStatus < 0) {
            return NativeRouteResult.ProtocolError("native buffer contract rejected")
        }

        receipt.position(0)
        return decodeReceipt(receipt, authorityRef)
    }

    internal fun encodeRequest(
        triggerCode: Int,
        sourceIdentity: IntArray,
        ambiguous: Boolean,
        expansionRequired: Boolean
    ): ByteBuffer {
        require(sourceIdentity.size == 4) { "sourceIdentity must contain exactly four words" }

        var flags = FLAG_SOURCE_BOUND
        if (ambiguous) flags = flags or FLAG_AMBIGUOUS
        if (expansionRequired) flags = flags or FLAG_EXPANSION_REQUIRED

        return ByteBuffer.allocateDirect(FRAME_BYTES)
            .order(ByteOrder.LITTLE_ENDIAN)
            .apply {
                putInt(REQUEST_MAGIC)
                putShort(VERSION.toShort())
                putShort(triggerCode.toShort())
                putInt(flags)
                sourceIdentity.forEach(::putInt)
                putInt(0)
                flip()
            }
    }

    internal fun decodeReceipt(
        receipt: ByteBuffer,
        authorityRef: String
    ): NativeRouteResult {
        require(authorityRef.isNotBlank()) { "authorityRef must not be blank" }

        val buffer = receipt.duplicate().order(ByteOrder.LITTLE_ENDIAN)
        if (buffer.remaining() < FRAME_BYTES) {
            return NativeRouteResult.ProtocolError("receipt shorter than 32 bytes")
        }

        val magic = buffer.int
        val version = buffer.short.toInt() and 0xffff
        val status = buffer.short.toInt() and 0xffff
        val errorMask = buffer.int
        val triggerCode = buffer.int
        val routeCode = buffer.int
        val routeTag = buffer.int
        buffer.int // safe flags are preserved by native receipt; not promoted here.
        val sourceFold = buffer.int

        if (magic != RECEIPT_MAGIC || version != VERSION) {
            return NativeRouteResult.ProtocolError("receipt magic/version mismatch")
        }

        if (status != 0) {
            return NativeRouteResult.Rejected(
                errorMask = errorMask,
                triggerCode = triggerCode
            )
        }

        val routeTagHex = Integer.toUnsignedString(routeTag, 16).padStart(8, '0')
        val sourceFoldHex = Integer.toUnsignedString(sourceFold, 16).padStart(8, '0')

        return runCatching {
            NativeRouteResult.Resolved(
                binding = ManifoldRouteBinding(
                    triggerCode = triggerCode,
                    routeCode = routeCode,
                    routeTagHex = routeTagHex,
                    authorityRef = authorityRef
                ),
                sourceFoldHex = sourceFoldHex
            )
        }.getOrElse {
            NativeRouteResult.ProtocolError("receipt violates V1 binding contract")
        }
    }

    private external fun resolveNative(
        request: ByteBuffer,
        receipt: ByteBuffer
    ): Int
}
