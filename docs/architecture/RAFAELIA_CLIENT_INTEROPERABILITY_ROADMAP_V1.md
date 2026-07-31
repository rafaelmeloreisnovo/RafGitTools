# RAFAELIA Client Interoperability Roadmap V1

```yaml
schema: rafaelia_client_interop_roadmap_v1
status: IMPLEMENTED_PARTIAL
branch: feat/raffaelia-client-interop-v1
claim_allowed: false
mode: CONSENT_FIRST_NON_AUTONOMOUS
canonical_runtime_owner: rafaelmeloreisnovo/RafGitTools
canonical_cross_source_owner: rafaelmeloreisnovo/Mapa
source_method: RAFAELIA_Implementacao_Latentes_e_Papers_Drive_GitHub_V1
```

## 0. Objective

Create one lightweight, lawful and auditable client architecture that can expose multiple user interfaces without duplicating security or operational logic:

```text
Tampermonkey userscript
Kiwi / Chromium WebExtension
loopback web UI / PWA
Android APK / share sheet
Termux CLI
        ↓
raf.client.envelope.v1
        ↓
Raf Bridge policy gate
        ↓
typed local capabilities
        ↓
local model / GitHub / Drive / validators
```

The design does not rely on hidden automation, DOM scraping, credential extraction, contract bypass, access-control circumvention or undocumented privileged endpoints.

The legal-engineering principle inspired by software-interoperability doctrine is narrow:

```text
compatible interface != copied proprietary implementation
interoperability != unauthorized access
public protocol != permission to collect everything
client capability != user consent
```

## 1. Vocabulary correction

These layers must not be merged:

| Layer | Meaning |
|---|---|
| Front-end | Human interaction surface: popup, page panel, PWA, APK screen or CLI prompt |
| Client | Endpoint that initiates a protocol request; it may be visual or headless |
| Contract | Typed request/response and capability rules |
| Transport | HTTP, Android Intent, Unix-domain socket or another byte channel |
| Network | TCP/IP, loopback, routing and link infrastructure |
| Adapter | Translation between the canonical contract and GitHub, Drive, model or local runtime |
| Executor | Component allowed to create an external effect after authorization |

A browser front-end may be a client, but TCP/IP is not a front-end. Keeping this separation permits rapid replacement of UI without rewriting governance or execution.

## 2. Existing foundation observed

RafGitTools already contains:

- a foreground Android loopback service on `127.0.0.1:8765`;
- `GET /health` and `POST /v1/chat`;
- a local pairing token compared in constant time;
- per-message consent;
- intent and data classification;
- credential-pattern rejection;
- message-size limits;
- a Kiwi Manifest V3 extension using `activeTab` and selected text;
- a local llama.cpp-compatible model adapter;
- the typed `raf.job.v1` runtime handoff contract;
- multi-provider Git hosting adapters;
- GitHub OAuth/PAT and bounded local operations;
- explicit separation between conversation and command execution.

Observed status remains `IMPLEMENTED / LOCAL DEVICE VALIDATION PENDING`. No APK build, device smoke, Tampermonkey execution or end-to-end runtime result is promoted to PASS by this document.

## 3. Changes introduced in this branch

### 3.1 Canonical client envelope

Created:

```text
contracts/raf-client-envelope-v1.schema.json
```

The envelope fixes these invariants:

- one `request_id` per request;
- only `action=chat` in the current bridge;
- explicit intent;
- explicit consent and consent timestamp;
- data class;
- declared source and transport;
- message-size boundary;
- declared retention request;
- optional capability request list;
- page metadata excluded by default.

### 3.2 Tampermonkey client

Created:

```text
userscripts/raf-client.user.js
```

Properties:

- only runs on explicitly listed domains;
- no automatic network request;
- no automatic DOM extraction;
- reads only the selected text after a user menu action;
- also permits manually typed text;
- asks intent, data class and consent for every send;
- sends only to `http://127.0.0.1:8765`;
- does not send URL, page title, cookies or page metadata;
- stores only the local pairing token in userscript storage;
- requests only `LOCAL_CHAT` capability.

### 3.3 APK gate compatibility

Updated:

```text
app/src/main/java/com/rafgittools/bridge/RafBridgeContract.java
```

The gate now accepts:

```text
kiwi-extension
tampermonkey-userscript
```

It preserves compatibility with the existing Kiwi v0.1 client while accepting `schema=raf.client.envelope.v1` from the new client.

## 4. Target architecture

