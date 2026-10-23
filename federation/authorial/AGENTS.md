# AGENTS.md — Authorial federation source archive

Scope: `federation/authorial/**`.

This subtree is a **provenance-preserving source archive**, not native RafGitTools implementation.

Before touching a file here:

1. Read `../../configs/authorial-federation.v1.json`.
2. Read `../../docs/federation/AUTHORIAL_FEDERATION_V1.md`.
3. Read the source-specific provenance and license beside the imported files.
4. Resolve exact source repository/ref/path/blob.
5. If authorship, license, authority or destination role is missing, stop with `TOKEN_VAZIO`.

Hard invariants:

```text
REPO_OWNER != AUTHOR
HEADER_ADDED != SOLE_AUTHORSHIP
IMPORTED != INTEGRATED != TESTED != PASS
SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM
```

Do not edit imported snapshots to erase origin or make them look native. If a module is useful, create a separate RafGitTools adapter/implementation outside this archive and preserve the governing license and attribution.

Current PCR boundary:

- `pcr/native/rafaelia_audit.rs` and `rafaelia_telemetry.rs` are archived because the source repository's authorship analysis identifies them as RAFAELIA additions by Rafael.
- The PCR repository remains a Magisk derivative; the archive does not transfer authorship of Magisk or third-party code.
- The imported Rust sources still assume Magisk/base runtime surfaces; archive presence is not Android/app integration or runtime evidence.

Promotion requires a separate adapter, test/build evidence and successor receipt.