# Scoped Android Kotlin 2 temporary-directory hotfix — 2026-10-28

status: DRAFT_HOLD
claim_allowed: false
source_main: 03a3f6a3f9ce851693d063b799fe88d00e26c78b
source_test_blob: 65685723c3499f14cdb98ad5dd7610f4e2e6b0ca
origin_failed_run: 40659618593
origin_failed_job: 122719486809
origin_compiler_diagnostic: app/src/test/kotlin/com/rafgittools/offline/AtomicFileQueueStorageTest.kt:12:25 deprecated createTempDir
root_cause_observed: Kotlin 2.4.21 treats deprecated createTempDir in test sources as compile error; Kotlin main/Hilt/KSP compiles up through :app:hiltJavaCompileDevDebug, while unit-test Kotlin compilation fails.
change: replace createTempDir with kotlin.io.path.createTempDirectory("raf-offline-").toFile(); original finally deleteRecursively cleanup retained.
security: avoid deprecated default temporary-directory permission behavior; no new dependencies or runtime permissions.
scope: source test only; preserve Android target/minSdk, AGP, Kotlin/KSP/Hilt versions, Compose, CodeQL, native ABI, warning gates, and receipts.
acceptance: testDevDebugUnitTest succeeds at exact HEAD; assembleDevDebugAndroidTest, assembleDevDebug, APK SHA-256, lintDevDebug, CodeQL, final receipt also succeed; independent device, legal and review gates before release.
rollback: close this isolated draft PR or revert its single commit. No merge by this operation.
state: TOKEN_VAZIO_UNMEASURED for CI at creation; no promotion.
