# RAFGITTOOLS_CURRENT_STATE

- Status: **ACTIVE — source advanced / current documentation reconciled against main**
- Observed repository: `rafaelmeloreisnovo/RafGitTools`
- Observed base revision: `56f4ce95158e6b8a1dbfa4fd8c029937aea20224`
- Observed date: **2026-09-06**
- Documentation audit scope: **docs-only**
- `claim_allowed=false`
- `release_allowed=false`

## Canonical evidence rule

```text
SOURCE_OBSERVED != TEST_PROVEN != BUILD_PROVEN != RUNTIME_PROVEN
                != DEVICE_PROVEN != RELEASE_PROVEN
TOKEN_VAZIO != FAIL != PASS
HISTORICAL_RECEIPT != CURRENT_HEAD_RECEIPT
```

This document describes the current source/documentation relationship. Historical receipts remain valid only for the exact revisions and artifacts to which they were originally bound.

## Current source observed on main

The current source tree contains, among other already-integrated surfaces:

- Android application architecture using Kotlin, Compose, Hilt and Room;
- local Git/JGit services and advanced Git flows;
- GitHub API integration and multiple provider adapters;
- authentication, offline/recovery infrastructure and native/JNI surfaces;
- repository-governance UI, provider readback/audit models and governed mutation planning;
- governance dry-run and rollback planning that fail closed on unknown pre-state, lossy rollback or provider drift;
- local append-only repository-governance receipts with a SHA-256 chain;
- the fail-closed FNEXT cross-repository receipt validator;
- workflow/security hardening merged through the current main lineage.

For repository governance specifically, the source at the observed revision includes:

```text
RepositoryGovernanceApiService
RepositoryGovernanceAudit / RepositoryGovernanceAuditor
RepositoryGovernanceReceiptStore
RepositoryGovernanceMutationPlanner
RepositoryGovernanceViewModel
RepositoryGovernanceScreen
```

The mutation planner requires a proven provider pre-image and reversible path before a write is executable. It blocks `TOKEN_VAZIO`, non-reversible state and provider drift. The receipt store records local V2 envelopes in an append-only SHA-256 chain and explicitly separates a recorded attempt from authoritative provider acceptance/re-probe.

## Evidence currently safe to state

### Historical build anchor

The 2026-08-14 build remains a valid **historical commit-bound checkpoint**:

- commit `bbdb556a59c06a23cc2f6df6ba0ae7c98466a4fa`;
- Android Client Build run `31821491676` — PASS;
- unit tests/lint/`assembleDevDebug` — PASS for that revision;
- APK SHA-256 `115b9cb1e71f53f16b2648924a09549b8e5e0b9e453280cab2e7f183a411ebf6`;
- `armeabi-v7a` and `arm64-v8a` observed in that artifact.

It is **not** a build receipt for current `main`.

### 2026-09-06 lineage

Recent merged work includes the repository-governance transaction path, compile/Hilt repairs, FNEXT8 validator integration, security/SARIF corrections, urgency/gate/gap reconciliation and the TruffleHog event-range correction. These merges establish **source presence on main**, not automatic device/release proof.

One current-main workflow run was directly observed for revision `56f4ce95158e6b8a1dbfa4fd8c029937aea20224`: Human Impact Cross-Repo Gate V1 run `34031951218` completed with `success`. Other final-head workflow states must be read from provider metadata before being credited individually.

FNEXT8 predecessor execution evidence remains bounded to its executed revision/run: 8/8 unit tests passed, seven receipts were accepted, zero rejected, and all retained their higher-order `TOKEN_VAZIO` boundaries. Structural receipt validity does not establish physical/scientific/runtime claims.

## Current capability classification

