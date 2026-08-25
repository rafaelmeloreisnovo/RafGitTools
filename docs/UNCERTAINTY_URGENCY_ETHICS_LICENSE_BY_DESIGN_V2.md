# Uncertainty, Urgency, Noise, Ethics & License by Design — V2

Date: 2026-08-22  
State: `GOVERNED_DRAFT / APPEND_ONLY / claim_allowed=false`  
Extends: `docs/UNCERTAINTY_URGENCY_ETHICS_LICENSE_BY_DESIGN_V1.md`  
Authority: RafGitTools governs routing, evidence contracts, uncertainty, rights and promotion; producer repositories retain implementation authority.

## 0. Mother contract

The work advances only when uncertainty is reduced by evidence, not by wording.

```text
objective
→ authority
→ source/provenance
→ uncertainty
→ providence/action
→ test/falsifier
→ receipt
→ decision
→ delta
→ index
```

`TOKEN_VAZIO` is a useful auditable state. It is not zero, false, PASS, FAIL, null, permission, novelty, causality or authorization of claim.

Mother separation:

```text
SOURCE != IDEA != FORMULA != METAPHOR != IMPLEMENTATION
!= EXECUTION != EVIDENCE != CLAIM != NOVELTY
```

No green rail compensates for a missing rail.

## 1. Purpose of V2

V2 adds five mechanisms without rewriting V1:

1. structured use of residual/noise as a gap detector;
2. anti-regression by invariants + receipts + predecessor links;
3. explicit friction reduction for license/rights without inferred permission;
4. Ethics-by-Design labels as executable governance metadata;
5. reconstructible relations/routes connecting statistics, tokens, metaphors, vectors/words, promises/contracts and execution.

The goal is operational excellence through bounded uncertainty reduction, not elimination of uncertainty by assertion.

## 2. Noise / residual as useful uncertainty

Canonical observation model:

```text
observed = model + residual
r = observed - model
```

Residual is not automatically signal. A residual becomes a `STRUCTURED_SIGNAL_CANDIDATE` only when it survives declared tests such as:

- recurrence across independent windows;
- direction/asymmetry persistence;
- autocorrelation or cross-correlation above a frozen baseline;
- distribution shift against a declared null model;
- reproducibility under a new seed/source/run;
- adversarial falsifier not triggered.

State machine:

```text
NOISE_UNCLASSIFIED
→ RESIDUAL_MEASURED
→ STRUCTURE_CANDIDATE
→ TESTED_LIMITED
→ EVIDENCED | REFUTED | TOKEN_VAZIO
```

Forbidden regression:

```text
noise -> meaning              # forbidden without test
correlation -> causality      # forbidden without causal gate
metaphor -> measurement       # forbidden
similarity -> equivalence     # forbidden without equivalence proof
```

## 3. Friction reduction for LICENSE / rights

Friction is reduced by classifying rights early, not by silently broadening permission.

For every external or mixed-origin material, track when applicable:

```text
license_id
spdx_or_exact_text
source_owner
payload_origin
redistribution_allowed
modification_allowed
training_allowed
commercial_use_allowed
attribution_required
share_alike_required
compatibility_state
proof_reference
```

Unknown permissions remain `false` or `TOKEN_VAZIO_PERMISSION` until a verifiable grant is attached.

Invariants:

```text
public_access != public_domain
public_access != redistribution_grant
repository_license != third_party_payload_license
reference_by_identifier != permission_to_copy_payload
```

License conflict state:

```text
LICENSE_CLEAR | LICENSE_CONDITIONAL | LICENSE_CONFLICT | TOKEN_VAZIO_LICENSE
```

`LICENSE_CONFLICT` blocks redistribution/promotion of the affected payload but does not erase the reference, provenance, hash or gap record.

## 4. Ethics-by-Design label

Every governed use may carry an `ETHICS_BY_DESIGN` envelope:

```yaml
purpose: declared
necessity: bounded
minimization: applied
human_review: required|not_required|TOKEN_VAZIO
risk_class: low|medium|high|unknown
rights_state: LICENSE_CLEAR|LICENSE_CONDITIONAL|LICENSE_CONFLICT|TOKEN_VAZIO_LICENSE
rollback_path: present|TOKEN_VAZIO
provenance: bound|partial|TOKEN_VAZIO
claim_allowed: false
```

