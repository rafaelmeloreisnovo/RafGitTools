# SAF Directory Intake Workbench V1

Status: implementation candidate; claimAllowed=false

## User flow

The Drive tab exposes a separate folder-intake card:

1. Select one folder through Android's Storage Access Framework (SAF). Google Drive is available when its provider is installed and signed in on the phone; RafGitTools does not collect a Google password.
2. RafGitTools reads the folder tree's metadata and shows counts, known sizes, a few relative paths and an inventory fingerprint.
3. The user confirms the second action. The app re-reads the inventory and stops if its fingerprint differs from the reviewed preview.
4. Every descendant file is streamed into app-private storage. The original source is read-only and remains in place.
5. RafGitTools verifies bytes and SHA-256 from the completed local file, then writes a versioned manifest and receipt.

## Private snapshot layout

Snapshot root: filesDir/saf-directory-intake/snapshot-<time>-<operation-id>/

    payload/                 copied source tree, preserving relative paths
    index/manifest.v1.json   private local content index
    index/receipt.v1.json    private local operation receipt

The manifest has a stable serialization order and records relative paths, MIME types, measured byte counts, content SHA-256 values, hashed document references, and the hashed tree reference. It does not store raw content:// URIs. File names and hashes are private metadata; the Android application manifest disables application backup.

## Safety and limits

- Read permission only; no SAF source write request.
- The feature does not delete, rename, or move source items.
- Every discovered file is included; no extension filter or silent omission.
- Default ceiling: 50,000 descendant documents and 512 MiB copied data per snapshot.
- Names with traversal, separators, control characters, ambiguous path collisions, or unsafe lengths fail closed.
- Preview and copy re-enumerate the tree; a changed metadata inventory must be reviewed again.
- Copy is streamed. Per-file source bytes, local staging readback and final-file readback are verified.
- On an incomplete operation, cleanup is limited to that newly created partial app-private snapshot.
- Output classification is PRIVATE_LOCAL_ONLY; no data is sent to Drive, GitHub, CI, or a public index.
- SAF does not expose whether a selected source folder is shared. That state stays TOKEN_VAZIO; it does not change the local-only publication block.
- Repository, branch, destination path, and provider visibility remain unbound. The app must not promote this index directly.

## Evidence boundary

Unit tests cover safe path composition, traversal/control-character rejection, and stable metadata fingerprints independent of provider enumeration order.

Android build, SAF-provider integration on device, Drive-account selection, large-file behavior, and end-to-end provider publication require exact-head CI or physical-device evidence. Source presence and hashes alone do not prove those runtime claims.
