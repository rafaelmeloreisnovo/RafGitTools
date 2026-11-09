# RAFYML-FREESTANDING-V1

## Decision

RafGitTools is the authority and build-tool home for `rafymlc`. It owns parsing,
schema boundaries, deterministic generation, custody hashes and CI receipts.
Generated C may later be consumed by Termux RAFCODEΦ, Vectras/ZIPRAF, GAIA_phi
or another runtime without copying the YAML parser into those repositories.

## Boundary

```text
human config.yml
  -> restricted parser (host, Python stdlib only)
  -> canonical tree
  -> static node table + UTF-8 string pool
  -> generated C object (-ffreestanding)
  -> runtime validation
  -> receipt
```

The final C contains no YAML parser and requires no heap, libc, JNI, filesystem,
shell or dynamic allocation. It uses relative indices and string offsets.

## Supported profile

- maps, lists, UTF-8 strings, signed 64-bit integers, booleans and null;
- indentation of exactly two spaces per level;
- deterministic lexical ordering of map keys;
- duplicate-key rejection;
- explicit limits: depth 32, nodes 4096, string pool 1 MiB.

Rejected fail-closed: anchors, aliases, tags, merge keys, block scalars,
multiple documents, complex keys, flow maps/lists, tabs, ambiguous scalars and
out-of-range integers.

## Usage

```sh
python3 tools/rafymlc/rafymlc.py validate examples/rafyml/config.yml
python3 tools/rafymlc/rafymlc.py emit-c examples/rafyml/config.yml \
  --out examples/rafyml/generated --prefix config
python3 scripts/ci/validate_rafyml_freestanding.py
```

Compile generated objects:

```sh
clang -std=c11 -ffreestanding -fno-builtin -Iinclude \
  -Iexamples/rafyml/generated -c src/rafyml_runtime.c -o runtime.o
clang -std=c11 -ffreestanding -fno-builtin -Iinclude \
  -Iexamples/rafyml/generated -c examples/rafyml/generated/config.generated.c -o config.o
```

## Evidence semantics

Generation alone does not promote a runtime claim. The compiler receipt uses
`claim_allowed=false` and `GENERATED_REQUIRES_COMPILE_GATE`. A CI or physical
runtime receipt is required to promote architecture-specific execution.

## Federation

- **RafGitTools:** source authority, compiler, gates and custody.
- **Termux RAFCODEΦ:** physical ARM32/ARM64 execution receipt.
- **Vectras/ZIPRAF:** optional packaging and page-graph binding.
- **Mapa:** longitudinal index and cross-repository pointers.

## Successor boundary: L0 multi-ABI, PIRC-2 / 6Σ (2026-11-09)

**Status:** CONTRACT_ONLY / IMPLEMENTATION_NOT_RUN / claim_allowed=false.
This append-only section preserves the V1 Python-hosted generator and the existing C runtime. It is not a claim that Python, LLVM, the OS, or the GitHub runner have been replaced.

### Source versus target, without ambiguity

| Stage | Observed V1 | Desired L0 contract | Gate |
|---|---|---|---|
| YAML authoring / parsing | Python 3 stdlib host parser | Optional future freestanding C11 or assembly parser for the **restricted profile only** | Byte-for-byte deterministic canonical output against existing corpus; reject unsupported syntax |
| Generated document | Static C nodes/string pool | Same V1 node ABI and canonical byte encoding | Compare sizes, offsets, ordering and content digests on both architectures |
| Runtime | C11 with `-ffreestanding -fno-builtin` | Keep no heap/libc/JNI/Android framework calls in L0 | Inspect undefined symbols and disassembly; prohibit implicit memcpy/memset unless supplied intentionally |
| Target ARMv7 / AArch64 | Cross-compile probes exist in V1 test script | Compile, link, and execute with an explicit platform adapter; no assumption that an object file is executable | Per-ABI ELF machine/class/endianness/relocations, runtime negative tests on registered device |
| Host orchestration | Python/shell/compiler/runner | Isolate as L1, replace only after independent verified tool implementation | Provider/toolchain identity, digest, nonzero exit propagation, receipt |
| GitHub workflow YAML | Parsed by GitHub Actions host | Declarative orchestration, **not executable freestanding code** | Syntax validation, permissions, action pinning, isolated smoke test |

