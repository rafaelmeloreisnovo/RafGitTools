# RafGitTools Android Conversation Manifold V1

State: `SAF_INVENTORY_AND_QUEUE_CI_PASS / ONE_ITEM_EXECUTOR_SOURCE_IMPLEMENTED_UNTESTED / DEVICE_E2E_NOT_RUN`
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

The native Android path now includes per-file JSON processing/publication, metadata-only recursive SAF inventory and persistent private queue state. PR #583 exact-head CI passed. This does not prove physical Drive enumeration, full-corpus execution, process-death recovery, or integration with the separate Python SQLite/FTS Navigator.

## Current mobile vertical slice status

| Component | State | Evidence boundary |
|---|---|---|
| Drive-tab file/tree selection | SOURCE_PRESENT | SAF picker and user-granted URI permissions exist |
| Recursive SAF inventory | SOURCE_CI_PASS | PR #581; metadata only, bounded at 100,000 documents, source bytes stay unopened |
| App-private queue | SOURCE_CI_PASS | PR #583; stable candidate identity, persisted retry states, interrupted PROCESSING recovery on re-inventory |
| Foreground one-item executor | SOURCE_IMPLEMENTED_UNTESTED | This branch reads one queued URI, runs the Kotlin processor, and requests explicit per-file publication confirmation |
| Queue completion | SOURCE_IMPLEMENTED_UNTESTED | COMPLETE is gated on PUBLISHED_READBACK_VERIFIED plus Drive and Git readback flags |
| Background worker/process-death recovery | NOT_IMPLEMENTED | Foreground coroutine only; re-inventory recovers an interrupted item |
| Native processor | SOURCE_CI_PASS | One bounded top-level JSON array record at a time; default limits 2 GiB/source and 16 MiB/record |
| Drive/Git publication readback | SOURCE_CI_PASS | Per-file V2 publication verifies byte count and SHA-256 before completion receipts |
| Python SQLite/FTS parity in APK | NOT_IMPLEMENTED | Python Navigator remains separate from the Android processor |
| Physical phone canary | NOT_RUN | No installed APK/device receipt |
| Measured 25 GB inventory | TOKEN_VAZIO | No physical terminal cursor or reconciled streamed bytes |

Queue state is runtime state, not evidence that a source file was processed or published. Each queued file needs its own confirmed plan.

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

- **M0 — contract/tests:** per-file processor and queue-store tests passed the base queue-chain CI. This successor adds interrupted-state recovery and readback-gated completion tests; exact-head CI is pending. Full multi-file integration, source-change, low-storage and process-death tests remain.
- **M1 — Android discovery:** SAF tree selection, recursive metadata inventory and persistent app-private candidate queue are implemented and passed CI in PRs #581/#583. A dedicated Navigator screen and physical provider-enumeration receipt remain.
- **M2 — bounded Navigator:** integrate the parser/SQLite/FTS and typed graph in-process; verify small synthetic fixture against Python Navigator reference outputs.
- **M3 — private publication:** per-file exact-plan confirmation and Drive/Git readback are source-implemented and passed CI. Multi-file batch planning and batch receipt remain.
- **M4 — device canary:** on the phone, process one small real Drive JSON read-only, compare source hash and local index/output receipt, publish only sanitized receipt first.
- **M5 — private corpus batch:** user-visible bounded pilot on a small real batch; reconcile and review before larger batch.
- **M6 — full inventory/process:** only after canary gates pass, continue resumably through every enumerated source item until terminal reconciliation. Do not describe an interrupted or partial scan as complete.

## Evidence gates

| Gate | Required evidence | State now |
|---|---|---|
| Source-level Android route | Drive tab through queue URI to processor/publication | `PARTIAL`: per-file route is CI-tested; queue executor source in this branch awaits exact-head CI |
| Contract/fixture | schema + queue recovery/readback tests | `PARTIAL`: processor and queue-store CI passed; new executor-gate tests await exact-head CI |
| Android job | unit tests, lint, devDebug APK and APK hash verification | base queue chain `PASS` in run `36565432525`; current executor branch `PENDING` |
| Device install/launch | installed artifact SHA + device receipt | `NOT_RUN` |
| Drive exhaustive inventory | provider items + terminal cursor | `NOT_RUN` on phone; source inventory is metadata-only and bounded |
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
F_gap: exact-head CI for the foreground queue executor, background/process-death recovery, Python SQLite/FTS parity, physical-device canary and measured corpus inventory remain open.
F_next: close executor CI, then install that exact devDebug APK and run one small Drive JSON through queue, processing, explicit confirmation and both provider readbacks.


## Implementation checkpoint — 2026-09-29

PR #575/#576 introduced the per-file processor and UI; PR #579 added Drive/Git readback; PR #581 added recursive metadata-only SAF inventory; PR #583 added persistent app-private queue state. Those source changes passed run `36565432525`. This branch wires one queued URI into the processor and gates completion on verified two-provider readback; its exact-head CI is pending.

For run `36565432525` (PR #583), Android tests, instrumentation compilation, lint, devDebug assembly and APK SHA verification passed; Python tests and CodeQL Java/Kotlin/Actions passed. Final receipt `109399071582` is `PASS_WITH_TYPED_SKIPS`; signed release was skipped. APK SHA-256: `a848605f27aee58fe2099ed5fa117d2fe83a1700b3b3045ccf41964e280f4de7` (artifact `11032230928`). No APK install or physical phone execution is evidenced.

Current split: `PER_FILE_AND_SAF_QUEUE_CHAIN=CI_PASS`; `ONE_ITEM_EXECUTOR=SOURCE_IMPLEMENTED_UNTESTED`; `DEVICE_E2E=NOT_RUN`; `MEASURED_CORPUS_BYTES=TOKEN_VAZIO`; `claim_allowed=false`.
