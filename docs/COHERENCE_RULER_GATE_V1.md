# Coherence Ruler Gate V1

Status: `IMPLEMENTED_LOCAL / LIVE_CROSS_REPO_TOKEN_VAZIO / claim_allowed=false`

## Purpose

RafGitTools receives only a governed Mapa ruler-selection receipt and decides whether a limited execution route may proceed.

`MAPA_ROUTE != RAFGITTOOLS_EXECUTION != PRODUCER_TRUTH`

The gate does not infer scientific truth, authorship, provider identity, runtime performance, or private corpus content.

## Required source conditions

- source repository is the declared Mapa authority;
- source schema is `rafaelia.coherence_ruler_receipt.v1`;
- status is `RULER_FOUND_REGION_RESTRICTED`;
- selected region is explicitly allowed;
- broad random permutation sweep remains disabled;
- rollback is output-only and non-autonomous;
- watchdog budget is present;
- `claim_allowed=false` is preserved.

## Fail-safe behavior

Any mismatch returns `HOLD`.

The fallback does not try a wider computation. It preserves the typed failure reason and leaves producer-domain claims untouched.

## Cross-repository boundary

The local fixture proves validator behavior only. It is not a live Mapa-to-RafGitTools execution receipt.

`live_cross_repo_receipt = TOKEN_VAZIO`

A later receipt must bind exact producer and consumer revisions before interoperability can be promoted.

## Run

```bash
python3 tools/validate_coherence_ruler_gate.py
python3 -m unittest tests.test_coherence_ruler_gate -v
```

## Resilience

- watchdog: source budget must be present;
- watchdog-of-watchdog: the Mapa receipt carries the decision path and work budget;
- rollback: no autonomous source mutation;
- fail-safe: `HOLD`;
- failover: conservative typed gap;
- privacy: this gate accepts a public-safe receipt, not conversation bodies or Drive payloads.
