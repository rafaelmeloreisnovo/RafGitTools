# ARM32 / Termux State

## Supported targets
- Android app ABI: armeabi-v7a and arm64-v8a
- Termux runtime: ARM32 compatible environments

## Current official files
- `scripts/termux/arm32_runtime_check.sh`
- `scripts/native/verify_apks.sh`
- `app/src/main/cpp/CMakeLists.txt`
- `app/src/main/cpp/native_bridge.c`

## Experimental files
- `_incoming/termux_arm32_build.sh`

## Important constraints
- Android requires PIE.
- Bionic is not glibc.
- ARM32 should avoid f64 hot paths.
- NEON may exist but must be detected.
- Termux is runtime validation, not default Android SDK build environment.
- APK must contain armeabi-v7a and arm64-v8a libs.

## Promotion plan
1. Keep `_incoming` as source material.
2. Extract reusable headers/types only when tests exist.
3. Add small C native sanity functions first.
4. Add Kotlin/JNI instrumentation later.
5. Add CI artifact validation.
