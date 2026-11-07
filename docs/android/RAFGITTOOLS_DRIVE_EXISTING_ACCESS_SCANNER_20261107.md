# RafGitTools · Restore existing Google Drive folder grant → scan → index

## Observed defect
The Google Drive app was already available to the user. The new Home/Setup cloud-only route bypassed the older Android persisted Drive document-tree grant, requiring additional Google authorization even if the app still held a valid grant. This caused unnecessary UI friction. Do not interpret ChatGPT's connected Google Drive as an Android app token; tokens are isolated across apps.

## Restored first route
1. Read THIS app's previously persisted Android DocumentTree permissions, not any arbitrary local provider.
2. Reject unknown/local providers using GoogleDriveSafGate exact Android package and DocumentsContract.isTreeUri; ensure previously authorized read permission.
3. Home → Drive and Setup Wizard use GoogleDriveScannerEntry. If previously authorized Drive tree exists, show the one-click 'Escanear esta pasta do Google Drive' button, without displaying a new OAuth prompt or selecting local storage.
4. Execute existing NovoexportSafInventory.scan on metadata only, bounded to MAX_DOCUMENTS.
5. When the existing permission allows writing, use existing DriveAtlasSafPublisherV3.publish to create a new index run folder in the same Drive tree, with SHA-256 shard+manifest readback; append-only, originals untouched.
6. When previously authorized grant is read-only, still allow metadata scanning; clearly report index blocked by missing write permission. No silent fallback to the phone.
7. If no previously persisted app grant is available, fallback to GoogleDriveCloudCard: first try Google Identity authorization silently (previous Google grant) and show consent ONLY when the provider says it is necessary. No GitHub PAT reuse.

## Truth boundaries
- Reused app-specific tree URI only; never transfer ChatGPT plugin credentials into APK.
- Presence of Google Drive on the phone does not automatically grant app permission to every folder.
- Code integrated into feature branch with source readback; build/test gates pending.
- Physical Motorola E7 Power: NOT_RUN. OAuth config: TOKEN_VAZIO. No claim of account or folder access until tested.
- A user who previously granted ONLY an individual file URI, not a tree URI, may need to select a Drive folder via a supported provider flow. Do not pretend otherwise.
- Indexing only metadata, not archive members. Existing scanner has a 5000 document cap, plus size, semantic, readback bounds.
- Rollback: revert this narrowly scoped PR; no existing Google Drive files or GitHub PAT are modified.

SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM
claim_allowed=false | release_allowed=false
