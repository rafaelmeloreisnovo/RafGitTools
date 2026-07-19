# Ecosystem Runtime Gap Map — Phase 1

**Date:** 2026-07-19  
**Coordinator:** `rafaelmeloreisnovo/RafGitTools`  
**Branch:** `codex/close-runtime-gaps-phase-1`  
**Scope:** RafGitTools, LlamaRafaelia, Vectras VM Android, Termux RAFCODEPhi, QEMU RAFAELIA, AndroidX RmR and Gradle Vectra Orchestrator  
**Policy:** file present != integrated code != executed test != certified runtime

## 1. What this phase delivers

This first phase does not pretend to finish seven large repositories in one undocumented change. It creates the executable control map required to close gaps without losing provenance:

1. exact repository refs and declared roles;
2. quantified capability states;
3. explicit producer/consumer edges;
4. fail-closed evidence gates;
5. machine-readable schemas;
6. a standard-library validator;
7. tests that reject silent status inflation;
8. a non-destructive path for completing documents from loose files.

Canonical artifacts:

```text
configs/runtime-gap-map.phase1.json
configs/ecosystem-runtime-map.phase1.json
schemas/runtime-gap-map.schema.json
schemas/ecosystem-runtime-map.schema.json
schemas/document-completion.schema.json
tools/validate_ecosystem_runtime_map.py
tools/document_completion_scan.py
tests/test_ecosystem_runtime_map.py
tests/test_document_completion_scan.py
```

## 2. Quantified state

### Repositories

| State | Count |
|---|---:|
| PASS | 0 |
| PARTIAL | 5 |
| DESIGN | 0 |
| TOKEN_VAZIO | 0 |
| BLOCKED | 2 |
| QUARANTINE | 0 |
| **Total** | **7** |

`BLOCKED` repositories in this cut:

- `Vectras-VM-Android`: current observed commit lacks a canonical build/runtime validation; official state remains `BETA_BLOCKED`.
- `termux-app-rafacodephi`: project APK build exists, but real package/runtime promotion is blocked by `LEGACY_PREFIX_BINARY_RISK` inside upstream ELF payloads.

### Capabilities

| State | Count |
|---|---:|
| PASS | 1 |
| PARTIAL | 7 |
| DESIGN | 1 |
| TOKEN_VAZIO | 2 |
| BLOCKED | 2 |
| QUARANTINE | 0 |
| **Total** | **13** |

The single `PASS` is deliberately narrow:

```text
apk_project_build = PASS
```

It means the Termux RAFCODEPhi project has a proved APK build contract. It does **not** mean that every routed programming language has a complete frontend, DEX writer, resource compiler, signer and ART execution proof.

## 3. Repository roles and current state

| Repository | Operational role | State | Primary gap |
|---|---|---|---|
| RafGitTools | Android control plane, Git/GitHub governance, BrowserRaf and document coordinator | PARTIAL | control-to-Termux job transport and Browser TLS are not proved end to end |
| LlamaRafaelia | bounded semantic interpreter over provenance segments | PARTIAL | complete segment producer/consumer runtime evidence is absent |
| Vectras-VM-Android | Android VM application and QEMU artifact consumer | BLOCKED | current commit has no canonical build/device evidence |
| termux-app-rafacodephi | local execution plane, package/bootstrap runtime and APK build host | BLOCKED | real prefix-safe ELF dependency closure is not promoted |
| qemu_rafaelia | QEMU system-binary artifact producer and VM engine | PARTIAL | workflow is wired; green artifact plus Android guest boot evidence is absent |
| androidx_RmR | isolated native/matrix AndroidX extension | PARTIAL | current ABI, benchmark and consumer-app evidence is incomplete |
| gradle | isolated Vectra build orchestration and backend selection | PARTIAL | ASM exists for x86_64 only; AArch64 falls back to C/Java |

### Quantification note

`repository_size_kib` in the JSON map is the repository size returned by the GitHub repository API. It is **not** source lines of code, executable coverage, binary size or functional maturity. It must never be used as a completion percentage.

