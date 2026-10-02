# Freestanding JVM Orchestration V1

Status: `IMPLEMENTED_UNTESTED`  
Claim boundary: `claim_allowed=false`

## Purpose

Provide one deterministic **hosted JVM control plane** for specialized low-level modules without describing the JVM, Android runtime, JNI, POSIX, or provider layer as freestanding.

```text
SOURCE / AUTHORITY
       |
       v
+---------------------------+
| JVM CONTROL PLANE         |
| typed plan + bit flags    |
| iterative index loop      |
| no reflection/recursion   |
+---------------------------+
       |
       v
+---------------------------+
| HOSTED ADAPTER            |
| JNI / Android / provider  |
| fixed ABI where present   |
+---------------------------+
       |
       +-------------------+-------------------+------------------+
       v                   v                   v                  v
  RafPolimata L0       Audio DSP          BLAKE3/rmr         Vectra kernel
  freestanding         freestanding       authorial rmr      freestanding
       \                   |                   |                  /
        +------------------+-------------------+-----------------+
                               |
                               v
                       PLATFORM / DEVICE GATE
                       Termux / QEMU / Android
                               |
                               v
                            RECEIPT
```

The drawing is a boundary map, not a statement that every request traverses every module. A pipeline plan selects only the specialized stages needed for one bounded operation.

## Friction-reduction rules

| Problem | V1 control |
|---|---|
| tail/recursive dispatch | monotonically indexed `while` loop |
| shadow state across adapters | one `PipelineContext` at the JVM control boundary; core state remains owned by each core |
| stringly dispatch | typed `ModuleKind` / `ModuleBoundary` |
| warning noise | OR-composed integer warning mask |
| implicit fall-through | terminal `TOKEN_VAZIO`, `BLOCKED`, `FAILED` states stop downstream execution |
| evidence inflation | every `PipelineReceipt.claimAllowed` is false at this layer |
| duplicate module identity | constructor rejects duplicate `moduleId` values |
| flag drift | stage-required flags are checked before stage execution |

## Flags

The minimum cross-module safety flags are:

- `SOURCE_BOUND`
- `AUTHORITY_BOUND`
- `FAIL_CLOSED`
- `SHADOW_GUARD`

`LOW_ALLOCATION` is optional and must only be required where the owning module has an applicable allocation contract.

## Warnings

Warnings are machine-composable and remain distinct from execution state:

- `TOKEN_VAZIO`
- `MODULE_UNAVAILABLE`
- `AUTHORITY_MISSING`
- `EVIDENCE_MISSING`
- `ABI_MISMATCH`
- `HOSTED_BOUNDARY`
- `INCOHERENT_FLAGS`

A warning bit does not equal PASS or FAIL. `TOKEN_VAZIO` remains an explicit absence/insufficiency state.

## Repository authority

- **RafGitTools** — producer of this JVM orchestration/control-plane contract.
- **RafPolimata** — owns its L0/compiler/freestanding implementation and gates.
- **Est-dio-de-udio** — owns audio DSP/fixed-point freestanding cores; Android/JNI stays hosted.
- **BLAKE3** — upstream cryptographic core remains untouched; authorial federation belongs only under `rmr/` or `tools/` according to its repository contract.
- **Vectras-VM-Android** — owns VM consumer/kernel behavior and its hosted JNI boundary.
- **termux-app-rafacodephi** — owns Android/provider/platform runtime; it is a `PLATFORM_GATE`, not a freestanding core.

Cross-repository success requires producer and consumer evidence for the claimed boundary. This document cannot promote another repository's runtime state.

## Evidence gates

1. `python3 scripts/validate_freestanding_jvm_orchestration.py`
2. `./scripts/gradlew_with_java17.sh testDevDebugUnitTest --tests com.rafgittools.workspace.FreestandingModulePipelineTest`
3. Existing native/JNI owner gates for each selected module.
4. Physical ARM32/ARM64 or device-specific receipts only when the claim requires them.

Until the relevant gate executes on the exact commit, its state is `IMPLEMENTED_UNTESTED` or typed `TOKEN_VAZIO`, never PASS.

## Rollback

Close the feature PR or revert only the commits that introduce this V1 orchestration contract. Do not force-push or rewrite unrelated repository history. Leaf repositories keep independent rollback ownership.

## R3

`F_ok` = bounded hosted-JVM architecture, typed state/flags/warnings and deterministic fail-closed loop are source-materialized.  
`F_gap` = current-head CI, cross-repository leaf acceptance, device/runtime/performance evidence remain unobserved.  
`F_next` = materialize leaf contracts, bind them in the central manifest, execute structural/unit gates, then promote only the scopes actually observed.
