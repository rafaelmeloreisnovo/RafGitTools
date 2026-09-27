package com.rafgittools.library.processing

/**
 * Adapter from the measured rmrCti-style fields into the nine-head rigor
 * contract. This does not reproduce omega_neuro_full's project-specific
 * constants and does not treat its symbolic fields as evidence.
 */
data class RmrCtiNeurometricSnapshotV1(
    val entropyMilli: Int?,
    val semanticGapMilli: Int?,
    val logicDepthPerByteQ16: Int?,
    val hammingPerBitQ16: Int?,
    val technicalDensityQ16: Int?,
    val latentTokenDensityQ16: Int?,
    val crossEntropyMilli: Int?,
    val noiseFloorMilli: Int?,
    val forestPath: RmrCtiForestPath,
    val curatedTop: Boolean
)

object RmrCtiRigorAdapterV1 {
    fun applyMeasuredPressure(
        base: RigorBiasInputV1,
        snapshot: RmrCtiNeurometricSnapshotV1
    ): RigorBiasInputV1 {
        val novelty = maxKnownQ16(
            base.noveltyGapPressure,
            milliToQ16(snapshot.semanticGapMilli, 8000),
            milliToQ16(snapshot.crossEntropyMilli, 8000),
            snapshot.hammingPerBitQ16?.let(RigorChannelQ16::known)
        )

        val structureSignal = maxKnownQ16(
            base.structureCompleteness,
            snapshot.technicalDensityQ16?.let(RigorChannelQ16::known),
            snapshot.logicDepthPerByteQ16?.let(RigorChannelQ16::known)
        )

        val multimodalSignal = maxKnownQ16(
            base.multimodalCompleteness,
            snapshot.latentTokenDensityQ16?.let(RigorChannelQ16::known)
        )

        val contradiction = maxKnownQ16(
            base.contradictionPressure,
            milliToQ16(snapshot.noiseFloorMilli, 1000)
        )

        return base.copy(
            structureCompleteness = structureSignal,
            noveltyGapPressure = novelty,
            multimodalCompleteness = multimodalSignal,
            contradictionPressure = contradiction,
            forestPath = snapshot.forestPath,
            curatedTop = snapshot.curatedTop
        )
    }

    private fun maxKnownQ16(
        first: RigorChannelQ16,
        vararg rest: RigorChannelQ16?
    ): RigorChannelQ16 {
        val known = (listOf(first) + rest.filterNotNull())
            .filter { it.state == RigorChannelState.KNOWN }
        if (known.isEmpty()) return RigorChannelQ16.tokenVazio()
        return RigorChannelQ16.known(known.maxOf { it.valueQ16 })
    }

    private fun milliToQ16(value: Int?, maxMilli: Int): RigorChannelQ16? {
        if (value == null) return null
        val normalized = (value.toLong().coerceAtLeast(0L) * RIGOR_Q16_ONE) /
            maxMilli.coerceAtLeast(1).toLong()
        return RigorChannelQ16.known(
            normalized.coerceIn(0L, RIGOR_Q16_ONE.toLong()).toInt()
        )
    }
}
