# Raf Privacy — per-repository protection and inventory review (V1)

**State:** `REVIEW_ONLY / BRANCH_ONLY / IMPLEMENTED_UNTESTED`. This is a public
engineering contract, **not** a list of private repositories, an executed audit,
a legal compliance certificate, or an instruction to change GitHub visibility.

## Goal and authority

The authenticated inventory is scoped by the active GitHub identity, never by
guessing ownership. `RafGitTools` owns the Android UI and visibility manager;
each producer repository owns its code, dependencies, licenses and release
requirements. `Mapa` governs the federated route. The Drive START HERE Ω V2.2
is a pointer, not an export destination.

A repository's existence in a search result is not evidence of administrative
authority. Unknown or unsupported visibility blocks mutations. A public repo
can be intentionally public and healthy; a private repo can still have security
defects. A public-to-private operation may break Pages, forks, integrations and
user expectations. Private-to-public may disclose historical commits and data.

## What the source currently collects (not a security certification)

- The app pages through `GET /user/repos` with `visibility=all` and
  `affiliation=owner,organization_member`, 100 per page, hard cap 1,000 pages.
  Exceeding the cap produces an error instead of a partial-success claim.
- A listing can expose only the owner, numeric repository ID, full name,
  current visibility, fork/archive/disabled flags, Pages flag, social counters
  and explicit admin permission. It **cannot** infer secrets, license rights,
  data sensitivity, the full access graph, or successful CI.
- Eligibility is recomputed against the requested direction. Unknown/internal/
  contradictory metadata, forks, archives, disabled projects and missing admin
  authority are blocked. A live `GET /repos/{owner}/{repo}` is required before
  a visibility `PATCH`; durable journal checkpoints precede remote mutations.

## User-facing flow implemented in the candidate branch

1. Choose one account/organization, or the available authenticated owner scope.
2. Choose the requested visibility. The source state is computed as its inverse.
3. Review a searchable, lazy list of **all returned repositories in scope**,
   including excluded rows with explicit reasons. No repository is hidden merely
   because it is already in the target state. The list is local metadata, not a
   newly executed security scan.
4. PRIVATE: the previous eligible-by-default selection remains preview-only;
   an optional exception mode supports per-repository exclusions.
5. PUBLIC: automatic bulk selection is **disabled**. The user must select each
   eligible repository individually. A filtered search shows the number of
   selected repositories outside the current filter.
6. A frozen list of IDs and destination is shown in the confirmation dialog.
   The ViewModel compares these values again immediately before starting the
   manager. Duplicate submissions are guarded synchronously; refresh is blocked
   while executing. Failed inventory refresh clears stale candidates.

**No visibility mutation happens from this branch or from viewing a preview.**
Device execution and integration tests are `TOKEN_VAZIO / NOT_RUN` until proven.

## Proposed per-repository evidence profile — NOT IMPLEMENTED

For each returned `repository_id`, a future read-only collector should produce
a separate, access-scoped object. Do not create a binary `safe` flag from missing
data. Every axis requires typed `OBSERVED`, `NOT_CHECKED`, `DENIED`,
`STALE`, `NOT_APPLICABLE`, `FAIL`, or `VERIFIED_SCOPED` with a source/ref.

| Axis | Evidence to collect | Candidate action after authority |
|---|---|---|
| Identity and access | numeric ID, owner, visibility, admin rights, organization policies | recheck live before change |
| Secrets and privacy | secret scanning status, historical leak assessment, personal/sensitive data classification | review privately; never copy secrets into logs |
| Branch governance | default branch, active rulesets, reviews, status checks, bypass principals | propose exact-rule hardening; do not apply silently |
| CI and dependencies | workflows, action pinning, token permissions, dependency alerts, artifacts | scoped least privilege, pinning and maintenance proposals |
| Licensing and provenance | license files, imported/fork origin, ownership, attribution, third-party terms | preserve upstream licenses; request rights review |
| Exposure surfaces | Pages, releases, packages, forks, webhooks, integrations, clone consumers | build an impact list before restricting access |
| Data lifecycle | external stores, backups, retention, deletion semantics, Drive routing | minimize identifiers and define private destination |
| Evidence and runtime | test matrix, exact HEAD, CI run, ARMv7/AArch64, Android device | never infer device PASS from source tests |
| Recovery | pre-change snapshot, receipt path, reconciliation protocol, explicit reverse approval | ensure post-change readback and rollback plan |

Missing evidence is `TOKEN_VAZIO`, not false, zero, or PASS.

## Proposed bounded acquisition sequence — NOT IMPLEMENTED

- **P0 — Identity, privacy and governance:** finish authenticated pagination;
  check owner/ID/visibility consistency; inspect existing policy metadata without
  reading repository contents unnecessarily. No write.
- **P1 — Per-repo facts:** collect read-only facts by repo and endpoint, with
  page/cursor, timestamp, permitted scope and provenance. Rate-limit/backoff,
  incremental retries, and resume receipts; never represent truncated reads as
  exhaustive. Do not export private names or vulnerabilities into this public repo.
- **P2 — Proposals:** show `evidence -> risk -> proposed minimum delta ->
  dependency -> test -> rollback`. Forks and archives may require a different
  governance path rather than a visibility PATCH.
- **P3 — Authorized changes:** after individual, context-specific approval,
  execute only the selected action with live authority checks and durable receipts.
  A change to visibility and a change to branch policy require **separate**
  authorization. No mass publishing, no implicit deletion, no auto merge.
- **P4 — Readback:** compare exact repository IDs, destination state, errors,
  receipts, affected integrations and rollback ability. A scope-specific PASS
  does not establish legal compliance, global protection or device readiness.

## Falsifiers and gate

- An automatic PUBLIC plan with no checked IDs **must have zero selections**.
- A checked ID outside the selected owner scope or with unsupported visibility
  **must not enter the plan**.
- A search filter must not silently drop already-selected IDs from execution:
  the UI must show the hidden-selected count and the confirmation list.
- A changed list between preview and confirmation **must be rejected**.
- An overlapping mutation/refresh **must not enqueue duplicate PATCH operations**.
- If a refresh fails, the UI must not offer a stale inventory as eligible.
- Exact-head Kotlin tests, Compose build/lint, Android instrumentation, GitHub
  API canary and receipt recovery are `TOKEN_VAZIO / NOT_RUN` here.

**Promotion gate:** `claim_allowed=false` until exact-head tests, builds and
relevant device/provider evidence are recorded. Keep this branch isolated and
unmerged for review. Source rollback = revert reviewed candidate commits on a
separate branch; remote visibility reversal, if ever applied with explicit
authorization, is a distinct provider action requiring its own receipts.

## R3

`F_ok`: candidate source plus blob readback, scoped only.
`F_gap`: full per-repository protection inventory and CI/device tests absent.
`F_next`: exact-head Android tests, independently reviewed design, then
read-only evidence collection with private storage and incremental receipts.
