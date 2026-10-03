# Session AI Dispatch pin coherence receipt V1

Date: 2026-10-03  
State: `CORRECTED_RETEST_PENDING`  
claim_allowed: `false`

## Resolved boundary

- SOURCE: `rafaelmeloreisnovo/Mapa:data/control-plane/SESSION_AI_WORK_DISPATCH_V1.json`.
- AUTHORITY: Mapa federated routing authority; RafGitTools local resolver/consumer only.
- EXECUTION_TARGET: RafGitTools successor branch `hotfix/session-dispatch-pin-coherence-20261003`.
- EVIDENCE_RULE: session resolver unit tests + practice-router validation + exact-head START; assignment remains distinct from execution.

## Before

`configs/session-ai-dispatch-adapter.v1.json` had already been corrected to the Mapa merge commit `ad2efc2b9a4599bdcc37ab416c84bd99dc2a1e09`, while these consumer surfaces still pinned predecessor commit `21c2f9aefab86cd31b8ea7da22533452690fd087`:

- `scripts/resolve_session_ai_work_packet.py`;
- `configs/session-agent-dispatch.v1.json`;
- `configs/practice-router.v1.json`;
- `docs/AI_SESSION_DISPATCH_ADAPTER_V1.md`.

The authoritative dispatch blob at corrected merge commit `ad2efc2b...` is `c1d1d8f1692c74957f8b70968fa863f855d93cb0`, identical to the content pin already used by the consumers. Therefore this delta corrects commit identity/provenance and does not alter dispatch content.

## After

All four consumer surfaces now bind the corrected merge identity `ad2efc2b9a4599bdcc37ab416c84bd99dc2a1e09` and the unchanged blob `c1d1d8f1692c74957f8b70968fa863f855d93cb0`.

`SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM`

`TOKEN_VAZIO != 0`

`AGENT_ASSIGNMENT != AGENT_EXECUTION`

## Falsifier

The correction is falsified if the pinned Mapa commit does not expose the stated blob/path/schema, if resolver fail-closed tests regress, or if practice-router validation rejects the route.

## Replay

1. Checkout Mapa at `ad2efc2b9a4599bdcc37ab416c84bd99dc2a1e09`.
2. Verify `HEAD:data/control-plane/SESSION_AI_WORK_DISPATCH_V1.json` equals blob `c1d1d8f1692c74957f8b70968fa863f855d93cb0`.
3. Run `python3 -m unittest discover -s tests -p 'test_resolve_session_ai_work_packet.py' -v`.
4. Run `python3 scripts/validate_practice_router.py configs/practice-router.v1.json --route session-ai-dispatch`.
5. Observe exact-head START before promotion.

## Rollback

Revert this successor branch/PR. Historical #539 evidence and Mapa merge identity remain unchanged.

## R3

- F_ok: content identity verified; four stale consumer references aligned to the corrected merge identity.
- F_gap: exact-head terminal validation remains pending.
- F_next: observe exact-head START; do not merge or promote before terminal evidence.
