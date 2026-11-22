# Federated Activity Governor — V1

Status: `SOURCE_PROPOSAL_HOLD` · `claim_allowed=false`

This directory is the bounded, reconstructible entry for a proposed RAFAELIA activity governor. It does not grant PAT authority, execute cross-repository writes, publish releases, delete Supabase events, or replace the authority of Mapa or producer repositories.

## 30-second route

1. Read the repository root `/AGENTS.md`.
2. Read this directory's `AGENTS.md`.
3. Read `ROADMAP.md`.
4. Validate `schemas/governor-event.v1.schema.json` and `maps/federation.v1.json`.
5. Bind an exact producer repository, ref, SHA, path, authority, evidence rule and rollback.
6. Stop with typed `TOKEN_VAZIO` if any non-compensatory gate is unknown.

## Authority

- Mapa: federated routing and state authority.
- RafGitTools: control-plane executor and validator.
- Producer repository: local code, tests and release authority.
- GitHub: source, PR, Actions, Pages and release evidence.
- Supabase: private append-only metadata/index; never merge authority or a durable execution queue.
- Drive: canonical navigation, current state and reconstruction receipts.
- Notion: durable decision/context pointers; no corpus duplication.
- Slack: public coordination only; no private evidence replication.
- alphaXiv: research references and falsifiers; never automatic validation.

## Contents

- `AGENTS.md` — scoped operating contract.
- `AGENT.md` — compatibility pointer; no separate authority.
- `ROADMAP.md` — phased evolution and exit criteria.
- `ARCHITECTURE.md` — event flow, trust boundaries and rollback.
- `maps/federation.v1.json` — machine-readable ownership and edges.
- `schemas/governor-event.v1.schema.json` — portable event envelope.
- `receipts/2026-11-15-audit-s1.md` — source audit and typed gaps.

## Invariants

`SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM`

`TOKEN_VAZIO != 0`

`SECRET_PRESENT != CAPABILITY_PROVEN`

`SCHEDULED != EXECUTED != VERIFIED`

`PRODUCER_PASS + CONSUMER_PASS` is required for a cross-repository claim.

Rollback for this proposal: close the draft PR. Main remains unchanged.
