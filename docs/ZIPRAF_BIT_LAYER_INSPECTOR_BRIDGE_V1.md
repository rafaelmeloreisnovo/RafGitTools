# ZIPRAF Bit Layer Inspector Bridge V1

State: TOOLING_BRIDGE / PHASE_A_INSPECTOR_IMPLEMENTED_UNTESTED  
claim_allowed=false

RafGitTools is an inspector/debug/conversion surface, not the format authority.

## Inputs to expose

- W: logical width
- q: active bit-layer depth
- M: block mask
- G(M): resolved geometry state
- block classification: structural / non-structural / corrupt / absent
- E_s: reconstruction error when a reference exists

## Existing nearby components

- `rafaelia_bitraf.c`
- `bitstack.c/.h`
- Q16 geometry and attractor tooling documented in `docs/REPO_MAP.md`

## Safety

Inspector output must distinguish SOURCE, ARTIFACT, EXECUTION, EVIDENCE and CLAIM.
No visual similarity may promote decoder correctness.

## Phase A consumer

The producer-side Phase A vectors are now merged in RafPolimata at `d52afbc38acf6d9580b32cbf9f7f259fa4afdf4b`.

RafGitTools consumes them only through an explicitly supplied JSON file and the pinned authority contract in `configs/zipraf-bit-layer-inspector-v1.json`.

The inspector validates contract identity, byte bit-planes and q reconstruction, but preserves `M`, `G(M)` and T-BL-010 as TOKEN_VAZIO. It does not fetch, rewrite or promote producer state.

F_next: exact-head START gate; after PASS, wire optional UI presentation without moving format authority into RafGitTools.
