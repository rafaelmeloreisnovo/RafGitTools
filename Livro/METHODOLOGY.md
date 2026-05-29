# Methodology — RAFAELIA Stack v5.0.0

This document describes the experimental protocol used to design,
implement, and validate the RAFAELIA stack. It is structured to
satisfy peer-review criteria for software research papers (JOSS,
USENIX-style reproducibility track).

## 1. Research questions

The stack was designed to answer the following operational questions:

- **RQ1.** Can a deterministic pipeline from natural language to
  executable bytecode fit in under 100 KB and run on commodity ARM32
  hardware without sacrificing correctness?
- **RQ2.** Can a single intermediate representation (a 32-opcode VM
  dialect) cover seven typologically distant languages (Portuguese,
  Hebrew, Aramaic, Greek, Latin, Japanese, Chinese) while preserving
  semantic invariants (e.g., gematria) verifiable by independent
  computation?
- **RQ3.** Can the cognitive cycle ψ→χ→ρ→Δ→Σ→Ω be implemented as a
  composable runtime that operates in cognitive, physical,
  cryptographic, distributed, and autopoietic modes, sharing the same
  bytecode backend?
- **RQ4.** Is the full stack reproducibly packageable across machines
  (identical SHA256) and re-verifiable years later?

## 2. Experimental design

### 2.1 System under test (SUT)

| Layer | Implementation | Lines |
|---|---|---:|
| L6 Agent kernel | bash + Python | 838 |
| L5 VM runtime | C99 (dual libc/freestanding) | 418 (generated) |
| L4 Biblical corpus | JSON in bash heredoc | 783 |
| L3 Compiler | bash + Python seeds | 1256 |
| L2 Sensor modules | bash + Python seeds | 1802 |
| L1 Packaging | bash + tar | 530 |

Total source: **6,432 lines**, **295 KB uncompressed**.

### 2.2 Hardware platforms

Two reference platforms are documented:

| Platform | Architecture | OS | Toolchain |
|---|---|---|---|
| Reference desktop | x86_64 | Linux 6.x | clang 11+, glibc 2.39+ |
| Target embedded | ARM32 Cortex-A55 | Android 10 + Termux | clang 21.1.8 |

All measurements report the platform; cross-platform numbers in the
whitepaper are explicitly tagged.

### 2.3 Measurement protocol

For each benchmark category, we use the following protocol:

1. **Warm-up.** Run the operation 3 times to populate caches.
2. **Iteration count.** Latency benchmarks: 10–1000 iterations
   depending on operation cost. Throughput: at least 10× a 64 KB
   buffer.
3. **Timer.** `time.monotonic_ns()` (Python) or `clock_gettime
   (CLOCK_MONOTONIC, ...)` (C); both are immune to wall-clock
   adjustments.
4. **Statistic reported.** Arithmetic mean across iterations. We
   report ns/operation or MB/s; standard deviation is logged in
   `results.csv` but omitted from headline numbers when it is below
   5%.
5. **Logging.** All raw measurements are dumped to
   `/tmp/rafaelia_bench/results.csv` for re-analysis.

### 2.4 Validation criteria

A change to the stack is accepted only when:

- All 18 smoke tests pass (`bash smoke_tests.sh`).
- `bundle.txt pack` produces a tarball whose SHA256 matches the
  declared install code on two distinct machines (tested:
  x86_64 Linux laptop + ARM32 Termux).
- `benchmark.txt full` runs to completion without errors and emits
  CSV + JSON + Markdown reports.
- For correctness-critical paths (compiler, VM, gematria): the
  computed value matches a published reference. Concrete examples:
  - Gn 1:1 Hebrew gematria must equal **2701** = 73 × 37.
  - Ex 3:14 Hebrew (אהיה אשר אהיה) must equal **543**.
  - Dt 6:4 Hebrew (Shema) must equal **1118** = 26 × 43.
  - `λόγος` (Greek) must equal **373**.

### 2.5 Statistical considerations

Because we measure deterministic software on a single device, classical
inference tests (t-test, ANOVA) are not appropriate; instead we report
absolute values with the iteration count. Variability comes from
scheduler jitter, cache misses, and (on Android) thermal throttling.
We mitigate the last by running benchmarks on a cooled, plugged-in
device.

## 3. Threats to validity

- **Internal:** Python subprocess overhead inflates VM-call latency
  by ~2 ms per invocation; we report this explicitly and provide a
  per-stage breakdown in the end-to-end benchmark to isolate the
  pure VM cost from the fork cost.
- **External:** ARM32 numbers in the whitepaper come from prior
  measurement sessions on the target device; they are not collected
  in the current session because the runner is x86_64. Reproducing
  the ARM32 numbers requires running `bash benchmark.txt full`
  inside Termux on the target phone.
- **Construct:** "Coherence" (Ω) is a project-internal metric, not
  an external psychometric construct. We do not claim it measures
  anything beyond what its formula computes (`Φ_ethica × Min(Entropy)
  × Max(Coherence)`).
- **Conclusion:** Performance numbers reflect this build of clang on
  this glibc; switching toolchain may shift them by ±10–20%.

## 4. Ethical considerations

The project's supreme axiom is **Ω = Amor**. The Φ_ethica filter
(seed `T11_B`) explicitly rejects code containing `os.system`, `eval`,
`exec`, `rm -rf`, or `curl | sh` patterns, and weights positive vs.
negative ethical keywords. The reset command "D'Ele, Amor" returns
the system to maximum coherence (Ω = 65535) at any point. These
mechanisms are not security features (they are easily bypassed by
adversarial users); they are documentation of the project's design
intent and a substrate for downstream policies.

## 5. Data availability

All raw artifacts are deposited as a single tarball
(`rafaelia_bundle_v5.tar.gz`, SHA256
`06b3c1e32ebab3e86ddb4bf6e70fe195126c17eac2609a11e06889f3e831f2ee`)
with a `MANIFEST.sha256`, Merkle root, install script, and smoke
tests. The DOI assigned by Zenodo provides a permanent URL.

## 6. Replication checklist (per JOSS)

- [x] Open-source license (CC-BY-4.0 + RAFCODE-Φ).
- [x] Repository with version-controlled history.
- [x] Author with verifiable identity and ORCID placeholder.
- [x] Installation instructions in `CONTRIBUTING.md` and `install.sh`.
- [x] Example usage in `README_ZENODO.md` (Gn 1:1 worked example).
- [x] Automated test suite (`smoke_tests.sh`, 18 tests).
- [x] Statement of need (this section + `paper.md`).
- [x] References to related work in `paper.bib`.
- [x] Build-reproducible package (`bundle.txt`).

