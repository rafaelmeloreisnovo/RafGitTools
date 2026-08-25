# Server Merge Enforcement Execution V1

State: `READY_TO_APPLY_BLOCKED_AUTHORITY` · `PLAN_ONLY_FAIL_CLOSED` ·
`claim_allowed=false`

## Outcome

This packet turns the recurrent relation

```text
POLICY_DECISION != SERVER_SIDE_MERGE_BARRIER
```

into one bounded control-plane execution envelope. It does not duplicate the
Mapa provider applicator and does not configure GitHub by itself.

## Evidence boundary

Four consecutive Mapa pull requests supplied the discriminant:

- PR #393: Promotion failed at `2026-08-25T01:41:37Z`, server
  enforcement failed at `2026-08-25T01:41:27Z`, and the PR merged at
  `2026-08-25T01:44:30Z`.
- PR #394: Promotion failed at `2026-08-25T02:42:12Z`, server
  enforcement failed at `2026-08-25T02:42:03Z`, and the PR merged at
  `2026-08-25T03:07:48Z`.
- PR #395: CI failed at `2026-08-25T05:20:14Z`, Promotion failed at
  `2026-08-25T05:20:26Z`, server enforcement failed at
  `2026-08-25T05:20:13Z`, and the PR merged at
  `2026-08-25T05:40:01Z`.
- PR #396: Promotion failed at `2026-08-25T05:40:17Z`, server
  enforcement failed at `2026-08-25T05:39:50Z`, and the PR merged at
  `2026-08-25T05:40:48Z`.

The last two merges advanced `Mapa/main` during this Wave. The envelope was
rebound to `423fd961a9c79ebfec7a879a325404191d1865b3`; provider readback still
showed no protection, status-check enforcement off, no required context, and
an empty repository-ruleset list. `RafGitTools/main` exposed the same provider
gap and is recorded as a successor target, not silently changed in this Wave.

## Bound producer

The provider mutation remains owned by:

```text
rafaelmeloreisnovo/Mapa@423fd961a9c79ebfec7a879a325404191d1865b3
scripts/apply_main_branch_protection.py
git blob 87e98d96ede76d47253326345d03b5769c92fad1
```

Desired scoped rule:

- required context: `promotion-control / enforce`;
- strict status checks;
- restrictions enforced for administrators;
- one independent approval;
- stale reviews dismissed and last push approval required;
- no automatic merge.

GitHub's current REST documentation requires repository
`Administration:write` permission to update branch protection. The connected
surface used for this Wave does not expose that mutation authority, so the
credential and apply receipts remain `TOKEN_VAZIO`.

## Local verification

```bash
python3 -m unittest -v \
  tests.federation.test_server_merge_enforcement_execution

python3 scripts/federation/validate_server_merge_enforcement_execution.py \
  configs/server-merge-enforcement-execution.v1.json \
  --report artifacts/server-merge-enforcement-execution-v1.json
```

`PASS_PLAN_ONLY` means only that the envelope is internally fail-closed. It is
not provider configuration and not rejection evidence.

## Authorized operator sequence

1. Re-read `Mapa/main`, branch protection and repository rulesets.
2. Stop if the main SHA or provider prestate differs from the envelope.
3. Capture the complete prestate without secrets.
4. Supply an `Administration:write` credential only through the process
   environment and run the bound Mapa applicator with `--apply`.
5. Re-read the provider and require the exact context, one approval and admin
   enforcement while the main SHA is unchanged.
6. Only after protection is observed, use a zero-approval review fixture to
   obtain a server rejection receipt; never use a merge call while unprotected.
7. Keep manual promotion and `claim_allowed=false` independent of the provider
   configuration result.

## Rollback

The observed prestate was no protection and no repository ruleset. If that
prestate is still authoritative immediately before the apply, rollback is the
target-scoped deletion of the protection rule followed by readback and a
main-SHA equality check. Its state remains `SPECIFIED_NOT_REHEARSED`.

## R3

- `F_ok`: four temporal discriminants, exact producer/SHA/context, and a
  fail-closed envelope.
- `F_gap`: `Administration:write`, provider apply/readback, rollback rehearsal,
  and rejection receipt.
- `F_next`: authorized Mapa apply, then zero-approval server rejection while
  the main SHA remains unchanged.

Primary references are the current GitHub documentation for protected-branch
REST endpoints, protected branches, and repository rulesets. Their canonical
URLs are preserved in the machine-readable evidence delta.
