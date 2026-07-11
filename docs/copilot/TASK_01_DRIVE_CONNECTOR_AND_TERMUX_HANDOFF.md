# TASK 01 — GOOGLE DRIVE CONNECTOR + TERMUX HANDOFF

## Copilot execution objective

Implement a production-grade first vertical in RafGitTools that lets the authenticated user:

1. connect a Google Drive account without entering a Google password in the app;
2. browse and select a real `conversations.json` file;
3. inspect bounded metadata before any transfer;
4. download the file resumably to a private staging area;
5. create a schema-valid `index_conversations` runtime job;
6. pass a read-only file descriptor or content URI to the RAFCODEΦ Termux service;
7. receive progress, completion, failure and cancellation states;
8. display the generated manifest/audit/checkpoint artifacts.

This task must produce working code and tests. Do not stop at interfaces, diagrams, TODOs or fake handlers.

---

## 1. Mandatory reconnaissance

Before editing, produce a short inventory in the PR description with the exact current paths and signatures of:

- authentication repository and session models;
- secure storage implementation;
- networking provider;
- offline queue/background sync;
- dependency injection module;
- navigation graph and screen conventions;
- `GovernanceGate` and `ToolRouter`;
- Android package/application ID;
- manifest permissions/services/providers;
- current test stack;
- Gradle flavors/build types.

Do not create a duplicate security, networking or queue subsystem when one already exists.

---

## 2. Proposed file map

Use the repository's actual package conventions. If the following packages do not exist, create them exactly unless a clearly equivalent existing package is found.

```text
app/src/main/kotlin/com/rafgittools/data/drive/
  DriveApi.kt
  DriveAuthRepository.kt
  DriveFileRepository.kt
  DriveRangeDownloader.kt
  DriveDtos.kt
  DriveMappers.kt

app/src/main/kotlin/com/rafgittools/domain/model/drive/
  DriveAccount.kt
  DriveFileRef.kt
  DriveTransfer.kt

app/src/main/kotlin/com/rafgittools/domain/model/runtime/
  RuntimeJob.kt
  RuntimeJobState.kt
  RuntimeArtifact.kt

app/src/main/kotlin/com/rafgittools/domain/usecase/drive/
  ConnectDriveAccountUseCase.kt
  ListDriveFolderUseCase.kt
  PrepareDriveSourceUseCase.kt
  ResumeDriveDownloadUseCase.kt
  DispatchConversationIndexJobUseCase.kt

app/src/main/kotlin/com/rafgittools/runtime/
  RafRuntimeClient.kt
  RafRuntimeServiceConnection.kt
  RafRuntimeProtocol.kt

app/src/main/aidl/com/rafgittools/runtime/
  IRafRuntimeService.aidl
  IRafRuntimeCallback.aidl
  RafRuntimeRequest.aidl
  RafRuntimeEvent.aidl

app/src/main/kotlin/com/rafgittools/ui/screens/datanavigator/
  DataNavigatorScreen.kt
  DataNavigatorViewModel.kt
  DataNavigatorState.kt
  DataNavigatorActions.kt

app/src/test/kotlin/com/rafgittools/data/drive/
  DriveAuthRepositoryTest.kt
  DriveRangeDownloaderTest.kt
  DriveMappersTest.kt

app/src/test/kotlin/com/rafgittools/runtime/
  RafRuntimeProtocolTest.kt
  RafRuntimeServiceConnectionTest.kt

app/src/test/kotlin/com/rafgittools/ui/screens/datanavigator/
  DataNavigatorViewModelTest.kt
```

If AIDL parcelables are more stable as Java/Kotlin Parcelable classes rather than `.aidl` parcelable definitions, document the choice and keep the wire contract versioned.

---

## 3. OAuth design

### 3.1 Requirements

