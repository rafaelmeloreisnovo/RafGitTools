# Federação — segurança de branches via START 08P V1

Successor of the Termux preflight, preserving that route. Scope: any public
repository belonging to the two explicitly authorized owner namespaces
rafaelmeloreisnovo and instituto-Rafael, with exact numeric ID, default HEAD,
live admin metadata and protected manual dispatch from RafGitTools main.

## Operations
* federation_ruleset_inventory: select owner, leave target inputs empty.
  Paginated public inventory, no write; coverage is limited to provider response.
* federation_ruleset_plan: enter owner, name, numeric repository ID, exact
  default branch SHA. Review the receipt, existing rules, bypass scope,
  proposed additive default-only ruleset, and rollback before approving.
* federation_ruleset_apply: same target inputs plus the exact reviewed
  provider_approved_plan_sha256 from the previous plan receipt. Owner dispatch
  is explicit authorization of that one plan, not bulk permission. One POST
  only, no retry; prestate re-read and poststate verification.

Baseline: add missing deletion / non_fast_forward rules in a distinct active
default-branch ruleset with no bypass, never edit/delete/disable existing
rulesets or branch protection. If existing rules have bypass actors, the new
rule can restrict their actions on the default branch; that effect is explicit
in plan. Existing protections with no bypass may yield NOOP. No claims about
negative merge tests, signatures, code quality, PR approval or required checks.
Those advanced rules require producer-specific policy and review (especially
single-owner repositories, merge methods, private CI and check contexts).
No automatic removal, no secret distribution, no arbitrary HTTP endpoint,
no PAT fallback, no PR job with secret, no target code execution.

Private targets deliberately fail closed in this public workflow: the
private target name/receipt must not leak through public workflow inputs or
artifacts. A private executor with a separately approved credential binding
is required; no copy of PAT_ENV is authorized here. The operation inventory
does not prove private coverage or PAT write permission. Do not infer all
federation repositories are public or accessible.

Rollback: for source revert this commit. Provider application is additive;
prior rules remain unchanged, recovery uses a human-reviewed corrective
successor with exact intent, old and new IDs and readback, never automatic
deletion or weakened protection. Receipt intent is persisted before POST.
After a timeout or mismatch, re-inventory before any new action.
SOURCE_ONLY / CI_NOT_RUN / PROVIDER_WRITE_NOT_RUN / claim_allowed=false.