## 4. BrowserRaf and TLS truth boundary

### What exists

- ASM/C browser bootstrap sources;
- a TLS 1.3-style ClientHello builder;
- TLS record-header parsing;
- protocol constants and state transitions.

### What does not yet exist as proved runtime

- cryptographically secure ClientHello entropy;
- X25519 key exchange;
- HKDF key schedule;
- transcript hashing;
- certificate-chain validation;
- hostname validation;
- Finished verification;
- AEAD record protection;
- complete TLS 1.2 profile;
- external certification.

The current fixed-seed LFSR in `TLS_INIT` makes security enablement fail closed:

```text
TLS_READY = false
HTTPS_READY = false
certified = false
claim_allowed = false
```

The next code patch must replace the deterministic seed path with a bounded `getrandom` provider and refuse network activation if secure entropy cannot be obtained.

## 5. APK, DEX and ELF separation

The audit keeps four different statements separate:

```text
Android project builds an APK
!=
source language is compiled
!=
valid DEX is emitted and executed by ART
!=
valid target ELF is emitted and loaded
```

### APK

- Project APK build: `PASS` under the narrow Termux build contract.
- Arbitrary multi-language source-to-APK compiler: `PARTIAL`.
- Deterministic package/install/run matrix per language: missing.

### DEX

Current state: `TOKEN_VAZIO` for the joint pipeline.

Release gate:

1. strict malformed-input verifier;
2. overflow-safe section and index validation;
3. signature/checksum policy by supported version;
4. deterministic class/method/code-item writer;
5. `dexdump` round trip;
6. ART install and execution evidence.

### ELF

Current state: `BLOCKED` for the ecosystem pipeline.

There are two independent blockers:

1. custom strict parser/emitter and ARMv7/AArch64 runtime matrix are incomplete;
2. Termux real payload contains binaries tied to the legacy Termux prefix.

Release gate:

```text
strict verifier
AND declared ELF scope
AND prefix-safe dependency closure
AND reproducible ELF32/ELF64 output
AND readelf validation
AND target execution
```

## 6. QEMU to Vectras contract

The QEMU repository now wires a multi-target producer job and package-contract scripts for:

- `qemu-system-x86_64`;
- `qemu-system-aarch64`;
- `qemu-system-i386`;
- `qemu-exec.json`;
- `BUILD_INFO.json`;
- `SHA256SUMS.txt`.

That closes a wiring gap, not the entire runtime chain. The edge remains `PARTIAL` until all of the following are attached:

1. green producer job;
2. downloadable artifact;
3. contract checker PASS;
4. hash verification by the consumer;
5. Vectras current-commit build;
6. APK install and launch;
7. minimal guest boot log.

## 7. AndroidX RmR and Gradle boundary

### AndroidX RmR

The RmR extension has a real isolated module with Java, CMake, C++ and Android tests. However, architectural existence and historical benchmark claims are not runtime proof for the current commit.

Required evidence:

- isolated module build;
- ABI matrix;
- reproducible benchmark JSON;
- exact device/toolchain metadata;
- minimal consumer application;
- license boundary between upstream AndroidX and RmR additions.

### Gradle Vectra Orchestrator

The Gradle fork preserves a valuable isolation rule:

```text
org.gradle.vectra.* is opt-in
and does not redefine Gradle core defaults
```

Current native backend matrix:

| Host | ASM | C | Java fallback |
|---|---|---|---|
| x86_64 | eligible | eligible | yes |
| AArch64 | not implemented in this module | eligible | yes |
| other | no | conditional/none | mandatory |

Therefore, `ASM everywhere` is not an allowed claim. AArch64 ASM remains `TOKEN_VAZIO` until implementation and ABI tests exist.

## 8. LlamaRafaelia boundary

LlamaRafaelia is not authorized to ingest raw repositories, ZIPs, databases or huge JSON sources directly. The canonical path is:

```text
raw source
-> deterministic parser
-> provenance-preserving bounded segment
-> semantic interpretation
-> anchored candidate relation
```

The model may propose relations and uncertainty. It may not:

