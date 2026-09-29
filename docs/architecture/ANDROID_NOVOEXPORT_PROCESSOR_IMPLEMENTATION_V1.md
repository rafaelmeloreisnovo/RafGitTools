# RafGitTools Android NOVOexport Processor — Implementation V1

State: `ANDROID_PROCESSOR_AND_SINGLE_FILE_FLOW_SOURCE_ADDED / BATCH_INVENTORY_NOT_WIRED / CI_PENDING / DEVICE_NOT_RUN`
claim_allowed: false

## Correct execution model

The user's phone runs the RafGitTools APK. RafGitTools reads the existing Drive files named `conversation*.json` and `codex*.json`, derives private JSONL index/chunk artifacts, and publishes them to a user-selected Drive folder and the private `CONVERSATIONS_CHUNKS_PRIVATE` repository. The user-mediated Android SAF provider handles Drive account selection and folder permissions. This work does not process the corpus in ChatGPT, Termux, a server, or a model-training job.

## Code added in this branch

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
  - writes completion markers with `PUBLISHED_UNVERIFIED_READBACK_PENDING`; they are not evidence of provider readback or a completed corpus.

The Drive tab in `app/src/main/kotlin/com/rafgittools/ui/screens/home/HomeScreen.kt` now exposes a per-file Process action, a user-selected Drive destination, an exact plan-hash confirmation, and publication through `HomeViewModel.publishConversationManifold`. Git writes use `GithubDataRepository.createPrivateProcessingFile`, which checks live repository privacy and enforces the 512 KiB namespace boundary. Commit SHAs are retained. These are source changes; CI and on-device execution have not yet established whether the code compiles or runs on the user's handset.

## Still required before the phone can run the full route

1. Add the in-app NOVOexport screen and bind `ACTION_OPEN_DOCUMENT_TREE` for the source and destination with persisted SAF grants.
2. Enumerate descendants of the selected source tree and filter `conversation*.json` and `codex*.json`; persist the queue and per-file status so app restart resumes safely.
3. Feed each source URI to `ConversationManifoldProcessor` on an IO/background executor and persist checkpoints, cancellation, permission loss and storage errors.
4. Show generated artifacts and the exact publication plan hash in the app; on explicit confirmation call `ConversationManifoldPublication` with the live-private GitHub writer already gated by repository privacy.
5. Implement provider readback for each Drive/Git output, compare hashes, then write a final cross-destination receipt. Current publication markers deliberately do not claim this.
6. Run unit tests and exact-head Android CI; install that exact APK on the phone and pass a small Drive canary before enabling multi-gigabyte batches.

## Limits and privacy

- The 25 GB value is the user's reported scope. This branch has not enumerated or measured the complete Drive source.
- No source file is changed or deleted.
- Corpus-derived text in chunks is private content: publish only to the selected Drive destination and a live-verified private GitHub repository. RafGitTools public source receives code/tests only.
- The processor currently requires a top-level JSON array and blocks records over its configured limit. Those boundaries must be surfaced in the app and recorded as gaps, never silently skipped.
- Per-file checkpoints make a completed file idempotently identifiable by source SHA-256. Cross-file queue persistence, retry scheduling and process-death recovery still need the APK UI/worker integration.
- No semantic embeddings, model training, truth promotion or causal claims are performed.

## R3

F_ok: Kotlin processor, bounded derived-artifact publisher and synthetic tests are added on a feature branch.
F_gap: app screen/SAF enumeration/queue wiring, provider readback, CI, handset run and exhaustive inventory.
F_next: validate this branch in CI; then wire the screen/Drive tree queue and run a small handset canary before scaling.
