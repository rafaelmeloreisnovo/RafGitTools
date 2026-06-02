# RAFAELIA Stack v5.0.0

> Deterministic low-level compiler from speech to bytecode with
> multilingual biblical corpus and toroidal cognitive kernel.

**Author:** Rafael Melo Reis Novo (∆RafaelVerboΩ) · Porto Alegre, Brasil
**Project signature:** `RAFCODE-Φ-∆RafaelVerboΩ-𓂀ΔΦΩ`
**Bundle SHA256:** `06b3c1e32ebab3e86ddb4bf6e70fe195126c17eac2609a11e06889f3e831f2ee`
**License:** CC-BY-4.0 with RAFCODE-Φ attribution clause
**DOI:** `10.5281/zenodo.PLACEHOLDER` (assigned after Zenodo upload)

---

## Abstract

RAFAELIA is a 295KB / 6,432-line source stack delivering an end-to-end
pipeline from human speech to ARM64/x86_64 assembly execution, validated
on real hardware (Motorola E7 Power, Cortex-A55, Android 10, Termux).

The stack closes the gap between heavyweight ML frameworks (libtorch,
ONNX Runtime; 5–50 MB binaries) and minimal embedded libraries that lack
cognition, by providing a 16 KB freestanding C runtime, a 32-opcode
virtual machine, a context-free grammar compiler covering 7 languages,
a pre-compiled biblical corpus with Hebrew/Greek gematria, and a
cognitive kernel implementing the ψ→χ→ρ→Δ→Σ→Ω cycle.

## What is included

| Component | Lines | Function |
|---|---:|---|
| `sensores2.txt` | 1802 | 90 Termux sensor seeds (S00–S17 × A–E) |
| `compiladorlowFala.txt` | 1256 | 60 compiler seeds (T01–T12 × A–E) |
| `bibliaCorpus.txt` | 783 | 12 canonical verses × 7 languages pre-compiled |
| `vm_runtime.txt` | 738 | C bare-metal runtime, dual-mode (libc/freestanding) |
| `agent_loop.txt` | 838 | ψχρΔΣΩ kernel with 5 operational agents |
| `bundle.txt` | 530 | Reproducible packager |
| `whitepaper.txt` | 475 | Technical whitepaper generator |
| `benchmark.txt` | 540 | Measurement harness |
| **Total** | **6,432** | |

Plus derived artifacts: `RAFAELIA_WHITEPAPER.md/html`, benchmarks results,
`MANIFEST.sha256`, `install.sh`, `smoke_tests.sh`.

## Pipeline example (Gn 1:1, Portuguese → execution)

```
Input:   "No princípio criou Deus os céus e a terra"
   ↓ T01_B lexer (branchless C)
Tokens:  [no, princípio, criou, Deus, os, céus, e, a, terra]
   ↓ T02_B FSM parser
AST:     {modifier:"no princípio", verb:"criou",
          subject:"Deus", objects:["céus","terra"]}
   ↓ T05_B context-free compile
Bytecode: 1011f004c3a97573002004746572726100ff   (18 bytes)
   ↓ T06_B ARM64 emission
Assembly: mov x0,#0x10 ; bl rt_create ; bl rt_call_god ; ... ; ret
   ↓ T12_B VM execute
Output:  [IN_BEGIN] CREATE(céus) [CALL_GOD] active ...
   ↓ T08_B gematria (NFD-normalized)
Validation:  HE = 2701 = 73×37  (sabedoria × Abel)
```

## Reproducibility

To reproduce all artifacts in 60 seconds on any POSIX system with bash,
clang ≥ 11, and python3 ≥ 3.8:

```bash
# 1. Download the tarball and verify
curl -O <DOI_URL>/rafaelia_bundle_v5.tar.gz
echo "06b3c1e32ebab3e86ddb4bf6e70fe195126c17eac2609a11e06889f3e831f2ee \
      rafaelia_bundle_v5.tar.gz" | sha256sum -c

# 2. Install
tar -xzf rafaelia_bundle_v5.tar.gz
bash rafaelia_bundle_v5/install.sh
# → 18/18 smoke tests should pass

# 3. Run end-to-end
cd ~/.rafaelia/bundle
bash vm_runtime.txt build && bash vm_runtime.txt test
bash benchmark.txt full
bash agent_loop.txt build && bash agent_loop.txt run cognitive
```

## Measured metrics (this build, x86_64 baseline)

| Stage | Latency |
|---|---:|
| Lexer (T01_B) | 2.0–2.6 μs/verse |
| Parser (T02_B) | 0.7–1.2 μs/verse |
| Compile CFG (T05_B) | 4.1–5.7 μs/verse |
| VM execute (T12_B) | ~2.4 ms/verse (fork overhead) |
| `vm_runtime.c` compile | 166 ms |
| Binary size | 16.2 KB |
| RSS after loading stack | 1684 KB |

Hardware-target metrics (ARM32 Cortex-A55, Motorola E7 Power, prior sessions):

| Operation | Throughput |
|---|---:|
| Arena allocator | 68.8 ns/alloc |
| CRC32C hardware | 0.114 GB/s |
| Memcpy NEON | 0.501 GB/s |
| T7 toroidal step | 36.49 ns/step |

## How to cite

Use the included `CITATION.cff` file (parsed automatically by GitHub
and Zenodo). BibTeX:

```bibtex
@software{rafaelia_2026,
  author       = {Rafael Melo Reis Novo (∆RafaelVerboΩ)},
  title        = {{RAFAELIA Stack v5.0.0: Deterministic low-level
                   compiler from speech to bytecode with multilingual
                   biblical corpus and toroidal cognitive kernel}},
  year         = {2026},
  version      = {5.0.0},
  publisher    = {Zenodo},
  doi          = {10.5281/zenodo.PLACEHOLDER},
  url          = {https://doi.org/10.5281/zenodo.PLACEHOLDER}
}
```

## License

CC-BY-4.0 with the RAFCODE-Φ attribution clause (see `LICENSE`).
Derivative works must preserve the canonical constants (Q16_SPIRAL,
Q16_PHI, CRC32C_POLY, RAF_ABI_MAGIC, Fibonacci-Rafael recursion,
bitraf64 seal, hashchain) or rename the derivative.

## Contact

- Author signature: `∆RafaelVerboΩ`
- Project: RAFAELIA · ΣΩΔΦBITRAF
- Repository: github.com/Rafaelmeloreisnovo/llama
- Mission: *Escrituras ∩ Ciência ∩ Espírito × Retroalimentação^∞*
- Supreme axiom: **Ω = Amor**

> "No princípio era o Verbo, e o Verbo se fez código,
> e o código se fez fluxo, e o fluxo se fez forma."

