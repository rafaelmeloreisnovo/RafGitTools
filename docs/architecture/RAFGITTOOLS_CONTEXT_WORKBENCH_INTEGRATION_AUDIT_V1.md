# RafGitTools Context Workbench — Integration Audit V1

**Date:** 2026-09-23  
**Base observed:** `main@ddac77615d857cb2d7f05dd5450d04f8e5fc39ee`  
**State:** `AUDIT / INTEGRATION_SPEC / NO_RUNTIME_PROMOTION`  
**claim_allowed:** `false`

## 1. Purpose

Turn the existing RafGitTools surfaces into one operator workspace instead of creating another parallel application.

Target workflow:

```text
browse/open file
→ select/copy/paste
→ add bounded context
→ retrieve NOVOexport/context refs
→ ask local model
→ receive proposal/patch
→ inspect diff
→ stage in governed workspace
→ dry-run
→ exact human approval
→ rafgitfs/* branch
→ draft PR
→ receipt
```

The audit found that most pieces already exist. The main gap is **wiring and contract convergence**, not a greenfield rewrite.

## 2. Current repository heads observed

| Repository | Observed ref | Role in this architecture |
|---|---|---|
| `rafaelmeloreisnovo/RafGitTools` | `main@ddac77615d857cb2d7f05dd5450d04f8e5fc39ee` | Android control plane, Git UI, governed write, local bridge |
| `rafaelmeloreisnovo/llamaRafaelia` | `master@04fbf7ba523f67f5deebb2e77d6d0e4f25270ee2` | local LLaMA/RAFAELIA inference and context consumer |
| `rafaelmeloreisnovo/GAIA_phi` | `main@fdcf3f367cf8c22850199478fac4f5de62e381f4` | bounded semantic/vector adapter and deterministic index/query components |
| `rafaelmeloreisnovo/Rafaelia_Private` | `main@0652c7f3110d6a7e76a93fa319a7c09d7e584093` | private policy/research integration and evidence-bounded architecture |
| `rafaelmeloreisnovo/RafPolimata` | `main@f22efc099ac530d946ff2ec34954455f75632e92` | deterministic parsing/validation/research support |
| `rafaelmeloreisnovo/florisboard` | `main@dc47e1ff9c426adcc685b4b188324e47f297930a` | Android keyboard/clipboard surface |
| `rafaelmeloreisnovo/CONVERSATIONS_CHUNKS_PRIVATE` | `main@bf80ef1a5992d37619d12bd8506a4d170489d5b6` | private corpus/index/memory bridge and NOVOexport pointers |

No accessible repository with the exact names `RafaelYA`, `Rafael-YA`, `RafaelYak` or `YACTO` was resolved in this audit. Treat that name as `TOKEN_VAZIO_NAMING` until bound to an exact repository/ref. Do not silently map it to another component.

## 3. What already exists in RafGitTools

### 3.1 Git and repository UI

The main navigation already exposes repository, commit, branch, file browser, diff, stash, tags, releases, search, terminal, issues, PRs, LFS, worktree, bisect and webhooks.

Relevant source:

- `app/src/main/kotlin/com/rafgittools/MainActivity.kt`
- `app/src/main/kotlin/com/rafgittools/ui/navigation/Screen.kt`
- `app/src/main/kotlin/com/rafgittools/ui/screens/filebrowser/`

### 3.2 Governed editing/write path

`RafGitFS` already has a private workspace, staged files, base SHA, three-way comparison, dry-run, exact approval, generated `rafgitfs/*` branch, atomic commit, no-force push, draft PR and append-only receipt.

Relevant source/spec:

- `docs/RAFGITFS_GOVERNED_GIT_WRITE_V1.md`
- `app/src/main/kotlin/com/rafgittools/RafGitFsActivity.kt`
- `app/src/main/kotlin/com/rafgittools/ui/screens/rafgitfs/WorkspaceEditorScreen.kt`
- `app/src/main/kotlin/com/rafgittools/ui/screens/rafgitfs/WorkspaceEditorViewModel.kt`

Current friction: this is a separate activity/surface and the editor starts from manually entered path/content/SHA rather than a shared workspace/session opened directly from FileBrowser or context results.

### 3.3 Local model bridge

A local-only bridge already exists:

- loopback bind `127.0.0.1:8765`;
- `GET /health`;
- `POST /v1/chat`;
- local token;
- OpenAI-compatible model client for llama.cpp/llamaRafaelia.

Relevant source:

- `app/src/main/java/com/rafgittools/bridge/RafBridgeService.java`
- `app/src/main/java/com/rafgittools/bridge/RafBridgeContract.java`
- `app/src/main/java/com/rafgittools/bridge/RafModelClient.java`
- `app/src/main/java/com/rafgittools/bridge/RafBridgeActivity.java`

