# RAFANDROID Toolchain Shell V1

`rafandroid` is the RafGitTools authorial **control shell** for the Android build stack.

It does **not** pretend Gradle, Android SDK, AndroidX, NDK, D8/R8, ART, QEMU or the Android platform are authorial implementations. Those remain external authorities. The authorial layer is the deterministic way they are discovered, bound, executed, gated and turned into receipts.

## Single entrypoint

```bash
tools/rafandroid/rafandroid probe
tools/rafandroid/rafandroid shadow
tools/rafandroid/rafandroid tails
tools/rafandroid/rafandroid friction
tools/rafandroid/rafandroid graph
tools/rafandroid/rafandroid bind
tools/rafandroid/rafandroid gradle assembleDevDebug
tools/rafandroid/rafandroid androidx
tools/rafandroid/rafandroid qemu
tools/rafandroid/rafandroid receipt
```

## Direct wrappers

```bash
tools/rafandroid/rafandroid tool sdkmanager --list
tools/rafandroid/rafandroid tool aapt2 version
tools/rafandroid/rafandroid tool d8 --version
tools/rafandroid/rafandroid tool r8 --version
tools/rafandroid/rafandroid tool apksigner --version
tools/rafandroid/rafandroid tool adb devices
```

There is no automatic download and no automatic license acceptance.

## Gates

Freestanding project core:

```bash
tools/rafandroid/rafandroid native-gate path/to/core.c 29 both
```

JNI artifact:

```bash
tools/rafandroid/rafandroid jni-gate path/to/lib.so
```

DEX / APK:

```bash
tools/rafandroid/rafandroid dex-gate app-debug.apk
tools/rafandroid/rafandroid apk-gate app-debug.apk
```

## Minimal JNI scaffold

```bash
tools/rafandroid/rafandroid scaffold /tmp/MyMinimal io.rafaelia.minimal
```

The template starts with Java platform Activity + JNI + C11 freestanding core, ARM32/ARM64, and AndroidX OFF by default. AndroidX is inventoried separately and enabled only when the product needs it.

## QEMU boundary

`rafandroid qemu` proves discovery only. VM boot, guest state, exit code and physical Android remain `TOKEN_VAZIO` until their owning runtime produces receipts.

## Invariants

- `SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM`
- `TOKEN_VAZIO != 0`
- `IMPLEMENTED_UNTESTED != PASS`
- no implicit network
- no silent SDK bootstrap
- no automatic license acceptance
- wrapper before system Gradle
- no static-to-runtime promotion
- external platform code is not relabeled authorial

See `docs/RAFANDROID_METHOD_V1.md`.


## Tails e fricção

`shadow` deduplica caminhos de SDK equivalentes antes de sinalizar autoridade concorrente.

`tails` inventaria, sem remover automaticamente:
- referências AndroidX;
- code generation (KSP/KAPT/annotation processors);
- shrink/minification;
- native build;
- Compose.

`friction` relata bloqueadores concretos e shadows. O campo `numeric_score` permanece `TOKEN_VAZIO_UNCALIBRATED`: não existe pontuação sintética até haver calibração com builds medidos.


## Silicon Light L0

O núcleo autoral independente fica em `native/silicon_light_v1`.

```bash
make -C native/silicon_light_v1 host-test audit
tools/rafandroid/rafandroid silicon-gate native/silicon_light_v1 24 both
```

O gate exige no L0:

- zero headers de sistema;
- zero heap/allocator;
- zero syscall;
- zero JNI/Android API;
- zero símbolos externos indefinidos em ARMv7 e AArch64.

A independência é do **runtime L0**. Clang/NDK são ferramentas de construção, e Android continua sendo autoridade externa para APK, ART, permissões, UI, armazenamento e instalação.
