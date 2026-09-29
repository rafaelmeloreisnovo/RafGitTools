# Conversation Relation Vector — Low-Level Pure Python Core V1

State: `IMPLEMENTED_UNTESTED`  
Parent: `CONVERSATION_RELATION_VECTOR_MANIFOLD_V1`  
Claim gate: `claim_allowed=false`

## Constraint implemented

The low-level core intentionally uses:

- zero imports;
- zero stdlib modules;
- zero third-party packages;
- zero FFI / ctypes / JNI calls;
- zero native-extension calls;
- authored SHA-256;
- authored canonical serializer for the supported data subset;
- integer/rational overlap metrics;
- linear list scans instead of sets/index helpers;
- explicit loops and deterministic ordering.

It does **not** use `hashlib`, `json`, `pathlib`, `argparse`, `collections`, `math` or external libraries.

## Physical boundary

Python still executes inside a Python interpreter/runtime. Therefore:

`PURE_PYTHON_LOWLEVEL != BARE_METAL`

and:

`NO_EXPLICIT_NATIVE_CALLS != NO_NATIVE_RUNTIME`

A truly runtime-free implementation requires a freestanding compiled target such as C/assembly, which is outside this Python-core artifact.

## Deterministic representation

Floating point was removed from the overlap metric.

Instead of a float Jaccard value:

`J = intersection / union`

the artifact stores:

- `token_jaccard_num`
- `token_jaccard_den`
- `lexical_divergence_num`
- `lexical_divergence_den`

This keeps the representation exact and deterministic.

## Authored SHA-256

The file contains its own SHA-256 implementation and two KATs:

- SHA-256("")
- SHA-256("abc")

No `hashlib` is used.

## Output boundary

The output remains metadata-only:

`CHK-* -> VEC-* -> MDELTA-*`

Semantic embedding, causality and truth remain `TOKEN_VAZIO` until a separate evidence-producing implementation exists.

## Gate

Source presence alone is not PASS.

Required before promotion:

1. direct execution of embedded selftest;
2. SHA-256 KAT PASS;
3. deterministic repeat PASS;
4. fixture/corpus run;
5. manifest/receipt bound to commit SHA;
6. CI terminal state.

`IMPLEMENTED_UNTESTED != PASS`.
