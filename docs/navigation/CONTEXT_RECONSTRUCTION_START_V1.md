# Context Reconstruction START V1

Status: `MERGED / VERIFIED_LIMITED / claim_allowed=false`

Revision-bound router evidence recorded in the current-state ledger: PR #617 candidate `5878176b49f19f297f6573ea528f66218c3a036d`, START #590 / run `37090622351` = `SUCCESS`, merge `f2ab825454a42f07ba55db742b04390092826475`. This proves the router/contract gate at that revision only; it does not make a moving `main` an exact-head PASS.

Purpose: give a human or AI one short, deterministic entry path into RafGitTools without copying the corpus or inventing a second source of truth.

## 30-second route

```text
INTENT
→ CURRENT_STATE
→ SOURCE_MIN (1..3)
→ AUTHORITY
→ ROUTE / SERVICE
→ ACT
→ EVIDENCE
→ SEED
→ μWRITE / R3
```

Ask, in order:

1. **What is the intent?** One bounded task.
2. **What is current?** Read `docs/RAFGITTOOLS_CURRENT_STATE.md` and bind the answer to the observed repo ref. A stale state document is a pointer, not current-head proof.
3. **What is the minimum source set?** Open only 1–3 canonical sources from `configs/context-reconstruction-routes.v1.json`.
4. **Who is authoritative?** Producer repository owns implementation; Drive/documental memory does not override code/test/CI authority.
5. **Which service owns the step?** Route through an existing service; do not create parallel middleware by default.
6. **What action is allowed?** Respect read/write, approval, provider and rollback boundaries.
7. **What proves the result?** Commit/run/artifact/receipt/readback appropriate to the claim.
8. **What must be remembered?** Emit a small `rafgittools.context_reconstruction_seed.v1` reference packet.
9. **What changed materially?** Append a receipt/μWRITE only for a real delta and close with `R3=<F_ok,F_gap,F_next>`.

## No-friction route matrix

Use one row first. Expand beyond its 1–3 roots only for `missing_source`, `contradiction`, `unresolved_authority`, `missing_evidence`, or explicit request.

| Intent | SOURCE_MIN | Authority | Execution target | Evidence boundary |
|---|---|---|---|---|
| Re-enter current RafGitTools work | `docs/RAFGITTOOLS_CURRENT_STATE.md`; `docs/INDEX.md` | exact Git/provider state first; docs are navigation overlays | read-only reconstruction | exact ref + named evidence; stale prose is not current-head proof |
| CI/test/lint/provider problem | `AGENTS.md`; `.github/workflows/START.yml`; directly linked `docs/audit/` receipt/problem evidence | RafGitTools source for source defects; GitHub provider for provider state | existing/successor branch only | terminal run/job + exact head + rerun; provider claim additionally needs provider readback |
| Session AI work dispatch | `configs/session-ai-dispatch-adapter.v1.json`; `scripts/resolve_session_ai_work_packet.py`; pinned Mapa dispatch registry | Mapa routes; RafGitTools executes local authorized packets | resolver/validator named by adapter | assignment/route evidence only; `AGENT_ASSIGNMENT != AGENT_EXECUTION` |
| Documentation/navigation contribution | `docs/INDEX.md`; `configs/context-reconstruction-routes.v1.json`; `configs/practice-router.v1.json` | existing documentation router + Mapa federation where cross-repo | named documentation validator | navigation structure only; no runtime/claim promotion |
| Drive/NOVOexport corpus reconstruction | `docs/architecture/RAFGITTOOLS_DRIVE_CORPUS_START_HERE_V1.md`; existing corpus catalog implementation; directly authorized private/provider pointer | Drive/SAF owns physical corpus; public repo owns code/contracts only | metadata/catalog/project-pack route | public repo contains no raw Drive IDs, ACL identities, credentials, raw corpus bytes or private payload |
| Provider-main enforcement | `contracts/MAIN_PROVIDER_ENFORCEMENT_PLAN_20260927.v4.json`; `scripts/apply_rafgittools_main_protection.py`; current provider readback | GitHub provider is authoritative for actual protection state | bounded provider-environment gate only | dry-run/apply/rollback receipt + post-write provider readback; skipped lane != PASS |

### Stop condition

Stop instead of broad-searching when any required field is unresolved:

```text
SOURCE=TOKEN_VAZIO
AUTHORITY=TOKEN_VAZIO
EXECUTION_TARGET=TOKEN_VAZIO
EVIDENCE_RULE=TOKEN_VAZIO
ROUTE_STATE=BLOCKED
claim_allowed=false
```