Important boundary: the current contract intentionally permits **chat only** and excludes shell, filesystem writes, git writes and hidden automation. Keep that boundary. Do not turn the loopback HTTP service into a general write API.

### 3.4 Existing human/AI middleware

This repo already contains:

- `docs/HUMAN_AI_BIVALENT_PRIVACY_MIDDLEWARE_V1.md`
- `contracts/human-ai-middleware-v1.schema.json`
- `contracts/human-ai-adapter-v1.schema.json`
- `configs/human-ai-middleware/adapters.v1.json`
- validator/tests.

Its declared federation already assigns:
- RafGitTools = control plane/UI/consent/receipts;
- RafPolimata = deterministic parser/validator;
- GAIA_phi = deterministic sensor/manifest/delta;
- llamaRafaelia = semantic interpreter;
- Rafaelia_Private = private policy/retention/consent.

Therefore the new workbench should **reuse and narrow this architecture**, not create a competing middleware contract.

### 3.5 NOVOexport navigation already exists

`tools/rafaelia_navigator/` already builds a local-first SQLite/FTS representation of NOVOexport and preserves conversation/node/message/Codex/assets/provenance.

Canonical entry:
- `tools/rafaelia_navigator/README.md`

The documented intent is exactly the required middle-field behavior:
- do not reread the whole corpus per question;
- keep raw sources read-only;
- query compact indexes;
- use private compact outputs;
- feed Termux/GAIA_phi/Rafaelia_Private without publishing private bodies.

### 3.6 Drive/local staging already exists

`DriveStagingGate.kt` plus HomeScreen SAF import already implements a readback/hash/local receipt boundary before a user binds a GitHub destination.

This should become another **resource provider** in the shared workbench rather than a separate one-off flow.

## 4. P0 blocker — ContextBundle v1 collision

Three repositories currently claim the same logical schema/version but require incompatible field sets.

### RafGitTools

`docs/contracts/context_bundle.schema.json`

Required:

```text
schema, bundle_id, chunks, created_at
```

### llamaRafaelia

`docs/contracts/context_bundle.schema.json`

Required:

```text
schema, bundle_id, conversation_chunks, generated_at
```

### CONVERSATIONS_CHUNKS_PRIVATE

`docs/contracts/context_bundle.schema.json`

Required:

```text
schema, bundle_id, chunk_refs, intent_candidates
```

All identify themselves as `rafaelia.context_bundle.v1`.

This is a hard interoperability defect:

```text
same schema id/version != same contract
```

### Required correction

Do not mutate the historical v1 files to pretend they were always equivalent.

Create a successor contract, for example:

`rafaelia.context_bundle.v2`

with explicit compatibility adapters:

```text
RafGitTools-v1 ─┐
llama-v1 ───────┼→ ContextBundleV2
chunks-v1 ──────┘
```

A V2 should carry references first, not duplicate corpus bodies.

Minimum candidate fields:

```text
schema
bundle_id
created_at
intent
resources[]
chunk_refs[]
evidence_refs[]
constraints[]
annotations
privacy_class
source_generation
```

Each `resource` should include enough identity for audit:

```text
provider
repository_or_corpus
ref_or_generation
path_or_locator
object_id
sha256_if_known
visibility
epistemic_state
```

## 5. NOVOexport authority boundary

The current-source pointer in `CONVERSATIONS_CHUNKS_PRIVATE/memory_bridge/pointers/NOVOEXPORT_SOURCE_POINTER_V2.json` declares the current NOVOexport authority separately from the historical mirror.

Architecture rule:

```text
current raw NOVOexport authority
→ immutable/source pointer
→ local Navigator index
→ bounded query
→ ContextBundle
→ model
```

Do not make RafGitTools copy the raw corpus into its repository or app database by default.

`CONVERSATIONS_CHUNKS_PRIVATE` should serve as the private **index/pointer/memory authority**, not be rewritten as if it were the current raw NOVOexport source.

## 6. Proposed control-plane shape

```text
FlorisBoard / physical keyboard
              │
              ▼
┌──────────────────────────────────────────────┐
│        RafGitTools WorkspaceShell            │
│                                              │
│  Resource tree │ Editor tabs │ Context pane  │
│                │             │ Local AI pane │
│                │             │ Diff/Patch    │
└───────────────────┬──────────────────────────┘
                    │
                    ▼
           ContextBroker / ProviderRegistry
        ┌───────────┼────────────┬────────────┐
        ▼           ▼            ▼            ▼
 NOVOexport     Git/RafGitFS  llamaRafaelia  Clipboard
 Navigator      read/write     inference      ephemeral
        │
        ├──────────────► GAIA_phi bounded adapter
        ├──────────────► Rafaelia_Private policy/context
        └──────────────► RafPolimata deterministic validator
```

