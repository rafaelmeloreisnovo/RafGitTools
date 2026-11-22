# AGENTS.md — PAT federation adapter (RafGitTools)

State: SOURCE_PROPOSAL_HOLD; no executable delegation. Read parent `/AGENTS.md`, `/configs/agent-entry-kernel.v1.json`, then `/docs/governance/PAT_CAPABILITY_ROUTING_V1.md`. Mapa owns cross-repository route/state; the target producer AGENTS owns its local write/build/test boundaries.

## Q01–Q12 preflight

Bind role, exact repo/ref/SHA/path, owner, evidence, scope and rollback. Open at most 3 authoritative roots before widening. Resolve the Mapa route and local repository's own AGENTS.md; do not import local AGENTS text from another repo as authority.

## PATs are not transferable permissions

The six public *identifiers* (not credentials) are `PAT_ACTIONS`, `PAT_AGENTS`, `PAT_CODESPACES`, `PAT_DEPENDABOT`, `PAT_ENV`, and `PAT_ENVIRONMENTS`. Never read or copy their values into the repository, Supabase, Drive, stdout, artifacts, or receipts. `PAT_ENV` is not an alias for `PAT_ENVIRONMENTS`.

Only the already documented bounded `PAT_ACTIONS` and protected manual `PAT_ENV` lanes retain their existing authority. `PAT_AGENTS` has read-only identity/org-membership preflight; general cross-repo access, write, issue, PR, branch or administration permission is `TOKEN_VAZIO`, not granted here. Other identifiers remain unwired until exact endpoint/target/perms and environment readback.

No secret-bearing job on PR, fork, untrusted input or cross-repo checkout of unpinned code. Never use a PAT fallback or grant broad `contents:write`. Per-target permissions must be proven at the provider without exposing credentials. For a cross-repo mutation require separate approval and local-owner gates, exact SHA, rollback, per-side receipt and readback. Failure => BLOCKED and claim_allowed=false.

## Supabase / Drive

Drive stores versioned navigation and sanitized append-only receipts. Supabase is optional private metadata/index projection, never the password vault or source authority. Connected Supabase projects were inactive when assessed; no migration, reactivation or synchronization is authorized by this adapter.

## Minimal exit

Output source ref, authority, observed permission boundary, action/NOT_RUN, evidence, gaps, rollback and R3. No implicit autonomous execution or merge.

Machine contract: `/configs/agents-pat-federation.v1.json`.
