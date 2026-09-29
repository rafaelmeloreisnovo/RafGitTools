# Receipt — Semantic Context Runtime Binding V1 — preregistration

Date: 2026-09-29  
Kind: `SOURCE_IMPLEMENTATION + RUNTIME_BINDING + TEST_SOURCE`  
Base: `RafGitTools main@58f3ec90bf7f0f43b502667babe596801e2a6baa`  
claim_allowed: `false`

## Intended delta

Bind the already-canonical Semantic Context Exam to the existing local-model
consumer in read-only mode:

```text
ContextBroker
→ ContextBundle V2
→ runtime semantic exam
→ privacy / credential / size gates
→ RafModelClient
```

## Preregistered evidence states

```text
BINDING_SOURCE=IMPLEMENTED_ON_BRANCH
RUNTIME_EXAM_TEST_SOURCE=IMPLEMENTED
PR_EXACT_HEAD_CI=TOKEN_VAZIO
ANDROID_BUILD=TOKEN_VAZIO
MODEL_SERVER_ROUNDTRIP=TOKEN_VAZIO
PHYSICAL_DEVICE=TOKEN_VAZIO
```

## Non-promotion invariants

```text
MODEL_OUTPUT != EXECUTION_PERMISSION
SEMANTIC_EXAM_PASS != CLAIM_PROMOTION
CI_PASS != MODEL_SERVER_ROUNDTRIP
MODEL_SERVER_ROUNDTRIP != PHYSICAL_DEVICE_RECEIPT
TOKEN_VAZIO != 0
```

## F_next

Run exact-head CI. If unit tests, Hilt/Android compilation, lint and APK
assembly pass, create a successor receipt. Keep real model-server and physical
device execution as separate TOKEN_VAZIO gates until observed.
