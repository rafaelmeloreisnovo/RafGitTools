# RafGitTools — Responsive Layout Gate V1

Date: 2026-09-18
Observed base: 2e69dae6d45dd23c6252eee9b42cd230d1bd6bac
Candidate: audit/drive-github-responsive-delivery-20260918
State: IMPLEMENTED_UNTESTED

## Problem closed at source level

ResponsiveUtils.kt existed and MainActivity imported responsive helpers, but no application call consumed them. Documentation that implied universal adaptive layout therefore exceeded the observed integration.

The candidate makes the contract executable and testable:

- pure width classifier: windowSizeForWidth
- compact/medium/expanded padding functions
- compact/medium/expanded max-content-width functions
- reusable ResponsiveContentFrame
- HomeScreen consumes ResponsiveContentFrame
- unit tests exercise breakpoint boundaries and layout tokens

## Breakpoints

COMPACT: width < 600dp
MEDIUM: 600dp <= width < 840dp
EXPANDED: width >= 840dp

## Layout tokens

COMPACT: 16dp horizontal padding, no max-width cap.
MEDIUM: 24dp horizontal padding, 720dp max content.
EXPANDED: 32dp horizontal padding, 1200dp max content.

## Evidence gate

Source presence: OBSERVED in candidate.
JVM test execution: TOKEN_VAZIO until exact-head CI.
Android build: TOKEN_VAZIO until exact-head CI.
Physical compact/medium/expanded screenshots or semantics: TOKEN_VAZIO_PHYSICAL_DEVICE_REQUIRED.

The gate is not allowed to label all screens adaptive from one dashboard integration.

## Next expansion order

1. Home/source dashboard.
2. repository/file browser and RafGitFS browser.
3. diff/commit detail and terminal high-density surfaces.
4. settings/auth/forms.
5. navigation rail/drawer only if a tested large-window need is demonstrated.

## R3

F_ok: responsive helpers are no longer an unused island.
F_gap: screen-by-screen and device matrix.
F_next: execute exact-head tests/build and inspect wide/narrow physical layouts before broadening the claim.
