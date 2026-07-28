# Repository Map — RafGitTools

Generated: 2026-07-20

---

## Top-Level Layout

```
RafGitTools/
├── app/                        # Android application (single-module Gradle project)
├── _incoming/                  # C/ASM pipeline: rafaelia engine + raf_client binary
├── _upcoming/                  # Archived zip snapshots (not compiled, not integrated)
├── BrowserRaf/                 # Standalone freestanding HTTPS browser (no Android link)
├── IaCopiler/                  # Empty directory (reserved, no current content)
├── Livro/                      # Book/document assets
├── Raf/                        # Miscellaneous assets
├── COMPILER/                   # Compiler experiments
├── fazer/                      # OLDER draft versions of app/src/ files — superseded
├── rafaelia/                   # Native algorithm libraries (standalone, no JNI)
│   ├── block1/                 # Q16.16 fixed-point geometry primitives
│   └── omega_hybrid/           # Higher-level EMA/attractor state machine
├── kernel/                     # Android JNI bridge to LLaMA inference
│   ├── native/                 # raf_kernel_jni.c (requires llama.h — external dep)
│   └── policy/                 # Policy JSON files
├── kiwi-extension/             # Kiwi Browser extension (JS/HTML, no Android JNI)
├── native/                     # Validation evidence and reference artifacts
│   └── rafaelia_omega_v32/     # Freestanding ELF reference (19016 bytes, zero DT_NEEDED)
├── internal/                   # Governance and orchestration metadata
│   ├── governance/             # capabilities.json, policy.json
│   └── orchestrator/           # vertical_slice.md
├── docs/                       # Project documentation
├── scripts/                    # CI, federation, native, termux, vertical-slice scripts
├── .github/workflows/          # CI/CD GitHub Actions
└── [config files]              # build.gradle, settings.gradle, gradle.properties, etc.
```

---

## app/ — Android Application

