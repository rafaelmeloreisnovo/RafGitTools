# RAFAELIA Ω — C4 corrigendum | 2026-11-21 (append-only)

**Supersedes exactly one label** in predecessor `docs/audit/DRIVE_CAPTURE_C4_SCOPED_FALSIFICATION_READBACK_20261121.md`, Git blob `62d97d86a84158ccf5a9f0093f2f524db80121ce`. Original receipt stays preserved.

- Previous label in the predecessor: `PARENT_EXPLICIT_NULL`.
- **Correct, implementation-authoritative literal label:** `PROVIDER_EXPLICIT_NULL`.
- Code authority: `scripts/drive_folder_metadata_normalize_v1.py`, Git blob `a80b1d229e2e027a545d4d826d407e5a5f79308d`; corresponding 15-case synthetic test suite Git blob `ed47735c27e8de2c1c04c3a18dcf6b39a3cecfcf`.
- Related distinct states: `TOKEN_VAZIO_PARENT_FIELD_ABSENT` when the parent field is absent, `EMPTY_ARRAY` for an explicit empty parent array, `OBSERVED_ARRAY` when a validated nonempty parent array is returned.
- None of these values alone proves source-file deletion, root status, global completeness, temporal isolation or byte-level content equivalence.
- No private IDs, names, cursors, content or account identifiers included; no modification to historical capture rows.
- `CURRENT_STATE=B0159_COMMITTED` | `B0160=BLOCKED` | `CLAIM_ALLOWED=false`.

R3: F_ok=corrected label by verified producer constant; F_gap=global terminal/provider snapshot/CI/content bytes; F_next=obtain independent terminal source capture and exact-head test receipts.
