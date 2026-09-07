---
name: control-plane-pattern-leaf
description: Apply reusable architecture patterns to deterministic routing, gates, ledgers, receipts, service classification, or cross-repository orchestration without importing producer authority. Use when a prior system architecture suggests a better trigger, state-machine, validation, rollback, logging, or workflow mechanism.
---

# Control-Plane Pattern Leaf

Read `AGENTS.md` first. RafGitTools owns routing/executor mechanics, not another producer's runtime or scientific truth.

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

- Pascal-like discipline -> typed work envelopes, explicit state values, range/schema validation, initialized defaults and deterministic error states.
- InterBase-like trigger discipline -> event-conditioned transitions, guarded ledger updates, durable audit events and post-transition actions with rollback/parent linkage.

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

A private seed may provide only a sanitized mechanism. Do not copy private prompts, corpus, personal data, secrets or private locators into this public repository.

## Completion

Record `source_pattern`, `control_plane_leaf`, authority boundary, falsifier, transition/receipt path, `F_ok`, `F_gap`, `F_next`, rollback and `claim_allowed`.
