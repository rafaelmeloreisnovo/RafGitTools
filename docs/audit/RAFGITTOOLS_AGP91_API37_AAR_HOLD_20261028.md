# RAFAELIA Android AAR compatibility candidate — 2026-10-28

STATE: DRAFT_HOLD | claim_allowed=false | promotion=HOLD | main unchanged.

## Observed failure
RafGitTools PR #728 at bb11cc7e9535f013907d56fde751ce7b0f748e8f: START run 40647616265 failed :app:checkDevDebugAarMetadata with 47 incompatibilities. hilt-navigation-compose 1.4.0 demands AGP >=9.1/API >=37; Work 2.12.0 requires API >=35. CodeQL Java/Kotlin failed on the same AAR metadata gate.

## Reversible candidate
- AGP 8.13.2 -> 9.1.1; compileSdk 34 -> 37; Gradle wrapper 9.5.1, targetSdk34, minSdk24 and all dependencies unchanged.
- Temporary opt-out of AGP 9 built-in Kotlin/new DSL as instructed in Android migration guidance; Kotlin 1.9.24, Compose compiler 1.5.14, KSP1.9.24-1.0.20 and Hilt2.48 preserved. Bridge is provisional, not compatibility proof.
- Android + CodeQL jobs install SDK37 explicitly. No removal of gates, permission changes, release or signature.

## Strict acceptance / rollback
- Required: exact-HEAD CI, Android test+lint+assembly, CodeQL Java/Kotlin+Actions, topology, docs, SEMCHAOS, SHA receipt, negative/adversarial tests, independent review, physical Android and rights checks.
- If compatibility fails, keep gate RED and return to legacy stack. AAR violations and warnings must not be suppressed.
- Before release replace temporary Kotlin bridge with fully reviewed built-in Kotlin upgrade, satisfy downstream API consumers, preserve targetSdk/minSdk intent.
- Rollback: close this draft PR; main and original PR #728 remain untouched.

## External references
https://developer.android.com/build/releases/agp-9-1-0-release-notes
https://developer.android.com/build/migrate-to-built-in-kotlin

## S4 — independent Kotlin/Compose/KSP candidate (2026-10-28)

The merged S3 source failed Android and CodeQL configuration after SDK37.0 installation: `Starting in Kotlin 2.0, the Compose Compiler Gradle plugin is required` (run40657783245). S4 is an isolated draft to test Kotlin2.4.21/Compose2.4.21/KSP2.3.10, not a production authorization. Preserve AGP9.1.1/API37.0, Hilt, dependencies, signing, Android target/min SDK and all CI checks. Original approved Kotlin1.9.24 tuple is retained; candidate is in separate `ci_candidates` with `SCOPED_CI_ONLY`, `promotion=HOLD`, `claim_allowed=false`, and negative admission tests. Freestanding C L0 source and object remain unchanged: L0's legacy version-prefix mask2 for KSP2 independent version is conservatively `TOKEN_VAZIO_UNVALIDATED_INDEPENDENT_KSP2`, not a build proof. All hosted Android/CodeQL/KSP/Hilt, reviewer, device and rights tests remain blockers; no automatic merge. Reference: https://kotlinlang.org/docs/ksp-quickstart.html ; https://kotlinlang.org/docs/compose-compiler-migration-guide.html ; https://kotlinlang.org/docs/gradle-configure-project.html .

## S6 — Hilt/KSP Gradle classloader and atomic Dagger 2.60.1 trial (2026-10-28)

On PR #732 HEAD f7ecee40420ab4f8bb07c7270cf5c5efe05fbbab, START run40659126465: Python/coherence/docs/SEMCHAOS/topology passed; Android task configuration failed at `KspTaskJvm` class not found with Hilt plugin in root buildscript and KSP in the root plugin DSL (different classloaders). Dagger documentation states KSP2.3.9+ has stable support with Dagger/Hilt2.60+ and recommends shared root `plugins { id ... apply false }` declarations. The isolated S6 trial moves Hilt to the root plugins DSL next to KSP, upgrades Hilt plugin/runtime/KSP compiler atomically to 2.60.1, and adds fail-closed plugin-classloader scope validation plus Kotlin1.9/Kotlin2 negative fixtures. Original legacy Hilt classpath remains acceptable to the validator for historical compatibility, but ambiguous simultaneous declarations fail. Hilt/KSP runtime interaction, API/ABI, APK, device, security/privacy/licensing are NOT yet validated; `claim_allowed=false`, promotion=HOLD, no merge or release. Sources: https://dagger.dev/hilt/gradle-setup ; https://dagger.dev/dev-guide/ksp.html ; https://developer.android.com/training/dependency-injection/hilt-android .
