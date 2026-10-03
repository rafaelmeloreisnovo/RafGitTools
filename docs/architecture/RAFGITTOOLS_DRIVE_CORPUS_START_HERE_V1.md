# RafGitTools — Drive Corpus START HERE V1

Status: IMPLEMENTED_UNTESTED until exact-head CI completes

## Purpose

Provide one low-friction route for humans and AI agents to reconstruct the NOVOexport corpus context without publishing private provider identifiers or copying raw source material.

```text
SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM
TOKEN_VAZIO != 0
IMPLEMENTED_UNTESTED != PASS
```

## Canonical logical route

```text
Google Drive
└── NOVOexport
    └── 01_SISTEMA_CORPUS_CUSTODIA
        ├── conversations-000.json
        ├── ...
        └── conversations-050.json
```

The canonical corpus namespace is exactly `000..050`, inclusive: **51 expected files**.

The public repository intentionally does **not** carry Google Drive file IDs, folder IDs, raw SAF URIs, account tokens, ACL member identities, or corpus bytes. Those belong to the private documentary/evidence surface.

## Fast reconstruction

1. Read this file.
2. In the private Drive Master Index, open `START_HERE_DRIVE_GITHUB_V1`.
3. Resolve the source tree and current access evidence there.
4. On Android, use the SAF tree picker and select the logical source route above.
5. `NovoexportSafInventory` recursively inventories metadata only.
6. `NovoexportConversationCorpus` validates the exact `conversations-000.json` through `conversations-050.json` namespace.
7. `NovoexportLibraryCatalogComposer` records any missing, duplicate, or out-of-range corpus member as an explicit gap.
8. Materialize only derived catalog/receipt artifacts; do not mutate raw corpus files.

## Corpus gate

`NovoexportConversationCorpus` enforces:

- expected range: `000..050`;
- expected count: `51`;
- canonical filename: `conversations-%03d.json`;
- missing members become `CONVERSATIONS_MISSING_<indexes>`;
- duplicate members become `CONVERSATIONS_DUPLICATE_<indexes>`;
- `051` or other canonical-looking out-of-range names become `CONVERSATIONS_OUT_OF_RANGE_PRESENT`;
- unrelated `conversation*.json` or `codex*.json` candidates remain eligible for the broader processing queue but do not satisfy the canonical 51-file gate.

A catalog may still be produced when the corpus gate is incomplete, but `claimAllowed=false` and the gap remains explicit. This preserves the existing metadata-only catalog workflow without promoting incomplete coverage.

## Public / private / shared boundary

### PRIVATE_SOURCE

Use for raw corpus and source custody. Read is allowed only through an authorized provider/session. Mutation is denied by default. Raw source must not be published by a documentation or indexing operation.

Examples: NOVOexport source tree, `01_SISTEMA_CORPUS_CUSTODIA`, `conversations-000..050`.

### PRIVATE_DERIVED

Use for private indexes, provider-bound evidence, route tables, receipts, and reconstruction maps. Append/supersede derived artifacts; do not rewrite source history.

### SHARED

`LibraryAccessClass.SHARED` means evidence shows an object is shared with at least one non-owner principal while not being established as public. A filename containing words such as `shared` is **not** sharing evidence.

Do not classify `SHARED` from SAF visibility alone. The current Android SAF inventory does not inspect Drive ACLs. Use provider ACL metadata or an explicit trusted sharing receipt.

### PUBLIC

Use only for material intentionally safe for unrestricted publication: source code, tests, schemas, synthetic fixtures, public-safe architecture, and redacted receipts.

The public repository must not contain:

- raw conversation corpus content;
- raw Google Drive provider IDs or SAF capability URIs;
- credentials, PATs, OAuth tokens or secrets;
- private ACL member lists;
- private evidence payloads that make the corpus reconstructable without authorization.

### TOKEN_VAZIO

When visibility has not been proven, preserve `TOKEN_VAZIO`. Unknown is not equivalent to private, shared, or public.

## Drive ↔ GitHub responsibility split

| Surface | Authority | What belongs there | Default mutation |
|---|---|---|---|
| Private Drive source | Source/custody | raw corpus, source manifests | read-only |
| Private Drive index | Documentary routing | exact provider refs, ACL evidence, route map, gaps | append/supersede derived |
| RafGitTools public repo | Implementation | code, tests, schemas, public-safe docs | branch + PR |
| GitHub Actions | Execution evidence | exact-head CI results/artifacts | workflow-controlled |
| Public docs | Publication | redacted reproducible method and logical routes | branch + PR |

## Navigation contract

A human or agent should be able to answer, in order:

```text
INTENT
→ CURRENT_STATE
→ SOURCE
→ AUTHORITY
→ ACCESS_CLASS
→ ROUTE
→ EXECUTION_TARGET
→ EVIDENCE_RULE
→ ACT
→ RECEIPT
→ R3
```

If any of `SOURCE`, `AUTHORITY`, `ACCESS_CLASS`, `EXECUTION_TARGET`, or `EVIDENCE_RULE` is unresolved, stop at the typed gap instead of guessing.

## No-friction naming

Use stable role names rather than provider-specific identifiers in public navigation:

- `DRIVE_SOURCE_NOVOEXPORT`
- `DRIVE_CORPUS_CUSTODY`
- `CORPUS_CONVERSATIONS_000_050`
- `DRIVE_PRIVATE_INDEX`
- `GITHUB_RAFGITTOOLS_EXECUTOR`
- `DRIVE_EVIDENCE`
- `DRIVE_RECEIPTS`
- `DRIVE_GAPS`

Provider IDs may be resolved from the private Master Index when authority permits.

## Current evidence boundary

The current connected Drive readback observed NOVOexport and its corpus-custody surface as not shared/owner-only. That observation belongs in the private index rather than being copied into public source as provider identifiers.

The historical index currently leaves the provider identity for `conversations-018.json` unresolved. Therefore `018` remains a typed gap until a current provider lookup proves its physical identity.

## F_next

1. run exact-head CI for this branch;
2. physically select the Drive custody tree through Android SAF;
3. verify the generated coverage reports `51/51` or preserves the exact missing member;
4. emit a provider-bound receipt in the private Drive evidence surface;
5. only then promote implementation state from `IMPLEMENTED_UNTESTED` to a tested state.
