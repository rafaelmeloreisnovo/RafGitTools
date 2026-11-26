# Gradle 9.9.2 / AGP 9.5.1 — coordinated CI-only candidate

PR #839, predecessor `0f508ba730c201e9fea728dc5b4d96eb104c7930`. START 45145402585 failed coherence (old L0 exact pin) and RAFANDROID minimal APK fixture (AGP 8.13.2 relies on InternalProblems removed in Gradle 9.6). No APK success claimed.

Root AGP 9.1.1 and minimal JNI AGP 8.13.2 are replaced together by stable 9.5.1. Official [AGP 9.5 release notes](https://developer.android.com/build/releases/agp-9-5-0-release-notes) list 9.5.1 and minimum Gradle 9.6, JDK 17; [Gradle migration guide](https://docs.gradle.org/current/userguide/upgrading_version_9.html) describes removal. Minimum version is not proof that this exact combined tuple works.

Gradle wrapper 9.9.2 remains pinned from the original PR; L0 exact comparison and byte fixture track actual source. RAFANDROID lock follows its template. Kotlin/Compose/KSP/Hilt, SDK, JDK17, NDK, CMake, both ABIs, signing, lint and CI gates unchanged.

Gradle and AGP are hosted build-time tooling, not freestanding runtime. Exact distributed artifact notices and SHA-256 remain TOKEN_VAZIO. Retained for Android resource/DEX/JNI/APK assembly; independent replacement requires equivalent artifact and device parity. No upstream code copied.

Falsifiers: exact HEAD coherence, minimal fixture APK, full APK, tests, lint, CodeQL, dual ABI and receipt. Preserve any new failure; no relaxed gate. Full execution pending. Rollback: reviewed revert of coordinated change and wrapper upgrade together; preserve historical commits. claim_allowed=false.
