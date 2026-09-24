# ZIPRAF Bit Layer Inspector Bridge V1

State: TOOLING_BRIDGE  
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

F_next: add read-only inspector adapters after canonical vectors are merged.
