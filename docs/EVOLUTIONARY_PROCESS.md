# Evolutionary Process — RafGitTools

Generated: 2026-07-21 (updated)

This document maps the next development iterations in order of impact and effort.
Each phase builds on the previous without breaking existing behavior.

---

## ~~Phase 1 — Stability (COMPLETE as of 2026-07-21)~~

All five E1 items are done. See BUGS.md for details.

| ID | Item | Status |
|----|------|--------|
| E1-01 | Fix `ComplianceManager.kt:48` map-access `!!` crash | **Fixed** PR #279 |
| E1-02 | Fix `HomeScreen.kt:61` / `AuthScreen.kt:91` null-assert | **Fixed** PR #279 |
| E1-03 | Fix `GIT_SAFE_COMMANDS` computed per-call | **Fixed** PR #278 |
| E1-04 | Implement `LfsManager.install()` / `track()` / `fetch()` | **Verified done** — were already implemented; `NotImplementedError` only for empty `repoPath` guard |
| E1-05 | Add Makefile to `rafaelia/block1/` | **Fixed** PR #279 |

---

## Phase 2 — Completeness (next sprint)

Features that are architecturally present but not wired end-to-end.

### 2A — LLaMA kernel JNI

`kernel/native/raf_kernel_jni.c` is complete except for two PENDING items:

1. Wire `cti_path` argument into llama context initialization (line 89).
2. Implement full multi-turn tool call loop (lines 235–242): continue calling
   `llama_decode()` + tool dispatch until the model stops requesting tools.

**Requires**: `llama.h` from a llama.cpp build. Recommended approach:
- Add llama.cpp as a Git submodule under `kernel/native/llama.cpp/`
- Update `kernel/native/Android.mk` or `CMakeLists.txt` to build it

### ~~2B — Multi-platform manager (COMPLETE 2026-07-21)~~

All five providers are implemented:

| Provider | Adapter | PR |
|---|---|---|
| GitHub | Original `GithubApiService` | — |
| GitLab | `GitLabApiService` — `GET /api/v4/projects?membership=true` | #283 |
| Bitbucket | `BitbucketApiService` — `GET /2.0/repositories/{workspace}` | #283 |
| Gitea/Forgejo | `GiteaApiService` — `GET /api/v1/user/repos` (token auth) | #284 |
| Azure DevOps | `AzureDevOpsApiService` — `GET /{org}/{project}/_apis/git/repositories?api-version=7.0` (PAT Basic) | #284 |

### ~~2C — LFS UI exposure (DONE 2026-07-21)~~

`ui/screens/lfs/LfsScreen.kt` and `LfsViewModel.kt` added and fully wired:
- Shows tracked patterns (`LfsManager.listTracked()`) in a `LazyColumn`
- Top-bar actions: Install (installs LFS hooks), Env (shows `git lfs env` in dialog)
- FAB → Track Pattern dialog (glob input → `LfsManager.track()`)
- Inline Fetch and Pull buttons when patterns exist
- `NotAvailable` state when git-lfs binary is absent (with Termux install hint)
- Snackbar feedback for all operations
- `Screen.Lfs` route added; composable registered in `MainActivity` NavHost
- "Git LFS" entry in `RepositoryDetailScreen` overflow menu (⋮ → Git LFS)

### 2D — TerminalEmulator PTY

