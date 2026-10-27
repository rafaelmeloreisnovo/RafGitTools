# RafGitTools — GitHub private tree → Drive (SAF) → GPT private lookup V1

Date: 2026-10-27 | state: CANDIDATE_BRANCH / claim_allowed=false.

## Scope and authority

A user-selected private GitHub repository, read over the existing authenticated
Retrofit API, is an index **source**, not a public CI payload. This module
fetches branch commit SHA, commit's Git tree SHA and recursive tree metadata.
It rejects non-private targets, incomplete/truncated trees, bad/duplicate paths
and object-identity mismatch. Raw blobs, chat messages and credentials are
never fetched by this operation.

The Android Home → **Drive** tab includes a new card:
1. Authenticate GitHub and select a private repository in the existing picker.
2. Tap **Indexar GitHub privado**; write a metadata-only JSON snapshot to
   Android app-private storage (not external public cache).
3. Tap **Exportar índice para pasta privada do Drive** and pick a Drive folder
   through Android SAF. Existing `CorpusCatalogTreeGate` writes the index and
   verifies destination SHA-256 by readback.
4. GPT, via an authorized Google Drive connector, reads that private index
   to resolve exact SHA and path; then it may request the minimum private file
   content using an authenticated GitHub connector. Private file contents must
   never be posted to public GitHub workflow logs, repo artifacts or summaries.

## Contract

Schema `rafgittools.github_private_tree_index.v1`.
Snapshot: repository identifier, exact branch commit, exact Git tree hash,
file-count, summed reported bytes, number of blobs exceeding 2 MiB,
classification `C3_RESTRICTED_METADATA`, `raw_conversations_copied=false`,
`content_sha256_verified=false`, `claim_allowed=false`, ordered file entries
(path, byte count, Git blob SHA-1 object ID). Index metadata may itself be
correlatable personal data; only store it in a folder whose permissions are
verified. This index does not claim cryptographic validation of raw file bytes.

## Boundaries and tests

No external dependency beyond existing Android/Gson/Retrofit/Kotlin/SAF code.
No PAT value is written to files. GitHub session remains in existing
`AuthRepository`, not in the snapshot. Fail-closed local unit tests cover
repo identifier, truncated trees, object mismatch, path traversal, duplicates,
oversize, missing SHA and privacy flags.

The project operates independently of public RafGitTools Actions scheduling.
Never run private source indexing on a public hosted runner.

Verification status: SOURCE_IMPLEMENTED_BRANCH; exact commit readback required,
Gradle test compilation, APK, physical Android, authenticated end-to-end GitHub
and Drive export = NOT_RUN unless separate receipts prove otherwise.
No auto-merge or promotion. Rollback: close the unmerged draft PR; no source
changes in the private producer repository or data files.
