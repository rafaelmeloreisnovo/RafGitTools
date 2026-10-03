package com.rafgittools.governance

/**
 * RAFAELIA governance kernel.
 *
 * Invariants:
 * SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM
 * TOKEN_VAZIO != 0
 * IMPLEMENTED_UNTESTED != PASS
 *
 * SPIRIT constrains action; it never manufactures technical evidence.
 */
enum class GateState {
    PASS, FAIL, NOT_RUN, PENDING, AUDIT, TOKEN_VAZIO,
    IMPLEMENTED_UNTESTED, OBSERVED_UNPROMOTED, ROUTE_STATE_BLOCKED
}

enum class BindingForce { BINDING, GUIDANCE, STANDARD, INTERNAL_POLICY, PERSPECTIVE }
enum class OriginClass { CLEAN_AUTHORIAL_COMPONENT_VERIFIED, THIRD_PARTY_DERIVED, MIXED_ORIGIN, TOKEN_VAZIO_ORIGIN }
enum class Boundary { CORE_FREESTANDING, ADAPTER_PLATFORM, TOKEN_VAZIO }

data class EvidenceRef(
    val sourceRef: String?,
    val artifactRef: String?,
    val executionRef: String?,
    val evidenceRef: String?,
    val claimRef: String?
) {
    fun hasTechnicalEvidence(): Boolean = !executionRef.isNullOrBlank() && !evidenceRef.isNullOrBlank()
}

data class NormativeRef(
    val standardId: String,
    val versionOrDate: String,
    val clause: String?,
    val bindingForce: BindingForce,
    val applicability: String,
    val evidence: String?,
    val gap: String?,
    val owner: String?
)

data class RightsContext(
    val privacyRelevant: Boolean = false,
    val childSafetyRelevant: Boolean = false,
    val humanRightsRelevant: Boolean = false,
    val environmentalRelevant: Boolean = false,
    val humanOversightRequired: Boolean = false,
    val consentRequired: Boolean = false
)

data class Custody(
    val language: String,
    val origin: OriginClass,
    val owner: String?,
    val licenseRef: String?,
    val transformationChainRef: String?,
    val dependencyGraphRef: String?,
    val ffiJniRef: String?,
    val generatorsScriptsRef: String?,
    val boundary: Boundary
)

data class Reconstruction(
    val beforeState: String?,
    val afterState: String?,
    val exactRef: String?,
    val expectedObservable: String?,
    val actualObservable: String?,
    val falsifier: String?,
    val replayRecipe: String?,
    val rollbackProcedure: String?,
    val rollbackTest: String?,
    val reconstructionMinimum: String?
) {
    fun demonstrated(): Boolean = listOf(
        beforeState, afterState, exactRef, expectedObservable, actualObservable,
        falsifier, replayRecipe, rollbackProcedure, rollbackTest, reconstructionMinimum
    ).all { !it.isNullOrBlank() }
}

data class GovernanceRoute(
    val routeId: String,
    val sourceRef: String,
    val subject: String,
    val objectRef: String,
    val authority: String?,
    val jurisdictionOrScope: String?,
    val normativeSources: List<NormativeRef>,
    val privacyClass: String?,
    val rights: RightsContext,
    val evidenceRef: String?,
    val receiptRef: String?,
    val predecessor: String?,
    val supersedes: String?,
    val rollbackRef: String?,
    val replayRecipe: String?,
    val unresolvedGap: String?,
    val claimAllowed: Boolean,
    val hashRef: String?
)

data class GatePin(
    val bodyState: GateState,
    val soulState: GateState,
    val spiritState: GateState,
    val route: GovernanceRoute,
    val custody: List<Custody>,
    val evidence: EvidenceRef,
    val reconstruction: Reconstruction,
    val tailState: GateState,
    val shadowState: GateState,
    val frictionState: GateState,
    val freestandingGate: GateState,
    val authorshipGate: GateState,
    val privacyGate: GateState,
    val childSafetyGate: GateState,
    val humanDignityGate: GateState,
    val evidenceNeeded: List<String>,
    val claimAllowed: Boolean
)

object ComputationalEthicsGate {
    /** Fail closed on missing authority, evidence, child-safety controls or reconstruction. */
    fun evaluate(pin: GatePin): GatePin {
        val rights = pin.route.rights
        val missingAuthority = pin.route.authority.isNullOrBlank()
        val missingNormativeApplicability = pin.route.normativeSources.any { it.applicability.isBlank() }
        val childUnsafe = rights.childSafetyRelevant && pin.childSafetyGate != GateState.PASS
        val privacyUnsafe = rights.privacyRelevant && pin.privacyGate !in setOf(GateState.PASS, GateState.AUDIT)
        val technicalClaimWithoutEvidence = pin.claimAllowed && !pin.evidence.hasTechnicalEvidence()
        val reconstructed = pin.reconstruction.demonstrated()

        val blocked = missingAuthority || missingNormativeApplicability || childUnsafe ||
            privacyUnsafe || technicalClaimWithoutEvidence

        return pin.copy(
            spiritState = if (blocked) GateState.ROUTE_STATE_BLOCKED else pin.spiritState,
            claimAllowed = pin.claimAllowed && !blocked && pin.bodyState == GateState.PASS,
            evidenceNeeded = pin.evidenceNeeded + if (!reconstructed) listOf("TOKEN_VAZIO_RECONSTRUCTION") else emptyList()
        )
    }

    /** Explicit vocabulary: descriptive, never accusatory. */
    const val TAIL = "transitive chain or post-step"
    const val SHADOW = "implicit or duplicated path/implementation"
    const val FRICTION = "necessary external boundary"
    const val TOKEN_VAZIO_NORMATIVE = "TOKEN_VAZIO_NORMATIVE"
}
