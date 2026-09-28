# RafGitTools

**State:** `ACTIVE / SOURCE_ADVANCED / EVIDENCE_GATED`  
**Observed documentation base for this reconciliation:** `main@8af97a580e535d2015e8211000850e282b031763`  
**Documentation cut:** 2026-09-28

> **License authority notice — 2026-09-25:** the root `LICENSE` is the GPLv3-or-later project license surface. The root `LICENSE.md` is an observed C/source artifact with symbolic/legal commentary and must not be treated as a replacement license. See [License Authority Reconciliation](docs/legal/LICENSE_AUTHORITY_RECONCILIATION_20260925.md).

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
4. [`docs/RAFGITTOOLS_DEVELOPMENT_DELIVERY_MAP_V1.md`](docs/RAFGITTOOLS_DEVELOPMENT_DELIVERY_MAP_V1.md)
5. [`docs/architecture/RAFGITTOOLS_DRIVE_GITHUB_DELIVERY_ARCHITECTURE_V1.md`](docs/architecture/RAFGITTOOLS_DRIVE_GITHUB_DELIVERY_ARCHITECTURE_V1.md)
6. [`docs/RESPONSIVE_LAYOUT_GATE_V1.md`](docs/RESPONSIVE_LAYOUT_GATE_V1.md)
7. [`docs/RELEASE_NOTES_NEXT.md`](docs/RELEASE_NOTES_NEXT.md)
8. [`docs/CODE_TO_DOC_MAP.md`](docs/CODE_TO_DOC_MAP.md)
9. [`docs/INDEX.md`](docs/INDEX.md)
10. [`docs/audit/SESSION_RECONCILIATION_RAFANDROID_SILICON_LIGHT_20260928.md`](docs/audit/SESSION_RECONCILIATION_RAFANDROID_SILICON_LIGHT_20260928.md) — RAFANDROID/Silicon Light session ledger, complications and F_gap/F_next
11. [`docs/URGENCY_GATE_GAP_20260906.md`](docs/URGENCY_GATE_GAP_20260906.md) — append-only historical audit snapshot

Historical receipts and canonical checkpoints remain valid only for their exact revisions/artifacts.

## 2026-09-28 RAFANDROID + Silicon Light reconciliation

Two low-level/toolchain domains are now part of the merged main lineage with revision-bound canonical CI evidence:

- **RAFANDROID V1** — PR #521, exact head `62925c8...`, START #241 / run `36360576045` = SUCCESS, merge `f0c192d9...`.
- **Silicon Light L0 V1** — PR #526, exact head `40d90c9...`, START #263 / run `36385528436` = SUCCESS, merge `0fdfc8a0...`.

RAFANDROID is the single local toolchain-control shell for discovery/orchestration/gates around Gradle/JDK/SDK/NDK/AAPT2/AIDL/D8/R8/JNI/DEX/APK/ADB/emulator/QEMU discovery. The external tools remain external authorities.

Silicon Light is the L0 freestanding substrate. Its declared core boundary has no system headers, heap, syscall, JNI/Android API or external undefined runtime symbols. The final #526 CI also generated a fresh minimal scaffold, compiled the L0 behind JNI and produced/verified an APK.

```text
L0 Silicon Light -> L1 JNI adapter -> L2 Android shell -> L3 Gradle/CI/device
```

Only L0 has the stated runtime-independence contract. Physical Android and QEMU guest execution remain separate evidence gates.

## Historical 2026-09-18 delivery reconciliation

The source is ahead of the 2026-09-06 documentation cut. This candidate reconciles the gap without promoting evidence:

- the Home/source dashboard now consumes the responsive layout contract instead of merely importing unused helpers;
- Drive/SAF staging now re-reads the promoted private copy, verifies byte count and SHA-256, and emits a local receipt;
- GitHub recipient repository/ref/path remains explicit `TOKEN_VAZIO` until a human binds a destination;
- the downstream Git mutation route is the existing RafGitFS governed workspace → branch → commit → push → draft PR path;
- `.github/workflows/START.yml` validates the development/delivery map in the documentation lane;
- exact-head CI, physical-device acceptance and signed release remain evidence gates.

The active development/delivery control surface is `configs/rafgittools-delivery-map.v1.json`; historical feature-count documents do not override it.

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
- RAFANDROID Android-toolchain control shell and minimal JNI scaffold;
- Silicon Light freestanding L0 with ARMv7/AArch64 CI gates;
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

### Prior 2026-09-06 boundary (historical)

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

The Android project currently uses the repository Gradle Wrapper 9.5.1, AGP 8.13.2 and JDK 17 as the documented baseline. Build variants and signing details are documented in [`docs/BUILD.md`](docs/BUILD.md). Security/authentication boundaries are documented in the security/auth documents routed from [`docs/INDEX.md`](docs/INDEX.md).

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

- **F_ok:** README routes current evidence and now includes merged RAFANDROID/Silicon Light revision-bound CI anchors without promoting device/runtime claims.
- **F_gap:** complete current-main execution inventory, physical Android, QEMU/VM runtime, machine-state regeneration, low-level equivalence migration and release remain open.
- **F_next:** use RAFANDROID/Silicon Light as regression anchors; migrate low-level primitives only after equivalence/property evidence and close higher gates through exact revision/artifact/device receipts.