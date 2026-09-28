# Session AI Executor Adapter V1

State: `IMPLEMENTED_UNTESTED`
claim_allowed: `false`

RafGitTools is the local executor adapter for the Mapa session dispatcher. It does not copy Mapa authority and does not treat an agent assignment as execution.

## Fast path

1. Resolve one `packet_id` from the federated dispatcher.
2. Load <=3 local sources.
3. Resolve execution target and evidence rule.
4. Create/reuse a non-default branch.
5. Apply the smallest reversible delta.
6. Run/observe the named gate.
7. Append problem/evidence/rollback receipt.
8. Return refs to Mapa and Drive.

## Safety

- no default-branch direct mutation;
- no auto-merge/release;
- no weakening tests to obtain green CI;
- no raw private payload in this public adapter;
- no OCR-resistance privacy claim;
- provider mutation claims require provider readback.

## Local packets

`SP01` Templo Vivo privacy intake; `SP02` CI/provider correction loop; `SP03` ARCH_PIN/receipt; `SP06` documentation routes; `SP08` session dispatch adapter.

## R3

- F_ok: local consumer contract is machine-readable.
- F_gap: exact-head validator/CI and Mapa PR binding are pending.
- F_next: validate adapter, open draft PR, then bind Drive documentary mirror.
