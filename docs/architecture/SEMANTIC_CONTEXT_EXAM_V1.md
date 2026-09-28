# Semantic Context Exam V1 — from parable to executable context discipline

State: `IMPLEMENTED_SOURCE / TEST_PENDING`  
Authority: `rafaelmeloreisnovo/RafGitTools`  
claim_allowed: `false`

## Purpose

Turn **As Parábolas da Ponte que Recusava Mentir** into an operational layer above model tokenization.

This does **not** modify a model tokenizer, embeddings, weights, or hidden attention. It governs the layer RafGitTools can actually control:

```text
selected source
→ semantic object
→ type/unit/dimension
→ explicit transform
→ invariant
→ provenance/evidence
→ execution state
→ claim gate
→ useful delivery
```

## Seven executable gates

| Gate | Parable | Operational question |
|---|---|---|
| G01 | Balança do mercado | Are values typed and dimensionally compatible? |
| G02 | Ponte do carpinteiro | Is every representation change explicit and invariant-backed? |
| G03 | Bambu e dois caminhos | Are coherent alternatives preserved without hidden selection? |
| G04 | Oleiro | Is internal coherence kept separate from domain interpretation? |
| G05 | Biblioteca | Can sources and evidence be traced? |
| G06 | Ferreiro | Are implemented, tested, and physically proven distinct states? |
| G07 | Duas fechaduras | Do scope, evidence, and authority justify claim promotion? |

## Relationship to existing RafGitTools contracts

This is a **successor layer**, not a replacement.

```text
ContextBundle V2
  transports bounded selected context

Content Validity Contract V1
  validates semantic tokens/windows/support dimensions

Semantic Context Exam V1
  validates whether a requested operation may execute
  and whether its result may be promoted to a claim
```

The exam may reference ContextBundle/Content Validity artifacts, but it does not inherit their authority silently.

## Core invariants

```text
SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM
TOKEN_VAZIO != 0
IMPLEMENTED_UNTESTED != PASS
COHERENT != PHYSICALLY_PROVEN
REPRESENTATION != AUTHORITY
```

## Fail-closed semantics

For additive/comparative operations:

1. each input needs semantic type, unit and dimension;
2. incompatible representations need an explicit transform;
3. every transform needs an invariant and evidence reference;
4. after transforms, all additive inputs must share one dimension and one unit;
5. missing transform or authority is not guessed.

Example:

```text
36 Pa
  --[1 Pa = 1 J/m^3]--> 36 J/m^3
9 J/m^3
ADD -> EXECUTABLE
```

while:

```text
4 kg/m^3 + 9 J/m^3
TRANSFORM=TOKEN_VAZIO
ADD -> BLOCKED
```

## Useful delivery contract

A material AI answer must expose:

```text
ANSWER_STATE
WHAT_WAS_DONE
EVIDENCE_REFS
LIMITS
TOKEN_VAZIO
NEXT_EXECUTABLE_ACTION
```

The purpose is to force a transition from “response generation” to “work that another human or agent can inspect and continue”.

## Canonical files

- `configs/semantic_context_exam_contract.v1.json`
- `scripts/semantic_context_exam.py`
- `examples/semantic-context-exam/bridge-that-refused-to-lie.example.json`
- `tests/test_semantic_context_exam.py`

## Execution

```bash
python3 scripts/semantic_context_exam.py validate-contract \
  configs/semantic_context_exam_contract.v1.json

python3 scripts/semantic_context_exam.py evaluate \
  configs/semantic_context_exam_contract.v1.json \
  examples/semantic-context-exam/bridge-that-refused-to-lie.example.json \
  --report artifacts/semantic-context-exam-report.json
```

## Boundary

A PASS means the **context operation and epistemic gates are internally coherent under this contract**.

It does not mean:

- model weights were changed;
- tokenizer internals were changed;
- a local/remote LLM executed correctly;
- a physical theory was proven;
- external evidence was independently replicated.

Those remain separate evidence gates.
