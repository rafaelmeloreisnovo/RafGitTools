# RafGitTools — Urgency / Gate / Gap Matrix — 2026-09-06

Status: AUDIT / APPEND-ONLY SNAPSHOT
Source revision: `eacfcaaa8b4feb8d42021cc3437192e9ce87ff20`
Claim policy: `claim_allowed=false` until the exact capability is closed by the
source/build/runtime/device evidence appropriate to that capability.

## Invariants

`SOURCE_OBSERVED != WIRED != TEST_PROVEN != BUILD_PROVEN != RUNTIME_PROVEN != DEVICE_PROVEN != RELEASE_PROVEN`

`TOKEN_VAZIO != FAIL != PASS`

`ROADMAP_COUNT != CURRENT_RUNTIME_STATUS`

## Priority scale

- `U0`: blocks trustworthy promotion, release, provider safety or principal runtime claim.
- `U1`: high-value implementation/evidence debt affecting core user capabilities.
- `U2`: maturity/governance/reproducibility debt.
- `U3`: optimization/expansion after correctness and runtime gates.

## Matrix

| ID | Urgency | Surface | Current state | Gap | Gate required | Closure evidence | F_next |
|---|---|---|---|---|---|---|---|
| RGT-U0-01 | U0 | GitHub governance | `UNPROTECTED_OBSERVED` | `main` has no active branch protection in observed provider response | desired-policy dry-run → reversible apply → provider readback | provider JSON + policy fingerprint + rollback capsule + readback receipt | use existing governance transaction machinery; no blind mutation |
| RGT-U0-02 | U0 | exact-head CI | `PREDECESSOR_HEAD_PASS_FINAL_HEAD_REVALIDATION_REQUIRED` | head `b6ee0645...` passed CI, Android Client Build, Security Scan, CodeQL, Internal Validation and PR Validation; this audit delta moves the head | rerun required gates on the new exact head | workflow run/job IDs + head SHA + artifact digest + test/lint summary | preserve predecessor receipts and revalidate the moved head |
| RGT-U0-03 | U0 | Android physical device | `TOKEN_VAZIO` | same-artifact install/launch/restart/recovery proof is still absent from the canonical evidence chain | install and launch exact CI APK on supported physical device; capture ABI/package/logcat and lifecycle receipt | commit + APK SHA-256 + package id + device/ABI + install/launch result + logcat digest | close one reference ARM device without rebuilding the APK |
| RGT-U0-04 | U0 | release | `BLOCKED_BY_EVIDENCE` | release cannot be inferred from source/build alone; signing + device + release checks need one bound chain | signed release build + verification + physical smoke + provenance | signing certificate digest, APK/AAB digest, build receipt, physical smoke receipt | keep `release_allowed=false` until all inputs are exact-artifact bound |
| RGT-U0-05 | U0 | security CI | `PREDECESSOR_HEAD_PASS_FINAL_HEAD_REVALIDATION_REQUIRED` | Security Scan and CodeQL succeeded on `b6ee0645...`; a new audit delta requires exact-head revalidation | execute security workflows on the new head and preserve step-level evidence | security workflow run, step logs/digests and categorized receipt | do not infer device/runtime security from CI success |
| RGT-U1-01 | U1 | FNEXT8 validator | `PR_EXECUTED_NOT_MAIN` | PR #412 has executed 8/8 tests and 7/7 accepted receipts, but the capability is not part of observed `main` revision | review/final-head CI then merge without claim promotion | PR head, CI run, merge SHA and post-merge exact-head revalidation | preserve all higher-order `TOKEN_VAZIO` after structural validation |
| RGT-U1-02 | U1 | Git/remote fixtures | `IMPLEMENTED_ADVANCED_RUNTIME_GATED` | remote conflict/rebase/force-with-lease/worktree/bisect behavior needs controlled real repositories/filesystems | deterministic local + remote fixture matrix | fixture repos/hashes, command transcript, expected/actual state receipts | prioritize destructive/recovery-sensitive Git operations |
| RGT-U1-03 | U1 | authentication | `PARTIAL_RUNTIME_GATED` | OAuth Client ID, real PAT lifecycle, SSH agent/server matrix, gh import and GPG runtime are not fully proven | authorized fixture/account tests with secrets excluded from receipts | redacted config fingerprint + provider response class + runtime receipt | separate auth correctness from credential material |
| RGT-U1-04 | U1 | multi-provider | `IMPLEMENTED_FIXTURE_GATED` | GitLab/Bitbucket/Gitea-Forgejo/Azure adapters need provider-real E2E fixtures | provider-by-provider CRUD/read-only test contract appropriate to permission level | provider/version, request class, response digest, rollback/cleanup receipt | never infer five-provider parity from shared interfaces |
| RGT-U1-05 | U1 | offline/recovery | `IMPLEMENTED_DEVICE_GATED` | queue persistence and worker recovery need process-death/network-loss/device proof | restart/process-death/network transition harness | DB state hashes + worker transitions + recovery receipt | test idempotency and no duplicate mutation first |
| RGT-U1-06 | U1 | terminal | `BOUNDED_EXECUTOR` | current executor is not PTY/VT100-compatible terminal emulation | wire PTY backend with lifecycle, resize, signals and escape-sequence tests | PTY integration tests + device receipt | keep bounded executor classification until this gate closes |
| RGT-U1-07 | U1 | LLaMA JNI | `BLOCKED_EXTERNAL` | external headers/model/runtime are not pinned/proven | pin source/version/build flags/model fixture then execute JNI smoke | dependency SHAs + library hash + model hash + JNI/device receipt | no AI capability claim from bridge presence alone |
| RGT-U1-08 | U1 | licensing metadata | `DIVERGENT` | README declares GPL-3.0 while GitHub repository metadata reports an unrecognized/Other license | reconcile root LICENSE, SPDX detection and attribution compatibility | root LICENSE hash + dependency/source attribution inventory + provider metadata readback | preserve upstream license obligations distinctly |
| RGT-U2-01 | U2 | test coverage | `EXACT_HEAD_WORKFLOW_EXECUTED_DENOMINATOR_REVIEW_OPEN` | Code Coverage succeeded on `b6ee0645...`, but the denominator and branch/line/function semantics still require canonical binding | persist exact coverage report and denominator manifest | coverage report + source/test file manifest hashes | track branch/line/function separately; avoid single opaque percent |
| RGT-U2-02 | U2 | documentation/source drift | `ACTIVE_RISK` | roadmap/status documents can lag rapidly moving source | regenerate code→docs/status map from exact head and flag stale claims | generated map + diff + review queue | source and executed evidence outrank roadmap prose |
| RGT-U2-03 | U2 | cross-repo receipts | `VALIDATOR_AVAILABLE_IN_PR` | receipts can be structurally valid while physical/scientific gates remain open | enforce validator at federation boundaries and preserve closure ownership | accepted/rejected bundle + closure IDs + no-promotion flag | use structural validity only for routing, not proof promotion |
| RGT-U3-01 | U3 | performance/UX | `CI_BUILD_METRICS_EXECUTED_DEVICE_BENCHMARK_OPEN` | predecessor CI built the release APK, measured 4,270,203 bytes and completed build-time/method-analysis jobs; controlled physical-device/repo-size percentiles remain open | pinned device, thermal state, repo fixtures and percentile methodology | raw traces + environment + p50/p95/p99 and variance | keep CI build metrics distinct from controlled device benchmarking |

