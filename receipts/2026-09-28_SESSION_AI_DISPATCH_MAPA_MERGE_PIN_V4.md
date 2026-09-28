# Receipt — Session AI Dispatch — Mapa Merge-Pin Correction V4

Date: 2026-09-28  
Parent: `receipts/2026-09-28_SESSION_AI_DISPATCH_ADAPTER_DRIVE_BINDING_V2.md`  
State: `CORRECTED_RETEST_PENDING`  
claim_allowed: `false`

## Before state

PR #538 was merged while its branch-local adapter pointer had been advanced to Mapa commit
`39a8bac6188f46a374176c9826d296bcfebd2ec6`.

That Mapa commit was created after PR #698 had already merged and its source branch was deleted. It is therefore not the merged authority commit used by `main`.

## Authoritative Mapa state

- PR #698 merged: `ad2efc2b9a4599bdcc37ab416c84bd99dc2a1e09`.
- merged PR head: `c71ddf9e48614107d357074e42b91dbb8e49d5e6`.
- exact-head CI run after Markdown repair: `36396562990` = `success`.
- dispatcher registry blob on main: `c1d1d8f1692c74957f8b70968fa863f855d93cb0`.
- original failing CI: run `36396306692`, job `108843407236`, Markdown MD022/MD032.
- provider/server enforcement and credential/promotion gates remain separate external/governance states and were not weakened.

## Correction

The local adapter now pins:

`main@ad2efc2b9a4599bdcc37ab416c84bd99dc2a1e09`

as the exact merge commit containing the dispatcher registry. The pin is immutable evidence; it does not assert that moving `main` must remain at that HEAD.

The stale/orphan post-merge pointer and nonexistent V3 receipt reference were removed from the active adapter contract and replaced by explicit CI evidence fields.

## Expected observable

The local adapter validator accepts the corrected exact authority pointer and RafGitTools exact-head START CI exercises this successor branch.

## Falsifier

Any adapter still requiring the orphan commit, a missing Mapa source blob, or a source-level validator failure keeps this correction unresolved.

## Rollback

Revert commit `eb267c7f60753580f07ffe9c33f7139ce99332c5`; historical merged PR #538 evidence remains preserved.

## R3

- F_ok: merged Mapa authority and successful corrected CI are now explicitly pinned.
- F_gap: RafGitTools successor exact-head CI is pending.
- F_next: open draft PR, observe START; fix only concrete local source defects.
