# Receipt — Semantic Context Exam V1

Date: 2026-09-28  
Kind: `SOURCE_IMPLEMENTATION + CONTEXT_GOVERNANCE + TEST_SOURCE`  
Parent: ContextBundle V2 + Content Validity Contract V1 + Semantic Invariant Glossary  
claim_allowed: `false`

## Delta

The seven pedagogical parables were converted into seven executable context/epistemic gates:

```text
G01 BALANCE    -> type/unit/dimension
G02 CARPENTER  -> transform/invariant
G03 BAMBOO     -> alternative representations
G04 POTTER     -> coherence vs interpretation
G05 LIBRARY    -> provenance/evidence
G06 BLACKSMITH -> implemented/tested/physical separation
G07 DOOR       -> claim authority/evidence gate
```

Added a dependency-free evaluator, canonical contract, synthetic executable example, and unit tests. The canonical RAFAELIA workflow gate will consume the focused tests and evaluator.

## Evidence state

```text
SOURCE_IMPLEMENTATION = IMPLEMENTED
TEST_SOURCE = IMPLEMENTED
EXACT_HEAD_TEST_EXECUTION = TOKEN_VAZIO
MODEL_TOKENIZER_INTERNAL_CHANGE = NOT_CLAIMED
MODEL_RUNTIME = TOKEN_VAZIO
PHYSICAL_CLAIM = TOKEN_VAZIO
claim_allowed = false
```

## F_ok

The parables now have machine-checkable semantics suitable for ContextBundle/ContextBroker handoff instead of remaining prose-only guidance.

## F_gap

Exact-head execution, Android ContextBroker binding, llamaRafaelia consumer integration, and any physical/domain claim remain separate.

## F_next

Run the canonical repository gate on the exact PR head. Only after PASS should downstream ContextBroker/agent consumers attach this examiner to live selected context.
