# Roadmap Placeholder — Conversation Manifold μ∆Steps V1

Status: `ROADMAP / PARTIALLY_IMPLEMENTED / claim_allowed=false`

## Current state

Already present in RafGitTools:

- governed content-addressed conversation chunks;
- deterministic token references;
- typed chunk edges;
- canonical publication tree `00_INDEX..09_CONVERSATION_CHUNKS`;
- TOKEN_VAZIO fail-closed semantics;
- append-only evidence/receipt conventions.

This delta adds the first deterministic `VEC-*` and `MDELTA-*` layer.

## Roadmap

| μStep | State | Operation | Evidence required |
|---|---|---|---|
| μΔ-01 | IMPLEMENTED_UNTESTED | Generate structural vector per CHK-* | selftest + manifest |
| μΔ-02 | IMPLEMENTED_UNTESTED | Generate n→n+1 lexical microdelta | fixture + deterministic repeat |
| μΔ-03 | TOKEN_VAZIO | Add bounded co-occurrence metrics | test dataset + metric definition |
| μΔ-04 | TOKEN_VAZIO | Add agent-response convergence/divergence | explicit agent/source identity |
| μΔ-05 | TOKEN_VAZIO | Add embedding vectors | model/version/hash authority |
| μΔ-06 | TOKEN_VAZIO | Add semantic-neighbor graph | threshold calibration + falsifiers |
| μΔ-07 | TOKEN_VAZIO | Add Bayesian evidence update | dependency/independence model |
| μΔ-08 | TOKEN_VAZIO | Add contradiction/supersession resolver | typed evidence + authority rule |
| μΔ-09 | TOKEN_VAZIO | Publish Drive atlas projection | exact Drive target + receipt |
| μΔ-10 | TOKEN_VAZIO | Promote any scientific claim | claim gate evidence |

## Vector contract target

Each interaction vector should eventually expose, where evidence exists:

```text
<identity,
 provenance,
 temporal_position,
 token_structure,
 lexical_delta,
 relation_degree,
 convergence,
 divergence,
 novelty,
 contradiction,
 evidence,
 parent,
 authority,
 gap,
 claim_state>
```

Missing dimensions stay `TOKEN_VAZIO`; they are not zero-filled.

## Drive projection

The Drive side should remain a metadata/navigation projection over NOVOexport, not a
second copy of the raw corpus.

Suggested canonical child:

`NOVOexport/RAFAELIA_CONVERSATION_MANIFOLD_V1/`

with:

`00_INDEX, 01_ATLAS, 02_ROUTES, 03_MANIFOLD, 04_SCAFFOLDS, 05_EDGES, 06_GAPS, 07_EVIDENCE, 08_RECEIPTS, 09_CONVERSATION_CHUNKS`

The chunk folder stores pointers/indexes unless an explicitly authorized private
materialization requires otherwise.

## Stop rule

Do not promote `IMPLEMENTED_UNTESTED` to `PASS` merely because source exists.

`IMPLEMENTED_UNTESTED != PASS`.
