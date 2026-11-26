# RAFAELIA Ω — C13 | Terminal folder-only coverage product V1

Date: 2026-11-21 (UTC).
Status: **WRITTEN_READBACK / PASS_SCOPED**, **claim_allowed=false**.
This public-safe receipt does not contain private file IDs, filenames, account identifiers, opaque cursors, credentials, or raw manifest rows.

## Product and producer
- Product ID: `RAFAELIA_FOLDER_TERMINAL_SCOPE_COVERAGE_V1`.
- Version: `1.0.0` (additive private SQL view).
- Producer: `rafaelmeloreisnovo/RafGitTools`, isolated branch `rafcode/drive-parent-nullsafe-manifest-20261121-hold`.
- Source and SQL contract: `docs/supabase/drive_folder_terminal_scope_coverage_v1.sql`, Git blob `208f13fa8404fe401a7b132272c19d7ba6d8e449`.
- Private deployed view: `raf_private.drive_folder_terminal_scope_coverage_v1` via migration `rafaelia_private_drive_folder_terminal_scope_coverage_v1_20261121`.
- Immutable predecessor: `docs/audit/DRIVE_CAPTURE_C12_TERMINAL_SCOPED_READBACK_20261121.md`, Git blob `17f10c78c3203ddf722c4c79c49e1b1497d6602e`.
- Underlying source: private `drive_folder_capture_pages_v1` terminal A/B (132 pages and 60,687 folder IDs each); historical B0154 membership and the folder portion of Drive metadata index.

## Verified delta and count denominator
- New source object IDs: **0**. No pagination advance; no copy of 60,687 folder IDs into a second table.
- Derived classification (denominator: **60,687 distinct folder IDs** from one terminal query-scoped capture):
  - B0154 and metadata projection: **236**.
  - B0154 only: **2,074**.
  - Metadata projection only: **160**.
  - Neither of those two narrower historical sources: **58,217**.
- Conservation: `236+2074+160+58217=60687`. Historical coverage: `236+2074=2310` of 2310; projection coverage: `236+160=396` of 396. Readback: `conservation_ok=true`, `historic_covered=true`, `projection_covered=true`, `scoped_acceptance=true`, `claim_allowed=false`.
- CRITICAL: **58,217 absent from two specific indexes** ≠ missing files, deleted files, unowned files, unindexed across every system, or data loss.

## DMAIC / falsification and rollback
- Preflight: official private AGENTS.md → START HERE Ω V2.2 → CURRENT_STATE B0159; C12 predecessor Git blob verified, private ACL verified, no existing target view.
- Dry-run SQL returned the exact four-way partition and account/query/provider scope cardinalities of 1, plus two clean terminal auditors.
- 4 synthetic PostgreSQL adversarial cases executed and passed: clean partition, duplicate-source positions, missing historical membership, and false terminal claim. These are scoped algebraic falsifiers, not CI or provider E2E.
- Writer lease resource `RAFAELIA:INVENTORY:DRIVE_GITHUB`, fencing token **68** validated before every mutation, released on completion.
- Source SQL was committed to isolated GitHub branch and read back exact bytes. Supabase migration applied, view readback confirmed scoped counts, view `security_invoker=true`, `anon`/`authenticated` without private schema USAGE; original 264 capture page rows preserved.
- No row deletion, checkpoint change, access control modification, PR, merge, CI dispatch, release, deployment, or publication of sensitive identifiers.
- Rollback: append-only corrective successor with source/receipt references. Do not drop original view or erase any capture rows.
- Cursor before/after: **NOT_APPLICABLE** (read-only projection over already-terminal source).
- Source content-byte SHA-256: `TOKEN_VAZIO(NOT_READ)`.
- Historical 60,687 provider-positions individual-ID equivalence: `TOKEN_VAZIO_UTIL(NO_HISTORIC_POSITION_IDS)`.
- Provider transaction-level snapshot isolation and Drive `incompleteSearch` bit: `NOT_PROVEN` / `TOKEN_VAZIO`.
- All-file Drive↔GitHub reconciliation, GitHub CI: `NOT_COMPLETED` / `NOT_PROVEN`.
- Canonical checkpoint: `B0159_COMMITTED`; `B0160=BLOCKED`; global `CLAIM_ALLOWED=false`.

R3 = <F_ok: one private additive coverage product with 60687-count conservation and scoped readback; F_gap: source completeness semantics and cross-source all-files/CI; F_next: provenance-controlled all-file inventory comparison or authorized Drive changes-feed baseline>.