- alter original files;
- overwrite timestamps;
- merge duplicates without provenance;
- execute filesystem or network actions;
- convert semantic similarity into proof of identity.

This is why document completion remains in RafGitTools/RafPolimata governance rather than being delegated to an unconstrained model prompt.

## 9. Loose-file document completion

The implemented scanner is intentionally non-destructive:

```text
scan
-> hash
-> classify
-> identify byte duplicates
-> quarantine historical/generated/unknown material
-> emit review manifest
```

It does not yet perform:

- semantic section matching;
- Git author/commit/license recovery;
- factual conflict resolution;
- automatic canonical patch generation.

The next proof must select one canonical document and execute:

```text
DRY_RUN
-> reviewed target-section mapping
-> conflict ledger
-> proposed patch
-> human review
-> commit
```

No source file may be silently concatenated, deleted or overwritten.

## 10. Cross-repository edges

| Producer | Consumer | Contract | State |
|---|---|---|---|
| RafGitTools | Termux RAFCODEPhi | signed/hashed `raf.job.v1` | PARTIAL |
| Termux RAFCODEPhi | QEMU RAFAELIA | local build/install runtime | PARTIAL |
| QEMU RAFAELIA | Vectras VM Android | packaged QEMU artifact contract | PARTIAL |
| Gradle | Vectras VM Android | isolated Vectra orchestration | PARTIAL |
| Gradle | AndroidX RmR | isolated extension build | PARTIAL |
| AndroidX RmR | Vectras VM Android | Android compatibility/runtime consumption | TOKEN_VAZIO |
| RafPolimata | LlamaRafaelia | bounded provenance segment bundle | PARTIAL |

No single trace currently proves the entire graph. Therefore:

```text
cross_repo_e2e = TOKEN_VAZIO
```

## 11. First execution order

The next work should follow dependency and risk order, not repository size:

### P0 — Security and truthful network boundary

1. replace deterministic BrowserRaf TLS entropy;
2. add fail-closed capability flags;
3. keep TLS 1.2/1.3 certification claims disabled.

### P0 — ELF prefix and ABI closure

1. inventory every promoted ELF dependency;
2. rebuild for the RAFCODEPhi prefix;
3. reject legacy-prefix strings and incompatible interpreters;
4. execute ARM32/ARM64 device smoke.

### P1 — QEMU producer to Vectras consumer

1. obtain green artifact contract evidence;
2. verify hashes in the consumer;
3. run current Vectras canonical build;
4. install, launch and boot a minimal guest.

### P1 — DEX strict verifier and writer

1. harden parser with malformed corpus;
2. implement minimal deterministic writer;
3. verify with Android tools and ART.

### P2 — Document completion proof

1. scan one canonical document;
2. resolve provenance and conflicts;
3. generate one review-only patch.

### P2 — Full transaction trace

Record one content-addressed operation across:

```text
RafGitTools
-> Termux
-> RafPolimata
-> LlamaRafaelia
-> QEMU/Vectras when VM execution is requested
```

Required fields:

- actor and authorization;
- source and output hashes;
- exact refs;
- commands and environment;
- typed result;
- rollback state;
- device evidence.

## 12. Validation commands

```bash
python3 tools/validate_ecosystem_runtime_map.py \
  configs/ecosystem-runtime-map.phase1.json

python3 -m unittest \
  tests.test_ecosystem_runtime_map \
  tests.test_document_completion_scan
```

The repository tests are the intended gate. Until a runner result is attached to the current PR head, their execution state remains `TOKEN_VAZIO`; file presence alone is not a test result.

## 13. Phase-1 verdict

```text
MAP_CREATED = true
SCHEMA_CREATED = true
VALIDATOR_CREATED = true
NEGATIVE_TESTS_CREATED = true
IMPLEMENTATION_COMPLETE = false
CROSS_REPO_RUNTIME_PROVED = false
CLAIM_ALLOWED = false
```

This is the correct first step: the ecosystem now has a typed map showing exactly where code exists, where evidence exists, where a dependency is blocked and which smallest executable change comes next.
