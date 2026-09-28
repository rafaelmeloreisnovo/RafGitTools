# Session Reconciliation — RAFANDROID + Silicon Light — 2026-09-28

Status: `DOCUMENTATION_RECONCILED_CANDIDATE`  
Observed main at branch creation: `8af97a580e535d2015e8211000850e282b031763`  
Scope: this isolated conversation session from the first RAFANDROID request through Silicon Light L0 and the later evidence readback.  
`claim_allowed=false`.

## Invariants

```text
SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM
TOKEN_VAZIO != 0
IMPLEMENTED_UNTESTED != PASS
historical receipt != successor receipt
CI/build PASS != physical-device PASS
tool present != tool integrated != tool executed
```

## 1. Session chronology

| Delta | Request / observation | Work performed | Terminal evidence |
|---|---|---|---|
| S01 | Create one authorial shell for Gradle/JNI/AndroidX/QEMU/NDK/R8/SDK/DEX and related Android tooling with lower friction/shadows/tails | Chose RafGitTools as local control-plane; created `tools/rafandroid/`, one entrypoint, lock contract, minimal JNI scaffold, method docs and gates | PR #521 |
| S02 | Avoid implicit external behavior | Added no-implicit-network, no silent SDK bootstrap, no automatic license acceptance, wrapper-before-system-Gradle and fail-closed discovery | START #241 PASS |
| S03 | First CI topology attempt | A separate RAFANDROID workflow was initially added | Falsified by RafGitTools single-root invariant |
| S04 | Repair topology | Removed the extra workflow and integrated RAFANDROID gates into canonical `START.yml` | topology gate PASS in START #241 |
| S05 | Harden command execution | Removed `eval`; made signature verification fail the APK gate when `apksigner` exists; constrained V1 JNI package naming | START #241 PASS |
| S06 | Expand tool surface | Added AIDL, dexdump, APK Analyzer, lint, keytool/jarsigner and LLVM inspection/strip/archive tools | source + CI PASS |
| S07 | Make friction/shadows/tails observable | Added `shadow`, `tails`, `friction`; deduplicated SDK roots; kept synthetic numeric friction score as `TOKEN_VAZIO_UNCALIBRATED` | self-test PASS |
| S08 | Bind native checks to selected NDK | `native-gate` and Android lane use the selected NDK compiler and verify ARMv7/AArch64 plus real DEX/APK | START #241 PASS |
| S09 | Apply a deeper low-level architecture | Reused existing freestanding lineage instead of duplicating it; introduced generic `native/silicon_light_v1` L0 | PR #526 |
| S10 | Remove C portability shadows | Removed unrelated-pointer ordering and implementation-dependent negative signed shift from L0 Q16 logic; extended tests | START #263 PASS |
| S11 | Prove L0→JNI→APK boundary | Added `rafandroid silicon-gate`; embedded canonical L0 in scaffold; generated and built an independent minimal Android fixture; ran DEX/APK gates | START #263 PASS |
| S12 | Re-read terminal state | Confirmed PR #521 and #526 merged and their exact-head canonical START runs completed successfully | #521 merge `f0c192d...`; #526 merge `0fdfc8a...` |

## 2. Exact execution anchors

### RAFANDROID V1

- PR: **#521**
- exact PR head: `62925c8a573f1860a9c278567c1716e153d580f1`
- canonical START run: **36360576045 / #241**
- conclusion: **SUCCESS**
- merge commit: `f0c192d9c56b2cc794d1ed18e361260a3478f598`

Observed successful stages include topology, coherence, RAFANDROID shell gate, Python tests, docs gate, Android unit/instrumentation compile/lint/assemble/APK verification, CodeQL Actions, CodeQL Java/Kotlin and final receipt.

### Silicon Light L0

- PR: **#526**
- exact PR head: `40d90c93bf7bfe6d3299a89585688c0324d25245`
- canonical START run: **36385528436 / #263**
- conclusion: **SUCCESS**
- merge commit: `0fdfc8a01e9a513facc3873d9002b3bac79af8ae`

Observed successful stages include:

- RAFANDROID gate;
- Silicon Light host freestanding gate;
- NDK-bound ARMv7 + AArch64 gate;
- generated minimal Silicon Light APK fixture;
- unit tests;
- Android instrumentation APK compile;
- Android lint;
- devDebug assembly;
- APK/SHA verification;
- CodeQL Actions;
- CodeQL Java/Kotlin;
- final append-only START receipt.

## 3. Complication ledger

| Complication | Initial risk | Resolution | Current state |
|---|---|---|---|
| Separate RAFANDROID workflow violated single-root `START.yml` architecture | duplicate CI authority / shadow | workflow removed; gate moved into START | CLOSED |
| `eval` in Gradle wrapper path | command-composition ambiguity | replaced with direct argv execution | CLOSED |
| signature verification could emit a message without hard failure | false packaging success | signature gate returns failure when verifier exists and rejects artifact | CLOSED |
| generic `clang` could outrank selected NDK | toolchain shadow | native/silicon gates bind to selected NDK toolchain | CLOSED |
| same SDK reachable by multiple environment paths | false shadow count | canonical path deduplication | CLOSED |
| “friction” could become an invented score | uncalibrated metric promoted as truth | numeric score remains `TOKEN_VAZIO_UNCALIBRATED` | CLOSED BY BOUNDARY |
| static QEMU discovery could be mistaken for VM proof | claim promotion | QEMU boot/runtime remains separate TOKEN_VAZIO | CLOSED BY BOUNDARY |
| “no external dependencies” could be misread as whole Android APK independence | impossible architecture claim | isolated L0; Android/JNI/Gradle remain upper-layer authorities | CLOSED BY BOUNDARY |
| host compiler emitted relocations in an early L0 prototype | hidden object dependency ambiguity | L0 implementation/flags changed; host relocation gate requires zero | CLOSED |
| unrelated-pointer comparison in C | undefined/unspecified portability shadow | V1 copy contract made non-overlap; relational pointer order removed | CLOSED |
| signed negative right shift | implementation-dependent arithmetic | Q16 multiply rewritten using unsigned magnitude path | CLOSED |
| local prototype and final Git blobs differed | invalid evidence inheritance | exact-blob reconstruction recorded separately; CI exact-head used for promotion | CLOSED |
| dynamic “Code scanning AI findings” returned HTTP 400 unsupported model | could be misclassified as code/security failure | recorded as provider-side scanner infrastructure failure; canonical CodeQL remains separate | EXTERNAL / NON-BLOCKING TO CODE CLAIM |
| repeated branch commits cancelled predecessor START runs | partial evidence confusion | only successor exact-head terminal run is promotion evidence | CLOSED BY CUSTODY |