```text
┌──────────────────────────────────────────────────────────────┐
│ L0 — USER SURFACES                                           │
│ Tampermonkey | WebExtension | Loopback Web UI | APK | CLI    │
└──────────────────────────────┬───────────────────────────────┘
                               │ raf.client.envelope.v1
┌──────────────────────────────▼───────────────────────────────┐
│ L1 — CONSENT AND POLICY GATE                                 │
│ source allowlist | token | intent | class | size | secrets   │
│ default effect: conversation only                            │
└──────────────────────────────┬───────────────────────────────┘
                               │ capability request
┌──────────────────────────────▼───────────────────────────────┐
│ L2 — CAPABILITY BROKER                                       │
│ LOCAL_CHAT | READ_SELECTION | READ_GITHUB | READ_DRIVE       │
│ VALIDATE_ARTIFACT | future typed write capabilities          │
└──────────────────────────────┬───────────────────────────────┘
                               │ typed adapter call
┌──────────────────────────────▼───────────────────────────────┐
│ L3 — ADAPTERS                                                │
│ llama.cpp | GitHub | Drive | RafPolimata | Termux | Mapa     │
└──────────────────────────────┬───────────────────────────────┘
                               │ result + receipt
┌──────────────────────────────▼───────────────────────────────┐
│ L4 — PROVENANCE                                              │
│ request ID | source ref | version | content hash | result    │
│ retention state | external effects | evidence state          │
└──────────────────────────────────────────────────────────────┘
```

## 5. Repository allocation

| Repository/system | Canonical responsibility | Must not become |
|---|---|---|
| `RafGitTools` | APK, clients, policy gate, capability broker and Git/provider adapters | global knowledge authority |
| `Mapa` | cross-source records, contracts, relations, provenance and evidence states | runtime executor |
| `termux-app-rafacodephi` | authorized local runtime, IPC boundary and execution receipts | invisible browser scraper |
| `RafPolimata` | segmentation, classification, indexing and retrieval over authorized corpus | silent writer to source systems |
| `Vectras-VM-Android` | isolated validation, reproducible environments and compatibility tests | default interactive client |
| Google Drive | original corpus, documentary evidence and editorial derivatives | transactional event bus |
| GitHub | versioned implementation, reviews, diffs, releases and technical evidence | complete personal-memory store |

The anti-split-brain rule is mandatory: each object has one canonical owner; mirrors and indices only point to it.

## 6. Roadmap ordered by impact × development speed

### P0 — Unified envelope and Tampermonkey MVP

**State:** implemented on branch; runtime unverified.

**Impact:** very high.  
**Development cost:** very low.

Deliverables:

- canonical client schema;
- explicit-source gate;
- manual-selection userscript;
- no external effects;
- no page metadata by default.

Acceptance gate:

1. static review;
2. APK compile;
3. userscript install;
4. valid health request;
5. valid chat;
6. invalid token rejection;
7. missing consent rejection;
8. source rejection;
9. credential rejection;
10. recorded APK/script hashes.

### P1 — Same-origin loopback web UI

**Impact:** very high.  
**Development cost:** low.

Serve a tiny static interface directly from the APK bridge:

```text
GET http://127.0.0.1:8765/ui/
```

Benefits:

- deploy without extension packaging;
- one UI works in multiple browsers;
- same-origin request removes broad CORS dependence;
- easier accessibility and mobile layout;
- installable as local PWA where browser behavior permits.

Rules:

- no remote assets;
- strict CSP;
- no analytics;
- no service-worker persistence until explicitly designed;
- token remains local;
- each send still requires consent.

### P2 — Receipt v1 and local provenance

**Impact:** very high.  
**Development cost:** low to medium.

Add a response receipt containing:

```yaml
schema: raf.client.receipt.v1
request_id: ...
source: ...
received_at: ...
message_sha256: ...
message_retained: false
external_effects: []
model_endpoint_class: local
result_sha256: ...
evidence_state: VERIFIED_LIMITED
```

The receipt stores hashes and bounded state, not raw message content, unless the user explicitly requests persistence.

### P3 — Harden the loopback boundary

**Impact:** high.  
**Development cost:** medium.

Current implementation returns `Access-Control-Allow-Origin: *`. Loopback plus a strong token reduces exposure but is not the final boundary.

Hardening sequence:

