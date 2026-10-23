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

    @Test fun avalancheIsBoundedAndNotCausal() {
        val n = listOf(
            node("NODE_001", listOf("a", "b")),
            node("NODE_002", listOf("b", "c")),
            node("NODE_003", listOf("c", "d")),
            node("NODE_004", listOf("d", "e"))
        )
        assertEquals(1, FederatedLensCore.lexicalReach(n, "NODE_001", 0).size)
        assertEquals(4, FederatedLensCore.lexicalReach(n, "NODE_001", 3).size)
        try {
            FederatedLensCore.lexicalReach(n, "NODE_001", 4)
            fail("unbounded depth")
        } catch (_: IllegalArgumentException) { }
    }

    @Test fun capacityBoundaryDistinguishesCompleteFromTruncated() {
        val pair = listOf(node("NODE_001", listOf("a")), node("NODE_002", listOf("a")))
        assertFalse(FederatedLensCore.candidateResult(pair, 1).truncated)
        assertTrue(FederatedLensCore.candidateResult(pair, 0).truncated)
        assertFalse(FederatedLensCore.candidateResult(listOf(pair[0]), 0).truncated)
        val dense = (0 until 12).map { node("NODE_" + it.toString().padStart(3, '0'), listOf("a")) }
        val preview = FederatedLensCore.candidateResult(dense)
        assertEquals(64, preview.edges.size)
        assertTrue(preview.truncated) // 12 choose 2 = 66, not 64.
        try {
            FederatedLensCore.lexicalReach(dense, "NODE_000")
            fail("partial graph must not support a complete reachability claim")
        } catch (_: IllegalArgumentException) { }
    }

    @Test fun duplicatesFailClosed() {
        try {
            FederatedLensCore.candidates(listOf(node("NODE_001", listOf("mapa")),
                node("NODE_001", listOf("etica"))))
            fail("duplicate ID")
        } catch (_: IllegalArgumentException) { }
    }
}
