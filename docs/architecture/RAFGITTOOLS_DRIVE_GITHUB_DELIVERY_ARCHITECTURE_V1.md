# RafGitTools — Drive ↔ GitHub Delivery Architecture V1

Status: CANDIDATE / FAIL-CLOSED / claim_allowed=false
Observed implementation authority: github:rafaelmeloreisnovo/RafGitTools
Observed main base: 2e69dae6d45dd23c6252eee9b42cd230d1bd6bac
Candidate branch: audit/drive-github-responsive-delivery-20260918
Date: 2026-09-18

## 1. Authority split

Google Drive is documentary/source-memory custody and receipt/index territory.
GitHub RafGitTools is code, schema, test, CI and application implementation authority.
The Android private files directory is staging/workbench only.

No local copy becomes canonical merely because it exists. No Drive item becomes a Git commit without an explicit recipient and a governed Git plan.

## 2. Existing surfaces reused

The app already has two real halves:

1. HomeScreen Drive/SAF import: Android document provider → streaming local staging.
2. RafGitFS governed writer: workspace → branch → commit → push → draft pull request, with protected-branch blocking and rollback support.

The architectural gap is the typed handoff between them. A second Git mutation engine is not required.

## 3. Copy gate now defined

Flow:

Drive or SAF document
→ stream to private .part file
→ compute source-stream SHA-256
→ atomic rename into private staging
→ read the staged file again
→ verify byte count and SHA-256
→ emit local staging receipt
→ only then expose STAGED_VERIFIED

If byte count, readback SHA-256, receipt creation or receipt promotion fails, the staged artifact is not accepted by the gate.

The local receipt deliberately stores provider authority and display name, not the raw content URI.

## 4. Recipient gate

STAGED_VERIFIED is not equivalent to synced.

At this state the receipt must carry:

recipientProvider = GITHUB
recipientRepository = TOKEN_VAZIO_EXPLICIT_TARGET_REQUIRED
recipientRef = TOKEN_VAZIO_EXPLICIT_TARGET_REQUIRED
recipientPath = TOKEN_VAZIO_EXPLICIT_TARGET_REQUIRED

Promotion can proceed only after the human binds repository, ref and path and the RafGitFS plan passes its authority, conflict, protected-branch and approval gates.

## 5. Governed promotion route

STAGED_VERIFIED
→ RECIPIENT_BOUND
→ create RafGitFS workspace
→ stage selected file into workspace
→ diff/plan
→ explicit approval for the exact plan hash
→ create non-protected branch
→ create commit
→ push branch
→ open draft PR
→ provider readback
→ receipt

Direct writes to main/master/release-class refs remain blocked.

## 6. Drive reconstruction receipt

After a provider-observed Git result exists, Drive receives only a compact reconstruction pointer:

source/ref
parent
kind
delta summary
routes
evidence
gap
next
hash/ref

The Drive receipt points to Git commit/PR/run identifiers; it does not duplicate the repository tree or binary artifacts.

## 7. Reverse direction

GitHub → Drive content export is not implemented by this delta and remains TOKEN_VAZIO.
There is no blind two-way sync. A reverse bridge must have its own recipient, conflict, overwrite, provenance and rollback policy.

## 8. Gates

G0 SOURCE_SELECTED: provider returned a readable document handle.
G1 STAGED_VERIFIED: byte count + readback SHA-256 + local receipt.
G2 RECIPIENT_BOUND: explicit GitHub repository/ref/path.
G3 PROMOTION_PLANNED: RafGitFS exact plan and base SHA.
G4 APPROVED: exact plan hash human approval.
G5 GIT_PROMOTED: branch/commit/push/draft PR observed.
G6 PROVIDER_READBACK: GitHub observed SHA/PR state.
G7 DRIVE_RECEIPT: compact cross-system reconstruction pointer appended.

Current candidate closes code for G1. G2 is intentionally TOKEN_VAZIO until a concrete destination is chosen. G3–G6 reuse existing RafGitFS source but are not claimed for a Drive import until the handoff is implemented and executed.

## 9. Evidence boundary

SOURCE_OBSERVED != TEST_PROVEN != BUILD_PROVEN != RUNTIME_PROVEN != DEVICE_PROVEN != RELEASE_PROVEN

A successful staging receipt proves only the bounded local copy event. It does not prove GitHub promotion, physical-device acceptance or release readiness.

## R3

F_ok: one copy gate and one Git mutation engine; readback hash and typed recipient gap; rollback-capable downstream route.
F_gap: explicit import-to-RafGitFS workspace action, device execution, reverse GitHub→Drive export.
F_next: execute CI on the exact candidate head; then bind one disposable GitHub recipient and test the full staged-file→draft-PR route.
