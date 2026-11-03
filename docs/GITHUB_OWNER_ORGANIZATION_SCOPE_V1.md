# GitHub account/organization selector — scoped V1 candidate

## User intent

Select the GitHub **owner account or organization** consistently across repository browse, GitHub-originated Drive destinations, and the Home local inventory bridge. This change is additive and does not change provider permissions or move repositories.

## Authority and data source

- GitHub authenticated repository inventory is the only source for displayed owner candidates.
- Owner type comes from the repository owner's `type` field; no organization membership is fabricated.
- Filtering is local and read-only. `All` is a browsing scope, never write authority.
- Changing the selected owner changes the visible repository candidates in **all three Home tabs**. An owner missing from the refreshed inventory yields **zero matches**, not a silent return to All.
- On authenticated identity change, the saved Composable selection is keyed by the login.
- A repository's exact `owner/name` and its numeric ID, preflight, auth scope and explicit confirmation remain the write-time boundary. This feature does not grant or emulate organization administration.
- Partially fetched inventory is labeled incomplete; missing owners remain `TOKEN_VAZIO`.

## What this candidate implements

1. Reusable Compose owner picker `GithubOwnerPicker` (account/organization; observed counts).
2. Pure fail-closed `GithubOwnerScope` filter with unit-test source.
3. HomeDashboard propagation into remote repositories, private GitHub destinations in Drive bridge, and local GitHub inventory.
4. No visibility changes, repo transfers, secret use or API write from the picker.

## Not yet done; explicit gates

- App-wide selection for Actions, issues, pull requests, repository factory, settings and external cross-repository tools requires individual binding to this scope, with a provider-backed inventory/capability check.
- GitHub organization membership/permissions/SSO confirmation remains a distinct live-provider gate.
- Test source is not test execution. Validate exact-head unit tests, Android compilation, Compose UI integration, and authenticated personal + organization canary before claiming product-wide functionality.
- Reverting the isolated PR is source rollback. No GitHub object moves or permission changes are made.

`SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM`; `IMPLEMENTED_UNTESTED != PASS`; `claim_allowed=false`.
