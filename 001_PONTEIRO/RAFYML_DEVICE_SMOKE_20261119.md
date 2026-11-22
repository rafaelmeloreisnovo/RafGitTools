# RAFYML L0 — opt-in Linux/Android ARM device smoke kit (2026-11-19)

## Authority, sources, and predecessor

- Producer `rafaelmeloreisnovo/RafGitTools` (public source + synthetic fixtures only); `AGENTS.md` local governs code; Mapa controls federation routing; Drive remains state/receipt custodian.
- Stacked successor of [PR #829](https://github.com/rafaelmeloreisnovo/RafGitTools/pull/829), exact predecessor `78f1c68811c27ffa1d2c85b94a61b3c808b134b0` (Draft/HOLD at write). Independent static-link proof: [CI job #135908567664](https://github.com/rafaelmeloreisnovo/RafGitTools/actions/runs/44622782695/job/135908567664); predecessor START [#44622782635](https://github.com/rafaelmeloreisnovo/RafGitTools/actions/runs/44622782635) completed success. Neither establishes device runtime.
- This is a **test-only Linux EABI adapter** layered on the unmodified `src/rafyml_runtime.c` and `include/rafyml_runtime.h`. No JNI, NDK, SDK Android, Gradle, Java or libc in the compiled ARM smoke ELF; shell/compiler/GitHub Actions are hosted build/test adapters.

## What is tested (PIRC-2)

- `tests/rafyml_device_smoke_l0.c`: a valid RAFYML document, a four-ASCII-space string with NUL termination, an invalid child index, a missing-string offset and explicit exit codes 11–15 for failure (exit 0 only if every condition succeeds).
- `__arm__` Linux EABI exit syscall (`r7=1`) or `__aarch64__` Linux exit syscall (`x8=93`) is confined to this **test adapter**; the production C parser has no new syscalls. No root, network, storage or external API use.
- `scripts/rafyml/build_device_smoke_l0.sh` builds host ASan/UBSan fixture, ARMv7 ELF32 and AArch64 ELF64, verifies headers/ELF type/symbols, rejects wrong-architecture host execution and emits `SHA256SUMS`.
- `.github/workflows/ci-rapidtools-semantic-l0.yml` adds one exact-head job in the existing **nine-workflow** topology. Existing RapidTools, multi-ABI object and static-link jobs remain intact. Artifact `rafyml-device-smoke-<exact HEAD>` is a short-lived test kit, not a release or signed package.
- `scripts/rafyml/run_device_smoke_l0.sh` verifies the local package checksum and `uname -m` before running on a device. It appends only UTC/ABI/file SHA/exit/status (no device IDs). SHA-256 alone is not an authenticated publisher signature.

## Device gate [N5 until executed]

1. Download only the exact-head CI artifact from the GitHub Actions run, inspect the files, and extract under Termux `$HOME`, not shared Downloads.
2. From the extracted directory run `sh run_device_smoke_l0.sh` manually. The selected ELF exits with status `0` on the fixture checks; a nonzero exit, permission denial, loader/SELinux failure, checksum failure or unsupported ABI is a **negative result**, not PASS.
3. Privately inspect `device_receipts.log`; do not upload system information or private files to public CI. Do not claim E2E unless a real device/ABI/exit/byte hash, OS and tool version are independently bound to a receipt.
4. ARM64 success does not imply ARM32 compatibility; Android version, Linux ABI and loader support vary. No universal physical performance, APK or production claim.

## 6Σ / adversarial, rollback, and gaps

- Define: one bounded negative/positive runtime exercise. Measure: exact source HEAD + host build + artifact bytes. Analyze: source vs host vs device. Improve: opt-in test adapter only. Control: fail-closed SHA + ABI + exit code and append-only result. No statistical Six Sigma capability asserted.
- Falsifiers: sanitizer fails, invalid ELF, missing/extra runtime imports, wrong ISA accepted, SHA mismatch accepted, impossible semantic fixture passes, or Android entry crashes. `TOKEN_VAZIO_UTIL` is never converted into `PASS` by a successful build.
- N0 after CI: only hosted build/fixture verification/artifact creation. N5 before device execution: ARM physical exit, Android loader, SELinux policy, stack footprint, resource use, power, validation across devices, system-wide runtime E2E.
- Rollback: close stacked Draft PR without merge. Its predecessor PR #829 and `main` are left intact; after merge only reviewed compensating successors may revise published source.

R3 = ⟨F_ok: prepared opt-in harness and bounded hosted evidence; F_gap: device/ABI runtime and human gate; F_next: exact-head CI+artifact readback followed by voluntary device execution⟩.

`SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM` ; `claim_allowed=false`.
