# RafGitTools Connectivity Self-Test V1

Date: 2026-09-26

## Purpose

Make source connectivity observable inside the Android app instead of inferring
success from navigation alone.

The self-test separates four boundaries:

| Boundary | Action | PASS means | Does not mean |
|---|---|---|---|
| GitHub READ | uncached authenticated-user API probe | GitHub answered now for the active credential | GitHub WRITE permission |
| GitHub WRITE | explicit user-triggered diagnostic Issue | GitHub accepted a write to `rafaelmeloreisnovo/RafGitTools` | arbitrary repository/content write |
| Android private storage | write + read-back + SHA-256 receipt | the app private files directory is writable and readable | SAF tree or external-storage access |
| Drive/SAF | Android `OpenDocument` selection + verified staging | a provider returned a readable document and staging verified byte count/hash | Google OAuth account login inside RafGitTools |

## Baseline observation

GitHub Issue #494 exists in `rafaelmeloreisnovo/RafGitTools`, created
2026-09-26T12:00:05Z. It is valid remote-object evidence for a successful
GitHub Issue write. The GitHub object itself does not identify RafGitTools as
the originating Android client, so app-origin binding remains separate until
the in-app diagnostic receipt is exercised.

## UI changes

- Home / GitHub now exposes **Testar READ** and **Receipt WRITE**.
- READ deliberately bypasses repository cache.
- WRITE runs only after an explicit user tap and sends bounded diagnostic
  metadata only. Tokens, local paths, Drive content and corpus payloads are not
  uploaded.
- Home / Local can create a small receipt under the Android app-private
  `files/connectivity-receipts/` directory and verifies it by read-back and
  SHA-256.
- Home / Drive now states explicitly that the control opens Android's Storage
  Access Framework document picker. A Google Drive account is available only
  when the Android provider exposes it; this is not an embedded Google login.
- Settings / repository location now persists the selected SAF tree URI and
  displays it instead of silently discarding the selection. Git repository
  indexing remains a separate boundary.

## Evidence states

At source level in this branch:

- implementation: `IMPLEMENTED_UNTESTED`
- unit tests: added, not yet observed in CI
- physical Android execution: `TOKEN_VAZIO`
- Drive provider/account availability on device: `TOKEN_VAZIO`
- remote diagnostic Issue created by the new button: `TOKEN_VAZIO`
- claim_allowed: `false`

Promotion rules:

`SOURCE_PRESENT != BUILD_PASS != TEST_PASS != DEVICE_PASS != REMOTE_WRITE_PASS`.

## Privacy / governance

The remote diagnostic body contains only:

- RafGitTools receipt schema identifier;
- authenticated GitHub login returned by GitHub;
- count of repositories already loaded in the UI;
- epoch timestamp;
- explicit markers that no local/Drive payload was uploaded;
- `claim_allowed=false`.

No token, authorization header, password, cookie, local filesystem path, Drive
file content or corpus content is included.

## Rollback

The feature is isolated on branch
`feature/connectivity-self-test-20260926`. Close the PR or revert its commits.
No migration of user corpus data is required.
