# COPILOT ASSIGNMENT — PHASE 3

Use this assignment after the Termux runtime service contract exists and its protocol version/API are known.

---

## PROMPT

You are working in `rafaelmeloreisnovo/RafGitTools`.

Your task is to implement **Phase 3: Google Drive read-only connector, resumable source transfer, governed Termux handoff and first Data Navigator screen**.

Before editing, read completely:

1. `.github/copilot-instructions.md`
2. `docs/copilot/COPILOT_CROSS_REPO_EXECUTION_MASTER.md`
3. `docs/copilot/TASK_01_DRIVE_CONNECTOR_AND_TERMUX_HANDOFF.md`
4. `schemas/rafaelia_runtime_job.schema.json`
5. `runtime-lock.json`
6. current authentication, secure storage, networking, offline queue, background sync, DI, navigation, `GovernanceGate`, `ToolRouter` and kernel bridge files
7. the final Termux Phase 2 AIDL/service contract
8. the final RafPolimata Phase 1 artifact/manifest contract

Start by inspecting the current default branch. In the PR description list the exact real files and APIs you found. Reuse existing infrastructure and package conventions. Do not create duplicate security/network/queue systems.

Implement a functional first vertical that allows the user to:

- authenticate a Google Drive account through a browser-based OAuth authorization-code flow with PKCE;
- store tokens through the existing Android Keystore-backed secure storage;
- list/search Drive files and select a real `conversations.json`;
- inspect bounded metadata and revision identity;
- download the source by streaming with resume, cancellation and revision/ETag protection;
- verify final size/content identity according to the cross-repository contract;
- build and validate an immutable `index_conversations` job from the canonical schema;
- run `GovernanceGate` before network/file/service side effects;
- pass the source read-only to the explicit Termux runtime service;
- persist and display job progress, cancellation, completion/failure and allowlisted artifacts;
- show the first tree/table/inspector Data Navigator UI without loading the corpus into memory.

Security and architecture constraints:

- no Google password input;
- no cookie scraping;
- no embedded client secret;
- minimum read-only Drive scope;
- no plaintext tokens or authorization headers in logs;
- no TLS hostname/certificate bypass;
- no whole-response `ByteArray`/`String` for corpus data;
- no direct access to Termux private paths;
- typed/versioned Binder/AIDL only;
- no generic command field;
- source always read-only;
- no mutation of original Drive files;
- no LLM parsing of the raw file;
- no UI state containing full source content.

Download closure must handle:

- pagination;
- token refresh;
- bounded retry/backoff;
- partial journal;
- valid Range resume;
- server ignoring Range;
- changed revision/ETag;
- local partial-size mismatch;
- cancellation;
- final-size mismatch;
- atomic `.part` promotion.

Runtime closure must handle:

- service absent;
- permission/caller rejection;
- protocol mismatch;
- duplicate dispatch;
- Binder death/reconnect;
- callback correlation;
- cancellation race;
- process/ViewModel recreation;
- artifact list/open preview with strict caps;
- governance denial before side effects.

UI state must explicitly represent disconnected/authenticating/connected/listing/selected/downloading/ready/dispatching/running/completed/failed/cancelled. Show `TOKEN_VAZIO` and temporal contradictions honestly.

Required tests:

- OAuth state/PKCE/token/identity/secure-storage failure cases;
- Drive listing and metadata mapping;
- 401 refresh and bounded 429 retry;
- resumable-download edge cases;
- runtime job schema and policy validation;
- governance denial;
- AIDL client/service protocol behavior;
- Binder death/cancellation/correlation;
- ViewModel state transitions and recreation;
- no secrets in logs/toString;
- lint and affected APK build.

CI must run unit tests, lint, AIDL compilation, the affected debug APK build, schema/protocol compatibility checks and publish the APK/test reports/hashes as artifacts. Do not claim physical-device proof without an actual emulator/device run.

Do not stop at interfaces, screen mockups or TODO handlers. Implement the complete vertical, run tests, correct failures and open a PR only after focused CI is green. Document remaining blockers as exact `TOKEN_VAZIO` items.

The PR description must include:

- files inspected;
- files changed;
- OAuth scope/redirect/security decisions;
- token-storage boundary;
- download journal/resume algorithm;
- memory buffer limits;
- runtime protocol version;
- governance integration;
- tests and commands actually executed;
- CI results;
- APK/artifact hashes;
- remaining `TOKEN_VAZIO` items;
- manual verification steps.

Execute the architecture already defined. Do not redesign RafGitTools into the data parser or Termux into an unrestricted terminal service.

---
