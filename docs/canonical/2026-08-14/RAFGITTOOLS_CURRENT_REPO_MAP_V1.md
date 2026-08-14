# RafGitTools — Current Repository Map V1

Date: **2026-08-14**  
Status: **CANONICAL_CURRENT_MAP / append-only checkpoint**

## Purpose

Este mapa representa a árvore operacional relevante do RafGitTools no corte atual. Ele não apaga `docs/REPO_MAP.md`; o documento anterior permanece histórico. Quando houver conflito, a realidade deve ser resolvida por código + teste + build + evidence, não por antiguidade do texto.

## Truth hierarchy

```text
app/src + build configuration
  -> tests
  -> workflows/gates
  -> APK + receipt
  -> device receipt
  -> current-state docs
  -> roadmap
  -> historical docs
```

## Top-level operational map

```text
RafGitTools/
├── app/                         Android application source of truth
│   └── src/
│       ├── main/                production source/resources/native wiring
│       ├── test/                JVM/unit regression tests
│       └── androidTest/         instrumented-test surface
├── tests/                       Python custody/structure/runtime contract tests
├── contracts/                   machine-readable governance contracts/schemas
├── scripts/
│   ├── runtime/                 build/device receipts + triangle closure
│   └── ...                      readiness/audit/native/Termux helpers
├── .github/workflows/           CI, Android build and validation gates
├── data/evidence/               append-only machine-readable evidence
├── docs/
│   ├── RAFGITTOOLS_CURRENT_STATE.md
│   ├── STATUS_REPORT.md
│   ├── RAFGITTOOLS_CODE_REALITY_MATRIX.md
│   ├── RAFGITTOOLS_ROADMAP_TRUE.md
│   ├── PENDING_33_ITEMS.md
│   ├── INDEX.md
│   └── canonical/               append-only checkpoints
├── kernel/                      native/JNI/AI integration surfaces
├── rafaelia/                    standalone native/research components
├── kiwi-extension/              Manifest V3 local bridge client
├── BrowserRaf/                  standalone browser/native experiment
├── _incoming/                   experimental/native material; not automatic source truth
├── fazer/                       superseded historical drafts; not compiled source truth
└── ECOSYSTEM_RUNTIME_STATE.json executable current-state matrix
```

## Android application map

### Authentication / security

Primary surfaces include:

- `AuthViewModel`;
- `AuthRepository`;
- `AuthInterceptor`;
- `TokenRefreshManager`;
- `OAuthDeviceFlowManager`;
- `GhCliAuthImporter`;
- `SshKeyManager` / SSH transport;
- `BiometricAuthManager`;
- `MultiAccountManager`.

Current reality: source advanced; auth unit tests passed in the verified build checkpoint; real Android/OAuth/SSH fixtures remain separately gated.

### Git engine

Primary surface: `data/git/JGitService.kt` plus `InteractiveStagingService.kt`.

Capabilities include clone variants, commit/amend, fetch/pull/push, pull-rebase, force-with-lease, branch operations, merge, diff/log, stash, cherry-pick/revert/reset, reflog/blame/config/search and per-hunk staging.

P33 current source state: **33/33 SOURCE_FUNCTIONAL**.

### GitHub / network

Retrofit/OkHttp repositories/services cover the GitHub client surface. Endpoints and UI availability are not treated as equivalent to full E2E validation.

### Multi-provider

`platform/MultiPlatformManager.kt` contains implemented adapters for:

- GitLab;
- Bitbucket;
- Gitea/Forgejo;
- Azure DevOps.

This supersedes the historical description “GitHub-only/TODO” found in older repository maps. Provider credentials/endpoints remain `FIXTURE_GATED`.

### Offline

Operational surfaces:

- `offline/OfflineQueue.kt`;
- `offline/RoomOfflineQueueStorage.kt`;
- `offline/AtomicFileQueueStorage.kt`;
- `offline/SyncWorker.kt`.

Source/build status is implemented; physical restart/recovery evidence remains DEVICE-gated.

### Terminal

`terminal/TerminalEmulator.kt` is a **bounded executor**, not a PTY/VT100 terminal. Writable Git actions remain governed/typed. Full PTY remains `TOKEN_VAZIO_PTY`.

### External Git runtimes

LFS, worktree, bisect and GPG have code/adapters, but their external runtime fixtures are not promoted without execution evidence.

### Native/JNI

The Android/native path is included in the verified dual-ABI APK:

- `armeabi-v7a` PRESENT;
- `arm64-v8a` PRESENT.

JNI/RAFAELIA bridge is therefore BUILD-verified for the checkpoint APK, while native calls on a physical device remain DEVICE-gated.

### Local AI / browser bridge

Source surfaces include local loopback bridge/model client and `kiwi-extension/`. Presence in source/build does not prove a working GGUF/model server or Kiwi sideload runtime.

## Evidence / governance map

```text
SOURCE
  app/src/
    ↓
TESTS
  app/src/test/ + tests/
    ↓
CI / BUILD
  .github/workflows/android-client-build.yml
    ↓
BUILD RECEIPT
  scripts/runtime/write_android_build_receipt.py
    ↓
DEVICE RECEIPT
  physical collector / expected commit + APK SHA
    ↓
TRIANGLE CLOSURE
  scripts/runtime/close_first_compile_run_triangle.py
```

## Verified build anchor

Commit: `bbdb556a59c06a23cc2f6df6ba0ae7c98466a4fa`  
Run: `31821491676`  
APK SHA-256: `115b9cb1e71f53f16b2648924a09549b8e5e0b9e453280cab2e7f183a411ebf6`

This anchor proves BUILD for that commit/APK. It does not prove DEVICE for later commits.

## CI provenance hardening added after that build

The active `ci.yml` formerly contained a hardcoded publication target `issues/236/comments`. The current branch replaces it with the current pull-request event number and adds `tests/test_workflow_pr_binding.py` so a numeric hardcoded target is rejected structurally.

Because workflow/test files changed after the verified build anchor, the current head requires a fresh CI/build receipt before being treated as commit-bound BUILD_VERIFIED.

## Historical zones

These zones may contain valuable material but are not automatically promoted:

- `fazer/` — superseded drafts;
- `_incoming/` — experimental/import material;
- standalone native/research directories;
- historical roadmap/status documents.

Promotion requires explicit integration path and evidence.

## Current frontier

```text
CI_HEAD_PASS
  -> APK/BUILD_RECEIPT for exact current head
  -> physical ARM install + launch
  -> runtime receipt
  -> triangle_closure PASS
  -> critical real fixtures
  -> release gate
```

`DEVICE = TOKEN_VAZIO_PHYSICAL_DEVICE_REQUIRED` until that physical chain exists.
