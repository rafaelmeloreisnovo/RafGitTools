# Repository Privacy Manager — V2

RafGitTools gains a dedicated **Raf Privacy** launcher surface for authenticated inventory and fail-closed bulk `public -> private` repository changes.

## Inventory

The feature calls `GET /user/repos` with:

- `visibility=all`
- `affiliation=owner,organization_member`
- `per_page=100` with full pagination

This intentionally includes repositories owned by the authenticated personal account and repositories reachable through organization membership while excluding collaborator-only repositories.

Pagination has a defensive upper bound. Exceeding it is an explicit failure; partial inventory is never silently presented as complete.

## Eligibility gate

A repository is selectable only when all conditions hold:

1. it is not already private;
2. it is not a fork;
3. it is not archived or disabled;
4. GitHub explicitly reports `permissions.admin=true`.

Missing admin evidence is represented as `TOKEN_VAZIO`, never guessed.

## Live preflight / TOCTOU protection

The inventory shown to the user is not sufficient authority for a destructive mutation. Immediately before every PATCH the app calls:

```text
GET /repos/{owner}/{repo}
```

and revalidates:

- repository numeric identity (`id`) still matches the selected object;
- repository is still public;
- repository is still not a fork;
- repository is still not archived/disabled;
- GitHub still reports `permissions.admin=true`.

If any invariant changed between selection and execution, that repository is skipped fail-closed and no PATCH is sent.

## Mutation

The app sends only:

```json
{"visibility":"private"}
```

to `PATCH /repos/{owner}/{repo}`. Mutations run sequentially. A `401` aborts the remaining queue; `403`, `404`, `422` and other HTTP/runtime failures are recorded with sanitized messages.

For fine-grained PATs, the token must have repository administration write authority. Organization policy can independently restrict visibility changes to organization owners.

## Confirmation and consequences

Before mutation the user must type `PRIVATIZAR N` exactly. The UI warns that a public-to-private transition can affect public social metadata, forks, Pages availability and feature availability depending on GitHub plan/policy.

## Provenance journal — V2

The mutation path uses `RAFGITTOOLS_REPOSITORY_PRIVACY_RECEIPT_V2`.

The journal is **append-only** and stored under Android app-private storage. Every checkpoint receives a unique operation ID + sequence number and is written via `AtomicFile` with `flush` + `fsync` before publication.

Critical invariant:

```text
no durable PLANNED receipt -> zero GitHub PATCH requests
```

Before every external PATCH, the selected repository is recorded as `ATTEMPTING` in a durable checkpoint. If the process/device stops after GitHub accepts the PATCH but before the local result is recorded, that durable `ATTEMPTING` state identifies the exact repository requiring reconciliation.

After each result another checkpoint is attempted. If provenance persistence fails, remaining mutations abort rather than continuing without an audit trail.

Receipt content is deliberately minimized:

- operation/checkpoint identity;
- repository numeric ID + `owner/name`;
- previous and target visibility;
- mutation status;
- sanitized HTTP/status message.

It contains **no PAT/OAuth token, Authorization header, SSH key, repository file contents or secret material**.

## Provenance states

- `DURABLE` — checkpoints remained durable;
- `DURABLE_RECOVERED` — a transient checkpoint failed but a final complete checkpoint was later persisted;
- `FAILED_BEFORE_MUTATION` — initial journal could not be created; zero mutations attempted;
- `PARTIAL_DURABILITY_LOSS` — latest durable checkpoint must be used for reconciliation and remaining mutations were stopped.

## Evidence boundary

Source implementation and tests are present in the feature branch. Build/device execution remain separate evidence gates. No repository visibility is changed by CI or by merely merging this code; execution still requires authenticated user selection and explicit confirmation on the Android device.
