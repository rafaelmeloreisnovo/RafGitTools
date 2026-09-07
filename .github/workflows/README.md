# START — single active GitHub Actions entrypoint

`START.yml` is the only active YAML workflow in this directory.

The previous workflow tree is preserved byte-for-byte at:

```text
.github/workflows-legacy-20260907/
```

## Pipeline

```text
00 Plan / routing
  -> 01 Workflow topology
  -> 02 Coherence / anti-regression
  -> 03 Python deterministic tests
  -> 04 Federation (conditional)
  -> 05 Documentation (conditional)
  -> 06 Android test/lint/APK (conditional)
  -> 07 CodeQL security (conditional)
  -> 08 Signed release (explicit/tag only)
  -> 09 Append-only final receipt
```

## Modes

- `auto`: core + Android + security; federation/docs selected by changed paths.
- `fast`: topology + coherence + Python tests only.
- `full`: all non-release validation lanes.
- `android`: Android lane plus mandatory core.
- `federation`: federation lane plus mandatory core.
- `security`: CodeQL lane plus mandatory core.
- `docs`: documentation lane plus mandatory core.
- `release`: signed release lane; unsigned release remains blocked.

## Invariants

```text
TOKEN_VAZIO != PASS
SOURCE != EXECUTION != EVIDENCE != CLAIM
CI_GREEN != PHYSICAL_RUNTIME_PROOF
DATASET_INFORMS != MISSION_AUTHORITY
MODEL_PROPOSAL != EXECUTION_PERMISSION
LEARN_APPEND_ONLY != ONLINE_SELF_TRAINING
```

The refactor changes workflow topology, not scientific or physical claim authority. `claim_allowed=false` and automatic promotion remains disabled.

## Rollback

The full prior `.github/workflows` tree is retained as an immutable Git tree under the legacy archive directory. Restoring the old topology is a tree-level revert; no legacy workflow body was rewritten during migration.
