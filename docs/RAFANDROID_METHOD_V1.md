# RAFANDROID Method V1 — Android Shell with Minimum Friction

State: `VERIFIED_LIMITED / MERGED` for the revision-bound shell/build scope.  
Authority: RafGitTools local control-plane mechanics.  
Federated authority: Mapa.  
Evidence anchor: PR #521 head `62925c8a573f1860a9c278567c1716e153d580f1`, START run `36360576045` / #241 = `SUCCESS`, merge `f0c192d9c56b2cc794d1ed18e361260a3478f598`.  
`claim_allowed=false`.

## 1. Boundary

The Android stack remains layered:

`Gradle -> AGP -> SDK/AAPT2 -> JVM/Kotlin -> D8/R8 -> DEX -> NDK/Clang -> JNI -> package -> align -> sign -> device -> ART`

QEMU/emulator and AndroidX are additional authorities when used. RAFANDROID does not rename those external systems as authorial. The authorial contribution is discovery order, binding, routing, gates, scaffold policy, shadow diagnostics and receipts.

## 2. Method

`PROBE -> BIND -> EXECUTE -> VERIFY -> RECEIPT`

- **PROBE:** non-mutating discovery. Missing = `TOKEN_VAZIO`.
- **BIND:** one concrete SDK root; no download.
- **EXECUTE:** Gradle wrapper preferred; direct tools are allowlisted.
- **VERIFY:** artifact-specific C/JNI/DEX/APK gates.
- **RECEIPT:** commit/ref/toolchain/shadow evidence with bounded claims.

## 3. Friction

Friction is kept as named causes, not an invented universal score:

- missing tool;
- ambiguous authority path;
- implicit network;
- version drift;
- duplicate entrypoint;
- hidden native helper;
- packaging/signing ambiguity;
- runtime evidence gap.

## 4. Shadows

A shadow is a second plausible authority path: multiple SDK roots, NDKs, build-tools, command-line tools, system Gradle plus wrapper, or duplicated dependency authority. `SHADOW_REVIEW` means review is required; it is not automatically FAIL.

## 5. Tails

A tail is a stage that still survives after the main output: shrink, packaging, signing, install, generated code, VM boot, remote service, etc.

Rules:

1. optional tails stay off in the minimal scaffold;
2. required tails are explicit graph nodes;
3. no tail inherits PASS from an upstream stage;
4. removing a tail requires evidence that the product no longer needs it.

## 6. AndroidX

AndroidX is not required for the minimal Java/JNI shell. Scaffold default is OFF. Existing AndroidX is inventoried from Gradle. Removing AndroidX from the current RafGitTools app is a separate product refactor and is not implied by this module.

## 7. QEMU

Binary discovery is not VM execution:

`qemu present != process success != guest boot != correct VM != physical Android success`.

Runtime claims remain with Vectras/qemu/Termux authorities.

## 8. Falsifiers

The method fails if implicit downloads or silent license acceptance appear, TOKEN_VAZIO becomes PASS, undefined symbols are ignored for a declared freestanding core, static QEMU discovery promotes runtime, or artifact gates claim results without invoking artifact verifiers.

## 9. Current evidence and F3

The exact PR #521 canonical START run completed successfully. The observed successful scope includes the single-root topology gate, RAFANDROID shell gate, Python tests, documentation gate, Android unit/instrumentation compile/lint/assemble/APK verification, CodeQL Actions, CodeQL Java/Kotlin and final receipt.

This promotes the **shell/build integration scope only**. It does not promote physical Android, QEMU guest execution or cross-repository runtime.

`F_ok`: RAFANDROID shell, canonical START integration, bound NDK gate and real DEX/APK gates are revision-bound PASS and merged.  
`F_gap`: physical Android install/launch, QEMU guest boot and Vectras/Termux consumer runtime remain separate evidence boundaries.  
`F_next`: keep RAFANDROID as the single Android-toolchain control shell and migrate existing build entrypoints only when equivalence/regression evidence exists; do not create parallel wrappers.
