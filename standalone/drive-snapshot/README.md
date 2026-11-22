# RAFAELIA Ω — Drive Structural Snapshot (APK solo)

**Status: SOURCE_IMPLEMENTED / APK_BUILD_NOT_YET_VERIFIED / ON_DEVICE_NOT_RUN / claim_allowed=false.**

This is a stand-alone Android app. Its package ID is `org.rafaelia.drivesnapshot`.
It is built in isolation with `gradle :app:assembleDebug`, and does not require
EstudioAudio, RafGitTools, Termux, TinyGPT or llamaRafaelia at runtime. RafGitTools
is only the GitHub producer repository of the source tree.

## Real purpose

Google Drive OAuth authorization -> read-only `files.list` metadata (50 items/page,
`files(*)`) -> one-object-at-a-time JSON processing -> gzip JSONL page spool ->
small SQLite tree cache -> Drive folder `__ORGANIZANDO/010_SNAPSHOTS/SNAP_<runId>/`
-> remote `files_00000000.jsonl.gz` shards -> journal advance after confirmed upload.
A notification keeps an Android dataSync foreground service active while scanning.

It does **not** download ZIP contents, unzip, delete, rename, reorganize, move,
share or publish source files. It creates only its own destination folders and
new metadata shards in the selected Drive account after the user taps Scan.

## RMRCTI/ONES source research, no copied private records

Source: `rafaelmeloreisnovo/llamaRafaelia`, `rmrCti/RMRCTI_JSONL_ONES_V3_CONTRACT.md`,
`DATASET_MOBILE_TERMUX_V1.md`, `omega_index.jsonl`, `omega_threads.jsonl`,
`omega_events.jsonl`, `forest.jsonl`, `omega_value_map.jsonl`.
The raw RMRCTI indices remain **outside** this public APK source and binary.
The reusable principle is `Object != Node != Evidence != Score`.
The Drive file `id` is identity, `parents` encode edges, returned fields are evidence,
and no semantic score is asserted. `ΔP_RMRCTI` is not a folder-change metric.

## Low-resource budget

- 50 Drive metadata objects per HTTP page; each JSON object is processed separately.
- At most 512 Ki **characters** for any one metadata object (fail-closed if exceeded).
- A single compressed shard/page is stored in the app's cache at a time.
- The local SQLite cache is minimal (ID, parent, name, MIME, optional size, modified, trashed).
- No LLM inference, GPU, Termux, QEMU, remote server or full file downloads.
- The APK uses Play Services OAuth rather than storing API keys/tokens on disk.
- One worker, one HTTP stream, one upload at a time; pause only at safe page boundary.
- No local full Drive JSON export; remote JSONL shards are produced as each page finishes.

## Authentication prerequisite — CRITICAL

A sideloaded Android APK **does not automatically inherit ChatGPT/Drive access**.
For authorization, an Android OAuth client associated with this application ID and
its installed signing-certificate SHA-1 must be configured in a Google Cloud project
with the Drive API enabled. Debug builds use a signing certificate that can change
on different machines/CI runs. The user must grant `drive.metadata.readonly` plus
`drive.file` (restricted scope policies/verification may apply). Without valid
OAuth app registration, the UI opens but Drive authorization will be blocked.
The token remains in memory and is not embedded in artifacts.

## Scope and correctness

- First version lists `corpora=user` (files owned/shared to the authorized user).
  It does **not** claim a complete scan of every shared drive, trash, inaccessible
  file, or every possible Drive Activity/Labels/Permissions history. To cover shared
  drives exhaustively, implement and gate `drives.list` + per-`driveId` enumeration.
- `files(*)` requests all `File` fields exposed by this API call, but many are
  conditional and absent fields remain unknown; `labelInfo` also needs label IDs.
- `LIST_PAGINATION_COMPLETE` means all page tokens in that run were consumed;
  it does **not** mean an instant atomic snapshot or proof of complete coverage.
- `incompleteSearch=true` becomes `INCOMPLETE_SEARCH`, never a PASS.
- A missing `size` is not zero. SQLite stores `size_known=0` distinctly.
- The active full source stays in Google Drive, never packaged into the APK.
- If the server returns an expired/invalid pagination token, don't erase evidence:
  mark BLOCKED and start a fresh run only via explicit user action.
- Upload retries read the remote file MD5 before creating a replacement. If the
  name exists with a mismatched digest, stop as `REMOTE_SHARD_COLLISION`.
  An extremely narrow concurrent-create race is still possible; a future stronger
  producer must add remote idempotency/transactional fencing. No claim of exactly-once.
- A service can be stopped by Android. On reopening, authorize and select Retomar.

## Build

```shell
# In standalone/drive-snapshot/ on a Linux CI host with Android SDK:
gradle --no-daemon :app:assembleDebug
# output: app/build/outputs/apk/debug/app-debug.apk
python3 tools/test_snapshot_contract.py
```

The producer GitHub Actions workflow uploads a debug APK **only if the build passes**.
GitHub Actions success is not evidence of Drive OAuth registration, device runtime,
ZIP extraction, successful scanning of real user data, or exhaustive corpus coverage.

## Required end-to-end gates

1. APK built and SHA-256 receipt checked for the exact commit.
2. Android package installation/launch verified on ARMv7 device.
3. Google OAuth client ID/package/certificate registration succeeds.
4. Actual read-only metadata page returns file IDs/parents/missing size distinctions.
5. Confirm first shard uploaded to the authorized account; check MD5 readback.
6. Kill/restart service after page N, then prove no missing/duplicate shards.
7. Expired token, 403, 429, 5xx, missing page tokens, incompleteSearch and page
   mutation result in typed states rather than silent PASS.
8. Compare local tree against controlled fixture with duplicate names, shortcuts,
   zero-byte files, missing sizes, shared drives and inaccessible folders.

Rollback: revert this **additive** PR, uninstall the independent APK. It makes no
changes to preexisting Drive source items. The app-created metadata folders are not
automatically deleted; retain them for audit unless separately approved.

R3 = <SOURCE_IMPLEMENTED, OAUTH_AND_DEVICE_E2E_TOKEN_VAZIO, BUILD_VERIFY_NEXT>.
