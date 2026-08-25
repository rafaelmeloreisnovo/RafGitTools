# RafGitTools source-gap baseline probe — 2026-08-21

Purpose: execute the existing `Source Gap Audit` against the exact pre-adapter base `ab3ca76dbde9a7d9e7121801fa5ab8dc321766af` while changing no compiled source.

Hypotheses:
- H0: the 82 blockers / 34 warnings observed on PR #368 pre-exist the adaptive-resilience adapter.
- H1: the adapter branch introduced or changed the compiled-source gap set.

This file is an audit trigger only. It changes no application source, scanner, workflow, dependency or runtime behavior.

Closure evidence: compare `files`, `blockers`, `warnings`, and `tree_sha256` from this PR's Source Gap Audit with PR #368. `claim_allowed=false`.
