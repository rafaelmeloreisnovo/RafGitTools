package com.rafgittools.lens

import org.junit.Assert.*
import org.junit.Test

class FederatedLensCoreTest {
    private fun node(id: String, tokens: List<String>, gap: LensGap = LensGap.TOKEN_VAZIO) =
        LensNode(id, LensSource.DRIVE_PRIVATE, tokens, gap)

    @Test fun deterministicPermutationAndBounds() {
        val a = node("SHARD_000", listOf("custodia", "mapa", "etica"))
        val b = node("SHARD_000", listOf("etica", "custodia", "mapa"))
        assertEquals(FederatedLensCore.vectorize(a), FederatedLensCore.vectorize(b))
        assertEquals(64, FederatedLensCore.vectorize(a).counts.size)
        assertEquals(3, FederatedLensCore.vectorize(a).counts.sum())
    }

    @Test fun noPrivateBodyOrSecretToken() {
        for (bad in listOf("texto bruto", "sk-proj-SECRET", "drive/file/id/123", "email@example.com")) {
            try {
                FederatedLensCore.vectorize(node("SHARD_001", listOf(bad)))
                fail("token should be rejected: $bad")
            } catch (_: IllegalArgumentException) { }
        }
    }

    @Test fun typedUnknownIsNotPass() {
        assertEquals("ROUTE_STATE_BLOCKED", FederatedLensCore.operationGate(node("SHARD_002", listOf("mapa"))))
        assertFalse(FederatedLensCore.CLAIM_ALLOWED)
        assertEquals("METADATA_PREVIEW_ONLY",
            FederatedLensCore.operationGate(node("SHARD_003", listOf("mapa"), LensGap.EVIDENCED_SCOPED)))
    }

    @Test fun boundedLexicalRelationsOnly() {
        val a = node("NODE_001", listOf("custodia", "mapa"))
        val b = node("NODE_002", listOf("custodia", "paper"))
        val c = node("NODE_003", listOf("etica"))
        val edges = FederatedLensCore.candidates(listOf(a, b, c))
        assertEquals(1, edges.size)
        assertEquals("LEXICAL_CANDIDATE_NOT_CAUSAL", edges[0].relation)
        assertEquals(1, edges[0].commonTokens)
        assertEquals(3, edges[0].unionTokens)
        assertTrue(FederatedLensCore.candidates(listOf(a, b), budget = 0).isEmpty())
    }

    @Test fun duplicatesFailClosed() {
        try {
            FederatedLensCore.candidates(listOf(node("NODE_001", listOf("mapa")),
                node("NODE_001", listOf("etica"))))
            fail("duplicate ID")
        } catch (_: IllegalArgumentException) { }
    }
}
