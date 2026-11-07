# RafGitTools · Google Drive first (provider SAF guard) · 2026-11-07

## Cause
The previous Drive UI launched Android `OpenDocumentTree()`. The system picker may list only local providers. The picker grants a scoped URI, not Google account authentication. The old UI accepted any provider, including device-local storage, despite describing it as Drive.

## Scoped change
- Reject every source, ZIP and export destination URI unless Android resolves its DocumentsProvider to the Google Drive application package. No source scan or index write starts on a local URI.
- Show the selected provider limitation explicitly, with BLOCKED state when Drive is unavailable.
- Retain the existing SAF HyperAtlas V3, SHA-256 readback and legacy exporters, but collapse local staging/processing under a deliberate **Ferramentas locais (último recurso)** toggle.
- Explicitly declare Drive app package visibility for Android 11+.

## Critical boundary
This prevents a wrong-location operation. **It does not make Google Drive appear** if the Android DocumentsProvider does not expose folder trees. No new Google OAuth token, Drive REST API client, all-account access, ZIP Range transport, or cloud executor is claimed. A true native Drive-first browser requires Google Identity authorization and an API-backed inventory/publishing vertical with explicit user consent, pagination, revision/ETag and provider readback. GitHub PAT grants no Google permissions.

## Validation gates
- JVM unit: positive exact Google package and negative local/unknown/spoofed package strings.
- Android build/test exact branch HEAD: NOT_RUN until CI evidence.
- Motorola device: verify source picker showing Google Drive, reject local URI, publish to Drive and hash readback, test cancel/permission absence.
- Fail closed on unavailable provider; no fallback to local.
- No secret and no source ZIP copied; SHA/readback only after explicit Drive selection.
- `SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM`; `claim_allowed=false`.
