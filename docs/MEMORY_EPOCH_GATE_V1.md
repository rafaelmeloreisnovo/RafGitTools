# MEMORY_EPOCH_GATE V1

Status: **IMPLEMENTED_LOCAL / PRODUCER_LIVE_RECEIPT_TOKEN_VAZIO**

Issue: `#394`

## Purpose

This gate separates a process/runtime reset claim from evidence that a larger memory boundary was actually reset. It is a reusable, producer-neutral contract for training/model/runtime experiments and other stateful systems.

```text
RESIDUAL_STATE != AUTHORIZED_DATA
RELEASE != SANITIZE
RESET_PROCESS != RESET_MEMORY_EPOCH
RESET_PROCESS != RESET_LEARNING
LEARNING != PROMOTION
OBSERVED_CORRELATION != AUTHORIZED_CHANNEL
TOKEN_VAZIO != PASS
NO_OBSERVED_LEAK != PROOF_OF_CONTAINMENT
```

## Contract

Normative shape:

- `schemas/memory_epoch_receipt.v1.schema.json`

Executable semantics:

- `tools/validate_memory_epoch_receipt.py`

Deterministic fixture/test surface:

- `fixtures/memory_epoch/positive.v1.json`
- `fixtures/memory_epoch/negative.reproducible-correlation-not-quarantined.v1.json`
- `tests/test_memory_epoch_receipt.py`

Scopes are ordered from narrowest to broadest:

`PROCESS -> RUNTIME -> CONTAINER -> VM -> EXTERNAL_MEMORY -> LEARNING_STATE -> HOST -> PHYSICAL_EPOCH`

A receipt is rejected if its claimed reset scope or reset class exceeds its evidenced scope.

## Independent observer boundary

The observer must be independent and the model/training runtime must not be able to modify the control plane represented by policy, watchdog/audit/attestation, rollback anchor, or promotion decision. Cross-epoch markers/canaries must be owned by an external observer and exercised only through an authorized interface.

## Replay binding

Every accepted receipt binds exactly:

`artifact_sha256 + epoch_id + reset_class + test_corpus_sha256 + policy_version`

A mismatch is rejected rather than normalized.

## Fail-closed states

- mandatory `TOKEN_VAZIO` blocks `PROMOTE`;
- observed residual state blocks `PROMOTE`;
- reproducible unexpected cross-boundary correlation requires `QUARANTINE`;
- quarantine is evidence handling, **not** an exploit claim;
- V1 keeps `claim_allowed=false`.

## Producer adapter contract

The `producer` object carries exact `repo`, `commit`, `adapter_id`, and `adapter_version`. This lets different producers emit the same receipt without copying producer-sensitive internals into public policy.

## Evidence boundary

The local contract and deterministic tests do not establish physical zeroization, device containment, security certification, model behavior, or a live producer reset. Those remain receipt-bound:

```text
producer_live_receipt = TOKEN_VAZIO
physical_epoch_proof = TOKEN_VAZIO
claim_allowed = false
```

## R3

- **F_ok:** typed scopes, fail-closed validator, replay binding, independent observer rule and deterministic negative tests.
- **F_gap:** first exact producer integration and independent live cross-epoch receipt.
- **F_next:** bind one real producer adapter to an exact artifact/epoch and preserve `TOKEN_VAZIO` until that execution exists.
