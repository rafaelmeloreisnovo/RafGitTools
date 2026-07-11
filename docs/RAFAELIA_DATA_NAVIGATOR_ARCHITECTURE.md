# RAFAELIA Data Navigator

## Purpose

Local-first navigation and indexing for large Google Drive corpora, ChatGPT exports, ZIP/TGZ archives, Git repositories and scientific files. UI model: tree + file table + inspector, inspired by Clipper/DOS Shell.

## Roles

- RafGitTools: Android UI, Drive/GitHub authentication, BrowserRaf TLS/HTTP, governance gate, selection and preview.
- RafPolimata: freestanding parsers, normalization, compact indexes, hashing and validation.
- Termux RAFCODEΦ: local runtime, cache, workers, sockets and offline resume.
- Google Drive: immutable sources, manifests, published segments and reports; never credentials.

## First vertical

```text
selected ZIP
  -> fingerprint
  -> ZIP/ZIP64 audit
  -> locate conversations.json
  -> streaming parse
  -> normalize conversations/messages
  -> content-addressed deduplication
  -> temporal evidence ledger
  -> compact index segments
  -> navigator UI
```

## Temporal rule

Preserve all clock domains:

```text
filename time | ZIP entry time | JSON claimed time | Drive created/modified
Git time | ingest time | structural order
```

Never silently replace conflicting timestamps. Emit a contradiction group and confidence.

## Security

OAuth local, minimum scopes, Android Keystore, resumable transfer, certificate and hostname validation, no secrets in logs, dry-run and rollback journal for destructive operations.

## States

`VERIFIED`, `DECLARED_BY_AUTHOR`, `TOKEN_VAZIO`, `CONTRADICTION`.

## Completion gate

The first vertical is complete only after streaming ingestion, interruption resume, overlap detection across exports, bounded memory, malformed/zip-bomb corpus tests, ARM32/ARM64 CI and deterministic manifests.
