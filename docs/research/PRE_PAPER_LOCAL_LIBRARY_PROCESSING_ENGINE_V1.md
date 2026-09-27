# PRE-PAPER — RapidGT/RafGit Tools as a Local Library Processing Engine V1

Date: 2026-09-27
State: PRE-IMPLEMENTATION CONTRACT
claim_allowed: false

## Problem

The library is not only a catalog of files already classified elsewhere.

The Android device must be able to perform the local transformation:

```text
SOURCE
-> INGEST
-> DECOMPOSE
-> EXTRACT
-> VECTORIZE
-> RELATE
-> MATERIALIZE
-> DISTRIBUTE
```

while preserving:
- source identity;
- read-only source boundaries;
- deterministic replay where applicable;
- checkpoints;
- resource budgets;
- evidence state;
- rollback;
- TOKEN_VAZIO instead of invented metadata.

## Source-derived inspiration: Voynich prototypes

The uploaded Voynich analysis code contains four reusable engineering ideas:

1. ordered corpus acquisition into a local dataset;
2. multiple traversal/stride views over the same corpus;
3. extraction of compact image measurements such as entropy, color statistics and pairwise linking;
4. discovery of repeated structures/families from the derived measurements.

These engineering patterns are reusable.

Interpretive conclusions about the Voynich manuscript are **not** inherited as facts by this library engine.

## Hypothesis

A phone-scale library engine can materially reduce repeated cloud work if it stores compact deterministic descriptors rather than repeatedly reopening complete source objects.

The target is not "compress arbitrary information beyond its entropy".

The target is:

```text
raw object
-> stable identity + descriptors + relations
-> small searchable local representation
```

## Local-first invariants

```text
SOURCE != JOB != VECTOR != RELATION != MATERIALIZATION != CLAIM
READ_SOURCE != MUTATE_SOURCE
QUICK_MATCH != VERIFIED_IDENTITY
VISUAL_SIMILARITY != SEMANTIC_IDENTITY
VECTOR_DISTANCE != PROOF_OF_COPY
TOKEN_VAZIO != 0
```

## Processing stages

### J0 DISCOVER
Enumerate source identities only.

### J1 INGEST
Bind opaque source reference and immutable/read-only metadata.

### J2 EXTRACT
Generate typed descriptors from bytes/text/image/code/container metadata.

### J3 VECTOR
Create compact deterministic vectors. V1 vectors are integer/fixed-point friendly.

### J4 RELATE
Generate relation candidates with explicit evidence state.

### J5 MATERIALIZE
Create catalog cards, authority records, indexes, graphs and receipts.

### J6 DISTRIBUTE
Export catalog/vector/receipt artifacts, not raw private source by default.

## Rigor Lens

Rigor is a processing contract, not a confidence decoration.

```text
QUICK
STRUCTURAL
MULTIMODAL
EVIDENCE
```

QUICK may use names, size, MIME and shallow byte samples.

STRUCTURAL adds complete hashes where authorized, token/byte distributions and format-specific structure.

MULTIMODAL adds typed visual/text/code descriptors.

EVIDENCE requires exact source identity, full required hashes, replayable job parameters and a receipt before a relation can be promoted.

## Resource model

Every job declares:
- max bytes;
- max working memory;
- max item count;
- thermal class;
- battery policy;
- network policy;
- checkpoint interval;
- retry count.

A job that cannot satisfy its budget becomes BLOCKED_RESOURCE or CHECKPOINTED. It does not silently downgrade rigor.

## Phone execution model

Use existing RafGit Tools primitives:
- OfflineQueue for durable local ordering;
- WorkManager/worker orchestration for deferred execution;
- SAF for read-only Drive/local roots;
- existing corpus catalog export gate;
- existing receipt/provenance infrastructure.

## Vision V1

No OpenCV/Numpy dependency in the core.

The primitive visual input is a decoded 8-bit grayscale/RGB tile supplied by an adapter.

V1 computes:
- 16-bin intensity histogram;
- mean/variance/entropy approximation;
- quadrant occupancy;
- horizontal/vertical gradient energy;
- coarse orientation bins;
- optional RGB channel means;
- deterministic perceptual signature.

A future image decoder adapter may use Android Bitmap/ImageDecoder without changing descriptor semantics.

## Vector model

A library item can carry orthogonal vectors:

```text
V_meta
V_byte
V_text
V_visual
V_code
V_graph
```

No universal scalar merge is required.

A query declares which axes and rigor level it uses.

## Relation gate examples

```text
same SHA-256 => byte identity evidence
similar visual vector => SIMILAR_TO candidate
same work authority/title => bibliographic candidate
same filename => weak signal only
```

COPY_OF remains gated by hash-verified evidence.

## Distribution

The local device may distribute:
- catalog segments;
- vector segments;
- graph edges;
- receipts;
- manifests;
- indexes.

Raw private source remains local unless an explicit authorized route allows transfer.

## Falsifiers

F001: descriptor changes across deterministic replay for the same bytes.
F002: rigor downgrade occurs without explicit state transition.
F003: resource limit is exceeded without checkpoint/block.
F004: relation promotion occurs from vector similarity alone.
F005: raw capability URI or credential leaks to exported catalog.
F006: queue retry duplicates a materialization without idempotency guard.
F007: image transform changes descriptor semantics without version increment.
F008: two source accounts collapse into one source authority.
F009: TOKEN_VAZIO is serialized as zero/empty factual value.
F010: distributed vector cannot be traced back to source/job/descriptor schema versions.

## Initial implementation delta

Implement:
- typed local library job contract;
- rigor profile;
- device/resource budget;
- deterministic byte/text/visual descriptor primitives;
- checkpointed executor boundary;
- integration adapter to OfflineQueue;
- JSON schema;
- tests;
- architecture document.

Physical full-Drive execution remains NOT_RUN.
