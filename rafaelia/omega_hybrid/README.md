# RAFAELIA Omega Hybrid

`rafaelia/omega_hybrid` is a freestanding C99 implementation of the RAFAELIA hybrid Bayesian-hermetic core requested for the repository. It is intentionally small, static, and heapless: no `malloc`, no garbage collector, no dynamic containers, and no release-signing changes.

## Delivered system surface

- **Toro T⁷ state**: seven Q0.16 toroidal coordinates with deterministic hash + entropy mapping.
- **Two Omega cycles**:
  - `Ω1`: static expert registry fuses text/signal embeddings into a shared 64-dimensional byte embedding.
  - `Ω2`: EMA toroidal update, coherence scoring, attractor collapse, snapshot CRC validation, rollback, and token emission.
- **Seven antiderivative directions**:
  1. inverse toral symmetry;
  2. reverse snapshot/state replacement;
  3. recursive nearest-attractor collapse;
  4. indirect coherence-gradient step;
  5. analytic spectral-style coordinate mixer;
  6. relative logistic-like coordinate remap;
  7. direct EMA update.
- **Forty-two attractors**: generated from the first 42 primes and Fibonacci-derived residues.
- **Failsafe / failover / rollback**:
  - expert failures set failover flags without heap allocation;
  - KL-like logarithmic coherence conference emits the void token when the gap is unsafe;
  - CRC32C snapshots allow rollback to the latest valid state.
- **Void token**: token `0`, used when coherence/proof is insufficient.

## Build and test

```bash
cd rafaelia/omega_hybrid
make clean all test
./demo
```

## API sketch

```c
RafOmegaRuntime rt;
RafOmegaState st;
raf_omega_init(&rt, &st, 0x52414641454C4941ULL);
raf_omega_register_expert(&rt, raf_omega_text_expert, 0, 256, 1);
raf_omega_register_expert(&rt, raf_omega_signal_expert, 0, 192, 2);
RafOmegaResult r = raf_omega_cycle(&rt, &st, bytes, len, observed_token);
```

## Enterprise expansion route

This directory is a native core module, not a claim that GPG/LFS/worktree/webhook stubs are production-ready. The recommended expansion path is:

1. add Android JNI bindings that call the heapless API from the existing app native bridge;
2. add ARM32 NEON and ARM64 ASIMD specializations behind compile-time capability flags;
3. add domain experts for image/audio/market/DNA as fixed-size modules that fill `int16_t[64]` embeddings;
4. wire state snapshots to the app persistence layer with explicit CRC checks;
5. keep Termux ARM32 as runtime/toolchain validation unless a compatible SDK is already configured.
