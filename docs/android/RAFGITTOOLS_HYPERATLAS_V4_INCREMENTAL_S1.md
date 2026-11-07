# HyperAtlas V4 S1 — SAF incremental with private checkpoint

Producer: RafGitTools. State: SOURCE_IMPLEMENTED / HOST_TESTS_SCOPED / ANDROID_E2E_NOT_RUN / claim_allowed=false.
Baseline: main@5f45ef204532242bcb1a181f8eee80121262e7d1. V3 preserved, provider fix PR #801 remains separately DRAFT.

## Existing Android flow, opt-in
Home → Drive → HyperAtlas → V4 experimental · indexar Drive em lotes locais (retomável).
Calls one existing Android OpenDocumentTree picker then GoogleDriveSafGate. No second login or OAuth.
If Google Drive DocumentsProvider does not expose folder trees, remain BLOCKED (never substitute a local folder).

## Executable design
- HyperAtlasV4Incremental: bounded BFS with SHA-256 pseudonymous document keys, null-preserving file sizes, graph cycle guard, idempotent directory batches, replayable checkpoints.
- HyperAtlasV4SafLocal: DocumentsContract child-cursor adapter, explicit EXTRA_LOADING rejection, no source byte access.
- Private app filesDir/hyperatlas-v4/<sha256-of-selected-tree-URI>/CHECKPOINT.json (raw IDs private) and batch-<hash>.jsonl (paths/names are sensitive).
- An AtomicFile batch is read back before checkpoint advancement. Replay accepts identical batch and rejects divergent content. Corrupt checkpoint blocks, not resets.
- Default caps: 10,000 folder work/visited records; 2,048 children per directory; 250,000 file/directory observations. Bounds, not observed coverage.
- Completed checkpoint covers only selected SAF tree at scan time, not all Google Drive.
- V4 does not copy ZIPs, parse conversation bodies, download originals or publish Drive files.

## Fail-closed limitations
1. Pure Kotlin host core: compiled with kotlinc; six test cases passed with local JUnit-API stub. Not Android Gradle proof.
2. Exact-head Android CI, physical Motorola E7 Power Drive picker, provider readback and crash replay: NOT_RUN.
3. Directory > 2048 children or a provider still loading blocks without falsely marking completion. SAF paging remains unsupported.
4. Changes after a COMPLETE checkpoint require a separate refresh mechanism (not implemented). Cross-URI identity and change feed are not claimed.
5. Storage remains app-private LOCAL ONLY until a future separately authorized exporter is implemented. V3 keeps its explicit Drive output.
6. Permission persistence and Drive folder exposure depend on the installed provider; PR #801 is a separate source gate.
7. No claim of 51 NOVOexport-shard semantic coverage or billions of records.

## Rollback and privacy
Do not export the private CHECKPOINT.json; it contains provider document IDs. Do not publish local path index without privacy review.
Close this DRAFT PR or revert its additive Kotlin/tests/doc and small UI button. Never delete or overwrite Drive originals, V3 or earlier checkpoints.
Source ≠ Artifact ≠ Execution ≠ Evidence ≠ Claim. claim_allowed=false.