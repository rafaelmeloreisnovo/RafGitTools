# MASTER COVERAGE READ-ONLY IMPORT V1

date=2026-09-28
claim_allowed=false
state=IMPLEMENTED_PR_VALIDATION_IN_PROGRESS

## Purpose

Allow RafGitTools to consume the RAFAELIA whole-program master coverage packet as a read-only control plane without transferring write authority.

## Exact source configured

```text
repository=rafaelmeloreisnovo/Rafaelia_Private
commit=73695cd8605a84e57a8134cd40d47c9b4a758f90
source_pr=260
source_packet=Wave0 Coverage Snapshot V2 + Authority Registry V3 + Ledger V2
task=master_coverage_import
write_allowed=false
```

The target is an exact Git SHA, not a branch or moving tag.

## Existing provider lane reused

No additional active workflow was added.

The existing `.github/workflows/START.yml` remains the single workflow root.

The provider-actions lane keeps:

- `contents: read`
- `actions: read`
- `PAT_ACTIONS` as the only provider-actions secret
- private access preflight
- exact-SHA checkout
- `persist-credentials: false`
- post-checkout SHA equality verification

## Closed capability pairs

Only these task/repository pairs are accepted:

```text
river7_offline
  -> rafaelmeloreisnovo/ChipQuantum

master_coverage_import
  -> rafaelmeloreisnovo/Rafaelia_Private
```

No arbitrary repository/task executor was introduced.

## Importer behavior

`scripts/import_master_coverage.py`:

- performs no network I/O;
- reads no credentials;
- mutates no target file;
- requires exact lowercase 40-hex source SHA;
- hashes every required JSON file;
- validates registry/ledger/coverage/profile/backlink consistency;
- validates authority classification;
- validates execution-wave coverage;
- emits `MASTER_COVERAGE_IMPORT_RECEIPT.json`.

Required packet roles:

- master workstream registry;
- document obligation matrix;
- coverage snapshot;
- backlink graph;
- authority registry V2;
- active work ledger V2.

## Tests

New deterministic tests cover:

- valid packet PASS;
- ledger/registry mismatch FAIL;
- missing required file FAIL;
- non-SHA ref rejected;
- malformed repository identity rejected;
- one-shot exact private source;
- closed task/repository pairs;
- target checkout remains read-only;
- START remains single-root.

The RIVER-7 receipt assertions were extracted into a read-only helper so the provider task switch does not weaken prior validation.

## Runtime promotion rule

```text
SOURCE_IMPLEMENTED
!= PULL_REQUEST_CI_PASS
!= PRIVATE_IMPORT_RUNTIME_PASS
```

Remote import becomes PASS only after a main push run proves:

1. PAT_ACTIONS read access;
2. exact target checkout identity;
3. importer receipt state=PASS;
4. artifact receipt retained and hashed;
5. final START receipt reconciled without hidden failure.

## F_next

Open PR; reconcile pull-request START checks; after merge, inspect the main one-shot provider-actions run and freeze the exact import receipt.


## Successor correction log

- Initial provider-routing test incorrectly searched the entire START workflow for the token `eval `.
- Existing unrelated/legacy workflow content caused that guard to fail even though the provider lane introduced no generic executor.
- Successor test scopes the invariant to the `provider_actions` block only.
- This is a test-boundary correction; provider runtime authority was not broadened.

Current exact private target:
`73695cd8605a84e57a8134cd40d47c9b4a758f90`

Required successor packet:
- DOCUMENT_COVERAGE_SNAPSHOT_V2.json
- DOCUMENT_AUTHORITY_REGISTRY_V3.json
- active-work-ledger.v2.json
