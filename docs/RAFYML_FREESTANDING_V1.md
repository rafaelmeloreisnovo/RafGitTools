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
