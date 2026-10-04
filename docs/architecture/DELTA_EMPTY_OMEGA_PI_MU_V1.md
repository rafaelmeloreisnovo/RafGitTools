# ∆∅ → ΩΠμ — executable grammar V1

**Status:** IMPLEMENTED_UNTESTED until CI evidence  
**Claim gate:** claim_allowed=false  
**Scope:** software protocol only; no physics-law claim.

## Purpose

Encode the session grammar as deterministic, replay-friendly software primitives without collapsing gaps into zero/null/empty/false and without conflating source, evidence and claim.

## Executable invariants

```text
SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM
TOKEN_VAZIO != 0/null/empty/false
IMPLEMENTED_UNTESTED != PASS
```

State evolution:

```text
S(t+1) = S(t) ⊕ ∆(t)
```

Projection composition:

```text
Π(A→C) = Π(B→C) ∘ Π(A→B)
```

only if domains compose. Provenance lineage is concatenated, never replaced.

## Narrow symbolic rule

The implementation intentionally defines only:

```text
red(0001123) = 123
```

Any other symbolic reduction returns `TOKEN_VAZIO`. This prevents the code from inventing a generalized arithmetic law not defined by the source material.

## TOKEN_VAZIO lifecycle

`TOKEN_VAZIO_OPEN` creates a typed gap with:
- `gapId`
- `reason`
- `expectedRef`
- `closureRoute`

`TOKEN_VAZIO_CLOSE` requires an `evidenceRef`; a missing evidence pointer cannot close the gap.

## Claim eligibility

`claimEligible` is only a protocol gate:

```text
evidence resolved AND no open gaps
```

It is **not** a truth predicate and does not establish scientific validity.

## Prior-art boundary

The companion RLL research note records an initial alphaXiv screen:
- arXiv:2606.15246 — provenance/factual commitment in knowledge graphs;
- arXiv:2608.29606 — provenance-aware layered long-term memory;
- arXiv:2602.23193 — event sourcing/replay/materialized projections.

Those mechanisms are treated as known antecedents. The exact `∆∅ → ΩΠμ` grammar remains `CANDIDATE_AUTHORIAL_COMPOSITION / NEEDS_BROADER_SEARCH`.

## Files

- `DeltaEmptyOmegaPiMu.kt` — pure protocol model.
- `DeltaEmptyOmegaPiMuTest.kt` — deterministic unit tests.
- RLL prior-art note — academic boundary and citations.
- JSON Schema — provider-neutral interchange contract.
