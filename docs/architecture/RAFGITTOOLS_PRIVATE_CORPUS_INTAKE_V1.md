# RafGitTools — Private Corpus Intake & Custody V1

Status: CANDIDATE / FAIL-CLOSED / claim_allowed=false

## Purpose

Extend the existing Drive/SAF staging path into a bounded corpus-intake flow without turning
GitHub, CI or a public map into raw-corpus storage.

```text
LOCAL OR DRIVE JSON
→ SAF read grant
→ streaming private staging
→ SHA-256 + readback receipt
→ explicit "Catalogar corpus (privado)"
→ streaming structural JSON vector (no text retained)
→ private manifest
→ sanitized public-risk projection
→ explicit SAF folder export
→ provider readback receipt
```

## Privacy boundary

The public projection MUST NOT contain source filename, source path, raw JSON, JSON field names,
raw SHA-256, embeddings, personal identifiers, or private Drive IDs.

The private manifest may contain the source name and raw digest because it stays under private
custody. The public risk handle is random/opaque and is mapped back to private evidence only inside
the private custody plane.

## Vector boundary

V1 produces only `JSON_TOKEN_COUNTS_V1`: counts of objects, arrays, names, primitive token kinds,
and maximum nesting depth. It does not retain JSON strings or field names.

`semanticVectorProvider = TOKEN_VAZIO_EXPLICIT_PROVIDER_REQUIRED` until a separately authorized
provider is selected and governed.

## Authority split

- RafGitTools: user-facing selection, local staging, private structural catalog, explicit provider export.
- Rafaelia_Private: private intake contract, Drive destination registry, private risk review.
- MemRafcode: typed void/relation/custody semantics and longitudinal reentry.
- Mapa: sanitized public risk/status projection only.
- Google Drive: private corpus/catalog custody when explicitly selected by the user.

## Non-equivalences

`SELECTED != STAGED != CATALOGED != EXPORTED != EVIDENCE_PROMOTED`

`LOCAL_RECEIPT != PROVIDER_RECEIPT != INDEPENDENT_REPRODUCTION`

`STRUCTURAL_VECTOR != SEMANTIC_EMBEDDING`

`PUBLIC_RISK_HANDLE != RAW_CORPUS_IDENTITY`

## F_next

1. Build the candidate branch locally or on a public-capable runner.
2. On a physical Android device, select one disposable JSON fixture and verify staging/catalog/export.
3. Bind the private Drive directory only through user selection or private provider configuration.
4. Validate the same golden corpus-intake vectors in Rafaelia_Private.
5. Publish only sanitized risk projections to Mapa.

## RMR-ZIPRAF custody adapter

The private manifest declares `rmr-zipraf-evidence-envelope-v1` as the custody-envelope contract,
with authority routed to `rafaelmeloreisnovo/papers`. This app does **not** claim that an envelope
has been sealed merely because a corpus was staged/cataloged. The default state is
`TOKEN_VAZIO_NOT_SEALED`; sealing and any external cryptographic/time anchors are separate gates.
