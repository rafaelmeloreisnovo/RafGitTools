# RafGitTools Android — prova local e catálogo público V1

State: `IMPLEMENTED_SOURCE / DEVICE_E2E_NOT_RUN / claim_allowed=false`.
Owner: `rafaelmeloreisnovo/RafGitTools` (app implementation).
Public catalog: https://github.com/rafaelmeloreisnovo/catalogos
Public falsifier/format: https://github.com/rafaelmeloreisnovo/conhecimentos

## Problem observed by the user

After PAT authentication, profile avatar and repositories become visible only after
leaving/reopening RafGitTools. This is a **user report**, not a reproduced defect
on this exact candidate build. Previously HomeViewModel read DataStore only in init;
the candidate now observes authentication and username state changes.

## Device procedure — no restart should be required

1. Install an APK with exact artifact/run SHA from the reviewed candidate; record
   the APK checksum **privately**, not in a public repository.
2. Open Home and tap **Exportar prova local · sem secrets**. Choose a PRIVATE
   destination in the Android system document picker.
3. Authenticate through the existing PAT screen. Never include the PAT in
   screenshots, logs, file names, messages or public proof repositories.
4. Return to Home **without restarting the process**. Capture another receipt.
5. Observe the profile icon, GitHub list and any error state. Capture screenshots
   only into private custody if they add evidence, after verifying no personal
   account information or credential is exposed.
6. Perform an actual app process restart and capture a third receipt. Compare
   before login, immediate after login, after restart.
7. On a trusted host, validate each envelope with
   `conhecimentos/scripts/validate_proofs.py`. Compare the emitted
   `credential_present`, `auth_flag`, `profile_visible`, `repository_count`,
   `repository_listing_complete`, and `live_probe`.
8. Pair the privately retained evidence with the exact APK SHA, app version,
   branch/commit, tester-controlled observations and capture time. Maintain
   a successor receipt for corrections; do not silently replace prior records.

## Evidence boundary

`DeviceProofV1` exports an exact payload UTF-8 string and its SHA-256 into an
`application/json` document via Android SAF. The snapshot includes booleans and
repository count, not repo names, login, PAT, OAuth keys, location, serial number,
Android ID, or local paths. The app does NOT upload the file automatically.

`live_probe=PASS_SCOPED` means the app received an authenticated identity
response at capture time; it does **not** mean the avatar or repositories loaded
and does not attest the physical device or APK origin.
`device_attestation=TOKEN_VAZIO`, `source_commit=TOKEN_VAZIO` until the
separate build binding is implemented and verified.
A SHA proves tamper detection of the stored payload only, not its producer.

For actual physical verification, require independent human observation or
trusted device attestation + exact artifact binding. CI on public repositories
only validates JSON shape, privacy red flags and negative tests. It does not
run on the user's handset.

## Falsifiers

| Case | Input | Expected |
| --- | --- | --- |
| Login refresh | PAT success without app restart | Home receives DataStore update; profile/repository update is observable |
| Missing token | Auth flag true but encrypted token unavailable | No remote repo claim; state fails closed |
| Repo fetch fails | Live probe may pass but list fetch fails | No invented repository completeness |
| Logout | User disconnects GitHub | Profile/list cleared, no cached identity claim |
| Proof mutation | One byte changed in payload_json | Digest verifier rejects |
| Secret injection | Token/email/local path in payload | Public verifier rejects |
| Forged attestation | `device_attestation=PASS` | Public verifier rejects |
| Android process not tested | CI passed but no phone receipt | `DEVICE_E2E_NOT_RUN` |

Rollback: close this draft PR; leave `main`, secrets, private files and
existing receipts untouched. For any successor after merge, use reviewed revert
or a new append-only correction with source and receipt.

`SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM`.
