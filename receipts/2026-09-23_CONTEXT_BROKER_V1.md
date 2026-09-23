# Receipt — ContextBroker V1

Date: 2026-09-23
Parent: WorkspaceSession V1 / PR #480
Kind: `ANDROID_SOURCE + CONTEXT_CONTRACT_WIRING`
claim_allowed: `false`

## Delta

Added explicit in-memory ContextBroker with bounded text segments, deterministic SHA-256 and segment identity, ResourceRef provenance, privacy propagation, ContextBundle V2 construction/JSON, no automatic source ingestion, no silent truncation, no binary ingestion and unit-test source.

FileBrowser wiring is part of this branch: current text file can be added only by explicit action.

## Evidence

```text
SOURCE = IMPLEMENTED
UNIT_TEST_SOURCE = IMPLEMENTED
EXACT_BRANCH_CI = TOKEN_VAZIO until run completes
MODEL_CALL = NOT_IMPLEMENTED in this delta
NOVO_PROVIDER = TOKEN_VAZIO
DEVICE = TOKEN_VAZIO
```

R3=<F_ok: local explicit context assembly exists; F_gap: NOVO provider + llama consumption; F_next: bind read-only Navigator result refs and then local model>.
