# Human-impact fail-closed gate — human review checkpoint (draft)

Status: **PROPOSED_PATCH / CI_NOT_YET_CONFIRMED / DEPLOYMENT_DENIED**  
Parent: public `main` at `beb0f29c7d14b25c05a32fd3ad0ed8059138cfbf` (read-only rollback anchor).  
Purpose: protect vulnerable people, especially minors, when a classification is missing or contradicted. No corpus, profile, identifier, key, personal data, or training set is included.

## ★ Observed facts / structural defects

1. Existing gate converts absent `data.personalData`, `data.sensitiveData` and `population.*` into `False`. This permits an unknown classification to avoid the corresponding human-review safeguards. Candidate fix requires explicit booleans.
2. Existing `safety.riskTier` permits a missing/unknown value to skip the high-risk conditions. Candidate fix rejects any tier not in `LOW|MEDIUM|HIGH|CRITICAL`.
3. Existing `_missing_ref` first checks set membership before verifying type; list/dict input can raise a TypeError rather than produce a stable blocked result. Candidate fix verifies type first.
4. Valid low-risk fixtures may reach **READY_FOR_AUTHORIZED_REVIEW**, never `deploymentAuthorized=true`. This state must not be interpreted as an authorization.

## † Typed gaps, urgency, falsifiers, owner and next steps

| ID | P | Type | Risk | Gate | Falsifier | Next and accountable review |
|---|---|---|---|---|---|---|
| HI-P0-001 | P0 | TOKEN_VAZIO_UNKNOWN | CRITICAL | explicit population booleans | delete childrenPossible | CI negative controls, human safeguarding review |
| HI-P0-002 | P0 | TOKEN_VAZIO_UNIDENTIFIABLE | HIGH | lawful personal/sensitive-data classification | replace personalData with unknown | CI negative controls, privacy/legal review |
| HI-P0-003 | P0 | TOKEN_VAZIO_UNMEASURED | HIGH | riskTier explicit | omit riskTier | CI negative controls, independent safety assessment |
| HI-P0-004 | P0 | TOKEN_VAZIO_CONTRADICTORY | HIGH | invalid type denies | inject dict/list reference | CI negative controls; error-to-block monitoring |
| HI-P0-005 | P0 | TOKEN_VAZIO_UNMEASURED | HIGH | pipeline supply chain/secret isolation | CI exposes secret or write token | provider security audit required before promotion |
| HI-P1-006 | P1 | TOKEN_VAZIO_UNKNOWN | HIGH | exact-head CI/receipt | workflow missing, stale or failed | verify PR head, run ID, job steps, log and result |
| HI-P1-007 | P1 | TOKEN_VAZIO_UNKNOWN | MEDIUM | rollback predecessor preserved | main diverged; revert conflict | compare base and head, plan revert with human approval |
| HI-P2-008 | P2 | TOKEN_VAZIO_OUT_OF_DOMAIN | MEDIUM | external system integration separate | tests only on fixtures | no cross-system, private data or scientific claims |
| HI-P2-009 | P2 | TOKEN_VAZIO_WITHHELD | HIGH | human authorization required | CI green but no explicit approval | hold PR draft until human approval |
| HI-P2-010 | P2 | TOKEN_VAZIO_IRRECOVERABLE | HIGH | downstream release reversibility | unauthorized propagation | forbid release; do not claim guaranteed rollback |

Unknown or pending findings are not zero, negative evidence, or safe. `TOKEN_VAZIO_WITHHELD` means intentionally undisclosed; do not attempt to retrieve secrets.

## ‡ Candidate correction, watchdog and rollback

The patch modifies only `scripts/human_impact_gate.py`, adds synthetic negative tests, and creates a scoped **read-only pull_request** Actions gate. No write permission, secret reference, OIDC, external network requests, artifact upload, deployment, release or dispatch is introduced by the new workflow.

Watchdog envelope: `timeout-minutes: 6`, cancellation on superseding PR runs, default-deny on absent/invalid inputs, nonzero exit on failed tests. A timeout or connector error remains **BLOCKED**, never PASS. Existing main START.yml may run independently and must be evaluated separately; this patch does not change it.

**Pre-approval rollback:** keep `main` unchanged; close the draft PR if a defect, failure or authorization conflict appears. **Post-approval (future only):** independent review of exact head + workflow run + status, then, if approval explicitly granted, merge via authorized procedure; any later rollback must revert only the authorized commits with a new receipt, not rewrite Git history. Neither a draft PR nor a green CI is human approval.

## Human-review checklist (decision AFTER Actions evidence)

- [ ] Confirm user and privacy/legal authority, purpose, destination, and data classification.
- [ ] Exact branch/head SHA, diff, provenance and LICENSE reviewed; all changed files intended.
- [ ] New negative gate CI at **exact head** SUCCESS; logs/jobs prove tests executed, no skipped tests.
- [ ] Existing START workflow behavior examined; no unintended privileged job/deployment.
- [ ] Children, vulnerable groups, Indigenous peoples and cultural boundaries evaluated appropriately; UNKNOWN remains blocked.
- [ ] Rollback feasibility and affected consumers documented; no secret/corpus/artifact exposed.
- [ ] Independent human decision recorded as a distinct authorization; only then consider code promotion.
- [ ] If any check unknown: STOP, record typed TOKEN_VAZIO, retain prior checkpoint.

## Evidence boundary

`CI_PASS != HUMAN_APPROVAL != DEPLOYMENT`. This patch is a candidate, not a certification under NIST, ISO/IEC, IEEE, LGPD or child-protection law. Observation feedback may refine future gates append-only; **no ML model training or personal behavioral profiling** occurs.

F_ok: candidate source + synthetic falsifiers prepared.  
F_gap: actual workflow run, exact-head result, independent review, real permissions and deployment boundary not yet validated.  
F_next: create draft PR, observe Actions run/jobs for exact head, present test failures/success to human reviewer; **do not merge without subsequent approval**.
