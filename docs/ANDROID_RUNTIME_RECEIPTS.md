# Android runtime receipts — evidence-first gate

This repository distinguishes source/build evidence from device runtime evidence.
A valid APK hash, ZIP structure, or source test does **not** prove signature
acceptance, installation, activity launch, or execution on Android hardware.

The canonical collector is:

```bash
python3 scripts/runtime/capture_android_runtime_receipt.py --help
```

## Observation-only receipt

Observation mode is the default and has no install/launch side effect:

```bash
make runtime-receipt \
  RUNTIME_APK=app/build/outputs/apk/dev/debug/app-dev-debug.apk
```

The Make target binds the receipt to the current Git commit. Missing
`apksigner`, `adb`, a connected device, install evidence, or launch evidence is
recorded as `TOKEN_VAZIO`, `NOT_MEASURED`, or `BLOCKED`; it is never promoted to
`PASS`.

## Physical device promotion

Installation and launch require explicit flags. Supply the actual package and
activity from the APK being tested:

```bash
make runtime-receipt \
  RUNTIME_APK=app/build/outputs/apk/dev/debug/app-dev-debug.apk \
  RUNTIME_RECEIPT_ARGS='--install --launch --package <package> --activity <activity> --require-runtime-pass'
```

`--install` performs `adb install -r`. `--launch` performs an explicit
`adb shell am start -W -n package/activity`. The collector records a one-way
SHA-256 of the device serial rather than the serial itself.

## Promotion contract

`claim_allowed=true` requires all of the following in the *same receipt*:

1. repository commit matches the expected commit;
2. APK is a valid ZIP and contains both `armeabi-v7a` and `arm64-v8a` native ABIs;
3. `apksigner verify --verbose` returns success;
4. ADB reports a connected device;
5. explicit installation returns `Success`;
6. explicit activity launch returns without an Android error/exception.

The invariant is:

```text
CUSTODY_PASS_DOES_NOT_PROMOTE_RUNTIME
```

A failure or missing observation keeps `runtime=BLOCKED`. Use
`--require-runtime-pass` in a physical-device gate when a non-PASS result must
fail the command.

## Local tests

```bash
make runtime-receipt-tests
```

The tests use fake `adb`/`apksigner` executables. They verify fail-closed state
transitions only; they are not device evidence.
