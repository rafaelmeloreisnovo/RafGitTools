# START topology: explicit ninth workflow successor

## SOURCE / provenance
- Producer: `rafaelmeloreisnovo/RafGitTools` main `b8bdba9a5e3a75c9aa5c621d670913524364f770`.
- Falsifier: START [run 42587176657](https://github.com/rafaelmeloreisnovo/RafGitTools/actions/runs/42587176657), job 129070868705, observed exact 9 active `.yml` versus expected 8.
- The additional filename is `.github/workflows/drive-snapshot-solo-apk.yml`, blob `6e1d968f809fedd622ba6839caa188b3c1e7f57a`, preexisting in main from merged PR #804 (standalone independent Drive Snapshot APK).
- START file before intervention blob `9f9f4817d6be162111d82532a08957bc2badb6d3`; branch based on main unchanged.

## Smallest correction
- Exact single allowlist entry for the already-present standalone workflow.
- Change structural PASS label to `PASS_9_EXACT_SCOPED_WORKFLOWS`.
- No wildcard, no security/permissive fallbacks, no additional workflows created, no Gradle/Android/JNI code changes, no source runtime changes.
- New active workflows remain forbidden by the exact count/path equality gate.

## Adversarial control / testing
- Pre-write static exact-list check verified all nine filenames match the actual main directory.
- Synthetic tenth `.github/workflows/unknown.yml` fails that same equality predicate; removing one of the nine also fails.
- CI on exact PR head = NOT_RUN until GitHub job finalization. PIRC2 PR #806 is a dependent consumer, not evidence this proposal passes.
- Provider-controlled Actions YAML cannot itself be freestanding. `native/pirc2_token_l0` C11 objects are separately tested.

## STOP / rollback
- DRAFT/HOLD only; do not merge automatically.
- Close this PR to return to existing main; if merged after independent review, revert only the two exact START lines.
- Verify START stage '01 · Workflow topology' terminal success on exact HEAD and all downstream jobs; if failure persists, preserve evidence and make a scoped successor, not a broader allowlist.
- Do not infer any Android APK success, OAuth approval or physical device validation.

## State
N0 = exact-main file count/contents, PR #804 merged history, run 42587176657 logs, local static list equality.
N2 = allowlist lag caused the observed count mismatch.
N5 = future CI result / physical runtime / license scope of unrelated files.
claim_allowed=false for end-to-end application readiness.
SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM.
