# Contributing to RAFAELIA Stack

Thank you for considering contributing to RAFAELIA. This document
describes how to reproduce all artifacts, propose changes, and validate
that your changes preserve the reproducibility guarantees.

## 1. Prerequisites

| Tool | Minimum version | Why |
|---|---|---|
| bash | 4.0+ | All artifact generators are bash scripts |
| clang or gcc | clang 11+ / gcc 10+ | Compile `vm_runtime.c` |
| python3 | 3.8+ | Benchmarks, gematria with NFD, JSON tooling |
| tar | GNU tar | `--sort`, `--mtime`, `--owner` flags |
| sha256sum | coreutils | Manifest verification |
| jq (optional) | any | Pretty-printing JSON outputs |

For Termux on Android: `pkg install bash clang python3 jq termux-api`.

## 2. Reproducing the entire stack from scratch

```bash
git clone https://github.com/Rafaelmeloreisnovo/llama
cd llama

# 1. Generate the bundle
bash bundle.txt pack
# → produces rafaelia_bundle_v5.tar.gz with deterministic SHA256

# 2. Verify the SHA256
sha256sum rafaelia_bundle_v5.tar.gz
# expected: 06b3c1e32ebab3e86ddb4bf6e70fe195126c17eac2609a11e06889f3e831f2ee

# 3. Install + smoke
tar -xzf rafaelia_bundle_v5.tar.gz
bash rafaelia_bundle_v5/install.sh
# → 18/18 smoke tests should pass

# 4. Run benchmarks (produces fresh metrics)
bash benchmark.txt full
# → patches whitepaper with current measurements
```

## 3. Reproducibility checklist

A change is reproducible when:

- [ ] All 18 smoke tests pass (`bash smoke_tests.sh`)
- [ ] `MANIFEST.sha256` hashes match files (`bash bundle.txt verify`)
- [ ] `bundle.txt pack` produces a tarball with the same SHA256 on
      two different machines (try Linux laptop + Termux on ARM)
- [ ] `benchmark.txt full` completes without error and emits
      `results.csv`, `results.json`, `bench_section.md`
- [ ] `bibliaCorpus.txt demo` shows gematria HE = 2701 for V01
- [ ] `vm_runtime.txt build && vm_runtime.txt test` outputs V01–V12

## 4. Proposing changes

### Code changes

1. Fork the repository.
2. Make changes on a topic branch (`feature/...` or `fix/...`).
3. Run the full reproducibility checklist locally.
4. Update `CHANGELOG.md` with a new entry under `## [Unreleased]`.
5. Open a pull request describing:
   - What changed
   - Which smoke tests are affected
   - Old vs new bundle SHA256
   - Any breaking changes to the ABI (`RAF_ABI_MAGIC`, opcodes)

### Documentation / paper changes

The whitepaper is **auto-generated** from `whitepaper.txt`. Do not edit
`RAFAELIA_WHITEPAPER.md` directly; instead edit the generator and run:

```bash
bash whitepaper.txt full
bash benchmark.txt full   # auto-patches the whitepaper with fresh data
```

## 5. Adding a new artifact

A new artifact `myartifact.txt` is integrated when:

1. It declares its purpose in the header comment.
2. It is added to the `ARTIFACTS=(...)` array in `bundle.txt`.
3. A smoke test is added to `smoke_tests.sh` (typically a syntax check
   plus a content-count assertion).
4. `bundle.txt pack` succeeds and the new SHA256 is recorded.
5. `CHANGELOG.md` is updated.

## 6. ABI stability

The following are **frozen** and cannot change without a major version bump:

- 32 opcodes of RAFAELIA-VM (`0x00`..`0xFF`)
- Canonical constants: `Q16_SPIRAL`, `Q16_PHI`, `CRC32C_POLY`,
  `RAF_ABI_MAGIC`, `PHI64`
- Bytecode encoding (variable-length, `PUSH_OBJ` null-terminated)
- bitraf64 string and 15+ hashchain blocks
- Project signature `RAFCODE-Φ-∆RafaelVerboΩ-𓂀ΔΦΩ`

## 7. Coding style

- **Bash:** strict mode encouraged (`set -euo pipefail` at function level).
- **C:** freestanding-friendly, no `malloc`, no heap on hot paths,
  branchless when possible. Style is K&R-ish with 2-space indent.
- **Python:** PEP-8, type hints encouraged in new code.
- **Markdown:** GFM, 72-char lines for paragraphs, fenced code blocks.

## 8. Ethical guidelines (Φ_ethica)

Per the project's supreme axiom **Ω = Amor**, contributions should:

- Avoid code that facilitates targeted harm or surveillance of
  non-consenting individuals.
- Preserve the project's open and contemplative character —
  performance optimizations are welcome; surveillance backdoors are not.
- When in doubt, run the change through `seed_T11_B` (rules-based
  Φ_ethica checker) and ensure `phi >= 0`.

## 9. Contact

- Author signature: `∆RafaelVerboΩ`
- Email: (via GitHub profile)
- Repository: https://github.com/Rafaelmeloreisnovo/llama
- DOI (after first deposit): https://doi.org/10.5281/zenodo.PLACEHOLDER

---

**Mission:** *Escrituras ∩ Ciência ∩ Espírito × Retroalimentação^∞*
**Master reset command:** "D'Ele, Amor"
**Selo:** `RAFCODE-Φ-∆RafaelVerboΩ-𓂀ΔΦΩ`
**Ω = Amor**
