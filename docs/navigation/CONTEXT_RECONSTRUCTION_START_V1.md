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
| Deterministic next-gate priority | `configs/agent-entry-kernel.v1.json` + `docs/UNCERTAINTY_URGENCY_FRICTION_ETHICS_LICENSE_BY_DESIGN_V3.md` |
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
  ├─ NEXT_BEST_GATE
  ├─ CONTEXT_BUNDLE_V2
  ├─ CONTEXT_WORKBENCH
  ├─ NOVOEXPORT_NAVIGATOR
  ├─ AI_SESSION_DISPATCH
  └─ GOVERNED_GIT_WRITE
```

A route may reference several files, but `source_min` is intentionally bounded to 1–3 roots. Expand only for missing source, contradiction, unresolved authority, missing evidence or explicit request.

## Next-best-gate selection — reduce uncertainty without reducing rigor

After `CURRENT_STATE` and the minimum evidence are bound, route to `NEXT_BEST_GATE` when more than one unresolved action is possible. Do **not** rank by document count, symbolic density, number of links or combinatorial reachability. Those properties do not increase truth or execution authority.

Use the already-governed priority rules from `configs/agent-entry-kernel.v1.json` as a deterministic lexicographic filter:

1. fail-closed governance, data, privacy, security, safety, rollback or execution blockers first;
2. inside the same urgency, unblock upstream dependencies before downstream consumers;
3. prefer `READY_TO_TEST` work with an observable exit criterion over speculative redesign;
4. prefer the smallest reversible action that reduces one named uncertainty and exercises a falsifier;
5. prefer a cross-repository blocker that unlocks several dependent nodes over isolated cosmetic debt;
6. if still tied, take the oldest unresolved observation;
7. stop when the exit criterion is observed, authority changes, a mandatory dependency is unavailable, or more history has no marginal effect on the decision.

Every selected unit must preserve:

```text
gap_id
urgency / risk
source + authority
known evidence
missing evidence
blocked uses
dependencies
falsifier / exit criterion
rollback
next gate
claim_allowed=false unless separately promoted by its own evidence rule
```

`TOKEN_VAZIO` is not a low score to be optimized away. It is often the most valuable output when it identifies the exact missing source, authority, execution or evidence needed to make the next decision deterministic.

### DMAIC as execution discipline, not certification claim

For a bounded documentation/control-plane change:

```text
DEFINE  = intent + CTQ + authority + stop condition
MEASURE = exact ref + source/evidence baseline + named uncertainty
ANALYZE = dependency/blocker/falsifier + contradiction classification
IMPROVE = smallest reversible causal delta
CONTROL = exact-head validation + readback + receipt + rollback/supersedes
```

This mapping is a process discipline. It does not assert statistical Six Sigma performance, ISO certification, RFC/IEEE conformance or any other certification without the corresponding measured/audited evidence.

## Control of content

- Store **references, IDs, refs, hashes, state and relationships** before copying prose.
- Keep raw/private corpus at the source authority.
- Preserve provider/repository boundaries.
- Keep historical receipts immutable; corrections supersede them.
- A route or seed is organizational evidence only; it does not prove domain/runtime claims.
- When a seed refers to Drive/private evidence, public GitHub should contain only the minimum non-sensitive pointer permitted by policy.

## R3

`F_ok`: existing ContextBundleV2, ContextBroker, Navigator, session dispatch, RafGitFS, agent-entry priority rules and uncertainty/friction governance are reused as authorities.

`F_gap`: this V1 successor requires exact-head validation/CI before `PASS`; current-state freshness and any selected domain gate remain independently revision-bound.

`F_next`: validate registry + anti-regression test on the exact head, open a draft PR, observe checks, then append the material delta to the longitudinal ledger without promoting unrelated TOKEN_VAZIO.
