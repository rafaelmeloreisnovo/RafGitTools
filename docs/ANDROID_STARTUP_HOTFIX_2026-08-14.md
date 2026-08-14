# Android startup hotfix — WorkManager initialization

**Status:** `IMPLEMENTED_SOURCE / VALIDATED_STATIC`  
**Claim boundary:** `claim_allowed=false` for APK or device-runtime success

## Symptom and source cause

The reported symptom was: the APK installs but exits before the interface opens.

At the reviewed source base, `RafGitToolsApplication.onCreate()` schedules two periodic
workers through `WorkManager.getInstance(this)`. The app manifest simultaneously removed
`androidx.work.WorkManagerInitializer` and the application did not provide a replacement
initialization path. That source relationship can cause a cold-start exception before the
first activity is displayed.

Android's documented default is automatic WorkManager initialization. Removing it requires
a custom `Configuration.Provider` or an explicit initialization path. This app has neither,
so retaining the default initializer is the bounded repair.

## Change

- Removed the manifest override that suppressed the WorkManager AndroidX Startup initializer.
- Kept the application's existing periodic-sync scheduling intact.
- Added a source-level regression contract that rejects a removed initializer without a
  replacement configuration or explicit initialization.
- Added that regression test to the Android CI workflow before Gradle tasks.

## Validation observed here

```text
test_validate_runtime_truth.py
  - current startup contract: PASS
  - disabled default initializer without replacement: rejected
```

## Evidence still required

```text
APK build for this exact commit       = TOKEN_VAZIO
install/start smoke on Android device = TOKEN_VAZIO
logcat confirmation of no startup crash = TOKEN_VAZIO
```

Next probe: build the exact branch, install the resulting `devDebug` APK on an authorized
Android device, launch `com.rafgittools.dev/.MainActivity`, and bind the install/start
receipt and logcat result to the APK SHA-256.
