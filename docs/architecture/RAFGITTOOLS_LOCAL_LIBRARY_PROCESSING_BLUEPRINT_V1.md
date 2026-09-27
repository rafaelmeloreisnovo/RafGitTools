# RapidGT / RafGit Tools — Local Library Processing Blueprint V1

## Executive model

The Android device is an active processing node:

```text
Drive / local / Git / ZIP
 -> source binding
 -> durable local job queue
 -> extractors
 -> orthogonal vectors
 -> relation candidates
 -> bibliographic catalog
 -> indexes/graphs
 -> receipts
 -> optional distribution
```

## Modules

### 1. source
Responsibilities:
- enumerate SAF/Git/ZIP sources;
- create opaque source binding;
- never leak raw capability URI to public artifacts;
- preserve Drive A vs Drive B authority.

### 2. jobs
Responsibilities:
- typed job contract;
- rigor;
- resource budget;
- idempotency;
- checkpoint;
- retry.

Implementation reuses `OfflineQueue`; it does not create a second scheduler.

### 3. extract
Typed adapters:
- BYTE
- TEXT
- IMAGE_GRAY8
- future CODE/ELF/DEX/PDF/ZIP.

### 4. vector
V1 axes:
- byte distribution;
- text token buckets;
- visual intensity/region/gradient descriptors.

No single scalar vector is authoritative.

### 5. rigor
`QUICK / STRUCTURAL / MULTIMODAL / EVIDENCE`.

Rigor is monotone for a job. Resource pressure may checkpoint/block, but cannot silently downgrade.

### 6. relate
Candidate edges:
- SIMILAR_TO
- VERSION_OF
- DERIVED_FROM
- PART_OF
- REFERENCES.

Strong relations reuse catalog gates. In particular `COPY_OF` still requires hash-verified evidence.

### 7. materialize
Convert descriptors/results to:
- Work/Expression/Manifestation/Item;
- authority cards;
- index segments;
- relation edges;
- preservation events;
- receipts.

### 8. distribute
Default artifacts:
- catalog JSON;
- vector segment;
- relation segment;
- receipt;
- manifest.

Raw private content is excluded unless separately authorized.

## Queue

```text
PENDING
 -> RUNNING
 -> SUCCEEDED
 |-> CHECKPOINTED -> PENDING
 |-> BLOCKED_RESOURCE -> PENDING after conditions change
 |-> BLOCKED_POLICY
 |-> FAILED
 |-> TOKEN_VAZIO
```

Each job has `idempotencyKey`; materializers must refuse/replace by explicit policy rather than accidentally duplicating output after retry.

## Resource scheduling

The orchestrator should observe:
- battery percentage;
- charging state;
- Android thermal status;
- free working memory;
- source size;
- foreground/background policy.

V1 model already records battery, working-memory and thermal policy. Android adapters remain a separate layer so the core stays unit-testable.

Suggested presets:

| preset | min battery | network | use |
|---|---:|---|---|
| QUICK_INTERACTIVE | 10% | optional read-only | filenames/metadata/light vectors |
| STRUCTURAL_IDLE | 25% | read-only | full hash/text structure |
| MULTIMODAL_CHARGING | 40% + charging | read-only | image/text multimodal |
| EVIDENCE_CONTROLLED | explicit | pinned | full hashes/replay/receipts |

These are defaults, not universal safety thresholds.

## Vision: Voynich-derived engineering pattern

The uploaded Voynich pipeline uses multiple traversal views and derives entropy/color/linking measurements.

RafGit Tools V1 generalizes the engineering idea, not the manuscript interpretation:

```text
decoded tile
 -> intensity histogram
 -> regional occupancy
 -> gradient directions
 -> deterministic visual descriptor
```

Future V2:
- RGB means / color histograms;
- multi-scale tiles;
- rotational views;
- edge orientation histograms;
- perceptual hash;
- connected-component measurements.

Future V3:
- optional Android NNAPI/TFLite adapter only if explicitly selected;
- model hash/version must be recorded;
- model output remains a candidate signal, never identity proof.

## "Dog with glasses" rigor example

```text
QUICK:
 filename + dimensions + coarse visual histogram

STRUCTURAL:
 full content hash + deterministic visual descriptor

MULTIMODAL:
 visual + text/caption/metadata + multiple views

EVIDENCE:
 source pin + exact hash + descriptor version + replay + relation gate + receipt
```

Changing coat color or adding glasses may alter surface descriptors but should not alone decide identity/class.

## Bibliographic materialization

Descriptor jobs do not write arbitrary shelf paths directly.

They emit facts/candidates consumed by the existing catalog layer:

```text
descriptor
 -> work/authority candidate
 -> relation candidate
 -> catalog gate
 -> materialization
 -> shelf projection
```

This prevents "AI guessed category" from becoming a physical Drive move.

## Two Drive accounts

Each SAF root gets a distinct source ID.

```text
DRIVE_A item X
DRIVE_B item Y
```

may point to the same Work/Manifestation only after the catalog evidence supports it.

Source authority is never collapsed because two items hash equal.

## Data schema

`contracts/library-local-job-v1.schema.json` defines the portable job envelope.

Internal descriptor schemas are versioned separately:
- ByteVectorV1
- TextVectorV1
- VisualVectorV1

A descriptor version change creates a new version and does not rewrite old receipts.

## Physical implementation milestones

P0 — core contract/tests (this delta)
P1 — Android battery/thermal adapter
P2 — SAF streaming reader with bounded chunks
P3 — persistent job/checkpoint store
P4 — catalog materialization bridge
P5 — NOVOexport bounded pilot
P6 — second Drive binding
P7 — vector/relation indexes
P8 — library UI: queue, rigor lens, search, evidence view
P9 — Vectra/PCR accelerated/sandbox backends
P10 — distributed catalog/vector exchange with private custody

## Explicit non-claims

- visual descriptor is not breed recognition;
- similarity is not semantic identity;
- vector distance is not proof;
- local-first does not mean zero battery/thermal cost;
- no claim that current V1 fully processes both Drives;
- no physical full-corpus run has been performed by this commit.