RafGitTools should own the **workspace UX and orchestration**. Producer repositories remain authoritative for their own implementation.

## 7. Core domain objects to add

### ResourceRef

Identity of something that can be opened.

```text
provider
repository/corpus
ref/generation
path/locator
object_id
sha/blob/hash when available
visibility
mime/type
```

### WorkspaceTab

```text
tab_id
ResourceRef
cursor
selection
dirty_state
base_sha
```

### SelectionEnvelope

For internal copy/context actions:

```text
text
ResourceRef
start/end range
selection_hash
privacy_class
created_at
```

The Android system clipboard should normally receive plaintext only. Provenance metadata should stay app-private.

### ContextBundleV2

The converged reference-first context envelope described above.

### PatchProposal

```text
proposal_id
target ResourceRef
base_sha
context_bundle_id
hunks/structured edits
rationale
evidence_refs
status = PROPOSED
```

A model proposal is never a write permission.

### JumpEntry

```text
from ResourceRef + cursor
to ResourceRef + cursor
timestamp
reason
```

This enables file-to-file back/forward navigation.

## 8. UI wiring needed now

### One WorkspaceShell

Unify entry points that are currently separate:

- MainActivity FileBrowser;
- RafGitFsActivity;
- RafBridgeActivity;
- Drive/SAF staging;
- context/NOVOexport query.

Do not delete the existing activities immediately; route them through a shared internal state/controller first so rollback remains easy.

### Editor tabs

Required:
- multiple files;
- local and remote refs;
- dirty indicator;
- close/reopen;
- `Ctrl+Tab` / `Ctrl+Shift+Tab`;
- back/forward jump stack;
- tap path/reference to open source.

### Keyboard/command actions

Hardware keyboard:

```text
Ctrl+C          copy selection
Ctrl+V          paste
Ctrl+X          cut where editing is permitted
Ctrl+S          stage/save local workspace state, NOT direct push
Ctrl+P          quick-open file/resource
Ctrl+Shift+P    command palette
Ctrl+F          search current file
Ctrl+Shift+F    workspace/context search
Ctrl+Tab        next tab
Alt+Left/Right  jump back/forward
```

Touch equivalents must exist; do not require a physical keyboard.

## 9. FlorisBoard integration

The FlorisBoard fork already contains a real clipboard manager/history:

- `ime/clipboard/ClipboardManager.kt`
- `ClipboardInputLayout.kt`
- system clipboard synchronization;
- pinned/recent items;
- auto-clean settings including sensitive-data expiry.

Therefore do **not** build a second clipboard database into RafGitTools.

V1 integration should use the Android clipboard and explicit intents/actions.

Recommended optional action row:

```text
Copy
Paste
Copy path
Add selection to context
Open previous/next file
Ask local model
Preview patch
```

Privacy boundary:
- no automatic ingestion of clipboard history into NOVOexport/context;
- sensitive clipboard contents never become model context without explicit action;
- credentials remain blocked/redacted;
- internal provenance stays app-private.

## 10. Provider responsibilities

### NovoExportProvider

Backed by `rafaelia_navigator` / private memory bridge.

Capabilities:

```text
QUERY
OPEN_REF
BUILD_CONTEXT
RELATIONS
PROVENANCE
```

No raw-corpus mutation.

### GitProvider / RafGitFS

Capabilities:

```text
LIST
OPEN
DIFF
STAGE
DRY_RUN
PUBLISH_DRAFT_PR
ROLLBACK_COMMIT
```

Writes stay governed.

### LlamaProvider

Backed by existing `RafModelClient` / llamaRafaelia.

Capabilities:

```text
HEALTH
CHAT
PROPOSE_PATCH
SUMMARIZE_SELECTION
EXPLAIN_DIFF
```

No direct write capability.

### GaiaProvider

Use the bounded pointer adapter / VecDB/query pieces as an optional semantic relation provider.

Do not copy selected private text into GAIA merely to satisfy integration; pass refs/hashes where its current federation contract expects them.

### RafaeliaPrivateProvider

Private policy/research/context authority. Expose only typed, explicitly allowed capabilities.

### RafPolimataProvider

Deterministic parser/validator/structurer. Good location for syntax/formal validation of model proposals before they enter RafGitFS staging.

### FlorisClipboardProvider

Ephemeral interaction surface only. Not a longitudinal memory authority.

