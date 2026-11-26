# Termux provider preflight via 08P — 2026-11-26

SOURCE_ONLY / HOLD. The two public producer identities were observed by GET:
- termux-app-rafacodephi: ID 1082194563, default master, ruleset 21908888.
- termux-packages: ID 1324848257, default main, ruleset 26461859.

The existing START lane is reused. After reviewed merge into main, the owner
manually runs START with mode=provider_environments,
provider_operation=termux_ruleset_preflight, repository_factory_owner=rafaelmeloreisnovo,
repository_factory_name equal to one exact target, and provider_expected_main_sha
equal to that target's fresh default branch SHA. Use one target per run. PAT_ENV
remains in the existing environment, not in input, source or artifacts.

The lane is GET-only; a preflight is not Administration:write proof. It inventories
rulesets, including inherited lists, classic protection HTTP result, exact target
identity and bounded scope. It records a canonical prestate digest and no secret.
Unknown scope, readback failures or stale SHA block. No PUT/POST/DELETE and no
automatic retry or merge.

App authority: governance/provider/PROVIDER_RULESET_TARGET.v3.json at the
observed master 78fb037f60a0d9e013d066eba3b60b92fe1423aa.
Packages: local required-check contract not yet established. Do not transplant
App's exact policy to Packages. Existing all-branch rules and bypass scope need
explicit reconciliation; do not narrow coverage, silently remove existing
bypass actors or infer independent review. The existing provider enforcement
executor requires an independently verified reviewer and rollback evidence;
this preflight does not circumvent that gate.

Next: review receipt, exact producer policy and reviewer/rollback evidence,
then review a bounded mutation plan in a successor PR. No provider state changed.
Rollback: revert this source delta; no remote rule was created or deleted.
claim_allowed=false.
