# RLL Branch Atlas V1.1

Status: `IMPLEMENTED_SOURCE / DISPLAY_ORDERED / PHYSICAL_REFS_PRESERVED`  
Claim gate: `claim_allowed=false`

## Selection model

`main` remains exactly `main` and is pinned at the top of the selector.

Everything below it is ordered by navigation number:

```text
main
05-lab
06-integration
07-release
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

The number is a **navigation/order label**, not a mandatory physical Git rename.

The maturity refs keep their real names:

- `main`
- `rll/lab`
- `rll/integration`
- `rll/release`

## Snapshot rule

The branch census is a point-in-time observation. The validator checks that each repository total equals the number of records in the snapshot instead of hard-coding a historical count.

## UI behavior

`docs/site/rll-atlas/index.html` uses three selectors:

1. repository;
2. ordered selection;
3. branch.

When a repository contains `main`, the selector starts at `main`. Choosing another numbered family exposes its branches below the root selection.

## Rename boundary

No bulk rename is authorized by the atlas.

For historical/non-maturity branches, a physical rename remains `REVIEW_REQUIRED` until PR references, workflow literals, protection/rulesets and Pages/document links are checked.

## R3

**F_ok:** main is fixed as the root display name; maturity refs have explicit ordered families; numeric labels organize selection without changing protected refs.  
**F_gap:** provider Pages publication and per-branch physical rename safety remain separate evidence gates.  
**F_next:** use the ordered UI immediately; execute physical rename only where dependency scans prove it safe.
