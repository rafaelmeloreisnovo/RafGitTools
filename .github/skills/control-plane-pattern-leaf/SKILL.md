---
name: control-plane-pattern-leaf
description: >-
  Apply reusable architecture patterns to routing, gates, ledgers, receipts,
  service classification, or cross-repository orchestration without importing
  producer authority. Use when a prior architecture suggests a better trigger,
  state-machine, validation, rollback, logging, allocation, or workflow
  mechanism.
---

# Control-Plane Pattern Leaf

Read `AGENTS.md` first. RafGitTools owns routing/executor mechanics, not another
producer's runtime or scientific truth.

## Local projection

```text
extracted mechanism
-> control-plane problem
-> typed event/state transition
-> authority + capability guard
-> deterministic action
-> terminal verifier/falsifier
-> append-only receipt
```

Reference transfers:

- Pascal-like discipline -> typed work envelopes, explicit state values,
  range/schema validation, initialized defaults and deterministic error states.
- InterBase-like trigger discipline -> event-conditioned transitions, guarded
  ledger updates, durable audit events and post-transition actions with
  rollback/parent linkage.

## Event and commit geometry

When the source architecture contains add/create, update, delete and commit,
normalize them as separate transitions:

```text
EVENT
-> VALIDATE
-> APPEND / STAGE
-> DURABLE COMMIT
-> SEQUENCE / HASH
-> INDEX
-> CURRENT VIEW
```

For append-only ledgers, an update or delete should normally become a successor
record, tombstone, supersession or explicit state transition. Do not rewrite
historical evidence merely to make the current view easier to read.

The control plane must distinguish:

- event acceptance from durable commit;
- commit from promotion of a claim;
- identifier allocation from object location;
- ledger order from wall-clock chronology;
- current view from immutable event history.

## Identifier and partition geometry

Older auto-number, per-user partitioning or record-placement ideas transfer as:

```text
scope/owner
-> allocation namespace
-> typed sequence or identifier
-> collision/range guard
-> durable event identity
-> index projection
```

Physical storage placement may influence a cost model, but it must not silently
change logical identity or authority.

## Preserve

- `TOKEN_VAZIO != 0 != false != PASS`;
- local authority vs Mapa federated authority vs producer authority;
- high/critical rollback before mutation;
- append/supersede history instead of rewriting;
- security/privacy/provenance as non-compensatory gates;
- terminal verifier evidence as the source of PASS.

## Forbidden transfer

```text
router success != producer runtime success
receipt presence != truth
hash != scientific validation
pattern reuse != authority transfer
skill != evidence
```

A private seed may provide only a sanitized mechanism. Do not copy private
prompts, corpus, personal data, secrets or private locators into this public
repository.

## Completion

Record `source_pattern`, `control_plane_leaf`, authority boundary, identifier
scope, event/commit geometry, falsifier, transition/receipt path, `F_ok`,
`F_gap`, `F_next`, rollback and `claim_allowed`.
