# RafGitTools Android NOVOexport Processor — Implementation V1

State: `PER_FILE_PROCESS_AND_PUBLICATION_UI_MERGED / ANDROID_CI_PASS / FINAL_RECEIPT_PASS / DEVICE_NOT_RUN / BATCH_INVENTORY_NOT_WIRED`
claim_allowed: false

## Correct execution model

The user's phone runs the RafGitTools APK. RafGitTools reads the existing Drive files named `conversation*.json` and `codex*.json`, derives private JSONL index/chunk artifacts, and publishes them to a user-selected Drive folder and the private `CONVERSATIONS_CHUNKS_PRIVATE` repository. The user-mediated Android SAF provider handles Drive account selection and folder permissions. This work does not process the corpus in ChatGPT, Termux, a server, or a model-training job.

## Implementation status at main

The per-file processor, publisher, Drive-tab action and tests were introduced by PR #575 and compile/publishing fixes by PR #576. Both PRs are merged. Exact-head pipeline run `36558610755`: Android/test/lint/devDebug APK job `109375861051` completed successfully, including unit tests, lint, devDebug assembly and APK hash verification; CodeQL Java/Kotlin and Python deterministic tests also completed successfully. Final receipt job `109382657128` completed successfully, closing the workflow run. The signed-release job was skipped. The built devDebug APK SHA-256 is `4120a8f91a1de47a28c3c8db27b3e1a8fbaa94146c1b044ee7f1ecc3575fac7d`, uploaded in workflow artifact `start-06-devDebug-36558610755` (artifact ID `11030485325`). The merge commit `695bed743c5b22427ebc7f6cf4b71169208001bc` is one commit ahead of tested head `07081ef774d773cf570fb20aaab1058301761155` with no changed files in the comparison. This binds source trees, but no APK installation or physical-device execution has been run.

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
- `app/src/main/kotlin/com/rafgittools/navigator/NovoexportSafInventory.kt`
  - recursively walks only metadata from a user-selected SAF tree using provider document IDs;
  - never opens source document bytes and never renames, moves or deletes a source;
  - filters the bounded source families `conversation*.json`, `conversations*.json` and `codex*.json`;
  - records candidate URIs, MIME types and provider-reported sizes, retaining unknown sizes as `TOKEN_VAZIO_SIZE`;
  - prevents directory cycles by document ID and fails closed above the configured 100,000-document bound.
- `app/src/test/kotlin/com/rafgittools/navigator/NovoexportSafInventoryTest.kt`
  - positive and negative source-family selection gates.

- `app/src/main/kotlin/com/rafgittools/navigator/ConversationManifoldPublication.kt`
  - creates a metadata-only plan with source/output hashes and bounded Git-sized parts;
  - requires the confirmation value to equal the exact plan SHA-256;
  - writes each derived part to a user-selected Drive SAF folder and a Git private-processing namespace through a caller-supplied writer;
  - re-reads every Drive artifact through `ContentResolver.openInputStream` and every private Git artifact through a live Contents API GET; byte count and SHA-256 must match the confirmed plan;
  - writes a V2 completion receipt with `PUBLISHED_READBACK_VERIFIED` only after all derived parts pass both readbacks, then re-reads the receipt itself on both providers;
  - any mismatch, permission loss, missing content or non-private Git target fails closed and cannot return a verified publication state.

The Drive tab in `app/src/main/kotlin/com/rafgittools/ui/screens/home/HomeScreen.kt` exposes a per-file Process action, a user-selected Drive destination, an exact plan-hash confirmation, and publication through `HomeViewModel.publishConversationManifold`. Git writes use `GithubDataRepository.createPrivateProcessingFile`, which checks live repository privacy and enforces the 512 KiB namespace boundary. Commit SHAs are retained. This establishes merged source presence, a successful CI build, and a single-file UI path; it does not establish a complete batch workflow or operation on the user's handset.

## Still required before the phone can run the full route

1. Obtain exact-head CI for the recursive SAF inventory successor; `IMPLEMENTED_UNTESTED != PASS`.
2. Persist the inventoried candidate URI + metadata set as a resumable queue with per-file states, retry/error counters and source identity so app restart resumes safely.
3. Feed queued source URIs to `ConversationManifoldProcessor` on an IO/background executor and persist checkpoints, cancellation, permission loss and storage errors.
4. Extend the existing per-file preview/plan confirmation into a batch queue view with per-file status and resume controls; publish only after explicit confirmation through the live-private GitHub writer.
5. Implement provider readback for each Drive/Git output, compare hashes, then write a final cross-destination receipt. Current publication markers deliberately do not claim this.
6. Install the CI-built APK from workflow run `36558610755` on the phone and pass a small Drive canary before enabling multi-gigabyte batches.

## Limits and privacy

- The 25 GB value is the user's reported scope. No exhaustive recursive Drive enumeration or measurement exists in this evidence set.
- No source file is changed or deleted.
- Corpus-derived text in chunks is private content: publish only to the selected Drive destination and a live-verified private GitHub repository. RafGitTools public source receives code/tests only.
- The processor currently requires a top-level JSON array and blocks records over its configured limit. Those boundaries must be surfaced in the app and recorded as gaps, never silently skipped.
- Per-file checkpoints make a completed file idempotently identifiable by source SHA-256. Cross-file queue persistence, retry scheduling and process-death recovery still need the APK UI/worker integration.
- No semantic embeddings, model training, truth promotion or causal claims are performed.

## R3

F_ok: per-file processor/publisher/UI and synthetic tests are merged through PRs #575 and #576; workflow routing/topology/coherence stages observed successful.
F_gap: recursive SAF inventory, persistent resumable batch queue, provider readback, physical handset canary and measured corpus inventory remain unproven.
F_next: implement recursive inventory/queue and validate a small on-device canary before scaling.
