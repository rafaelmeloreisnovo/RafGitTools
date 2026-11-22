# S7 — Single-root docs-integrity hotfix · 2026-10-27

Status: IMPLEMENTED_ON_ISOLATED_BRANCH; CI=TOKEN_VAZIO_PENDING; PROMOTION=HOLD.
Authority: RafGitTools local; Mapa federation authority is not presumed.
Baseline: `0c6bbb32acda3a729652e1fb2559a5976dd0a86c`. No changes to main, Atlas APK, runtime, provider secrets or rights.

## The reproducible contradiction
- `.github/workflows/START.yml` job 01 requires **exactly one** active *.yml/*.yaml workflow.
- `.github/workflows/rafaelia-docs-integrity-v1.yml` was a second active workflow, so job 01 failed before coherence, Android and end-to-end receipts.
- Separate docs checks previously had independent scoped PASS; deleting them would be a regression.

## S7 surgical reconciliation
1. Copy the legacy workflow blob **without changing bytes** to `.github/workflows-legacy-20260907/rafaelia-docs-integrity-v1.yml`, removing only the active original path.
2. Add `docs_integrity` as a job in existing `START.yml` with the same offline unittest, five blocking navigation targets, and audit-only historical scan.
3. Keep it independent of topology/coherence, run on **all** START pull requests (stronger than path-only for audit coverage), plus manual docs/full. Verify exact source SHA and no credentials persistence.
4. Make S7 a required final receipt stage for pull requests/manual docs/full. Skipped modes remain `TOKEN_VAZIO_NOT_SELECTED`; failures propagate.
5. Preserve single-root strict topology assertion unchanged. Do not permit arbitrary extra workflows; do not auto-merge/release.

## Provenance and rollback
SOURCE: main `0c6bbb32acda3a729652e1fb2559a5976dd0a86c`, START original blob `3f5b401329359f7d073e84347a3c89689b3c2e1d`, legacy YAML blob `6a1bffe03ed4cd0f5075ec94f6cd59bd994e8cfe`.
ARTIFACT/EXECUTION/EVIDENCE: fill from exact-SHA Actions jobs, not inferred by document authors.
Rollback before merge: close this draft PR and discard isolated branch.
Rollback after permitted merge: revert S7 commit to restore both active YAMLs **only as a diagnostic recovery** (the prior topology fault returns). Prefer forward fixing a failing docs job while preserving its invariant.
Falsifiers: changed or missing five target checks; ignored test failure; topology allowing >1 active workflows; receipt marking failed required docs as PASS; security/permissions increase; unrelated APK regressions.
Scope: source/CI only. Android physical validation, cross-repository producer+consumer, all-history reference integrity and global runtime remain `TOKEN_VAZIO_NOT_RUN`.

## S7.1 negative CI falsifier / surgical link repair

First exact-head run: `40529849174`, job `01` SUCCESS_SCOPED; job `05Z` failed with a true `MISSING_TRACKED_TARGET` at `docs/RAFAELIA_DOCS_RECONCILIATION_20261025.md:73` (legacy workflow was correctly archived). The 14 independent unit tests passed. This negative finding is retained, not reclassified as PASS. Follow-up changes only the historical link to the byte-identical archived file and adds a successor pointer to active START 05Z. Re-run must verify job `01`, `05Z`, and global receipt on the new exact SHA. `claim_allowed=false`.
