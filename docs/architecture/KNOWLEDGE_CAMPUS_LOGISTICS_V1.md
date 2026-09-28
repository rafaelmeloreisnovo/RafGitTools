# Knowledge Campus & Corpus Logistics V1

State: IMPLEMENTED_UNTESTED
claim_allowed: false

## Purpose

Turn the existing RAFAELIA library/corpus into a navigable knowledge campus without confusing analogy with evidence.

The model is:

SOURCE -> CAMPUS -> GYMNASIUM -> SYSTEM -> SUBSYSTEM -> COMPONENT -> PROPERTY -> FUNCTION -> INTERACTION -> EVIDENCE/GAP

A gymnasium is a bounded domain for exercising relations and transformations. It is not an evidence class.

## Physical/logistical model

- campus: federation/domain collection
- gymnasium: bounded knowledge/problem domain
- building: project/repository/corpus family
- street: route/domain
- shelf: locality/temperature bucket
- book: conversation/document
- session: coherent working session
- chapter: bounded semantic sequence
- chunk: content-addressed retrieval unit
- component/property/function: decomposition of technical objects
- formula/parable/token/gap/evidence: typed cross-cutting registries

## Invariants

SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM.
ANALOGY != FACT.
CORRELATION != CAUSATION.
PARABLE != EVIDENCE.
TOKEN_VAZIO != 0.
IMPLEMENTED_UNTESTED != PASS.

Raw sources remain immutable/read-only by default. Derived chunks are content addressed. Overlap is represented with PREVIOUS/NEXT edges rather than duplicating source text.

## Knowledge gymnasiums

Each gymnasium has:
- subject and scope;
- source authorities;
- systems and components;
- characteristics and units;
- functions and constraints;
- typed relations;
- formulas;
- hypotheses and analogies;
- materialization state;
- evidence refs;
- explicit gaps;
- next falsifiable/reversible action.

Example mechanical decomposition (illustrative only):

ENGINE -> CRANKSHAFT -> SHAFT
ENGINE -> PISTON -> COMPRESSION_RING
ENGINE -> VALVE
CRANKSHAFT -> FUNCTION -> CONVERT_RECIPROCATING_TO_ROTATION

Counter-rotating shafts/rotors may be represented as a relation that cancels or reduces a torque/reaction component only when a concrete system/source establishes that fact. The analogy itself does not promote a claim.

## Corpus logistics

Drive/NOVOexport -> immutable generation -> streaming parser -> semantic chunk -> token/type extraction -> typed relation graph -> atlas/routes -> HOT/WARM/COLD/ARCHIVE slotting -> governed publication.

Current producers already present in RafGitTools:
- rafaelia_navigator SQLite/FTS;
- deterministic publication segments;
- provenance/source pointers;
- governed Git write path.

This V1 adds the missing ontology/control contract. It does not copy private corpus bodies.

## Retrieval cost

A picker should minimize bytes read, files opened, retokenized bytes and remote hops while preserving provenance and reconstructibility.

Default route:
INTENT -> <=3 roots -> depth 1 -> answer if sufficient; expand only for missing_source, contradiction, unresolved_authority, missing_evidence or explicit_request.

## Privacy/publication boundary

RafGitTools is public. CONVERSATIONS_CHUNKS_PRIVATE is the intended private corpus/index authority.

Therefore this repository receives schemas, validators, orchestration and synthetic fixtures only. Private conversation bodies must not be committed here.

A future publisher must require an explicit private target and write capability before publishing corpus-derived bodies.

## R3

F_ok = ontology + logistics contract + fail-closed validator + synthetic fixture.
F_gap = exact Cortex source identity; private publisher runtime; token/formula/parable extractors; hotness telemetry; end-to-end canary.
F_next = validate this contract, then implement one synthetic/canary vertical slice before any bulk corpus movement.
