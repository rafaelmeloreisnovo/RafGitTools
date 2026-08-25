# Known Bugs — RafGitTools

Generated: 2026-07-20

Correctness defects with file:line references. Fixed items are noted.

---

## FIXED — BUG-01: PrivacyManager GlobalScope leak

**File**: `app/src/main/kotlin/com/rafgittools/core/privacy/PrivacyManager.kt`
**Was**: Line 50 used `GlobalScope.launch` inside `init {}` — coroutine outlives any
lifecycle scope and cannot be cancelled.
**Fix (PR #271)**: Replaced with `managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)`.

---

## FIXED — BUG-02: PrivacyManager GDPR counts always 0

**File**: `app/src/main/kotlin/com/rafgittools/core/privacy/PrivacyManager.kt`
**Was**: Lines 284–306: `getCredentialsCount()`, `getRepositoriesCount()`, `getSettingsCount()`
all returned hardcoded `0`, breaking GDPR Article 15 data inventory responses.
**Fix (PR #271)**:
- `getCredentialsCount()`: queries `AndroidKeyStore` for alias count.
- `getRepositoriesCount()`: counts subdirectories in `filesDir/repositories/`.
- `getSettingsCount()`: reads `DataStore.data.first().asMap().size`.

---

## FIXED — BUG-03: TerminalViewModel unsafe `!!` after async gap

**File**: `app/src/main/kotlin/com/rafgittools/ui/screens/terminal/TerminalViewModel.kt`
**Was**: Line 118: `workingDir = _workingDir.value!!` inside `viewModelScope.launch {}`.
The null check happened before the launch but the captured value could change between
the check and the coroutine body executing.
**Fix (PR #271)**: Captured `val dir = _workingDir.value ?: return` before the launch,
passed `dir` directly into `TerminalEmulator.executeCommand()`.

---

## FIXED — BUG-04: NotificationsScreen renders empty-state only

**File**: `app/src/main/kotlin/com/rafgittools/ui/screens/notifications/NotificationsScreen.kt`
**Was**: The screen only showed the empty-state `Box`; even when `viewModel.notifications`
was non-empty (API returned results), no list was rendered. `uiState` was collected but
never branched on.
**Fix (PR #271)**: Added full `LazyColumn` with `NotificationCard` composables, wired
to `uiState` (Loading/Empty/Error/Success branches), added mark-as-read and mark-all-read
actions in the app bar.

---

## FIXED — BUG-05: _incoming/ compilation broken — missing header aliases

**Files**: `_incoming/repo_toroidal.c`, `_incoming/repo_commit_gate.c`,
`_incoming/repo_gpu_orch.c`, `_incoming/baremetal_nomalloc.c`,
`_incoming/repo_baremetal_orig.c`, `_incoming/repo_baremetal_jni_orig.c`
**Was**: Each source file `#include`d a canonical header name that did not exist in
`_incoming/`. The headers existed under different `repo_*` / `baremetal_nomalloc` names
but with the canonical `#ifndef` guards.
| Missing name | Actual file |
|---|---|
| `rafaelia_toroidal_inference.h` | `repo_toroidal.h` |
| `rafaelia_commit_gate_ll.h` | `repo_commit_gate.h` |
| `rafaelia_gpu_orchestrator.h` | `repo_gpu_orch.h` |
| `baremetal.h` | `baremetal_nomalloc.h` |
**Fix (PR #271)**: Created 4 one-line alias headers that `#include` the canonical files.

---

## FIXED — BUG-06: ComplianceManager map access with `!!`

**File**: `app/src/main/kotlin/com/rafgittools/core/compliance/ComplianceManager.kt:48`
**Was**: `getComplianceStatus()[standard]!!` — map lookup returns nullable; `!!` would
crash if `standard` is not present in the map.
**Fix (PR #279)**: Replaced with `getComplianceStatus().filterKeys { it == standard }`,
which returns an empty map instead of crashing if the key is missing.

---

## FIXED — BUG-07: LfsManager install/track/fetch always throw

**File**: `app/src/main/kotlin/com/rafgittools/gitlfs/LfsManager.kt`
**Was**: These three functions were documented as unconditionally returning `NotImplementedError`.
**Fix (verified 2026-07-20)**: The functions already have real `ProcessBuilder` + `git lfs`
implementations. `NotImplementedError` fires only when `repoPath.isEmpty()` — the same
backward-compatibility guard used by `WorktreeManager` and `BisectManager` (see P1 in PENDING.md).
Non-empty `repoPath` invokes `isLfsAvailable()` then `runLfs(repoPath, ...)`. The `PENDING.md`
entry P2 was based on a stale reading of the file.

---

## FIXED — BUG-08: HomeScreen / AuthScreen non-null assert on user data

**Files**:
- `ui/screens/home/HomeScreen.kt:61` — `user!!`
- `ui/screens/auth/AuthScreen.kt:91` — `username!!`
**Was**: Non-null asserts on StateFlow-backed delegate properties. Kotlin cannot smart-cast
delegates, so `user!!` inside `if (user != null)` was needed to compile but was still
unsafe if the value changed between check and use.
**Fix (PR #279)**: Captured `val u = user` / `val currentUsername = username` as local
vals before the null check, allowing Kotlin smart-cast to eliminate the `!!` entirely.

---

## FIXED — BUG-09: TerminalEmulator GIT_SAFE_COMMANDS check uses `map { split() }`

**File**: `ui/screens/terminal/TerminalViewModel.kt`
**Was**: Line 108: `GIT_SAFE_COMMANDS.map { it.split(" ").first() }` re-created a list on
every command execution.
**Fix (PR #278)**: Added `private val SAFE_BASE_CMDS: Set<String> = GIT_SAFE_COMMANDS.map {
it.split(" ").first() }.toSet()` at class-init level; the check now reads `baseCmd !in
SAFE_BASE_CMDS`.
