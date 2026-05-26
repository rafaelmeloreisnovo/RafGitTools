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
