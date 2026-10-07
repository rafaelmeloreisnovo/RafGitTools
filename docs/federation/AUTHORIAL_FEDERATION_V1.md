# Authorial Federation V1 — RafGitTools

**State:** `SOURCE_ARCHIVE / RIGHTS_GATED / NOT_IN_APP_BUILD`  
**Base:** `main@80bcaa59562a809d887cfba884f81b95cff7fe8e`

## Zero-friction entry for humans and agents

Read in this order:

1. `configs/authorial-federation.v1.json` — machine route and exact source refs.
2. `federation/authorial/README.md` — source-archive rules.
3. Source-specific provenance and license beside the imported files.
4. Only then inspect imported source.

Hard rule:

`REPO_OWNER != AUTHOR`  
`HEADER_ADDED != SOLE_AUTHORSHIP`  
`SOURCE_IMPORTED != BUILD_INTEGRATED != TESTED != PASS`

## Why PCR lands here

The PCR repository is a Magisk derivative. Its own authorship analysis identifies the RAFAELIA audit and telemetry systems as Rafael additions while preserving Magisk/third-party ownership elsewhere.

Imported here:

- `pcr/native/rafaelia_audit.rs`
- `pcr/native/rafaelia_telemetry.rs`
- source GPL license
- source authorship analysis and author registry

They are **reference/source archive only**. They still assume parts of the Magisk runtime and are not wired into RafGitTools.

## Cross-repo route

- **RafPolimata** receives low-level/math/freestanding-oriented authored source.
- **RafGitTools** receives governance, audit, telemetry, orchestration and provenance-oriented authored source.
- **Gaia** remains blocked until file-level rights/authorship evidence is explicit.
- **Rafaelia_Private** is evaluated per subtree because its root license is custom and file-level licenses may override it.
- **Rafcodifi** is not silently equated with `Rafcodephi_Sdk...`; the probable candidate is indexed but not copied until identity is grounded.

## Agent stop conditions

Stop and emit `TOKEN_VAZIO` when any of these is missing:

`SOURCE_REF | AUTHORSHIP_EVIDENCE | LICENSE | DESTINATION_ROLE | BUILD_BOUNDARY`.

Do not search for a green build by weakening rights, provenance, dependency, or provider gates.
