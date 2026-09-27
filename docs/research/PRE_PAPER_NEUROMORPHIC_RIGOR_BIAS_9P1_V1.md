# PRE-PAPER — Neuromorphic Rigor Bias 9+1 for Local Library Processing V1

Date: 2026-09-27
State: PRE-IMPLEMENTATION CONTRACT
claim_allowed: false

## Provenance reviewed

### llamaRafaelia / rmrCti

Observed head: `6a75ec21456c73303a4e0bf4ca987a65b29010eb`

Key artifacts:
- `rmrCti/omega_neuro_full.c` blob `1818b52f2b02bb2913bf6077a15ad1922c7109cc`
- `rmrCti/omega_forest.c` blob `7f8065419a935a1e9a99e4bdcdb7dbe3613c981e`
- `rmrCti/build_forest_neuro.sh` blob `23a45976476a0c5390f64083a858a14dce888672`
- `rmrCti/CTI_MEMORY_INTEGRATION.md` blob `91b5924fe9772a41e78ff479129793978cbb1c1a`
- `rmrCti/rmrcti_dataset_mobile.py` blob `f8e6687ef1994f64db8bcb5ce765f8cd21c842f1`

Observed behavior:
- 29 NeuroMetrics over corpus chunks;
- entropy, latent-token count, semantic gap, transition energy, Hamming and technical density are explicit measurements;
- forest classifier routes records into PROCESSUAL / VOID / FORGOTTEN / MENOSPREZADO / URGENT;
- CTI memory uses forest/frame values as ranking bonuses, not as semantic proof;
- mobile dataset runner already has bounded-memory and strict byte budgets.

### Rafaelia_Private

Observed head at review: `3ef3b6dfc26180538542ad2a03db6767ee39394d`

Key artifacts:
- `FRAMEWORKRAFAELIA/neuromorphic_core.py` blob `32ad1ca3d9328c89c1944b83bde1e4808b60e63f`
- `Low-level/raf_synaptic_core.c` blob `87dc524ace79e4c3f11cd53dd21dcf7783e8b392`
- `src/python/raf_synaptic_core.py` blob `75f918208cfd5f826712efaa16313d189a97867e`
- `src/c/plasticity_engine.c` blob `b705d077ffeab5686f10e3dca62774cd90cea47b`
- `src/c/synapse_registry.c` blob `d4fceaf0e1366eca75c0053ef141f879727ccc49`

Observed behavior:
- HDC hypervectors with bind/bundle/rotate/similarity;
- sparse distributed associative memory;
- Hebbian strengthening/decay;
- 9 processing heads + 1 observer pattern;
- low-level version uses random weights, heap, pthreads and float;
- plasticity engine persists learned action records to disk.

### Vectras-VM-Android

Observed head: `09923dc8e9d2b547920d7d518fb2fc269d96a975`

Key artifact:
- `tools/vision/visao_index.py` blob `a1c1775de8153cfe023311e6e8ce1c3f063bb203`

Observed behavior:
- local image flow to_do / doing / done / imgdataset;
- SHA-256 + CRC32 over real file bytes;
- duplicate identity is defined by SHA-256 equality, not filename.

## Design conclusion

The reusable invariant is not "use a neural network to decide truth".

It is:

```text
9 independent measured channels
-> bounded observer
-> processing priority + requested rigor
```

The observer cannot:
- change source bytes;
- promote evidence state;
- create COPY_OF;
- authorize a claim;
- overwrite explicit user rigor;
- convert TOKEN_VAZIO into zero.

## Two outputs, never one

Urgency and rigor are orthogonal.

```text
priority_bias = what should be inspected sooner
rigor_bias    = how deeply it should be inspected
```

A record can be urgent but weakly evidenced, or non-urgent but require evidence-grade processing.

## Nine input heads

H1 IDENTITY
- exact source identity / content-hash coverage.

H2 PROVENANCE
- source authority, version, locator and lineage completeness.

H3 STRUCTURE
- parseability, format structure, technical density.

H4 NOVELTY_GAP
- semantic/descriptor discontinuity, outlier pressure, unresolved gap.

H5 RELATION
- independent relation support and neighborhood consistency.

H6 MULTIMODAL
- availability/completeness of text/image/code/vector views.

H7 CONTRADICTION
- contradiction pressure; this increases required rigor, never confidence.

H8 REPRODUCIBILITY
- replay parameters, deterministic descriptor version, repeated agreement.

H9 FRESHNESS
- staleness/version drift pressure.

The tenth observer combines the nine without learning from raw user access count.

## Forest/path input

The rmrCti path is a scheduler hint:
- URGENT: strong priority increase;
- MENOSPREZADO: moderate priority increase;
- FORGOTTEN: review-candidate increase;
- VOID: gap-review signal;
- PROCESSUAL: neutral.

No forest path changes evidence state.

## Neurometric input

Selected omega_neuro fields may supply measured pressure signals:
- entropy_raw;
- semantic_gap;
- logic_depth;
- hamming_dist;
- technical_d;
- s_latent;
- cross_entropy;
- noise_floor;
- weight_bias.

Names such as `phi_integral`, `omega_point` and fixed project constants are not treated as scientific validation signals.

## Deterministic V1

V1 uses integer Q16 only.

No:
- random weights;
- Hebbian self-reinforcement;
- access-count reinforcement;
- float;
- model inference;
- automatic online learning.

Future adaptive weights require an append-only validated-feedback ledger and replay tests.

## Rigor routing

The observer may recommend:

```text
QUICK
STRUCTURAL
MULTIMODAL
EVIDENCE
```

Rules:
- explicit user-requested rigor is a floor;
- contradiction/reproducibility gaps may raise rigor;
- battery/thermal pressure may checkpoint but not lower rigor;
- evidence grade requires the existing evidence gates independently of this bias.

## Falsifiers

RB-F01 same inputs produce different bias.
RB-F02 path URGENT changes evidence state.
RB-F03 user EVIDENCE request is downgraded.
RB-F04 TOKEN_VAZIO channel is interpreted as factual zero.
RB-F05 access frequency alone raises rigor/evidence.
RB-F06 contradiction lowers required rigor.
RB-F07 priority and rigor collapse into a single score.
RB-F08 random/float-dependent output enters deterministic V1.
RB-F09 source-account identity is lost.
RB-F10 bias result cannot be traced to channel values and schema version.

## Implementation target

Add to RafGit Tools:
- `RigorBiasInputV1`
- nine typed channels with known/unknown state;
- `RigorBiasEngineV1`
- separate priority and rigor Q16 outputs;
- path scheduler bias;
- recommended rigor with explicit floor;
- tests proving no evidence promotion and no silent downgrade;
- provenance crosswalk receipt.
