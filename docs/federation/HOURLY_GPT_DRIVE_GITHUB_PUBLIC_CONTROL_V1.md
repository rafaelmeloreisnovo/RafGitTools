# RAFAELIA Ω — GPT / Drive / GitHub public–private hourly federation V1

Date 2026-10-27 | status: IMPLEMENTED_BRANCH_ONLY / HOLD / claim_allowed=false.

## Clarified goal
The public RafGitTools CI **can orchestrate coordination with GPT and private repositories**. It is not necessary to run GitHub Actions in private repositories for GPT (connected app) to read/write approved scoped private resources. However the public CI runner is a separate trust domain; it must never directly receive private source, raw data or PAT values. No assumption is made that a GPT scheduled task can directly trigger a GitHub workflow: these are independent clocks reconciled by receipts.

## Hourly order and roles
1. GPT task, existing hourly schedule at minute :28 UTC (best-effort): Drive official START HERE V2.2 → CURRENT_STATE → private queue 04_JOB_QUEUE_V1. Select one authorized item, exact source/ref, purpose, expiry, class C0–C4 and evidence rule.
2. Mapa private governs routing, approvals and gap state; producer private repository owns implementation; Google Drive retains confidential source, ledger and receipt. Use authenticated tools directly. No permission is inferred from a connected app alone; each write requires an existing authorized target and explicit scope.
3. RafGitTools public Actions candidate schedule **43 * * * *** (UTC) independently runs only `public_hourly`. This stage tests the public contract, 8 disabled privileged jobs, and emits a four-field public receipt: schema, state, public_source_sha, claim_allowed=false. It does not fetch GitHub private repositories or Google Drive.
4. Next GPT observation can correlate the last *public* run status with the *private* queued work using internal IDs only in the private ledger, not by writing identifying payload into the public Actions artifact.
5. Private execution paths: (a) GPT connected authorized GitHub/Drive tools for bounded approved actions; (b) future owner-controlled Termux worker. A PAT_AGENTS or App token, if ever required, stays in a private credential broker, scoped per repository and operation, never in the current public heartbeat.

## Ordering and idempotency
Private identifier: hour bucket UTC + queue object ID + owner + exact SHA + consent record. Public identifiers: public GitHub run ID and public source SHA only. Link the private/public receipts inside the private NEXUS index. Schedule triggers may delay or skip; **absence of a public run is TOKEN_VAZIO, not PASS**. Never infer a causal GPT → Actions trigger from timestamps alone.

## Controls
- GitHub Actions schedule only fires from the default branch; this unmerged branch does not automatically activate the cron.
- Daily START full `35 3 * * *` remains unchanged in this candidate; hourly `43 * * * *` runs lightweight C0 control only. No hourly Android/Gradle/full test lane.
- Public Actions: read-only `contents:read`, checkout without persisted credentials, no `PAT_AGENTS`, `PAT_ACTIONS`, Drive credentials or dynamic private repository URL.
- Private source never transferred, decrypted or cached by the public job. Logs/annotations/artifacts include no private repo path, titles, secrets, personal data or corpus excerpts. Do not rely on GitHub masking as sole security boundary.
- Unknown permissions, source authority, output class, license or protection: BLOCKED / TOKEN_VAZIO.
- Governance per LGPD/cultural dignity/children; provenance and independent review before promotion. No auto-merge or private Actions.

## Evidence and pending
Candidate head must be verified on exact ref. Python tests and full hosted GitHub Actions execution = TOKEN_VAZIO_NOT_RUN until verified. Authorizing branch/main merge requires human independent review and provider-side blocking merge/required-check evidence; historic main P0 remains unresolved. The public control CI does not establish private execution, end-to-end encryption or scientific claims.

Rollback: leave branch HOLD; if later approved, revert the hourly routing independently without changing historical ledger or enabling old privileged jobs.
