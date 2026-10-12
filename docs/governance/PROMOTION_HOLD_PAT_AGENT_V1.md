# Ω Promotion Hold Agent — PAT_AGENTS title-only v1

**Executor:** `rafaelmeloreisnovo/RafGitTools` · **Federated route:** `rafaelmeloreisnovo/Mapa`  
**Source state:** `IMPLEMENTED_UNTESTED` until exact-head `START.yml` finishes; **live PAT authority:** `TOKEN_VAZIO_PERMISSION_NOT_PROBED`.  
**Decision:** `claim_allowed=false`, `promotion_allowed=false`, `automatic_merge=false`.

## Problem and source of truth

An open PR can have all scoped source checks green and still be **ineligible for promotion** because of missing rights, independent approval or actual GitHub provider enforcement. The first visible words of an affected PR title must explain the P0 obstacle:

`[NÃO PROMOVER — P0: <documented blocker>] <component>`

A title is an easily visible warning, **not** an enforced server merge protection. Even a successful title write cannot authorize promotion.

The machine contract at `configs/promotion-hold-agent.v1.json` pins exactly these targets and their current proposed HEADs:

- `Mapa #750` — `main` protection disabled, CodeScan missing SARIF, and no independent reviewer, backed by 38176049162/38176049186 and the previous PR receipt.
- `RafPolimata #431` — redistribution-rights/provenance P0 and independent review unresolved; device execution remains untested.
- `RafPolimata #432` — redistribution-rights/provenance P0 and independent review unresolved; real ELF/DEX/NUMA corpus and physical execution untested.

The contract is intentionally exact-head. **When a producer PR receives a new push, its old SHA ceases to authorize any PAT action.** A new independent readback and reviewed manifest successor are required. Do not erase unknowns or silently broaden this allowlist.

## Integration in the existing START

`.github/workflows/START.yml` remains the **only active workflow**. All push and PR runs execute the offline unit/falsifier contract from the core coherence lane. Those checks are source-scope only; they **do not access PAT_AGENTS**.

The PAT-bearing job is separate: `promotion_hold_pat_agent`, manual owner-only on protected `main`, after topology and coherence PASS, under existing GitHub Environment `Pat_environments`. It has `permissions: contents: read` for its normal `GITHUB_TOKEN`. Only its title-control execution step receives `secrets.PAT_AGENTS` via `RAFAELIA_AGENTS_TOKEN`. No fallback to `PAT_ACTIONS`, `PAT_ENV`, `PAT_ENVIRONMENTS`, `GITHUB_TOKEN` or an unscoped token. Exact provider authorization on **both target repositories** is not inferred from the secret's name or existence.

Because the existing START dispatch already has 12 input keys, v1 **reuses** `repository_factory_name` as the strictly validated *target ID* and `provider_operation` as the operation selector, instead of adding more workflow_dispatch inputs. These fields have their original meaning in their existing lanes.

### Manual steps (after source review and authorized merge into RafGitTools main)

1. RafGitTools → Actions → **START · RAFAELIA Orchestrated Pipeline** → Run workflow; choose **main**.
2. Select `mode=promotion_hold_pat`; select `provider_operation=inspect_hold` to GET and verify or `mark_hold` for title-only PATCH.
3. Enter **one** of `mapa750`, `rafpolimata431`, `rafpolimata432` in `repository_factory_name`.
4. Read the environment approval policy and execute only under the authorized owner account; inspect the uploaded `promotion-hold-receipt.json`, job state and exact PR HEAD. No manual dispatch was executed by this source PR.

**Inspect:** GET a single pinned PR. A matching title produces `HOLD_TITLE_CONFIRMED_WRITE_PERMISSION_UNPROVEN`; a missing title, 403/404, stale HEAD, closed PR, wrong base or missing secret fails closed.

**Mark:** the same GET preflight, followed by a single `PATCH /repos/<owner>/<repo>/pulls/<id>` body containing **only** `{"title":"[NÃO PROMOVER ...] ..."}` *if needed*, followed by a second GET. An already correct title is idempotent and **does not prove** PAT write permission. A successful changed title is evidence of title-write permission for that exact repo/PR/time only; never proof of `Administration:write` or branch-protection enforcement.

All outputs/receipts redact provider payload and token. Redirects, arbitrary hosts, other HTTP methods and other PATCH fields are disallowed. Failed writes/readback retain blocked states. No title is removed by this agent.

## Guardrails, limitations and gaps

- **P0 server enforcement** belongs to the target producer's own GitHub rulesets/branch protection with independent review and real negative rejection test. RafGitTools source cannot impose required CI checks on another repository merely by running locally.
- External pushes in Mapa/RafPolimata **do not automatically trigger** RafGitTools `START.yml`; a separate authorized dispatch/webhook integration would be required. This v1 does not claim such integration. The CI auto-runs only source falsifiers on RafGitTools's own push/PR events.
- GitHub Environment protection and PAT_AGENTS actual effective permissions remain `TOKEN_VAZIO` until provider-native readback. `PAT_ENV` remains the only separately owned provider administration route; do not substitute credentials.
- `SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM`; `TOKEN_VAZIO != PASS`; `HOLD_TITLE_CONFIRMED != SERVER_ENFORCED`.
- Neither a test PASS nor a PATCH enables self-approval, merges, credentials administration, CodeScan SARIF, license acceptance or device/runtime claims.

## Falsification, rollout and rollback

```sh
python3 -m unittest discover -s tests -p 'test_promotion_hold_pat_agent_v1.py' -v
python3 scripts/promotion_hold_pat_agent_v1.py --operation validate
```

The tests reject malformed contracts, wrong targets, unsafe dispatch, bool-as-PR, SHA drift, wrong base, title drift during inspect, missing/denied PATCH and post-write drift. No real PAT appears in those tests.

**F_ok:** exact three-target policy + offline tests + safe single-workflow route. **F_gap:** exact-head CI, real PAT scope and environment, independent reviews and provider-native target enforcement. **F_next:** independent code review → authorized RafGitTools merge → protected manual inspection → only then targeted title mark if needed; separately fix server protections. **Rollback:** close unmerged PR or perform reviewed scoped revert, preserve GitHub/Drive historical receipts; never force-push or delete source.
