# RafGitTools Android Conversation Manifold V1

State: `SPEC_MATERIALIZED / PER_FILE_FLOW_MERGED / BATCH_GAPS_OPEN / DEVICE_E2E_NOT_RUN`
claim_allowed: false

## Intent

Make **RafGitTools APK itself** the mobile operator for the user's explicit route:

`Android RafGitTools → Google Drive/NOVOexport (read-only) → on-device resumable processing → private CONVERSATIONS_CHUNKS_PRIVATE`.

This is an application capability, not a Termux/rclone workflow and not a separate app. The user selects the Drive source from inside RafGitTools; the Android provider grants access through SAF. The GitHub destination is `rafaelmeloreisnovo/CONVERSATIONS_CHUNKS_PRIVATE`, subject to live privacy and authorization checks.

## Current source baseline observed 2026-09-29

Already present in RafGitTools source:

- HomeScreen SAF picker and `stageDriveDocument` stream-to-private-storage path;
- `DriveStagingGate` readback SHA-256/byte-count receipt;
- `CorpusIntakeGate` bounded structural JSON intake;
- sanitized private activity receipt writer with a private-target live-readback gate;
- RafGitFS governed branch/commit/push/PR machinery;
- Python `tools/rafaelia_navigator/` local-first parser, SQLite/FTS, conversation/node/message parent graph, content hashes, append-only checkpoints, deterministic publication and bounded query;
- source-lock and corpus logistics/canary contracts for Drive/NOVOexport and the private destination.

These facts establish source presence only. They do not prove the APK invokes the Python Navigator, emits its SQLite/FTS/typed graph, completes all Drive items, or has run on a physical phone.

## Missing mobile vertical slice

The code and acceptance work must connect these components inside RafGitTools:

1. A dedicated **NOVOexport Navigator** entry in the app UI, with Drive source selection and visible destination binding.
2. SAF tree/document provider support that enumerates all eligible files under the selected NOVOexport root; no assumption that selecting one file means the corpus is inventoried.
3. A persistent queue keyed by provider identity + relative logical path + source SHA-256. Unknown provider identities remain `TOKEN_VAZIO`.
4. Bounded, cancellable, resumable processing with one file/transaction/checkpoint at a time; verify stream byte count and SHA-256 before parse; source remains read-only.
5. Native Android parser/index implementation or a specifically justified in-process route; the APK must not silently require Termux, shell access, or a server.
6. Per-conversation structural records: source pointer, conversation/message/node IDs, parent edges (including nodes without messages), role, timestamps, content type, assets/errors, lexical tokens and missing fields. Values absent in source remain `TOKEN_VAZIO`, never fabricated as zero.
7. Local SQLite/FTS search and typed routes/manifold/atlas projections. Semantic embeddings, model-derived similarity, causal claims and scientific promotion remain separate opt-in gates with explicit model/source authority.
8. A deterministic private publication plan that writes derived index/chunk artifacts and receipts into the private repository only, using RafGitFS or an audited adapter. Require confirmation over the exact plan hash; perform live visibility/auth/readback and reject public or unexpected targets.
9. Read-back verification of every published blob/commit and append-only run receipt linking source generation, per-file hashes, parser/schema/app versions, output hashes, checkpoints, gaps and next cursor.
10. UI states for inventory, queued/processed/blocked counts, bytes, current path, checkpoint, pause/resume/cancel, battery/storage/network constraints and recoverable errors.

## 25 GB handling contract

The 25 GB figure is a user-reported scope, not a current measured inventory in this delta. Do not load the corpus into memory or a single Git commit. Process files incrementally, enforce configurable per-file and total-storage limits, retain resumable cursors, and publish bounded batches. Never commit raw corpus bodies by default. Derived conversation chunks can contain private text and therefore belong only in the private repository under its privacy gate. Keep local indexes/private files app-scoped and deletable through an explicit retention control.

A completed run must reconcile:
- all listed provider objects and exclusions;
- files/bytes read and hashed;
- files parsed/indexed/blocked;
- conversations, nodes, messages, assets and unlinked/orphan edges;
- output objects and hashes;
- remaining cursor and failure reasons.

No count is complete until an exhaustive provider enumeration and terminal cursor are observed.

