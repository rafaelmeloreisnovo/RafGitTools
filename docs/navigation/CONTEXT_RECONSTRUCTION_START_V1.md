# Context Reconstruction START V1

Status: `IMPLEMENTED_SOURCE / EVIDENCE_PENDING / claim_allowed=false`

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

## Invariants

```text
SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM
TOKEN_VAZIO != 0
IMPLEMENTED_UNTESTED != PASS
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
| AI session routing | `docs/AI_SESSION_DISPATCH_ADAPTER_V1.md` + `configs/session-ai-dispatch-adapter.v1.json` |
| Governed Git writes | `docs/RAFGITFS_GOVERNED_GIT_WRITE_V1.md` |

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

## R3

`F_ok`: existing ContextBundleV2, ContextBroker, Navigator, session dispatch and RafGitFS are reused as authorities.

`F_gap`: this V1 requires exact-head validation/CI before `PASS`; current-state freshness remains independently revision-bound.

`F_next`: validate registry + seed example, open draft PR, observe exact-head checks, then append the material delta to the longitudinal ledger.