```
app/src/main/kotlin/com/rafgittools/
├── MainActivity.kt                     # Single Activity, Compose NavHost
├── RafGitToolsApplication.kt          # Hilt Application class
│
├── data/
│   ├── auth/
│   │   ├── AuthInterceptor.kt          # OkHttp token injection
│   │   ├── AuthMethod.kt               # Sealed class: PAT / OAuth / GhCli / SSH
│   │   ├── AuthRepository.kt           # Token get/save/clear
│   │   ├── AuthTokenCache.kt           # In-memory + DataStore cache
│   │   ├── GhCliAuthImporter.kt        # Imports token from ~/.config/gh/
│   │   ├── OAuthDeviceFlowManager.kt   # GitHub Device Flow (POST /login/device/code)
│   │   └── TokenRefreshManager.kt      # Proactive token refresh
│   ├── cache/
│   │   ├── AsyncCacheManager.kt        # Generic TTL cache on top of Room
│   │   ├── CacheDao.kt, CacheDatabase.kt, CacheEntities.kt
│   ├── git/
│   │   └── JGitService.kt              # 2100+ lines, full JGit 7.5.0 wrapper
│   ├── github/
│   │   ├── GithubApiService.kt         # Retrofit interface (100+ endpoints)
│   │   └── GithubRepository.kt        # Repository pattern wrapping the API
│   ├── network/
│   │   └── RetrofitProvider.kt         # OkHttp + Gson + AuthInterceptor setup
│   ├── preferences/
│   │   └── PreferencesRepository.kt    # DataStore-backed settings
│   ├── repository/
│   │   └── GitRepositoryImpl.kt        # Domain ↔ JGitService bridge
│   └── storage/
│       └── RepoStorage.kt              # Cloned repo path management
│
├── di/
│   └── AppModule.kt                    # Hilt module wiring
│
├── domain/
│   ├── error/AppError.kt               # Sealed error hierarchy
│   ├── model/
│   │   ├── Git*.kt                     # GitBranch, GitCommit, GitDiff, GitFile, etc.
│   │   └── github/GithubModels.kt      # All GitHub API models with @SerializedName
│
├── core/
│   ├── compliance/ComplianceManager.kt # GDPR/CCPA/SOC2 compliance checks
│   ├── error/                          # GlobalExceptionHandler, ErrorHandler, Validator
│   ├── feature/FeatureFlags.kt         # Runtime feature toggles
│   ├── haptics/HapticFeedbackManager.kt
│   ├── localization/                   # i18n support (LocalizationManager)
│   ├── logging/DiffAuditLogger.kt      # Immutable audit trail for git diffs
│   ├── privacy/
│   │   ├── PrivacyManager.kt           # GDPR Article 15/17/20 + CCPA compliance
│   │   └── EncryptedPrivacyStorage.kt  # AndroidKeyStore-backed privacy data
│   └── security/
│       ├── BiometricAuthManager.kt
│       ├── CredentialManager.kt
│       ├── EncryptionManager.kt
│       ├── MultiAccountManager.kt      # Multi-account switching
│       ├── SecureStorage.kt
│       ├── SecurityManager.kt
│       ├── SshKeyManager.kt
│       └── SshSessionFactory.kt        # JGit SSH via JSch
│
├── bisect/BisectManager.kt             # git bisect wrapper (real + stub mode)
├── gitlfs/LfsManager.kt                # Git LFS wrapper (real + stub mode)
├── platform/MultiPlatformManager.kt    # GitHub-only; GitLab/Bitbucket TODOs
├── terminal/TerminalEmulator.kt        # Allowlist-based command runner (no PTY)
├── webhook/WebhookHandler.kt           # GitHub webhook event handling
├── worktree/WorktreeManager.kt         # git worktree (real + stub mode)
│
└── ui/
    ├── screens/                        # One Screen.kt + ViewModel.kt per feature
    │   ├── auth/                       # Login, OAuth flow
    │   ├── commits/                    # Commit list + detail
    │   ├── createissue/                # New issue form
    │   ├── createpr/                   # New PR form
    │   ├── diff/                       # Unified diff viewer
    │   ├── filebrowser/                # Repository file tree
    │   ├── home/                       # Dashboard
    │   ├── issues/                     # Issue list + detail
    │   ├── notifications/              # GitHub notifications
    │   ├── profile/                    # User profile
    │   ├── pullrequests/               # PR list + detail
    │   ├── releases/                   # Releases list + detail
    │   ├── repository/                 # Repo list, detail, add
    │   ├── search/                     # Global search
    │   ├── settings/                   # App settings
    │   ├── stash/                      # git stash list
    │   ├── tags/                       # git tag list
    │   └── terminal/                   # Embedded terminal UI
    ├── components/SyntaxHighlighter.kt # Token-based syntax coloring
    └── theme/                          # Material3 colors, typography, custom tokens
```

---

## _incoming/ — C/ASM Pipeline

All files are freestanding (nomalloc, nolibc, no external deps) or near-freestanding.

### raf_client subsystem (complete, tested)
| File | Role |
|------|------|
| `raf_client.c` | Entry point — ELF/DEX/PE detection + friction-gate CRC32C EMA |
| `raf_client_sys.h` | Types, syscall wrappers (ARM32/ARM64/x86-64/RISCV64), arena, I/O |
| `raf_elf.h` | ELF32/ELF64 parser (ECtx, macros, mach/type string tables) |
| `raf_dex.h` | DEX 035–039 parser (DCtx, Adler-32 verification) |
| `raf_pe.h` | PE/COFF parser (DosH→CoffH→OptHdr, PeCtx, machine codes) |
| `raf_client_start.S` | _start assembly for ARM32/ARM64/x86-64/RISCV64 |
| `Makefile.client` | Build targets: arm64, arm, x64, riscv64 |

### Header alias layer (bridges canonical names ↔ repo filenames)
| Alias header | Points to |
|---|---|
| `rafaelia_toroidal_inference.h` | `repo_toroidal.h` |
| `rafaelia_commit_gate_ll.h` | `repo_commit_gate.h` |
| `rafaelia_gpu_orchestrator.h` | `repo_gpu_orch.h` |
| `baremetal.h` | `baremetal_nomalloc.h` |

