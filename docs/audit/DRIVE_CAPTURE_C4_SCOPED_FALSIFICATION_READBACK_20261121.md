# 𓂀ΔΦΩ — RAFAELIA C4 | Scoped Capture Falsification & Readback, 2026-11-21

**Security classification:** PUBLIC-SAFE METADATA ONLY. Never include private Google Drive file IDs, names, cursors, account IDs, OAuth credentials, raw corpus, hashes of unobserved content or unapproved document text here.

**Predecessor:** `docs/audit/DRIVE_SIZE_GUARD_SCOPED_READBACK_20261121_C3.md`.
**Canonical inventory state:** `B0159_COMMITTED_20261115`, status PARTIAL, `claim_complete=false`. B0160 NOT PROMOTED.
**Producer authority:** `rafaelmeloreisnovo/RafGitTools`, branch `rafcode/drive-parent-nullsafe-manifest-20261121-hold`; GitHub AGENTS.md governs local code. Drive official AGENTS.md → START HERE Ω V2.2 → CURRENT_STATE governs private inventory.

## 6Σ / DMAIC / falsifiers

1. Define: determine whether historical folders are truly missing, whether large captured prefixes duplicate IDs, and whether later reobservations change metadata, WITHOUT claiming a full snapshot.
2. Measure: private source `raf_private.drive_folder_capture_pages_v1`, two separate nonterminal 60-page captures, each 27,600 provider positions. PostgreSQL SQL returned exactly 27,600 distinct IDs in EACH; across-page duplicated identity positions = 0; source page digest mismatches = 0; request/continuation chain breaks = 0.
3. Analyze adversarial hypotheses:
   - H_loss: historical B0154 folders absent in the two captured prefixes. **REFUTED_SCOPED:** all 2,310 B0154 distinct folder IDs overlap EACH prefix. This does not prove permanent availability or source-byte integrity.
   - H_cross_page_dup: some object IDs repeat within or across each 60-page prefix. **REFUTED_SCOPED:** each prefix has 27,600 unique IDs from 27,600 positions.
   - H_temporal_independence: matching capture A/B proves broad temporal stability. **NOT_SUPPORTED:** per-page stored observation times differ only by 1–4 ms; not independent temporal snapshots.
   - H_missing_projection_folder: 39 known projection folders absent from the prefix were lost. **REFUTED_IN_CURRENT_METADATA_PROBES:** each of 39 was independently returned by Google Drive `files.get` as a folder with parent metadata. Still outside the captured first 60 pages; global `files.list` terminal remains unknown.
   - H_orphan: six folder records with JSON `parent_ids=null` necessarily mean corruption. **REFUTED_AS_AUTOMATIC_CLASSIFICATION:** direct Google Drive `files.get` for all six also returned no parent list. State = `PARENT_ABSENT_IN_PROVIDER_RESPONSE`; hierarchy = `TOKEN_VAZIO_UTIL`, not false.
   - H_metadata_drift: 920 IDs in first two pages changed on a later reobservation. **REFUTED_SCOPED:** fresh two-page lookup matched all 920 IDs, order, `parent_ids`, creation and modification timestamps after correct alias normalization; both pages were nonterminal. A preliminary comparator with wrong aliases falsely marked 920 dates changed; this comparator was rejected before claim.
