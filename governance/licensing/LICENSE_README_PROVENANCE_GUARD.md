# LICENSE / README / Provenance Guard

Status: audit-policy-v1
Date: 2026-08-15
Scope: RafGitTools federation targets and repository maintenance workflows.

## Purpose

Prevent false authorship, license drift, missing attribution and repeated re-reading of already verified license/README state while preserving fail-closed behavior.

## Order of operations

`UPSTREAM_IDENTITY -> UPSTREAM_LICENSE/README -> TARGET_LICENSE/README -> PROVENANCE_BOUNDARY -> OBLIGATION -> ACTION -> RECEIPT`

No attribution-sensitive code/documentation change should skip this order.

## Authorship classes

- `THIRD_PARTY`: external origin demonstrated.
- `FORK_UPSTREAM`: repository or substantial work remains based on an upstream project.
- `VENDORED_DEPENDENCY`: external dependency copied/vendorized into the tree.
- `THIRD_PARTY_WITH_USER_MODIFICATIONS`: external work with user modifications that do not establish an independently authorial whole.
- `EXTERNAL_REFERENCE_ONLY`: external source used as a technical reference/capability reminder without adopting its expressive structure.
- `CLEAN_AUTHORIAL_COMPONENT_VERIFIED`: independently conceived, structured and implemented component with a separable provenance boundary.
- `USER_AUTHORIAL_STANDALONE_VERIFIED`: standalone work with sufficient provenance to support independent authorship.
- `UNKNOWN_ORIGIN/TOKEN_VAZIO`: origin cannot yet be demonstrated.

Different programming language, rewritten syntax, formatting, renaming, mechanical refactoring or quantity of changed lines are not evidence of independent authorship by themselves.

## Verification cache

Use `VERIFICATION_LEDGER.md` as a reusable cache. The primary invalidator is evidence change, not elapsed time.

Minimum fingerprint set:

- target repository and ref;
- target `LICENSE`/equivalent blob SHA;
- target `README` blob SHA;
- upstream repository/identity;
- upstream license ref or immutable fingerprint when available;
- verification date/time;
- classification and obligations;
- verifier/evidence pointers.

A record is reusable when all relevant fingerprints still match. Re-run verification immediately when any fingerprint or upstream relationship changes, or when a new third-party component enters scope.

## Enforcement

Safe automatic corrections are limited to already-proven obligations such as attribution links, README references, NOTICE/index metadata and other reversible bookkeeping. The following require human review: relicensing, changing copyright/authorship, removing substantial code for license reasons, dual licensing, publication, merge and release.

`UNKNOWN != USER_AUTHORIAL`.

When exact legal consequences depend on jurisdiction, patent, contract or an ambiguous license interaction, preserve the facts and mark `LEGAL_REVIEW_REQUIRED`; do not manufacture a legal conclusion.
