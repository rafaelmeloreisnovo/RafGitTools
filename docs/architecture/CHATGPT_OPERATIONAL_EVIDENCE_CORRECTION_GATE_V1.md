# RafGitTools — ChatGPT Operational Evidence Correction Gate V1

**Status:** `SOURCE_CONTRACT / DRAFT_NO_PROMOTE / claim_allowed=false`

## Purpose

Treat a ChatGPT session instruction as **operational evidence**, not a training
dataset. A validated session receipt may trigger a corrective workflow for
program source only after a deterministic source-side falsifier or reproducer
binds the observation to an exact target revision.

```text
SESSION_CONTEXT != TRAINING_DATASET
OBSERVATION != ROOT_CAUSE
CORRECTION_CANDIDATE != CORRECTION_PASS
AUTO_CORRECT != AUTO_MERGE
```

## Route

```text
ChatGPT session
-> RafPolimata operational-evidence validator
-> exact evidence receipt
-> target program/revision binding
-> deterministic reproducer/falsifier
-> root-cause evidence
-> bounded patch
-> RafGitFS DRY_RUN
-> work branch mutation
-> exact-head tests
-> post-patch receipt
-> human/provider promotion gates
```

The automatic portion is deliberately bounded to **work-branch corrective
execution + tests + rollback + receipt**. It does not authorize direct main
writes, force push, remote deletion, auto-merge or claim promotion.

## Eligibility

`AUTO_CORRECT_ELIGIBLE` requires all of:

1. validated ChatGPT operational evidence with `training_use=false`;
2. explicit repository/path/target;
3. exact source revision;
4. deterministic reproducer or source-side falsifier;
5. root-cause evidence;
6. bounded patch;
7. pre-patch receipt;
8. rollback condition;
9. required post-patch tests;
10. non-regression invariants.

Missing evidence yields `HOLD_EVIDENCE_ONLY`, never PASS.

## PASS boundary

A patch is not PASS merely because it was written. PASS requires the exact
patched head to satisfy both the original falsifier and all declared regression
gates, with a post-patch receipt.

## Rollback

A failed falsifier, regression, wrong target SHA, or broken invariant requires
rollback of the bounded work-branch mutation. Historical evidence is preserved.

## R3

- **F_ok:** session evidence has a typed consumer and a bounded automatic correction route.
- **F_gap:** no target-specific correction is authorized by this generic contract alone.
- **F_next:** feed a validated RafPolimata receipt plus a target-specific correction envelope; execute only through governed RafGitFS work-branch flow.
