package com.rafgittools.navigator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeltaEmptyOmegaPiMuTest {
    private val source = DeltaEmptyOmegaPiMu.Provenance(sourceRef = "drive:file-1")

    @Test fun reductionIsNarrowAndUndefinedFormStaysVazio() {
        assertEquals("123", DeltaEmptyOmegaPiMu.reduceSymbolic("0001123"))
        assertEquals(DeltaEmptyOmegaPiMu.TOKEN_VAZIO, DeltaEmptyOmegaPiMu.reduceSymbolic("001123"))
    }

    @Test fun appendOnlyApplyIncrementsGenerationWithoutRewritingHistory() {
        val initial = DeltaEmptyOmegaPiMu.State(omegaId = "omega")
        val d1 = DeltaEmptyOmegaPiMu.Delta(
            deltaId = "d1",
            kind = DeltaEmptyOmegaPiMu.DeltaKind.SOURCE_DISCOVERED,
            muId = "mu-1",
            provenance = source
        )
        val s1 = DeltaEmptyOmegaPiMu.apply(initial, d1)
        assertEquals(0L, initial.generation)
        assertTrue(initial.deltas.isEmpty())
        assertEquals(1L, s1.generation)
        assertEquals(listOf("d1"), s1.deltas.map { it.deltaId })
        assertFalse(runCatching { DeltaEmptyOmegaPiMu.apply(s1, d1) }.isSuccess)
    }

    @Test fun tokenVazioCannotCloseWithoutEvidence() {
        val gap = DeltaEmptyOmegaPiMu.TokenVazio(
            gapId = "g1",
            reason = "evidence unresolved",
            expectedRef = "evidence:e1",
            closureRoute = "route:evidence"
        )
        val opened = DeltaEmptyOmegaPiMu.apply(
            DeltaEmptyOmegaPiMu.State(omegaId = "omega"),
            DeltaEmptyOmegaPiMu.Delta(
                deltaId = "open",
                kind = DeltaEmptyOmegaPiMu.DeltaKind.TOKEN_VAZIO_OPEN,
                muId = "mu",
                provenance = source,
                gap = gap
            )
        )
        assertTrue(opened.gaps.single().open)
        assertFalse(
            runCatching {
                DeltaEmptyOmegaPiMu.apply(
                    opened,
                    DeltaEmptyOmegaPiMu.Delta(
                        deltaId = "close-without-evidence",
                        kind = DeltaEmptyOmegaPiMu.DeltaKind.TOKEN_VAZIO_CLOSE,
                        muId = "mu",
                        provenance = source,
                        gap = gap
                    )
                )
            }.isSuccess
        )
    }

    @Test fun tokenVazioClosesWithEvidenceAndClaimGateCanOpen() {
        val gap = DeltaEmptyOmegaPiMu.TokenVazio(
            gapId = "g1",
            reason = "evidence unresolved",
            expectedRef = "evidence:e1",
            closureRoute = "route:evidence"
        )
        val opened = DeltaEmptyOmegaPiMu.apply(
            DeltaEmptyOmegaPiMu.State(omegaId = "omega"),
            DeltaEmptyOmegaPiMu.Delta(
                deltaId = "open",
                kind = DeltaEmptyOmegaPiMu.DeltaKind.TOKEN_VAZIO_OPEN,
                muId = "mu",
                provenance = source,
                gap = gap
            )
        )
        val evidenced = DeltaEmptyOmegaPiMu.Provenance(
            sourceRef = "drive:file-1",
            evidenceRef = "evidence:e1"
        )
        val closed = DeltaEmptyOmegaPiMu.apply(
            opened,
            DeltaEmptyOmegaPiMu.Delta(
                deltaId = "close",
                kind = DeltaEmptyOmegaPiMu.DeltaKind.TOKEN_VAZIO_CLOSE,
                muId = "mu",
                provenance = evidenced,
                gap = gap
            )
        )
        assertFalse(closed.gaps.single().open)
        assertTrue(DeltaEmptyOmegaPiMu.claimEligible(evidenced, closed.gaps))
        assertFalse(DeltaEmptyOmegaPiMu.claimEligible(source, closed.gaps))
    }

    @Test fun projectionCompositionPreservesBothProvenanceSegments() {
        val p1 = DeltaEmptyOmegaPiMu.Projection(
            projectionId = "p1",
            fromDomain = "physics",
            toDomain = "math",
            lineage = listOf(DeltaEmptyOmegaPiMu.Provenance("source:physics"))
        )
        val p2 = DeltaEmptyOmegaPiMu.Projection(
            projectionId = "p2",
            fromDomain = "math",
            toDomain = "compute",
            lineage = listOf(DeltaEmptyOmegaPiMu.Provenance("source:math"))
        )
        val p3 = DeltaEmptyOmegaPiMu.compose(p1, p2, "p3")
        assertEquals("physics", p3.fromDomain)
        assertEquals("compute", p3.toDomain)
        assertEquals(listOf("source:physics", "source:math"), p3.lineage.map { it.sourceRef })
    }
}