## Minimum implementation sequence

- **M0 — contract/tests:** schema for job/checkpoint/output manifest; synthetic multi-file fixture; restart/idempotency, source-change, malformed-file, permission-loss, duplicate-node, node-without-message and low-storage tests.
- **M1 — Android discovery:** dedicated screen, SAF root selection, recursive provider enumeration and persistent inventory/checkpoint; no Git writes.
- **M2 — bounded Navigator:** integrate the parser/SQLite/FTS and typed graph in-process; verify small synthetic fixture against Python Navigator reference outputs.
- **M3 — private publication:** preview exact content-addressed output tree; explicit confirmation; batch commit through RafGitFS; private-target live-readback and post-write readback.
- **M4 — device canary:** on the phone, process one small real Drive JSON read-only, compare source hash and local index/output receipt, publish only sanitized receipt first.
- **M5 — private corpus batch:** user-visible bounded pilot on a small real batch; reconcile and review before larger batch.
- **M6 — full inventory/process:** only after canary gates pass, continue resumably through every enumerated source item until terminal reconciliation. Do not describe an interrupted or partial scan as complete.

## Evidence gates

| Gate | Required evidence | State now |
|---|---|---|
| Source-level Android route | code path from app screen through intake to Navigator | `TOKEN_VAZIO` |
| Contract/fixture | schema + meaningful tests | `TOKEN_VAZIO` |
| Android job | exact-head unit tests, lint, devDebug APK and APK hash verification | `PASS`; overall final receipt `PENDING` |
| Device install/launch | installed artifact SHA + device receipt | `NOT_RUN` |
| Drive exhaustive inventory | all pages/items + terminal cursor | `TOKEN_VAZIO` |
| End-to-end canary | source/output hashes and private readback | `NOT_RUN` |
| Full corpus completion | reconciled counts/bytes + terminal cursor + receipts | `NOT_RUN` |
| Claim/release | applicable independent gates | blocked |

## Authority and privacy

- Google Drive/NOVOexport is immutable source authority.
- RafGitTools GitHub is the APK/code/schema/test/CI implementation authority.
- CONVERSATIONS_CHUNKS_PRIVATE is the destination authority for corpus-derived private indexes/chunks.
- No corpus body or Drive capability URI belongs in this public repository.
- User-selected Drive access is granted by the Android document provider; the APK must request only this user-mediated access. This capability does not silently add Google OAuth scopes or assume account access.
- GitHub auth in the APK is a distinct capability; no credential is copied from one lane to the other.

## R3

F_ok: current app staging/receipt route and Python Navigator tooling are observed in source; destination repository is private and identified.
F_gap: the full APK-to-Navigator data model, recursive Drive inventory, persistent mobile batch checkpoints, provider readback and physical-device run are not proven; Android CI has passed for the per-file implementation.
F_next: implement M0/M1 as a reviewable APK feature, then close M2-M4 with synthetic/reference parity and a phone canary before scaling to the reported 25 GB.


## Implementation checkpoint — 2026-09-29

PR #575 introduced the per-file processor, bounded publisher, tests and a Drive-tab Process action. PR #576 fixed Kotlin compilation issues in the processor/publisher and was merged at commit `695bed743c5b22427ebc7f6cf4b71169208001bc`. The app now has a source-level per-file action; it does not yet have recursive NOVOexport tree inventory or a persistent resumable batch queue.

For workflow run `36558610755`, the Android/test/lint/devDebug APK job `109375861051` completed successfully: unit tests, lint, devDebug assembly and APK hash verification passed. Python deterministic tests and CodeQL Java/Kotlin also passed. The overall run's final receipt job `109382657128` remains queued. No APK installation, phone execution, Drive provider readback, or 25 GB corpus enumeration has been evidenced.

Current split: `IMPLEMENTED_SOURCE_PER_FILE`; `BATCH_INVENTORY=NOT_IMPLEMENTED`; `ANDROID_CI=ANDROID_JOB_PASS_FINAL_RECEIPT_PENDING`; `DEVICE_E2E=NOT_RUN`; `MEASURED_CORPUS_BYTES=TOKEN_VAZIO`; `claim_allowed=false`.
