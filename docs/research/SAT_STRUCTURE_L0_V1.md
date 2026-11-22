# SAT Structure L0 V1 — RAFAELIA / ATLAS falsifier

State: `LOCAL_SOURCE_EXECUTED / SOURCE_ONLY_GIT_PR_CANDIDATE / CI_UNOBSERVED / claim_allowed=false`.

## Source and authority

- Producer authority: `rafaelmeloreisnovo/RafGitTools` (this repository), C11 `native/sat_structure_l0_v1/`.
- Federated route authority: `rafaelmeloreisnovo/Mapa` (`docs/MANIFOLD_CATALOG_DELTA_METRICS_V1.md`). The Atlas maps references; it does not execute/validate NP claims.
- Drive entry: `START HERE Ω V2.2 DISPATCH` → `CURRENT_STATE Ω` → `CANONICAL INDEX MINIMUM Ω` → type-specific source → `μWRITE LEDGER V2`.
- No private Drive shards read or copied; synthetic fixtures only.
- Provenance: see exact Git commit for source and receipt; the local receipt binds SHA256 of the source and header, not Git's tree SHA.

## Preregistered falsifiable question

Do families of synthetic **satisfiable** 3-CNF instances with locality (three adjacent variables per clause) remain within a fixed elimination-width budget, while equally sized random-wiring controls may exhaust the same budget?

This is a **bounded algorithm engineering experiment**, not a proof of `P=NP`. Its null/falsifier is a constructed local instance with elimination-width above the budget or a disagreement with an independent truth-table evaluator. It is not a statistical population-generalization study.

Elimination uses fixed increasing-variable order with bucket factors, existentially quantifying one variable at a time. Complexity is exponential in the *induced width of this chosen order*, roughly `2^w` factor entries per bucket, multiplied by bucket and input-processing costs. The algorithm is not a general polynomial-time SAT algorithm. Width cap = 8; exceeding it returns `WIDTH_BUDGET`, never `UNSAT`.

## Execution

```sh
python3 tests/sat_structure_probe.py
```

The host harness uses Python standard library, `cc` and `nm`; compiles the C11 core with `-ffreestanding -nostdinc -fno-builtin -fno-stack-protector` and asserts no undefined object symbols. For host-only ctypes interoperability it builds a `-shared -nostdlib` library. None of those host APIs enter L0. No heap, libc, syscalls, system headers or FP in the core. This does not demonstrate ARM32 binary execution.

Two different C algorithms are compared: bounded variable elimination (VE) and generic DPLL with unit propagation. Independent **Python exhaustive enumeration** checks handcrafted and 126 randomized small CNFs (`n<=8`), including satisfiable/unsatisfiable cases. An additional exhaustive check covers synthetic positive fixtures at `n<=12`, and all 48 synthetic instances have planted witnesses checked by a separate Python evaluator. DPLL's correctness beyond small cases is not independently formally proved; planted witnesses independently establish SAT for those positive fixtures.

For each `n={6,8,10,12,16,20,24,30}`, two wiring families, three deterministic seeds and identical clause count `m=2n`: total **48 planted formulas**. Positive witnesses are guaranteed by construction. The first 4 handcrafted + 126 randomized small cases form the **130 additional oracle-check cases**; total `cases_checked=178`. Inputs are synthetic, not random samples from all NP instances. Timings report median of 3 host invocations without benchmarking controls for CPU scaling; do not generalize absolute speed.

## Observed local results (2026-10-16)

| Wiring | Instances | VE `SAT` | VE `WIDTH_BUDGET` | DPLL `SAT` |
|---|---:|---:|---:|---:|
| Banded/local | 24 | 24 | 0 | 24 |
| Uniform/random links | 24 | 9 | 15 | 24 |

- All 178 local cases' checks completed successfully; negative and invalid-input fixtures failed closed.
- Local wiring used observed VE width at most 2. Random wiring triggered width 9 or more in 15 instances.
- **No faster overall solution claim:** for the measured subset, the generic DPLL baseline was generally faster; VE's key result is the effect of explicit width budget and typed refusal.
- Local run source SHA256: `fa31cd0702ec8aebaf0e4a14ae3524dc011596556253eb8b9b598d043b3777cb`.
- Detailed per-instance machine-readable metrics: `receipts/SAT_STRUCTURE_LOCAL_MEASUREMENT_20261016.json`; host-specific times must be recomputed on each platform.

## Research graph (typed, append-only candidates)

```text
CONCEPT:P_NP_OPEN ---STATUS---> OPEN_PROBLEM
CNF_INSTANCE ---STRUCTURED_BY---> PRIMAL_GRAPH
PRIMAL_GRAPH ---BOUNDED_BY---> INDUCED_WIDTH_FIXED_ORDER
VE_RESULT ---VERIFIED_BY---> EXHAUSTIVE_ORACLE (for n<=12)
VE_RESULT ---LIMITED_BY---> WIDTH_BUDGET (when exceeded)
UNKNOWN_QUESTION ---GENERATES---> HYPOTHESIS_CANDIDATE (NOT_EVIDENCE)
ATLAS_ROUTE ---POINTS_TO---> EXPERIMENT_RECEIPT (NOT_PROOF)
```

Typed epistemic state distinguishes `TOKEN_VAZIO`, `UNKNOWN_QUESTION`, `NOT_APPLICABLE`, `UNVERIFIED_RELATION`, `WIDTH_BUDGET`, `INVALID` and `UNSAT`. `∅` is the mathematical empty set, not absence of a measurement.

## Gaps and control gate

- `F_ok`: deterministic synthetic fixtures; independent small-instance oracle; source L0 no-undefined-symbol compile; local measured result and receipt.
- `F_gap`: hosted exact-head CI and review not run/observed at this cut; no ARM32/ARM64 physical execution; no independent model checker for large UNSAT; no measured performance on real SAT benchmarks; private-shard/source graph edges not read; no broad scientific or P=NP claim.
- `F_next`: exact-HEAD CI + negative/positive gates; run a hold-out SAT corpus with fixed seeds and public provenance; independent reviewer and device probe before promotion of any performance assertion.
- Rollback: isolated branch/PR may be discarded; no production caller, private data, default branch or bootstrap changed.

`SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM`. `IMPLEMENTED_UNTESTED != PASS`. `claim_allowed=false`.