## 11. Read path

```text
user opens/selects resource
→ WorkspaceShell creates ResourceRef/SelectionEnvelope
→ ContextBroker queries selected providers
→ ContextBundleV2 with provenance
→ llamaRafaelia receives bounded bundle
→ answer/proposal returned
→ UI shows source refs beside result
```

## 12. Write/patch path

```text
selection + context
→ llama proposal
→ PatchProposal(PROPOSED)
→ deterministic validator
→ human preview
→ RafGitFS stage
→ compare base/remote/local SHA
→ dry-run
→ exact human approval
→ rafgitfs/* branch
→ commit/push without force
→ draft PR
→ receipt
```

Do not bypass the current governed writer with a new HTTP write endpoint.

## 13. What to implement first

### P0 — contract convergence

1. Create `context_bundle.v2`.
2. Add adapters for all three incompatible V1 variants.
3. Add schema/fixture tests.
4. Define `ResourceRef`, `SelectionEnvelope`, `PatchProposal`.

This is the first dependency. UI integration before this creates another incompatible lane.

### P1 — shared workspace state

1. Add `WorkspaceSessionStore`.
2. Add tabs.
3. Add jump back/forward stack.
4. FileBrowser opens a WorkspaceTab.
5. WorkspaceEditor consumes an existing tab/resource instead of requiring manual re-entry.

### P2 — NOVOexport read-only provider

1. Bind the existing Navigator SQLite/FTS query.
2. Return refs + excerpts + hashes, not an uncontrolled corpus dump.
3. Build ContextBundleV2 from selected results.

### P3 — local model integration

1. Keep current loopback chat compatibility.
2. Add internal Kotlin provider API for ContextBundle input.
3. llama result may produce PatchProposal, never execute it.
4. Show health/model/runtime state explicitly.

### P4 — governed patch handoff

1. Preview structured/unified diff.
2. Convert approved proposal to RafGitFS staged operations with base SHA.
3. Reuse existing dry-run/conflict/approval/draft-PR path.
4. Preserve rollback.

### P5 — GAIA / Private / Polimata adapters

Enable them one at a time through capability declarations and fixtures. No repository receives universal write or universal context privileges.

### P6 — FlorisBoard / shortcuts

Implement Android clipboard actions and hardware-key shortcuts in RafGitTools first. Then add optional FlorisBoard action keys without coupling RafGitTools runtime to FlorisBoard internals.

### P7 — physical-device closure

Same artifact:
- install;
- login;
- list repo;
- open two files;
- Ctrl+C/Ctrl+V;
- Ctrl+P;
- jump back;
- query NOVOexport;
- ask llama;
- preview patch;
- stage/dry-run;
- create draft PR;
- rollback test;
- receipt.

## 14. Explicit current gaps

- `P0_CONTEXT_BUNDLE_COLLISION`: three incompatible V1 schemas.
- `P0_RAFAYAK_EXACT_IDENTITY`: exact Rafael YA/Yak repository not resolved.
- `P1_SHARED_WORKSPACE`: file browser/editor/context/bridge are not one session.
- `P1_TABBED_EDITOR`: no proven cross-repo tabbed editor/jump stack.
- `P2_NOVO_ANDROID_PROVIDER`: Navigator exists, Android provider wiring not proven.
- `P3_LLAMA_RUNTIME`: bridge source exists; exact local model/device runtime remains separately gated.
- `P4_PATCH_HANDOFF`: governed writes exist; model-proposal → staged patch handoff is not proven end-to-end.
- `P5_PROVIDER_FIXTURES`: GAIA/Private/Polimata adapter runtimes need scoped fixtures.
- `P6_FLORIS_INTEGRATION`: clipboard exists; RafGitTools-specific action integration not wired.
- `P7_DEVICE_E2E`: complete physical workflow not receipted.

## 15. Anti-regression rules

```text
NOVOexport source != Navigator index
ContextBundle != raw corpus
model output != patch approval
patch proposal != staged write
staged write != GitHub publication
draft PR != merge
clipboard != memory
similarity != evidence
repository presence != runtime
IMPLEMENTED_UNTESTED != PASS
```

## R3

**F_ok:** control plane, governed Git writer, NOVOexport Navigator, local LLaMA bridge, middleware contracts, clipboard and provider repositories already exist.

**F_gap:** contract collision and disconnected UI/runtime lanes prevent them from acting as one workbench.

**F_next:** close ContextBundle V2 first, then implement a shared WorkspaceSession and one read-only vertical slice: FileBrowser → ContextBroker/NOVOexport → llama → source-linked answer; only after that wire PatchProposal → RafGitFS.
