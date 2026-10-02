# Conversation Relation Vector + μDelta Manifold V1

State: `IMPLEMENTED_UNTESTED`  
Authority: `RafGitTools/tools/corpus_logistics`  
Source authority: governed output of `corpus_logistics_gymnasia_v1.py`  
Claim gate: `claim_allowed=false`

## Intent

Add a non-destructive relation/vector layer over existing conversation chunks without
duplicating raw conversation bodies.

Existing pipeline remains authoritative:

```text
private normalized records
→ corpus_logistics_gymnasia_v1.py
→ content-addressed CHK-* chunks
→ token_refs + typed edges
→ conversation_relation_vector_v1.py
→ VEC-* structural vectors
→ MDELTA-* adjacent micro-deltas
```

## What V1 measures

For each chunk:

- source/book/session identity;
- text SHA-256 pointer;
- bytes and token count;
- token-signature SHA-256;
- in/out relation counts;
- NEXT/PREVIOUS counts;
- tier and materialization state.

For each `NEXT` edge (n → n+1):

- token-set Jaccard;
- lexical divergence (1 - Jaccard);
- tokens added/removed;
- token-count delta;
- byte delta.

## Fail-closed semantic boundary

These values are intentionally **not inferred** in V1:

- semantic embedding;
- causal relation;
- truth state;
- scientific validity;
- claim promotion.

They remain `TOKEN_VAZIO`.

```text
LEXICAL_OVERLAP != SEMANTIC_EQUIVALENCE
CONVERGENCE != TRUTH
DIVERGENCE != ERROR
COOCCURRENCE != CAUSALITY
SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM
TOKEN_VAZIO != 0
```

## Manifold placement

The generated artifact belongs under the existing publication topology:

- `03_MANIFOLD/RELATION_VECTORS.json`
- edges continue under `05_EDGES/`
- chunk authorities continue under `09_CONVERSATION_CHUNKS/`
- execution evidence belongs under `07_EVIDENCE/`
- append-only receipts belong under `08_RECEIPTS/`

No new raw-corpus tree is required.

## μ∆Step interpretation

`MDELTA-*` represents one bounded observed transition:

```text
chunk_n
  --NEXT-->
chunk_n+1
  → lexical/token delta
  → evidence boundary
  → unresolved semantic fields stay TOKEN_VAZIO
```

A sequence can therefore be represented as:

```text
ΩΠ + μΔ1 + μΔ2 + ... + μΔn
```

without claiming that textual continuity proves learning of model weights.

## Completion gate

V1 may move from `IMPLEMENTED_UNTESTED` only after:

1. local/selftest execution;
2. corpus fixture execution;
3. deterministic repeated-output comparison;
4. privacy check proving no raw body in output;
5. receipt tied to commit SHA and input/output manifests.

Until then: `claim_allowed=false`.