This is a valid result. Do not convert absence into inferred completion.

## Invariants

```text
SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM
TOKEN_VAZIO != 0
IMPLEMENTED_UNTESTED != PASS
AGENT_ASSIGNMENT != AGENT_EXECUTION
SESSION_CONTEXT != LONGITUDINAL_MEMORY != EXECUTION_RECEIPT
REFERENCE != COPIED_CORPUS
```

Unknown source, authority, execution target or evidence rule is fail-closed:

```text
ROUTE_STATE=BLOCKED
missing_field=TOKEN_VAZIO
claim_allowed=false
```

## Existing authorities — do not duplicate

| Need | Existing authority |
|---|---|
| Editorial/current-state pointer | `docs/RAFGITTOOLS_CURRENT_STATE.md` |
| Documentation map | `docs/INDEX.md` + `CODE_TO_DOC_MAP.md` |
| Workspace/context architecture | `docs/architecture/RAFGITTOOLS_CONTEXT_WORKBENCH_INTEGRATION_AUDIT_V1.md` |
| Canonical portable context contract | `contracts/context-bundle-v2.schema.json` |
| Context conversion/validation | `scripts/context_bundle_v2.py` |
| Android context broker | `app/src/main/kotlin/com/rafgittools/workspace/ContextBroker.kt` |
| NOVOexport local-first reconstruction/index | `tools/rafaelia_navigator/README.md` |
| Drive/NOVOexport corpus boundary | `docs/architecture/RAFGITTOOLS_DRIVE_CORPUS_START_HERE_V1.md` |
| AI session routing | `docs/AI_SESSION_DISPATCH_ADAPTER_V1.md` + `configs/session-ai-dispatch-adapter.v1.json` |
| Governed Git writes | `docs/RAFGITFS_GOVERNED_GIT_WRITE_V1.md` |
| Provider-main mutation plan | `contracts/MAIN_PROVIDER_ENFORCEMENT_PLAN_20260927.v4.json` |

The machine-readable selection layer is `configs/context-reconstruction-routes.v1.json`.

## Reconstruction seed

A seed is a **pointer packet**, not a summary dump. It should be sufficient to re-enter a task deterministically while keeping original material at its authority.

Required conceptual fields:

```text
seed_id
created_at
intent
repo + ref
route_id
current_state_ref
source_refs[1..3]
authority
execution_target
evidence_refs[]
gaps[]
next
claim_allowed=false
```

Schema: `contracts/context-reconstruction-seed-v1.schema.json`.

Example: `examples/context-reconstruction-seed/minimal.example.json`.

## Service selection

Use the route registry rather than filename intuition:

```text
human/AI intent
  ↓
context-reconstruction-routes.v1.json
  ├─ CURRENT_STATE
  ├─ DOCUMENTATION_INDEX
  ├─ CONTEXT_BUNDLE_V2
  ├─ CONTEXT_WORKBENCH
  ├─ NOVOEXPORT_NAVIGATOR
  ├─ AI_SESSION_DISPATCH
  └─ GOVERNED_GIT_WRITE
```

A route may reference several files, but `source_min` is intentionally bounded to 1–3 roots. Expand only for missing source, contradiction, unresolved authority, missing evidence or explicit request.

## Control of content

- Store **references, IDs, refs, hashes, state and relationships** before copying prose.
- Keep raw/private corpus at the source authority.
- Preserve provider/repository boundaries.
- Keep historical receipts immutable; corrections supersede them.
- A route or seed is organizational evidence only; it does not prove domain/runtime claims.
- When a seed refers to Drive/private evidence, public GitHub should contain only the minimum non-sensitive pointer permitted by policy.
- Provider/external governance failures stay separate from source defects.
- Documentation navigation must point to an existing authority instead of creating a second current-state narrative.

## R3

`F_ok`: existing ContextBundleV2, ContextBroker, Navigator, session dispatch, corpus route, provider enforcement gate and RafGitFS are reused as authorities; the router now exposes bounded entry paths for the most common human/AI intents.

`F_gap`: moving-main freshness, provider enforcement, physical Drive/SAF execution and exact-head gates remain independently evidence-bound.

`F_next`: validate this documentation-only successor head; if the gate passes, keep this file as the single human/AI reconstruction router instead of adding parallel START documents.
