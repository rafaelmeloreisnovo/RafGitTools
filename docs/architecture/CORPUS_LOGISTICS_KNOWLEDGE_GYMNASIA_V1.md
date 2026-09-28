# Corpus Logistics & Knowledge Gymnasia V1

State: **IMPLEMENTED_SOURCE / EXECUTION_EVIDENCE_PENDING**  
Executor: **RafGitTools**  
Private library: **CONVERSATIONS_CHUNKS_PRIVATE**  
Federated campus map: **Mapa**  
Default: **PRIVATE_DEFAULT_DENY**

## Purpose

Turn large Conversations/Codex/Cortex-style JSON corpora into a warehouse/library that minimizes movement cost without losing provenance.

The structure is:

```text
Campus
└── Gymnasium
    └── Lab
        └── Aisle
            └── Shelf
                └── Book
                    └── Session
                        └── Chunk
                            └── Token
```

A **Gymnasium** is a cross-cutting knowledge arena. It groups materialized and unmaterialized objects by relations, characteristics, questions, formulas, parables, gaps, evidence and components. It can connect domains without treating analogy as proof.

## Mechanical decomposition pattern

The planner supports the pattern:

```text
SYSTEM
→ SUBSYSTEM
→ COMPONENT
→ CHARACTERISTIC
→ RELATION
→ EFFECT
→ EVIDENCE
```

Examples such as engine → crankshaft → shaft → rotation → counter-rotation are useful as decomposition/analogy routes. Physical validity remains separately evidenced.

## Inventory-cost rule

The corpus is not reread wholesale. Movement cost is approximated from bytes, access frequency, active-project links, relation degree and duplicate penalty. The output assigns HOT/WARM/COLD/ARCHIVE using the existing Mapa semantic-carrier vocabulary.

The tier is a retrieval-placement decision, not an epistemic ranking.

## Source and publication boundary

Raw source bodies remain under immutable/private authority by default. GitHub receives content-addressed chunk metadata, indexes, typed edges, atlas/routes and receipts. Raw Git commit is disabled by default; LFS is an optional separately governed mode.

Overlap is represented by NEXT/PREVIOUS edges rather than repeated text bytes.

## Tokenization

Stable lexical/symbolic tokens are derived deterministically. Semantic marks are conservative:
- explicit kind/tag;
- TOKEN_VAZIO lexical sentinel → GAP;
- math syntax → FORMULA candidate with claim_allowed=false;
- explicit PARABLE tag → PARABLE with evidence_effect=NONE.

Model-specific tokenization is a derived view and must not become the chunk identity.

## Rollback

A generation is append-only. Rollback changes the active-generation pointer. It does not rewrite or delete historical generations.

## Execution

```bash
python tools/corpus_logistics/corpus_logistics_gymnasia_v1.py selftest
python tools/corpus_logistics/corpus_logistics_gymnasia_v1.py build normalized-records.jsonl campus-plan.json
```

The plan is handed to the governed RafGitFS writer for preview/dry-run/exact approval/publication. This module does not bypass write gates.

## Invariants

```text
SOURCE != CHUNK != INDEX != ATLAS != EVIDENCE != CLAIM
ANALOGY != EVIDENCE
CORRELATION != CAUSATION
MATERIALIZED != VALIDATED
TOKEN_VAZIO != 0
```

## R3

F_ok = deterministic content addressing + typed campus topology + gymnasia + tiers + relations + formula/parable/gap marking + reference-first privacy.

F_gap = exact Cortex source naming, live Drive stream adapter and governed publication runtime are separately evidenced.

F_next = run a bounded canary over one Conversations shard, validate plan, publish only derived private artifacts through RafGitFS, then scale by immutable generation.

## Privacy-preserving token index

Clear lexical tokens are not required in the Git publication. The planner emits `TOK-<sha256(normalized-token)>` references with kind/ordinal and a reverse chunk index. A query hashes the user's local token with the same normalization and resolves matching chunks. This preserves fast token routing while reducing plaintext leakage.

Additional indexes: `TOKENS.json`, `BOOKS.json`, `SESSIONS.json`, `MATERIALIZATION.json`, and `CHARACTERISTICS.json`.

Bounded query:

```bash
python tools/corpus_logistics/campus_query_v1.py campus-plan.json --token torque --limit 20
```
