# SAF Workspace Import V1

Date: 2026-09-26

## Intent

Convert a user-selected Android Storage Access Framework (SAF) tree into a
real RafGitTools local workspace without giving JGit direct access to a
`content://` URI and without mutating the source tree.

## Route

```text
OpenDocumentTree
  -> persist READ permission only
  -> enumerate SAF tree
  -> copy to app-private staging
  -> require standard .git directory
  -> JGit open/branch validation
  -> atomic rename to versioned private snapshot
  -> LocalRepositoryDao registration
  -> local receipt
  -> Home / Local workspace
```

## Security and provenance boundaries

- Source permission persisted by this flow: READ only.
- Source mutation: NONE.
- Raw source URI: not written to the receipt.
- Receipt stores SHA-256 of the URI plus provider authority.
- Imported repository remote URL is not copied into the Room registry.
- Each import creates a new versioned directory; existing snapshots are not
  overwritten.
- Failed validation deletes staging data and does not leave a registered
  repository.
- Limits: 50,000 files and 512 MiB per import.
- V1 accepts a standard working tree where `.git` is exposed as a directory.
  Worktrees/submodules where `.git` is represented as a file are a future
  extension.

## Evidence states

Before exact-head CI and physical execution:

- source: IMPLEMENTED_UNTESTED
- exact-head Android build/test/lint: NOT_RUN
- physical SAF provider enumeration: TOKEN_VAZIO
- physical JGit validation: TOKEN_VAZIO
- physical DAO registration: TOKEN_VAZIO
- claim_allowed=false

Promotion rule:

`SOURCE_PRESENT != CI_PASS != DEVICE_PASS != SAF_IMPORT_PASS`.

## Rollback

The delta is isolated on branch `feature/saf-workspace-import-v1`.
Closing/reverting its PR removes the feature. Imported snapshots are user data
created only after an explicit in-app import action and are not created by
repository merge or application startup.
