# Canonical validation exit status — bounded hotfix

Source baseline: `e3e2bc202b9cf7c6678968fc9afdc3acbd879def`.
Scope: `scripts/validate_rafaelia_workflow.sh`, invoked by existing START CI.
Urgency: P1; risk: false acceptance after validator failure.
Data: public source and synthetic process fixtures only. `claim_allowed=false`.

## Observed failure and correction

Five validators ended in `| tee report.json` under POSIX `sh` with `set -eu`.
The pipeline returned the reporter's status. A synthetic validator exiting 23
was followed by later checks and the wrapper exited 0 at all five call sites.
This is a reproduced control-flow failure, not evidence that real invalid
payloads were previously accepted in production.

`run_report` now executes the validator without a pipeline, retains its stdout
in the same report path, displays that report, and returns its recorded status.
Stderr remains on the original CI channel. The subshell isolates bookkeeping
variables. A report-open or display failure is also nonzero. Report output is
displayed after the command completes, rather than streamed live.

All five validators, arguments, report filenames and downstream gates remain.
The new regression test is called from the existing canonical gate. It copies
the actual wrapper into an isolated temporary tree and replaces validator/build
commands with deterministic stubs. This tests the wrapper's process boundary;
it does not prove the correctness of the stubbed validators.

## Verification and limits

| Check | Result / scope |
|---|---|
| Original wrapper + exit-23 injection at five report sites | All five incorrectly returned 0 |
| Patched wrapper + same five injections | Exit 23; failed stdout retained; later validators not invoked |
| Exit codes 1, 64, 126, 127, 143 | Preserved; synthetic statuses, not actual signal delivery |
| Report destination is a directory | Nonzero before validator invocation |
| Successful synthetic commands | Five reports retained and final validator reached |
| POSIX shell syntax | `sh -n` succeeded |
| Canonical gate with real validators | Reached L0 toolchain gate, stopped on missing Clang; full run BLOCKED |
| Existing PIRC2 host C fixtures | 19 fixtures succeeded with GCC |
| Existing PIRC2 adversarial C harness | 7,953 assertions, zero failures; ASan/UBSan with leak detection disabled |
| LeakSanitizer | Unsupported under this execution environment's ptrace; original run failed |
| ARMv7 / AArch64 objects in this session | NOT_RUN: Clang unavailable |
| Exact remote HEAD CI / Android device | TOKEN_VAZIO until separate evidence |

No C source, ABI, toolchain version, Android packaging or inherited Gradle code
is changed by this hotfix. The C tests are adjacent regression evidence only.
The canonical wrapper remains hosted and uses Python, shell and build tools.

## Freestanding boundary and producer routes

| Surface | Source / remaining dependency |
|---|---|
| RafGitTools byte classification | `native/pirc2_token_l0/`; pure C core; host tests and compiler separate |
| RafGitTools graph compilation | `src/rafyml_l0_graph.c`, `scripts/ci/verify_rafyml_l0_multiabi.sh` |
| RafGitTools runtime objects | `src/rafyml_runtime.c`, `scripts/rafyml/verify_multiabi_l0.sh` |
| actions fork local ELF gate | `rafaelmeloreisnovo/actions@86b79a442f2a7c54f28f03058a7f57a066644c7a:freestanding/kernel4/ci_gate.sh` |
| actions inherited product | Gradle Actions fork; inherited rights and hosted implementation retained |
| JNI/NDK/JDK/SDK/Gradle removal from full app | Not implemented; equivalent application behavior needs a separate producer gate |

YAML is a hosted orchestration description, not an executable freestanding C/ASM
module. This patch adds no workflow, installation, package manager or runtime
dependency. Compiler availability alone is not proof of a linked or executable
target: target ABI, entrypoint, memory layout and platform I/O still need gates.
No performance gain or total determinism claim is made.

## Authority, rollback and continuity

Authorized operation: user's requested PR hotfix. Local producer: RafGitTools;
Mapa remains routing authority. Review branch only; no merge/release/settings
change. Rollback: leave the branch unmerged, or reverse only this scoped commit
through a reviewed successor. Baseline commit remains the reconstruction point.

Provider rulesets 26448830 and 26458469 were read live: their listed rules cover
deletion/non-fast-forward, not required reviews/status checks. A draft/title
does not prove enforcement. Do not treat this patch as closure of that P0 gap.

Research providers remain reference catalogs. No paper validates this shell
fix and no operational receipt is uploaded as a scientific paper. Unknown
semantic senses of the user's multiscript tokens remain N5; byte validity
does not establish meaning, translation or provenance.

F_ok: five masked-failure paths corrected and falsified with process fixtures.
F_gap: full local gate blocked by Clang; remote CI, ARM and device evidence absent.
F_next: inspect exact-head CI and independent review before any promotion.