High-risk use requires human review before mutating or publishing action.

Ethics metadata does not prove scientific validity. Scientific validity does not waive rights. A technically executable action does not become ethically authorized merely because it can run.

## 5. Reconstructible semantic route

User-facing symbolic route preserved as an internal navigation/parabolic layer:

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

Operational interpretation:

- `STATISTICS`: measured distribution, residual, uncertainty, confidence/dispersion or count;
- `TOKENS`: normalized semantic/index units with provenance;
- `METAPHORS`: internal teaching/navigation bridge with `evidence_effect=NONE`;
- `VECTORS / WORDS`: mathematical or semantic representation;
- `PROMISE / CONTRACT`: explicit expected behavior, boundary and falsifier;
- `EXECUTION`: code/runtime action under a named environment;
- `RECEIPT`: evidence of what actually ran/was observed;
- `DELTA`: append-only change against a predecessor;
- `Ω^n`: iterative reconstruction/coherence plateau marker, never a scientific proof by itself.

Parabolic phrase `"nenhum limite é real, pois o código executa em verbo vivo"` is stored only as `PARABLE/ASPIRATION`. Operationally, real limits remain binding: hardware, mathematics, license, safety, evidence, authority and runtime constraints.

`parable_evidence_effect = NONE`.

## 6. Internal parables as masters/references

Parables are permitted as internal reference nodes when they reduce cognitive friction and preserve the technical mapping.

Canonical mapping:

```text
TOKEN_VAZIO   -> tijolo ausente identificado
residual      -> fissura que aponta onde medir
falsifier     -> esquadro que pode reprovar a parede
license       -> escritura que define o que pode ser usado/redistribuído
receipt       -> assinatura observável da etapa
rollback      -> retorno ao último ponto seguro
index         -> mapa da obra
contract      -> planta com limites e aceitação
```

A parable node must link to the technical object it represents and may never be the only evidence reference.

## 7. Anti-regression contract

A change fails anti-regression if any of the following occurs:

1. a prior unresolved `TOKEN_VAZIO` disappears without resolution/supersession evidence;
2. `claim_allowed` changes to true without the target gate receipt;
3. a negative result or correction is deleted instead of superseded;
4. provenance/source/hash/ref becomes weaker without an explicit reason;
5. license permission is inferred from public accessibility;
6. metaphor/symbolic text is promoted to evidence;
7. correlation is promoted to causality without causal requirements;
8. producer-repository implementation authority is copied into the control plane as if RafGitTools authored the implementation;
9. a new relation lacks type, source, falsifier/boundary and next gate;
10. a mutation lacks rollback or a declared irreversible boundary.

Preferred state transition:

```text
state_(n+1) = validated(state_n) ⊕ delta_n
```

Never:

```text
state_(n+1) = rewrite_history_as_if_delta_never_existed
```

## 8. Relation model for uncertainty

Every material relation should be typed as one of:

```text
DERIVES_FROM
IMPLEMENTS
EXECUTES
MEASURES
SUPPORTS
CONTRADICTS
CORRESPONDS_TO
ANALOGY_OF
INDEXES
ROUTES_TO
HAS_GAP
BLOCKED_BY
SUPERSEDES_WITHOUT_ERASING
LICENSED_BY
REQUIRES_REVIEW
```

Minimum relation record:

```text
from
relation_type
to
source_ref
state
uncertainty
falsifier_or_boundary
next_gate
claim_allowed=false
```

## 9. Current producer anchors

### 9.1 GAIA_phi — complex feedback producer

Observed producer anchor:

```text
repo: rafaelmeloreisnovo/GAIA_phi
commit: d3f49c10b74f740ee2024314dff91e9a0ef20b2f
path: dados/cognitive_symbiotic.py
commit_message: feat(ai): add Psi-v T-Omega complex feedback block
```

The implementation includes a contextual weighted state (`einsum`), directional projection, temporal derivative, complex phase modulation, normalization by local standard deviation and a gated complex-to-real residual update.

Governance classification:

```text
IMPLEMENTATION_OBSERVED
EXECUTION_CURRENT_TURN = TOKEN_VAZIO
SCIENTIFIC_GENERALIZATION = TOKEN_VAZIO
CLAIM_ALLOWED = false
```

