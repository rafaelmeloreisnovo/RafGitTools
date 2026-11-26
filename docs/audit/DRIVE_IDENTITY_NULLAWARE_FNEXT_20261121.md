# RAFAELIA Ω — Drive identity NULL-aware F_next (2026-11-21)

**Class:** public contract/receipt pointer; private row IDs, contents, tokens and credentials excluded. **State:** implementation proposed; not a database deployment, not CI pass, not global inventory completion. `claim_allowed=false`.

## Provenance and authority

- Local producer: RafGitTools `AGENTS.md`, `scripts/drive_debianum_metadata_stream_v1.py`, `scripts/drive_debianum_recursive_stream_v2.py`, `docs/supabase/drive_metadata_identity_projection_v1.sql` at predecessor commit `a07efb3fe4def800a61514fd255c66cbb4243e31`.
- Federation: Mapa `AGENTS.md`; canonical Drive `START HERE Ω V2.2`, `CURRENT_STATE_B0159` and B0159 predecessor receipt. B0160 remains blocked.
- Supabase private read-only source: `raf_private.drive_metadata_index_v1` and the V1 identity projection. Do not expose private data or apply migrations from public CI.

## Scoped findings (not universal)

- V1 and historical B0154 have **different scopes**: B0154 is a folders-only membership set; the metadata projection contains folders and nonfolders. Exact live counts and member IDs remain in private database evidence, never this public file.
- SQL `count(distinct source_parent_id)` omits NULL. Null-aware row tuple counting detects missing parent variants. A private scoped check detected `NULL_AND_VALUE_OBSERVED`, `NULL_ONLY`, and multiple-fingerprint groups. Historical records remain untouched.
- V1/V2 Python producers hash a canonical JSON metadata record, not source content bytes. Their input schemas differ. NULL-to-value coincides with a fingerprint difference; **byte-exact causation of historical fingerprints is NOT_PROVEN** without original canonical payload.
- Fresh bounded Drive `files.list` probe (2026-11-21): two independent passes of two pages gave matching scoped ID sets and compared metadata; both passes **nonterminal**. Provider page tokens differed and are not comparable identifiers. Detailed counts remain in the private audit.
- Historical terminal probe (private Supabase continuity event seq 218) contains chunk counts but no discoverable per-position ID manifest in that record. Global dedup, stable snapshot and content hashes are unproven.

## Proposed, testable delta

- `docs/supabase/drive_metadata_identity_nullsafe_v2.sql` adds an independent, security-invoker private view without replacing V1. Preserves NULL, blank and present values, and records fingerprint co-occurrence without causal inflation.
- `docs/supabase/verify_drive_metadata_identity_nullsafe_v2.sql` contains read-only conservation, private-schema and negative-fixture gates.
- `scripts/drive_identity_partition_reconcile_v1.py` and synthetic tests verify *authorized, complete* per-page identity manifests offline; fail closed on missing/partial page, invalid ID/time, count mismatch and cross-page duplicates. The manifest SHA-256 covers identity keys only, not file content.

## Next acceptance gates

1. Independent code review and exact-head synthetic test; do not promote from implementation alone.
2. Authorized private SQL migration and readback, with current ACL and independent verification. No existing view drop or overwrite; rollback by additive corrective successor.
3. Export provider page IDs to private custody as explicit complete manifests (not counts-only); token hashes/references and query contract; keep originals protected. Do not infer IDs from aggregate events.
4. Independently repeat complete folder traversal, compare identity manifests, changes and timestamps. No `files.list` snapshot-isolation guarantee; even equal sets remain scoped observations.
5. Obtain byte-hash evidence only by explicit authorized file-byte access; Drive metadata fingerprint is not byte proof.
6. Only authorized writer may advance B0160 after lease/fencing, partitions, predecessor, receipt and readback; `claim_allowed` remains false meanwhile.

R3 = <F_ok: scoped independent diagnostics, F_gap: historical byte inputs + missing global ID manifest + provider stability, F_next: authorized migration/manifest capture>. No new B0160 receipt or provider completeness claim.