| Surface | Current source state | Evidence boundary |
|---|---|---|
| Android/Compose/Hilt/Room | `SOURCE_OBSERVED_ADVANCED` | exact-current-head build/device evidence must be independently bound |
| Git/JGit | `SOURCE_OBSERVED_ADVANCED` | real remote/destructive/recovery fixtures remain granular |
| GitHub API | `SOURCE_OBSERVED_ADVANCED` | provider-real E2E matrix remains granular |
| Multi-provider | `SOURCE_OBSERVED` | provider parity is not inferred from shared interfaces |
| Auth lifecycle | `SOURCE_OBSERVED` | real disposable credential/device paths remain runtime-gated |
| Offline/recovery | `SOURCE_OBSERVED` | process-death/network/device recovery receipt remains open |
| Repository Governance | `SOURCE_OBSERVED_ADVANCED` | provider mutation/readback must remain authority/pre-image/rollback bound |
| Governance receipts | `SOURCE_OBSERVED` | local chain validity != provider acceptance |
| FNEXT receipt validator | `SOURCE_OBSERVED + predecessor execution evidence` | generic receipt validation != runtime/device/scientific proof |
| Terminal | `BOUNDED_EXECUTOR` | PTY/VT100 remains separate capability |
| LFS/worktree/bisect/GPG | `SOURCE_PRESENT / RUNTIME_GATED` | external/runtime fixtures remain open |
| Local LLM/LLaMA bridge | `SOURCE_PRESENT / EXTERNAL_RUNTIME_GATED` | dependency/model/device evidence remains open |
| Physical Android runtime | `TOKEN_VAZIO_PHYSICAL_DEVICE_REQUIRED` | exact-artifact install/launch/recovery receipt required |
| Release | `BLOCKED_BY_EVIDENCE` | signing + exact artifact + physical acceptance required |

## Superseded current-state narrative

The former active framing around PR #346/#347 and branch `hardening/first-compile-run-triangle-20260814` is now **historical genealogy**, not current operational state. Preserve the 2026-08-14 canonical/evidence files unchanged; do not use them as current-head receipts.

`ECOSYSTEM_RUNTIME_STATE.json` still carries an older 2026-08-14 mutable-state snapshot. Because this documentation audit is docs-only, that JSON is not rewritten here. Until a machine-state regeneration is executed against current main, treat it as:

```text
ECOSYSTEM_RUNTIME_STATE.json = HISTORICAL_MACHINE_STATE / REGEN_REQUIRED
current machine-state regeneration = TOKEN_VAZIO_REGEN_REQUIRED
```

## Current open gates

```text
exact-current-head complete CI/build receipt = TOKEN_VAZIO until individually provider-bound
physical install/launch/restart receipt       = TOKEN_VAZIO_PHYSICAL_DEVICE_REQUIRED
real Git/provider/auth fixture matrix         = TOKEN_VAZIO_RUNTIME
PTY/VT100                                      = TOKEN_VAZIO_PTY
external LLaMA/model runtime                  = TOKEN_VAZIO_RUNTIME
signed release provenance                    = TOKEN_VAZIO_RELEASE
provider governance enforcement              = TOKEN_VAZIO until authoritative readback proves it
claim_allowed                                 = false
release_allowed                               = false
```

## Source-of-truth order

1. exact Git commit and provider metadata;
2. source/build/test/workflow files at that exact revision;
3. execution artifacts and receipts bound to the same revision;
4. physical/runtime receipts bound to the exact artifact;
5. `docs/RAFGITTOOLS_CURRENT_STATE.md`;
6. `docs/STATUS_REPORT.md`;
7. `docs/RAFGITTOOLS_ROADMAP_TRUE.md`;
8. `docs/URGENCY_GATE_GAP_20260906.md` as append-only snapshot;
9. older roadmaps/status/canonical checkpoints as historical context.

Machine-readable files that are older than the current revision remain useful historical evidence but cannot outrank current source/provider evidence merely because they are structured data.

## Next operational documentation gate

Keep documentation synchronized from exact source and evidence boundaries. Any future source merge that changes user-visible capability, evidence state, build/runtime gate or release boundary must update the current-state/status/roadmap triad in the same review cycle or explicitly emit `TOKEN_VAZIO_DOC_DRIFT`.

## R3

- **F_ok:** current main source was re-observed; obsolete PR #346/#347 framing is retired from the active state; governance/FNEXT8 surfaces are routed; historical receipts are preserved.
- **F_gap:** exact-current-head full workflow/build inventory, machine-state regeneration, physical device, provider-real fixtures and release remain evidence-gated.
- **F_next:** require revision-bound evidence for each promoted state and keep generated/machine state separate from editorial documentation.