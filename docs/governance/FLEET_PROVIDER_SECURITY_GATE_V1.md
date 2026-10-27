# Security fleet provider gate V1 — P0 HOLD

**State:** SOURCE_CANDIDATE / PROVIDER_NOT_APPLIED / CLAIM_ALLOWED=false

This is an additive, hosted-Python (stdlib-only) provider adapter. It does not replace the existing RafGitTools START.yml, PAT_CAPABILITY_MATRIX_V1.json, ARM freestanding C/ASM nuclei, provider 08P lane, or official platform APIs.

## Authority
- RafGitTools owns its hosted executor; private Mapa owns the exact 100-repository inventory and routing pointers.
- Secrets stay in GitHub's protected environment, not in code, receipts, tests, or public inventory.
- Read PAT_ENV only at runtime; it is not an alias for PAT_ENVIRONMENTS. Never fallback to PAT_ACTIONS or other PATs.
- Repository admin metadata is not proof of PAT Administration:write.

## Runtime use
Run on a trusted Termux/UserLAnd Python host with a private manifest and a PAT_ENV injected by an authorized protected environment. Never place PAT values in CLI flags, APKs, logs or code. Protect private manifest and receipt paths.

Read-only audit:

    python scripts/provider_fleet_gate_v1.py --manifest /private/manifest.json --repo OWNER/REPO --expected-sha EXACT_SHA40 --reviewer INDEPENDENT --receipt /private/audit-1.json

One-target apply (NOT RUN in this delivery):

    python scripts/provider_fleet_gate_v1.py --manifest /private/manifest.json --repo OWNER/REPO --expected-sha EXACT_SHA40 --reviewer INDEPENDENT --apply-single --confirm OWNER/REPO@EXACT_SHA40 --receipt /private/apply-1.json

## Guarded PUT
1. Require exact repository ID, visibility, branch and 40-hex HEAD SHA with administrative metadata from the provider.
2. Require public target, no existing protection, no rulesets, readable GET and an independent collaborator with write/review capability.
3. Re-read full prestate before one PUT.
4. Enforce admins, prevent force push/deletion, require PR with one approval, stale-review dismissal, last-push approval and conversation resolution.
5. Re-read protection settings and write a nonsecret receipt; required CI checks are intentionally not fabricated.

The controller never bulk applies, merges, changes visibility, reads/edits secrets, dispatches CI, removes protections or silently retries ambiguous writes. If a PUT might have succeeded but readback failed, DO NOT automatically retry. Separately approved rollback and rejected-merge negative proof remain mandatory.

## Known limitations
- GitHub Free public repositories can support branch protection; private repository branch protection/rulesets can require GitHub Pro or a qualifying plan.
- HTTP 403 remains TOKEN_VAZIO_PROVIDER_AUTHORITY_OR_PLAN.
- Collaborator permission is not proof of submitted independent review.
- No downgrade to zero required reviews when a second human is unavailable.
- Existing branch protections or rulesets require manual reconciliation, never blind overwrite.
- Hosted exact-HEAD CI, provider mutation, Android runtime and fleet-wide apply are NOT_RUN.
- Code-level review gates are not provider enforcement; no compliance certification claim.

Next: exact-head CI, protected provider environment, real reviewer, canary provider readback, negative rejected-merge test, rollback drill, sequential reviewed expansion.

R3 = <F_ok: source candidate and unit fixtures; F_gap: provider and human gates; F_next: metadata canary and controlled authorization>.
