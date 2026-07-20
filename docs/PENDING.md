# Pending Items — RafGitTools

Generated: 2026-07-20 | Branch: claude/code-docs-alignment-4ue3r1

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

## P2 — Stub: LFS install/track/fetch

`LfsManager.install()`, `LfsManager.track()`, and `LfsManager.fetch()` all return
`NotImplementedError("... is not implemented yet")` unconditionally (not just for empty
repoPath). These functions need real implementations using `ProcessBuilder` + `git lfs`.

| File | Function | Line |
|------|----------|------|
| `gitlfs/LfsManager.kt` | `install()` | 65 |
| `gitlfs/LfsManager.kt` | `track(pattern)` | 85 |
| `gitlfs/LfsManager.kt` | `fetch()` | 104 |

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

## P4 — Pending: MultiPlatformManager GitLab/Bitbucket/Gitea/Azure

`platform/MultiPlatformManager.kt` contains 4 `// TODO` items for non-GitHub platforms:
- GitLab API integration
- Bitbucket API integration
- Gitea/Forgejo API integration
- Azure DevOps API integration

Only GitHub is currently functional.

---

## P5 — Pending: rafaelia/block1 Makefile

`rafaelia/block1/` has no Makefile. The `raf_geom.c` file compiles cleanly with
`gcc -O2 -std=c11 raf_geom.c raf_geom_demo.c -lm -o raf_geom_demo`. A Makefile should be
added to match the `rafaelia/omega_hybrid/Makefile` style.

---

## P6 — Pending: _upcoming/ contents audit

`_upcoming/` contains two ZIP files and an unexplored `1/` subdirectory. These should be
inventoried and either promoted to active branches or deleted.

---

## P7 — Stub: OfflineQueue persistence

`OfflineQueue.kt` (if present) stores queued git operations in memory only — they are
lost on process death. A Room-backed persistent queue would be needed for reliable
offline-first operation.

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

## P10 — fazer/ cleanup

`fazer/` contains 19 `.kt` files that are older drafts. They should be deleted once the
team confirms no unique algorithm or UI pattern exists in them that was not forward-ported
to `app/src/`.

---

## P11 — License consolidation

Four license files exist at the repo root:
- `LICENSE`
- `LICENSE.md`
- `License2.md`
- `Lincense4.md` (typo: "Lincense")

These should be consolidated into a single `LICENSE` file with the correct SPDX identifier.
