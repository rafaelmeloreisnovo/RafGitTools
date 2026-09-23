# Route Device Evidence Contract V1

State: `TEST_SOURCE_IMPLEMENTED / DEVICE_EXECUTION_TOKEN_VAZIO`

Purpose: close the final evidence gap between Android build/packaging and a real JNI invocation.

```text
BUILD_PROVEN
→ authorized Android device/emulator
→ librafroute.so load
→ DirectByteBuffer request
→ rafcode_route_v1
→ native receipt
→ Kotlin decode
→ device receipt
→ DEVICE_PROVEN_BOUNDED
```

## Instrumented checks

The test class `ManifoldRouteNativeBridgeInstrumentedTest` performs two device-side calls:

1. CODE_RUNTIME trigger 4 must return `R0004`.
2. ATLAS trigger 7 with the ambiguous bit must fail closed and preserve the native ambiguity error.

This exercises the actual JNI symbol and shared library on the target Android runtime. It is not equivalent to a JVM unit test.

## Runner

`scripts/run_route_device_smoke.sh` requires exactly one authorized adb target, invokes only this instrumentation class, and writes a local receipt with:

- exact Git HEAD;
- ABI and SDK;
- manufacturer/model;
- SHA-256 of the build fingerprint rather than the raw fingerprint;
- Gradle exit code;
- receipt SHA-256.

The script does not upload the receipt by itself.

## Evidence boundary

```text
ANDROIDTEST_COMPILED != DEVICE_EXECUTED
EMULATOR_PASS != PHYSICAL_HANDSET_PASS
ONE_DEVICE_PASS != ALL_ABIS_PASS
DEVICE_PASS != SCIENTIFIC_CLAIM
```

Until an actual runner receipt exists:

`PHYSICAL_ARM32_JNI = TOKEN_VAZIO`

`PHYSICAL_ARM64_JNI = TOKEN_VAZIO`
