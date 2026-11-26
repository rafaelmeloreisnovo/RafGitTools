# RAFAELIA Ω — Supabase private NULL-aware identity V2 | readback receipt (2026-11-21)

**Type:** append-only public *pointer* to a private database migration. No private file IDs, metadata rows, account IDs, tokens, credentials or content bytes here. Not a new inventory checkpoint.

## Provenance

- **Producer:** `rafaelmeloreisnovo/RafGitTools`, branch `rafcode/drive-parent-nullsafe-manifest-20261121-hold`.
- **Predecessor HEAD:** `a07efb3fe4def800a61514fd255c66cbb4243e31`.
- **Source DDL:** `docs/supabase/drive_metadata_identity_nullsafe_v2.sql`; verified Git blob `ed90ced1c71b6cf19cfdb9142e40dc6cfcda3b8b`.
- **Migration identity:** `20261121151557_rafaelia_private_drive_identity_nullsafe_v2_20261121` (private Supabase Rafcodephi).
- **Existing authority:** Drive AGENTS.md → START HERE Ω V2.2 → B0159 CURRENT_STATE; RafGitTools AGENTS.md governs producer code; Mapa AGENTS.md governs federation.
- **Concurrency:** acquire → validate → additive DDL → structural readback → release, fencing token bound to that execution. Release confirmed. No data mutation or permission grant.
- **Rollback:** additive V3 successor if correction needed. Do not drop V1, erase historical observations or silently change B0159.

## Observed tests and limits

- **MIGRATION_WRITTEN:** yes, authorized private `CREATE VIEW` plus comment.
- **POST_MIGRATION_READBACK:** PASS_SCOPED; V1/V2 original observation and identity totals equal; NULL-aware parent grouping detects all scoped undercounts; source observations were not rewritten.
- **PRIVACY_GATE:** `security_invoker=true` and non-exposed private schema confirmed by role privilege checks. `claim_allowed=false` and content equivalence `TOKEN_VAZIO` throughout the new view.
- **SECURITY_ADVISOR:** no finding targeting the new view among returned informational RLS notices; not a blanket security certification.
- **SYNTHETIC_POSTGRES_FIXTURE:** five edge cases passed in private read-only SQL.
- **OFFLINE_MANIFEST_UNIT_TESTS:** 17/17 passed locally on the development artifact; matching Git blob hashes after upload.
- **METADATA_PRODUCER_HASH_TESTS:** source test module added on isolated branch, not yet run against exact producer source or CI. `IMPLEMENTED_UNTESTED`, not PASS.
- **CI:** no status checks or PR-triggered workflow runs were reported for the working branch commit at inspection. `NOT_PROVEN`.
- **HISTORICAL_FINGERPRINT_PREIMAGE:** `TOKEN_VAZIO`, byte-exact reproduction not established.
- **GLOBAL_OLDER_DRIVE_MANIFEST:** `TOKEN_VAZIO` (historical chunk counts are not page ID manifests).
- **PROVIDER_SNAPSHOT_ISOLATION:** `NOT_PROVEN`; scoped double-probe does not prove globally stable listings.
- **B0160:** BLOCKED. B0159 unchanged; no new inventory receipt and no promoted inventory claim.

## Subsequent gates

1. Run the synthetic metadata producer contract tests on exact HEAD and examine why historical imports hold NULL despite present-day admitted producer records always assigning parent fields.
2. Perform an authorized, private *complete* provider `files.list` capture into per-page identity manifests with explicit scope, timestamps, sequence, completeness, and terminal status. Do not fabricate historical ID rows.
3. Independently repeat the entire capture and run `scripts/drive_identity_partition_reconcile_v1.py`; compare actual sets, scoped metadata and identity-only digests. Even two equal passes do not prove provider snapshot isolation.
4. Obtain byte equality evidence only through separately authorized source-byte custody; metadata SHA is not content SHA.
5. Only promote B0160 with a valid lease, fenced write, immutable partitions, predecessor receipt and independent readback. Until then, `claim_allowed=false`.

**R3:** F_ok = additive V2 migration + scoped readback + lease release; F_gap = historical canonical fingerprint preimage, complete global ID manifests and provider stability; F_next = exact-source contract test and private complete two-pass capture under authorized gates.
