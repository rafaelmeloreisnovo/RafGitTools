# RafGitTools — Seven Knowledge/Work Guards V1

State: **IMPLEMENTED_SOURCE / REMOTE_CI_TOKEN_VAZIO**  
Authority: `rafaelmeloreisnovo/RafGitTools`  
Federated source: `rafaelmeloreisnovo/Mapa` PR #619, merged as `3a2821d44d555c28cd041ba029cfa940fe3d4c0a`  
Claim gate: `claim_allowed=false`

## Purpose

Project the knowledge/work-house semantics into the RafGitTools control plane without duplicating the Mapa corpus or moving producer authority.

The seven explicit guards are:

1. **Provenance** — source/provider/repository/ref/path/hash/time/authority.
2. **Context** — intent/scope/boundary/time/dependencies.
3. **Evidence** — typed exact references with bounded scope.
4. **Contradiction** — comparable conflicts stay OPEN/RESOLVED/SUPERSEDED; OPEN blocks readiness.
5. **Uncertainty** — `TOKEN_VAZIO`, `BLOCKED` and `PARTIAL` remain typed and require evidence-needed, falsifier and next probe.
6. **Reproduction** — procedure, environment, input and output identities are explicit; missing/failed reproduction blocks readiness.
7. **Rollback** — any mutation requires a READY or EXECUTED rollback with predecessor, procedure and verification.

Reconstructibility remains transversal: every envelope must carry a reconstruction pointer, but this RafGitTools adapter does not redefine the Mapa eight-guard source contract.

## Invariants

```text
SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM
TOKEN_VAZIO != 0
READY_FOR_DOMAIN_REVIEW != CLAIM_ALLOWED
ROLLBACK_READY != ROLLBACK_EXECUTED
RafGitTools receipt != producer runtime evidence
```

## Decision

The adapter emits only:

- `BLOCKED`
- `READY_FOR_DOMAIN_REVIEW`

It never emits a producer claim approval. `claim_allowed` must remain `false`.

A unit is blocked by any of:

- malformed or unresolved provenance;
- missing evidence;
- OPEN contradiction;
- `TOKEN_VAZIO/BLOCKED/PARTIAL` uncertainty;
- reproduction `TOKEN_VAZIO/BLOCKED/FAIL`;
- a mutation without rollback READY/EXECUTED.

## Files

```text
configs/knowledge-work-seven-guards.v1.json
scripts/validate_knowledge_work_seven_guards.py
tests/test_knowledge_work_seven_guards.py
examples/knowledge-work-seven-guards.example.json
docs/federation/RAFAELIA_KNOWLEDGE_WORK_SEVEN_GUARDS_V1.md
```

## Validation

```bash
python3 scripts/validate_knowledge_work_seven_guards.py
python3 scripts/validate_knowledge_work_seven_guards.py examples/knowledge-work-seven-guards.example.json
python3 -m unittest tests.test_knowledge_work_seven_guards -v
```

The canonical repository gate also runs the focused unittest and example validation. Structural PASS does not prove Android runtime, remote producer execution, privacy completeness, scientific validity or external reproduction.

## Rollback

This layer is additive. Reverting its commits removes the adapter and restores the prior RafGitTools gate. Mapa PR #619 and its history remain untouched.
