# RAFAELIA Ω — Agent Workspace / APK → Drive Inbox V1

Status: `IMPLEMENTED_UNTESTED_ANDROID` until exact-head CI and device receipts.
Scope: proposal preparation and explicit SAF export. No automatic agent execution.

## User path (Android)

1. Open RafGitTools, tap the edit icon labelled **Agent Workspace**.
2. Write an intended task, without PATs/passwords/private corpus data.
3. Tap **Preparar proposta para revisão**. The app creates one UUIDv4,
   SHA-256 of the exact UTF-8 intent, UTC timestamp and immutable-looking
   proposal envelope in **PENDING_HUMAN / NOT_EXECUTED** status.
4. Review the JSON and tap **Salvar JSON no Drive ou armazenamento escolhido**.
5. Select the already-established official Drive folder
   `02_MEIO_DE_CAMPO_GPT_DRIVE_GITHUB/10_INBOX_PROPOSTAS` via Android SAF.
   The system picker controls provider access; **no Google token is copied**.
6. A user-authorized ChatGPT/Drive connector may read the proposal and
   prepare a separate scope-bound review; the file alone confers **zero**
   execution or approval authority.

## Producer and consumer

- Producer: `app/src/main/kotlin/com/rafgittools/AgentWorkspaceActivity.kt`
- Consumer: `LAB_RAPIDTOOLS_OMEGA/bridge/agent_workspace_v1.py`
- Adversarial tests: `bridge/tests/test_agent_workspace_v1.py`
- Exact envelope: `rafaelia.agent-interaction.v1`
- Admission receipt: `rafaelia.agent-admission-receipt.v1`

**Fail closed:** unknown fields, wrong repo/action, invalid UUIDv4, malformed
UTC timestamp, duplicate keys, changed intent/hash, oversized JSON, unsolicited
approval, runtime status or claims are refused. The parser never executes code,
opens remote API connections, reads GitHub PATs, or invents provider authority.

Example validation on a controlled host:

```sh
python3 LAB_RAPIDTOOLS_OMEGA/bridge/agent_workspace_v1.py path/to/proposal.json
python3 -I -B -m unittest discover -s LAB_RAPIDTOOLS_OMEGA/bridge/tests -p 'test_agent_workspace_v1.py' -v
```

The command prints a *shape-only* receipt; it is not a human signature,
a CI PASS, a Supabase transaction or proof that any request was fulfilled.

## Existing components preserved

- GitHub producer `RafGitTools` / federated `Mapa` authority.
- `LAB_RAPIDTOOLS_OMEGA/bridge/dispatch.py` existing REQ-0002 route.
- Google Drive `10_INBOX_PROPOSTAS` and `40_OUTBOX_RECEIPTS`.
- Supabase `raf_private.rafdb_federated_events_v1` historical metadata only.
- Existing GitHub Actions canary, Android permissions, secrets and releases.

## Explicit next gates (not claimed)

1. Signed-in app identity and secure broker for Supabase with RLS; **no**
   service-role key or OpenAI key inside the APK.
2. A durable request queue with atomic claim/idempotency and authenticated
   GitHub Action dispatch. **Do not repurpose the historical event ledger.**
3. GPT/model API linkage with its *own* auth and budget; this is distinct from
   ChatGPT session context and connected-app permissions.
4. Proof of Supabase Realtime state update / Drive receipt readback.
5. Exact-head CI, real ARMv7 APK installation + roundtrip E2E, and independent
   reviewer approval before any promotion.

## Rollback and falsifier

The implementation is additive in a branch/DRAFT PR. Rollback closes the
unmerged PR; no deleted source and no change to main. If any build/test fails,
preserve the failure in the PR and produce a corrective successor, never
relabel FAILED as PASS. A successfully exported JSON proves only that Android
SAF persisted that byte stream, not that GPT received or executed it.

SOURCE ≠ ARTIFACT ≠ EXECUTION ≠ EVIDENCE ≠ CLAIM.
`TOKEN_VAZIO` remains typed; `claim_allowed=false`.
