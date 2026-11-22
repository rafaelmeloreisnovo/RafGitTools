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

## P0 SAF provider and metadata cursor hotfix (2026-11-07)

Root issue bounded to existing SAF implementation:
- `GoogleDriveSafGate` previously accepted only `PackageManager.resolveContentProvider(authority, 0)` yielding exactly `com.google.android.apps.docs`; if Android obscured resolution, even a real Google Drive URI was rejected without meaningful diagnosis. New fallback accepts ONLY an authority listed in the **installed Google Drive package's declared providers**, never an arbitrary `google` substring. Other providers still blocked.
- `NovoexportSafInventory` previously crashed if cloud provider omitted the optional `COLUMN_SIZE`; it now records unknown size as `null/TOKEN_VAZIO` rather than zero or a failed scan.
- UI keeps one `ACTION_OPEN_DOCUMENT_TREE` picker and same-tree scan→publish. Grant persistence separately retries read-only when combined read/write persistence is unavailable. Scan and publishing errors are now disjoint statuses: `BLOCKED_DRIVE_SAF_READ_*` vs `DRIVE_SCAN_PASS_INDEX_WRITE_BLOCKED`. The latter proves scan returned a snapshot but does NOT claim a published index.
- Changes are source/CI/test only until exact-head checks complete; neither Android device selection nor actual Google Drive cloud readback have been proven.

**Provider limitation:** Google Drive may be available for `ACTION_OPEN_DOCUMENT` (single files) but absent for `ACTION_OPEN_DOCUMENT_TREE` (directories) on a particular Android/Google Drive version. The app cannot force a provider to implement tree selection. A single-file grant cannot ethically/safely be promoted to whole-folder access. Such a case is `BLOCKED_PROVIDER_TREE_NOT_EXPOSED`, not an APK success or an authorization bypass. If a previously granted tree URI remains present, it can be offered as a picker initial location; current UI still requires explicit selection.

No changes to source Drive files, license, GitHub PAT or signed release. Rollback: revert hotfix PR and preserve all receipts. `claim_allowed=false` until a physical E2E on the Moto and index readback.
