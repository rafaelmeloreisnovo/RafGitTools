# RAFCODE freestanding L0 → RafGitTools native bootstrap overlay (candidate)

Status: DRAFT/HOLD. The producer is `rafaelmeloreisnovo/ninja_Rafcode` PR #9 @ `f9822388a2909aa54ce4d959a861686bd96aab97`, tied to four exact source SHA256 values in `scripts/termux/build_apkc_rafb_auto.sh`. The pre-existing consumer `scripts/termux/build_apkc_hermetic.sh` is not modified, as the ApkC builder is governed by `RafPolimata`. Do not rebrand the borrowed builder as new RAFCODE code.

## Execution

Place a CLEAN checkout of the pinned producer commit alongside RafGitTools, plus the existing local RafPolimata checkout. Run `sh scripts/termux/build_apkc_rafb_auto.sh`. It invokes Clang+LLD directly for ARM32 and ARM64 (`-nostdlib`, freestanding C, no JNI/SDK/NDK) and then the pre-existing ApkC internal-assembly bootstrap generator in dual-ABI, unsigned mode. No network download, Gradle, SDK or NDK invocation is introduced by this bridge. The builder itself requires a host C compiler and Python 3 and is not a dependency-free build tool.

The host-side `rafb_merge_unsigned.py` creates a **separate** unsigned APK ZIP and inserts:
- `lib/armeabi-v7a/librafb.so`
- `lib/arm64-v8a/librafb.so`

Both bootstrap libraries are checked for ELF32/ARM or ELF64/AArch64. The adapter rejects missing/duplicate ABI, malformed ELF, unsigned-source naming mismatch, signed ZIP signatures, unsafe paths, source ZIP over 32 MiB and overwrites. It creates an append-only-style receipt for its new output, with exact hashes. Signing remains a separate explicit owner-controlled gate.

**Automatic ABI behavior:** both libraries are packed and Android's platform ABI selection chooses the matching native-library directory on install/load. This is not a single CPU-independent ELF and does not amount to bare-metal boot. The RAFCODE ELF gate itself selects among the host-authorized ABI mask; it cannot infer CPU capability from an ELF header alone.

## Boundaries & gaps

The ApkC `NativeActivity` bootstrap uses Android platform app/lifecycle services; ART/Android framework use by the operating system is not ruled out. No JNI or ART calls are introduced by the RAFCODE library. Bundling `librafb.so` does not invoke it: runtime wiring is **NOT IMPLEMENTED**. This overlay does not replace the full Kotlin/Compose app. No APK signing, 4/16KiB page alignment, installation, load-time symbols, device test, independent audit or 70KiB budget proof has been performed. Do not report those as completed.

Producer `OUTBOUND_LICENSE=TOKEN_VAZIO`, `TMD_EXPANSION=TOKEN_VAZIO`, `claim_allowed=false`. Do not merge consumer until producer is independently reviewed, the producer branch is promoted and rebased/pinned to its final exact source commit, and physical ABI/install/signature tests pass. CI test here uses synthetic ZIP+ELF fixtures only; synthetic zip has no installable manifest.

Rollback: close draft PR; original APK builder and main branch are unchanged. Output artifact has a distinct name, so deleting that user-generated output does not affect predecessor APKs.
