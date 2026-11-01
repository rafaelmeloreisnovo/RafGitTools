# inventarioRAF V1 / format_probe L0 — START HERE (DRAFT/HOLD)

Parent producer: native/inventario_raf_v1 — path/MIME/size SAF metadata analyzer and ARM32/AArch64 assembly. This submodule adds bounded ELF/DEX/ZIP byte inspection without duplicating the parent core. The two contracts run side-by-side and are not yet a unified Android device implementation.


An isolated freestanding C byte-inspection middleware **inside RafGitTools**, not a replacement Android APK. RafGitTools is implementation authority; Mapa is federation authority; Google Drive remains private-data authority.

## Navigation / relationships

- Host (Android/SAF, Termux, future private Drive adapter) authorizes one source, obtains immutable bounded bytes and identifies exact version.
- L0 receives only pointers, sizes and verified capability bits, never credentials, file handles, threads or paths.
- L0 emits a typed format/architecture/ZIP header count and deterministic state/receipt. A hosted private adapter links that receipt into the existing Navigator, Atlas/Manifold and source graph.
- Existing archive-line Atlas draft PR #754 is an independent private line locator, not this low-level parser. These stages require proven interop before integration.
- Do not duplicate files, publish paths/contents or infer absent entries as zero.

## Code / minimum adapter

native/inventario_raf_v1/format_probe/
  inventarioraf.h           caller-owned request/state/receipt
  inventarioraf.c           bounded freestanding no-libc ELF/DEX/ZIP detector
  test_inventarioraf.c      hosted C falsifiers (host libc allowed only here)
  Makefile                  host tests and cross-ABI objects
  README.md                 navigation, stop/rollback
  LICENSE_SCOPE.md          authorship and license boundaries

One externally visible processing entry. Compiler-local static helpers are not platform imports. Core has no heap, libc, OS calls, JNI, SDK, Android headers, Java, network or runtime external dependencies. Compile and linker tools are build-time dependencies. An Android APK still requires the Android OS and authorized packaging/signing/permission infrastructure.

## What really exists

- ELF32/64 little-endian header/table bounds and machine hint ARMv7/AArch64/x86_64/RISC-V.
- DEX header-only structural checks. No method, class, string or source-level semantic index.
- ZIP local/central directory header counts and path bounds for a *complete bounded buffer*. No inflate/CRC, recursion, APK signature proof or ZIP contents. ZIP64/data descriptor/encryption unsupported.
- ART/OAT currently unsupported (versioned Android format); no ART execution.
- Two logical budgets (fuel and bytes), request generation check, immutable failure rollback and fail-closed unknowns.
- Only scalar backend implemented. NEON/SIMD/GPU/NUMA are described as candidate host capabilities; when requested and unavailable, fall back to scalar only under a verified scalar profile. Never claim hardware acceleration or speedup.

## Test + invariants

Host: make -C native/inventario_raf_v1/format_probe test
Cross-object: make -C native/inventario_raf_v1/format_probe objects  (Clang required; objects != APK)

Main invariant: same authorized input and verified backend must have the same normalized meaning. Meta-invariants: byte bounds, authority, generation, rollback, provenance, reproducibility and no silent claim promotion.

Falsify on malformed ELF/DEX/ZIP, zero/unknown confusion, backend unqualified, source unauthorized, budget exceeded and stale generation. External host controls wall-time/thermal watchdog, device permissions and serialized state commits; L0 does not.

## Stop / rollback

Status DRAFT/HOLD. Stop on missing source authority, output uncertainty, unsupported format, incomplete coverage or failed host/device verification. Rollback: close unmerged draft, leave all prior code and Drive contents untouched.

APK/ELF native launcher wiring, signing, dual-ABI installation, physical device and real GPU/NEON tests are TOKEN_VAZIO_NOT_EXECUTED. Full Kotlin/Compose app is unchanged. Existing scripts/termux/build_apkc_hermetic.sh is a separate RafPolimata-dependent NativeActivity bootstrap, not an independently proven freestanding full app.

SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM; claim_allowed=false; release_allowed=false.
