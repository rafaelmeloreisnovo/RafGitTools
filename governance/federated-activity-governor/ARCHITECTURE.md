# Architecture — Evidence before authority

## Event flow

`observe -> normalize -> classify -> validate -> propose -> authorize -> apply -> readback -> receipt`

The governor never converts observation into authorization.

## Trust boundaries

1. GitHub event payloads are untrusted input until bound to repository, ref and SHA.
2. PAT values remain inside provider secret storage and are never persisted by the governor.
3. Supabase receives minimum metadata only and cannot authorize GitHub mutations.
4. Drive/Notion private bodies are referenced, not copied to public surfaces.
5. Slack carries coordination, not canonical evidence.
6. RLL scientific validity remains under RLL-specific methods and evidence.

## Idempotency

Suggested key:

`provider/repository/event_kind/source_identity/source_sha/schema_version`

A repeated key must read back the existing event or produce a typed conflict; it must not duplicate side effects.

## Routing

- valid + sufficient evidence -> candidate;
- known failure -> remediation successor;
- unknown -> investigation;
- unauthorized -> blocked;
- stale SHA -> superseded;
- privacy/security ambiguity -> fail closed.

## Rollback

Rollback means a compensating successor or revert PR tied to the original event. It does not mean deleting the original observation. Publication rollback must preserve release, artifact and receipt identity.

## Cost model

Measure provider minutes, queue time, cache hits, artifacts, storage, retries and cross-provider calls. Free-tier optimization cannot weaken provenance, retention or security gates.

## Initial audit result

- RafGitTools is the public executor/tool-router; Mapa is private federated authority.
- RLL has a Pages deployment workflow on `main`.
- No RafGitTools Pages workflow was found by scoped code search.
- Supabase already contains private append-only continuity and federated event tables.
- A public Slack coordination channel exists.
- PAT federation is documented as proposal/HOLD; capability remains provider-readback dependent.
