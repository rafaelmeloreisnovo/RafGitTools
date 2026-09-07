# RAFAELIA START Pipeline V1

State: `CANDIDATE_SINGLE_ROOT`

`claim_allowed=false`

## Purpose

Collapse the GitHub Actions trigger plane into one explicit orchestration root without destroying the previous workflow bodies.

Before this refactor the repository had many independently triggered YAML workflows while documentation described `ci.yml` as the canonical entrypoint. This created overlapping push/PR/schedule roots and made execution state harder to reason about.

The new invariant is:

```text
.github/workflows/*.yml count == 1
active root == .github/workflows/START.yml
legacy workflow bodies == preserved outside active workflow directory
```

## Topology

```text
START
  00 plan
  01 topology
  02 coherence
  03 python tests
  04 federation [conditional]
  05 docs [conditional]
  06 android [conditional]
  07 security [conditional]
  08 signed release [explicit]
  09 receipt
```

All lanes inherit the evidence boundary:

```text
TOKEN_VAZIO != PASS
SOURCE != EXECUTION != EVIDENCE != CLAIM
CI_GREEN != PHYSICAL_RUNTIME_PROOF
DATASET_INFORMS != MISSION_AUTHORITY
MODEL_PROPOSAL != EXECUTION_PERMISSION
LEARN_APPEND_ONLY != ONLINE_SELF_TRAINING
```

## Legacy custody

The exact previous `.github/workflows` tree is mounted at `.github/workflows-legacy-20260907` using its original Git tree object:

```text
legacy_tree_sha = d4cb73f43297f6b38dd5ead0cd32998910794ec9
baseline_main = 3ef42beea96a341329d103147ccfbd1a9a9e1ff0
baseline_root_tree = adf2198374e71096ed2e94278b7994f4e75934ae
```

No legacy YAML body is rewritten in the migration commit. GitHub Actions does not treat the archive directory as an active workflow directory.

## Promotion boundary

This refactor can prove only orchestration/source CI properties. It cannot prove Android physical runtime, provider authorization, scientific claims, model training or release signing until those corresponding lanes actually execute with their required evidence.

## Rollback

Revert the migration commit, or replace `.github/workflows` with tree `d4cb73f43297f6b38dd5ead0cd32998910794ec9`.
