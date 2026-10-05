# RAFAELIA Condominium Data Governance V1

Status: CANONICAL_DRAFT
Scope: RafGitTools orchestration and cross-repository custody

## Purpose

This document defines a condominium-style governance boundary for the RAFAELIA ecosystem. A condominium is a shared operational structure where each repository keeps its own authority, while shared custody, provenance, privacy, and release gates are coordinated through explicit routes.

This document is governance, not a license change, not a claim of scientific validation, and not permission to publish private material.

## Core Rule

SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM.

TOKEN_VAZIO is valid whenever a source, authority, execution target, or evidence rule is absent.

## Repository Roles

| Surface | Role | Authority |
| --- | --- | --- |
| RafGitTools | orchestration, custody, receipts, cross-repo control | operational governance |
| Rafaelia_Private | private memory, authorship, personal/sacred artifacts, protected corpus | private authority |
| BLAKE3/rmr | public fork material, code, contracts, reproducible technical artifacts | public repository boundary |
| Est-dio-de-udio | private audio/app implementation and CI contracts | producer repository |
| Google Drive | memory, ledgers, indexes, receipts, source references | documentary authority |

## Privacy Classes

| Class | Meaning | Default action |
| --- | --- | --- |
| PUBLIC_CODE | code intended for public repository use | may be open source with explicit license |
| PUBLIC_DOC | public documentation without private data | may be published after review |
| PRIVATE_CODE | implementation not intended for public release | keep private |
| PRIVATE_DOC | private planning, memory, receipts, or personal notes | keep private |
| SACRED_PERSONAL | faith, belief, spiritual practice, temple/private identity artifacts | private by default; never publish by inference |
| SECRET | token, credential, key, auth material, private endpoint | block, rotate if exposed |
| TOKEN_VAZIO | classification not yet known | block promotion |

## Sacred Personal Boundary

Faith, belief, spiritual authorship, temple language, and personal sacred artifacts are private by default. They may be indexed as private provenance when necessary, but the content itself must not be copied into public repositories or open-source releases unless there is an explicit human authorization naming the exact artifact and target.

Respect for belief is part of data governance: the system may preserve provenance without converting belief into public evidence, license text, scientific claim, or executable requirement.

## Zero-Trust Handling

- Treat every cross-repo movement as untrusted until source, authority, and target are known.
- Never move secrets or sacred personal content into public repositories.
- Never infer permission from existence in a private repository.
- Public release requires a privacy-class check and a license check.
- Private material can be referenced by opaque ID, hash, or receipt without copying content.
- Claims require evidence; documentation alone is not execution.

## Promotion Gate

A file or directory may be promoted across repositories only when all fields are known:

```yaml
promotion_gate:
  source_repo: ""
  source_path: ""
  source_ref: ""
  target_repo: ""
  target_path: ""
  privacy_class: TOKEN_VAZIO
  authority: TOKEN_VAZIO
  license_status: TOKEN_VAZIO
  evidence_rule: TOKEN_VAZIO
  rollback_ref: TOKEN_VAZIO
  claim_allowed: false
```

If any required field is TOKEN_VAZIO, the promotion is blocked.

## Allowed Minimal Movement

Allowed without content exposure:

- route records;
- hashes;
- repository/path/ref identifiers;
- high-level private/public classification;
- receipts that do not include private payloads.

Not allowed without explicit artifact authorization:

- private spiritual text;
- family or personal data;
- secrets or credentials;
- raw private corpus;
- unpublished identity materials;
- private audio/data samples.

## Control

Every material delta should append a receipt with source/ref, parent, kind, summary, evidence, gap, next step, and rollback. Corrections supersede prior records; they do not erase history.