Replace the `ProcessBuilder` allowlist approach with Termux `terminal-view` for:
- True PTY with ANSI color codes
- Interactive commands (git rebase -i, vim, less)
- No command allowlist needed (sandboxed by app's filesystem permissions)

---

## Phase 3 — Offline-First (resilience)

### ~~3A — OfflineQueue persistence (DONE 2026-07-21)~~

Room entity and DAO added to `CacheDatabase` (v3 via `MIGRATION_2_3`):

| Artifact | Details |
|---|---|
| `offline/OfflineOperationEntity.kt` | `@Entity(offline_operations)`: id, repoPath, command, args, createdAt, retryCount |
| `offline/OfflineOperationDao.kt` | `loadAll()`, `observeAll()`, `observeCount()`, `replaceAll()` (transactional) |
| `offline/RoomOfflineQueueStorage.kt` | Implements `OfflineQueueStorage<SyncOperation>`; encode/decode via `SyncOperation.encode/decode` |
| `data/cache/CacheDatabase.kt` | Bumped to v3; entity registered; `offlineOperationDao()` accessor added |
| `di/AppModule.kt` | `provideOfflineOperationDao()` and `provideRoomOfflineQueueStorage()` provided as singletons |

`SyncWorker` still uses `AtomicFileQueueStorage` (file-based). `RoomOfflineQueueStorage` is available via DI for callers that need SQL visibility (retry count filtering, repoPath queries, `observeCount()` Flow for UI badges).

### 3B — Repository sync state

Add a `SyncState` column to the Room `Repository` entity:
- `SYNCED` / `BEHIND` / `AHEAD` / `DIVERGED` / `CONFLICT`
- Background `WorkManager` job runs `git fetch` every 15 minutes for open repos

### 3C — Credential rotation

`TokenRefreshManager.kt` proactively refreshes OAuth tokens but does not handle
key rotation for SSH keys or PATs. Add:
- SSH key rotation: generate new Ed25519 key, call GitHub API to replace
- PAT expiry detection: parse `X-OAuth-Scopes` header, warn if < 7 days remaining

---

## Phase 4 — Native Performance (advanced)

### ~~4A — rafaelia engine → Android (BRIDGE DONE 2026-07-21)~~

JNI bridge created (P9):
- `kernel/native/rafaelia_jni.c` — re-targeted from `_incoming/rafaelia_jni_direct.c` to `com.rafgittools.kernel`; zero malloc, DirectByteBuffer I/O, inline CRC32C
- `app/.../kernel/RafaeliaCore.kt` — five native methods: `processNative`, `stepNative`, `profileNative`, `arenaSizeNative`, `crc32Native`

**Remaining**: wire `rafaelia_jni.c` into the Android build system (`CMakeLists.txt` or `Android.mk`) to produce `librafaelia.so`. Then use the EMA/commit-gate primitives for predictive prefetch of git objects.

### 4B — raf_client as forensic tool

`_incoming/raf_client` parses ELF/DEX/PE on-device. Potential use cases:
- APK diff tool: compare two DEX files, report class count delta and Adler-32 shift
- Pre-install binary scan: compute friction EMA for downloaded APKs before install
- CI artifact validator: run as a post-build step to verify no unexpected DT_NEEDED

### 4C — BrowserRaf integration

`BrowserRaf/` is a fully functional freestanding HTTPS client. Possible integration:
- Use it as a network probe to check GitHub API reachability without the Android networking stack
- Note: requires outbound TLS from the embedded runtime — evaluate security implications

---

## Phase 5 — Federation (long-term)

`scripts/federation/` and `docs/federation/` suggest a multi-instance federation model.
This is the most speculative phase:
- Federated repository index: share clone metadata between devices/users
- Distributed commit verification: cross-validate signed commits across federation peers
- `internal/governance/capabilities.json` defines the capability negotiation protocol

---

## Connection Map

```
Android App ─────────────► GitHub / GitLab / Bitbucket / Gitea / Azure DevOps APIs (Retrofit)
     │
     ├──► JGitService ─────────────────────► Local git repositories (JGit 7.5.0)
     │
     ├──► kernel/RafKernelBridge ─(llama.h PENDING)─► Local LLaMA inference
     │
     └──► kernel/RafaeliaCore ──(build PENDING)────► rafaelia_jni.c → 7D toroidal engine

_incoming/raf_client ────(standalone)────► ELF/DEX/PE binary analysis (arm64/arm/x64/riscv64)
BrowserRaf/ ─────────────(standalone)────► HTTPS client (ARM64 Linux, freestanding)
rafaelia/block1/ ────────(standalone)────► Q16.16 geometry primitives
rafaelia/omega_hybrid/ ─(standalone)────► EMA attractor state machine
kiwi-extension/ ─────────(standalone)────► Browser extension (JS)
```

Items marked `(standalone)` have no current Android app connection.
`kernel/RafaeliaCore` bridge exists but needs the native `.so` in the build system.
`kernel/RafKernelBridge` bridge exists but needs `llama.h` from an external llama.cpp build.
