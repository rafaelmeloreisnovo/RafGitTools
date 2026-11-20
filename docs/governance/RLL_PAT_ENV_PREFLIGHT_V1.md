# RLL institutional PAT_ENV preflight — bounded lane V1

Status: `SOURCE_WRITTEN_READBACK / CI_EXACT_HEAD_NOT_RUN / LIVE_PAT_NOT_RUN`. No provider mutation. `claim_allowed=false`.

## Authority and identity

- Routing: `rafaelmeloreisnovo/Mapa` under START HERE Ω V2.2 and AGENTS.md.
- Executor: `rafaelmeloreisnovo/RafGitTools`, file `.github/workflows/START.yml`.
- Producer: `instituto-Rafael/relativity-living-light` (repository ID 1046495816), default branch `main`.
- Credential binding: existing protected RafGitTools environment `Pat_environments` using `secrets.PAT_ENV`. Never copy values, perform fallback, or identify `PAT_AGENTS` as administrator.
- Other repository secrets: even equal names do not prove equal values or rights.
- [RLL provider ruleset #23116926](https://github.com/instituto-Rafael/relativity-living-light/rules/23116926) is inspected by exact ID; a changed ruleset set blocks the probe.

## Independent RLL secret authority (not automatically the same PAT)

The institutional RLL producer maintains [RLL_CREDENTIAL_AUTHORITY_V1](https://github.com/instituto-Rafael/relativity-living-light/blob/a1dbcaac91b0235d21714a40b02328032869186d/docs/governance/RLL_CREDENTIAL_AUTHORITY_V1.md): its Actions repository secret is `GITPAT` and agent-side candidate is `PATGITHUB`. RafGitTools `PAT_ENV` is a distinct secret reference and provenance. This lane does **not** consume RLL `GITPAT`, read its value, or assume token equality. A successful RafGitTools `PAT_ENV` GET against RLL would prove only that token's scoped RLL read access, not RLL's separately stored credential or Administration:write.

## Manual invocation after reviewed incorporation into default branch

Use **RafGitTools → Actions → START · RAFAELIA Orchestrated Pipeline → Run workflow**, branch `main`.

- `mode=provider_environments`
- `provider_operation=federated_preflight`
- `repository_factory_owner=instituto-Rafael`
- `repository_factory_name=relativity-living-light`
- `provider_expected_main_sha=<LIVE_RLL_MAIN_SHA_40_LOWER_HEX>`, copied immediately before dispatch from GitHub.
- Do not run `federated_protect` for RLL: **not allowlisted** for mutation.

The job requires manual owner actor, workflow main branch, attempt=1, exact immutable target SHA, repository ID, public ownership, admin-read permission, branch state, ruleset contents and branch-protection API readback. All five tests execute in the secret-free fast lane and again before the privileged step. The PAT value is only injected into the single GET-only provider step.

An artifact named `start-08p-provider-environments-<run_id>` includes `rll-pat-env-preflight.json`. It contains only a narrow provider status; no token or raw source. A green GET-only probe is **not** proof of `Administration:write` or enforcement of required reviews/checks.

## Rollback, adverse tests and next gate

- **Before merge:** close the review branch without touching `main` (do not delete originals under RAFAELIA preservation policy).
- **After an approved merge:** corrective revert PR with independent review and exact-head CI; no force-reset, no workflow-dispatch of unpinned code.
- **Provider rollback:** not applicable; this route never writes provider settings.
- **Negative checks:** wrong actor, wrong ref, retry attempt, SHA drift, repo ID drift, admin-read false, unexpected ruleset, protection HTTP 403 and denied endpoint => blocked. No fake PASS.
- **Next gate:** after real PAT GET readback, separately design and approve the RLL-specific admin plan with captured current provider policy, no bypass, named checks proven on target branch, independent reviewer, transaction-aware apply+rollback and negative rejection test. Current legacy RafGitTools `apply_main_protection` and `rollback_main_protection` remain separate and unchanged.
- **Supabase:** optional private, RLS-guarded projection of sanitized receipt metadata **after** provider proof; no PAT values, no claim of database synchronization by workflow presence.

`SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM`. `TOKEN_VAZIO != 0`.
