# Pending Items — RafGitTools

Generated: 2026-07-21 | Branch: claude/code-docs-alignment-4ue3r1

This file tracks stubs, incomplete implementations, and known gaps.
See BUGS.md for correctness defects. See EVOLUTIONARY_PROCESS.md for roadmap.

---

## P1 — Stub: empty-repoPath guard (by design)

These three managers return `NotImplementedError` when `repoPath` is empty. This is an
intentional backward-compatibility guard for callers that pass a default empty string. The
real implementation runs when `repoPath` is non-empty. **No action required unless the
calling code is discovered to be broken.**

| File | Lines | Stub condition |
|------|-------|---------------|
| `worktree/WorktreeManager.kt` | 70, 95, 117 | `repoPath.isEmpty()` → `NotImplementedError` |
| `bisect/BisectManager.kt` | 68, 94, 116, 138, 160, 177 | same pattern |
| `gitlfs/LfsManager.kt` | 65, 85, 104 | same pattern |

---

## ~~P2 — DONE: LFS install/track/fetch~~

`LfsManager.install()`, `.track()`, and `.fetch()` already have real `ProcessBuilder` +
`git lfs` implementations. The `NotImplementedError` fires only for the empty-repoPath
guard (see P1), not unconditionally. Verified 2026-07-20 by reading the actual file.
Also closes BUG-07 in BUGS.md.

---

## P3 — Pending: kernel/native JNI

Two items in `kernel/native/raf_kernel_jni.c` remain incomplete:

1. **Line 89**: CTI path integration marked `/* RMR-CTI integration: PENDING */`
   — the `cti_path` JNI argument is received but cast to `(void)`. Need to wire
   it through to the llama context initialization.

2. **Lines 235–242**: Multi-turn tool call loop described as `/* Full multi-turn tool call
   loop is PENDING */` — currently executes only one round of tool calls and returns.
   A full loop would continue until the model stops requesting tools.

**Blocker**: both items also require `llama.h` from an external llama.cpp build, which
is not in this repository.

---

## ~~P4 — DONE: MultiPlatformManager all providers~~

All five providers are now implemented:

| Provider | Status | PR |
|---|---|---|
| GitHub | Implemented (original) | — |
| GitLab | Implemented: `GET /api/v4/projects?membership=true` via `GitLabApiService` | #283 |
| Bitbucket | Implemented: `GET /2.0/repositories/{workspace}` via `BitbucketApiService` | #283 |
| Gitea/Forgejo | Implemented: `GET /api/v1/user/repos` via `GiteaApiService` (token auth) | #284 |
| Azure DevOps | Implemented: `GET /{org}/{project}/_apis/git/repositories?api-version=7.0` via `AzureDevOpsApiService` (PAT Basic auth) | this branch |

Azure DevOps uses PAT-as-Basic-auth (`Base64(":token")` per Microsoft docs). The
`isDisabled`/`isInMaintenance` flags on `AzureDevOpsRepository` allow callers to
filter inactive repos. HTTP 203 (Non-Authoritative — returned by Azure when the PAT
lacks `vso.code` scope) is treated as an auth error alongside 401.

---

## ~~P5 — DONE: rafaelia/block1 Makefile~~

Added `rafaelia/block1/Makefile` in PR #279. Targets: `libraf_geom.a`, `demo`, `clean`.
Style matches `rafaelia/omega_hybrid/Makefile`.

---

## ~~P6 — DONE: _upcoming/ contents audit~~

Inventoried 2026-07-20. `_upcoming/` contains:

| File | Size | Content |
|------|------|---------|
| `RafGitTools-main_fixed_build (1).zip` | 548 KB | Source snapshot with build fixes applied prior to PR #278 |
| `RafGitTools-main_patched (1).zip` | 552 KB | Source snapshot with additional patches |
| `1` | 1 byte | Newline-only placeholder — trivial |

The ZIP archives are historical build snapshots. No unique code exists in them that is not
already present in the current `main` history. They can be deleted from the branch once the
team is satisfied the snapshots are no longer needed as references.

---

## ~~P7 — DONE: OfflineQueue persistence~~

`OfflineQueue.kt` already implements the `OfflineQueueStorage<T>` interface pattern.
The queue accepts an optional `storage: OfflineQueueStorage<T>?` constructor parameter;
when supplied, every `enqueue()` and `dequeue()` atomically commits the new snapshot via
`storage.replace()`, with rollback on failure. The in-memory default remains when `storage`
is null. A Room-backed `OfflineQueueStorage` implementation can be wired in without changing
`OfflineQueue` itself — the extension point is ready.

