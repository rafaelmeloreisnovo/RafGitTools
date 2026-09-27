# PRE-PAPER — RMRCTI Mobile Content-Pack Bridge V1

Date: 2026-09-27
State: PRE-IMPLEMENTATION CONTRACT
claim_allowed: false

## Source contract

Pinned producer:

- repository: `rafaelmeloreisnovo/llamaRafaelia`
- path: `rmrCti/rmrcti_dataset_mobile.py`
- blob: `f8e6687ef1994f64db8bcb5ce765f8cd21c842f1`

The producer emits `rmrcti.content-pack/v1` records containing:
- pack ID;
- archive/member identity;
- CRC32;
- original uncompressed byte count;
- category/lifecycle role;
- top lexical terms;
- bounded sample text;
- `sample_scope=bounded_head_and_tail_not_full_member`;
- `epistemic_state=OBSERVED_SAMPLE`;
- `claim_allowed=false`.

## Critical boundary

```text
BOUNDED_SAMPLE != FULL_SOURCE
HASH(sample) != HASH(full member)
CRC32 identity hint != cryptographic full-content proof
OBSERVED_SAMPLE != semantic truth
```

Therefore the RafGit Tools executor must know the content scope of its bytes.

A bounded sample may produce:
- lightweight byte descriptor;
- lightweight text descriptor;
- lexical/card candidates.

It may not satisfy a rigor contract requiring a full-source hash.

## New source-scope contract

```text
FULL_SOURCE
BOUNDED_SAMPLE
METADATA_ONLY
TOKEN_VAZIO
```

For STRUCTURAL/MULTIMODAL/EVIDENCE work requiring full-content identity:

```text
contentScope != FULL_SOURCE
    -> CHECKPOINTED
    -> FULL_SOURCE_SCOPE_REQUIRED
```

The executor must not hash bounded sample bytes and call that result a full-content hash.

## Bridge behavior

The content-pack adapter:
1. validates exact pack schema/state;
2. hashes the raw archive locator into an opaque source-locator hash;
3. preserves member name/CRC32/size as observed metadata;
4. creates a QUICK local job over the bounded sample;
5. marks `contentScope=BOUNDED_SAMPLE`;
6. produces no `contentSha256`;
7. records that deeper rigor requires a later full-member stream.

The raw archive path is not copied into public/job receipts.

## Relationship to rigor bias

The bridge does not fabricate nine-head rigor values.

Before NeuroMetrics/forest/provenance signals are attached, those heads remain
`TOKEN_VAZIO`.

The bounded QUICK pass is allowed as acquisition/description.

If a later bias decision asks for deeper processing, the next action is to
stream the full member through the existing bounded RMRCTI mobile route rather
than pretending the sample is sufficient.

## Falsifiers

MP-F001: bounded sample receives non-null full-content SHA-256.
MP-F002: STRUCTURAL job over bounded sample succeeds as full source.
MP-F003: raw local archive path leaks into the job source locator.
MP-F004: malformed/claim-allowed content pack is accepted.
MP-F005: lexical co-occurrence is promoted to semantic equivalence.
MP-F006: sample job silently changes to full-source state.
