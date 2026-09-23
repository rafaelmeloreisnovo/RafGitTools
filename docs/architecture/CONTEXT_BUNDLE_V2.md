# ContextBundle V2 — compatibility and workbench boundary

State: `IMPLEMENTED_SOURCE / TEST_PENDING`  
Scope: contract + dependency-free adapter + unit tests.  
claim_allowed: `false`

## Why V2 exists

Three incompatible V1 contracts were observed under the same logical schema id:

- RafGitTools: `chunks + created_at`
- llamaRafaelia: `conversation_chunks + generated_at`
- CONVERSATIONS_CHUNKS_PRIVATE: `chunk_refs + intent_candidates`

Historical V1 files remain untouched.

## V2 rule

```text
reference-first
+ optional bounded materialized segments
+ explicit privacy
+ provenance resources
+ compatibility metadata
+ TOKEN_VAZIO for unknown required knowledge
```

A V2 bundle is context transport, not a permission envelope.

```text
ContextBundle != raw corpus
ContextBundle != model weights
ContextBundle != execution permission
ContextBundle != Git write approval
```

## Canonical files

- `contracts/context-bundle-v2.schema.json`
- `scripts/context_bundle_v2.py`
- `tests/test_context_bundle_v2.py`
- `examples/context-bundle-v2/native.example.json`

## Compatibility adapters

The adapter recognizes exactly three V1 shapes:

```text
rafgittools.docs-v1
llamarafaelia.docs-v1
conversation-chunks-private.docs-v1
```

Ambiguous input fails closed.

Missing source data is never synthesized. For example, the private V1 has no required timestamp, so V2 receives:

```text
created_at = TOKEN_VAZIO
```

and the gap is recorded in `compatibility.unresolved_fields`.

Unknown V1 key **names** are listed in `unmapped_keys`; their values are not blindly copied.

## Materialization

`chunk_refs` may identify selected source material without copying bodies.

When text must actually be supplied to a local model, `segments[]` can carry bounded materialized text, its source ref, privacy class and SHA-256/TOKEN_VAZIO.

Materialization is therefore explicit:

```text
ResourceRef
→ selected chunk ref
→ bounded resolution
→ segment
→ local model
```

## Commands

```bash
python3 scripts/context_bundle_v2.py validate \
  examples/context-bundle-v2/native.example.json

python3 scripts/context_bundle_v2.py adapt old-v1.json new-v2.json
```

When V1 shape cannot be uniquely detected, use a reviewed source hint rather than guessing:

```bash
python3 scripts/context_bundle_v2.py adapt old.json new.json \
  --source-hint llamarafaelia.docs-v1
```

## Next integration

The next vertical slice consumes this contract:

```text
FileBrowser
→ WorkspaceTab
→ ContextBroker
→ NOVOexport Provider
→ ContextBundle V2
→ llamaRafaelia
→ source-linked answer
```

Patch/write routing remains a later gate through RafGitFS.
