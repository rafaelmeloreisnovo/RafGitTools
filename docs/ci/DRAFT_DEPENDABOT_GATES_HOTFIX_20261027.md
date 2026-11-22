# Hotfix P0 — Draft CI and Dependabot exact-head gates (2026-10-27)

**Producer:** `rafaelmeloreisnovo/RafGitTools` · **source branch:** `hotfix/draft-gates-dependabot-20261027`
**State:** SOURCE_IMPLEMENTED / CI_AND_DEVICE_PENDING / NO_AUTO_MERGE / CLAIM_ALLOWED=false.

## ★ Observed problem

- Canonical `.github/workflows/START.yml` previously forced *every* Draft PR to `mode=fast`. Android/Gradle/Privacy app changes could show orchestrator success while Android JVM tests, lint and APK were SKIPPED.
- Its final receipt recorded `PASS_WITH_TYPED_SKIPS`, but no explicit contradiction when a **selected** gate was skipped.
- Dependabot Gradle group `androidx.*` appeared before specific `androidx.compose.*` and `androidx.hilt.*` groups. GitHub processes groups in definition order (first match wins), making grouping wider than requested. Major version changes grouped with minors also increased compatibility uncertainty.

## † Minimal reversible remedy

1. Keep exactly **one active Actions YAML**: `START.yml`. No new workflow or provider secrets.
2. For *Draft PRs*, route a NUL-delimited exact-base-to-head path diff through the stdlib `scripts/ci/draft_lane_guard.py`:
   - `docs/**` and low-risk source-only files → `fast` (no Android claim).
   - `app/**`, Kotlin/Java, Gradle/build wrapper/version catalogs → `android` (Kotlin unit + AndroidTest APK assembly + lint + devDebug APK).
   - `.github/**`, policy/workflow and CI router edits → `full` (including Android/security lanes).
   - Missing/unsafe diff → `full` or explicit failure, never `fast`.
3. Execute Kotlin and Compose app build gates **before native fixture builds** in the existing Android job. Do not remove native tests.
4. Final receipt compares actual job results against `android/security/docs/federation` outputs when required; required-but-skipped becomes **FAIL_REQUIRED_GATE** with explicit `missing_required_gates`.
5. Dependabot: specialized Compose/Hilt/Kotlin/network/testing groups precede catch-all Android. Group routine **minor/patch version updates**; major updates are individual review items. GitHub Actions core and CodeQL each have their own version-only groups. Keep review assignment and no automatic merging. Security updates stay outside version-only groups; provider-side security policy must be checked independently.

## ‡ Falsifiers and tests

- `python3 -m unittest discover -s tests -p 'test_draft_lane_guard.py' -v`
- `python3 -m unittest discover -s tests -p 'test_dependabot_policy_gate_v1.py' -v`
- `./gradlew --no-daemon testDevDebugUnitTest assembleDevDebugAndroidTest lintDevDebug assembleDevDebug --stacktrace`
- Canonical `START.yml` exact-HEAD workflow; inspect `06 Android`, `07 CodeQL` and final `09 receipt` **individually**. Top-level success alone is insufficient.
- Real Android instrumentation execution, OAuth/PAT authority, permissions, organization policy and repository visibility canary are separate evidence gates; GitHub Actions compiling `androidTest` is **not** device execution.

## Scope, rollback and provider boundary

The change touches no existing Android product source, no dependency versions and no repository-visibility state. CI is read-only except routine upload of test artifacts. Do **not** automatically merge this branch or any Dependabot PR. GitHub provider branch-protection/ruleset proof, external review and Android physical evidence remain independently required.

Rollback: keep Draft unmerged; if merged after review, revert the scoped source commits without deleting historical receipts. Existing Dependabot PRs are **not** automatically rebased/closed by changing `dependabot.yml`. They each require exact-head CI and compatibility review.

`SOURCE ≠ ARTIFACT ≠ EXECUTION ≠ EVIDENCE ≠ CLAIM`.
