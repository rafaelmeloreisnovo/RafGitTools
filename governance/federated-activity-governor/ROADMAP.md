# Roadmap — Federated Activity Governor V1

Status: proposal; no autonomous cross-provider mutation.

## P0 — Close authority and inventory gaps

- Bind Mapa route and producer-local authority.
- Inventory GitHub workflows, schedules, Pages sources and branch policies by exact SHA.
- Inventory Supabase schemas and existing append-only event tables without reading secret values.
- Classify Drive, Notion and Slack destinations by privacy and retention.
- Prove PAT capabilities by provider readback; presence/name remains insufficient.
- Establish a single-writer or compare-and-swap contract for current pointers.

Exit: every edge has owner, source, destination, permitted operation, evidence rule, idempotency key and rollback.

## P1 — Read-only governor

- Normalize GitHub Action/run/PR/Page observations into the portable event envelope.
- Query Supabase only through bounded read-only views/RPC or least-privilege role.
- Generate cost/duration/failure metrics without publishing private payloads.
- Produce recommendations and draft receipts; no automatic promotion.

Exit: replaying the same event is idempotent; unknowns remain typed; synthetic adversarial tests pass.

## P2 — Bounded write adapters

- GitHub: draft PR/title/label/receipt only, exact repository and SHA allowlist.
- Supabase: append-only event metadata with readback.
- Notion: minimal decision pointer, no source-body duplication.
- Slack: public-safe notification with links only.
- Drive: append-only reconstruction receipt using revision/CAS guard.

Exit: each adapter has negative authorization tests, provider readback and compensating rollback.

## P3 — Promotion pyramid

- Base: contribution and discovery.
- Observation: identity, license, dependency and data classification.
- Validation: build, tests, CodeQL/static analysis and project-specific falsifiers.
- Integration: producer-consumer evidence.
- Candidate: independent review and exact-SHA freeze.
- Publication: human/community authorization, tag/release and receipt.

Exit: a deliberate unauthorized promotion is rejected and evidenced.

## P4 — Reusable CheckValidation

- Versioned reusable workflows and composite actions.
- Capability-driven profiles for Android/Gradle/R8/JDK/NDK/JNI, Rust/C/ASM, JSON/schema, RLL and Pages.
- Pinned actions and dependency manifests.
- Cost budgets, timeouts, concurrency and cache provenance.
- No universal green: PASS remains scoped to the selected profile.

Exit: RafGitTools and RLL consume the same versioned core while retaining local gates.

## P5 — Observability and cost governance

- Per composition: runs, minutes, artifacts, storage, failure class and retry amplification.
- Free-plan budget guardrails are optimization constraints, never permission to delete evidence.
- Detect loops, duplicate schedules, stale branches and repeated expensive checks.
- Human-readable dashboard and machine-readable receipts.

Exit: cost reduction is demonstrated without reducing required evidence.

## Deferred / TOKEN_VAZIO

- Exact PAT scopes and provider bindings.
- Active GitHub Pages URL for RafGitTools.
- RLL Pages deployment success at the current HEAD.
- Cross-provider single-writer enforcement.
- Slack retention/export policy.
- Independent reviewer/community authorization model.
- Physical/runtime evidence for producer-specific claims.

## Rollback

Every mutation uses a successor PR/event; no force-push, receipt deletion or history rewrite. Before P2, rollback is closing the draft and retaining the audit receipt.
