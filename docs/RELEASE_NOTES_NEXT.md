# RafGitTools — Release Notes NEXT

State: NOT_RELEASED / CANDIDATE
Date: 2026-09-18
Observed base: 2e69dae6d45dd23c6252eee9b42cd230d1bd6bac
Candidate branch: audit/drive-github-responsive-delivery-20260918
release_allowed=false
claim_allowed=false

## Added

- Verified Drive/SAF staging gate with readback SHA-256 and byte-count validation.
- Appendable local staging receipt that deliberately leaves GitHub recipient fields TOKEN_VAZIO until explicit binding.
- Reusable responsive content frame and pure breakpoint functions.
- Development/delivery map and machine-readable delivery-gate manifest.
- Drive↔GitHub delivery architecture that reuses RafGitFS rather than creating a second Git writer.

## Changed

- The primary Home/source dashboard now consumes the responsive frame instead of merely importing unused responsive helpers.
- The Drive staging UI now shows STAGED_VERIFIED state and the local receipt path.
- Documentation gates can validate the new delivery map.

## Evidence available at source level

- Responsive JVM tests are present.
- Drive staging-gate JVM tests are present.
- Python delivery-map validator is present.
- START remains the single active CI orchestration surface.

These are source statements until the exact candidate workflow terminates successfully.

## Known gaps

- GitHub repository/ref/path recipient is intentionally unresolved for a generic Drive import.
- Drive staging has not yet been wired into a concrete RafGitFS workspace action.
- Compact/medium/expanded physical-device evidence is not yet present.
- Same-artifact Drive import on a physical device is not yet present.
- Signed release provenance and final distribution decision remain open.
- Reverse GitHub→Drive export remains TOKEN_VAZIO.

## Release gate

Do not publish merely because CHANGELOG.md can be generated. Release requires exact candidate SHA, required CI/build/security results, signed artifact and digest, physical smoke, and an explicit release decision.

When START performs a signed release, its generated CHANGELOG.md remains the machine-generated Git-history input. This NEXT document is the human evidence-aware summary and should be finalized against the exact release tag.

## R3

F_ok: release notes now distinguish source changes from proof.
F_gap: provider CI, device and signed artifact.
F_next: finalize only after exact-head receipts exist.