## 4. F_gap / F_next reconciliation

| ID | Original F_gap | Result in this session | Current F_next |
|---|---|---|---|
| FG-RA-01 | RAFANDROID shell lacked exact-head CI | START #241 SUCCESS | preserve regression gate in canonical START |
| FG-RA-02 | full scaffold APK was not exercised | Silicon Light successor added generated minimal APK fixture; START #263 PASS | keep fixture as regression oracle |
| FG-RA-03 | current app not consuming RAFANDROID gates | canonical Android lane invokes RAFANDROID NDK/DEX/APK gates | expand only when an existing build path is actually migrated |
| FG-RA-04 | QEMU guest boot | not executed | producer-owned QEMU/VM runtime receipt |
| FG-RA-05 | physical Android install/launch | not executed | exact APK → physical device install/launch/restart receipt |
| FG-RA-06 | cross-repository Vectras/Termux runtime | not executed | producer + consumer receipt for same protocol/artifact |
| FG-SL-01 | generic low-level substrate absent | Silicon Light L0 merged | migrate only duplicated primitives with equivalence tests |
| FG-SL-02 | system headers/runtime helpers could leak into L0 | gates enforce headers=0, forbidden runtime primitives=0, undefined symbols=0 | keep source/object audits on every change |
| FG-SL-03 | ARMv7/AArch64 cross-ABI evidence | NDK-bound gates PASS | add physical ABI execution only when same artifact can be bound |
| FG-SL-04 | L0→JNI→APK integration | generated minimal fixture PASS | optional physical fixture execution |
| FG-SL-05 | object semantics portability shadows | pointer-order and signed-shift issues repaired | fuzz/property tests for overlap/range/boundaries |
| FG-SL-06 | migration of existing duplicated native helpers | intentionally not automatic | inventory duplicates → reference implementation → equivalence → migrate one primitive at a time |
| FG-DOC-01 | RAFANDROID doc still said IMPLEMENTED_UNTESTED | this reconciliation updates it | future source merge must update docs or emit `TOKEN_VAZIO_DOC_DRIFT` |
| FG-DOC-02 | Silicon Light docs still said CI_PENDING/IMPLEMENTED_UNTESTED | this reconciliation updates them | bind each status to PR head/run |
| FG-DOC-03 | current-state/status/roadmap omitted these merged surfaces | this reconciliation adds routed state | keep semantic map and index synchronized |
| FG-DOC-04 | old repository map predates RAFANDROID/Silicon Light | this reconciliation adds a current supplement, not a fake full regeneration | perform full generated repository-map refresh separately |

## 5. What is now safe to say

### VERIFIED / revision-bound

- RAFANDROID V1 source and canonical CI are verified at PR #521 exact head.
- Silicon Light L0 source, freestanding host gate, NDK ARMv7/AArch64 gate and generated minimal JNI/APK fixture are verified at PR #526 exact head.
- Both PRs are merged into main lineage.

### Not promoted

- physical handset execution;
- QEMU guest boot correctness;
- Vectras/Termux cross-repository runtime;
- release acceptance/signing for these deltas;
- universal independence of the Android app from Android/Gradle/SDK/NDK;
- cryptographic meaning for the Silicon Light structural tag.

## 6. Remaining low-level program

The next low-level work is not “remove Android from Android.” It is:

```text
inventory duplicated primitive
→ choose reference semantics
→ property/equivalence tests
→ move deterministic memory/register transform to L0
→ zero-symbol cross-ABI gate
→ thin adapter
→ existing application regression
→ optional physical receipt
```

Priority candidates:

1. duplicated byte transforms and fixed-size state helpers;
2. fixed-point math with explicit overflow semantics;
3. non-cryptographic structural tags/checksums where semantics already match;
4. adapter reduction between JNI and domain-specific cores;
5. reproducible object/toolchain receipts.

Do not migrate filesystem, network, permissions, UI, JGit, Room, Compose, package manager, ART, QEMU runtime or signing into L0.

## 7. Session R3

**F_ok:** RAFANDROID and Silicon Light moved from request → source → canonical CI → merged source with bounded evidence; major shadows found during the session were either removed or typed.

**F_gap:** physical Android, QEMU/VM guest proof, cross-repository runtime, full duplicate-primitive migration, complete repository-map regeneration and release/device acceptance remain open.

**F_next:** keep the merged L0/RAFANDROID gates immutable as regression anchors; inventory one duplicated low-level primitive family and prove exact functional equivalence before moving it into Silicon Light.
