# RafGitTools — Organização e repositórios privados · hotfix V1

Status: `SOURCE_IMPLEMENTED / CI_UNVERIFIED / DEVICE_E2E_NOT_RUN`.
Authority: RafGitTools Android client; GitHub provider controls access.
Source base: `main@f86b9f1994dd0063412985b091a2141193cfe85e`.
Candidate: `fix/org-private-repository-selector-20261106`.

## User-observed gap

The GitHub personal repo picker and the Drive/private destination list do not
reliably expose private repositories owned by the organization
`instituto-Rafael`. This is a report, not an observed failure of the exact
candidate build. Public/private organization repositories exist in provider
results, but the Android PAT's actual scope has not been independently read.

## Minimal reversible change

- `GET /user/repos?affiliation=owner,collaborator,organization_member` explicitly
  includes organization membership. Do NOT send the conflicting `type` query
  parameter on that endpoint.
- `GET /user/orgs` discovers provider-visible organization names, but an
  organization membership is NOT evidence of read access to all private repos.
- Picker has an optional manual organization login for cases where the PAT
  cannot list organization memberships.
- Upon explicit selection, `GET /orgs/{organization}/repos?type=all` fetches
  authorized owner-scoped repository metadata, at most 50 pages of 100.
  Every returned repository owner must match the requested organization.
- The selected organization is applied to Remote, Drive/private destinations
  and Local Home views. Private destinations remain filtered by `isPrivate`
  and require live write-time revalidation for any mutation.
- Cancel old organization requests on new lookup or auth transition. Network
  errors preserve previously observed metadata in the same authenticated
  session but mark the new request partial; never report a false full inventory.
- No tokens, emails, private repo names, raw file contents or API response
  bodies are stored in public manifests or workflow logs.

## Falsifiers

1. Login with a PAT actually authorized to read `instituto-Rafael` repos.
2. Open Home → **Conta / organização GitHub**; choose the organization or use
   **Login da organização** to search it directly.
3. Confirm that Remote includes only `instituto-Rafael` owners and that
   Drive/private destinations include only returned repositories with
   `isPrivate=true`. Do not infer absent private repositories from a 200 OK.
4. Simulate partial pagination, GitHub 403, missing organization discovery,
   invalid login, token expiry, logout, and rapidly changing the selected org.
5. Confirm no stale identities survive logout or owner change.
6. Run source tests, JVM unit tests, exact-SHA Android compilation, and actual
   Moto e(7) Power physical UI canary. Each is a separate evidence gate.

## Scope limitation

`/user/repos` may provide a partial view when the PAT is restricted to a
single resource owner, when an organization requires SSO approval, or when
permission/installation policies limit repository access. The application
cannot enlarge PAT privileges. A missing private repository means
`TOKEN_VAZIO_PROVIDER_SCOPE` until the PAT and organization permission are
verified; it does not mean the repository does not exist.

No automatic organization repo transfers, visibility modifications, secret
changes, upload to public repos, or scientific claim promotions.
`SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM`.

Rollback: close candidate draft PR, keep `main` and PAT unchanged, append a
successor receipt upon contradictory provider/device evidence.
