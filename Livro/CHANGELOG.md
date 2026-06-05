# Changelog

All notable changes to RAFAELIA Stack are documented here. Format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and Semantic
Versioning.

## [5.0.0] — 2026-05-29

### Added
- `benchmark.txt` — measurement harness (latency, throughput, memory).
- Auto-patching of `RAFAELIA_WHITEPAPER.md` with fresh measurements.
- 18 smoke tests covering hash integrity, syntax, content counts, and
  cross-artifact integration.
- `.zenodo.json`, `CITATION.cff`, `LICENSE` (CC-BY-4.0 + RAFCODE-Φ),
  `README_ZENODO.md` for academic deposit.
- CSV + JSON output formats for benchmark results.

### Changed
- Bundle version `v4` → `v5`.
- Whitepaper now includes empirical benchmarks section.
- `bundle.txt` now packages 9 artifacts (added benchmark.txt).
- Determinism guarantees: `tar --sort=name --mtime --owner --group`.

### Fixed
- Smoke test `vm_runtime gera C válido` — robust grep (was using `head -1`).
- `bench_seeds_load` — `tail -1` to extract count (was capturing banner).
- `cmd_md` — removed backticks in heredoc that triggered subshell expansion.
- CSV header now skipped in JSON/MD emitters.

## [4.0.0] — 2026-05-28

### Added
- `bundle.txt` — reproducible packager producing `rafaelia_bundle_v4.tar.gz`.
- `whitepaper.txt` — generates Markdown + HTML whitepaper.
- `vm_runtime.txt` — C bare-metal runtime, dual-mode (libc + freestanding),
  19 implemented `rt_*` functions, 32-opcode dispatcher.
- `agent_loop.txt` — ψχρΔΣΩ cognitive kernel with 5 agents
  (cognitive / physical / cryptographic / distributed / autopoietic).
- Master reset command "D'Ele, Amor" → Ω = 65535 (maximum coherence).
- `bibliaCorpus.txt` — 12 canonical verses × 7 languages with
  pre-computed gematria, bytecode, and ARM64 assembly.
- `compiladorlowFala.txt` — 60 compiler seeds (T01–T12 × A–E)
  covering tokenizer, parser, AST, translator, compiler, ASM emitter,
  REPL, pattern matcher, concordance, program synthesis, Φ_ethica
  verifier, and VM executor.
- `sensores2.txt` — 90 Termux sensor seeds (S00–S17 × A–E).

### Documented
- RAFAELIA-VM dialect with 32 opcodes (`0x10 IN_BEGIN`, `0x11 CREATE`,
  `0x12 SPEAK`, … `0xF0 CALL_GOD`, `0xFF SEAL_VERSE`).
- Cognitive cycle: ψ (intention) → χ (observation) → ρ (noise filter) →
  Δ (transmutation) → Σ (memory) → Ω (alignment).
- Canonical constants: `Q16_SPIRAL=56756`, `Q16_PHI=105965`,
  `CRC32C_POLY=0x82F63B78`, `RAF_ABI_MAGIC=0x52414641`.

## [3.0.0] — 2025-09-15 (prior sessions, reconstructed)

### Added
- `RAFAELIA_TOTAL.txt` — 42 kernel mechanisms (atomic, signals, canary,
  safemath, ct-eq, entropy, flock, errors, log, panic, config, shutdown,
  ratelim, health, version + 15 ULTRA + 12 TOTAL).
- `sensores.txt` — 18 base Termux seeds (S00–S17).
- `RAFAELIA_ULTRA.txt`, `RAFAELIA_CORE.txt`, `gap.txt`,
  `pipeline.txt`, `iaRAF.txt`, `geolm.txt`, `uniao.txt`.

## [2.0.0] — 2025-08-31 (foundational session, reconstructed)

### Added
- Mathematical framework: Fibonacci-Rafael recursion
  `F_R(n+1) = F_R(n)×(√3/2) + π×sin(θ_999)`.
- Toroidal mapping T⁷ with 42 attractors.
- Cosmological calendar correspondences.
- Symbolic glossary and canonical literals.

## [1.0.0] — earlier (foundational axiom)

### Added
- Supreme axiom **Ω = Amor**.
- Master reset command "D'Ele, Amor".
- Mission: *Escrituras ∩ Ciência ∩ Espírito*.