### Invariants and counterexamples

- P0: A V1 document must produce the same logical node graph under both ABIs; changing `sizeof(pointer)` must not change the serialized wire format. Explicit byte order and alignment shall be fixed in the future wire specification; until then portability of raw in-memory structs is **N5/TOKEN_VAZIO**.
- P1: `0`, null, empty string, four spaces, absent key, missing document, and failed parse are distinct states. No implicit coercion from text, phoneme, or analogy into a semantic key.
- P2: For bounds, test zero nodes, max 4096 nodes, 1 MiB pool, depth 32/33, dangling sibling, cyclic graph, offset overflow, duplicate keys, invalid UTF-8 and invalid scalar. All invalid inputs must terminate with a typed error without out-of-bounds read/write.
- P3: Require fixed-size input/output or caller-owned memory with lifetime contract; `void` is a C type, not a guarantee of purity. No unwarranted demand to eliminate function names or local variables: at machine level instructions and ABI-visible interfaces must remain inspectable.
- P4: Inline assembly only for a measured need, per ISA, with declared clobbers, alignment and fallback. Branchless transformations require assembly review and constant-time tests when security-relevant; never equate branchless with guaranteed faster/safer.
- P5: Host kernel syscalls, compiler/linker, loader, CI service and ELF/ABI remain **external dependencies** even if L0 has no libc/JNI/NDK/JDK/SDK/Gradle/R8 dependency. An Android APK requires Android packaging and signatures; a freestanding ELF does not become an installable APK by naming it one.

### Falsification matrix (all pending, not PASS)

| Falsifier | Expected observation | Failure response |
|---|---|---|
| Same input twice, two clean builds | Identical canonical serialized bytes and digest | HOLD, preserve V1 generator |
| ARM32 vs ARM64 | Equivalent semantic graph; explicit ABI-specific ELF evidence | BLOCK promotion; retain old path |
| Unsupported YAML tokens, duplicate keys | Deterministic reject, nonzero code, no partial promoted artifact | Fail closed |
| Truncated / malicious offsets, cyclic edges | Bounded rejection under sanitizer and target probe | Revert branch change only |
| Empty/NULL/whitespace/zero | Six distinct typed cases in test vector | No automatic normalization |
| Compiler introduces libc helper | `nm -u` empty or exact allowlist with intentional implementation | BLOCK freestanding claim |
| Provider workflow cannot run without hosted runner | Explicit L1/provider dependency receipt | Never claim the YAML itself is freestanding |

### Reversible DMAIC execution plan

1. Define: isolate a single candidate authorial L0 parser/serializer module, preserve V1 wire semantics and upstream licenses.
2. Measure: pin source commit, host/compiler versions, V1 golden vectors, binary sizes, import tables, ABI widths and latency baseline; unknown=TOKEN_VAZIO.
3. Analyze: choose one verified source-level dependency at a time, distinguish hosted build-time Python from target runtime dependency.
4. Improve: introduce **additive** C11/ASM candidate in a new path, without replacing `rafymlc.py` or GitHub workflows. No release/merge/dispatch.
5. Control: run adversarial vectors on host, ARMv7 and AArch64; capture exact-head CI, checksums, warnings, stdout/stderr and rollback instructions. `IMPLEMENTED_UNTESTED` is not `PASS`.
6. Receipt: append `source_ref|parent|artifact_hash|compiler|ABI|tests|negative_cases|warnings|gap|rollback|claim_allowed=false`. Do not synthesize artifact hashes.

**Rollback:** close or revert the isolated review branch/PR only; the existing `main` V1 Python generator, C validator and downstream integrations remain unchanged.
**PIRC-2 classification:** N0 = the V1 code and documentation directly fetched; N2 = hosted orchestration cannot be removed by `-ffreestanding`; N3 = an authorial replacement parser can preserve semantics after golden-vector tests; N5 = actual cross-device behavior, complete ABI checks, performance and licensing of new code. No metaphor or scientific claim is used as test evidence.
