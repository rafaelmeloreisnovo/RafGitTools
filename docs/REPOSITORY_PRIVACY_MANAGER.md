# Repository Privacy Manager — V1

RafGitTools gains a dedicated **Raf Privacy** launcher surface for authenticated inventory and fail-closed bulk `public -> private` repository changes.

## Inventory

The feature calls `GET /user/repos` with:

- `visibility=all`
- `affiliation=owner,organization_member`
- `per_page=100` with full pagination

This intentionally includes repositories owned by the authenticated personal account and repositories reachable through organization membership while excluding collaborator-only repositories.

## Eligibility gate

A repository is selectable only when all conditions hold:

1. it is not already private;
2. it is not a fork;
3. it is not archived or disabled;
4. GitHub explicitly reports `permissions.admin=true`.

Missing admin evidence is represented as `TOKEN_VAZIO`, never guessed.

## Mutation

The app sends only:

```json
{"visibility":"private"}
```

to `PATCH /repos/{owner}/{repo}`. Mutations run sequentially. A `401` aborts the remaining queue; `403`, `404`, and `422` are recorded per repository.

For fine-grained PATs, GitHub requires repository **Administration: write**. Organization policy can independently restrict visibility changes to organization owners.

## Confirmation and consequences

Before mutation the user must type `PRIVATIZAR N` exactly. The UI warns that GitHub can erase stars/watchers, detach existing public forks, affect Pages availability, and change availability of some security/analysis features when a public repository becomes private.

## Receipt

Each run writes `RAFGITTOOLS_REPOSITORY_PRIVACY_RECEIPT_V1` under Android app-private storage. It contains repository identity, old/target visibility, status, sanitized error information, and no token/header/repository contents.
