# AGENTS.md federation — PAT capability boundary V1

Status: `SOURCE_PROPOSAL_HOLD` · `claim_allowed=false` · 2026-11-02.

This is a scoped federation **entry contract**, not a new token broker or a grant of cross-repository access. The existing RafGitTools `AGENTS.md`, `configs/agent-entry-kernel.v1.json` and `docs/governance/PAT_CAPABILITY_ROUTING_V1.md` remain authoritative for executor and PAT inventory; Mapa owns cross-repository routing. Local producer `AGENTS.md` owns local implementation. Drive remains reconstruction/ledger, Supabase an optional private index once a project and policy are approved.

### Routing and gates

```text
INTENT → CURRENT_STATE → SOURCE_MIN → MAPA_ROUTE → TARGET_AGENTS
→ CAPABILITY_ID → PROVIDER_PERMISSION_READBACK → EXPLICIT_AUTHORITY
→ EXACT_TARGET_REF/SHA → PROTECTED_EXECUTION_GATE → RECEIPT(S)
→ Drive μWRITE → R3
```

The provider must prove target repository selection and operation-level permissions. PAT names do not prove permissions. The six identifiers are distinct and case-sensitive: `PAT_ACTIONS`, `PAT_AGENTS`, `PAT_CODESPACES`, `PAT_DEPENDABOT`, `PAT_ENV`, `PAT_ENVIRONMENTS`. Treat the last two as **different identifiers**; the Environment named `PAT_ENVIRONMENTS` is not itself proof of the similarly named secret's scope. Do not read token contents or insert them into public source, Drive or Supabase.

### Existing capability boundaries are preserved

* `PAT_ACTIONS`: existing bounded main one-shot read/test at pinned SHA; this proposal does not widen it.
* `PAT_ENV`: existing manual protected administration path, unchanged.
* `PAT_AGENTS`: identity/org-membership GET-only preflight; generic cross-repository writes and administration remain `TOKEN_VAZIO`.
* `PAT_CODESPACES`, `PAT_DEPENDABOT`, `PAT_ENVIRONMENTS`: no automatic wiring; require provider observation for concrete endpoint/scope.

### Denials / threat model

Block secret fallback, forks and pull-request secret use, untrusted code, missing exact target/SHA, missing producer authority, undocumented provider scopes, secret values in logs or artifacts, permission broadening, data transfer across providers, automatic merge or release, and confusing a source validator PASS with live agent execution. If a credential leaks, stop dispatch and rotate it using provider controls; preserve only a redacted incident receipt.

### Supabase target — not applied

Both connected Supabase projects were `INACTIVE` at observation. Do not unpause, create or alter schema to claim this task is complete. Future minimal private index should store only `event_id, source_ref, repo_ref, policy_version, capability_id, state, evidence_ref, parent_id, observed_at`, with no PAT value or private content. Keep schema non-exposed, deny anon/authenticated grants, and require migration/readback/negative access tests before enabling a writer.

### Validation and rollback

Source checks: JSON parsing, six identifiers distinct, `claim_allowed=false`, `automatic_dispatch_allowed=false`, fallback forbidden, no workflows modified, existing PAT lanes unchanged. Runtime authorization and cross-repo E2E remain `NOT_RUN`. Rollback by closing the draft PR; `main`, `START.yml`, secret store and provider ACL remain unmodified.

Source-of-truth machine entry: `configs/agents-pat-federation.v1.json`; local agent adapter: `federation/agents-pat/AGENTS.md`. Mapa companion must be reviewed separately; no Mapa producer authority is implied by this document.

R3: `F_ok=bounded source federation`, `F_gap=provider permissions + inactive Supabase + E2E`, `F_next=verify provider metadata per endpoint without secret disclosure, exact head checks and human review`.
