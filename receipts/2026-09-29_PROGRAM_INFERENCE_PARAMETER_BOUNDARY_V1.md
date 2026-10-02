# Receipt — Program / Inference / Parameter Update Boundary V1

Date: 2026-09-29  
Kind: `GOVERNANCE_SCHEMA / FAIL_CLOSED`  
State: `IMPLEMENTED_UNTESTED`  
claim_allowed: `false`

## Delta

The conversation relation-vector core now separates four independent planes:

```text
PROGRAM_STATE
MODEL_INFERENCE_STATE
PARAMETER_UPDATE_STATE
AI_TRAINING
```

Current enforced values:

```text
PROGRAM_STATE          = PROGRAM_OUTPUT_MATERIALIZED
MODEL_INFERENCE_STATE  = NOT_RUN
PARAMETER_UPDATE_STATE = NOT_RUN
AI_TRAINING            = NOT_RUN
```

Training remains blocked while `parameter_update_evidence=[]`.

## Boundary

Processing, tokenization, chunks, graph edges, vectorization, statistics,
Bayesian state, persistence and μDelta generation are program operations and
do not themselves establish model training.

## Gate

`AI_TRAINING` may only be promoted by evidence of a real trainable-parameter
update with parameter identity, before/after states, update method/objective,
data scope, execution evidence and provenance receipt.

## Evidence state

- core schema write: MATERIALIZED;
- unit-test contract write: MATERIALIZED;
- machine-readable contract: MATERIALIZED;
- CI terminal state: PENDING;
- parameter update execution: NOT_RUN;
- AI training: NOT_RUN.
