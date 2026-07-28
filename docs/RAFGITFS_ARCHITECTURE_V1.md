# RafGitFS — GitHub Virtual Storage Architecture V1

**State:** FOUNDATION_IMPLEMENTED / RUNTIME_NOT_CONNECTED / CLAIM_ALLOWED=false  
**Date:** 2026-07-25  
**Scope:** Prompt 1 of the RafGitFS implementation sequence.

## 1. Purpose

RafGitFS presents GitHub repositories as a navigable virtual storage surface inside RafGitTools while preserving Git semantics, provenance, authorization, review and rollback.

```text
account
→ organization or user scope
→ repository
→ ref (branch/tag/commit)
→ directory
→ file
```

RafGitFS is not an Android kernel mount, a FUSE filesystem, an SD-card replacement or an unrestricted rclone clone. It is an application-level virtual tree governed by GitHub identities, refs and content SHAs.

## 2. Authority boundaries

```text
GitHub       = canonical remote versioned content
Room         = reconstructible metadata/cache index
Workspace    = bounded local working copy
RafGitTools  = navigation, policy, approval and receipts
Termux       = future bounded executor; not enabled by this foundation
Google Drive = editorial memory and large-source surface; not mounted here
```

A cache hit never becomes canonical merely because it is local. A database row never overrides the remote Git SHA.

## 3. Prompt 1 delivered surface

```text
app/src/main/kotlin/com/rafgittools/rafgitfs/model/RafGitFsEnums.kt
app/src/main/kotlin/com/rafgittools/rafgitfs/model/RafGitFsModels.kt
app/src/main/kotlin/com/rafgittools/rafgitfs/policy/RafGitFsPolicyEngine.kt
contracts/rafgitfs-storage-profile-v1.schema.json
contracts/rafgitfs-operation-receipt-v1.schema.json
configs/rafgitfs/default-readonly-profile.json
examples/rafgitfs/operation-receipt.readonly.json
scripts/validate_rafgitfs_foundation.py
tests/test_validate_rafgitfs_foundation.py
docs/RAFGITFS_ARCHITECTURE_V1.md
```

No Room migration, Retrofit endpoint, Compose screen, worker, remote write or Android permission is added in Prompt 1.

## 4. Core invariants

1. `provider=GITHUB` in V1.
2. `claim_allowed=false` in profiles and receipts.
3. `receipt_required=true` for every future operation.
4. `READ_ONLY` implies `write_policy=BLOCKED`.
5. `main`, `master` or equivalent release refs remain protected.
6. A remote mutation requires a later dry-run and explicit human approval.
7. Direct protected-branch writes remain blocked.
8. Delete is unavailable in V1.
9. Missing SHA, authorization or execution evidence is `TOKEN_VAZIO`, never zero or PASS.
10. Local cache and Room records are reconstructible projections, not remote authority.

## 5. State model

### Cache

```text
REMOTE_ONLY
→ METADATA_CACHED
→ CONTENT_CACHED
→ PINNED_OFFLINE
```

Exceptional states:

```text
STALE | CORRUPTED | TOKEN_VAZIO
```

### Future governed operation

```text
REQUEST
→ SCAN
→ DIFF
→ PLAN
→ DRY_RUN
→ APPROVE
→ EXECUTE
→ RECEIPT
```

Prompt 1 defines these phases but does not connect an executor.

## 6. Default profile

The canonical fixture is deliberately conservative:

```yaml
access_mode: READ_ONLY
cache_policy: ON_DEMAND
write_policy: BLOCKED
receipt_required: true
claim_allowed: false
```

This profile is a convention fixture, not evidence of a live authenticated GitHub session.

## 7. Policy behavior

The pure Kotlin policy engine may allow only read-oriented operations in V1:

- list repositories;
- list refs;
- list tree entries;
- read content;
- cache content;
- pin selected content offline.

Workspace mutation, branch creation, commit, push, pull request and delete remain denied by the foundation. Their vocabulary exists so later prompts can implement adapters without inventing a second state machine.

## 8. Validation

Dependency-free gate:

```bash
python3 scripts/validate_rafgitfs_foundation.py
python3 -m unittest tests/test_validate_rafgitfs_foundation.py -v
```

The adversarial suite covers:

- claim promotion;
- write enablement on a read-only profile;
- missing receipts;
- empty protected-branch policy;
- successful receipt without result hash;
- malformed request hash;
- false write-execution fixture;
- incomplete `TOKEN_VAZIO`;
- missing contract file.

Until execution occurs on the exact branch checkout, test results remain `TOKEN_VAZIO_EXECUTION`, not PASS.

## 9. Next implementation waves

```text
Prompt 2 → Room entities, DAOs, indexes and migration v5→v6
Prompt 3 → GitHub tree/indexing adapter and rate-limit handling
Prompt 4 → Compose browser and read-only V1 UI
Prompt 5 → selective cache and offline mode
Prompt 6 → persistent jobs, diff, conflict and receipts
Prompt 7 → governed workspace, branch, commit and pull request
Prompt 8 → security closure, CI, performance, accessibility and device evidence
```

## 10. R3

```yaml
F_ok:
  - foundation vocabulary is shared by UI, database and adapters
  - safe default profile is explicit
  - policy evaluation is side-effect-free and fail-closed
  - profile and receipt contracts are machine-readable
F_gap:
  - exact-checkout test execution
  - Room persistence
  - live GitHub adapter
  - Compose UI
  - Android device receipt
F_next:
  - execute the foundation gate on the PR head
  - only then begin Room migration v5 to v6 in Prompt 2
claim_allowed: false
```
