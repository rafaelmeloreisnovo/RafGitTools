# RAFAELIA Ω — Semantic L0 (PIRC-2) — DRAFT/HOLD

Producer: `rafaelmeloreisnovo/RafGitTools`. New path only: `freestanding/semantic_omega_l0/`.
Scope: freestanding C11 **object** with no libc, heap, JNI, NDK, JDK, SDK, Gradle, dynamic runtime or syscalls in the core. No external symbols when compiled. This is not an Android APK or a self-booting ELF.

## Input and epistemic contracts

- Typed outer parser supplies `raf_omega_atom`; this kernel is not a JSON parser, identity checker, source verifier or inference model.
- `∅` (absent), JSON `null`, empty string, whitespace-only string, JSON numeric zero, JSON boolean false, unresolved token and aborted token receive distinct enum values.
- `"NULL"`, `"false"`, `"0"`, `"TOKEN_VAZIO"` are literal strings, not their JSON or epistemic counterparts.
- Strict JSON-number lexer rejects invalid numeric forms without floating-point rounding; signed fractional scientific zero remains zero.
- `molho` remains ambiguous absent a checked context; context-tag `authority_checked=1` is caller-provided and does not itself prove provenance.
- Unicode codepoints, symbols, ideograms and foreign words are not translated by this byte-level kernel. It does not assert phonetic, causal or synaptic relationships.
- PIRC-2 levels N0–N5 are *structurally* filtered; even a zero violation bitmask is **not** a PASS, evidence authenticity or claim permission.
- No corpus bodies, personal data, credentials, hashes of private data or stable conversation IDs included in this public source.

## Ω00–Ω15 adversarial routing map

| Case | Structural treatment | Evidence boundary |
|---|---|---|
| Ω00 | `molho` has multiple explicit senses | N5 until contextual evidence |
| Ω01 | absent key bundle is not empty string | no inferred location |
| Ω02 | tree, pan and tomato are separate typed concepts | glyph 龍 not translated |
| Ω03 | mixed-script expression is opaque text | composition meaning N5 |
| Ω04 | typed absent/null/empty/spaces/zero/false separate | core unit tested |
| Ω05 | stove, water bucket, reversal word are distinct tokens | no physical causal inference |
| Ω06 | before/after and reversal need provenance/time direction | no undo without known source |
| Ω07 | verb / food / bundle sense needs context | ambiguity enforced |
| Ω08 | τ³, ⊗, multilingual glyphs remain bytes/symbolic | no automatic geometric proof |
| Ω09 | four spaces differ from empty string | core unit tested |
| Ω10 | tomato as key is only an analogy or hypothesis | no access/control claim |
| Ω11 | absent pan differs from invalid key capability | semantic relation N5 |
| Ω12 | unknown origin must not produce valid rollback | fail closed |
| Ω13 | noise/claim sequence requires source-level validation | no inferred truth among noise |
| Ω14 | 𓂀 not proof of origin/translation | no philological claim |
| Ω15 | infinity/fractal/aborted token and μREAD are distinct types | no mathematical/computational equivalence claimed |

## Local build / falsification

Commands below are hosted test infrastructure; **only the compiled C object is freestanding**:

    clang -std=c11 -O2 -Wall -Wextra -Werror -ffreestanding -fno-builtin -c omega_l0.c -o /tmp/omega_x86_64.o
    clang -std=c11 -Wall -Wextra -Werror -fsanitize=address,undefined omega_l0.c test_host.c -o /tmp/omega_test && /tmp/omega_test
    clang --target=armv7-none-eabi -march=armv7-a -mfloat-abi=soft -std=c11 -O2 -Wall -Wextra -Werror -ffreestanding -fno-builtin -c omega_l0.c -o /tmp/omega_armv7.o
    clang --target=aarch64-none-elf -march=armv8-a -std=c11 -O2 -Wall -Wextra -Werror -ffreestanding -fno-builtin -c omega_l0.c -o /tmp/omega_aarch64.o
    nm -u /tmp/omega_x86_64.o /tmp/omega_armv7.o /tmp/omega_aarch64.o
    readelf -h /tmp/omega_armv7.o /tmp/omega_aarch64.o

Acceptance: all three object compilations warning-free; ARMv7=ELF32/ARM; AArch64=ELF64/AArch64; zero undefined symbols; host 30 structural checks pass; no live promotion. Reject malformed buffers/numbers, missing evidence refs, N3 without falsifier, N5 promotion and context-free sense assignment.

## Handoff and rollback

- `actions` is a separate producer and already hosts `ci-kernel4-freestanding.yml`; no inherited Gradle/Node sources or workflow files are modified by this branch.
- GitHub YAML is declarative orchestration, not a freestanding binary. Cross-platform operation requires a compiler and a target runtime; the object has no executable entrypoint by itself.
- Software hosted tests: PASS_SCOPED. Android/physical ARM: TOKEN_VAZIO. Full project CI: NOT_RUN. Semantic/model inference: NOT_IMPLEMENTED. `claim_allowed=false`.
- Branch review only. Do not merge, publish release, create Actions automation or alter permissions without separate evidence and authorization.
- Rollback: retain historical commit receipts; reverse the isolated changes through a reviewed successor commit if needed. Predecessor: `main@e3e2bc202b9cf7c6678968fc9afdc3acbd879def`.
- Rights: preserve repository and any original upstream licenses; authorial/legal status of new work requires a separate explicit assessment, not a license invented by this gate.

R3: F_ok=freestanding structural kernel and local negative tests; F_gap=provider CI/device/provenance verification; F_next=exact-head checks, independent review and source-bound typed adapter before any promotion.
