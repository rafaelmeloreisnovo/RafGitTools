# RafGit Tools — Bibliotheca / Google Drive Control Plane V1

Status: SOURCE_IMPLEMENTED / CI_PENDING / claim_allowed=false

## Goal

Extend the existing Drive/SAF corpus path into a non-destructive library-control plane for multiple Google Drive roots.

SOURCE != BIBLIOGRAPHIC IDENTITY != SHELF PROJECTION != RELATION != EVIDENCE != CLAIM.

## Existing capabilities reused

RafGit Tools already has:
- Drive/SAF staging with SHA-256 readback;
- private corpus intake;
- structural JSON cataloging;
- catalog-only SAF export;
- private SAF repository snapshots;
- append-only/governance receipts;
- GitHub branch/write/rollback machinery.

V1 adds the missing bibliographic model.

## Entities

- Source: one selected provider tree/root.
- Work: conceptual work/title.
- Edition: version/manifestation of a work.
- Item: one concrete Drive/local/Git object, analogous to an exemplar.
- Authority: controlled person/org/project/repository/software/dataset/term/standard record.
- Collection/shelf/section/session: navigation projections.
- Relation: typed edge between catalog objects.

COPY_OF is fail-closed and requires HASH_VERIFIED evidence.

## Multi-Drive

Two paid Google Drive accounts are modeled as two independent source bindings such as DRIVE_A and DRIVE_B.

Android SAF remains the acquisition boundary:
OpenDocumentTree -> READ permission -> source locator hash -> LibrarySourceBinding -> catalog.

Raw capability URI is not stored in public receipts.

## Private Drive materialization observed 2026-09-27

RAFAELIA_LIBRARY_MAP_2026-09-22 was extended non-destructively with:
07_CATALOGO_FICHAS
08_AUTORIDADES_VOCABULARIOS
09_CLASSIFICACAO_TAXONOMIA
10_COLECOES_ESTANTES_SECOES
11_PROVENIENCIA_CIRCULACAO
12_PRESERVACAO_RETENCAO
13_MULTI_DRIVE_REGISTRY
14_INGEST_QUARENTENA
15_INDICES_BUSCA
16_RELACOES_MANIFOLD

Exact private Drive folder IDs are intentionally not committed to this public repository.

No existing Drive file was moved or deleted.

## NOVOexport

The canonical NOVOexport root and semantic-library artifacts were recorded as existing in Drive on 2026-09-27. This control plane treats them as source/catalog authorities and does not duplicate the raw corpus.

The catalog pilot on branch `codex/novoexport-library-catalog-v1-20261002` composes a complete metadata inventory of the user-selected SAF tree. Catalog tree nodes preserve folder/file parent links, display names, reported MIME types, and reported sizes. The independent processing queue remains limited to `conversation*.json` and `codex*.json` candidates.

The catalog keeps access classification at `TOKEN_VAZIO`, records missing bibliographic parents as explicit gaps, and leaves claims disabled. Source locator and document IDs are represented by SHA-256 fingerprints; raw SAF URIs and provider document IDs are not included in the catalog. No source bytes are read, copied, or content-hashed. These fingerprints are not anonymization or proof of truth.

The searchable preview filters the in-memory tree by path/name. It is not a full-text/vector index. Export requires a separate confirmation and writes only the materialized tree catalog and its receipt through SAF. The export receipt records the provider authority and readback result, but does not inspect Drive ACLs or verify private/shared/public visibility. CI passed for the prior exact head; a physical Drive/provider run remains pending for this branch.

## Materialization

LibraryCatalogMaterializer:
1. validates the bundle;
2. normalizes ordering;
3. writes catalog JSON atomically;
4. computes SHA-256;
5. performs readback verification;
6. writes a receipt.

LibraryCatalogTreeExporter reuses CorpusCatalogTreeGate so only catalog + receipt are exported to the selected Drive tree.

## Safety

- source bindings are read-only in V1;
- credential markers fail closed;
- claim_allowed=true fails closed;
- duplicate IDs fail closed;
- unknown references fail closed;
- physical move/delete is outside V1.

## Workflow

DISCOVER -> IDENTITY_BIND -> DESCRIBE -> AUTHORITY_CONTROL -> CLASSIFY -> CATALOG -> RELATE -> INDEX -> EVIDENCE -> RECEIPT -> SHELF_PROJECTION -> PRESERVATION -> LEARN

## F_gap

- second Drive source binding is not yet physically observed in the current app/connector session;
- full re-enumeration of both Drives is NOT_RUN;
- full item-level SHA-256 coverage is NOT_RUN;
- full semantic authority/subject extraction is NOT_RUN;
- reversible physical move planner is not implemented;
- multi-source circulation/change journal is not implemented;
- full library search is not wired; the catalog pilot exposes only an in-memory path/name-filtered preview of the selected tree;
- no whole-Drive delete/dedup authorization exists.

## F_next

Physically verify the bounded NOVOexport tree pilot on a handset/Drive provider; bind DRIVE_A and DRIVE_B through SAF; add content hashing or semantic indexing only as separately gated stages; compare identities before any move.
