---
title: 'RAFAELIA: A deterministic low-level compiler from speech to bytecode with multilingual biblical corpus and toroidal cognitive kernel'
tags:
  - C
  - bash
  - python
  - bare-metal
  - ARM
  - compiler
  - low-level
  - virtual machine
  - multilingual NLP
  - biblical corpus
  - gematria
  - toroidal geometry
  - embedded AI
authors:
  - name: Rafael Melo Reis Novo
    orcid: 0000-0000-0000-0000
    corresponding: true
    affiliation: 1
affiliations:
  - name: Independent Researcher, Porto Alegre, Brazil
    index: 1
date: 29 May 2026
bibliography: paper.bib
---

# Summary

`RAFAELIA` is a 295 KB, 6,432-line source stack that implements an
end-to-end pipeline from natural language to executable assembly on
resource-constrained hardware. Given a sentence in any of seven
languages (Portuguese, Hebrew, Aramaic, Greek, Latin, Japanese,
Chinese), the pipeline produces tokens, an abstract syntax tree, a
compact bytecode representation in a 32-opcode virtual machine
dialect, and ARM64 / x86_64 assembly that runs on a 16 KB freestanding
C runtime. The stack has been validated end-to-end on a low-cost
Android device (Motorola E7 Power, ARM Cortex-A55) inside the Termux
environment.

Beyond the compiler itself, `RAFAELIA` provides: (i) a pre-compiled
corpus of 12 canonical biblical verses in seven languages with
Hebrew and Greek gematria normalized via Unicode NFD, useful as a
multilingual benchmark; (ii) a cognitive kernel implementing a
ψ→χ→ρ→Δ→Σ→Ω cycle in five operational modes (cognitive, physical,
cryptographic, distributed, autopoietic); (iii) 90 reusable Termux
sensor seeds and 60 compiler seeds, each designed to be expanded by
any large language model into a complete implementation; and (iv) a
reproducible packaging system that produces byte-identical tarballs
across machines via deterministic `tar` flags.

# Statement of need

Modern embedded systems sit between two extremes. On one end,
heavyweight machine-learning runtimes such as `libtorch` and ONNX
Runtime [@onnxruntime] occupy 5–50 MB and assume megabytes of RAM
plus accelerator hardware. On the other end, micro-libraries such as
`micro-ROS` or bare-metal C SDKs provide deterministic execution but
no high-level cognition. There is a missing middle for researchers
who need: (a) deterministic and auditable execution on commodity ARM
hardware (Android phones, single-board computers), (b) multilingual
text processing with cryptographically verifiable inputs and outputs,
and (c) a software substrate that can host both symbolic and
probabilistic reasoning without dragging in a Python/PyTorch
dependency tree.

`RAFAELIA` fills that gap. The 16 KB C runtime executes a fixed
32-opcode VM dialect; the compiler is implemented as a context-free
grammar producing variable-length bytecode; the corpus, kernel and
sensor modules are pure bash + Python and can be expanded by any
language model into full implementations using the provided seed
abstractions. All artifacts are reproducibly packaged: identical
SHA256 across machines is guaranteed by `tar --sort=name
--mtime='2025-01-01 UTC' --owner=0 --group=0 --numeric-owner`.

Empirically, on x86_64 we measured: tokenization 2.0–2.6 μs per
verse, parsing 0.7–1.2 μs, bytecode emission 4.1–5.7 μs, VM
compilation 166 ms (16.2 KB binary), and a working-set memory
footprint of 1.7 MB after loading the full stack. On the target
ARM32 Cortex-A55 we previously measured 68.8 ns per arena
allocation, 0.114 GB/s for hardware-accelerated CRC32C
[@castagnoli1993], 0.501 GB/s for NEON memcpy, and 36.49 ns per step
of a seven-dimensional toroidal state update.

The cognitive kernel implements the free-energy-inspired cycle ψ
(intention) → χ (observation) → ρ (noise filtering) → Δ
(transmutation into bytecode) → Σ (persisted memory) → Ω
(coherence-aligned response) [@friston2010]. The five-mode design
allows the same kernel to be deployed as a single-device cognitive
agent, a low-power physical-only monitor, a cryptographically signed
audit chain, a distributed mesh over UDP, or an autopoietic system
that synthesizes new rules from observation clusters.

# Pipeline example

The opening verse of Genesis (Gn 1:1) in Portuguese, *"No princípio
criou Deus os céus e a terra"*, is processed as follows. The
branchless C lexer classifies bytes via a 256-entry table; the
finite-state parser extracts a modifier ("no princípio"), a verb
("criou"), a subject ("Deus"), and two objects ("céus", "terra").
The context-free compiler emits eighteen bytes:
`10 11 F0 04 c3 a9 75 73 00 20 04 74 65 72 72 61 00 FF`, where `0x10`
is `IN_BEGIN`, `0x11` is `CREATE`, `0xF0` is `CALL_GOD`, `0x04` is
`PUSH_OBJ` followed by a UTF-8 null-terminated string, `0x20` is
`LINK_AND`, and `0xFF` is `SEAL_VERSE`. The VM dispatcher runs the
sequence and produces the expected trace. As a cross-check, the
Hebrew form of the verse has gematria 2701, which factors as 73 × 37
(the Hebrew words for *wisdom* and *Abel*).

# Verification and reproducibility

The bundle ships with a `MANIFEST.sha256` file, a Merkle root over
all artifacts, an `install.sh` that verifies hashes before extraction,
and 18 smoke tests covering syntax, content counts, hash integrity,
and cross-artifact integration. Reproducing all artifacts from
scratch takes roughly 60 seconds on a modern laptop with `bash`,
`clang ≥ 11`, and `python3 ≥ 3.8`.

# Acknowledgments

The Hebrew gematria values were cross-verified against published
mispar hechrachi tables; Greek gematria (isopsephia) was verified
against `λόγος = 373`, `ἀγάπη = 93`, and `Ἰησοῦς = 888`. The
toroidal-geometry framework was inspired by Friston's free-energy
principle [@friston2010] and uses the spectral techniques of
Goertzel [@goertzel1958] for harmonic resonance detection in the
microphone-input seeds. The orientation-estimation seeds use the
Madgwick filter [@madgwick2010].

# References
