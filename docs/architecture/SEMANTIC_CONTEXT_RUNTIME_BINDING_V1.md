# Semantic Context Runtime Binding V1

State: `IMPLEMENTED_SOURCE / CI_PENDING / MODEL_ROUNDTRIP_TOKEN_VAZIO`  
Authority: `rafaelmeloreisnovo/RafGitTools`  
Mode: `READ_ONLY_LOCAL_MODEL`  
claim_allowed: `false`

## Purpose

Close the source-level gap between the already-tested Semantic Context Exam V1
and the existing local-model consumer without turning the model into execution
authority.

The route is now:

```text
explicit FileBrowser selection
→ ContextBroker
→ ContextBundle V2
→ action=context_chat
→ RafSemanticContextExamRuntime
→ RafContextPrivacyGate
→ credential / size gates
→ RafModelClient.chatExaminedContext
→ loopback llamaRafaelia / OpenAI-compatible server
→ reply only
```

The model cannot write files, run shell, mutate Git, publish, or promote a claim
through this route.

## What changed

### ContextBroker

`buildReadOnlyModelBundle(...)` is Java-friendly and requires at least one
explicitly selected segment.

It adds:

```text
consumer_mode=READ_ONLY_LOCAL_MODEL
semantic_exam_required=true
claim_allowed=false
```

and preserves:

```text
SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM
TOKEN_VAZIO != 0
```

### Runtime examiner

`RafSemanticContextExamRuntime` is a JVM/Android implementation of the
operational subset of Semantic Context Exam V1.

It validates:

1. source and evidence references;
2. semantic type, unit, dimension and object state;
3. explicit transforms, invariants and transform evidence;
4. operation outcomes versus declared expected state;
5. alternative representation selection;
6. implemented/tested/physical state separation;
7. claim scope, physical evidence and authority;
8. useful-delivery fields.

A safe read-only route requires:

```text
state=PASS_SAFE_CONTEXT_EXAM
claim_allowed=false
```

Even a fully evidenced `claim_allowed=true` manifest is rejected by the
read-only model channel because claim promotion belongs to another authority.

### Privacy gate

`RafContextPrivacyGate` is non-promoting:

```text
PUBLIC    → public | private | sensitive
PRIVATE   → private | sensitive
INTERNAL  → private | sensitive
SENSITIVE → sensitive
TOKEN_VAZIO → BLOCK
```

### Local bridge contract

The existing `action=chat` remains compatible.

The new path is:

```json
{
  "action": "context_chat",
  "semantic_exam": { "...": "explicit typed manifest" }
}
```

The request still requires explicit consent and the existing local token.

The actual context text is **not** accepted from the browser request. It comes
from the in-memory ContextBroker selection already made inside RafGitTools.

## Size and credential gates

Before model I/O:

```text
ContextBundle JSON <= 65536 chars
Semantic Exam result <= 16384 chars
credential-like material = BLOCK
unknown privacy = BLOCK
empty selected context = BLOCK
```

Oversized or unsafe input is a contract rejection, not a model-runtime failure.

## Evidence layers

This change intentionally separates:

```text
BINDING_SOURCE
BINDING_UNIT_TEST
ANDROID_BUILD
MODEL_SERVER_ROUNDTRIP
PHYSICAL_DEVICE
```

CI can prove the first three.

A real llamaRafaelia response can prove `MODEL_SERVER_ROUNDTRIP`.

A physical-device receipt is required for `PHYSICAL_DEVICE`.

No layer inherits the next one.

## Canonical files

- `app/src/main/kotlin/com/rafgittools/workspace/ContextBroker.kt`
- `app/src/main/java/com/rafgittools/bridge/RafBridgeContract.java`
- `app/src/main/java/com/rafgittools/bridge/RafBridgeService.java`
- `app/src/main/java/com/rafgittools/bridge/RafModelClient.java`
- `app/src/main/java/com/rafgittools/bridge/RafSemanticContextExamRuntime.java`
- `app/src/main/java/com/rafgittools/bridge/RafContextPrivacyGate.java`
- `app/src/test/java/com/rafgittools/bridge/RafSemanticContextExamRuntimeTest.java`
- `app/src/test/java/com/rafgittools/bridge/RafContextPrivacyGateTest.java`
- `app/src/test/kotlin/com/rafgittools/workspace/ContextBrokerTest.kt`

## Claim boundary

A successful build/test means the read-only binding and fail-closed runtime
gates are implemented and executable in the tested software scope.

It does **not** prove:

- a llamaRafaelia server is currently running;
- a model returned a correct answer;
- the route ran on a physical phone;
- tokenizer internals or model weights changed;
- any scientific or physical claim is true.

Those remain independent gates.
