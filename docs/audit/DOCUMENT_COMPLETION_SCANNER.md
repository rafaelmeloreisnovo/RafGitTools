# Document Completion Scanner

The scanner implements the first non-destructive stage of document completion.
It **does not merge or edit** the canonical document. It produces a review-only
manifest with hashes, classifications, duplicate records, quarantine decisions
and unresolved provenance fields.

## Invariant

```text
scan != merge
candidate != accepted content
unknown != success
```

Every generated manifest keeps:

```json
"claim_allowed": false
```

until a human-reviewed patch maps candidates to exact target sections and
resolves provenance, authorship, licensing and factual conflicts.

## Usage

```bash
python3 tools/document_completion_scan.py \
  --canonical docs/PROJECT_OVERVIEW.md \
  --source-root fazer \
  --source-root Livro \
  --source-root _incoming \
  --repository rafaelmeloreisnovo/RafGitTools \
  --ref codex/close-runtime-gaps-phase-1 \
  --repository-root . \
  --output reports/document-completion/project-overview.scan.json
```

Missing roots are ignored, so the same command can be used across checkouts
where only some loose-file areas exist. Existing files are read only; the output
is written atomically through a temporary file followed by `os.replace`.

## Initial classifications

| Classification | Initial decision | Meaning |
|---|---|---|
| `CODE` | `REFERENCE_ONLY` | executable material; never pasted into documentation automatically |
| `DOCUMENTATION` | `REFERENCE_ONLY` | candidate text requiring section mapping |
| `SPECIFICATION` | `REFERENCE_ONLY` | structured contract requiring schema/version review |
| `EVIDENCE` | `REFERENCE_ONLY` | evidence pointer, not automatically a claim |
| `GENERATED` | `QUARANTINE` | derived output; source and generator must be identified |
| `HISTORICAL` | `QUARANTINE` | legacy/draft material, including `fazer/` and `Livro/` |
| `UNKNOWN` | `QUARANTINE` | object type not recognized |
| `DUPLICATE` | `REJECT_DUPLICATE` | byte-identical SHA-256 already observed |

## Test command

```bash
python3 -m unittest \
  tests.test_document_completion_scan \
  tests.test_browser_capability_claims
```

## Next boundary

The next phase may create a proposed patch, but only after adding:

1. heading/section extraction;
2. semantic candidate ranking;
3. exact source-fragment references;
4. conflict ledger entries;
5. provenance and license resolution;
6. a generated diff that remains separate from the canonical file until review.
