# Workflow Master Index Crosswalk v1

Status: `METHOD_DEFINED / claim_allowed=false`

## Authority split

| Question | Authority |
|---|---|
| What does this mean? Which module or support layer applies? | `/workflow-master-index.json` — semantic v1 |
| Who may execute, with which runtime evidence and rollback? | `/configs/workflow-master-index.json` — control plane v2 |
| Does a request cross meaning and execution? | both profiles through the crosswalk |
| Do the profiles conflict? | `BLOCKED + CONTRADICTION`; no silent precedence |

The identical phrase “Workflow Master Index” names two complementary projections, not two interchangeable authorities.

## Seven-state join

The crosswalk covers each semantic layer `S01..S30` exactly once across:

`PSI_INTENT → CHI_OBSERVE → RHO_NOISE → DELTA_TRANSFORM → SIGMA_CUSTODY → OMEGA_CLOSE → PSI_REOPEN`.

Every route points to concrete control-plane fields. Unknown fields, missing layers, duplicate coverage, silent precedence, automatic merge or directive-based claim promotion fail closed.

## Directive boundary

A `DIRECTIVE_EVENT` may interpret, route, authorize scoped writing or block. It does not retroactively rewrite history, authorize destructive action, merge automatically or promote a claim.

Canonical directive schema: `rafaelmeloreisnovo/Mapa:schemas/directive-event.schema.json`.

## Validation

```bash
python3 scripts/federation/validate_workflow_master_crosswalk.py \
  --semantic workflow-master-index.json \
  --control configs/workflow-master-index.json \
  --crosswalk configs/workflow-master-index.crosswalk.v1.json

python3 -m unittest tests/federation/test_workflow_master_crosswalk.py -v
```

A passing report proves routing coherence only. Runtime, conformity, certification and scientific truth remain separate gates.
