# Toroidal Research Cycle Contract

## Purpose

This contract turns the symbolic cycle

```text
VOID -> QUERY -> SOURCE -> CLAIM -> FORMULA -> TEST
-> EVIDENCE -> RESIDUAL -> NEW_VOID -> FEEDBACK -> VOID
```

into a dependency-free, fail-closed research governance protocol.

It does **not** claim that every physical system is toroidal. It uses a torus as an
operational representation of two coupled recurrences:

1. the internal research cycle;
2. the federation cycle across repositories and authorities.

## Sine reference boundary

A reference signal may be written as

\[
s(t)=A\sin(2\pi f t+\phi).
\]

The contract permits it for system identification, phase reference, frequency
sweeps, calibration and regression fixtures. It rejects the statement that a
pure sine universally stabilizes physical or computational systems.

Every sine-reference manifest must declare:

- positive finite amplitude and frequency;
- units;
- phase;
- a feedback model;
- a damping or boundedness model;
- `universal_stabilizer_claim=false`.

## Repository federation

The canonical authorities are separated:

- `RafGitTools`: governance, custody, evidence states and rollback;
- `Mapa`: repository identity, dependencies and maturity navigation;
- `relativity-living-light`: scientific formulas, data, likelihoods and falsifiers;
- `RafPolimata`: formula and execution routing;
- `termux-app-rafacodephi`: device execution evidence;
- `llamaRafaelia`: memory and bounded-claim consumption.

An adapter may point to the canonical contract. It must not copy and mutate the
contract independently.

## Non-compensatory promotion

A claim is not promoted by averaging strong dimensions over missing ones.
Promotion requires provenance, scope, uncertainty, baseline, falsifier,
execution evidence, repository authority and no unresolved hard residual.

`TOKEN_VAZIO` remains valid as an absence record and requires:

- affected field;
- reason;
- owner;
- next action;
- exit criteria.

## CI integration

The validator is standard-library only. It is added to the existing canonical
gate, rather than creating another workflow YAML. The CI proves only that the
contract and example manifest satisfy their declared invariants in that run.
It does not prove an external physical theory.
