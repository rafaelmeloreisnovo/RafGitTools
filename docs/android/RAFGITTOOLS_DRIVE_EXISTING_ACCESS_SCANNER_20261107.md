# RafGitTools · One picker → select Google Drive folder → scan + index automatically

## Exact approved user flow
Home → Drive → 'Selecionar pasta no Google Drive' opens **one** Android ACTION_OPEN_DOCUMENT_TREE picker. User selects a folder from the Google Drive DocumentsProvider. On result, the APK:
1. Fails closed for null/cancel, local storage or non-Google provider.
2. Requests persistence of the one returned tree URI's read/write capability, if available.
3. Recursively enumerates **all child files and directories** of that selected Drive tree via existing `NovoexportSafInventory.scan`, metadata only; no content ZIP/JSON copied to phone.
4. Immediately (no second button, checkbox, login screen or destination picker) uses the **same selected URI** with existing `DriveAtlasSafPublisherV3.publish`.
5. Creates a new `HYPERATLAS_V3_<uuid>` folder in that same Google Drive directory. Writes bounded JSONL shards and a final MANIFEST.json; checks each SHA-256 from provider readback.
6. Displays verified counts and manifest digest, or explicit BLOCKED status.

Old Android Drive persisted URI is a **picker starting-location hint only**, not an alternate automatic choice. User always selects the folder.

## Important limits and truth
- Android Google Drive DocumentsProvider must expose folder-tree selection in its system picker. If Drive does not appear or forbids selecting a Drive folder, an app cannot force that provider to implement ACTION_OPEN_DOCUMENT_TREE. Fail closed; do not silently select local, claim it worked, or add another OAuth flow.
- No Google OAuth, Gmail, Google Pay, GitHub PAT or ChatGPT Drive credential required by this selected-folder SAF flow.
- Scope = the one folder explicitly selected, including subfolders, **not entire Google account** unless the provider explicitly permits choosing its root.
- Metadata-only index, no ZIP member contents or semantic full-body analysis. The existing gate limits inventory to **5000 documents**, 32 MiB index data, 256 shards; above bounds is BLOCKED, not an incomplete PASS.
- Existing GoogleDriveSafGate uses the Google Drive Android application package to distinguish the provider from on-device providers. Provider visibility may differ by Android version.
- No deletion, relocation or mutation of source documents. On interruption, partial new index folder has no committed manifest. No phone storage selected.
- Kotlin code implementation/PR is NOT physical-device proof. GitHub CI exact-head, Android folder picker availability and provider readback require separate gates.
- claim_allowed=false, release_allowed=false until tested.

## Rollback
Revert scoped GitHub PR, preserve prior implementation sources, no source Drive files changed by this engineering step.
