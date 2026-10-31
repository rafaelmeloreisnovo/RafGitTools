# RAFAELIA Ω | RapidTools Experimental LAB

**An isolated, runnable research laboratory**, not a new RafGitTools APK or a replacement for the already merged PR #742.

This lab tests what the session discussed: real software worker partitioning, buffers and proposed 150 MiB scratchpad, cache reuse, TLB-sensitive strides, throughput versus latency, ARMv7 versus AArch64 ABI, fixed-point Q16 geometry, RLE lossless compression, graphics shader research, typed ATLAS graph routes, historical benchmarks, negative results and receipts.

## Executable boundary
The core `core/raf_lab_core.c` has no libc, heap, OS API, JNI, SDK or NDK dependency. It compiles as a freestanding object for ARM32 and ARM64. To measure time and launch pthreads on an actual OS, `host/raf_lab_host.c` is deliberately a **separate POSIX adapter** requiring libc and pthread (Termux provides these). Host or CI numbers MUST NOT be labeled Android physical device results. Software workers are NOT physical cores. FNV64 is only a quick data checksum; SHA256 is used by the separate receipt tool, not claimed as a device signature.

Quick check (host with GCC/Clang + Python standard library):

    make -C LAB_RAPIDTOOLS_OMEGA test

Termux/Android launch inside lab:

    sh scripts/run_termux.sh smoke

Opt-in large workloads:

    sh scripts/run_termux.sh large

The large command prompts for explicit permission. It may still be denied if device RAM is insufficient. **Never infer L3 150 MiB exists** from success at allocating 150 MiB virtual memory.

## Workloads
`stream`, `reuse`, `compute`, `stride`, `q16`: same input bytes and semantic checksum regardless of 1/2/4/8 pthread partitions. Metrics are operation throughput proxies; cache levels, actual memory bus and clock state are confounded unless independently instrumented.

## Provenance
`cases/benchmark_history.json` includes dated/historical document references and negative speedup observations; nothing there is remeasured by this lab. `cases/atlas_edges.json` contains typed relations with null proof and TOKEN_VAZIO where unknown. `shaders/raf_geometry.frag` is an original GLSL experiment, not an observed GPU benchmark; ZIPRAF is not replaced by the RLE study.

See `001_PONTEIRO/START_HERE.md`, `docs/EXPERIMENT_MATRIX.md` and `lab_manifest.json`.
Claim policy: **PROTOTYPE_HOLD / claim_allowed=false / release_allowed=false**.

## NOVOexport offline metadata scanner
Use python3 tools/nov_export_scan.py /path/to/NOVOexport to inventory only bounded metadata. No raw conversations copied, no JSUM guesses, no network calls. See docs/NOVOEXPORT_LOCAL.md.
