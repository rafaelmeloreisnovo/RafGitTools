# Mapa → RafGitTools PAT_ENV provider authority: bounded preflight

**State:** `SOURCE_PROPOSED / LIVE_PAT_PROBE_NOT_RUN / PROVIDER_MUTATION_NOT_AUTHORIZED / claim_allowed=false`

## Baseline and provenance

The canonical `docs/governance/PAT_CAPABILITY_ROUTING_V1.md` and `contracts/PAT_CAPABILITY_MATRIX_V1.json` distinguish six credentials. Only `PAT_ENV` has the existing manually gated provider-administration route, bound to the GitHub Environment `PAT_ENVIRONMENTS` (legacy environment label `Pat_environments`). No token values are stored or copied.

**Do not use `PAT_ACTIONS` or `PAT_AGENTS` as substitutes.** The first is read-only in its proved lane. The second is bound only to repository-factory identity/org-membership GET preflight. Neither has verified Administration:write permission for Mapa. `PAT_ENVIRONMENTS` is a different secret identifier from `PAT_ENV`.

Previous RafGitTools provider preflight may support administrative permission on **RafGitTools**, but it does not establish that `PAT_ENV` can administer **Mapa**. A new, explicit target readback is required. The authenticated ChatGPT GitHub connector has repository admin identity but no provider policy write or Actions-secret write tool.

Observed at Mapa on 2026-10-11:

- `main@de3aa28e203a84f291a61453e0e95f75f911650a`, `protected=false`, status-check enforcement `off`.
- Ruleset `21909304` `enforcement=disabled`; review requirement `0` and four `always` integration bypass actors `20150`, `29110`, `73253`, `1144995` whose identities and necessity are not independently verified.
- PR #750 has no independent APPROVED review; CodeScan preflight lacks `CODESCAN_AUTH_TOKEN`, `CODESCAN_ORGANIZATION_KEY`, `CODESCAN_PROJECT_KEY`.
- Mapa μWRITE real observation receipt: `OBS-20261011-MAPA-PROVIDER-REAL-001` (source endpoints and SHA-256 of connector-returned UTF-8 text, not GitHub HTTP wire bytes).

## Stage P0-A — one bounded credential capability observation

The additive `START.yml` mode `mapa_provider_pat_preflight` is deliberately owner-only, `workflow_dispatch` from RafGitTools `main`, and behind the pre-existing protected environment. It runs only after the existing topology/coherence checks and binds **only** `secrets.PAT_ENV` to the dedicated read-only Python step, not to tests or other jobs.

The Python endpoint allowlist supports six GET calls only: Mapa repository, main branch, branch protection, rulesets list, ruleset #21909304, and Actions secret *names* metadata. It never calls POST/PUT/PATCH/DELETE, follows no HTTP redirect, persists no raw PAT, does not print a token, and returns a typed receipt. A 403/404, missing secret, unexpected response, unknown permission or changed main SHA stays `PARTIAL_PROVIDER_READBACK` or `TOKEN_VAZIO`, never `PASS_AUTHORITY_WRITE`.

The one-time preflight requires a **fresh contract expected-main SHA**. If Mapa advances, create an append-only successor contract/review rather than weakening the SHA guard.

### Manual dispatch after reviewed merge to protected RafGitTools main

In RafGitTools → Actions → **START · RAFAELIA Orchestrated Pipeline** → Run workflow, select the `main` branch and `mode=mapa_provider_pat_preflight`. The GitHub Environment may require additional reviewer approval. These user interface operations are not executable by the current connected chat GitHub tools, which do not expose the `workflow_dispatch` mutation method. The one resulting run is not automatically a success: inspect the exact run/job, output receipt, SHA binding, secret-name-only metadata and terminal state.

This mode is **read-only**. Even a perfect response proves endpoint availability and observed authority flags, *not* `Administration:write` capability, token scope for writes, or permission to bypass other P0 gates.

## Stage P0-B — separate governed provider apply (NOT IN THIS PR)

Before proposing an actual ruleset/branch-protection write:

1. Verify explicit PAT permission scope on the Mapa target and authorized GitHub administration route.
2. Identify the real eligible independent reviewer, and require at least one non-author approval with stale approval handling.
3. Audit four integration bypass IDs and constrain exceptions; `required_approving_review_count=0` is not review enforcement.
4. Configure the three CodeScan credentials through GitHub native Actions secrets administration only. Do not use fake placeholders or print values. Require true analysis + nonempty SARIF upload terminal SUCCESS.
5. Resolve actual provider **status check context names** and ensure available, non-recursive required checks before editing protection.
6. Capture current effective ruleset and branch policy prestate and a tested reversible rollback for the precise surface.
7. Apply to the exact `main` SHA via a **separately reviewed** provider administration change, read back effective protection, and demonstrate a benign zero-approval PR cannot be merged (without ever merging it).
8. Obtain genuine independent approval for the exact PR head, retest all required contexts, then separately decide promotion.

Existing `START.yml provider_environments` runs `PAT_ENV` for RafGitTools main when manually dispatched, and its one-shot configuration currently points at RafPolimata; neither is silently retargeted in this PR. This proposal adds a distinct Mapa READ path, not an auto-apply trigger.

## Tests, stop and rollback

```sh
python3 -m unittest discover -s tests -p 'test_mapa_provider_authority_preflight_v1.py' -v
python3 scripts/validate_mapa_provider_authority_preflight_v1.py
```

All unit fixtures use fake tokens and mocked GET responses; they are not live GitHub secret/permission evidence. The workflow supply-chain and coherence checks must pass on the exact proposed head. Merging this PR is **not** automatic authorization for provider mutation; actual token-bound preflight is manual after source approval.

**Rollback:** close the draft PR. After any future separately authorized merge, use a scoped revert PR; the read-only preflight performs no provider writes. No file deletion, no ruleset mutation, no token persistence, no auto-merge and no rewriting history.

`SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM` · `PAT_PRESENT != PERMISSION_WRITE` · `TOKEN_VAZIO != PASS`
