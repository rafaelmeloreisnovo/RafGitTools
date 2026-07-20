# Evolutionary Process — RafGitTools

Generated: 2026-07-20

This document maps the next development iterations in order of impact and effort.
Each phase builds on the previous without breaking existing behavior.

---

## Phase 1 — Stability (immediate)

These are correctness fixes that should land before any new features.

| ID | Item | File | Effort |
|----|------|------|--------|
| E1-01 | Fix `ComplianceManager.kt:48` map-access `!!` crash | `core/compliance/ComplianceManager.kt` | 30 min |
| E1-02 | Fix `HomeScreen.kt:61` / `AuthScreen.kt:91` null-assert crashes | `ui/screens/home/`, `ui/screens/auth/` | 1 h |
| E1-03 | Fix `GIT_SAFE_COMMANDS` computed per-call (set → constant) | `ui/screens/terminal/TerminalViewModel.kt:108` | 15 min |
| E1-04 | Implement `LfsManager.install()` / `track()` / `fetch()` | `gitlfs/LfsManager.kt` | 2 h |
| E1-05 | Add Makefile to `rafaelia/block1/` | `rafaelia/block1/Makefile` | 30 min |

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

### 2B — Multi-platform manager

`platform/MultiPlatformManager.kt` is GitHub-only. Adding GitLab requires:
1. A `GitLabApiService.kt` (Retrofit interface for GitLab REST v4)
2. Token handling in `AuthRepository.kt`
3. A platform discriminator in `AuthMethod.kt`

Bitbucket and Azure DevOps follow the same pattern.

### 2C — LFS UI exposure

`LfsManager` is implemented but no UI screen exists. Add:
- `ui/screens/lfs/LfsScreen.kt` — shows tracked patterns, allows `lfs install`/`track`
- Wire into `RepositoryDetailScreen.kt` via a tab or menu item

### 2D — TerminalEmulator PTY

Replace the `ProcessBuilder` allowlist approach with Termux `terminal-view` for:
- True PTY with ANSI color codes
- Interactive commands (git rebase -i, vim, less)
- No command allowlist needed (sandboxed by app's filesystem permissions)

---

## Phase 3 — Offline-First (resilience)

### 3A — OfflineQueue persistence

Replace the in-memory queue with a Room entity:
```
OfflineOperation(id, repoPath, command, args, createdAt, retryCount)
```
A `WorkManager` periodic task drains the queue when connectivity is restored.

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

### 4A — rafaelia engine → Android

Promote `_incoming/` rafaelia C files from research to production:
1. Create `app/src/main/cpp/CMakeLists.txt` to build `rafaelia_core.c` + dependencies
2. Add a `RafaeliaJNI.kt` bridge and `librafaelia.so` target
3. Use the EMA/commit-gate primitives for predictive prefetch of git objects

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
Android App ─────────────► GitHub API (Retrofit)
     │
     ├──► JGitService ─────► Local git repositories (JGit 7.5.0)
     │
     ├──► kernel/native ─(PENDING llama.h)─► Local LLaMA inference
     │
     └──► [FUTURE] RafaeliaJNI ─────────────► _incoming/ rafaelia engine

_incoming/raf_client ────(standalone)────► ELF/DEX/PE binary analysis
BrowserRaf/ ─────────────(standalone)────► HTTPS client (ARM64 Linux)
rafaelia/block1/ ────────(standalone)────► Q16.16 geometry primitives
rafaelia/omega_hybrid/ ─(standalone)────► EMA attractor state machine
kiwi-extension/ ─────────(standalone)────► Browser extension (JS)
```

Items marked `(standalone)` have no current Android app connection.
Connecting them is the core evolutionary path.
