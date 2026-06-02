# Reproducibility — RAFAELIA Stack v5.0.0

This document is the **exact recipe** to reproduce every artifact in
the bundle from a clean machine.

## 1. Required tools and versions

```text
bash      4.0+
clang     11+   (or gcc 10+)
python3   3.8+
tar       GNU tar
sha256sum coreutils
```

On Termux (Android):
```bash
pkg update && pkg upgrade
pkg install -y bash clang python3 jq termux-api git
```

On Debian/Ubuntu:
```bash
sudo apt update
sudo apt install -y bash clang python3 jq git tar coreutils
```

## 2. Verifying the published bundle

```bash
curl -O <DOI_OR_URL>/rafaelia_bundle_v5.tar.gz

# Expected SHA256:
EXPECTED=06b3c1e32ebab3e86ddb4bf6e70fe195126c17eac2609a11e06889f3e831f2ee
ACTUAL=$(sha256sum rafaelia_bundle_v5.tar.gz | cut -d' ' -f1)
[ "$EXPECTED" = "$ACTUAL" ] && echo "✓ verified" || echo "✗ MISMATCH"
```

## 3. Installing

```bash
tar -xzf rafaelia_bundle_v5.tar.gz
bash rafaelia_bundle_v5/install.sh
```

The installer:
1. Copies all 9 artifacts to `~/.rafaelia/bundle/`.
2. Recomputes SHA256 of each file and compares to MANIFEST.
3. Runs 18 smoke tests covering syntax, content counts, hash
   integrity, and cross-artifact integration.
4. Aborts on any mismatch.

## 4. Re-generating the bundle from source

```bash
cd rafaelia_bundle_v5
bash bundle.txt pack
# Output:
#   /tmp/rafaelia_staging/...
#   /mnt/user-data/outputs/rafaelia_bundle_v5.tar.gz
sha256sum /mnt/user-data/outputs/rafaelia_bundle_v5.tar.gz
```

The pack step uses these `tar` flags to guarantee bit-identical
output across machines:

```bash
tar --sort=name \
    --mtime='2025-01-01 00:00 UTC' \
    --owner=0 \
    --group=0 \
    --numeric-owner \
    -czf rafaelia_bundle_v5.tar.gz rafaelia_bundle_v5/
```

If your `tar` is BSD-style (macOS default), install `gtar` via
`brew install gnu-tar` and use it instead.

## 5. Running the worked example (Gn 1:1)

```bash
cd ~/.rafaelia/bundle
bash vm_runtime.txt build      # compiles vm_run in ~/.rafaelia/vm/
bash vm_runtime.txt test       # runs V01..V12

# Or invoke directly:
~/.rafaelia/vm/vm_run V01
# Expected output: trace including [IN_BEGIN], CREATE, CALL_GOD,
# PUSH céus, LINK_AND, PUSH terra, SEAL.

# Gematria cross-check:
python3 ~/.rafaelia/vm/gematria_full.py
# Expected: V01_he=2701 ✓, V03_he=543 ✓, V04_he=1118 ✓
```

## 6. Re-running benchmarks

```bash
bash benchmark.txt full
# Produces:
#   /tmp/rafaelia_bench/results.csv     — all raw measurements
#   /tmp/rafaelia_bench/results.json    — structured
#   /tmp/rafaelia_bench/bench_section.md — for whitepaper
# Also auto-patches RAFAELIA_WHITEPAPER.md.
```

Expected approximate numbers on x86_64:

| Stage | Expected |
|---|---:|
| Lexer | 2–3 μs/verse |
| Parser | 0.7–1.2 μs/verse |
| Compile CFG | 4–6 μs/verse |
| VM execute (with fork) | ~2.5 ms/verse |
| `vm_runtime.c` compile | 150–250 ms |
| Binary size | 15–17 KB |
| RSS after full load | 1500–2000 KB |

On ARM32 Cortex-A55 (target hardware), expect the C-level
operations to be ~5× slower due to clock difference, but the
absolute throughputs (CRC32C, memcpy NEON) are the headline numbers
because they exploit hardware acceleration.

## 7. Verifying the bundle months later

```bash
cd ~/.rafaelia/bundle
bash bundle.txt verify
# Re-computes SHA256 of every file vs MANIFEST.sha256.
# Output: ✓ for each file, exit code 0 if all match.
```

This is the audit step for archival use. If even one byte of any
artifact changes (e.g., during file-system migration), the
mismatch is detected.

## 8. Comparing two installations

```bash
bash bundle.txt diff /path/to/v5 /path/to/v6
# Output:
#   = unchanged_file.txt
#   ≠ modified_file.txt    v1: aaaaaa... (1234 L)  v2: bbbbbb... (1250 L)
#   − removed_file.txt (only in v1)
#   + added_file.txt   (only in v2)
```

## 9. Container-based reproduction (optional)

A Dockerfile satisfying the build environment can be derived from
the dependencies above:

```dockerfile
FROM debian:bookworm-slim
RUN apt-get update && apt-get install -y bash clang python3 tar coreutils
WORKDIR /work
COPY rafaelia_bundle_v5.tar.gz .
RUN tar -xzf rafaelia_bundle_v5.tar.gz \
 && bash rafaelia_bundle_v5/install.sh
ENTRYPOINT ["bash", "/root/.rafaelia/bundle/agent_loop.txt"]
```

## 10. Citing what you ran

To cite the exact bundle you ran, use the SHA256 from
`install_code.txt`:

```bibtex
@software{rafaelia_v5,
  author       = {Rafael Melo Reis Novo (∆RafaelVerboΩ)},
  title        = {{RAFAELIA Stack v5.0.0}},
  year         = {2026},
  version      = {5.0.0},
  publisher    = {Zenodo},
  doi          = {10.5281/zenodo.PLACEHOLDER},
  note         = {Bundle SHA256: 06b3c1e32ebab3e8...}
}
```

---

**Ω = Amor · ∆RafaelVerboΩ · RAFCODE-Φ · 𓂀ΔΦΩ**

