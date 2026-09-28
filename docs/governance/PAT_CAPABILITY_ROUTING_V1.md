# PAT Capability Routing V1

Status: **IMPLEMENTED_SOURCE / EXECUTION_EVIDENCE_BOUNDED**  
Executor: `rafaelmeloreisnovo/RafGitTools`  
Federated routing authority: `rafaelmeloreisnovo/Mapa`  
Claim gate: `claim_allowed=false`

## Canonical naming

All secret identifiers are written in uppercase:

- `PAT_ACTIONS`
- `PAT_AGENTS`
- `PAT_CODESPACES`
- `PAT_DEPENDABOT`
- `PAT_ENV`

The canonical display name for the GitHub Environment is `PAT_ENVIRONMENTS`.
Historical receipts that recorded `Pat_environments` are preserved as historical evidence and are not rewritten. GitHub Environment names are case-insensitive, so the uppercase display is a naming normalization rather than a second environment.

## Authority split

`RafGitTools` is the bounded PAT-backed executor/orchestrator.  
`Mapa` is the federated routing/ontology authority.  
Neither repository stores PAT values in documentation, receipts, schemas, logs, or federation records.

`CAPABILITY != PERMISSION != EXECUTION != EVIDENCE != CLAIM`.

## Current matrix

| Secret | Scope | Current state | Proven route |
| --- | --- | --- | --- |
| `PAT_ACTIONS` | repository | `WIRED_MAIN_ONESHOT_READ_ONLY` | exact-SHA private checkout + bounded read/test + receipt |
| `PAT_AGENTS` | repository | `REGISTERED_NOT_WIRED` | preflight only |
| `PAT_CODESPACES` | repository | `REGISTERED_NOT_WIRED` | preflight only |
| `PAT_DEPENDABOT` | repository | `REGISTERED_NOT_WIRED` | preflight only |
| `PAT_ENV` | environment `PAT_ENVIRONMENTS` | `WIRED_MANUAL_ONLY` | provider preflight + main-protection apply/rollback |

## Provider permission hints

These are endpoint requirements, not proof that any stored PAT currently has them:

- `PAT_ACTIONS`: workflow-run reads require `Actions: read`; private target access must also include the exact target repository.
- `PAT_CODESPACES`: list/get requires `Codespaces: read`; lifecycle creation/mutation requires `Codespaces: write`.
- `PAT_DEPENDABOT`: alert reads require `Dependabot alerts: read`; alert updates require `Dependabot alerts: write`.
- `PAT_ENV`: listing environments uses `Actions: read`; creating/updating environments and updating branch protection require `Administration: write`; reading branch protection requires `Administration: read`.
- `PAT_AGENTS`: exact provider endpoint/permission mapping remains `TOKEN_VAZIO_UNSPECIFIED_ENDPOINT`; do not infer it from the secret name.

Candidate operations for unwired PATs remain `TOKEN_VAZIO_PERMISSION_PROBE_REQUIRED` until provider readback proves the required permission. The presence of a secret never promotes a capability.

## Execution rule

All PAT-backed operations must enter through a bounded RafGitTools lane with:

1. explicit capability/secret binding;
2. no fallback to another PAT;
3. exact target identity where applicable;
4. least privilege;
5. no secret-value persistence or printing;
6. provider readback or typed unresolved state;
7. append-only receipt;
8. `claim_allowed=false` unless a separate claim gate is satisfied.

## Federation projection

Mapa should store only capability ID, executor pointer, state, source ref, operation type, evidence/receipt pointer, gap and next. Mapa must never receive or reproduce PAT values.

## R3

`F_ok`: five canonical secret IDs typed; PAT_ACTIONS and PAT_ENV have bounded wired routes.  
`F_gap`: PAT_AGENTS, PAT_CODESPACES and PAT_DEPENDABOT provider permissions are not yet probed.  
`F_next`: add bounded permission probes per PAT, one capability at a time, and promote only from provider readback.
