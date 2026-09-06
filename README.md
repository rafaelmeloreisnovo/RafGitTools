# RafGitTools

**State:** `ACTIVE / SOURCE_ADVANCED / EVIDENCE_GATED`  
**Observed documentation base:** `main@56f4ce95158e6b8a1dbfa4fd8c029937aea20224`  
**Documentation cut:** 2026-09-06

RafGitTools is an Android Git/GitHub client and governance-oriented engineering workspace built around Kotlin, Jetpack Compose, Hilt, Room, JGit, provider APIs and native/JNI integrations.

The repository uses a strict evidence boundary:

```text
SOURCE_OBSERVED != TEST_PROVEN != BUILD_PROVEN
                != RUNTIME_PROVEN != DEVICE_PROVEN != RELEASE_PROVEN
TOKEN_VAZIO != FAIL != PASS
```

## Start here

For current technical truth, read in this order:

1. [`docs/RAFGITTOOLS_CURRENT_STATE.md`](docs/RAFGITTOOLS_CURRENT_STATE.md)
2. [`docs/STATUS_REPORT.md`](docs/STATUS_REPORT.md)
3. [`docs/RAFGITTOOLS_ROADMAP_TRUE.md`](docs/RAFGITTOOLS_ROADMAP_TRUE.md)
4. [`docs/CODE_TO_DOC_MAP.md`](docs/CODE_TO_DOC_MAP.md)
5. [`docs/INDEX.md`](docs/INDEX.md)
6. [`docs/URGENCY_GATE_GAP_20260906.md`](docs/URGENCY_GATE_GAP_20260906.md) — append-only audit snapshot

Historical receipts and canonical checkpoints remain valid only for their exact revisions/artifacts.

## Current source capabilities

At the observed revision, the source tree contains substantial implementations for:

- Android UI/application architecture with Kotlin, Compose, Hilt and Room;
- local Git operations through JGit;
- GitHub API integration;
- provider adapters including GitLab, Bitbucket, Gitea/Forgejo and Azure DevOps;
- PAT/OAuth/SSH/gh-oriented authentication surfaces;
- offline queue/recovery infrastructure;
- Git LFS/worktree/bisect/GPG-related surfaces at different runtime maturity levels;
- native/JNI RAFAELIA bridges;
- bounded terminal execution;
- repository configuration/security governance;
- fail-closed cross-repository receipt validation.

Source presence is not automatically a runtime or device claim.

## Repository Governance

Current source includes a provider-bound governance path with:

```text
RepositoryGovernanceApiService
RepositoryGovernanceAuditor
RepositoryGovernanceReceiptStore
RepositoryGovernanceMutationPlanner
RepositoryGovernanceViewModel
RepositoryGovernanceScreen
```

The mutation plan requires a known provider pre-state and a losslessly reversible path. It can block on:

```text
BLOCKED_TOKEN_VAZIO
BLOCKED_NON_REVERSIBLE
BLOCKED_DRIFT
```

Local V2 governance receipts are append-only and SHA-256 chained. A local receipt records an observation/attempt; provider acceptance and authoritative re-probe remain separate evidence.

## Cross-repository receipts

`scripts/rafaelia_receipt_validator.py` implements a stdlib-only, fail-closed structural validator for RAFAELIA/FNEXT receipts. It enforces schema, lineage and evidence-level promotion boundaries.

A structurally valid receipt does **not** by itself prove physical runtime, device execution, mathematical truth, scientific novelty or independent reproduction.

## Evidence status

### Historical BUILD checkpoint

The following remains a valid historical checkpoint for its own revision:

```text
commit       = bbdb556a59c06a23cc2f6df6ba0ae7c98466a4fa
workflow run = 31821491676 PASS
APK SHA-256  = 115b9cb1e71f53f16b2648924a09549b8e5e0b9e453280cab2e7f183a411ebf6
ABIs         = armeabi-v7a + arm64-v8a observed
DEVICE       = TOKEN_VAZIO_PHYSICAL_DEVICE_REQUIRED
```

It is not a current-main build receipt.

### Current-main boundary

The 2026-09-06 main lineage includes recent governance, receipt-validator, compile/Hilt and security/workflow fixes. One workflow was directly observed on exact current main during this documentation audit: Human Impact Cross-Repo Gate V1 run `34031951218` completed successfully.

A complete exact-current-head CI/build/security inventory remains individually evidence-bound and must not be inferred from predecessor runs.

## Open gates

| Gate | State |
|---|---|
| Exact-current-head complete CI/build inventory | `TOKEN_VAZIO` until each required run is bound |
| Physical Android install/launch | `TOKEN_VAZIO_PHYSICAL_DEVICE_REQUIRED` |
| Real Git/Auth/provider/offline fixture matrix | `TOKEN_VAZIO_RUNTIME` where not receipt-bound |
| PTY/VT100 terminal | `TOKEN_VAZIO_PTY` |
| External LLaMA/model runtime | `TOKEN_VAZIO_RUNTIME` |
| Signed release provenance | `TOKEN_VAZIO_RELEASE` |
| `claim_allowed` | `false` |
| `release_allowed` | `false` |

## Historical metrics

The old `288 total / 130 complete / 35 in progress / 123 pending` numbers are retained only as a planning baseline in historical material. They are not the current runtime percentage.

Current exact counts remain:

```text
feature denominator = TOKEN_VAZIO_RECOUNT_REQUIRED
Kotlin files        = TOKEN_VAZIO_RECOUNT_REQUIRED
test files          = TOKEN_VAZIO_RECOUNT_REQUIRED
document files      = TOKEN_VAZIO_RECOUNT_REQUIRED
```

## Build and development

The Android project uses Gradle and JDK 17-compatible tooling. Build variants and signing details are documented in [`docs/BUILD.md`](docs/BUILD.md). Security/authentication boundaries are documented in the security/auth documents routed from [`docs/INDEX.md`](docs/INDEX.md).

Do not treat an unsigned/internal artifact as an official release.

## Documentation governance

Current mutable state is maintained in the current-state/status/roadmap triad. Historical evidence stays immutable. Machine-readable state that is stale is not manually rewritten merely to match prose.

`ECOSYSTEM_RUNTIME_STATE.json` was observed carrying a 2026-08-14 state. This docs-only audit therefore classifies current regeneration as:

```text
TOKEN_VAZIO_REGEN_REQUIRED
```

until its proper generator/state process is executed against current main.

The documentation audit receipt is [`docs/DOCUMENTATION_AUDIT_RECEIPT_2026-09-06.md`](docs/DOCUMENTATION_AUDIT_RECEIPT_2026-09-06.md).

## Attribution and license

The repository contains root license/attribution material and references/inspirations from other Git/GitHub tooling. Exact reuse obligations belong to the relevant `LICENSE`, `LICENSES/`, `THIRD_PARTY_LICENSES.md` and documentation records.

Public repository visibility or README prose must not be used to infer rights beyond the applicable license files and provenance records.

## Contribution rule

For any material source change, update the relevant documentation route or explicitly record `TOKEN_VAZIO_DOC_DRIFT`. Do not promote source presence into test/build/runtime/device/release evidence.

## R3

- **F_ok:** README is now a compact current router instead of mixing historical feature percentages with present evidence.
- **F_gap:** complete current-head execution inventory, physical device, machine-state regeneration and release remain open.
- **F_next:** follow `docs/INDEX.md` and close gates only through exact revision/artifact receipts.