1. issue a short-lived session nonce from the APK;
2. require token + nonce + request timestamp;
3. reject replayed request IDs;
4. rate-limit authentication failures;
5. use a strict origin policy where the client surface provides a stable origin;
6. rotate the pairing token;
7. emit no secret or content in Android logs;
8. consider Unix-domain socket for APK ↔ Termux, leaving HTTP only at the browser edge.

### P4 — Read-only GitHub and Drive capabilities

**Impact:** high.  
**Development cost:** medium.

Introduce explicit read capabilities:

```text
READ_GITHUB_PUBLIC
READ_GITHUB_AUTHORIZED
READ_DRIVE_AUTHORIZED
VALIDATE_ARTIFACT
```

Every adapter call must contain:

- source locator;
- immutable version where available;
- authorization ID;
- maximum bytes;
- timeout;
- purpose;
- output retention policy.

No browser userscript receives OAuth tokens. Authentication remains inside the APK/provider adapter or the authorized connector/runtime.

### P5 — Android share sheet and SAF

**Impact:** high.  
**Development cost:** medium.

Add a dedicated Android share target:

```text
selected text / shared file
→ RafGitTools share activity
→ preview
→ classification
→ consent
→ local bridge
```

For files, use Android Storage Access Framework grants instead of broad filesystem access. Hash the selected file locally and preserve the original URI grant scope.

### P6 — Typed execution through `raf.job.v1`

**Impact:** very high.  
**Development cost:** medium to high.

Conversation must never directly become shell execution.

```text
conversation
→ proposed capability
→ visible job preview
→ explicit authorization
→ raf.job.v1
→ GovernanceGate
→ Termux authorized runtime
→ structured result
→ receipt
```

Initial effects remain read-only. Git write, Drive write, send, delete, purchase and remote execution require separate schemas and stronger confirmations.

### P7 — RafPolimata semantic indexing

**Impact:** medium to high.  
**Development cost:** medium.

Authorized GitHub/Drive content can be transformed into typed carriers:

```text
source ref
→ immutable version
→ chunk/carrier
→ semantic index
→ relation in Mapa
```

The index stores source pointers, hashes and state. It does not silently replace the canonical source.

### P8 — Vectras validation matrix and release

**Impact:** medium.  
**Development cost:** high.

Validate:

- ARMv7 and ARM64;
- Android 10, 14 and 15 targets where available;
- service restart;
- low-memory behavior;
- malformed HTTP;
- replay attempts;
- extension/userscript compatibility;
- offline operation;
- large selection boundaries;
- accessibility and keyboard navigation.

Only after these gates should the feature be promoted from `IMPLEMENTED_PARTIAL` to `VERIFIED_LIMITED`.

## 7. Fastest deploy path

```text
1. checkout feat/raffaelia-client-interop-v1
2. compile RafGitTools APK
3. install/open Raf Bridge
4. copy local pairing token
5. import userscripts/raf-client.user.js into Tampermonkey
6. open an allowed site
7. select text
8. run “RAFAELIA: enviar seleção”
9. confirm intent, class and consent
10. record output and hashes
```

Until this sequence runs on the target device, deployment state is `TOKEN_VAZIO`.

## 8. Contract-light engineering

“Light contract” means few stable concepts, not absence of boundaries:

```text
one envelope
one policy gate
one capability vocabulary
one receipt format
many replaceable front-ends
many bounded adapters
```

The client should remain aggressive in development speed and conservative in authority.

```text
fast UI replacement
+ stable contract
+ explicit capability
+ minimal permission
+ reversible effect
= sustainable velocity
```

## 9. Legal and ethical boundary

This architecture supports compatibility with documented/public interfaces and user-authorized data access. It does not claim that the Oracle v. Google decision creates a universal right to access private systems, bypass controls or disregard terms.

Operational rules:

- use official authentication where required;
- honor rate limits and access controls;
- do not imitate private credentials;
- do not bypass CAPTCHA, paywall or technical restrictions;
- do not collect unrelated page content;
- do not persist data merely because it was observable;
- distinguish public data, authorized private data and restricted data;
- preserve authorship, source and version;
- keep external effects off by default.

## 10. State summary

```yaml
F_ok:
  - existing local bridge and Kiwi client observed
  - canonical client envelope created
  - consent-first Tampermonkey client created
  - APK source gate generalized without enabling execution
F_gap:
  - Android build not executed
  - target-device smoke not executed
  - userscript not executed in Tampermonkey
  - receipt and replay protection not implemented
  - GitHub/Drive client capabilities not connected to this envelope
F_next:
  - compile and run P0 acceptance matrix
```
