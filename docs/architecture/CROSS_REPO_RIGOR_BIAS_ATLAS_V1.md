# Cross-repo Rigor Bias Atlas V1

## Stone that connects the existing parts

The reviewed repositories already contain three different mechanisms that must be kept separate:

1. **Measured corpus signals** — `omega_neuro_full.c`.
2. **Curation/ranking routes** — `omega_forest.c`, CTI memory and frames.
3. **Model behavior bias** — llama control-vector generator.

Rafaelia_Private additionally contains:
- HDC associative vectors;
- sparse distributed memory;
- Hebbian plasticity;
- 9+1 observer architecture.

The local library engine therefore uses the **9+1 observer geometry**, but its V1 output is deterministic Q16 and cannot mutate evidence.

## 9+1 mapping

```text
H1 identity completeness
H2 provenance completeness
H3 structure completeness
H4 novelty/gap pressure
H5 relation support
H6 multimodal completeness
H7 contradiction pressure
H8 reproducibility completeness
H9 freshness drift
        |
        v
H10 observer
  -> priority_bias_q16
  -> rigor_pressure_q16
  -> recommended rigor
```

Unknown channels remain absent in the arithmetic and are carried explicitly in `unknownHeads`. They are never imputed as zero.

## rmrCti bridge

Selected measured fields can raise processing pressure:
- semantic_gap;
- hamming distance;
- technical density;
- latent-token density;
- noise/cross-entropy pressure.

Forest path affects priority:
- URGENT highest;
- MENOSPREZADO next;
- FORGOTTEN and VOID are review candidates;
- PROCESSUAL neutral baseline.

No path can set:
- HASH_VERIFIED;
- RELATION_VERIFIED;
- COPY_OF;
- claim_allowed.

## Control-vector bridge

`tools/cvector-generator` is a separate route.

It can produce a model control vector from positive/negative examples and apply it to model layers. That is potentially useful later for a "rigorous answer style" or retrieval discipline.

It must never substitute for the data rigor engine because:
- it modifies model behavior;
- its effect depends on model/layers/scale;
- a stricter model tone is not stronger evidence.

## Loose-file finding

The rmrCti directory is not merely a pile of binaries. Its existing canonical record already distinguishes source, generated artifacts, legacy/experimental files, missing historical producers and known broken/truncated artifacts.

Therefore the correct action is not to rename/move everything blindly. The library engine should ingest the canonical record first, then reconcile loose files against it.

## Next execution bridge

After CI:
1. use `rmrcti_dataset_mobile.py` as a bounded mobile fixture producer;
2. map its content packs into RafGit Tools local jobs;
3. compute descriptor vectors;
4. feed measured rmrCti fields into `RmrCtiRigorAdapterV1`;
5. use bias only to order/raise rigor;
6. materialize cards/relations only through catalog gates;
7. emit receipt.
