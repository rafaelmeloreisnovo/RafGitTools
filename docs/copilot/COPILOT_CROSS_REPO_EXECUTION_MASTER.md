# COPILOT CROSS-REPOSITORY EXECUTION MASTER

## 0. Mission

Implement the architect-defined RAFAELIA data-navigation pipeline as a closed, testable, local-first system across four repositories:

1. `rafaelmeloreisnovo/RafGitTools` — Android control plane, authentication, governance, navigation UI and cross-app transport.
2. `rafaelmeloreisnovo/RafPolimata` — bounded low-level parsing, normalization, deterministic segment generation and temporal evidence.
3. `rafaelmeloreisnovo/termux-app-rafacodephi` — local runtime, worker lifecycle, checkpoints, Binder boundary and internal native transport.
4. `rafaelmeloreisnovo/llamaRafaelia` — semantic anchoring over bounded provenance segments; never raw-file parsing.

The first end-to-end vertical is:

```text
Google Drive conversations.json
  -> authenticated selection in RafGitTools
  -> resumable local transfer
  -> explicit runtime job
  -> Termux worker
  -> RafPolimata streaming parser
  -> deterministic segments + timeline + audit
  -> LlamaRafaelia bounded semantic anchoring
  -> RafGitTools tree/table/inspector UI
```

Do not replace this architecture with a generic cloud service, a database-first rewrite, an LLM-first parser, or a large framework.

---

## 1. Current source truth

Before modifying code, read the current default branch of every repository and verify these already-integrated artifacts.

### RafGitTools

- `runtime-lock.json`
- `schemas/rafaelia_runtime_job.schema.json`
- `kernel/native/raf_kernel_api.h`
- `kernel/native/raf_kernel_jni.c`
- `app/src/main/kotlin/com/rafgittools/kernel/RafKernelBridge.kt`
- `app/src/main/kotlin/com/rafgittools/kernel/GovernanceGate.kt`
- `app/src/main/kotlin/com/rafgittools/kernel/ToolRouter.kt`
- `app/src/main/kotlin/com/rafgittools/core/security/SecureStorage.kt`
- `app/src/main/kotlin/com/rafgittools/offline/OfflineQueue.kt`
- `app/src/main/kotlin/com/rafgittools/offline/BackgroundSyncManager.kt`
- `docs/RAFAELIA_DATA_NAVIGATOR_ARCHITECTURE.md`

### RafPolimata

- `include/rafaelia_runtime_protocol.h`
- `runtime/conversation_indexer/raf_convscan.h`
- `runtime/conversation_indexer/raf_convscan.c`
- `runtime/conversation_indexer/convscan_cli.c`
- `runtime/conversation_indexer/test_convscan.c`
- `runtime/conversation_indexer/Makefile`
- `.github/workflows/conversation-indexer-ci.yml`
- `docs/RAFAELIA_DATA_INGEST_INDEX_PROTOCOL.md`

The scanner has already passed Clang, GCC, ASan, UBSan, ARM32, ARM64 and a realistic fixture. Extend it; do not replace it with a DOM parser.

### termux-app-rafacodephi

- `scripts/rafaelia/run_index_conversations.sh`
- `.github/workflows/rafaelia-runtime-runner-ci.yml`

The runner already writes manifest, audit and checkpoint artifacts and refuses concurrent execution.

### llamaRafaelia

- `docs/RUNTIME_SEGMENT_CONTRACT.md`
- `.github/copilot-instructions.md`
- existing `rmrCti` and CTI-memory integration
- existing CMake and test conventions

---

## 2. Non-negotiable architecture

### 2.1 Component boundary

```text
Drive supplies bytes and revision metadata.
RafGitTools authenticates, selects, governs and displays.
Termux owns local execution and worker lifecycle.
RafPolimata turns bytes into deterministic records and segments.
LlamaRafaelia reads only bounded segments and emits candidate relations.
```

### 2.2 No raw-file model ingestion

Forbidden:

```text
500 MB JSON -> prompt -> model assertion
```

Required:

```text
500 MB JSON -> bounded parser -> provenance records -> bounded retrieval -> model
```

### 2.3 No hidden mutation

- Original Drive files remain read-only.
- Conflicting timestamps are preserved as separate evidence.
- Similarity never merges identities automatically.
- Every generated artifact records source identity, byte range and parser version.
- Destructive actions require governance approval, dry-run and journal.

### 2.4 No fake completion

Use these states consistently:

- `VERIFIED`: executed and reproduced.
- `DECLARED_BY_AUTHOR`: specified but not independently executed.
- `TOKEN_VAZIO`: evidence or implementation is absent.
- `CONTRADICTION`: observed evidence conflicts with the claim or input.

Do not return success for stubs. Do not label format recognition as full parsing. Do not label semantic similarity as identity.

---

## 3. Low-level rules

### 3.1 Integer and string representation

- Use explicit-width integer types for persisted or cross-process structures.
- Persisted integers use a declared byte order; v1 uses little-endian.
- Never persist native pointers, `size_t`, Kotlin object hashes or compiler-dependent enums.
- All strings are byte spans: `offset + length`; never rely on NUL termination in segment files.
- UTF-8 validity must be tracked explicitly; invalid source bytes are preserved or quarantined, not silently rewritten.
- File offsets and counts are 64-bit.
- Individual string lengths are 32-bit only after checking `length <= UINT32_MAX`.
- Timestamp canonical representation is signed 64-bit epoch microseconds. Unknown is `INT64_MIN`; original textual/numeric representation remains in evidence when needed.

### 3.2 Memory

