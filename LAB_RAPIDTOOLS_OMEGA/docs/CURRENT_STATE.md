# CURRENT_STATE — RAPIDTOOLS Ω LAB — 2026-10-31

Origin: prior discussion about applying ARM32/ARM64, memory banks, buffers, 150 MiB, NEON, branchless Q16, negative optimization, RLE/ZIPRAF, shaders, ATLAS semantics, provenance, receipts, and app credits. Original PR #741/#742 already merged and NOT a substitute for this experiment.

Current lab stage: PROTOTYPE_HOLD.
* Actual executable C source: five synthetic modes; pthread adapter + freestanding core.
* Intended tests: C unit, Python graph/custody tests, smoke matrix 5 modes x 4 software workers.
* ABI cross build: object-only and separate device physical runs. Compiler presence must be checked.
* Source graphs and historical benchmarks: typed and not promoted.
* GPU/shader: source only; no GPU run.
* 150 MiB: experimental mmap with explicit consent and available RAM guard, not hardware L3.
* Device/thermal/current toolchain facts: TOKEN_VAZIO until exact run.

HOLD until exact-head compilation/test readback and physical test receipts. No auto merge and no published performance claims. App code remains unchanged by this LAB tree.
