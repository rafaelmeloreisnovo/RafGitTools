# AGENTS.md — RAFAELIA / RafGitTools

## 0. Federation entry — read this first

This repository is the RAFAELIA **control plane**. `AGENTS.md` is an entry router, not a replacement for evidence, contracts or repository state.

Before acting:

1. bind the exact repository/ref/commit;
2. read `configs/agent-entry-kernel.v1.json`;
3. read `configs/workflow-master-index.json` for topology/authority;
4. read `configs/gap-closure-execution.v1.json` and the current gap ledger;
5. select the next action by urgency + dependency + observable exit criterion;
6. record baseline/rollback before mutation;
7. verify local tests and cross-repository edges separately;
8. emit `F_ok`, `F_gap`, `F_next` and an append-only transition receipt.

Core invariants:

- `TOKEN_VAZIO` is valid and never means zero/false/PASS.
- Urgency orders execution; it does not increase truth.
- `READY_TO_TEST != RESOLVED`.
- Evidence is bound to exact commit/artifact/protocol/environment/device.
- Deferred or ignored-with-reason work remains indexed; it is not silently deleted.
- Local repository authority governs local internals; federation contracts govern edges.
- Documentation is not runtime evidence; hash is not scientific validation.
- Cross-repository success requires producer + consumer evidence for the claimed boundary.
- Historical observations are append-only; successors supersede instead of rewriting.

Canonical federation reference: `docs/AGENT_FEDERATION_ENTRY_V1.md`.

## 1. Local role

RafGitTools owns routing, governance, state axes, ledgers, gates, cross-repository contracts and control-plane validation. It must not silently promote the runtime/scientific claims of repositories it indexes.

## 2. Build

- Primary dev build: `./scripts/gradlew_with_java17.sh assembleDevDebug`
- Hermetic native fallback: `sh scripts/termux/build_apkc_hermetic.sh --abi both` (NativeActivity only; it does not build the full Compose app).
- Install dev build: `./scripts/gradlew_with_java17.sh installDevDebug`
- Internal unsigned release validation: `ALLOW_UNSIGNED_RELEASE=true ./scripts/gradlew_with_java17.sh assembleProductionRelease`

## 3. Test & validation

- Local setup: `./scripts/prepare_local_properties.sh`
- Unit tests: `./scripts/gradlew_with_java17.sh testDevDebugUnitTest`
- Lint: `./scripts/gradlew_with_java17.sh lintDevDebug`
- Canonical governance gate: `sh scripts/validate_rafaelia_workflow.sh`

## 4. Stack contract

- Android app: Kotlin + Gradle + Jetpack Compose + JGit + Retrofit/OkHttp + Room.
- Target Android API: compileSdk/targetSdk 34, minSdk 24.
- Supported native ABIs: `armeabi-v7a` and `arm64-v8a`.

## 5. Safety rules

- Do not claim stubs (`GPG`, `LFS`, `worktree`, `webhook`) as production-ready.
- Do not alter release signing behavior for public distribution without explicit intent.
- Keep CI and local commands aligned with `docs/BUILD.md`.
- Do not copy a different repository's local AGENTS specialization into this repository as authority.

## 6. ARM32 / Termux invariant

Do not bootstrap Android SDK command-line tools inside Android/Termux ARM32 unless `ANDROID_SDK_ROOT` or `ANDROID_HOME` already points to a valid compatible SDK.

Termux ARM32 is a runtime/toolchain validation environment by default, not the canonical APK build host.

Desktop/CI validation:

```bash
./scripts/gradlew_with_java17.sh assembleDevDebug assembleProductionDebug
ALLOW_UNSIGNED_RELEASE=true ./scripts/gradlew_with_java17.sh assembleDevRelease assembleProductionRelease
./scripts/native/verify_apks.sh
```

Termux validation:

```bash
./scripts/termux_arm32_runtime_check.sh
```

## 7. Do not break

- Keep `armeabi-v7a`.
- Keep `arm64-v8a`.
- Keep JDK 17 unless Gradle/AGP/Kotlin/KSP are upgraded together.
- Do not replace ARM32 ASM with ARM64-only code.
- Do not claim full APK build support inside Termux ARM32 without proof.
