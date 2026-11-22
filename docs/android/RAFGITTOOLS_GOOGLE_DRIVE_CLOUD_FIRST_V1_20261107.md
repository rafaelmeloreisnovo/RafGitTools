# RafGitTools · Google Drive cloud-first · Android candidate

## User intent and regression
The previous Home → Drive and Setup Wizard used Android OpenDocumentTree. It presented local storage on the Motorola instead of the owner's Google Drive account. The added "Ferramentas locais" hide/show toggle was not requested.

## Native cloud route implemented
- Home Drive and Setup Wizard now mount GoogleDriveCloudCard; no SAF picker or local-storage toggle in active primary routes. The Home Local tab has been removed from the visible navigation. GitHub PAT remains untouched for GitHub.
- Google account authorization via Google Play Services AuthorizationClient. There is no Google Pay, no SAF and no reuse of the GitHub PAT or ChatGPT Drive connector.
- Real Google Drive v3 REST files.list folder navigation with pageToken, nested folder selection, and explicit independent SOURCE / DESTINATION folder choice.
- Recursively inventory metadata under the selected source Drive folder. Fail closed at 5000 documents or more, repeated page tokens, duplicate or unsafe paths and incomplete Drive search results.
- Under the selected Drive destination, create a new HYPERATLAS_CLOUD_V1_UUID folder. Upload 256KiB JSONL shards, verify each by Drive API GET SHA-256, and upload MANIFEST.json last. Never download original ZIPs or write to phone-local file paths.
- Schema rafgittools.hyperatlas.drive-api/v1 is distinct from legacy SAF v3. Legacy implementation stays in source but is not reachable from the main Drive UI.

## Required external OAuth configuration
1. Enable Google Drive API in the application owner's Google Cloud project.
2. Configure OAuth consent for restricted scope https://www.googleapis.com/auth/drive.
3. Configure Android OAuth client for actual application ID com.rafgittools.dev.debug (devDebug) and production application ID com.rafgittools (production), each with matching SHA-1 signing certificate.
4. Add intended test account(s) in Google Cloud OAuth testing audience until verification is complete.
5. Google Play Services must be available on the Moto E7 Power.
6. Restricted-scope verification is required for public production use. Do not claim user-ready access without provider confirmation. No embedded client secret.
7. Broad Drive OAuth scope is requested because the user demands browsing/writing within arbitrary existing folders. The runtime is limited to read/list/create/readback; no update, delete or move operation exists in the new module.
8. Token is volatile in memory, not persisted, logged or included in any receipt. UI reconstruction requires Google reauthorization.

## Tests and evidence
- Source and branch: GitHub commit readback after PR.
- Gradle assembleDevDebug, testDevDebugUnitTest, lintDevDebug: NOT_RUN until exact-head CI.
- Moto physical Drive account + folder navigation + cloud upload/hash readback: NOT_RUN.
- Google OAuth Cloud test audience and granted scope: TOKEN_VAZIO (external cloud configuration).
- 401 token expiry: reconnect. 403 permission gate: BLOCKED. API incomplete/repeated token/over 5000: BLOCKED. No silent local fallback.
- Provider may produce an orphan run folder if upload fails; no MANIFEST.json means incomplete, not PASS. Never delete original Drive files.
- Large archive interior ZIP64 HTTP Range: separate missing feature, NOT_IMPLEMENTED.

SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM
claim_allowed=false | release_allowed=false | device_e2e=NOT_RUN
