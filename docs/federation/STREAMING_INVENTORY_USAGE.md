# Streaming inventory usage

Status: `IMPLEMENTED_LOCAL / TARGET_CORPUS_NOT_SCANNED`.

The scanner reads file metadata and SHA-256 in bounded chunks. It does not extract archives, follow symbolic links or copy payloads into the report.

```bash
python3 scripts/federation/streaming_inventory.py \
  --root /path/to/custody/root \
  --chunk-size 1048576 \
  --output artifacts/inventory.json \
  --checkpoint artifacts/inventory.checkpoint.json
```

For a bounded trial:

```bash
python3 scripts/federation/streaming_inventory.py \
  --root /path/to/custody/root \
  --max-files 100 \
  --output artifacts/inventory-first-100.json \
  --checkpoint artifacts/inventory-first-100.checkpoint.json
```

## Guarantees in the implemented scope

- deterministic path ordering;
- SHA-256 calculated by streaming chunks;
- archive candidates marked by suffix;
- no archive extraction;
- no symlink traversal;
- no file payload embedded in output;
- optional bounded file count;
- checkpoint with record count and last path.

## Not yet implemented

- resumable continuation after the last path;
- archive-member inventory without extraction;
- secret scanning and redacted text indexing;
- semantic linking to exact session input and response;
- Drive API traversal;
- database table inventory;
- full multi-gigabyte corpus execution.

These remain separate gaps and must not be inferred from the existence of the scanner.
