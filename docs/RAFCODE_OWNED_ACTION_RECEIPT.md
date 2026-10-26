# Unified RAFCODE source and provisional receipt (R3)

Authority: `rafaelmeloreisnovo/RafGitTools` — executor & corpus custody. This is an additive hosted CI lane. Existing specialized workflows, security and test requirements remain unchanged.

The exact immutable owned Action refs are `verify-source@47e0ad95703009bfaaa902f605fd3bf69aae4b48` (reviewed default branch) and `evidence-receipt@885b6f4c4356d5e1cea352653cbe69a4e141d198` (unmerged **DRAFT** Actions PR#19, independently exercised by Mapa pilot). Neither ref is a mutable tag. The new falsifier tests only local README SHA256 agreement between Python and hosted sha256sum, plus a one-byte corruption counterexample.

PROVISIONAL_HOST_TEST binds exact checkout SHA + GitHub run ID/attempt + README artifact SHA256 + falsifier script SHA256 and exit 0. This is **not** package/application/physical/scientific test evidence. Provider run conclusion is not attested while job executes; standalone reviewer must later check run ID, source SHA, artifact bytes/hash and negative test semantics, provider rules and local authorities. Do not promote a source digest to runtime PASS.

Reviewer checklist: expected source SHA matches HEAD; exact owned Action SHA/rights; old domain jobs remain; negative mutation rejected; run concludes SUCCESS; artifact bytes match; independent review and branch protection confirmed; rollback scoped. Credentials are stripped before executing the falsifier; no extra marketplace Actions used. Host Git/Bash/Python are build-test dependencies, not the freestanding Android core. User data and private repository bytes are not published. `claim_allowed=false`, `PROMOTION=HOLD`, no delete/merge/release/auto-dispatch. Local adaptation only, preserve upstream copyrights/licenses.

## Successor provider reconciliation — 2026-10-26 (append-only)

Historical note above described `rafaelmeloreisnovo/actions` PR #19 as unmerged when written. **Later GitHub readback now reports PR #19 merged**; preserve the predecessor wording as historical rather than rewriting it.

- RafGitTools Draft PR #709 exact predecessor HEAD: `8e66b9b633fb5dac3eff64450a5ec2a81765ee64`. Run [40285947009](https://github.com/rafaelmeloreisnovo/RafGitTools/actions/runs/40285947009) completed with provider conclusion SUCCESS on that HEAD. Job `01 · Workflow topology` and its steps **RAFCODE exact source** and **RAFCODE provisional run/artifact/falsifier receipt** returned SUCCESS. This proves those *scoped hosted steps ran*, not private CI or application correctness.
- Job `03 · Python`, CodeQL, physical Android and private-CI bridge were SKIPPED in that run. No global PASS, provider enforcement attestation, signed package, independent review, or private end-to-end isolation is inferred.
- Historical PR #707 is **closed and unmerged**, not an integration source. PR #710 is **open Draft and unmerged** at `4f8a6337e233da0b9349472a96b60889f4d2bd41`, with separate scoped [run 40283101129](https://github.com/rafaelmeloreisnovo/RafGitTools/actions/runs/40283101129). Its freestanding L0 implementation must remain distinct from hosted adapters.
- At observation time `main=a6155333ad9c319ad9e1860c1d4ac21a8cf45726`. Both PR #709 and PR #710 diverge from main; each modifies `.github/workflows/START.yml`. **Integration is not proven** by independent PR success. Reconcile on a fresh branch from exact main, add both changes non-destructively, inspect single-root invariant, and demand a new exact-HEAD CI run before human review/merge. Do not rebase or force-push historical evidence branches merely to manufacture green status.
- Public human-review candidate: RLL [Draft PR #1131](https://github.com/instituto-Rafael/relativity-living-light/pull/1131); page publication is NOT_ATTESTED. Never include PAT metadata, secret material, private receipts or financial records in Pages.

`claim_allowed=false`; `PROMOTION=HOLD`; `PRIVATE_E2E=NOT_RUN`; `HUMAN_REVIEW=PENDING`. Rollback: revert only this added section or close PR; historical records unchanged.
