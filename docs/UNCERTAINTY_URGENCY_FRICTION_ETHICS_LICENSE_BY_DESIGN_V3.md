# Uncertainty, Urgency, Friction, Ethics & License by Design — V3

Date: 2026-08-22  
State: `GOVERNED_DRAFT / APPEND_ONLY / claim_allowed=false`  
Extends: V1 → V2 without rewriting either predecessor.

## 0. Contract of operational coherence

```text
{ coherence / operational_excellence / providences := contract }
```

Operationally:

```text
objective
→ authority
→ provenance
→ uncertainty
→ friction_class
→ providence
→ test / falsifier
→ receipt
→ decision
→ append-only delta
→ index
```

A providence is not a promise that a gap is closed. It is the smallest bounded
operation expected to reduce uncertainty or avoidable friction while preserving
rights, evidence, privacy, authority, safety and rollback.

```text
TOKEN_VAZIO != zero != false != PASS != FAIL != permission != claim
```

## 1. What V3 adds

V3 makes friction itself a governed object. It distinguishes:

- license and rights friction;
- provider/runner friction;
- authority/routing friction;
- semantic ambiguity;
- implementation binding friction;
- runtime reproducibility friction;
- evidence gaps;
- uncertain graph relations;
- rollback/irreversibility friction;
- privacy/minimization friction.

Reducing friction never means reducing a gate.

```text
friction_reduction != permission_broadening
friction_reduction != claim_promotion
friction_reduction != history_rewrite
friction_reduction != weaker_falsification
```

## 2. Useful TOKEN_VAZIO as an urgency ledger

A useful empty token records all of the following when available:

```text
gap_id
priority
friction_class
known evidence
missing evidence
blocked uses
safest next gate
predecessor
```

Closure requires a receipt. Partial closure preserves the residual gap.

The V3 urgency queue contains:

- `TV-V3-PROVIDER-CLASS-001` — distinguish provider failure from executed test failure;
- `TV-V3-LICENSE-PAYLOAD-001` — rights at payload/dependency/source granularity;
- `TV-V3-USE-REGISTRY-001` — concrete Ethics-by-Design usage records;
- `TV-V3-COMPLEX-RELATIONS-001` — evidence-bearing network/design relations;
- `TV-V3-FRICTION-BASELINE-001` — bounded before/after friction baseline;
- `TV-V3-B7-T2-001` — preserve B7→T² as a formal bridge gap until proven.

## 3. License friction without false permission

The rights unit is the smallest material object entering the use:

```text
repository source
!= dependency
!= packaged wheel/binary
!= model weight
!= dataset
!= third-party payload
```

The first bounded matrix separates:

- GAIA repository source under its observed MIT license;
- PyTorch as a separate dependency rights unit;
- model weights as `TOKEN_VAZIO_LICENSE` unless explicitly bound;
- external datasets/payloads as `TOKEN_VAZIO_LICENSE` unless explicitly bound.

Unknown permission fails closed for copying, redistribution, training and
commercial use. Reference by identifier may remain possible when payload bytes
are not copied and the purpose is governance/indexing.

## 4. Ethics-by-Design becomes use-bound

An ethics label is no longer only a general envelope. Each governed use carries:

```text
use_id
purpose
necessity
minimization
risk_class
human_review
rights_state
privacy_state
rollback_or_irreversible_boundary
provenance
evidence_state
claim_allowed=false
```

The first bounded registry covers deterministic GAIA testing, license
classification, parable navigation, Mapa routing and Drive longitudinal memory.

A new use requires a new record. A prior low-risk classification cannot be
silently inherited when scope changes.

## 5. Provider failure is not test failure

A workflow can fail before a test runs. V3 therefore introduces:

```text
NOT_RUN
QUEUED
PROVIDER_PRE_STEP_FAILURE
SETUP_FAILURE
TEST_EXECUTED_PASS
TEST_EXECUTED_FAIL
RECEIPT_EMITTED
TOKEN_VAZIO_PROVIDER_EXECUTION
```

Rule:

```text
workflow_failure + zero_observed_test_steps
!= TEST_EXECUTED_FAIL
```

For the GAIA PR #98 batch, the captured cognitive receipt job failed with zero
exposed steps, and parallel jobs in the same batch also failed. This is evidence
that execution is unresolved; it is not proof of a provider root cause and not
a scientific/implementation test failure.

A retry was requested. Its result must be inspected before closing the execution
gap. If a later retry passes, the failed attempt remains in custody.

## 6. Complex networks by design

The relation graph is an evidence graph, not a decoration surface.

Every material edge requires:

```text
relation_id
from
relation_type
to
source_ref
authority_domain
uncertainty_state
evidence_effect
boundary_or_falsifier
next_gate
```

The first bounded graph has 14 nodes and 11 typed relations connecting:

