# RafGitTools — Development & Delivery Map V1

Observed main: 2e69dae6d45dd23c6252eee9b42cd230d1bd6bac
Candidate: audit/drive-github-responsive-delivery-20260918
Date: 2026-09-18
State: SOURCE_ADVANCED / DELIVERY_GATED
claim_allowed=false
release_allowed=false

This map supersedes feature-count progress as the delivery control surface. Progress is gate/evidence based.

| Gate | Current candidate state | What exists | What still closes it |
|---|---|---|---|
| D0 Documentation reconciliation | IMPLEMENTED_UNTESTED | current map, architecture, release-note route and validator | exact-head START run |
| D1 Responsive dashboard | IMPLEMENTED_UNTESTED | breakpoint classifier, reusable frame, HomeScreen consumption, JVM tests | CI + physical size matrix |
| D2 Drive/SAF copy gate | IMPLEMENTED_UNTESTED | stream hash, atomic staging, readback hash, local receipt | CI + physical provider import |
| D3 GitHub recipient | TOKEN_VAZIO_EXPLICIT_TARGET_REQUIRED | target fields preserved as unknown | explicit repo/ref/path selected by user |
| D4 RafGitFS promotion | SOURCE_OBSERVED_EVIDENCE_GATED | workspace/branch/commit/push/draft-PR/rollback source | connect staged file to exact workspace + execute |
| D5 Exact-head CI | TOKEN_VAZIO_PENDING_PROVIDER | single START pipeline | terminal run/job evidence for candidate head |
| D6 Physical Android | TOKEN_VAZIO_PHYSICAL_DEVICE_REQUIRED | build path exists | exact APK install + compact/medium/expanded + Drive import smoke |
| D7 Signed release | BLOCKED_BY_EVIDENCE | signed release job and generated changelog route | D5 + D6 + signing provenance + explicit decision |

## Delivery route

SOURCE
→ TEST
→ BUILD
→ PROVIDER/RUNTIME
→ PHYSICAL DEVICE
→ SIGNED ARTIFACT
→ RELEASE DECISION
→ RELEASE NOTES + RECEIPT

No later state inherits PASS from an earlier state.

## Layout route

Compact: width < 600dp, horizontal token 16dp, full content width.
Medium: 600–839dp, horizontal token 24dp, max content width 720dp.
Expanded: width >= 840dp, horizontal token 32dp, max content width 1200dp.

The candidate applies this contract to the primary source dashboard. Other screens remain individually reviewable; this delta does not claim universal adaptive coverage.

## GitHub ↔ Drive route

Drive/SAF source
→ verified local staging
→ recipient TOKEN_VAZIO until explicit binding
→ RafGitFS governed workspace
→ branch
→ commit
→ draft PR
→ GitHub provider readback
→ compact Drive μWRITE pointer

Drive and GitHub are coordinated, not mirrored indiscriminately.

## Release-note route

START.yml still generates CHANGELOG.md from Git history when the signed release job runs.
docs/RELEASE_NOTES_NEXT.md is the human-readable candidate note with evidence/gap boundaries.
A release note is not a release receipt.

## Stop conditions

Stop mutation when authority is missing, recipient is unresolved, provider pre-state is unknown, protected-branch safety fails, a receipt cannot be emitted, or an exact-head gate is not observed.

## R3

F_ok: code→architecture→gate→release route is reconstructible.
F_gap: recipient binding, exact-head provider evidence, physical-device acceptance and signed release remain open.
F_next: let START evaluate the candidate head; close only observed failures, then run one explicit Drive→RafGitFS→draft-PR integration fixture.
