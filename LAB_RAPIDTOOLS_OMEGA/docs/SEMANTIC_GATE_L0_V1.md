# RapidTools Ω — semantic L0 / strict abstention (candidate v1)

State: `DRAFT/HOLD`. Producer: `rafaelmeloreisnovo/RafGitTools`. Scope: `LAB_RAPIDTOOLS_OMEGA/**` and a dedicated scoped workflow. This is an additive candidate; existing APK, JNI bridge, Gradle stack, baseline tests and the inherited `actions` fork are unchanged.

## Authority and build boundary

- Pure layer: `core/raf_semantic_gate.c/.h`. Freestanding C11, no platform headers, heap, libc, syscall, JNI, NDK, SDK, JDK, Gradle, R8, hidden I/O or dynamic dependency. Takes only a typed byte span or typed integer/null; returns an enum; never writes to the source.
- Hosted build/test layer: `tests/test_semantic_gate.c` and `scripts/verify_semantic_gate.sh`. The driver uses a **host compiler, shell, and optional `nm`**. GitHub Actions YAML, checkout, compiler, and runner are **not freestanding**. They produce/test the freestanding object, not an autonomous binary or APK.
- Targets: x86-64, ARMv7 EABI, AArch64 ELF **object-only**. The ARM cross-objects do not demonstrate Android APK compatibility, physical execution, multi-ABI loader support, performance, security guarantees or deployability.
- No new license grant; resolve all use under the repository's root `LICENSE` and applicable file notices. This module does not import `gradle/actions` implementation.

## Input and output contract

`raf_gate_classify(bytes, length, kind, integer_value)` is deterministic and side-effect-free for valid accessible memory spans of at most 4096 bytes. The caller must provide valid accessible memory for `length` bytes. `length > 4096` is rejected before any input dereference. A typed null requires null pointer, zero length and zero integer value. A typed integer requires null pointer and zero length. A byte string requires non-null pointer and zero integer value; zero bytes is a valid empty string.

Typed outputs preserve non-equivalence:

| Input | Output | Epistemic boundary |
| --- | --- | --- |
| typed null | `TYPED_NULL` | not empty/zero |
| nonnull pointer, 0 bytes | `EMPTY_BYTES` | not null |
| exact four ASCII spaces | `FOUR_SPACES` | not empty |
| other ASCII whitespace-only | `OTHER_WHITESPACE` | different raw bytes must remain in caller source |
| typed integer 0 | `INTEGER_ZERO` | not the ASCII character `0` |
| ASCII `0` | `TEXT_ZERO` | not the integer 0 |
| raw 0x00 byte | `RAW_NUL_BYTE` | not zero bytes / null pointer |
| `molho de tomate` exactly | `TOMATO_LITERAL` | lexical candidate, not proof of a physical sauce |
| `molho de chaves` exactly | `KEYS_LITERAL` | lexical candidate, not proof of keys |
| `molho`, `molho de lenha`, `Ω00`–`Ω15`, ideograms, phoneme/grapheme variants and other unrecognized text | `ABSTAIN` | N5, no guessed origin/translation/meaning |
| inconsistent type/length/pointer or oversize | `INVALID` | typed malformed input, not an unknown fact |

Matching is **byte-exact ASCII**: no case fold, Unicode normalization, grammar, phonetic equivalence, linguistic truth or source inference. Return labels are not unique identities; the caller must retain original bytes, length, kind and source/hash to reconstruct. `TOKEN_VAZIO` is a downstream epistemic state; it must not be collapsed into the input representations `∅`, `""`, `"    "`, SQL `NULL`, `0` or raw `0x00`.

## Falsifiers and gates

`sh LAB_RAPIDTOOLS_OMEGA/scripts/verify_semantic_gate.sh` runs a real host executable for positive/negative cases, 255 byte values, immutability, prefix collisions, oversize and malformed types; it optionally cross-compiles three object targets. Compiler-available means object compilation only. `nm -u` is checked when present. If `nm` is unavailable, this individual check is explicitly NOT_RUN, not PASS.

For a CI PASS claim require the exact PR head and provider run result. Device ABI, ARM hardware, binary reproducibility across compilers, measured performance, APK, security audit, output receipt signing and semantic interpretation remain `TOKEN_VAZIO`; `claim_allowed=false`, `release_allowed=false`.

Rollback: close the draft PR without merge; no existing L0 ABI/API, release or Android path changes. Next promotion only after independent provider evidence and a separately scoped consumer/device gate.

`R3=⟨F_ok: additive source+local scoped tests, F_gap: provider run/device/e2e/semantic meaning NOT_RUN, F_next: exact-head CI review then consumer gate⟩`
