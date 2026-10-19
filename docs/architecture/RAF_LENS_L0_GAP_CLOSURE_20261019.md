# Raf Lens L0 — fechamento conservador de gaps (2026-10-19)

**Estado:** `SOURCE_PATCHED / LOCAL_HOST_TEST_PASS / GITHUB_EXACT_HEAD_CI_PENDING / NO_PROMOTE`. `claim_allowed=false`.

## Authority and observed baseline

- Parent `RafGitTools PR #688` is **merged into the feature branch** of PR #682; **not into main**. PR #682 remains open. Preserve the original commit/receipt as predecessor; do not overwrite evidence.
- Scoped producer: `freestanding/lens` (C11, no libc/syscalls, explicit bounded buffers; NOT a universal CPU executable).
- A separate child branch carries three targeted fixes and one existing-workflow extension. No second active GitHub workflow, provider login or private corpus change.

## Diagnosed gaps, falsifiers and closes

| ID | Prior risk | Corrective delta | Evidence and boundary |
|---|---|---|---|
| G-RFL-01 | Invalid edge could change caller-owned `seen` before returning `RFL_INVALID` | Prevalidate every edge before touching `seen` | Negative KAT asserts sentinel bytes remain intact, scoped host PASS |
| G-RFL-02 | Shell pipeline could swallow errors in `nm`/`readelf` and produce false success | Directly capture `nm -u`, check command status, separate `readelf` readback, assert ET_REL/machine and no runtime sections | Host script PASS; remote gate pending |
| G-RFL-03 | Truncation, duplicate logical provenance and SHA block-padding residuals under-tested | Capacity returns `RFL_CAPACITY`, duplicate IDs denied, NIST 56-byte two-block/split-update KAT added | Local Clang + ASan/UBSan PASS scoped |
| G-RFL-04 | Prior START quick workflow omitted actual L0 verification | One step added to existing `coherence` job in `.github/workflows/START.yml` | Await exact-head Actions steps/receipt. A green aggregate run without this step is NOT proof. |
| G-RFL-05 | Physical bare-metal Android/ABI + Drive provider bridge absent | No unsafe shortcut; L1 remains separate and not connected | `TOKEN_VAZIO_NOT_RUN`; not counted closed |

## Reproduction

```sh
sh freestanding/lens/verify_l0.sh
clang -std=c11 -O1 -g -fsanitize=address,undefined -fno-omit-frame-pointer -Wall -Wextra -Werror \
  freestanding/lens/rfl_sha256.c freestanding/lens/rfl_lens.c freestanding/lens/rfl_graph.c \
  freestanding/lens/tests/test_l0.c -o /tmp/raf-lens-asan
/tmp/raf-lens-asan
```

The verifier itself is hosted; the compiled L0 objects are freestanding. Its host KAT + cross-target object receipt must not be translated into a claim of ARM physical execution, Android runtime/JNI, provider ACL authorization or scientific semantic validity.

**Regression rule:** source must remain machine/ABI explicit, single START pipeline, no libc/malloc/syscalls/network in L0; independent failures do not inherit passing statuses. `SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM`, `TOKEN_VAZIO != 0`.

## R3

`F_ok` = small reversible source hotfixes, new falsifiers, local three-target object test, existing START coherence gate wired.

`F_gap` = remote exact-head L0 job not yet checked, L1 Kotlin/JNI parity, ARM32/ARM64 device proof, bare-metal boot mapping, Drive ACL/receipt identity and human review.

`F_next` = (1) inspect exact-head remote START `Raf Lens L0 freestanding exact-head gate`; (2) add isolated L1 byte-parity consumer test before Android build; (3) pilot per-device or board only after approval, startup/linker/ABI mapped and rollback identified.

**Rollback:** close the new child draft PR. No merge into main, no private data or credential mutation.
