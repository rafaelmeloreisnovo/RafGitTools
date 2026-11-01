# START — single global owner + four scoped Actions workflows

`START.yml` is the only **global provider-required status-check owner**, NOT the only YAML file. The four auxiliary workflows are deliberately scoped to their specified branch/path or manual gate. They do not gain global authority from their presence.

- `.github/workflows/ci-android-atlas-consumer.yml`
- `.github/workflows/rafaelia-middlefield-bridge-scoped.yml`
- `.github/workflows/rafaelia-rapidtools-lab-scoped.yml`
- `.github/workflows/rafgittools-apk-canary.yml`

The source topology validator checks the exact allowlist; unknown workflows fail closed. A candidate can be moved to immutable historical storage only after dedicated trigger, CI, and status-check dependencies have been reconciled. Do not silently delete or archive workflows during a P0 fix.

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

## PAT_AGENTS Promotion Hold Agent (source v1)

The existing `START.yml` coherence lane now tests `scripts/promotion_hold_pat_agent_v1.py` on its own PR/push runs. The manual owner/environment-gated `promotion_hold_pat` mode may inspect or apply a **title-only** warning for the exact three allowlisted external PR heads in `configs/promotion-hold-agent.v1.json`. It never merges, reviews, alters protections or auto-triggers on pushes in other repositories. A successful source test is **not** PAT write permission; see `docs/governance/PROMOTION_HOLD_PAT_AGENT_V1.md`. If provider permissions are missing, the agent fails closed, and the target PR remains non-promotable.
