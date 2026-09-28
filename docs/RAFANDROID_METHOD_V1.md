# RAFANDROID Method V1 — Android Shell with Minimum Friction

State: `IMPLEMENTED_UNTESTED` until CI of the exact branch passes.  
Authority: RafGitTools local control-plane mechanics.  
Federated authority: Mapa.  
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

## 9. Initial F3

`F_ok`: shell + scaffold become VERIFIED_LIMITED only after exact-branch CI.  
`F_gap`: RafGitTools app integration, physical Android and QEMU boot remain separate evidence boundaries.  
`F_next`: consume this shell from legacy build scripts only after this module's CI is green.
