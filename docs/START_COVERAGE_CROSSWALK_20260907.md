# START coverage crosswalk — 2026-09-07

Status: `SOURCE_OBSERVED / claim_allowed=false`

Base: `main@0c78ec13295471937fbad4a014326f2e7bd74a8e`

This audit note separates the active `.github/workflows/START.yml` topology from the preserved `.github/workflows-legacy-20260907/` workflow set. It does **not** claim runtime equivalence merely because the current exact-head runs are green.

## Active coverage observed in START

- coherence / anti-regression and documentation claim boundary
- deterministic Python test discovery
- Android unit tests, lint, devDebug assembly, APK SHA-256 and artifact upload
- federation master-index / provenance validation and tests
- CodeQL for GitHub Actions and Java/Kotlin
- signed release gating with explicit signing authority checks

## Partial mappings

- Documentation: START checks the claim boundary and README presence; legacy documentation also described markdown lint, link checking, spell checking and Dokka generation.
- Scheduled/full lane: START schedules `full`; legacy nightly semantics additionally described all variants, AAB generation, repository statistics and issue creation on failure.
- PR validation: START provides core build/test paths, while the legacy PR workflow described Conventional Commit title validation, auto-labeling, APK-size analysis and automated PR result comments.

## Legacy controls not evidenced as active START jobs/steps in this source audit

- Trivy dependency vulnerability scan
- TruffleHog secret scan
- license metadata/policy gate
- JaCoCo / code-coverage reporting
- performance metrics (APK size threshold, build time, method count)
- stale issue / PR automation
- explicit unsigned production internal-validation lane

Historical receipts remain valid only for their bound commit. For example, `data/evidence/github/rafgittools-post-merge-assurance-20260907.v1.json` records Trivy, secret scanning, CodeQL and license checks as successful at `main@22f2be45c8e1c1fb39de95cbbc09aca5536ca4e8`; that evidence must not be silently promoted to the current START topology.

## Gap contract

`gap_id=TV-RAFGITTOOLS-START-CONTROL-COVERAGE-143`

- state: `OBSERVED_SOURCE_GAP + TOKEN_VAZIO_RUNTIME_EQUIVALENCE`
- missing_field: active mapping or explicit intentional-deprecation/external-provider disposition for each archived control
- evidence_needed: control crosswalk plus current-head test/receipt for every claimed replacement
- falsifier: source evidence proving a supposedly missing control is reachable from START
- closure_gate: every mandatory legacy control is `ACTIVE_EQUIVALENT`, `INTENTIONAL_DEPRECATION`, or `EXTERNAL_PROVIDER`, with evidence
- claim_allowed: `false`

`gap_id=TV-RAFGITTOOLS-SECURITY-ACTIVE-SURFACE-143`

- state: `OBSERVED_SECURITY_COVERAGE_REDUCTION + TOKEN_VAZIO_INTENTIONALITY`
- next probe: trace current source for Trivy / TruffleHog / license-policy execution reachable from START, without copying secret values into receipts
- closure_gate: each security control mapped and tested or explicitly retired with rationale
- claim_allowed: `false`

## Invariants

`TOPOLOGY_CONVERGED != CONTROL_COVERAGE_COMPLETE`

`SUCCESS_RUN != MISSING_GATE_TESTED`

`LEGACY_ARCHIVED != DEPRECATED_WITH_RATIONALE`

`HISTORICAL_RECEIPT(commit A) != CURRENT_PROOF(commit B)`

This file is an audit delta only. It does not modify the default branch, merge, approve, release, or promote any high-impact claim.