- Authorization Code with PKCE.
- Browser/custom-tab based.
- No WebView password capture.
- No client secret embedded in APK.
- State and nonce generated with a secure random source.
- Redirect URI handled by an explicit activity intent filter.
- Exchange response validated before persistence.
- Account identity fetched and stored separately from tokens.
- Logout clears local tokens and optionally revokes server-side access when supported.

### 3.2 Token storage

Reuse `SecureStorage` / `EncryptionManager`.

Persist:

```text
provider = google_drive
account_subject
account_email/display label if returned
access_token
access_token_expiry_epoch_seconds
refresh_token when returned
granted_scopes
token_type
authenticated_at
```

Never expose token values through `toString`, logs, crash reports, Compose state snapshots or Room debug dumps.

### 3.3 Scope policy

Request the smallest scope that supports user-selected file discovery and read-only download. The exact scope must be documented in code and tests. Do not request write access in this vertical.

---

## 4. Drive API behavior

### 4.1 File listing

Support:

- root and folder listing;
- direct lookup by file ID;
- exact filename search for `conversations.json`;
- metadata fields needed for provenance;
- pagination;
- cancellation;
- 401 refresh/retry once;
- 403/429 bounded backoff;
- no infinite retry.

Map network DTOs to immutable domain types.

### 4.2 Domain model

Use explicit types similar to:

```kotlin
data class DriveFileRef(
    val fileId: String,
    val name: String,
    val mimeType: String,
    val sizeBytes: Long?,
    val createdTime: Instant?,
    val modifiedTime: Instant?,
    val md5Checksum: String?,
    val revisionId: String?,
    val etag: String?,
    val parentIds: List<String>
)
```

Do not convert byte sizes to `Int`.

### 4.3 Download journal

Each transfer gets a private journal file or Room row containing:

```text
transfer_id
file_id
revision/etag
expected_size
bytes_committed
part_path
created_at
updated_at
state
last_http_status
retry_count
```

The `.part` file is append-only until completion. On resume:

1. verify journal;
2. verify local partial size equals `bytes_committed`;
3. re-fetch metadata;
4. reject changed revision/etag;
5. request `Range: bytes=<offset>-`;
6. require compatible server response;
7. stream to disk;
8. fsync at checkpoint policy boundaries;
9. update journal atomically;
10. verify final size;
11. promote `.part` to final staging name atomically.

Never append a full `200 OK` response to an existing partial file. If the server ignores Range, restart safely after policy confirmation.

### 4.4 Memory bound

- fixed transport buffer, default 256 KiB or smaller;
- never `body.bytes()` or `body.string()` for corpus content;
- metadata JSON may use normal DTO parsing because it is bounded;
- tests must assert streaming behavior indirectly through large fixture and bounded buffer configuration.

---

## 5. Runtime job construction

Build the job from `schemas/rafaelia_runtime_job.schema.json`.

Example semantics:

```json
{
  "schema_version": "1.0.0",
  "job_id": "<stable-id>",
  "operation": "index_conversations",
  "source": {
    "provider": "google_drive",
    "locator": "<drive-file-id>",
    "name": "conversations.json",
    "size_bytes": 499885038,
    "mime_type": "application/json",
    "etag": "<etag>",
    "revision": "<revision>",
    "content_hash": "<BLAKE3 after local verification>"
  },
  "policy": {
    "read_only": true,
    "exclude_private_media": true,
    "max_memory_bytes": 268435456,
    "max_expanded_bytes": 499885038,
    "allow_network": false,
    "allow_model_inference": false
  },
  "requested_outputs": [
    "source_manifest",
    "conversations_segment",
    "messages_segment",
    "timeline_segment",
    "audit_jsonl",
    "checkpoint_state"
  ]
}
```

Rules:

- run governance before dispatch;
- persist exact serialized bytes and their hash;
- do not let UI mutate a dispatched job;
- correlate every event with job ID and runtime request ID;
- reject incompatible runtime protocol versions.

---

## 6. Binder/AIDL client

Implement client-side only in this repository. The service implementation belongs to `termux-app-rafacodephi`.

Required API semantics:

