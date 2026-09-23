# Receipt — Route JNI Adapter V1

Date: 2026-09-23  
Parent: merged PR #484 / `rafcode_route_v1`  
Kind: `HOSTED_NATIVE_ADAPTER / APPEND_ONLY`  
claim_allowed: `false`

## Delta

A minimal JNI wrapper was added on an isolated branch. It maps two direct 32-byte buffers to the existing freestanding resolver and returns the native receipt status.

The wrapper is explicitly not classified as freestanding.

## Gates at write

```text
SOURCE = IMPLEMENTED
JVM_FRAME_CODEC_TESTS = TOKEN_VAZIO_CI_PENDING
ANDROID_SHARED_LIBRARY_BUILD = TOKEN_VAZIO_CI_PENDING
APK_PACKAGING = TOKEN_VAZIO_CI_PENDING
PHYSICAL_ARM32_JNI = TOKEN_VAZIO
PHYSICAL_ARM64_JNI = TOKEN_VAZIO
```

R3=<F_ok: freestanding route core already integrated on main; F_gap: hosted adapter exact-head CI and physical device invocation; F_next: compile/package through canonical START without weakening the core contract>.
