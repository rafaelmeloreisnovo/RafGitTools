# GitHub Credential Broker — Authority Boundary V1 (25 Oct 2026)

State **CANDIDATE_HOLD**. This is an additive documentary/security contract, not a token rollout, secret migration, workflow authorization, or proof of installed PAT scope.

## Source facts
- GitHub connector reports admin-level **account repository permission** on **95** accessible repositories owned by `rafaelmeloreisnovo`. This is not proof that the app token can administer repo settings: branch-protection GET returned HTTP 403 on RafGitTools, RLL and Mapa.
- RafGitTools `.github/workflows/START.yml` references `PAT_ACTIONS` and `PAT_ENV`. Existing `docs/governance/PAT_CAPABILITY_ROUTING_V1.md` identifies six distinct PAT IDs. **Never alias `PAT_ENVIRONMENTS` to `PAT_ENV`.**
- Mapa references a distinct `RAFAELIA_ADMIN_TOKEN` in `.github/workflows/provider-governance-admin-closure.yml`; presence and scope of this repository secret were **not verified**.
- GitHub connector cannot read/set Actions secret metadata/value through its allowed methods. Secrets are not available to this documentation workflow.

## One authority, not one unrestricted secret
- **RafGitTools**: bounded executor; **Mapa**: typed authority/routes/evidence; **RLL**: source producer.
- For routine CI use the built-in `GITHUB_TOKEN` with `contents: read` unless a task's specific permission is needed.
- For cross-repo jobs prefer an installation-scoped **GitHub App** with short-lived tokens, selected repositories and only requested actions. A fine-grained time-limited PAT is a bounded fallback. An account-wide admin PAT in ordinary CI is **prohibited by this candidate contract**.
- Owner-approved administrative operations must be isolated in a manually triggered environment, exact repository allowlist, exact SHA, provider readback and rollback. Admin status or PAT identifier never implies these gates passed.

## Separation of storage and authority
A personal account's repository secrets are independently scoped; don't claim that one org secret is shared across personal repositories. An organization-level secret is available only under an organization owner and its configured repository policy. No PAT value is held in source, logs, receipts, Mapa, RLL or this file.

## Gates to close
1. Provider-side **metadata-only** audit: each secret identifier, scope, expiration, repo access and endpoint-specific permission; never print or export values.
2. For each operation, record capability → issuer → repository allowlist → permission → exact HEAD → receipt → negative test.
3. Confirm runner trust boundary (PR/fork), prevent untrusted workflows from receiving admin credentials, and prevent automatic merge.
4. If admin endpoint still returns 403, remain `TOKEN_VAZIO_PROVIDER_AUTHORITY`, do not attempt using alternate secrets silently.

Machine contract: `contracts/GITHUB_CREDENTIAL_BROKER_V1.json`. Existing PAT routes are preserved. No branch-protection setting, secret, token, or CI workflow was modified.

R3: F_ok=bounded control-plane contract authored; F_gap=provider secret/exact permission readback and real job tests; F_next=owner-side provider gate + independent scoped execution; no promotion by documentation alone.
