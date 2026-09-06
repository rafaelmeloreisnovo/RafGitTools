# RAFAELIA FNEXT8 — 05 — Cross-Repository Receipt Validator Contract

id: FNEXT8-20260906-05
state: IMPLEMENTED_ON_BRANCH
claim_allowed: false
authority: RafGitTools / evidence automation

## Purpose
Define the machine-checkable minimum for a receipt that binds a repository event to evidence and lineage.

## Required receipt fields
- `receipt_id`
- `schema_version`
- `repo`
- `ref`
- `commit_sha`
- `event_type`
- `source_pointer`
- `artifact_hashes[]`
- `predecessor_receipt`
- `evidence_level_before`
- `evidence_level_after`
- `claim_allowed`
- `token_vazio[]`
- `next_verifiable_step`
- `timestamp`

## Validator invariants
1. Reject missing commit SHA for GitHub-bound events.
2. Reject evidence-level promotion without supporting artifact pointer.
3. Reject `claim_allowed=true` when a required gate is `TOKEN_VAZIO`.
4. Preserve predecessor lineage; do not rewrite prior receipts.
5. Hashes are evidence of byte identity, not semantic correctness.
6. A CI pass is not automatically device/runtime proof.

## Output states
`VALID`, `VALID_WITH_TOKEN_VAZIO`, `INVALID_SCHEMA`, `BROKEN_LINEAGE`, `UNSUPPORTED_PROMOTION`.

This contract specifies validation behavior; executable implementation and test receipts remain separately evidenced.