```text
getProtocolVersion()
submitJob(request, readOnlySourceFd, callback) -> requestId
cancelJob(requestId)
getJobState(requestId)
listArtifacts(requestId)
openArtifact(requestId, artifactName) -> readOnly ParcelFileDescriptor
```

Do not expose:

```text
exec(command)
runShell(text)
openAnyPath(path)
```

Handle:

- service unavailable;
- signature/permission denial;
- binder death;
- protocol mismatch;
- duplicate submit;
- callback after ViewModel recreation;
- cancellation race;
- result for unknown request ID.

All service connection code must be testable behind an interface.

---

## 7. Governance integration

Add typed tools to the governance registry only when their handlers exist:

```text
drive.list_readonly
drive.download_readonly
runtime.index_conversations
runtime.cancel_job
runtime.read_artifact
```

Each tool definition must declare:

- user-authentication requirement;
- source read-only requirement;
- maximum payload;
- whether network is permitted;
- whether model inference is permitted;
- audit category.

A denied governance decision must stop before network, file or Binder side effects.

---

## 8. Data Navigator first screen

The first functional UI does not need every future feature. It must close the vertical.

Required states:

```text
Disconnected
Authenticating
Connected(account)
Listing(folder, page)
SourceSelected(file)
Downloading(progress)
ReadyToDispatch(stagedFile)
Dispatching
Running(bytes, records, checkpoint)
Completed(artifacts)
Failed(stage, code, recoverable)
Cancelled
```

Required user actions:

- connect/logout;
- open folder;
- search exact filename;
- select source;
- start/resume/cancel download;
- dispatch indexing;
- cancel indexing;
- open manifest/audit/checkpoint preview;
- retry only when safe.

UI previews must be bounded. Never render the entire manifest or audit if it is unexpectedly large; page or cap it.

---

## 9. Tests

### OAuth/auth tests

- state mismatch;
- PKCE verifier missing;
- token exchange failure;
- identity validation failure;
- secure storage failure;
- logout clears session;
- no token in logs/toString.

### Drive transport tests

- paginated listing;
- exact file lookup;
- 401 refresh then success;
- 429 bounded retry;
- valid range resume;
- ignored Range response;
- changed ETag;
- local part-size mismatch;
- cancellation;
- final-size mismatch;
- atomic promotion.

### Runtime tests

- schema-valid job;
- invalid operation;
- `read_only=false` rejected;
- service absent;
- protocol mismatch;
- Binder death and reconnect;
- callback correlation;
- cancellation race;
- governance denial before bind.

### ViewModel tests

- state transitions;
- process recreation from persisted transfer/job state;
- error stage surfaced;
- completed artifacts displayed;
- no duplicate dispatch.

---

## 10. CI requirements

Update or add a focused workflow that executes:

```text
unit tests
lint
assembleDevDebug
APK SHA-256
relevant schema validation
AIDL compilation
```

Do not weaken existing workflows. Do not claim a device-level proof without an emulator or physical-device run.

Artifacts:

- dev debug APK;
- test reports;
- lint report;
- generated protocol/schema compatibility report.

---

## 11. Acceptance criteria

The PR is acceptable only when all are true:

- no placeholder handlers in the implemented vertical;
- no credentials committed;
- no corpus loaded fully into memory;
- download resumes safely;
- revision conflict is detected;
- job schema is validated;
- governance runs before side effects;
- Binder API is typed and contains no arbitrary command execution;
- source is passed read-only;
- job status survives ViewModel recreation;
- tests pass;
- APK builds;
- PR documents actual executed commands and remaining `TOKEN_VAZIO` items.

---

## 12. Required PR report template

```markdown
## Objective

## Existing files inspected

## Architecture preserved

## Files changed

## Protocol/API changes

## Security analysis

## Memory and large-file behavior

## Tests executed

## CI result

## Generated artifacts and hashes

## TOKEN_VAZIO / remaining work

## Manual verification steps
```