---

## P8 — Stub: TerminalEmulator (no PTY)

`terminal/TerminalEmulator.kt` uses `ProcessBuilder` with an allowlist — it is not a
real VT100/PTY terminal. ANSI escape sequences in command output are not handled.
For a richer terminal, integrate the Termux `terminal-view` library.

---

## P9 — Missing JNI integration: rafaelia engine → Android app

The `_incoming/` rafaelia files (C) are not compiled into any Android `.so`. They are
standalone research artifacts. If real-time inference inside the app is needed, a JNI
bridge analogous to `kernel/native/raf_kernel_jni.c` must be created.

---

## ~~P10 — DONE: fazer/ audit (safe to delete)~~

All 18 `.kt` files in `fazer/` were audited 2026-07-20 by diffing against `app/src/`
counterparts:

| Result | Files |
|--------|-------|
| Identical (already integrated) | `NotificationsViewModel.kt`, `ReleaseDetailViewModel.kt`, `ReleasesViewModel.kt`, `SyntaxHighlighter.kt` |
| `app/src/` is ahead | All remaining 14 — they use `collectAsStateWithLifecycle()` vs the older `collectAsState()` in fazer/, and include bug-fixes from PR #278 (SAFE_BASE_CMDS, `val dir` capture) and PR #282 (smart-cast fixes). |

No unique algorithm or UI pattern in `fazer/` was missed in the forward-port.
The `fazer/1.md` and `fazer/README_FIXES.md` describe the February 2026 integration plan
that is now complete. The entire `fazer/` directory can be deleted.

---

## P11 — Root artifact rename (three misnamed files)

Four files at the repo root were originally described as license files. Audited 2026-07-20:

| File | Actual content | Action |
|------|----------------|--------|
| `LICENSE` | GNU GPL v3 (real license) | Keep as-is |
| `LICENSE.md` | C source: `core_rafaelia_matriz_supralegal.c` — author's protected IP | Rename to `.c` only with author approval |
| `License2.md` | C source: `bitraf64_compressor_supralegal.c` — author's protected IP | Rename to `.c` only with author approval |
| `Lincense4.md` | Math/theory document (not a license; typo in filename) | Rename to `math_theory.md` only with author approval |

`LICENSE.md` and `License2.md` carry explicit "cláusula pétrea" intellectual-property headers
forbidding alteration of the file or its metadata. These files must **not** be renamed, moved,
or deleted without the author's consent. The file name is part of the metadata.

**Owner action required**: confirm whether these three files should be renamed / moved,
or left at their current paths. The `LICENSE` (GPL v3) is the operative license and
GitHub will detect it correctly. SPDX header `SPDX-License-Identifier: GPL-3.0-only`
can be added to `LICENSE` as a cosmetic improvement without touching the other files.

---

## ~~P12 — DONE: raf_client RISCV64 + PE/COFF support~~

`_incoming/raf_client` now supports four ISAs and three binary formats. Verified 2026-07-21
by reading the actual files:

| File | What was added |
|------|---------------|
| `_incoming/raf_client_sys.h` | RISCV64 ecall block (`a7`/`a0`–`a3`, `ecall`); `openat=56 read=63 write=64 close=57 exit_group=94`; `usize` typedef covers RISCV64 |
| `_incoming/raf_elf.h` | `EM_RISCV=243u` constant; `ELF_MACH_STR()` returns `"riscv64"` |
| `_incoming/raf_client_start.S` | RISCV64 `_start`: `mv fp,zero / mv ra,zero / call client_main / li a7,94 / li a0,0 / ecall` |
| `_incoming/Makefile.client` | `ARCH=riscv64` target using `riscv64-linux-gnu-gcc`, `-march=rv64gc -mabi=lp64d` |
| `_incoming/raf_pe.h` | New file — PE/COFF parser: `DosH`, `CoffH`, `PeCtx`, `PE_PARSE`, `PE_MACH_STR` macro; machine codes AMD64/ARM64/ARM/I386/RISCV64/RISCV32/THUMB |
| `_incoming/raf_client.c` | PE detection branch after ELF/DEX: `PE_MAGIC_OK` → `PE_PARSE` → friction gate → `_out_pe()` |

Build matrix: `ARCH=arm64` (default), `ARCH=arm`, `ARCH=x64`, `ARCH=riscv64`.
Freestanding constraints maintained: zero `DT_NEEDED`, zero libc, zero malloc.
The plan at `~/.claude/plans/os-c-digos-est-o-mais-purring-starlight.md` (PRÓXIMA FASE section) is complete.
