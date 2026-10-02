# Execution Gate UI V1 — Android one-button WorkGroup

State before exact-head CI: `IMPLEMENTED_UNTESTED`.

## Purpose

Materialize the visible “portão” without inventing an executor. The Android surface delegates real local processing to the pre-existing `LibraryLocalJobExecutor` and displays receipt-derived stage evidence.

```text
SELECT SOURCE
-> SOURCE LOCK
-> PREFLIGHT
-> OPEN GATE
-> DISCOVER
-> INGEST
-> EXTRACT
-> VECTORIZE
-> RECEIPT
-> GAPS
-> CLAIM GATE
-> CLOSE
```

## Authority and limits

V1 is intentionally narrow:

- source: Android SAF document selected by the user;
- source access: read-only;
- content scope: `FULL_SOURCE` only;
- hard input ceiling: 16 MiB;
- local processing rigor: `STRUCTURAL`;
- network policy: `OFFLINE_ONLY`;
- stages: `DISCOVER`, `INGEST`, `EXTRACT`, `VECTORIZE`;
- resource/battery preflight remains owned by `LibraryRigorLens`;
- `claimAllowed=false` always;
- no arbitrary shell, Git mutation, merge, deploy or provider mutation;
- oversized/unknown-failure paths fail closed rather than silently sampling.

The launcher is isolated as `Raf Convoy`, following the repository's existing pattern for `RafGitFS`, privacy, setup and bridge control surfaces. It also exposes the bounded deep-link identity `rafgittools://convoy`.

## UI contract

The surface contains:

1. `Selecionar fonte` — Android SAF only;
2. `ABRIR PORTÃO` — enabled only after a source is selected;
3. one state field;
4. a monospace `SYSLOG / RECEIPT` list;
5. close action disabled while the bounded run is active.

The UI does not synthesize stage success. `PASS_RECEIPT` lines are emitted only from `receipt.completedStages`. Missing descriptor/evidence remains `TOKEN_VAZIO` or the executor's explicit gap/state.

## Identity and replay

`ExecutionGateContractV1` binds the job to:

```text
opaque_locator_sha256
+ content_sha256
+ STRUCTURAL rigor
+ execution-gate-v1 namespace
```

This yields deterministic `sourceId`, `jobId` and `idempotencyKey` for the same bounded source identity. Raw SAF URI is not placed into the job contract; only its SHA-256 locator digest is used.

## Evidence boundary

Source implementation and unit tests are not physical-device evidence.

Required promotion chain:

```text
SOURCE
-> exact-head Android unit tests
-> lint
-> assemble devDebug
-> APK SHA-256 verification
-> post-merge CI
-> physical device install/launch/execution receipt (separate gate)
```

`CI_PASS != PHYSICAL_DEVICE_PASS`.

## R3 before CI

- `F_ok`: bounded UI source, pure contract and unit tests materialized; existing local executor reused rather than duplicated.
- `F_gap`: exact-head CI and physical Android execution not yet observed; BLAKE3 is not introduced by this UI.
- `F_next`: run exact-head START; merge only after gate success; then require post-merge START and keep device receipt separate.
