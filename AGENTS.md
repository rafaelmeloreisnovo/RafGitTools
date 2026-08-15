# AGENTS.md — RafGitTools (Android)

## Build
- Primary dev build: `./scripts/gradlew_with_java17.sh assembleDevDebug`
- Hermetic native fallback: `sh scripts/termux/build_apkc_hermetic.sh --abi both` (NativeActivity only; it does not build the full Compose app).
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

# Mandatory License / README / Provenance Guard

Before creating, editing, refactoring, porting, documenting, packaging, publishing, or removing code in any repository reached through RafGitTools:

1. Resolve the repository's upstream/reference lineage when any external origin is known or suspected.
2. Read the applicable upstream `LICENSE`/`LICENCE`/`COPYING`, `NOTICE`/`AUTHORS` when present, and the relevant upstream `README` before changing licensing or attribution-sensitive material.
3. Read the target repository's corresponding license and README files and compare them with the upstream obligations.
4. Consult `governance/licensing/VERIFICATION_LEDGER.md`. A prior verification may be reused only while its evidence fingerprints remain unchanged. Date alone never proves freshness.
5. Treat third-party work as third-party. Porting, translation to another language, cosmetic edits, build changes, renaming, reformatting, or large mechanical rewrites do not establish independent authorship.
6. A project may be used as `EXTERNAL_REFERENCE_ONLY` for facts, capabilities, standards or technical requirements without copying its expressive architecture. If architecture, decomposition, module sequence, specific logic or expressive structure is reused, classify it as third-party/derived as applicable; do not call it independently authorial.
7. Claim `CLEAN_AUTHORIAL_COMPONENT_VERIFIED` only for a separately identifiable component whose conception, structure, logic and implementation have independent provenance sufficient to support that classification.
8. If authorship/origin cannot be demonstrated, use `UNKNOWN_ORIGIN/TOKEN_VAZIO`; never upgrade doubt to user authorship.
9. Never delete or rewrite an upstream copyright, notice or license merely because it names another author. Preserve what the applicable license requires.
10. License changes, relicensing, copyright/authorship changes, material removal for licensing reasons, dual licensing, publication, merge or release require human review. Routine attribution/index/link fixes may be prepared on an audit branch when the applicable obligation is already verified.

## Verification cache rule

The reusable verification key is evidence-based, not time-based. At minimum track:

`repo + target_ref + target_LICENSE_blob_sha + target_README_blob_sha + upstream_identity + upstream_LICENSE_ref/fingerprint + verification_date + status`.

Re-verify whenever any relevant fingerprint changes, the upstream/reference changes, the license/README changes, or a new third-party component is introduced. A stale date by itself does not invalidate an unchanged verified record; a changed fingerprint invalidates it immediately.

## Fail closed

If the applicable license, upstream, provenance or authorship boundary is unclear, stop attribution-sensitive promotion and record `TOKEN_VAZIO` / `LEGAL_REVIEW_REQUIRED` as appropriate. Do not guess to be helpful.
