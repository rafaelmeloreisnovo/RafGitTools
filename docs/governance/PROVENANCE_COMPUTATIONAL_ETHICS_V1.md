# Provenance + Computational Ethics V1

Status: `IMPLEMENTED_UNTESTED` until exact-head CI proves otherwise.

## Invariants

`SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM`

`TOKEN_VAZIO != 0`

`IMPLEMENTED_UNTESTED != PASS`

Ethics constrains action and promotion. It never manufactures technical evidence.

## BODY / SOUL / SPIRIT

- **BODY** — implementation, build and runtime evidence.
- **SOUL** — semantics, contracts, provenance, authorship and custody.
- **SPIRIT** — ethics, safety, reversibility, dignity, consent, impact, non-regression and cultural respect.

SPIRIT may block an action or claim. SPIRIT cannot convert BODY or SOUL to PASS.

## Fail-closed rights gates

When applicable, routes must explicitly represent privacy-by-design, data minimization, purpose limitation, least privilege, human oversight/appeal, accessibility, non-discrimination, dignity and proportionality. Child-safety is priority and fails closed.

Normative references require version/date, scope/applicability and binding force. Missing applicability is `TOKEN_VAZIO_NORMATIVE`; perspectives are not silently promoted to law, certification or consensus.

## Custody

Rust, C++, Kotlin, Java, C and Assembly components are classified from evidence as one of:

- `CLEAN_AUTHORIAL_COMPONENT_VERIFIED`
- `THIRD_PARTY_DERIVED`
- `MIXED_ORIGIN`
- `TOKEN_VAZIO_ORIGIN`

Porting or rewriting does not erase provenance.

## Runtime boundaries

`CORE_FREESTANDING` and `ADAPTER_PLATFORM` are separate boundaries. Claims such as no-libc, no-syscall, no-JNI/JVM, no external dependency, no tail or no shadow require source/build graph, link evidence and runtime receipt.

Vocabulary:

- TAIL = transitive chain or post-step.
- SHADOW = implicit or duplicated path/implementation.
- FRICTION = necessary external boundary.

These are engineering descriptions, not accusations.

## Reconstruction

Promotion requires enough evidence to reconstruct the transition: before/after state, exact ref, expected/actual observable, falsifier, replay recipe, rollback procedure, rollback test and reconstruction minimum. Missing proof remains `TOKEN_VAZIO_RECONSTRUCTION`.

Corrections append a successor/supersedes relation; predecessors are not erased.

## Promotion rule

A technical claim may be promoted only when BODY is `PASS`, execution/evidence exist, and applicable SPIRIT gates do not block the route. CI green is scoped evidence, not certification or universal compliance.
