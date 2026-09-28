# Receipt — Semantic Context Exam V1 — C1 exact-head closure

State: `PASS_SEMANTIC_CONTEXT_EXAM_SOURCE_AND_CI`  
Parent: `receipts/2026-09-28_SEMANTIC_CONTEXT_EXAM_V1.md`  
Supersedes only the pending execution state of the parent; history is preserved.  
claim_allowed: `false`

## Canonical identity

```text
repository=rafaelmeloreisnovo/RafGitTools
pr=533
tested_head=d8cbade9f1f999763336afda1cb6377068e59969
main_merge=b5831fd07f44561b9b79f7e7f291b1f6266cee6d
changed_paths_identity=9/9 byte-identical tested-head ↔ main
```

## Execution evidence

```text
workflow=START · RAFAELIA Orchestrated Pipeline
run_id=36393361561
run_number=289
conclusion=success

semantic_context_exam_tests=9/9 PASS
contract_validation=PASS
example_evaluation=PASS_SAFE_CONTEXT_EXAM
example_executable_operations=1
example_blocked_operations=1
example_claim_allowed=false
example_execution_state=TESTED_NOT_PHYSICALLY_PROVEN
```

Observed general jobs:

```text
plan/route=SUCCESS
workflow_topology=SUCCESS
coherence_anti_regression=SUCCESS
python_deterministic_tests=SUCCESS
federation_provenance=SUCCESS
documentation_claim_boundary=SUCCESS
codeql_actions=SUCCESS
codeql_java_kotlin=SUCCESS
android_test_lint_devDebug=SUCCESS
final_receipt=SUCCESS
provider_environment_lane=SKIPPED_BY_POLICY
signed_release=SKIPPED_BY_POLICY
```

Artifacts:

```text
coherence_artifact=10956883475
coherence_digest=sha256:f78e1c4edb18c83cc9c6250e60ada6fbf30795dda6d29bdee54bf0c883b0e959

android_diagnostics_artifact=10957945576
android_diagnostics_digest=sha256:f18e3aa202c5ede946a2cf8d4c4b52037ec612bb84982f322cb9bd1b4e5123b8

devDebug_artifact=10957940516
devDebug_digest=sha256:69d83172a0eea248c71757b4be96a59e5ee13f4fd119c77456c3e3c4ba3c9d91

final_receipt_artifact=10957661498
final_receipt_digest=sha256:4dac501e2a443f3cc186a01d0704467534d7e5f9312c27c2f6cc0139acde4693
```

## What is now proven

The seven parable-derived gates are materialized, syntactically valid,
falsifiable, and executed on the exact PR head. The safe example demonstrates
both positive execution and fail-closed blocking.

## What is not proven

```text
MODEL_TOKENIZER_INTERNAL_CHANGE=NOT_CLAIMED
MODEL_WEIGHT_CHANGE=NOT_CLAIMED
HIDDEN_ATTENTION_CHANGE=NOT_CLAIMED
LIVE_CONTEXTBROKER_BINDING=TOKEN_VAZIO
LOCAL_MODEL_CONSUMER_RUNTIME=TOKEN_VAZIO
PHYSICAL_DOMAIN_AUTHORITY=TOKEN_VAZIO
PHYSICAL_THEORY_CONFIRMED=TOKEN_VAZIO
```

## R3

```text
F_ok=seven executable gates + 9/9 focused tests + full orchestrated CI success + main merge
F_gap=live consumer binding and domain/physical authority remain open
F_next=bind the examiner read-only between ContextBundle V2 and a live consumer, then capture an independent runtime receipt
```
