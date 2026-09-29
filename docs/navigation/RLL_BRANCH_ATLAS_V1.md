# RLL Branch Atlas V1

Status: `IMPLEMENTED_SOURCE / SNAPSHOT_OBSERVED / RENAMES_NOT_EXECUTED`  
Claim gate: `claim_allowed=false`

## Census

The 2026-09-29 snapshot records:

- RLL: **873 branches**
- RafGitTools: **325 branches**
- Combined: **1198 branches**

The snapshot stores every observed branch name and assigns a proposed numbered family. Classification is navigation metadata, not proof that the branch belongs semantically to that family.

## Canonical ordered families

```text
00-anchor
05-lab
10-science
20-data
30-math-geometry
40-runtime-work
50-docs-papers
60-ci-release
70-governance-security
80-audit-evidence
90-agent-automation
95-legacy-unclassified
```

`main` and `rll/lab` remain anchors. They are not renamed by this migration.

## Why there is no bulk rename

The repositories contain active/historical PR heads, workflow branch filters, receipts, links and provider controls. A Git ref rename can make those references stale even when the commit survives.

Therefore each non-anchor branch is currently:

`REVIEW_REQUIRED`

with four required preconditions:

1. scan open PR references;
2. scan workflow triggers and explicit branch literals;
3. read back branch protection/ruleset state;
4. scan Pages/document links.

Only after all four can a branch move from its historical name to a numbered canonical prefix.

## Pages / combobox

`docs/site/rll-atlas/index.html` provides repository, family and branch selectors. It reads `branch-atlas.json`, a presentation copy of the canonical snapshot.

The page source is materialized, but GitHub Pages provider configuration was not assumed or mutated:

`TOKEN_VAZIO_PAGES_PROVIDER_CONFIGURATION`

## PAT boundary

This work does not reuse all PATs generically.

- `PAT_ACTIONS`: existing bounded read-only route remains unchanged.
- `PAT_ENV`: manual-only provider route remains unchanged.
- `PAT_AGENTS`, `PAT_CODESPACES`, `PAT_DEPENDABOT`: remain registered but unwired until provider permission readback.

No secret value is read, printed, persisted or copied into the atlas.

## Files

- `configs/rll-branch-atlas.v1.yml` — governance contract.
- `data/navigation/RLL_BRANCH_ATLAS_SNAPSHOT_20260929.json` — canonical branch census.
- `docs/site/rll-atlas/branch-atlas.json` — Pages/UI projection.
- `docs/site/rll-atlas/index.html` — combobox navigation.
- `scripts/navigation/rll_branch_atlas.py` — offline validator.
- `tests/test_rll_branch_atlas.py` — CI regression checks.

## R3

**F_ok:** all currently observable branch names are captured and ordered into a migration taxonomy.  
**F_gap:** per-branch PR/workflow/protection/Pages dependency scans are not yet completed; Pages provider state is not read back.  
**F_next:** execute dependency scans by family, starting with `50-docs-papers` and `80-audit-evidence`, then migrate only branches with zero unresolved references.
