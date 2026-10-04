package com.rafgittools.navigator

/**
 * Executable protocol model for the ∆∅ → ΩΠμ grammar.
 *
 * This is a software contract, not a physics claim. It preserves the project invariants:
 * SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM
 * TOKEN_VAZIO != 0/null/empty/false
 * IMPLEMENTED_UNTESTED != PASS
 */
object DeltaEmptyOmegaPiMu {
    const val TOKEN_VAZIO = "TOKEN_VAZIO"
    const val SCHEMA_VERSION = "delta-empty-omega-pi-mu/v1"

    enum class EpistemicLayer {
        SOURCE,
        ARTIFACT,
        EXECUTION,
        EVIDENCE,
        CLAIM
    }

    enum class DeltaKind {
        SOURCE_DISCOVERED,
        NODE_ADDED,
        EDGE_ADDED,
        PROJECTION_ADDED,
        TOKEN_VAZIO_OPEN,
        TOKEN_VAZIO_CLOSE,
        EVIDENCE_BOUND,
        RECEIPT
    }

    data class Provenance(
        val sourceRef: String,
        val sourceSha256: String = TOKEN_VAZIO,
        val evidenceRef: String = TOKEN_VAZIO
    ) {
        init {
            require(sourceRef.isNotBlank()) { "sourceRef is required; unresolved source must be explicit upstream" }
            require(sourceSha256.isNotBlank()) { "sourceSha256 cannot collapse to empty" }
            require(evidenceRef.isNotBlank()) { "evidenceRef cannot collapse to empty" }
        }

        val hasEvidence: Boolean
            get() = evidenceRef != TOKEN_VAZIO
    }

    data class TokenVazio(
        val gapId: String,
        val reason: String,
        val expectedRef: String = TOKEN_VAZIO,
        val closureRoute: String = TOKEN_VAZIO,
        val open: Boolean = true,
        val evidenceRef: String = TOKEN_VAZIO
    ) {
        init {
            require(gapId.isNotBlank())
            require(reason.isNotBlank())
            require(expectedRef.isNotBlank())
            require(closureRoute.isNotBlank())
            require(evidenceRef.isNotBlank())
        }
    }

    data class Projection(
        val projectionId: String,
        val fromDomain: String,
        val toDomain: String,
        val lineage: List<Provenance>
    ) {
        init {
            require(projectionId.isNotBlank())
            require(fromDomain.isNotBlank())
            require(toDomain.isNotBlank())
            require(lineage.isNotEmpty()) { "projection must preserve provenance lineage" }
        }
    }

    data class Delta(
        val deltaId: String,
        val kind: DeltaKind,
        val muId: String,
        val provenance: Provenance,
        val gap: TokenVazio? = null,
        val projection: Projection? = null
    ) {
        init {
            require(deltaId.isNotBlank())
            require(muId.isNotBlank())
            if (kind == DeltaKind.TOKEN_VAZIO_OPEN || kind == DeltaKind.TOKEN_VAZIO_CLOSE) {
                require(gap != null) { "TOKEN_VAZIO delta requires a typed gap" }
            }
            if (kind == DeltaKind.PROJECTION_ADDED) {
                require(projection != null) { "PROJECTION_ADDED requires a projection" }
            }
        }
    }

    data class State(
        val omegaId: String,
        val generation: Long = 0,
        val deltas: List<Delta> = emptyList(),
        val projections: List<Projection> = emptyList(),
        val gaps: List<TokenVazio> = emptyList()
    ) {
        init {
            require(omegaId.isNotBlank())
            require(generation >= 0)
        }
    }

    /**
     * Narrow symbolic reduction recorded by the session.
     * Undefined reductions stay explicit instead of being guessed.
     */
    fun reduceSymbolic(raw: String): String =
        when (raw) {
            "0001123" -> "123"
            else -> TOKEN_VAZIO
        }

    /**
     * Pure append-only transition:
     *     S(t+1) = S(t) ⊕ ∆(t)
     *
     * Existing deltas are never rewritten or removed.
     */
    fun apply(state: State, delta: Delta): State {
        require(state.deltas.none { it.deltaId == delta.deltaId }) {
            "duplicate delta_id would break replay identity"
        }

        val nextGaps = when (delta.kind) {
            DeltaKind.TOKEN_VAZIO_OPEN -> {
                val gap = requireNotNull(delta.gap)
                require(state.gaps.none { it.gapId == gap.gapId && it.open }) {
                    "gap is already open"
                }
                state.gaps + gap.copy(open = true)
            }
            DeltaKind.TOKEN_VAZIO_CLOSE -> {
                val closure = requireNotNull(delta.gap)
                require(delta.provenance.hasEvidence) {
                    "closing TOKEN_VAZIO requires an evidenceRef"
                }
                var matched = false
                val updated = state.gaps.map {
                    if (it.gapId == closure.gapId && it.open) {
                        matched = true
                        it.copy(open = false, evidenceRef = delta.provenance.evidenceRef)
                    } else {
                        it
                    }
                }
                require(matched) { "cannot close a gap that is not open" }
                updated
            }
            else -> state.gaps
        }

        val nextProjections = when (delta.kind) {
            DeltaKind.PROJECTION_ADDED -> state.projections + requireNotNull(delta.projection)
            else -> state.projections
        }

        return state.copy(
            generation = state.generation + 1,
            deltas = state.deltas + delta,
            projections = nextProjections,
            gaps = nextGaps
        )
    }

    /**
     * Π(A→C) = Π(B→C) ∘ Π(A→B), only when the domains match.
     * Provenance is concatenated; composition never erases lineage.
     */
    fun compose(first: Projection, second: Projection, projectionId: String): Projection {
        require(first.toDomain == second.fromDomain) {
            "projection domains are not composable"
        }
        return Projection(
            projectionId = projectionId,
            fromDomain = first.fromDomain,
            toDomain = second.toDomain,
            lineage = first.lineage + second.lineage
        )
    }

    /**
     * A claim is eligible only when evidence is resolved and no gap is open.
     * Eligibility is not scientific truth; it is only a protocol gate.
     */
    fun claimEligible(provenance: Provenance, gaps: List<TokenVazio>): Boolean =
        provenance.hasEvidence && gaps.none { it.open }
}
