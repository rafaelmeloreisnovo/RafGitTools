# RafGitTools — Lente Federada Ω · cinco ciclos V1

State: **SOURCE_CANDIDATE / UNTESTED_ANDROID_DEVICE**. Claim promotion = false. Branch-only; no merge.

## Intention and authority

RafGitTools owns the hosted Android UI and metadata vector preview. Mapa owns federation/routes; producer repositories own their implementations. Drive owns private documentary custody/ACL/receipts. The existing Raf Actions screen alone owns workflow dispatch behind authenticated preflight. PAT identifiers are not grants. Follow contracts/PAT_CAPABILITY_MATRIX_V1.json. No secret values in this UI.

## Five successive cycles (bounded)

1. **Observe**: START HERE V2.2, HOTSTATE V4.5 and RafGitTools main head; classify unknown source, authority and proof without assuming they exist.
2. **Index**: type logical root (Drive private/dashboard, GitHub private/public, Mapa, paper, ethics), gap and a small set of public-safe taxonomy tokens; only local storage in volatile RAM.
3. **Vectorize**: SHA-256 feature hashing of sorted normalized taxonomy tokens into 64 integer count buckets; SHA-256 canonical node identity. This is NOT a semantic embedding, a meaning metric, encryption, ANN search or cryptographic proof of authorship.
4. **Relate**: cap 64 nodes, 16 tokens/node, 64 lexical candidate edges, input file 64 KiB; only pairs with actual shared tokens. No factorial random cross-products; no public/private export bridge. Pair overlap != causation.
5. **Gate and falsify**: reject malformed manifests, extra fields, duplicate IDs, raw-looking values and unauthorized promotions. Test deterministic reordering, bounds, typed gaps, privacy and relation semantics. Hold hosted CI, Android device, provider SAF behavior and OAuth scopes until exact-head evidence.

## Actual surface

A separate Android launcher named **Raf Lens** opens an isolated Compose screen; it never changes Home navigation. It can manually catalog logical IDs/tokens or read **one explicit, user-selected JSON metadata manifest** through Android SAF/Drive DocumentsProvider. No Drive OAuth client, no GitHub API call, no PAT read, no automatic repository crawl or content-indexing is implemented here. SAF source contents are bounded and validated, and no data are saved, uploaded or logged by the screen.

Input contract: `schemas/federated-lens-metadata-v1.schema.json`; no provider physical IDs, URIs, raw text, private receipts or access principals. Example (synthetic):

```json
{
  "schema": "rafgittools.federated-lens.metadata.v1",
  "nodes": [
    {"logical_id":"DRIVE_CORPUS","source_kind":"DRIVE_PRIVATE","tokens":["custodia","indices"],"gap_state":"TOKEN_VAZIO"},
    {"logical_id":"MAPA_ATLAS","source_kind":"MAPA_PUBLIC","tokens":["indices","rotas"],"gap_state":"UNVERIFIED"}
  ]
}
```

A manifest claiming `EVIDENCED_SCOPED` remains an **unverified declaration** from the imported file; in the UI it authorizes only local preview, never remote operation or claim promotion. Private source locators, full SHA fingerprints of low-entropy private tokens, identities and causal inferences must not be published.

## Gate matrix

| Gate | Requirement | V1 status |
| --- | --- | --- |
| G0 source identity | exact repo SHA + producer authority | read baseline only; session scoped |
| G1 schema | strict bounded metadata + unknown typed | code candidate |
| G2 vector | deterministic 64-dimensional feature hash | unit test source candidate |
| G3 privacy | no private payload/IDs, no network mutation | source invariant; device not run |
| G4 adapter | SAF provider manifest read | implemented source, physical pending |
| G5 pipeline | Raf Actions preflight per PAT + exact HEAD + rollback | **NOT CONNECTED**, HOLD |
| G6 claims | independently proven evidence per producer | **NO_PROMOTE** |

## Priority and selection

Use lexicographic priority, not fake confidence percentages:
P0 missing authority → P1 missing evidence/unknown → P2 unverified candidate → P3 scoped evidence, then bounded gain and age. "Avalanche" is a **change-impact propagation hypothesis** measured through typed edges; it is not unbounded code execution.

## Integration next

Only after independent review and exact-head CI: (a) consume read-only Drive provider metadata with ACL receipt, (b) consume GitHub private repository list through authorized logged-in API without raw data export, (c) reuse Mapa graph resolver as federated authority, (d) output separate append-only private Drive receipts with hash and readback, (e) gate provider PAT operations in existing Raf Actions. Store no private IDs in public source.

## Rollback and R3

Rollback: do not merge; close draft branch/PR if tests reject. Original main and private Drive corpus are untouched.

R3 = <F_ok: isolated metadata UI and structural-vector source candidate,
F_gap: runtime/CI/provider/semantic embedding and custody end-to-end,
F_next: exact-head CI + Android SAF negative tests + private-to-public ACL proof before any integration>.
