# RafGitTools Fluent Projection Architecture V1

Status: `MATERIALIZED_FOR_REVIEW / claim_allowed=false`

## Authority decision

RafGitTools is an executor/orchestrator and query surface. It is not the compiler authority and it is not the canonical event codec authority.

Authorities:

- compiler/event codec producer: `rafaelmeloreisnovo/RafPolimata`
- canonical Stage0 codec: `Apkc/raf_fluent_event.h`
- governance/routing: `rafaelmeloreisnovo/Mapa`

## SQL/Room boundary

The existing Android Room database remains useful for:

- UI/query acceleration;
- offline queue projections;
- reconstructible metadata;
- bounded content cache;
- local search/read models.

It must not be required to establish canonical build/runtime receipts.

Invariant:

`ROOM_SQLITE = RECONSTRUCTIBLE_PROJECTION`

not:

`ROOM_SQLITE = EVENT_AUTHORITY`

This is consistent with the existing `CacheDatabase` contract, which already describes Room as a reconstructible local index/cache/queue/receipt store while GitHub remains remote authority.

## Canonical ingest route

RafGitTools should consume RAFAELIA Fluent-compatible event bytes/events without inserting SQL as a mandatory translation stage:

`RafPolimata/RAFCODEPHI event -> event validator -> append-only receipt stream -> optional Room projection`

Room failure must not destroy the canonical event stream. Projection replay must be possible from retained canonical receipts.

## No duplicate compiler backend

RafGitTools must not independently grow another ARM32 code generator/linker.

Desired compile request:

`input + RAF_ARCH + ABI + flags/profile + expected gates -> rafcc-stage0/rafcc`

Desired response/evidence:

`artifact identity + hashes + gate state + RAFAELIA_FLUENT_EVENT/v1`

## Event classes to consume

Recommended tags:

- `rafaelia.compile.accepted`
- `rafaelia.compile.lowered`
- `rafaelia.compile.codegen`
- `rafaelia.artifact.sealed`
- `rafaelia.gate.result`
- `rafaelia.runtime.launch`
- `rafaelia.runtime.exit`
- `rafaelia.provenance.bound`

Unknown evidence remains `TOKEN_VAZIO`.

## Projection rule

Every SQL/Room row derived from an event should retain enough identity to trace back to the event authority:

- event sequence;
- source/artifact SHA-256 when present;
- tag/event type;
- component;
- architecture;
- state;
- projection timestamp/version.

A SQL migration changes only the read model. It must never reinterpret a previous failed/unknown gate as PASS.

## Roadmap

### G1
Add a canonical-event reader/validator that accepts the V1 envelope and rejects malformed/non-versioned records.

### G2
Persist canonical event bytes append-only before any projection mutation.

### G3
Project selected event fields into Room for UI/offline queries.

### G4
Implement replay: empty/rebuild the derived projection from canonical receipts.

### G5
Route compilation requests to the RafPolimata Stage0/compiler authority instead of Python/clang-specific helper logic where the Stage0 language/profile is supported.

### G6
Expose projection health separately from compiler/runtime health.

Invariant:

`PROJECTION_PASS != COMPILER_PASS != RUNTIME_PASS != CLAIM_PASS`

## R3

- `F_ok`: Room is explicitly demoted to reconstructible projection; compiler authority is singular.
- `F_gap`: canonical event reader/replay implementation not yet materialized; current Room entities remain operational legacy surface.
- `F_next`: implement append-first V1 event ingest and replayable Room projection after the RafPolimata V1 codec is validated by CI/ARM32 evidence.
