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
