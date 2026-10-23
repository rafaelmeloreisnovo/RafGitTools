# CI fastlane and queue containment — 2026-10-08

Copyright (c) 2026 Rafael Melo Reis. Retain original project license and provenance.

Single START.yml previously enabled Android and CodeQL for every auto PR draft. Change: draft auto selects fast (topology/coherence source); Python full suite, Android, security marked NOT_RUN; ready_for_review remains auto/full; main and develop push full; stale source jobs cancelled. Manual, scheduled, tag releases never auto-cancel. Existing exact-SHA, authorship, license, provider P0 and release gates retained. Platform dynamic dependency submission remains out-of-scope. Rollback by reverting PR; quick != full.

## Steps for a human or AI operator

1. Keep work in a Draft PR; inspect quick source-contract results without promoting them to binary PASS.
2. Mark Ready for review to trigger all retained full PR/ABI tests that apply.
3. Review source and rights; merge only with required checks and provider branch-protection readback.
4. Invoke expensive producer/beta/publish workflows deliberately after source is stable; capture exact source SHA, run ID and artifacts.
5. Never call source-only, QEMU-only or CI-only evidence physical Android validation. Record TOKEN_VAZIO, NOT_RUN, FAIL or PASS precisely.

R3 = <F_ok: source gate/refactor on PR, F_gap: CI executed exact head, provider P0, hardware receipts, F_next: CI quick -> ready full -> explicit delivery -> device readback>.
