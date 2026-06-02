# AGENTS.md — RafGitTools (Android)

## Build
- Primary dev build: `./scripts/gradlew_with_java17.sh assembleDevDebug`
- Install dev build: `./scripts/gradlew_with_java17.sh installDevDebug`
- Internal unsigned release validation: `ALLOW_UNSIGNED_RELEASE=true ./scripts/gradlew_with_java17.sh assembleProductionRelease`

## Test & Validation
- Local setup: `./scripts/prepare_local_properties.sh`
- Unit tests: `./scripts/gradlew_with_java17.sh testDevDebugUnitTest`
- Lint: `./scripts/gradlew_with_java17.sh lintDevDebug`

## Stack Contract
- Android app only: Kotlin + Gradle + Jetpack Compose + JGit + Retrofit/OkHttp + Room.
- Target Android API: compileSdk/targetSdk 34, minSdk 24.
- Supported native ABIs: `armeabi-v7a` and `arm64-v8a`.

## Safety Rules
- Do not claim stubs (`GPG`, `LFS`, `worktree`, `webhook`) as production-ready.
- Do not alter release signing behavior for public distribution without explicit intent.
- Keep CI and local commands aligned with `docs/BUILD.md`.

# AGENTS.md — RafGitTools Operational Rules

## ARM32/Termux invariant

Do not bootstrap Android SDK command-line tools inside Android/Termux ARM32 unless `ANDROID_SDK_ROOT` or `ANDROID_HOME` already points to a valid compatible SDK.

Termux ARM32 is a runtime/toolchain validation environment by default, not the canonical APK build host.

## Required validation

Desktop/CI:

```bash
./scripts/gradlew_with_java17.sh assembleDevDebug assembleProductionDebug
ALLOW_UNSIGNED_RELEASE=true ./scripts/gradlew_with_java17.sh assembleDevRelease assembleProductionRelease
./scripts/native/verify_apks.sh
```

Termux:

```bash
./scripts/termux_arm32_runtime_check.sh
```

## Do not break

Keep armeabi-v7a.

Keep arm64-v8a.

Keep JDK 17 unless Gradle/AGP/Kotlin/KSP are upgraded together.

Do not replace ARM32 ASM with ARM64-only code.

Do not claim full APK build support inside Termux ARM32 without proof.
