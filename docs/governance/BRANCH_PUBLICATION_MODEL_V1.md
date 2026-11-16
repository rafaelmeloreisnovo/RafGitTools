# Branch and publication model v1

## Names and responsibilities

| Ref | Responsibility | Human wait |
|---|---|---|
| `feature/*`, `fix/*` | Small reversible work | Fast gate only |
| `develop` | Continuous integration | No wait for heavy lane |
| immutable candidate SHA | Full asynchronous evidence | Runs independently |
| `main` | Published, reconstructible state | Explicit authorization |

`main` is the publication branch. A later release tag identifies a published version; no second permanent publication branch is needed, so no extra branch can silently drift.

## State machine

`PROPOSED -> FAST_RUNNING -> FAST_PASS_SCOPED -> INTEGRATED_DEVELOP`

In parallel, an exact `develop` SHA may become:

`CANDIDATE -> HEAVY_RUNNING -> READY_FOR_AUTHORIZATION | BLOCKED | FAIL`

Only `READY_FOR_AUTHORIZATION` plus explicit authorization may open the path to `main`. New work may continue on `develop` while the immutable candidate is tested.

## Required provider controls

Before autonomous promotion is enabled, GitHub rules must enforce:

1. PR-only changes to `develop` and `main`.
2. Required fast check on `develop`.
3. Required full receipt for the exact candidate SHA on `main`.
4. No force-push or deletion of protected branches.
5. Explicit authorization for publication.
6. Pinned actions, least-privilege tokens, concurrency cancellation, timeouts, and retained receipts.

Until those controls are observed, promotion remains `BLOCKED`; documentation or a green fast check cannot substitute for provider enforcement.

## Long jobs

A two-to-four-hour job is a queued evidence producer, not an interactive lock. It publishes state and receipt independently. Failed or stale evidence invalidates only that candidate; it does not stop unrelated development. Retry is bounded and targets only failed components when possible.

## Community transition

The policy is role-based, not person-based. Today one maintainer may authorize publication; future protected environments or teams can assume the same role without changing the branch contract.