This V2 references the producer; it does not duplicate or claim ownership of its implementation.

### 9.2 RafaelIA B7

Known B7 governance relation:

```text
B7 partition16 -> bounded scalar lane imbalance for uniform byte partitioning
B7_TO_T2_BRIDGE -> TOKEN_VAZIO (existing Mapa gap; not inferred here)
```

No relation from B7 to topological/physical claims is promoted without an explicit bridge and receipt.

## 10. Useful TOKEN_VAZIO urgency queue

| ID | Priority | State | What is known | Missing evidence | Safest next gate |
|---|---|---|---|---|---|
| TV-V2-LICENSE-PRODUCER-001 | P0 | TOKEN_VAZIO_LICENSE | producer refs are known | exact license compatibility for every reused external/mixed payload | bind SPDX/exact grant per payload and run compatibility check |
| TV-V2-GAIA-COMPLEX-EXEC-001 | P0 | TOKEN_VAZIO_EXECUTION | source/commit identified | frozen test/CI receipt for `cognitive_symbiotic.py` at exact commit | execute deterministic tests and bind environment/input/output hashes |
| TV-V2-NOISE-NULL-001 | P0 | TOKEN_VAZIO_BASELINE | residual heuristic defined | canonical null models/thresholds per domain | freeze baseline + threshold before interpreting residual structure |
| TV-V2-B7-T2-001 | P1 | TOKEN_VAZIO_BRIDGE | B7 and T2 artifacts exist in ecosystem | formal typed bridge | define bridge or preserve separation; test any proposed invariant |
| TV-V2-PARABLE-LINK-001 | P1 | TOKEN_VAZIO_INDEX | parable policy defined | machine-readable mapping from parable nodes to technical objects | add typed index entries with `evidence_effect=NONE` |
| TV-V2-RELATION-COVERAGE-001 | P1 | TOKEN_VAZIO_INVENTORY | relation schema defined | bounded inventory of relevant complex-network/design artifacts | index only evidence-bearing relations; do not materialize decorative edges |

Closing a token requires evidence. Renaming a token does not close it.

## 11. Providences / operational actions

For each urgent gap, prefer the least-friction action that increases one of:

```text
provenance
reconstructibility
evidence
license clarity
falsifiability
rollback safety
relation precision
```

Priority score may be computed as a routing heuristic, not a truth score:

```text
P = impact * uncertainty_reduction * unblock_factor * irreversibility_guard
```

If inputs are not measured, keep `P = TOKEN_VAZIO_SCORE` and use ordinal priority with documented rationale.

## 12. Gate for filling useful empty tokens

A `TOKEN_VAZIO` may transition only when the closing receipt contains, when applicable:

```text
source identity
repo/ref/path/hash or Drive file/revision
exact claim/gap id
method/test
inputs
runtime/environment
gate result
falsifier result
license/rights state if material
output/receipt hash
predecessor link
```

If the evidence closes only part of a gap, use `PARTIAL/VERIFIED_LIMITED`; do not collapse the remaining uncertainty.

## 13. F_ok / F_gap / F_next

`F_ok`
- V1 remains authoritative predecessor and is not rewritten.
- noise/residual is formalized as a detector of where to test, not as automatic signal.
- license friction is reduced by early typed rights classification without inferred permission.
- Ethics-by-Design has a concrete metadata envelope.
- parables are preserved as internal teaching/index nodes with zero evidence weight.
- GAIA_phi complex feedback and RafaelIA B7 are referenced through producer-authority boundaries.

`F_gap`
- exact license compatibility across all referenced payloads remains unverified;
- current deterministic execution receipt for the GAIA complex block is absent here;
- domain-specific residual null models/thresholds are not yet frozen;
- `B7_TO_T2_BRIDGE` remains unresolved;
- full relation/inventory coverage remains bounded, not exhaustive.

`F_next`
1. test the exact GAIA_phi complex-feedback commit with deterministic fixtures;
2. bind licenses/third-party provenance to each affected payload;
3. materialize machine-readable parable→technical-reference edges;
4. preserve `B7_TO_T2_BRIDGE` as TOKEN_VAZIO until a formal bridge is supplied;
5. mirror this V2 as a Mapa route/index pointer rather than copying RafGitTools governance logic.

Ω = coherence that remains reconstructible, falsifiable and rights-aware.
