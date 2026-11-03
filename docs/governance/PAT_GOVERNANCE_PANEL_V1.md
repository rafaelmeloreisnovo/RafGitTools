# PAT governance panel V1 — RafGitTools

State: `BRANCH_ONLY / SOURCE_IMPLEMENTED_UNTESTED / NO_PROVIDER_MUTATION / HOLD`

## Purpose

Extend the **existing** Android Settings → Repository Governance screen, without a
second credential manager or a new privileged API path. The panel displays the
six documented, distinct GitHub PAT identifiers as *contract labels*, never as
proof that a secret exists, has any scope, or has been used.

- UI: `app/src/main/kotlin/com/rafgittools/ui/screens/settings/GovernancePatRouteCard.kt`
- Mount point: `RepositoryGovernanceScreen.kt`
- Local authority: `AGENTS.md` and `docs/governance/PAT_CAPABILITY_ROUTING_V1.md`
- Machine authority: `contracts/PAT_CAPABILITY_MATRIX_V1.json`
- Federated authority: Mapa; producer repository keeps its own protections.

## Capability routing (documented, not provider-confirmed)

| Identifier | Bound intent | Mutations from this panel |
| --- | --- | --- |
| `PAT_ACTIONS` | one-shot exact-SHA read/test, bounded lane | none |
| `PAT_AGENTS` | preflight only, general admin/write unproven | none |
| `PAT_CODESPACES` | provider permissions unresolved | none |
| `PAT_DEPENDABOT` | provider permissions unresolved | none |
| `PAT_ENV` | separate manually gated GitHub protection lane | none |
| `PAT_ENVIRONMENTS` | distinct name, provider scope unresolved | none |

`PAT_ENV != PAT_ENVIRONMENTS`. No fallback or substitution between PATs.
Never query, display, copy, log or persist secret values.

## Target and permission gate

1. Human selects one repository in the existing governance UI.
2. The existing **Audit** action checks provider configuration, not PAT values.
3. The existing provider-settings action opens GitHub; this panel does not
   create a new mutation endpoint.
4. If change is genuinely necessary, a human reviews exact owner/repository,
   branch/ref, HEAD SHA, endpoint-level admin permission, existing protection,
   ruleset conflicts, required checks, dry-run, impact and rollback.
5. Only an already authorized, protected manual `PAT_ENV` lane can execute a
   separate provider mutation. Verify provider readback and append receipt.
6. Unknown/403/stale/mismatched or competing protection => `BLOCKED/TOKEN_VAZIO`.
   An account-level admin flag or registered PAT name is not sufficient.
7. Never apply blanket administration across all repos or public-CI secrets.

The existing 08P proposal is narrowly scoped; its public-repo allowlist must
not be extrapolated to every repository. Secret-bearing workflows are forbidden
on untrusted PR/fork code. The app screen does not dispatch or authorize them.

## Regression / acceptance gates

- [x] Additive route only: screen already exists; no existing control removed.
- [x] No new external dependency, backend endpoint or token provider in source.
- [x] No secret value or authentication header in UI or documentation.
- [ ] Kotlin/Compose build on exact HEAD: `NOT_RUN`
- [ ] UI/emulator/physical Android visual acceptance: `TOKEN_VAZIO`
- [ ] Existing deep-audit and provider-link behavior regression suite: `NOT_RUN`
- [ ] Independent security review and provider-protection readback: `TOKEN_VAZIO`
- [ ] Release: `HOLD`

No workflow, secret, environment, branch protection, ruleset, merge or app
release is changed by this panel.

Rollback: abandon this isolated branch, or remove the additive panel and
mount call through a reviewed compensating commit. Historical main remains
the authority until a separate evidence-gated promotion.

SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM.
`IMPLEMENTED_UNTESTED != PASS`; `claim_allowed=false`.
