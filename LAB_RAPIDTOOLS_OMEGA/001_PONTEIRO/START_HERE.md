# START HERE — LAB_RAPIDTOOLS_OMEGA (not the Android application)

## Router
INTENT -> CURRENT_STATE -> SOURCE_MIN -> AUTHORITY -> ACT -> TEST -> EVIDENCE -> RECEIPT -> R3.

## Locate
* Producer code: `LAB_RAPIDTOOLS_OMEGA/` in rafaelmeloreisnovo/RafGitTools **experimental branch**.
* Historical parent: PR #741 and PR #742 (both merged); **do not claim their source gates or CI are green**.
* Official Drive (private documentary custody): `Aplicações Rafael IA / RafGitTools / LAB_RAPIDTOOLS_OMEGA — ARM32 ARM64 Atlas/`.
* Catalog of 90 **references**, not 90 binaries: `../app/src/main/kotlin/com/rafgittools/benchmark/RafApplicationsCatalog.kt`.
* Experimental executor: `core/raf_lab_core.c` (freestanding), `host/raf_lab_host.c` (POSIX/Termux adapter), `Makefile`, `tools/run_lab.py`.
* Experiments: buffers, stride, workers, Q16, RLE, shader source, typed ATLAS graph.
* Reference baselines: `cases/benchmark_history.json` (read-only historical).
* Evidence: `build/receipts/*.json` and a SHA-256 hash chain (LOCAL, not externally signed).
* Tracked control state: `lab_manifest.json`, `contracts/claim_policy.json`.

## Gates
1. Unit semantics same across 1,2,4,8 worker partitions; RLE roundtrip; Q16 bounds.
2. Build with host C compiler then cross-compile pure core objects for ARMv7 and AArch64; cross compilation is not physical execution.
3. Physical device ARM32 and ARM64 runs are needed before any speedup comparisons. Use repeated runs, thermal/battery telemetry, same dataset and counterexamples.
4. Large 64/150 MiB require explicit operator flag, and adapter checks available RAM. Do not launch on low-RAM devices by default.
5. Shader GPU testing, B7 native compatibility, ZIPRAF compatibility and cross-repo agents are intentionally BLOCKED until sources, permissions and tests exist.
6. CI failure, skipped Android, unknown authority or missing provenance -> HOLD.

## Boundary
This lab does not modify AndroidManifest, launchers, K-Secrets, PR status, repo policies or release artifacts.
No merge, auto agent dispatch, secrets, hardware attestations or scientific promotion.
SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM; TOKEN_VAZIO != 0.
Rollback: close the lab draft without merge. No historical deletion.
