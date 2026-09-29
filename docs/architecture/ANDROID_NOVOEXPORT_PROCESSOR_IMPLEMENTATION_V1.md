# RafGitTools Android NOVOexport Processor — Implementation V1

State: `PROVIDER_READBACK_SOURCE_IMPLEMENTED_UNTESTED / NEW_EXACT_HEAD_CI_NOT_RUN / DEVICE_NOT_RUN / BATCH_INVENTORY_NOT_WIRED`
claim_allowed: false

## Correct execution model

The user's phone runs the RafGitTools APK. RafGitTools reads the existing Drive files named `conversation*.json` and `codex*.json`, derives private JSONL index/chunk artifacts, and publishes them to a user-selected Drive folder and the private `CONVERSATIONS_CHUNKS_PRIVATE` repository. The user-mediated Android SAF provider handles Drive account selection and folder permissions. This work does not process the corpus in ChatGPT, Termux, a server, or a model-training job.

## Implementation status at main

The per-file processor, publisher, Drive-tab action and tests were introduced by PR #575 and compile/publishing fixes by PR #576. Both PRs are merged. The previous exact-head Android job remains non-terminal at the recorded observation. This successor adds fail-closed provider readback source: every derived part is re-read from the selected Drive provider and from the live-verified private GitHub target and checked against its expected byte count and SHA-256. A completion receipt is written only after all parts pass both provider readbacks. New exact-head CI for this successor is `NOT_RUN` until the branch workflow executes; physical-device execution remains `NOT_RUN`.

## Implemented source

- `app/src/main/kotlin/com/rafgittools/navigator/ConversationManifoldProcessor.kt`
  - streams a top-level JSON array one record at a time;
  - hashes every source byte during processing;
  - rejects unsupported source names/shapes, malformed JSON, records over 16 MiB, and files over the configured 2 GiB default;
  - emits conversation, every mapping node (including nodes without messages), message chunks, and Codex JSON fragments as derived JSONL;
  - splits large text into reconstructible 64 KiB UTF-8 chunks;
  - atomically promotes output only after successful parsing and appends a file-complete checkpoint.
- `app/src/test/kotlin/com/rafgittools/navigator/ConversationManifoldProcessorTest.kt`
  - synthetic conversation and Codex inputs;
  - large-message reconstruction;
  - malformed/trailing-comma, unexpected source, source-size and record-size rejection.
- `app/src/main/kotlin/com/rafgittools/navigator/ConversationManifoldPublication.kt`
  - creates a metadata-only plan with source/output hashes and bounded Git-sized parts;
  - requires the confirmation value to equal the exact plan SHA-256;
  - writes each derived part to a user-selected Drive SAF folder and a Git private-processing namespace through a caller-supplied writer;
  - re-reads every Drive artifact through `ContentResolver.openInputStream` and every private Git artifact through a live Contents API GET; byte count and SHA-256 must match the confirmed plan;
  - writes a V2 completion receipt with `PUBLISHED_READBACK_VERIFIED` only after all derived parts pass both readbacks, then re-reads the receipt itself on both providers;
  - any mismatch, permission loss, missing content or non-private Git target fails closed and cannot return a verified publication state.

The Drive tab in `app/src/main/kotlin/com/rafgittools/ui/screens/home/HomeScreen.kt` exposes a per-file Process action, a user-selected Drive destination, an exact plan-hash confirmation, and publication through `HomeViewModel.publishConversationManifold`. Git writes use `GithubDataRepository.createPrivateProcessingFile`, which checks live repository privacy and enforces the 512 KiB namespace boundary. The successor uses `readPrivateProcessingFile` for uncached live readback of the same namespace and verifies the returned UTF-8 bytes before promotion. Commit SHAs are retained. This establishes source implementation of per-file dual-provider readback; it does not establish exact-head CI PASS, a complete batch workflow, or operation on the user's handset.

## Still required before the phone can run the full route

1. Add a dedicated NOVOexport batch/inventory screen and bind `ACTION_OPEN_DOCUMENT_TREE` for the source tree with persisted SAF grants. The current Drive-tab action selects one file at a time.
2. Enumerate descendants of the selected source tree and filter `conversation*.json` and `codex*.json`; persist the queue and per-file status so app restart resumes safely.
3. Feed each source URI to `ConversationManifoldProcessor` on an IO/background executor and persist checkpoints, cancellation, permission loss and storage errors.
4. Extend the existing per-file preview/plan confirmation into a batch queue view with per-file status and resume controls; publish only after explicit confirmation through the live-private GitHub writer.
5. Obtain terminal exact-head CI for the provider-readback successor; `IMPLEMENTED_UNTESTED != PASS`.
6. Install the exact tested APK on the phone and pass a small Drive→process→Drive/Git canary, including the V2 readback receipt, before enabling multi-gigabyte batches.

## Limits and privacy

- The 25 GB value is the user's reported scope. No exhaustive recursive Drive enumeration or measurement exists in this evidence set.
- No source file is changed or deleted.
- Corpus-derived text in chunks is private content: publish only to the selected Drive destination and a live-verified private GitHub repository. RafGitTools public source receives code/tests only.
- The processor currently requires a top-level JSON array and blocks records over its configured limit. Those boundaries must be surfaced in the app and recorded as gaps, never silently skipped.
- Per-file checkpoints make a completed file idempotently identifiable by source SHA-256. Cross-file queue persistence, retry scheduling and process-death recovery still need the APK UI/worker integration.
- No semantic embeddings, model training, truth promotion or causal claims are performed.

## R3

F_ok: per-file processor/publisher/UI and synthetic tests are merged through PRs #575 and #576; workflow routing/topology/coherence stages observed successful.
F_gap: Android job still queued in run 36558610755; recursive SAF inventory, persistent resumable batch queue, provider readback, physical handset canary and measured corpus inventory remain unproven.
F_next: obtain terminal Android CI result, then implement recursive inventory/queue and validate a small on-device canary before scaling.
