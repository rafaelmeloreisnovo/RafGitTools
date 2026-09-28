# AI + Human Fast Route V1

**State:** `IMPLEMENTED_UNTESTED`  
**Authority:** local adapter only; federated authority remains in `Mapa`.

This file gives a contributor — human or agent — a short route to useful work without crawling the repository.

## 30-second bootstrap

```text
1. Read AGENTS.md.
2. Pick one route in configs/practice-router.v1.json.
3. Read only its source_min (max 3 files).
4. Confirm authority + execution_target + evidence rule.
5. Make the smallest reversible change.
6. Run the named gate.
7. Record PASS/FAIL/NOT_RUN/TOKEN_VAZIO without promotion.
```

Machine query:

```bash
python3 scripts/validate_practice_router.py configs/practice-router.v1.json --route android-build-runtime
```

Validate all routes:

```bash
python3 scripts/validate_practice_router.py configs/practice-router.v1.json
```

## Why this exists

RafGitTools is large enough that broad crawling costs attention and increases the chance of using stale documentation.
The router turns repository navigation into a bounded contract:

```text
intent → route → <=3 source files → execution target → evidence boundary → next
```

It does **not** duplicate the canonical cross-repository ATLAS. It consumes:

```text
rafaelmeloreisnovo/Mapa
└── data/control-plane/PRACTICE_ATLAS_V1.json
```

## Code navigation comments

Comments should help a future human/agent understand **why this boundary exists** and where to look next.

Use a compact block only when it reduces ambiguity:

```text
ROLE: what this unit does
AUTHORITY: who owns the contract
INPUT: accepted data/state
OUTPUT: emitted data/state
INVARIANT: what must remain true
EVIDENCE: what can actually prove behavior
FAIL_CLOSED: what happens when required information is missing
SEE: exact contract/test/router path
```

Do not paste this into every function. Public APIs, adapters, decoders, executors, security boundaries and evidence producers benefit most.

### Example

```kotlin
/**
 * ROLE: read-only adapter from a canonical formula/vector producer.
 * AUTHORITY: adapter only; does not redefine the upstream ABI.
 * INPUT: versioned vector packet.
 * OUTPUT: inspection model for the UI.
 * INVARIANT: TOKEN_VAZIO is never converted to zero/PASS.
 * EVIDENCE: adapter unit tests + exact upstream vector identity.
 * FAIL_CLOSED: reject unknown schema/version.
 * SEE: configs/practice-router.v1.json#zipraf-inspection
 */
```

## Formula and symbolic routes

RafGitTools may inspect or visualize `Spiral√3/2`, Fibonacci/Tribonacci, Trinity633, φ/π and ZIPRAF material,
but must not become accidental mathematical authority merely because a copy is present here.

Use Mapa to find the producer. In particular:

- radial `sqrt(3)/2` recurrence != every angular spiral law;
- `sqrt(3/2) != sqrt(3)/2`;
- Fibonacci != Tribonacci unless a combined matrix contract explicitly defines both;
- symbolic labels (`42`, `633`, `φ`, seals) are semantic addresses until executable semantics and evidence are bound.

## PR discipline

A useful PR description states:

```text
INTENT
AUTHORITY
SOURCE_MIN
FILES_CHANGED
GATE_EXECUTED
EVIDENCE
GAP
ROLLBACK
NEXT
```

If a required gate did not run, say `NOT_RUN`. If authority/evidence is unknown, say `TOKEN_VAZIO`.

## R3

**F_ok:** local bounded router + code-comment contract defined.  
**F_gap:** remote execution of its validator is not yet evidence-bound.  
**F_next:** validate this router and then add route IDs to touched public APIs incrementally, not mechanically.
