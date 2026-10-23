# RAFAELIA L0 × START × Gradle/Kotlin/KSP — scoped conflict gate V1

**State:** DRAFT / no merge / no release / claim_allowed=false. This is not a Six Sigma certification.

## SOURCE → ★ observation → † bifurcation → ‡ minimal transformation

- ★ `.github/workflows/START.yml` is the single canonical workflow and is modified by PRs #680, #681, #682, #686, #687, #693, #694, #695 and #700. † Blind replacement can suppress tests or cross-producer gates. ‡ Keep `START.yml` intact; attach the new gate through the already-executed coherence call to `scripts/validate_rafaelia_workflow.sh`, preserving the single-root.
- ★ Root `build.gradle` has AGP 8.13.2, Kotlin Gradle plugin 1.9.24 and KSP 1.9.24-1.0.20; wrapper is Gradle 9.5.1; Compose compiler 1.5.14; JDK17; ARMv7/AArch64. † Gradle 9 disallows Kotlin plugin <2; Kotlin 1.9.x historically supports narrower AGP/Gradle matrix; Compose compiler 1.5.14 targets Kotlin 1.9.24 and KSP matches Kotlin suffix. ‡ Known incompatible tuple explicitly BLOCKED, not a green compatible build.
- ★ PRs #675, #676, #691 and #692 update Kotlin, Gradle wrapper, Android and KSP separately. † Version-by-version merging can turn a known blocker into an unknown mismatch. ‡ Exact-source tuple guard; future migration requires coordinated version matrix and Gradle/Android exact-head evidence.

## Layering contract

`INPUT: 8 × 8 bytes → void rmr_toolchain_l0(p,o) → OUTPUT: bit-mask + evidence guard`.

- L0: one pure specialized C kernel, no libc, headers, global mutable state, heap, syscalls, JNI, malloc, loops, or external calls. Each input is a *caller-prepared* 64-byte matrix; output 2 bytes. No parsing, logging, I/O or allocation in L0.
- Compile flags: `-O2 -ffreestanding -fno-builtin -fno-stack-protector -fno-unwind-tables -fno-asynchronous-unwind-tables -nostdlib`; three separately compiled ELF objects for ARMv7, AArch64 and x86-64. Undefined GLOBAL/WEAK symbols must be zero.
- Hosted bridge: Python **standard library** parses exact source pins, checks START connection and ABI, invokes Clang/readelf, executes synthetic host fixtures, emits JSON receipt. Python, Clang, OS and host loader are **external BUILD/TEST dependencies**, never called by L0.
- Android `classes.dex`, APK signing, ZIP and Gradle packaging remain unproven until an actual hosted build at exact head. NUMA topology is not inferred from Android ARM phones; raw machine-code hexadecimal is not injected without a measured need or emitter verification.

## Output bits (no claim promotion)

| Bit | Conflict or missing invariant |
|---|---|
| 0 | Gradle major >= 9 with Kotlin plugin major < 2 |
| 1 | Kotlin vs KSP three-byte prefix mismatch |
| 2 | Kotlin 1.9.24 with Compose Compiler different from 1.5.14 |
| 3 | JDK is not 17 in the scoped repository baseline |
| 4 | Expected ARMv7 + AArch64 ABI bits not both present |
| 5 | Reserved control row not zero |

`output[1]=1` **always**: the L0 kernel cannot prove an Android build, runtime behavior, scientific claims, device install or external certification.

Expected baseline: `conflict_mask=1`, so `PASS_SCOPED_DETECTS_BLOCKED_BASELINE` is a successful detector run **with the build readiness BLOCKED**. On unknown source versions, missing compiler, missing field, invalid ELF or failed fixture, stop `FAIL_CLOSED` rather than falsely accepting it.

## Precision, rollback and follow-up

- No `START.yml`, Gradle, Kotlin, KSP, Compose, Android source or original PR code is replaced by this PR.
- Existing `coherence` lane already calls `scripts/validate_rafaelia_workflow.sh`: only add the specialized gate call there. Tests are separable and measure 5 negative fixtures + baseline, never assert physical ARM.
- Exact user requirement 'no symbols' applies only where possible to **data layout**; an exported named ABI function and compiled ELF metadata are unavoidable when another module must invoke a gate. No invented zero-cost claims.
- Rollback: close this draft without altering `main`; if merged later, revert the narrow append-only gate call and its new isolated files, preserving receipts.
- Next: test coordinated Gradle/AGP/Kotlin/KSP+Compose proposal on an isolated branch with *full* Android CI, verify DEX/APK and physical ARM32/64; never infer compatibility from source alone.

Sources (external normative compatibility references only; no external code copied):
- https://docs.gradle.org/current/userguide/upgrading_major_version_9.html
- https://kotlinlang.org/docs/gradle-configure-project.html
- https://developer.android.com/build/releases/agp-8-13-0-release-notes
- https://developer.android.com/jetpack/androidx/releases/compose-kotlin

R3: F_ok=deterministic gate + L0 source + workflow adapter; F_gap=real Gradle compatibility + device + human review; F_next=controlled coordinated matrix proposal, no merge.
