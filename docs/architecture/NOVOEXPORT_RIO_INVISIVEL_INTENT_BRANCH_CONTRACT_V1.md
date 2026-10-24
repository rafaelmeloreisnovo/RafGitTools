# RAFAELIA — Lossless Intent Branch Handoff Contract (public, source-free)

**State:** `PROPOSED / PLAN_ONLY / claim_allowed=false`. This is a governance interface only, not executable model introspection or proof of human thought.

The NOVOexport Name Atlas is a metadata subtask, **not** an exhaustive representation of the human request. A downstream executor must not infer that narrowing a task to file names, SQLite, receipts or one canary was authorized as the final semantic objective.

## Required interpretation fields

For any substantive assistant-scoped action, preserve:

- `user_source_ref`: opaque locator to an exact user turn or explicitly typed `TOKEN_VAZIO_SOURCE_ID`.
- `user_literal_scope`: quoted or checksum-linked source (private stays private); meaning must not replace source.
- `assistant_interpretation`: labeled candidate, not human-asserted intent.
- `assistant_selected_action`: branch selected by assistant and reason, with scope.
- `unselected_branches`: possible alternatives preserved as `NOT_EXPLORED`, never `USER_REJECTED` without proof.
- `human_selection_evidence`: explicit turn/approval reference or `TOKEN_VAZIO_NO_EXPLICIT_SELECTION`.
- `modalities`: `VERBAL_LITERAL`, `AVERBAL_CUE`, `NONVERBAL_MEDIA`, `UNOBSERVABLE`; do not promote absent media or private model activations into observed facts.
- `lens`: metaphor/cultural/technical classification separate from proof, with provenance when attributed to a historical teacher.
- `receipt`: source commit, delta, rollback, head-specific CI, physical-device receipt when needed.
- `next_gate`: minimal falsifier that can re-open discarded branches.

## Refusal to silently collapse scope

`ASSISTANT_SELECTED != USER_SELECTED`.
`NOT_EXPLORED != FALSE != USER_REJECTED`.
`TOKEN_VAZIO != 0 != NULL != ∅`.
`SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM`.
`A_PARABLE != A_SCIENTIFIC_PROOF`.

Default for branch choice is `PLAN_ONLY`. A machine must not publish private semantic corpus, declare raw-language coverage, or claim it has access to the model's hidden weights, activations or internal reasoning.

## Event example (redacted, conceptual)

```json
{
  "schema": "rafaelia.intent_branch_handoff.v1",
  "user_source_ref": "TOKEN_VAZIO_SOURCE_ID",
  "assistant_interpretation": "Candidate: catalog file names",
  "assistant_selected_action": "Name Atlas",
  "unselected_branches": ["turn-level historical genealogy", "semantics and multimodal signals", "parables as lenses"],
  "human_selection_evidence": "TOKEN_VAZIO_NO_EXPLICIT_SELECTION",
  "result_state": "PLAN_ONLY",
  "claim_allowed": false
}
```

## Executor placement

- NOVOexport private: true message identities, user/assistant roles, raw or authorized projection, private detailed branch ledger.
- Mapa / ATLAS: schema/ontology and route pointers, never guessed identity of raw content.
- RafGitTools: format/gate/receipt consumer; no training, no assumed semantic completeness.
- Android Termux: canary or explicitly authorized local source inspection with independent physical receipt.
- Drive: append-only human-readable ledger, successor chain, source refs and decisions.

**R3:** `F_ok=contract documented`; `F_gap=all physical and exact-transcript semantics unproven`; `F_next=source-bound message-ID crosswalk and explicit human review`.