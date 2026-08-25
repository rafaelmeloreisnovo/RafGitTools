# Runtime Gap Map — Phase 1

**Repository:** `rafaelmeloreisnovo/RafGitTools`  
**Branch:** `codex/close-runtime-gaps-phase-1`  
**Scope:** BrowserRaf, TLS, APK/compiler, ELF, DEX and loose-document ingestion  
**Policy:** file present != integrated code != executed test != certified runtime

## Status vocabulary

- `PASS`: verified by a reproducible test or exact invariant.
- `PARTIAL`: substantive implementation exists, but the end-to-end path is incomplete.
- `DESIGN`: specification or roadmap only.
- `TOKEN_VAZIO`: evidence is missing.
- `BLOCKED`: a required invariant is known to be unsatisfied.
- `QUARANTINE`: artifact must not enter release until provenance/compatibility is resolved.

## Executive matrix

| ID | Domain | Observed artifact | Current state | Blocking gap | First executable action |
|---|---|---|---|---|---|
| BR-001 | Browser bootstrap | `BrowserRaf/internal/br_start.S`, `br_main.c` | PARTIAL | no device/runtime evidence and no full browser conformance suite | build ARM32/ARM64, run smoke test, capture hashes/logs |
| TLS-001 | TLS record/ClientHello | `BrowserRaf/internal/br_tls.h` | PARTIAL | no X25519, HKDF, transcript hash, AEAD, Finished verification, encrypted record layer or certificate validation | replace roadmap mask with real provider boundary and tests |
| TLS-002 | Entropy | deterministic LFSR seed in `TLS_INIT` | BLOCKED | predictable ClientHello random; unsuitable for security | add `getrandom` syscall wrapper and fail closed |
| TLS-003 | TLS 1.2 | constants/wire version present | DESIGN | no complete TLS 1.2 handshake implementation | either implement behind explicit profile or declare unsupported |
| TLS-004 | Certification | documentation language | TOKEN_VAZIO | no external certification, RFC conformance report or interoperability evidence | create conformance manifest; never label certified before evidence |
| DEX-001 | DEX parser | `_incoming/raf_dex.h` | PARTIAL | header-only parser, SHA-1 unverified, weak bounds checking, accepted version range too broad | move into operational module and add strict verifier/fuzz corpus |
| DEX-002 | DEX generation | compiler/factory references | TOKEN_VAZIO | no proved class/method/code-item writer and no ART install test | define writer contract and round-trip tests with `dexdump`/ART |
| ELF-001 | ELF parser | `_incoming/raf_elf.h` | PARTIAL | incoming/quarantine location; integration and architecture coverage unproved | audit parser, validate offsets/overflow and promote into module |
| ELF-002 | ELF emission/link | compiler/factory references | TOKEN_VAZIO | no reproducible ET_EXEC/ET_DYN output matrix and loader evidence | emit minimal ARM32/ARM64 ELF and validate with `readelf` + execution |
| APK-001 | APK build | `tools/termuxforge/forge.sh`, `COMPILER/rafaelia_factory.sh` | PARTIAL | full source-to-DEX/resources/manifest/sign/zipalign/install chain not proved | define APK pipeline contract and produce deterministic unsigned APK |
| APK-002 | Language frontends | factory/router documentation | PARTIAL | routing/profile support may be mistaken for complete compilers | inventory every frontend and attach compile/run vectors per language |
| DOC-001 | Loose files | `_incoming/`, `Livro/`, `fazer/`, root documents | BLOCKED | canonicality, provenance and destination are ambiguous | scan, hash, classify and create completion proposals without overwriting canonical docs |
| DOC-002 | Document completion | requested cross-directory completion | DESIGN | no deterministic merge policy or conflict ledger | implement `document-completion.v1` manifest and dry-run report |

## Critical findings

### TLS is not certified or complete

`br_tls.h` builds a TLS 1.3-style ClientHello and parses record headers, but it does not implement the cryptographic handshake. The source explicitly lists X25519, HKDF, transcript, AEAD, Finished and record encryption as upgrade items. The current deterministic random seed is a release blocker.

Required invariant:

```text
HTTPS_READY =
  CSPRNG
  AND key_exchange
  AND transcript_hash
  AND certificate_chain_validation
  AND hostname_validation
  AND Finished_verification
  AND AEAD_record_protection
  AND interoperability_tests
```

Until every term is proven, `https_runtime = TOKEN_VAZIO` and `certified = false`.

### DEX must be a strict verifier before becoming a writer

The current header recognizes DEX 035–039 and checks Adler-32, but a production verifier must additionally validate:

- exact magic/version allowlist;
- `file_size == mapped_length`;
- header size and endian tag;
- every `(offset, size)` pair with overflow-safe arithmetic;
- map list ordering and consistency;
- string/type/proto/field/method/class index bounds;
- SHA-1 signature when the selected DEX version requires it;
- code item, try/catch and debug-info boundaries;
- rejection of overlapping or aliased regions where forbidden.

### ELF requires parser and emitter evidence

ELF support is not established merely by possessing a header parser. The release gate requires independent evidence for:

- ELF32/ELF64 class;
- ARM/EABI and AArch64 machine identifiers;
- program-header and section-header overflow validation;
- alignment constraints;
- relocation policy;
- entry point containment in an executable segment;
- ET_REL, ET_EXEC and ET_DYN scope declaration;
- reproducible output and execution on target ABI.

### Loose documents need non-destructive completion

No document may be completed by silently concatenating files. Completion must produce a proposal with provenance and conflicts:

```text
scan -> identify -> hash -> classify -> retrieve candidates
     -> compare sections -> propose patch -> validate links/claims
     -> human review -> commit
```

Source material remains unchanged. Canonical documents are updated only through a reviewable patch.

## Phase-1 deliverables

1. `schemas/runtime-gap-map.schema.json`.
2. `schemas/document-completion.schema.json`.
3. `configs/runtime-gap-map.phase1.json`.
4. scanner/validator for loose files.
5. strict DEX/ELF verification tests.
6. TLS provider boundary with fail-closed capability flags.
7. draft PR containing evidence and no unsupported certification claim.

## Claim policy

```yaml
browser_asm:
  implementation: PARTIAL
  runtime_evidence: TOKEN_VAZIO

tls_1_3:
  client_hello: PARTIAL
  cryptographic_handshake: TOKEN_VAZIO
  certified: false

tls_1_2:
  implementation: TOKEN_VAZIO
  certified: false

apk_compiler:
  routing: PARTIAL
  end_to_end_apk: TOKEN_VAZIO

elf:
  parser: PARTIAL
  emitter: TOKEN_VAZIO

dex:
  header_parser: PARTIAL
  strict_verifier: TOKEN_VAZIO
  writer: TOKEN_VAZIO

document_completion:
  specification: DESIGN
  implementation: TOKEN_VAZIO

claim_allowed: false
```