## Executed predecessor evidence

The branch head `b6ee0645d2117b4cba6fd1adfd7725cc5b74d140` produced these observed
workflow results before the present corrective delta:

- CI `34029265867`: `success`.
- Android Client Build `34029265871`: `success`.
- Security Scan `34029265863`: `success`.
- CodeQL Advanced `34029265883`: `success`.
- Internal Validation `34029265848`: `success`.
- PR Validation `34029265845`: `success`.
- Source Gap Audit `34029265857`: `success`.
- Code Coverage `34029265870`: `success`.
- Performance Metrics `34029265847`: measurements succeeded; PR-comment permission failed.
- Documentation `34029265868`: failed only on four MD013 findings in this new document.

The last two failures are mechanical gates being corrected in this PR. They do not erase the
successful build/measurement steps, and they are not promoted to final-head PASS until rerun.

## Closure order

`RGT-U0-02 → RGT-U0-05 → RGT-U0-03 → RGT-U0-04 → RGT-U0-01 → RGT-U1-* → RGT-U2-* → RGT-U3-*`

Provider-governance mutation remains after exact-head CI/security observation unless a separate
security requirement forces earlier enforcement. Any mutation must remain reversible and have
provider readback.

## R3

- `F_ok`: advanced Android/Git source, exact predecessor CI/build/security receipts, dual ABI
  artifact history, governance dry-run/rollback machinery, and FNEXT8 validation evidence.
- `F_gap`: final-head revalidation after this corrective delta, physical-device chain, release
  provenance, real provider/auth/Git fixtures, PTY, external LLaMA runtime, and license metadata.
- `F_next`: rerun the corrected head, then close physical evidence before expanding feature count
  or promoting claims.
