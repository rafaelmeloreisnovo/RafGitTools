# Codex Workplan — RafGitTools

## Prime Directive
Never mark roadmap as complete without source evidence and tests.

## Work Model
1. Inspect.
2. Classify.
3. Patch minimally.
4. Build.
5. Test.
6. Document.
7. Report.

## Classification Rules
- COMPLETE requires code + build + test or runtime validation.
- PARTIAL requires code but missing coverage or incomplete UX.
- STUB means NotImplementedError, TODO body, placeholder return, or no real effect.
- EXPERIMENTAL means _incoming, research, or not wired to app.
- PLANNED means docs only.

## ARM32 Rules
- Android APK support is not the same as Termux runtime support.
- native ASM returning 1 is health/stub.
- _incoming Termux engine is not production until promoted.
- Use armeabi-v7a validation in APK.
- Keep ARM32 no-f64-hot-path discipline where possible.
- Bionic != glibc.

## Required Before Claiming ARM32 Ready
- Gradle builds APK with armeabi-v7a.
- APK contains lib/armeabi-v7a/librafcore.so.
- nativeAbiMask reports ARM32 bit on ARM32 device.
- scripts/termux/arm32_runtime_check.sh runs on Termux ARM32.
- docs/ARM32_TERMUX_STATE.md updated.

## Required Before Claiming Release Ready
- productionRelease builds.
- signing path documented.
- APK verification passes.
- tests pass.
- no secrets committed.
- status docs updated.
