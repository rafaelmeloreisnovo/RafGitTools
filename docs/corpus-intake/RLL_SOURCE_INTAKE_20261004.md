# RafGitTools — RLL Source Intake Contract — 2026-10-04

Purpose: allow the existing Drive processing flow to ingest these three source packages without putting raw corpus bytes into Git history.

## Drive custody

Folder: `13B9hxoGZB5P552fF6DsOVl3vbrsYVmeW`
Manifest: `1_N7I1M2FFuShSls5NIlD-CGbypq5ckKv`

## Sources and processor lanes

1. `Manifesto-publico-main (1).zip`
   - drive_file_id: `1swqfTf88AYI5Q5xyylz8awwx3Zq5fqie`
   - sha256: `bf30da9182ae2472b2d18158a6f9e3bc1a179b5a63b422d9683941499a094fb5`
   - lane: `ZIP_CONTAINER -> DOCUMENT + IMAGE`
   - policy: sensitive-data projection must be redacted

2. `Matriz_Simbiotica_Aurora_Boreal_Plantas_RAFCODE_SIGMA.zip`
   - drive_file_id: `1fmma2jAR0N1ZqIJMvOilW5t7OAAtO4nn`
   - sha256: `174b5db1a6698eb2fa8096572a4e9039b3fb6bed89822fbb7d0bf641f3a4aa30`
   - lane: `ZIP_CONTAINER -> IMAGE`
   - semantic state: `VISUAL_CONCEPT / NOT_EVIDENCE`

3. `RAFAELIA_RUIDOukkk_VETORES_AMOR_CRUZADO.zip`
   - drive_file_id: `1Vd6qmHf-ejKmY5HnCMjUB0MrtN2KjwUc`
   - sha256: `a2f9632a9b538d609c4b6745f27725627a626246a770dd8a6881107c28fa1e5f`
   - lane: `ZIP_CONTAINER -> CSV_MATRIX`
   - observed shape: `7777 x 128`
   - semantic schema: `TOKEN_VAZIO`

## Required emitted deltas

`SOURCE_DISCOVERED`, `OBJECT_DISCOVERED`, `CONTENT_TYPED`, `RELATION_DISCOVERED`, `SENSITIVE_DATA_DETECTED`, `MATRIX_SHAPE_DISCOVERED`, `TOKEN_VAZIO_OPEN/CLOSE`, `CHECKPOINT`, `RECEIPT`.

## Invariants

- source bytes remain in Drive custody
- Android cache is disposable
- `SOURCE != INDEX != EVIDENCE != CLAIM`
- incomplete semantics remain `TOKEN_VAZIO`
- scientific promotion belongs to the RLL evidence/claim gate, not the client
