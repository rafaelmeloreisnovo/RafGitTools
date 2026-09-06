# Mapa código → documentação

**Observed base:** `main@56f4ce95158e6b8a1dbfa4fd8c029937aea20224`  
**Updated:** 2026-09-06  
**Role:** semantic routing, not exhaustive tree inventory.

```text
file present != capability proven != runtime proven
TOKEN_VAZIO != FAIL != PASS
```

For recursive structural discovery use the repository-view/index tooling already documented under `docs/navigation/`. This file answers **which active documentation owns the meaning/status of a source surface**.

## Git core

| Source surface | Documentation route | Current bounded state |
|---|---|---|
| `app/src/main/kotlin/com/rafgittools/data/git/JGitService.kt` | `ARCHITECTURE.md`, `STATUS_REPORT.md`, `RAFGITTOOLS_GIT_OPERATIONS_MATRIX.md` | `SOURCE_OBSERVED_ADVANCED` |
| Git clone/status/rebase/stash/tag/use-case surfaces | `RAFGITTOOLS_GIT_OPERATIONS_MATRIX.md`, `STATUS_REPORT.md` | source present; fixture depth varies |
| worktree/bisect surfaces | `STATUS_REPORT.md`, `RAFGITTOOLS_ROADMAP_TRUE.md` | source present; runtime/fixture evidence remains separate |
| interactive staging | `STATUS_REPORT.md`, test plan | source present; physical smoke remains gated |

## GitHub API and providers

| Source surface | Documentation route | Current bounded state |
|---|---|---|
| `data/github/GithubApiService.kt` and repository layer | `RAFGITTOOLS_GITHUB_API_MATRIX.md`, `STATUS_REPORT.md` | `SOURCE_OBSERVED_ADVANCED` |
| `platform/MultiPlatformManager.kt` and adapters | `STATUS_REPORT.md`, roadmap | adapters present; provider-real parity not inferred |

## Repository Governance — current primary route

| Source surface | Documentation route | State |
|---|---|---|
| `data/github/RepositoryGovernanceApiService.kt` | `RAFGITTOOLS_CURRENT_STATE.md`, `STATUS_REPORT.md` | provider API surface present |
| `data/github/RepositoryGovernanceAudit.kt` | current state/status | deterministic audit model present |
| `data/github/RepositoryGovernanceReceiptStore.kt` | current state/status | local append-only SHA-256 receipt chain present |
| `ui/screens/settings/RepositoryGovernanceMutationPlan.kt` | current state/roadmap | pre-image + reversible mutation + drift gate present |
| `ui/screens/settings/RepositoryGovernanceViewModel.kt` | current state/status | governed UI state path present |
| `ui/screens/settings/RepositoryGovernanceScreen.kt` | current state/manual/navigation docs | UI surface present |
| corresponding governance tests | test plan/status | test source present; execution must remain revision-bound |

### Governance source invariants observed

```text
unknown provider pre-state -> BLOCKED_TOKEN_VAZIO
lossless rollback not proven -> BLOCKED_NON_REVERSIBLE
provider changed after apply -> BLOCKED_DRIFT
receipt recorded -> not equal to provider acceptance
```

## Cross-repository receipt validation

| Source surface | Documentation route | State |
|---|---|---|
| `scripts/rafaelia_receipt_validator.py` | `STATUS_REPORT.md`, `RAFGITTOOLS_ROADMAP_TRUE.md`, documentation audit receipt | fail-closed structural validator present |

The validator recognizes evidence levels from `TOKEN_VAZIO` through `INDEPENDENTLY_REPRODUCED`, limits promotion by event type and forbids generic `claim_allowed=true` while unresolved `token_vazio` exists. Structural receipt validity is not runtime/device/scientific proof.

## Authentication and security

| Source surface | Documentation route | State |
|---|---|---|
| auth/token lifecycle paths | `RAFGITTOOLS_SECURITY_AUTH_MAP.md`, `TERMUX_AUTH.md`, `STATUS_REPORT.md` | source implemented; real fixture/device evidence granular |
| SSH/key paths | security/auth docs + roadmap | source present; agent/server matrix runtime-gated |
| GPG path | status/roadmap | adapter/source present; runtime fixture not assumed |
| CI/security workflow surfaces | status + urgency snapshot | workflow evidence must be bound to exact SHA/run |

## Offline and recovery

| Source surface | Documentation route | State |
|---|---|---|
| offline queue/storage/workers | architecture/status/roadmap | source present; process-death/network/device recovery receipt open |

## UI and navigation

| Source surface | Documentation route | State |
|---|---|---|
| core screens | `RAFGITTOOLS_UI_NAVIGATION_MAP.md`, manuals | source varies by surface |
| Repository Governance screen | navigation/manual + current state | newly routed in current documentation |
| terminal screen/emulator | `RAFGITTOOLS_TERMINAL_STRATEGY.md`, status | `BOUNDED_EXECUTOR`, not PTY/VT100 |

## Native/JNI/external runtime

| Source surface | Documentation route | State |
|---|---|---|
| `app/src/main/cpp/` / RAFAELIA JNI | native/ARM32 docs + status | source/bridge present; device invocation evidence separate |
| LLaMA/local-model bridge | roadmap/status | external dependency/model/runtime gated |
| LFS | status/roadmap | external tool/remote fixture gated |

## Documentation-state routing

```text
current mutable state:
  RAFGITTOOLS_CURRENT_STATE.md
  STATUS_REPORT.md
  RAFGITTOOLS_ROADMAP_TRUE.md

semantic routing:
  CODE_TO_DOC_MAP.md
  INDEX.md

append-only/current-day audit snapshot:
  URGENCY_GATE_GAP_20260906.md

historical build evidence:
  canonical/2026-08-14/*

stale machine-state checkpoint:
  ../ECOSYSTEM_RUNTIME_STATE.json
  -> TOKEN_VAZIO_REGEN_REQUIRED for current-main regeneration
```

## Criterion for labels

- `SOURCE_OBSERVED`: source exists at exact audited revision.
- `TEST_PROVEN`: relevant tests executed at a bound revision.
- `BUILD_PROVEN`: build artifact tied to exact revision/hash.
- `RUNTIME_PROVEN`: runtime execution proven in declared environment.
- `DEVICE_PROVEN`: exact artifact proven on physical target.
- `TOKEN_VAZIO`: evidence is absent/insufficient.

Do not use `implemented`, `complete`, percentages or feature counts as evidence shortcuts without a declared denominator and revision.

## R3

- **F_ok:** code→docs routing now includes Repository Governance and FNEXT receipt validation and removes obsolete GPG-as-stub wording.
- **F_gap:** a recursive current-head semantic inventory/count remains `TOKEN_VAZIO_RECOUNT_REQUIRED` until the repository tooling is run against the exact revision.
- **F_next:** every new source domain must enter this map or explicitly carry `TOKEN_VAZIO_DOC_ROUTE`.