package com.rafgittools.governance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GovernanceGateTest {
    private fun route(
        authority: String? = "repo-producer",
        rights: RightsContext = RightsContext(),
        normative: List<NormativeRef> = emptyList()
    ) = GovernanceRoute(
        routeId = "GOV-TEST-1",
        sourceRef = "sha256:test",
        subject = "test",
        objectRef = "artifact:test",
        authority = authority,
        jurisdictionOrScope = "internal-test",
        normativeSources = normative,
        privacyClass = "test",
        rights = rights,
        evidenceRef = "evidence:test",
        receiptRef = "receipt:test",
        predecessor = null,
        supersedes = null,
        rollbackRef = null,
        replayRecipe = null,
        unresolvedGap = null,
        claimAllowed = false,
        hashRef = "sha256:test"
    )

    private fun pin(
        route: GovernanceRoute = route(),
        body: GateState = GateState.PASS,
        privacy: GateState = GateState.PASS,
        child: GateState = GateState.NOT_RUN,
        claimAllowed: Boolean = false,
        evidence: EvidenceRef = EvidenceRef("source", null, "run", "receipt", null)
    ) = GatePin(
        bodyState = body,
        soulState = GateState.AUDIT,
        spiritState = GateState.PASS,
        route = route,
        custody = emptyList(),
        evidence = evidence,
        reconstruction = Reconstruction(null, null, null, null, null, null, null, null, null, null),
        tailState = GateState.AUDIT,
        shadowState = GateState.AUDIT,
        frictionState = GateState.AUDIT,
        freestandingGate = GateState.NOT_RUN,
        authorshipGate = GateState.AUDIT,
        privacyGate = privacy,
        childSafetyGate = child,
        humanDignityGate = GateState.PASS,
        evidenceNeeded = emptyList(),
        claimAllowed = claimAllowed
    )

    @Test fun childSafetyFailsClosed() {
        val evaluated = ComputationalEthicsGate.evaluate(
            pin(route = route(rights = RightsContext(childSafetyRelevant = true)))
        )
        assertEquals(GateState.ROUTE_STATE_BLOCKED, evaluated.spiritState)
        assertFalse(evaluated.claimAllowed)
    }

    @Test fun technicalClaimCannotBeCreatedWithoutExecutionEvidence() {
        val evaluated = ComputationalEthicsGate.evaluate(
            pin(claimAllowed = true, evidence = EvidenceRef("source", null, null, null, "claim"))
        )
        assertEquals(GateState.ROUTE_STATE_BLOCKED, evaluated.spiritState)
        assertFalse(evaluated.claimAllowed)
    }

    @Test fun implementedUntestedNeverBecomesPassByEthics() {
        val evaluated = ComputationalEthicsGate.evaluate(pin(body = GateState.IMPLEMENTED_UNTESTED))
        assertEquals(GateState.IMPLEMENTED_UNTESTED, evaluated.bodyState)
        assertFalse(evaluated.claimAllowed)
    }

    @Test fun missingAuthorityBlocksRoute() {
        val evaluated = ComputationalEthicsGate.evaluate(pin(route = route(authority = null)))
        assertEquals(GateState.ROUTE_STATE_BLOCKED, evaluated.spiritState)
    }

    @Test fun reconstructionGapIsExplicit() {
        val evaluated = ComputationalEthicsGate.evaluate(pin())
        assertTrue(evaluated.evidenceNeeded.contains("TOKEN_VAZIO_RECONSTRUCTION"))
    }
}
