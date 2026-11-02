# InventarioRAF — freestanding build boundary

Date: 2026-11-02. Base: 4062d516f8f57a5705ac4442f74553875d630646.
Scope: native/inventario_raf_v1 core and its existing Android build integration.

The Makefile already requested freestanding compilation for ARM objects. Android previously compiled the core and JNI in one hosted target without that contract. The core now has its own PIC object target with RAFINV_FREESTANDING, -nostdinc, -ffreestanding, -fno-builtin and strict warnings. Existing ARM32/ARM64 assembly selection and JNI export/shared-library names are retained.

Host selftest now links the separately compiled freestanding object instead of recompiling the core with hosted defaults. A blocking nm check rejects any undefined symbol or nm command failure.

## Evidence

Baseline host selftest: PASS. Changed host selftest: PASS, all five existing cases, GCC 13.3.0 Linux host. Host core undefined symbols: zero. Negative audit with NM=false: correctly fails. Negative audit with NM=echo: correctly fails on nonempty output.

Commands: make -C native/inventario_raf_v1 clean host-test; nm -u native/inventario_raf_v1/build/host/core.o.
Core object SHA256: 0075291941045c22e9fa54dacab73467e9bb15b25163628f4806ab706d7e11d6.
Selftest SHA256: 9dbcf4dbbd9b47c63b4a02325cff8a5572f9edb8ef3c57743d8493fa79c7b28d.

## Boundary and gates

JNI, SAF, JVM/ART allocation, dynamic loading and the test harness remain hosted. The complete APK is not freestanding. No new dependency, license change, runtime API or source algorithm change. Existing repository rights apply. C++ in other unrelated targets is outside this change.

Local CMake/NDK/Clang and physical ARM validation: TOKEN_VAZIO (tools unavailable locally). Android exact-head build, emitted ARM objects, Kotlin tests, lint and independent review remain required before promotion. No speedup or global no-regression claim. claim_allowed=false.

Authority: user request Aplicar freestanding; producer controls implementation. Privacy: public build sources only, no source corpus, credentials or device data. R3: host boundary tested; Android integration pending; exact-head producer CI. Rollback: close unmerged draft, or separately reviewed revert of these scoped build changes after merge.
