# 𓂀ΔΦΩ — RAFAELIA C12 | FOLDER-ONLY TERMINAL SCOPED CAPTURE

**Recorded:** 2026-11-21 (UTC). **Classification:** PUBLIC-SAFE RECEIPT. No private Drive file IDs, names, account IDs, cursors, credentials, source text or raw manifests included.

## Authority, predecessor and intent

- Repository producer: `rafaelmeloreisnovo/RafGitTools`, isolated branch `rafcode/drive-parent-nullsafe-manifest-20261121-hold`; governed by local AGENTS.md.
- Append-only predecessor: `docs/audit/DRIVE_CAPTURE_C7_PAGE62_READBACK_20261121.md` Git blob `b71941f3967c3e52ae0c54c32756aa238d92c83c`.
- Canonical Drive routing: official AGENTS.md → START HERE Ω V2.2 → CURRENT_STATE `B0159_COMMITTED_20261115`, `PARTIAL`, `claim_complete=false`. DO NOT promote B0160 from a folder-only capture.
- Data custody: private Supabase `raf_private.drive_folder_capture_pages_v1`; independent read-only quality auditor `raf_private.drive_folder_capture_scoped_quality_v1`.
- Query contract: Google Drive search with `item_type=folder`, MIME folder and `trashed=false`, within the specified authorized accessible account context, paginated using individually stored opaque provider continuation tokens. `incompleteSearch` not separately established by this connector.

## Define / Measure — verified scoped terminal

- Two captures A/B from page index 0 through 131 (**132 consecutive pages each**); last page index 131 has **427 positions** and `next_cursor=NULL`, `is_terminal=true`, exactly once per capture.
- **60,687 provider positions PER capture, 60,687 DISTINCT stable folder IDs each**. Total stored: 264 page rows, **121,374 observations of the SAME 60,687 identities** across A+B, NOT 121,374 unique objects.
- Query-scoped A/B set intersection: 60,687; symmetric difference 0; selected metadata differences 0.
- Page-level: **132/132 JSON manifests equal** and same per-page SHA-256 across the two captures; provider pagination cursors differ legitimately.
- Auditor: 0 duplicate positions within either capture; 0 invalid IDs; 0 digest mismatches; 0 broken cursor links; no premature terminal. `TERMINAL_CAPTURE_SCOPED` for A and B, with `claim_allowed=false`.
- Historical B0154 folder membership (2,310 distinct stable IDs): **2,310/2,310 found** in the terminal scoped capture. Previous metadata-projection folders (396 distinct IDs): **396/396 found**. This disproves those exact historical sets being absent from this observed folder query; not a content-byte or future-presence guarantee.

## Full custody chain — append-only improvements after C7

Existing C7 predecessor had index 0–62, 63 pages and 28,980 IDs per capture. A further **69 pages per capture** were appended, adding **31,707 distinct positions per capture**, without overwriting earlier pages. Single-writer fencing tokens and closed ranges:

| Fence | Page indices | Page rows A+B | Positions A+B |
| --- | --- | ---: | ---: |
| 55 | 63–69 | 14 | 6,440 |
| 56 | 70–74 | 10 | 4,600 |
| 57 | 75–79 | 10 | 4,600 |
| 58 | 80–85 | 12 | 5,520 |
| 59 | 86–91 | 12 | 5,520 |
| 60 | 92–97 | 12 | 5,520 |
| 61 | 98–103 | 12 | 5,520 |
| 62 | 104–109 | 12 | 5,520 |
| 63 | 110–115 | 12 | 5,520 |
| 64 | 116–121 | 12 | 5,520 |
| 65 | 122–127 | 12 | 5,520 |
| 66 | 128–131 | 8 | 3,614 |

All batches were appended by `drive_folder_capture_append_batch_v1` (max 14 page records per atomic batch), validated against predecessor cursors, deduplicated against stored IDs, verified with post-write SQL auditor, and all leases released. An operation-message limit interrupted post-write cleanup of fence 55; direct SQL recovery confirmed persisted pages, readback and release. Neither data nor a lock was abandoned.

## Analyze / falsifiers — typed parent-field exception

