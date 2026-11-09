# RAFAELIA Ω | PIRC-2 | bounded START and SAF Kotlin hotfix | 2026-11-09

SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM.

## Current authority and exact source
- Producer: rafaelmeloreisnovo/RafGitTools main 1776f5c71f7e8101b6687b98dac19b86db4c0d70 (no overwriting main).
- Previous merges: PR #806, #807, #816 are MERGED. Prior claim 'DRAFT/HOLD' is historical, not current.
- Failure: START main run 42613190243 at exact 1776f5c71f7e8101b6687b98dac19b86db4c0d70; job 129147792275 coherence FAILED (2 tests); job 129147792194 Android `:app:compileDevDebugKotlin` FAILED.
- Old topology PR816 run42602825103: topology 01 PASS, coherence/Android FAIL, so no full pipeline claim.
- Main workflow START has explicit 9-entry allowlist; Python validator and tests still hardcode 8.
- DriveExistingGrantScanCard.kt contains unresolved Kotlin identifiers `granted`, `resolver`, `snapshot`, `result`. Type-inference errors cascade.

## Minimal intervention (four paths, two independent scoped causes)
1. `scripts/validate_main_provider_enforcement_plan_v3.py`: add exactly `drive-snapshot-solo-apk.yml` to hardcoded known scoped filenames; no glob broadening and no change to provider authority.
2. `tests/test_main_provider_enforcement_plan_v3.py`: update 8→9 and assert exact ninth filename. Two existing negative tests remain intact: missing workflow or extra unknown workflow must fail.
3. `app/src/main/kotlin/com/rafgittools/ui/screens/setupwizard/DriveExistingGrantScanCard.kt`: request persisted READ explicitly, optional WRITE separately as before; name scan `snapshot`, use `context.contentResolver`, then dispatch through existing `scan` Result. Do not change SAF provider, publish implementation, append-only behavior, root selection or source handling.
4. This successor pointer/receipt. No dependency upgrades, permission changes, deleted history, or rewritten third-party code.

## Adversarial falsifiers prior to PR
- Positive: exact declared set == actual 9 GitHub workflow filenames on the source commit.
- Negative: synthetic 10th workflow fails strict equality; missing Drive Snapshot workflow fails.
- Kotlin static: all four missing identifiers corrected at source call sites; no new Android dependency; `READ` persistence request is explicit.
- Runtime/syntax regression not inferred from static checks; CI on exact new HEAD = N5 until provider reports final.

## Rollback, authority, uncertainties
- Branch/PR DRAFT/HOLD only, no auto-merge. Rollback by closing PR (main unaffected); if merged by authorized maintainer, create scoped inverse changes with a successor receipt. Do not delete historical evidence.
- JNI/NDK/JDK/SDK/Gradle independence applies ONLY to freestanding C/ASM core, not to Android or YAML Actions orchestration. Never claim YAML is freestanding.
- N0: two failing main CI logs, exact main blobs and scoped static falsifiers.
- N2: stale tuple/test and undefined identifiers explain observed failures, pending exact-head CI.
- N5: full CI, APK hardware ARMv7, Google Drive DocumentsProvider, physical performance.
- claim_allowed=false for release/physical APK/Freestanding Android E2E.
