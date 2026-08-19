# RAFAELIA — Agent Federation Entry V1

State: `CANONICAL_DRAFT` · `claim_allowed=false` · append-only evidence

## Why this exists

The federation must not depend on a human re-sending a giant prompt or on an agent remembering the whole RAFAELIA universe. `AGENTS.md` is therefore treated as an **entry router**: it tells an agent how to recover the smallest authoritative subgraph required for the current task.

The common kernel is machine-readable in `configs/agent-entry-kernel.v1.json`. Repository-local `AGENTS.md` files specialize that kernel without replacing it.

## The three-layer model

1. **Federated kernel** — invariants common to every agent/repository.
2. **Local specialization** — build, runtime, ABI, security and domain constraints owned by the repository being changed.
3. **Observed state** — current commits, gaps, receipts, CI/device evidence and cross-repository relations.

This separation prevents a copied local rule from another repository from silently becoming authority.

## Entry algorithm

Every agent should execute the same boot sequence:

`bind → route → gaps → select → baseline → execute → verify-local → verify-edges → receipt → append`

The important property is that the agent loads **indices before bodies**. The default is not “read everything”; it is “find the relevant node/edge/gap, then open the exact evidence needed.”

## Orthogonal state axes

A single status is too weak. A gap can be known but deferred; urgent but uncertain; implemented but not tested. Therefore the model keeps independent axes:

- `knowledge_state`: what is actually known;
- `attention_state`: whether the item is active, deferred, ignored-with-reason, blocked by dependency or watched;
- `urgency`: execution ordering (`P0..P3`), never a truth score;
- `operational_state`: blocked/ready/testing/verified;
- `claim_gate`: whether a claim is blocked, structurally allowed, or allowed by the evidence boundary.

`IGNORED_WITH_REASON` is deliberately preserved. An ignored item is not absence. A deferred item is not solved. `TOKEN_VAZIO` is not zero.

## Deterministic next-action rule

The next action is selected lexicographically:

1. P0 safety/security/data-loss/recovery/execution blocker;
2. upstream dependency before dependent consumer;
3. `READY_TO_TEST` with an observable exit criterion;
4. narrow uncertainty-reducing action before broad redesign;
5. cross-repository blocker affecting more than one node;
6. oldest unresolved observation.

This avoids choosing work merely because it is prominent in the latest conversation.

## Historical determinism

Every meaningful action emits a transition receipt. The receipt binds:

`parent_event → event → source_commit → action → evidence → new state`

A successor may supersede an earlier observation, but must not rewrite it. Evidence from commit A is not silently inherited by commit B. Physical claims remain `TOKEN_VAZIO` until a physical receipt exists.

## Vector delta

Each execution should expose a compact delta vector:

- closed gaps;
- newly discovered gaps;
- uncertainty reduced/added;
- evidence added;
- dependencies changed;
- urgency transitions.

The federation therefore grows by **state transitions**, not by accumulating prose alone.

## Cross-repository rule

For an edge `producer → consumer`, a consumer-side dispatch or parser PASS is not enough to claim end-to-end success. The agent must locate the producer contract, consumer contract, protocol/version identity, exact request/response receipt, and any runtime/device boundary required by the claim.

If one side is absent, preserve the edge as `TOKEN_VAZIO`/`BLOCKED_BY_DEPENDENCY` rather than inventing continuity.

## Local role map

- **RafGitTools**: control plane, ledgers, gates, state axes, routing and federation contracts.
- **termux-app-rafacodephi**: Android runtime, bootstrap, package identity, local execution services, device receipts and Vectra provider boundary.
- **termux-packages**: package/source factory, prefix artifacts, hashes, stage schemas and handoff receipts.
- **Vectras-VM-Android**: VM/QEMU consumer, bounded Termux IPC, safe-state and guest/runtime receipts.
- **llamaRafaelia**: governed retrieval/memory ingestion, CTI/ONES provenance, privacy gates and semantic-evaluation boundary.

## Required output after an agent acts

Use the same three operational fields already used across RAFAELIA:

- `F_ok`: what was actually verified or materially improved;
- `F_gap`: what remains unresolved or what the action newly discovered;
- `F_next`: the next observable action chosen by the deterministic priority rule.

The receipt must also preserve `claim_allowed=false` unless the relevant local and cross-repository evidence gates explicitly permit promotion.

## Non-goals

This contract does not make every repository identical. It does not replace local `AGENTS.md`, READMEs, build contracts, threat models or scientific methods. It only guarantees that different agents enter the architecture through compatible state, evidence and priority semantics.