- RafPolimata core: no `malloc`, no hidden allocator, no unbounded recursion.
- Use caller-provided arenas, bounded buffers, explicit capacities and deterministic failure codes.
- Hosted CLIs may use OS file APIs only at the boundary.
- Large files are streamed; no full-file `String`, `ByteArray`, JSON tree or SQLite copy in memory.
- Android UI never receives the full corpus; it receives pages and previews.

### 3.3 Branches and optimization

- Parsers use explicit state machines and bounds checks.
- Numeric hot loops may be branch-minimized after correctness tests exist.
- ASM/SIMD is allowed only behind a portable reference implementation and golden-vector equivalence tests.
- Do not optimize before obtaining byte-for-byte deterministic reference output.

### 3.4 Cryptographic identity

- Content identity target: BLAKE3-256.
- Do not invent cryptographic code.
- First search for a repository-owned or pinned audited implementation.
- If vendoring is required, pin the exact upstream commit, preserve license, document source and add golden vectors.
- CRC32C remains an integrity/checkpoint signal, not cryptographic identity.

---

## 4. Cross-repository protocol

### 4.1 Runtime job

The canonical JSON schema is:

`RafGitTools/schemas/rafaelia_runtime_job.schema.json`

Required semantic fields:

```text
schema_version
job_id
operation
source.provider
source.locator
source.name
source.size_bytes
policy.read_only = true
policy.exclude_private_media
policy.max_memory_bytes
policy.max_expanded_bytes
requested_outputs[]
```

### 4.2 Cross-app transport

Android sandbox boundaries mean RafGitTools must not assume direct access to Termux private filesystem sockets.

Required topology:

```text
RafGitTools
  -> explicit Binder/AIDL call protected by signature permission
termux-app-rafacodephi service
  -> internal queue / Unix socket / native worker inside its own sandbox
RafPolimata executable or library
```

Security requirements:

- explicit component, never an implicit exported command service;
- signature-level permission;
- caller package/signature verification;
- bounded request size;
- no arbitrary shell command field;
- operation enum only;
- read-only policy enforced again in Termux;
- audit record for accepted and rejected calls.

### 4.3 Worker frame

For internal Termux worker communication, use a fixed 32-byte header:

```c
struct raf_rpc_header_v1 {
    uint8_t  magic[8];      /* "RAFRPC1\0" */
    uint16_t version;       /* 1 */
    uint16_t type;          /* request/event/result */
    uint32_t flags;
    uint64_t request_id;
    uint32_t payload_len;
    uint32_t crc32c;
};
```

- little-endian;
- maximum payload: 1 MiB for control messages;
- payload: UTF-8 JSON matching the canonical schema in v1;
- reject unknown versions, lengths or CRC before parsing payload;
- large data never travels in the control frame; pass a local file descriptor or validated local path owned by the Termux runtime.

---

## 5. End-to-end phases

### Phase A — RafPolimata deterministic segments

Deliver:

- conversation and message extraction from streaming JSON;
- exact source byte ranges;
- fixed binary segment v1;
- string pool;
- index table;
- temporal evidence records;
- checkpoint/resume at safe boundaries;
- deterministic output independent of input chunk size.

### Phase B — Termux runtime service

Deliver:

- signature-protected AIDL service;
- validated runtime job intake;
- worker queue with one active writer per output directory;
- foreground execution for long jobs;
- cancellation and resumable checkpoint;
- atomic result publication;
- audit log.

### Phase C — RafGitTools Drive connector and control UI

Deliver:

- OAuth browser flow with PKCE; no password and no cookie scraping;
- token storage through existing secure storage/Android Keystore;
- Drive file listing and metadata;
- resumable range download to app-controlled staging;
- explicit handoff to Termux service;
- job status and artifact browser;
- Clipper/DOS-shell-inspired three-panel navigator.

### Phase D — LlamaRafaelia segment reader

Deliver:

- read-only segment validation;
- bounded retrieval by IDs, time ranges and relation candidates;
- semantic anchoring output with evidence IDs and Q16 confidence;
- abstention when cross-source support is insufficient;
- no direct network/filesystem actions from model output.

### Phase E — Real corpus proof

Execute against a selected real `conversations.json` from Drive with no source mutation.

Required proof:

- source file ID, size, revision/etag and BLAKE3;
- peak memory;
- bytes processed;
- conversations and messages extracted;
- conflicting timestamps counted;
- deterministic segment hashes from at least two chunk sizes;
- interruption/resume demonstration;
- Android/Termux execution log;
- bounded Llama retrieval demonstration;
- final coverage report with `TOKEN_VAZIO` gaps.

---

## 6. PR discipline for Copilot

Every Copilot PR must include:

1. exact objective;
2. files inspected before editing;
3. files changed;
4. invariants preserved;
5. commands executed;
6. complete test output summary;
7. artifact hashes when binaries/segments are generated;
8. peak memory or bounded-memory explanation for large-data changes;
9. security impact;
10. remaining gaps marked `TOKEN_VAZIO`.

Reject PRs that:

- introduce placeholders or TODO-only handlers;
- claim unexecuted tests;
- add a heavy dependency without proving necessity;
- move critical parsing into Kotlin/Java;
- expose arbitrary shell execution;
- parse raw corpus with an LLM;
- silently normalize or overwrite dates;
- use strings as unbounded database keys without hashes/offsets;
- change a binary format without versioning and compatibility tests.

---

## 7. Definition of done

The system is not complete when documentation exists. It is complete when:

```text
Drive selection
+ authenticated resumable transfer
+ governed Binder handoff
+ bounded Termux worker
+ deterministic RafPolimata segments
+ bounded LlamaRafaelia anchoring
+ RafGitTools navigation
+ CI and real-corpus evidence
```

all execute without source mutation, unbounded memory, false success or provenance loss.
