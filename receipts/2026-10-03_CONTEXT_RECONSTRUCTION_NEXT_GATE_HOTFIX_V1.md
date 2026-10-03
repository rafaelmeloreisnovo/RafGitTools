# Receipt — Context Reconstruction NEXT_BEST_GATE Hotfix V1 — 2026-10-03

State: `IMPLEMENTED_UNTESTED / claim_allowed=false`

## Intent

Reduce human/AI reconstruction friction without adding middleware, weakening gates, broadening scope or converting `TOKEN_VAZIO` into an invented conclusion.

## Source / authority

- repository authority: `rafaelmeloreisnovo/RafGitTools`
- federated routing authority remains: `rafaelmeloreisnovo/Mapa`
- base: `main@196f021cb1ea40841d20d5cbb522109db5b4cb00`
- predecessor: merged context-reconstruction router PR `#617`
- branch: `docs/next-best-gate-router-v1-20261003`

## Causal observation

The repository already had both halves of the required behavior:

1. context reconstruction router with `source_min <= 3`;
2. deterministic priority/stop policy in `configs/agent-entry-kernel.v1.json` plus uncertainty/friction governance V3.

The navigation surface did not bind them as an explicit route for selecting one next gate when several gaps were available. The hotfix therefore links existing authorities instead of creating a new scoring engine or parallel backlog.

## Delta

- `configs/context-reconstruction-routes.v1.json`
  - adds `NEXT_BEST_GATE`;
  - preserves existing schema/version;
  - uses exactly two existing authority sources;
  - keeps priority selection separate from closure and claim promotion.
- `docs/navigation/CONTEXT_RECONSTRUCTION_START_V1.md`
  - documents the deterministic selection order and stop conditions;
  - maps DMAIC to the existing execution lifecycle as discipline only, not certification;
  - explicitly preserves `TOKEN_VAZIO` as a valuable typed state.
- `docs/INDEX.md`
  - exposes the no-friction decision route near the human/AI entry point.
- `tests/test_context_reconstruction_registry.py`
  - pins the route to the existing priority authorities and fail-closed claim boundary.

## Invariants preserved

```text
SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM
TOKEN_VAZIO != 0
IMPLEMENTED_UNTESTED != PASS
priority-selection != gap-closure != claim-promotion
friction-reduction != gate-reduction
route != authority
```

No runtime code, Android behavior, provider configuration, corpus payload, private material, release state or domain/scientific claim is changed by this delta.

## Selection rule now exposed

```text
non-compensatory P0 blocker
→ upstream dependency
→ READY_TO_TEST with observable exit criterion
→ smallest reversible/falsifiable delta
→ cross-repository blocker with broader dependent fan-out
→ oldest unresolved observation
→ STOP on exit / authority / dependency / no marginal gain
```

This is a deterministic ordering rule, not a numerical truth score. Connection count, symbolic density and combinatorial reachability do not increase evidential confidence.

## Evidence status at write cut

- source readback: PASS
- branch materialization: PASS
- structural validator on resulting exact head: `NOT_RUN_AT_WRITE_CUT`
- repository CI on resulting exact head: `NOT_RUN_AT_WRITE_CUT`
- merge: `NOT_AUTHORIZED_BY_THIS_RECEIPT`
- claim_allowed: `false`

## Falsifiers / anti-regression

The change must fail validation or remain unpromoted if:

- `NEXT_BEST_GATE` disappears from the registry;
- its sources are replaced by a copied/parallel priority implementation;
- `source_min` exceeds 3;
- `next_on_missing` stops preserving `TOKEN_VAZIO`;
- selection is documented as closure or claim promotion;
- documentation claims certification/conformance without corresponding evidence;
- unrelated runtime/provider/private scope changes enter this PR.

## Rollback

Revert this branch/PR delta only. PR #617 and the pre-existing agent/uncertainty authorities remain valid predecessors and must not be rewritten.

## R3

`F_ok`: smallest navigation hotfix is materialized and reuses existing authorities.

`F_gap`: exact-head structural/CI evidence and provider PR readback remain required before PASS; selected domain gaps remain independently open.

`F_next`: open draft PR, inspect exact-head checks, record PASS/FAIL without weakening gates, then append one longitudinal μWRITE only if the delta survives evidence.
