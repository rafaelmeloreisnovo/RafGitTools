# Private CI assurance: scoped review note V1

Status: ISOLATED_BRANCH_PROPOSAL. claim_allowed=false.

This branch adds an offline, non-authorizing preflight and synthetic tests. The active bridge, START.yml, credential configuration, registry and production workloads are unchanged.

Source authority: RafGitTools owns the executor, Mapa owns federated routing, and the Drive ledger owns documentary receipts. Each claim must retain its own source, artifact, execution, evidence and authority.

Twelve typed review dimensions: actor attestation; policy authority; exact HEAD; purpose limitation; provider credential scope; network isolation; filesystem isolation; process-tree cleanup; bounded output; receipt noninterference; idempotency/concurrency; independent reproduction.

Modes to falsify later in an authorized environment: malformed inputs and logical crashes; timeout and descendant cleanup; unlimited stdout/stderr; stale evidence; replay; scope drift; and cross-provider disclosures. These are UNVERIFIED runtime risks, not evidence of an observed attack.

The synthetic preflight differentiates missing, null, false, zero, blank, whitespace and NUL. It returns BLOCKED, HOLD or REVIEW_REQUIRED, but always operation_permitted=false and claim_allowed=false. A string declaring PASS_WITH_RECEIPT does not certify that a provider executed the gate.

Forward reconstruction: intent -> authority -> source@commit -> gate -> execution -> evidence -> receipt -> claim.
Backward reconstruction: claim -> receipt -> evidence -> execution -> gate -> source. Missing links retain typed TOKEN_VAZIO.

Local checks: 11 unit tests passed. Four artificial mutations to unsafe decisions were detected by the tests. Neither GitHub Actions nor the private access patch was executed in this validation.

Rollback: abandon the isolated branch. No main modifications, no private CI dispatch, no automated publication.

F_next: independent review, GitHub readback at exact branch HEAD, dedicated process-tree/output tests, provider isolation receipts, and a separate authorization decision before integration.
