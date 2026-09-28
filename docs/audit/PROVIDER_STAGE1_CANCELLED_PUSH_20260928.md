# Provider Stage-1 Main Protection — Cancelled Push Problem Report

**Observed:** 2026-09-28  
**State:** `READY_TO_RETRY / EVIDENCE_BOUND`  
**claim_allowed:** `false`

## Problem

PR #519 merged the stage-1 server-side main protection path at commit
`df16e6dd19c5d8e0350c66b78dbb3edae9ed642c`.

The corresponding main-push START run was:

- run: `36363833345`
- workflow: `START · RAFAELIA Orchestrated Pipeline`
- event: `push`
- conclusion: `cancelled`
- jobs observed through the GitHub jobs endpoint: `0`

A subsequent main push from PR #522 advanced main to
`74f4b5b66469bcfe16aee5253a4ea5d985bce841`.

Provider readback after those events reported:

```text
main protected = false
required status-check enforcement = off
```

Therefore:

```text
MERGED_STAGE1_SOURCE != EXECUTED_PROVIDER_MUTATION
CANCELLED_PUSH != FAILED_POLICY_LOGIC
PROTECTION_ABSENT != PROTECTION_APPLIED
```

The implementation source exists, but the one-shot execution did not reach the
provider mutation gate.

## PR #520 disposition

PR #520 (`codex/provider-environment-apply-once-20260927`) had exact-head PR CI
success on `71e1b19c94b44d45282be0f9374a0c54fb2b8a80`, but it was built from the
pre-stage1 base and is now diverged from current main.

Its policy also encodes a minimum approving review of 1, while the merged stage-1
contract deliberately preserves:

```text
required_approving_reviews = 0
stage2_gate = TOKEN_VAZIO_INDEPENDENT_REVIEWER
```

Merging PR #520 as-is would therefore mix a stale implementation line with a
later, explicitly bounded stage model. It is treated as superseded evidence, not
as the retry vehicle.

## Correction

The smallest reversible correction is to **re-arm the existing canonical
one-shot trigger**, not to add a second active workflow and not to replace the
stage-1 plan.

This branch changes only:

- `configs/provider-enforcement-stage1.once.json` — append retry evidence;
- `tests/test_provider_enforcement_stage1.py` — bind the retry to the observed
  cancelled run and unprotected provider state;
- this problem/evidence record.

On merge, the changed exact trigger path causes the existing START route to
select `apply_main_protection`. The applicator then binds the expected branch
SHA to the actual merge commit (`GITHUB_SHA`), performs fresh admin/prestate
checks, applies the existing v4 stage-1 policy, verifies provider readback and
uses the existing rollback path if the mutation/readback fails.

## Stop conditions

Do not promote to PASS until all of the following are observed on the retry
merge commit:

1. START push run reaches terminal state;
2. provider-environments job executes;
3. sanitized provider receipt is uploaded;
4. readback shows main protected with the exact four START core contexts;
5. current main SHA is unchanged through apply/readback;
6. branch protection readback from GitHub reports `protected=true`;
7. negative assurance remains a separate gate.

## R3

- **F_ok:** cancellation and absent protection are now explicitly bound to exact
  run/commit/provider readback; retry uses the merged stage-1 implementation.
- **F_gap:** retry has not yet executed; provider protection remains absent at
  this observation.
- **F_next:** exact-head PR CI, merge only after terminal success, then inspect
  the push-run provider receipt and branch-protection readback.
