# First Compile-Run Triangle

The first functional Android run is closed only by one evidence chain with three vertices:

```text
SOURCE
  exact Git commit
    ↓
BUILD
  APK + SHA-256 + dual ABI build receipt
    ↓
DEVICE
  exact APK installed + launched on a physical ARM device
    ↘
      SOURCE commit rechecked
```

## Invariant

```text
SOURCE_COMMIT
== BUILD_COMMIT
== RUNTIME_EXPECTED_COMMIT

BUILD_APK_SHA256
== PHYSICAL_DEVICE_APK_INPUT_SHA256
```

A historical APK, a successful source test, or a device launch by itself cannot close the triangle.

## Vertex 1 — source

On the target checkout:

```bash
python3 scripts/audit_source_gaps.py --self-test
python3 scripts/audit_source_gaps.py
git rev-parse HEAD
```

The source-gap audit is fail-closed. An allowlist exception is not runtime evidence.

## Vertex 2 — build

Canonical full app build host is desktop/CI with JDK 17 + compatible Android SDK. Termux ARM32 is not declared a canonical full Gradle build host.

The `Android Client Build` workflow now emits:

```text
RafGitTools-devDebug/
  *.apk
  SHA256SUMS.txt
  BUILD_RECEIPT.json
  BUILD_RECEIPT.sha256
```

`BUILD_RECEIPT.json` binds:

- full 40-hex Git commit;
- APK SHA-256;
- APK size;
- ZIP CRC result;
- required `armeabi-v7a` + `arm64-v8a` presence;
- workflow run ID/attempt;
- `claim_allowed=false` until a physical runtime receipt exists.

## Vertex 3 — physical device

Use a host with `adb`, `apksigner`, the exact source checkout, the downloaded APK and its `BUILD_RECEIPT.json`.

`devDebug` combines the `dev` flavor suffix (`.dev`) with the `debug` build-type suffix (`.debug`), so its exact application ID is `com.rafgittools.dev.debug`.

```bash
python3 scripts/runtime/close_first_compile_run_triangle.py \
  --build-receipt /path/BUILD_RECEIPT.json \
  --apk /path/RafGitTools-devDebug.apk \
  --repo . \
  --runtime-output .rafgittools/receipts/runtime-device.json \
  --triangle-output .rafgittools/receipts/first-compile-run-triangle.json \
  --package com.rafgittools.dev.debug \
  --activity com.rafgittools.MainActivity \
  --install \
  --launch \
  --require-runtime-pass \
  --require-triangle-pass
```

The runtime collector verifies:

- exact repository commit;
- APK ZIP integrity;
- both supported ABIs inside the APK;
- APK signature through `apksigner`;
- physical device presence through `adb`;
- physical ABI (`armeabi-v7a` or `arm64-v8a`);
- install success;
- launch success.

The triangle closer additionally verifies that the APK bytes used on the device have exactly the SHA-256 recorded by the build receipt.

## ARM32 / Android 10 note

A Termux session on the same non-root Android 10 device is not assumed to have authority to silently install its own APK. For a fully automated PASS, use an ADB-capable host connected to that phone (PC or another supported host). Manual package-installer UI is not silently promoted to an automated install receipt.

## Promotion states

```text
source-only PASS       != build PASS
build PASS             != device PASS
device launch alone    != source/build custody
triangle PASS          = first compile-run verified for that exact commit + APK + device observation
release_allowed        = false
```

After triangle PASS, the next gate is governed integration of the receipt into the project evidence ledger. It is a separate action and does not happen automatically.
