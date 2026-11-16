# AGENTS.md — Federated Activity Governor

Scope: `governance/federated-activity-governor/**`.

Read parent `/AGENTS.md` first. This file narrows authority; it never expands it.

## Mission

Observe, classify, index and propose bounded transitions across GitHub, Supabase, Drive, Notion and Slack while preserving producer authority, privacy, provenance, reconstruction and human authorization.

## Mandatory preflight

Resolve all fields or emit a typed `TOKEN_VAZIO_*`:

1. exact repository/ref/SHA/path;
2. local and federated authority;
3. event source and idempotency key;
4. data/privacy/security classification;
5. applicable validator profile;
6. producer and consumer evidence;
7. promotion or remediation target;
8. exit criterion and stop condition;
9. rollback or compensating successor;
10. receipt destination and readback rule.

## Allowed in V1

- read public GitHub metadata and authorized private metadata;
- normalize metadata without copying secret or private payloads;
- validate the portable event schema;
- generate a proposal, draft PR or receipt under explicit repository authority;
- record append-only metadata after provider readback;
- notify Slack with public-safe references after an explicit request.

## Forbidden in V1

- read, print, rotate or replicate PAT values;
- infer capability from a secret name or its existence;
- use PAT fallback or a personal login as universal authority;
- auto-merge, auto-release, modify rulesets or alter repository permissions;
- delete Supabase events or rewrite receipts;
- copy Drive/Notion private bodies to public GitHub or Slack;
- promote RLL scientific/runtime claims from RafGitTools infrastructure checks;
- claim GitHub Pages availability from a workflow file alone.

## State model

`OBSERVED -> TYPED -> VALIDATING -> {REMEDIATION | INVESTIGATION | CANDIDATE} -> AUTHORIZED -> PUBLISHED`

A failed or unknown item is re-routed by a successor event. Git history and original receipts are preserved.

## Promotion gates

- G0 identity and authority
- G1 governance/data/privacy/security
- G2 source/build/test
- G3 provenance and reproducibility
- G4 producer-consumer integration
- G5 independent review and exact-SHA authorization
- G6 publication/readback/rollback readiness

Only G5 may authorize promotion; no model, schedule, database trigger or PAT can replace it.

## Supabase boundary

Use existing private append-only tables when schema-compatible. Do not create a duplicate table without an explicit schema decision. Supabase is metadata memory, not executor or merge authority.

## Receipt minimum

`event_id | predecessor | exact source | observed_at | state | urgency | risk | authority | evidence | gap | next | falsifier | rollback | claim_allowed`

## Stop conditions

Stop fail-closed on ambiguous authority, changed SHA after approval, secret exposure risk, private-to-public transfer, missing producer/consumer evidence, destructive request, or unavailable rollback.
