# Experimental matrix (scoped, falsifiable)

| Suite | Executable? | What this tests | What it does NOT establish |
|---|---|---|---|
| ARM32/ARM64 | freestanding core compile; physical runs pending | same algorithm can be built for both ABIs | same runtime speed, CPU feature set |
| BUFFER 0.25/2/16/64/150 MiB | yes (high-risk gated) | working-set capacity and effective throughput | physically present 150 MiB L3/cache |
| WORKERS 1/2/4/8 | yes, pthread adapter | partition/correctness, potential concurrency | core count, GPU cores, true parallel speedup without repeated controls |
| STRIDE 4093 | yes | access-order sensitivity | uniquely identifies TLB versus cache |
| Q16 sqrt3over2=56756 | yes | fixed integer projection invariance | scientific law or physical proof |
| RLE | yes | bounded lossless run-length encode/decode | ZIPRAF historical codec compatibility |
| SHADER | GLSL source only | geometric visual projection | GPU compilation/device FPS or shader speedup |
| ATLAS | Python standard library | evidence-aware semantic routes | proof of historical cross-domain analogies |
| Receipt | local SHA-256 chain | accidental mutation of stored local events | prevention of malicious full-chain rewrite |

Physical falsifiers: reset seeds, repeat >=7 per setting, randomized ABBA trial order where possible, CPU governor/temperature/frequency logging, raw log+hashes and comparing ARMv7/ARM64 on actually identified devices. Don't select a fast run and call it a speedup.

Historical negatives preserved: BLAKE3 fork underperformed upstream and EDGE V7 unroll4/unroll8 underperformed BASE. Do not optimize against the history by omitting those counterexamples.

References K001–K090 remain pointers to the existing Android app catalog. These are not 90 implemented lab experiments. Modules S01–S09 are scoped suites, with observed states in lab_manifest.json.

Known gaps: exact hardware cache hierarchy, native NEON kernel parity, B7 real code authority, GPU runtime, off-device receipt anchoring, all-device comparability and exact Android JIT interactions.
