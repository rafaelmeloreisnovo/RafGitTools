# Receipt — ConversationProjectPackBuilder test compile hotfix V1 — 2026-10-03

State: `IMPLEMENTED_UNTESTED / claim_allowed=false`

## Intent

Restore the exact Android/unit-test compile gate that failed on the PR #625 candidate without changing runtime behavior, corpus semantics, provider access, privacy policy or production code.

## Source / authority

- repository: `rafaelmeloreisnovo/RafGitTools`
- base/current main at branch cut: `196f021cb1ea40841d20d5cbb522109db5b4cb00`
- merged predecessor: PR #625, merge `196f021cb1ea40841d20d5cbb522109db5b4cb00`
- failed predecessor candidate: `9935f25117be25f51c7d72eee23cf5697cc0cd91`
- failed START run: `37128467820`
- failed job: `06 · Android / test + lint + devDebug APK`

## Observed failure

The exact job log reports:

```text
app/src/test/kotlin/com/rafgittools/navigator/ConversationProjectPackBuilderTest.kt:63:40
Unresolved reference: File
```

and then:

```text
:app:compileDevDebugUnitTestKotlin FAILED
```

The test source calls `File(result.generationDir, it.filename)` but imported `ByteArrayInputStream` and `Files`, not `java.io.File`.

## Delta

One production-neutral source change:

```kotlin
import java.io.File
```

No application/runtime implementation code is changed.

## Falsifier / exit criterion

The existing START Android lane is the falsifier. The candidate is not promoted unless the exact hotfix head compiles/tests under the same gate that previously failed.

Minimum causal evidence:

```text
compileDevDebugUnitTestKotlin != failure on unresolved File
```

Full START outcome remains separately observed; a different failing lane is not rewritten as success.

## Invariants

```text
SOURCE != EXECUTION != EVIDENCE != CLAIM
MERGED != CI_PASS
IMPLEMENTED_UNTESTED != PASS
one-line compile fix != physical-device proof
one-line compile fix != provider/Drive execution proof
```

## Rollback

Revert only the import commit if the exact-head gate falsifies the fix or exposes a contradiction. Do not rewrite PR #625 history.

## R3

`F_ok`: exact compile cause is source-backed and the smallest reversible correction is materialized.

`F_gap`: exact-head START result is pending; PR #625 remains historically FAIL at its candidate head even if this successor passes.

`F_next`: run exact-head START, inspect Android/unit-test result, keep fail-closed on any remaining failure, then update current-state/ledger with the observed result.