- RafGitTools V3 governance;
- GAIA `CognitiveSymbioticBlock`;
- Ψv;
- TΩ;
- complex-to-real gated residual decoder;
- GAIA PR #98 execution gate;
- license matrix;
- Ethics-by-Design use registry;
- Mapa route;
- Drive memory;
- internal parable node;
- RafaelIA B7;
- T²;
- `B7_TO_T2_BRIDGE`.

The counts describe only this registry. They are not completeness or scientific
network metrics.

Forbidden:

```text
visual proximity -> evidence
co-occurrence -> causality
similarity -> equivalence
metaphor -> measurement
edge without source -> evidence relation
```

## 7. Parables as internal masters

Parables are permitted as internal references because they can reduce cognitive
friction while preserving the technical target.

```text
TOKEN_VAZIO -> tijolo ausente identificado
residual -> fissura que aponta onde medir
falsifier -> esquadro que pode reprovar a parede
license -> escritura dos usos permitidos
receipt -> assinatura observável da etapa
rollback -> retorno ao último ponto seguro
index -> mapa da obra
contract -> planta com limites e aceitação
```

The parable is the teacher/signpost. The technical artifact remains the evidence
surface.

```text
parable_evidence_effect = NONE
```

## 8. Statistics → tokens → metaphor → vectors → promise → execution

The symbolic route is preserved:

```text
STATISTICS
→ TOKENS
→ METAPHORS
→ VECTORS / WORDS
→ PROMISE / CONTRACT
→ EXECUTION
→ RECEIPT
→ DELTA
→ Ω^n
```

Technical interpretation:

- statistics: bounded measurements/counts/distributions/uncertainty;
- tokens: typed/indexed units with provenance;
- metaphors: internal navigation, zero evidence weight;
- vectors/words: mathematical or semantic representations;
- promise/contract: expected behavior, boundary and falsifier;
- execution: an operation in a named environment;
- receipt: what was actually executed/observed;
- delta: append-only difference from predecessor;
- Ω^n: iterative reconstruction/coherence marker, not proof.

The phrase:

```text
nenhum limite é real, pois o código executa em verbo vivo
```

is retained as `PARABLE_ASPIRATION`. Operational limits remain real and binding:
hardware, mathematics, license, safety, privacy, evidence, authority and runtime.

## 9. Friction measurement

A bounded friction vector may count:

```text
unresolved gaps
unbound license units
provider pre-step failures
untyped relations
missing rollback paths
missing provenance
manual rework
```

For identical scope:

```text
friction_delta = after - before
preferred direction <= 0
```

If scope changes, rebaseline. Zero measured friction never authorizes a claim.

## 10. Anti-regression delta

V3 adds AR41..AR52. Among other properties, CI must reject:

- provider pre-step failure promoted to test failure;
- retry overwriting the failed predecessor;
- repository license silently covering dependencies/data/weights;
- unknown rights promoted to permission;
- governed use without purpose/minimization/rollback/provenance;
- complex relation without source/boundary/next gate;
- analogy with evidence weight;
- friction reduction used to broaden rights or claims;
- friction score treated as truth;
- before/after comparison with changed scope and no rebaseline;
- partial token closure that erases the residual gap;
- parabolic aspiration used to waive real constraints.

## 11. Persistent artifacts

```text
configs/uncertainty-urgency-friction-ethics-license.v3.json
data/governance/license-friction-matrix.v1.json
data/governance/ethics-by-design-use-registry.v1.json
data/governance/complex-network-design-relations.v1.json
data/evidence/github/gaia-provider-execution-friction-20260822.v1.json
scripts/validate_uncertainty_urgency_friction_v3.py
tests/test_uncertainty_urgency_friction_v3.py
.github/workflows/uncertainty-urgency-friction-v3.yml
```

## 12. F_ok / F_gap / F_next

`F_ok`

- V1 and V2 remain intact predecessors.
- V3 friction classes and AR41..AR52 are materialized.
- license is separated by material rights unit.
- five concrete Ethics-by-Design uses are recorded.
- provider execution uncertainty is separated from test failure.
- a bounded complex/design relation graph is materialized.
- parables retain internal reference value and zero evidence effect.

`F_gap`

- GAIA retry outcome is not yet promoted until inspected.
- exact rights of any additional packaged dependency, dataset, weight or mixed
  payload remain open until those units enter scope.
- complex relation coverage is bounded, not exhaustive.
- `B7_TO_T2_BRIDGE` remains `TOKEN_VAZIO_BRIDGE`.
- friction baseline still requires a frozen identical scope for before/after.

`F_next`

1. inspect the GAIA retry and append the result without erasing the failure;
2. run V3 CI and retain its receipt/hash;
3. create the Mapa routing successor as a pointer, not a logic copy;
4. append this V3 delta and receipts to the Drive longitudinal mirror;
5. close only the tokens whose exact gates are actually satisfied.

Ω = less avoidable friction, more reconstructibility, no silent promotion.
