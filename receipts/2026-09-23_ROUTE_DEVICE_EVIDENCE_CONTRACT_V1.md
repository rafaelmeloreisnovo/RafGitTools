# Receipt — Route Device Evidence Contract V1

Date: 2026-09-23  
Kind: `DEVICE_GATE_PREPARATION / APPEND_ONLY`  
claim_allowed: `false`

## Delta

A real Android instrumentation test and an explicit adb receipt runner were materialized. No device execution is claimed by source existence.

## State

```text
INSTRUMENTATION_SOURCE = IMPLEMENTED
DEVICE_RUNNER_SOURCE = IMPLEMENTED
ANDROIDTEST_COMPILE = TOKEN_VAZIO_CI_PENDING
PHYSICAL_ARM32_JNI = TOKEN_VAZIO
PHYSICAL_ARM64_JNI = TOKEN_VAZIO
```

F_ok = physical evidence path is now reproducible and narrow.  
F_gap = actual authorized device/emulator execution.  
F_next = compile the androidTest APK in canonical START; physical execution only on an available authorized runner.
