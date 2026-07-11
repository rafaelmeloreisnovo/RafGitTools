# GitHub Copilot Instructions — RafGitTools

## 1. Repository role

RafGitTools is the Android control plane of the RAFAELIA ecosystem.

It is responsible for:

- user authentication and secure credential storage;
- Google Drive and GitHub source selection;
- governance of every external action;
- explicit cross-app communication with `termux-app-rafacodephi`;
- job creation using `schemas/rafaelia_runtime_job.schema.json`;
- status, audit and artifact presentation;
- the tree/table/inspector data navigator;
- the narrow JNI boundary to LlamaRafaelia.

It is not responsible for parsing multi-hundred-megabyte corpus files in Kotlin, running arbitrary shell commands, or letting the model execute tools directly.

Read `docs/copilot/COPILOT_CROSS_REPO_EXECUTION_MASTER.md` before implementing RAFAELIA data-navigation work.

---

## 2. Inspect before editing

At the beginning of every task, inspect the current default branch and list the real files that already implement the relevant boundary. Never assume a path from an issue is still correct.

For Drive/runtime work, inspect at least:

- `app/build.gradle`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/kotlin/com/rafgittools/core/security/SecureStorage.kt`
- `app/src/main/kotlin/com/rafgittools/core/security/EncryptionManager.kt`
- `app/src/main/kotlin/com/rafgittools/offline/OfflineQueue.kt`
- `app/src/main/kotlin/com/rafgittools/offline/BackgroundSyncManager.kt`
- `app/src/main/kotlin/com/rafgittools/kernel/RafKernelBridge.kt`
- `app/src/main/kotlin/com/rafgittools/kernel/GovernanceGate.kt`
- `app/src/main/kotlin/com/rafgittools/kernel/ToolRouter.kt`
- `app/src/main/kotlin/com/rafgittools/di/AppModule.kt`
- `runtime-lock.json`
- `schemas/rafaelia_runtime_job.schema.json`

If a proposed path does not exist, first identify the repository's real package convention and then create the minimal new package.

---

## 3. Non-negotiable boundaries

### Kotlin/Java boundary

Kotlin owns:

- Android lifecycle;
- UI state;
- OAuth/browser integration;
- Binder/AIDL calls;
- Room metadata and job status;
- bounded page/preview rendering.

Kotlin must not own:

- full `conversations.json` parsing;
- full ZIP/RAW/media parsing;
- content hashing of huge sources if a native streaming implementation exists;
- deterministic segment generation;
- low-level temporal ledger construction;
- model inference policy.

### Native/model boundary

- JNI stays narrow.
- Do not add a general `execute(command: String)` JNI or Binder API.
- LlamaRafaelia returns structured candidates only.
- `GovernanceGate` decides whether a tool operation may proceed.
- `ToolRouter` dispatches only typed, allowlisted operations.

### Source boundary

- Google Drive originals are read-only.
- Do not rename, move or edit source corpus files during indexing.
- Staging files and derived artifacts must have separate directories.
- Credentials never enter Drive, logs, manifests or model prompts.

---

## 4. Google Drive connector rules

Implement Drive access as three explicit layers.

### Authentication

- browser-based OAuth authorization code flow with PKCE;
- no user password collection;
- no cookie scraping;
- no embedded client secret;
- minimum required scopes;
- refresh/access tokens stored through the existing secure storage backed by Android Keystore;
- explicit logout and token revocation/clear path;
- account identity validated before marking the session authenticated.

### Transport

- use the repository's existing HTTP stack when feasible;
- bounded streaming response bodies;
- HTTP range/resume support for large downloads;
- ETag/revision validation;
- exponential backoff with a hard retry limit;
- cancellation support;
- partial-file journal;
- atomic promotion from `.part` to completed staging file;
- never read the whole response into a Kotlin `ByteArray` or `String`.

### Drive model

Persist only bounded metadata required for navigation and resumability:

```text
fileId
name
mimeType
sizeBytes: Long
createdTime
modifiedTime
md5Checksum when supplied by Drive
etag/revision
parentIds
localStagingPath
bytesDownloaded: Long
state
```

Do not use a filename as identity. Drive `fileId` + revision identifies a source appearance; BLAKE3 identifies content after local verification.

---

## 5. Cross-app Termux transport

RafGitTools and Termux run in different Android sandboxes. Do not assume direct access to Termux private filesystem paths.

Implement an explicit Binder/AIDL service contract:

- explicit `ComponentName`;
- signature-level permission;
- caller/signature validation on the Termux side;
- bounded request payload;
- no arbitrary command string;
- typed operation enum matching the runtime job schema;
- cancellation and status callbacks;
- reconnection after process death;
- service-not-installed and service-version-mismatch states;
- audit entry for accepted/rejected calls.

The app may stage a downloaded file in a location made available through a read-only `ParcelFileDescriptor` or a deliberately shared content URI. Do not pass broad filesystem permissions.

---

## 6. Runtime job representation

Use `schemas/rafaelia_runtime_job.schema.json` as the source of truth.

Create typed Kotlin models that preserve exact integer semantics:

- `size_bytes`, memory limits and offsets: `Long`;
- enums encoded by stable string names in JSON;
- no floating-point byte counts;
- `job_id` content-addressed or cryptographically random, never a timestamp alone;
- `read_only` must be `true` for corpus indexing;
- requested outputs must be explicit.

Before dispatch:

1. validate source metadata;
2. validate policy;
3. run `GovernanceGate`;
4. serialize canonical job JSON;
5. hash the serialized job;
6. persist queued state;
7. send through Binder;
8. record acknowledgment and runtime request ID.

---

## 7. UI requirements

The data navigator must be index-driven, not file-size-driven.

Required desktop-like mobile layout behavior:

- left panel: source/collection tree;
- middle panel: paged records;
- right panel: inspector/metadata/temporal evidence;
- keyboard shortcuts where Android hardware keyboard exists;
- search and filters do not load the full corpus;
- previews have a strict byte/character cap;
- long operations show bytes, records, checkpoint and state;
- `TOKEN_VAZIO` is visible instead of fabricated values;
- temporal conflicts show each source clock separately.

Accessibility:

- meaningful content descriptions;
- scalable text;
- focus order;
- no color-only state encoding;
- large touch targets;
- error text that names the failed stage and recovery action.

---

## 8. Security rules

Reject any change that:

- stores OAuth tokens in plain SharedPreferences;
- logs authorization headers, refresh tokens or cookies;
- accepts every TLS certificate;
- disables hostname verification;
- exports an unrestricted service;
- exposes arbitrary shell execution;
- trusts model output as authorization;
- downloads to an unbounded buffer;
- accepts a runtime result without matching job/request identity;
- silently follows a changed Drive revision during resume.

Required tests include:

- invalid/expired token;
- account mismatch;
- interrupted range download;
- changed ETag/revision;
- partial-file recovery;
- service absent;
- caller permission denied;
- payload over limit;
- job schema invalid;
- governance denial;
- cancellation;
- process recreation.

---

## 9. Dependency policy

- Prefer existing Retrofit/OkHttp/Hilt/Room/coroutines infrastructure.
- Do not add a large Google SDK only to list/download files if the existing HTTP stack can implement the required REST calls safely.
- A new dependency requires: reason, exact version, license, size impact, transitive dependency audit and proof that existing code cannot satisfy the requirement.
- Never add a cryptographic implementation from generated code.

---

## 10. Testing and CI

For every functional change:

- add unit tests for pure mapping/validation logic;
- add MockWebServer tests for HTTP behavior;
- add Binder/service contract tests where feasible;
- run repository canonical Gradle wrapper through the Java 17 helper when required;
- run lint and the affected build variant;
- preserve existing kernel/native workflows;
- upload APK and relevant manifests only through CI artifacts, not commits.

PR evidence must list commands actually executed and distinguish local execution from CI.

---

## 11. Definition of done

A Drive/runtime feature is not done because classes compile. It is done when:

- a real or deterministic fixture file can be selected;
- transfer resumes after interruption;
- revision conflict is detected;
- a schema-valid read-only job reaches the Termux service;
- the service returns correlated status;
- source files remain unchanged;
- secrets are absent from logs;
- tests and Android build pass;
- remaining unsupported behavior is marked `TOKEN_VAZIO`.
