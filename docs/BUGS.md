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

## OPEN — BUG-06: ComplianceManager map access with `!!`

**File**: `app/src/main/kotlin/com/rafgittools/core/compliance/ComplianceManager.kt:48`
**Problem**: `getComplianceStatus()[standard]!!` — map lookup returns nullable; `!!` will
crash if `standard` is not a key in the map. The key set should be validated first or a
default value used.
**Severity**: Medium — crashes on unknown compliance standard keys.
**Suggested fix**: Replace with `?: ComplianceStatus.UNKNOWN` or add an `in` guard.

---

## OPEN — BUG-07: LfsManager install/track/fetch always throw

**File**: `app/src/main/kotlin/com/rafgittools/gitlfs/LfsManager.kt`, lines 65, 85, 104
**Problem**: These three functions unconditionally return `NotImplementedError` (not just
for empty repoPath). Any UI that allows the user to trigger LFS operations will see an
unhandled error rather than a friendly message.
**Severity**: Low — LFS UI entry points are not yet exposed in the main navigation.

---

## OPEN — BUG-08: HomeScreen / AuthScreen non-null assert on user data

**Files**:
- `ui/screens/home/HomeScreen.kt:61` — `user!!`
- `ui/screens/auth/AuthScreen.kt:91` — `username!!`
**Problem**: Non-null asserts on state that flows from network calls. A race between the
null check and the coroutine delivering data (or a network failure leaving the field null)
will crash with `NullPointerException`.
**Severity**: Low-Medium — only triggered in error/race paths not the happy path.

---

## OPEN — BUG-09: TerminalEmulator GIT_SAFE_COMMANDS check uses `map { split() }`

**File**: `ui/screens/terminal/TerminalViewModel.kt:108`
**Problem**: `GIT_SAFE_COMMANDS.map { it.split(" ").first() }` re-creates a list on every
command execution. Should be a `Set` computed once at class init.
**Severity**: Very low — negligible performance impact given the command frequency.