4. Improve: private additive SQL view `raf_private.drive_folder_capture_scoped_quality_v1` deployed and read back, migration `20261121175152_rafaelia_private_drive_folder_capture_scoped_quality_v1_20261121`. Source Git blob `43eb53b63df4e52abb006e17503100f17d8182bf`. It recomputes counts, duplicates, page digest, cursor chain, parent JSON-null states and classifies both as `CONSISTENT_PREFIX_NONTERMINAL`; `snapshot_isolation=NOT_PROVEN`, `source_content_sha256=TOKEN_VAZIO`, `claim_allowed=false`. The migration used lease fencing token 44, released after readback.
5. Control: pure stdlib host-side alias normalizer `scripts/drive_folder_metadata_normalize_v1.py` Git blob `a80b1d229e2e027a545d4d826d407e5a5f79308d`; synthetic test suite `tests/test_drive_folder_metadata_normalize_v1.py` Git blob `ed47735c27e8de2c1c04c3a18dcf6b39a3cecfcf`. **15/15 local tests PASS_SCOPED** and exact Git blob readback after writes under fencing token 45, released. Comparison distinguishes `PARENT_EXPLICIT_NULL`, `FIELD_ABSENT`, `EMPTY_ARRAY`, `OBSERVED_ARRAY`; does not fabricate bytes. Full-module/CI tests are NOT_PROVEN.
6. Evidence and rollback: `SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM`. Original append-only capture rows and B0159 pointer unchanged. New SQL is additive with `security_invoker=true` in private schema; rollback means corrective additive successor, not dropping original tables or erasing receipts.

## External methodological context (NOT authorization, evidence of RAFAELIA results, or authorship proof)

- Google Drive `files.list`: `nextPageToken` continuation, `incompleteSearch`; invalidated tokens require restarting enumeration. <https://developers.google.com/workspace/drive/api/reference/rest/v3/files/list>.
- Google Drive `changes.list`: change-log and `newStartPageToken` are candidates for a delta-reconciliation architecture, but this connector has not established a verified changes-token receipt. <https://developers.google.com/workspace/drive/api/guides/manage-changes>.
- alphaXiv private reading library contains `Reproducibility is Not Enough: Artifact Verifiability in Decentralized-Build Package Ecosystems` (arXiv:2608.18180) and `Provenance-Enhanced Statements in Knowledge Graphs` (arXiv:2606.15246). Existing bookmarks are prior-art context only.
- Undermind academic search returned `Provenance for the Cloud` (2010) and `Syncpal: A Simple and Iterative Reconciliation Algorithm for File Synchronizers` (2019). These are conceptual comparators, not proof of source-code similarity or scientific validation of this implementation.

## Typed gaps and next constrained action

- `GAP_GLOBAL_TERMINAL = TOKEN_VAZIO_UTIL(PENDING_COMPLETE_TRAVERSAL)`; two immutable prefixes are nonterminal.
- `GAP_PROVIDER_SNAPSHOT_ISOLATION = NOT_PROVEN`; two near-simultaneous responses are not independent over time; one later 920-item prefix was reobserved without changes.
- `GAP_CONTENT_BYTES_SHA256 = TOKEN_VAZIO(NOT_READ)`; metadata JSONB SHA is a page receipt, NOT a file-content hash.
- `GAP_EXACT_HEAD_FULL_CI = NOT_RUN`; local synthetic verification is scoped.
- `GAP_COMPARABLE_GLOBAL_MANIFEST = PENDING`; the historical 60,687 provider positions are not a complete audited identity manifest equivalent to this folder-only prefix.
- `GAP_CHANGES_API_SYNC = TOKEN_VAZIO(UNSUPPORTED_BY_CURRENT_DRIVE_CONNECTOR)`; require explicit source-authorized getStartPageToken / changes.list and typed removal semantics if implementing later.
- `GAP_PRIVATE_ID_CUSTODY = PASS_SCOPED` for existing private captured pages; no public replication permitted.

**Next:** test full producer suite on exact GitHub HEAD through an authorized safe runner; obtain terminal provider enumeration (or change-log reconciliation with explicit token baseline) under private custody, fenced writes and readback; do not advance B0160 without global uniqueness, temporal and predecessor gates.

R3 = <F_ok: private readback + complete B0154 overlap inside two prefixes + local alias hotfix and 15 tests, F_gap: global terminal / time isolation / bytes / CI, F_next: one bounded, private and independently verified complete capture or authorized changes-token baseline>.

`CLAIM_ALLOWED=false` · `B0160=BLOCKED` · `CURRENT_STATE=B0159_COMMITTED`.
