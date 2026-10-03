# RafGitTools Wiring Closure Event — 2026-10-03 V1

State: `ACTIVE / BOUNDED / CLAIM_ALLOWED=false`  
Authority: `rafaelmeloreisnovo/RafGitTools`  
Federated authority: `rafaelmeloreisnovo/Mapa`  
Baseline: `03bc5f938985b07bc8d19771d316fac84f71227e`  
Master coordination: `RafGitTools#241`  
Drive/runtime vertical: `RafGitTools#240`

## Mission

Close the already-discovered integration gaps one responsibility family at a time.
The event does not redesign RafGitTools and does not broaden corpus, provider or
claim scope. It turns existing components into truthful, reconstructible edges.

```text
SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM
TOKEN_VAZIO != 0
IMPLEMENTED_UNTESTED != PASS
QUEUED_RESPONSE != DURABLE_ENQUEUE
PROTECTED != REQUIRED_CI_ENFORCED
```

## Operating discipline

This event applies engineering controls as working practice, not as a claim of
external certification: traceable requirements, explicit authority, bounded
change, measured exit criteria, nonconformity tracking, rollback, provenance,
privacy/security fail-closed behavior and append-only correction history.

Every family follows:

```text
BIND exact head
→ observe defect
→ smallest reversible patch
→ deterministic gate/falsifier
→ exact-head CI
→ receipt
→ only then advance to the next family
```

No family inherits PASS from another family.

## Family queue

| Order | Family | Existing material | Gap / falsifier | Tracker | State |
|---|---|---|---|---|---|
| F01 | Governance → ToolRouter → durable queue | `GovernanceGate`, `ToolRouter`, `OfflineQueue`, `SyncOperation`, `SyncWorker` | an allowed tool has no handler, a handler has no registry entry, or `queued` is emitted before persistence | #240 / PR #635 | `IMPLEMENTED_UNTESTED` |
| F02 | Runtime lock / supply-chain identity | `runtime-lock.json`, promotion gates, producer pins | lock differs from actual Gradle/AGP/Kotlin/KSP or promoted artifact hashes remain unbound | #597, #246 | `OPEN` |
| F03 | Local bridge secret boundary | `SecureStorage`, `RafBridgePrefs`, loopback bridge | bridge bearer token remains in ordinary `SharedPreferences`; origin policy not explicitly minimized | successor tracker required only after F01 terminal evidence | `DISCOVERED` |
| F04 | Provider-neutral Workspace | `ResourceRef`, `WorkspaceSessionStore`, `ContextBroker`, FileBrowser tabs | non-`LOCAL_GIT` resource cannot be reopened through provider dispatch | #240 | `OPEN` |
| F05 | Drive → typed runtime bridge → Navigator | runtime-job schema, Phase-3 specification, Navigator tools | `DriveApi` / `IRafRuntimeService` / `DataNavigatorScreen` absent as implementation on baseline | #240, #241 | `OPEN` |
| F06 | Corpus execution ownership | `rafaelia_navigator`, Android bounded processors | large `conversations.json` processing must not be silently promoted to APK/JVM ownership when runtime route is required | #240, #241 | `OPEN` |
| F07 | Native/runtime semantic build identity | CMake conditional `raf_llama_kernel`, runtime hydration | same RafGitTools SHA can denote different native capability unless runtime inputs are receipt-bound | #246 | `OPEN` |
| F08 | Default-branch promotion enforcement | protected `main`, START single-root | provider readback does not show required status checks enforced | #393 | `PROVIDER_ADMIN_PENDING` |
| F09 | Exported Android entry surfaces | launcher/deep-link activities, network security config | exported/deep-link entry lacks explicit untrusted-input review or evidence | create tracker only after higher-order blockers close | `DISCOVERED` |
| F10 | Human/AI reconstruction navigation | AGENTS, agent-entry kernel, current-state docs, issues, receipts | material delta cannot be reconstructed from <=3 canonical entry sources | #241 / this document | `ACTIVE` |

## F01 exact scope

PR `#635` is the active family transaction.

Required exit:

1. allowlisted tools have concrete ToolRouter handlers;
2. concrete handlers are represented in the registry;
3. handlerless future tools fail closed rather than appear executable;
4. `git.push` / `git.pull` persist the existing `SyncOperation` before returning
   `queued`;
5. persistence failure returns an error and never a queue-success receipt;
6. queue state remains distinct from execution state;
7. source validator passes;
8. exact-head START reaches a terminal result.

Until item 8 is observed:

```text
F01 = IMPLEMENTED_UNTESTED
claim_allowed = false
```

## Navigation rule

Human or AI entry for this event should require at most:

1. this event document — queue and state;
2. the active family tracker/PR — current transaction;
3. the exact source/receipt named by that family — evidence.

Do not perform a whole-repository crawl when these three anchors are sufficient.

## Public / private boundary

GitHub may contain code, schemas, tests, redacted receipts, hashes and typed source
references. Private corpus bodies, Drive credentials, OAuth tokens, bridge tokens,
private conversation payloads and unredacted personal data do not belong in this
public reconstruction layer.

A private source may be represented by opaque ID/hash/revision and privacy class;
a typed reference is preferred over copying payload.

## Receipt template

Each family closes with:

```text
baseline_sha
head_sha
files_changed
tracker
falsifier
commands_or_ci_run
terminal_gate_state
rollback_ref
F_ok
F_gap
F_next
claim_allowed=false
```

`PASS` is forbidden when the claimed layer was not executed.