- At the terminal page, **two folder records lacked the `parent_ids` key in Google_Drive.search (files.list)**. Treating missing as explicit null would have destroyed provenance.
- Independent `Google_Drive.get_file_metadata` (files.get) for the same two exact IDs returned `parent_ids=null`, matching identity, type and available source timestamps.
- Stored these composite metadata observations with `parent_ids=null` **sourced explicitly from files.get**, plus `parent_listing_state=TOKEN_VAZIO_PARENT_FIELD_ABSENT`, `parent_resolution_source=Google_Drive.get_file_metadata`, `parent_resolution_state=PROVIDER_EXPLICIT_NULL` and the read timestamp. No missing field was silently normalized to null.
- Existing six JSON-null parents per capture plus two independently resolved cases now yield **eight explicit-null parent values per capture**, of which precisely two have the typed listing-absence/fallback lineage.
- Structural auditor and set comparisons passed after the terminal append; no parent hierarchy, missing content, orphan status or provider root was invented.

## Cryptographic receipt — SCOPED, NOT file-byte SHA-256

Private SQL computed identical hashes for A and B; these SHA-256 values cover **stored identity keys or selected metadata**, never source-file bytes:

| Scope | SHA-256 |
| --- | --- |
| Ordered length-prefixed stable IDs (C collation) | `8e3f9b5c6884b6b4ce658c3311719125fef0e51b19056f57c66c99501a11657e` |
| Ordered canonical selected metadata JSONB | `7cb943fa978608b6c5d339b637bc2bde9f2101a8149d3bd494e681a78f32812d` |
| Ordered per-page manifest digest root | `801f61027dbda9593d1afaa9f13435dfbcaff6c564ade174d887840b830038ef` |

Canonical derivation and negative-gate queries: `docs/supabase/verify_drive_folder_terminal_scoped_v1.sql`.

## Improve / Control — boundaries and risk

- `CAPTURE_FOLDERS_TERMINAL=PASS_SCOPED`: TRUE only for the executed folder-only query, account and 2026-11-21 retrieval chain.
- `CROSS_CAPTURE_SET_UNIQUENESS=PASS_SCOPED`: 60,687 distinct within each capture and equal A/B; no duplicate stable ID across their 132 stored pages.
- `HISTORIC_60687_EQUIVALENCE=TOKEN_VAZIO_UTIL`: historical 60,687 provider positions aggregate equals the observed count, but historical per-position ID provenance was not supplied for object-by-object identity equivalence.
- `PROVIDER_SNAPSHOT_ISOLATION=NOT_PROVEN`: A/B were interleaved and the capture took hours; equal manifests do not establish one transactionally consistent Drive snapshot or demonstrate no mid-crawl omissions.
- `INCOMPLETE_SEARCH_FIELD=TOKEN_VAZIO`: connector's standalone `incompleteSearch` bit not verified.
- `CONTENT_BYTES_SHA256=TOKEN_VAZIO(NOT_READ)`: no source file bytes were fetched.
- `ALL_FILES_GITHUB_GLOBAL_RECONCILIATION=NOT_COMPLETED`: folder-only query does not establish file inventory or GitHub cross-source equivalence.
- `EXACT_HEAD_FULL_CI=NOT_PROVEN`: no complete GitHub CI pass asserted. Local synthetic tests from C5 are scoped evidence only.
- `B0160=BLOCKED`, `CLAIM_ALLOWED=false`, `CURRENT_STATE=B0159_COMMITTED`. No code merge, ACL change, new automation, publication of raw private IDs, or promotion of canonical inventory state.
- **Rollback:** append-only corrective successor bound to this receipt and source blob; never delete raw pages or silently rewrite the historical checkpoint.

R3 = <F_ok: terminal 132-page folder-only census and two matching 60687-ID manifests independently recomputed; F_gap: provider isolation/incompleteSearch/content bytes/CI/full federated reconciler; F_next: separately authorized and fenced file+GitHub cross-source reconciliation or changes-token baseline, with new receipts and negative tests>.

SOURCE ≠ ARTIFACT ≠ EXECUTION ≠ EVIDENCE ≠ CLAIM.
