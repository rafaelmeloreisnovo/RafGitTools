# PRE-PAPER ADDENDUM — Neuromorphic Rigor Bias Evidence Boundary V2

Date: 2026-09-27
State: CORRECTION_PREREGISTERED
claim_allowed: false

## Trigger

The merged V1 contract states:

```text
BIAS_CHOOSES_ORDER_NOT_TRUTH
evidencePromotionAllowed=false
```

but the current implementation can still derive `recommendedRigor=EVIDENCE`
from high pressure or missing critical heads, and `raiseOnly()` can apply that
result directly to a queued job.

This is a contract/implementation contradiction.

## Correction

Automatic neuromorphic bias may choose at most:

```text
QUICK
STRUCTURAL
MULTIMODAL
```

It may never create `EVIDENCE`.

When evidence-level review is indicated, V2 emits a separate boolean:

```text
evidenceGateRequired=true
```

The actual effective rigor becomes EVIDENCE only if the caller/user/job
already requested EVIDENCE explicitly.

## Provenance correction

The merged V1 has a provenance envelope factory, but the durable biased queue
stores only:

```text
job + biasInput
```

V2 binds provenance to the queued item itself:

```text
job
+ biasInput
+ per-head signal provenance
+ forest provenance
+ source registry IDs
```

On dequeue, the queue creates one deterministic decision envelope and returns
that envelope together with the effective job.

Therefore the selected priority can be reconstructed from its exact input and
provenance.

## Invariants

```text
AUTO_BIAS_MAX = MULTIMODAL
EXPLICIT_EVIDENCE_REQUEST is required for effective EVIDENCE
evidenceGateRequired != evidencePromotion
queued bias without provenance may still run only when its channels are
explicitly TOKEN_VAZIO or locally derived and identified
decision envelope is generated once at dequeue
priority selection != claim
```

## Falsifiers

- V2-F001: QUICK + maximal pressure becomes EVIDENCE.
- V2-F002: missing identity/provenance/reproducibility silently promotes EVIDENCE.
- V2-F003: explicit EVIDENCE request is downgraded.
- V2-F004: queue selection returns no provenance envelope.
- V2-F005: unknown provenance head is accepted.
- V2-F006: decision input hash changes across identical replay.