### Rafaelia engine files
| File | Role |
|------|------|
| `rafaelia_core.c` + `rafaelia_types.h` | Core EMA/Q16.16 primitives |
| `rafaelia_arena.h` | BSS arena macros |
| `rafaelia_bitraf.c` | Bit-manipulation accelerators |
| `rafaelia_glue.c` | Multi-component integration glue |
| `rafaelia_gpu_mid.c` + `rafaelia_gpu_mid.h` | GPU mid-layer (ARM NEON, dlopen) |
| `rafaelia_orchestrator.c` | Top-level scheduler/orchestrator |
| `rafaelia_sigma_omega.c` | Sigma-Omega attractor math |
| `rafaelia_integration.c` | Full integration patch |
| `repo_toroidal.c` + `repo_toroidal.h` | Toroidal topology inference |
| `repo_commit_gate.c` + `repo_commit_gate.h` | CRC32C commit gate (low-level) |
| `repo_gpu_orch.c` + `repo_gpu_orch.h` | GPU orchestration layer |
| `baremetal_nomalloc.c` + `baremetal_nomalloc.h` | Baremetal nomalloc runtime |
| `repo_baremetal_orig.c` + `repo_baremetal_orig.h` | Original baremetal reference |
| `repo_baremetal_jni_orig.c` | Original baremetal JNI reference |
| `bitstack.c` + `bitstack.h` | Bit-stack data structure |
| `rafaelia_jni_direct.c` | Direct JNI stubs |

---

## rafaelia/ — Native Algorithm Libraries

### block1/ — Q16.16 Fixed-Point Geometry
- `raf_geom.h` / `raf_geom.c` — sphere/torus volume, toroidal map, coherence update
- `raf_geom_demo.c` — CLI demo
- No Makefile (needs one — see PENDING.md)
- No JNI connection to Android app

### omega_hybrid/ — EMA/Attractor State Machine
- `raf_omega.h` / `raf_omega.c` — high-level state machine over block1 primitives
- `demo.c` — CLI demo
- `Makefile` — builds Linux x86-64 target
- No Android/JNI connection (standalone research artifact)

---

## BrowserRaf/ — Standalone HTTPS Browser

Freestanding (nomalloc, nolibc, svc #0 / syscall ABI):
- `internal/br_sys.h` — syscall wrappers (ARM64)
- `internal/br_types.h` — primitive types and arena
- `internal/br_start.S` — _start assembly
- `internal/br_dns.h` / `br_http.h` / `br_tls.h` / `br_html.h` — protocol layers
- `internal/br_main.c` — main entry
- `internal/Makefile` — Android NDK ARM64 build
- **No Android connection.** Outbound HTTPS is not supported in the remote CI environment.

---

## kernel/ — LLaMA JNI Bridge

- `native/raf_kernel_jni.c` — JNI bridge to local LLaMA inference
- `native/raf_kernel_api.h` — C API declarations
- **Requires `llama.h` from an external llama.cpp build** — not included in repo
- Two PENDING items remain (see PENDING.md)

---

## kiwi-extension/ — Browser Extension

- `popup.html` / `popup.css` / `popup.js` — Kiwi Browser extension UI
- `manifest.json` — WebExtension manifest
- `README.md` — Usage instructions
- **No Android JNI connection.** Loaded directly into Kiwi Browser as a sideloaded extension.

---

## native/rafaelia_omega_v32/ — Validation Evidence

- Contains a reference manifest with ELF metadata for a known-good freestanding build
- File size: 19016 bytes stripped
- Confirmed: `elf.interp=false`, `runtime.libc=false`, `elf.dt_needed_count=0`
- **Not connected to the Android app via JNI.** Evidence record only.

---

## _upcoming/ — Archived Snapshots

- Two ZIP files: `RafGitTools-main_fixed_build (1).zip` and `RafGitTools-main_patched (1).zip`
- Subdirectory `1/` (contents unverified)
- These are point-in-time snapshots, not active development branches.

---

## fazer/ — Superseded Draft Files

Contains 19 `.kt` files that are **earlier drafts** of their counterparts in `app/src/`.
The `app/src/` versions are newer and should be used. The `fazer/` directory exists for
historical reference only and can be deleted once the team confirms no unique content remains.
