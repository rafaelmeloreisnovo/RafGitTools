# RafCode Route Executable V1

State: `IMPLEMENTED_SOURCE / HOSTED_START_PASS / PHYSICAL_DEVICE_TOKEN_VAZIO`  
Core authority: `../rafcode_route_v1`  
claim_allowed: `false`

This module packages the existing route resolver as a tiny fixed-frame executable for direct shell, Termux or ADB use without JNI.

```text
stdin 32-byte raf_route_request
→ raw read syscall
→ rafcode_route_v1
→ raw write syscall
→ stdout 32-byte raf_route_receipt
```

Exit status is `0` for an accepted route receipt, `2` for a fail-closed request, and `3` when the receipt write does not complete.

## Hosted verification

Canonical START run [35838637759](https://github.com/rafaelmeloreisnovo/RafGitTools/actions/runs/35838637759) completed successfully on PR #489 head `69cf8346bf8a796090238bc3dcd34116cb6f3573`. The coherence job passed the route core, route-federation bridge, and freestanding executable gates. Android unit tests, instrumentation APK compilation, lint, devDebug APK verification, and Java/Kotlin CodeQL also passed.

This is hosted CI evidence for that source head. It does not demonstrate execution on an Android device.

## Runtime boundary

The production executable has no libc/CRT, allocator/heap, filesystem API, network, JSON parser or dynamic-loader dependency. The ELF contract forbids `PT_INTERP`, `DT_NEEDED`, runtime relocations, undefined symbols, GOT/PLT and writable static state. It requires one RX load segment and a non-executable stack.

Only Linux/Android raw `read`, `write` and `exit` syscalls are used.

## Builds

```sh
make -C native/rafcode_route_exec_v1 test audit
make -C native/rafcode_route_exec_v1 armv7 audit-armv7
make -C native/rafcode_route_exec_v1 aarch64 audit-aarch64
```

The ARM outputs target the Android API-24 syscall ABI and are intended as direct Termux/ADB proof artifacts.

For an actual device receipt, execute on the target device/Termux environment:

```sh
sh native/rafcode_route_exec_v1/tests/device_smoke.sh /path/to/rafcode-route
```

The runner records ABI/SDK/model, ELF SHA-256, valid/ambiguous receipt hashes and exit codes. It does not upload anything automatically.

## Evidence boundary

```text
HOSTED_START_PASS != PHYSICAL_DEVICE_EXECUTION
CROSS_COMPILED_ELF != PHYSICAL_DEVICE_EXECUTION
TERMUX_COMPATIBLE_TARGET != TERMUX_RECEIPT
SHELL_EXECUTION != JNI_EXECUTION
ROUTE_RECEIPT != CRYPTOGRAPHIC_AUTHENTICITY
```

Physical ARM32/ARM64 execution remains `TOKEN_VAZIO` until a device-side receipt exists.
