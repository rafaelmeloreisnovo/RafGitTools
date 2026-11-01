# RAFAELIA Ω — Cloud Custody: GPT ↔ Drive ↔ RafGitTools ↔ GitHub

**STATUS:** source under DRAFT/HOLD; no human/provider approval implied by a CI success.
**Authority:** RafGitTools executes only its own hosted, bounded synthetic CI. Mapa is federation router. Drive holds source proposals and mirrored custody. Any producer's code remains governed by that producer.

## One directory, two logical directions

The **existing official Drive** folder is `02_MEIO_DE_CAMPO_GPT_DRIVE_GITHUB`, inside the existing `LAB_RAPIDTOOLS_OMEGA` directory. Avoid duplicate copies and directories that look synchronized when they are not.

| Logical endpoint | Single physical Drive directory | Meaning |
| --- | --- | --- |
| GPT[OUT] ≡ RAFFGITTOOLS[IN] | 10_INBOX_PROPOSTAS | Human intention, versioned request, exact source, desired test |
| Approval[HOLD/GRANT] | 20_AUTORIZACOES | Separate user authorization; no fabricated identity |
| Config[VERSION] | 30_PIPELINES_VERSIONADOS | Immutable producer-specific, whitelisted operations |
| RAFGITTOOLS[OUT] ≡ GPT[IN] | 40_OUTBOX_RECEIPTS | CI status and actual provider hashes, not guesses |
| Policy / security | 50_GATES_E_PRIVACIDADE | Authority, privacy, licensing, release and stop |
| Producers | 60_REPOS_E_CONJUNTOS | Referenced producer scopes only |
| SYSLOG[DIR] | 70_SYSLOG_CUSTODIA | Raw CI receipt ZIP mirrored with readback when authorized |
| HOLD[DIR] | 80_HOLD_REVISAO | Pending independent review; not an actual Vault lock |
| 001_PONTEIRO | Existing entry pointer | Routes and exact current state |

**No phone daemon, background ChatGPT or native Drive listener.** ChatGPT can author proposals in Drive during a user-initiated conversation; GitHub Actions executes from reviewed GitHub file/workflow triggers. The Drive file alone cannot launch GitHub CI. An unattended broker needs explicitly provisioned identity, least-privilege GitHub App/Google service account, trigger, replay protection and independent administrative review.

## Bounded real pipeline

GitHub workflow: `.github/workflows/rafaelia-middlefield-bridge-scoped.yml`, using pinned `actions/checkout` and pinned `actions/upload-artifact`, CI scoped to bridge paths and repository source HEAD (not synthetic merge SHA). The existing `bridge.py` validates a small whitelist of steps, rejects arbitrary shell and executes only LOCAL_CI_READONLY. No new compiler or generic agent.

`cloud_custody.py --start`: records UTC start time, GitHub run/attempt, source SHA and monotonic start. The existing CI runs controlled tests. `--finish --status <job.status>` records UTC end, monotonic elapsed milliseconds, exact CI status, PAUSED_AT_REVIEW_GATE (runner not kept alive), no delivery or deletion, and produces a local JSON receipt.

The workflow uploads `start.json`, `custody-receipt.json`, and bounded `bridge-receipts/*.json` to GitHub Actions artifacts with a requested **14-day maximum**; the observed provider actually expires the first artifact on **2026-11-11** (about ten days). Provider policy overrides the requested value. Do not rely on GitHub for long-term custody. This is *not indefinite*: prompt Drive mirror and server metadata readback are needed to preserve beyond GitHub expiration. The Drive side requires an authorized live connector operation, not a fictitious cloud listener. Artifact hashes in GitHub represent bytes associated with that upload; custody copy must be independently validated before a stronger claim.

### Safety guarantees and non-guarantees

- If the run fails, the receipt says FAIL_HOST_CI. A passed build or tests are not provider protection, physical device proof or scientific truth.
- `claim_allowed=false`, `release_allowed=false`, `deletion_allowed=false` remain fixed. Any attempted promotion fails the guard.
- **PAUSED** means no further release or privileged action; it does not mean holding a billable CI runner idle.
- No provider-attested approval identity is available from a plain chat message or JSON; actor approval and independent assessor credentials remain typed TOKEN_VAZIO.
- Hash-linked receipts are **tamper-evident only if protected independent copies and hash anchors exist**; GitHub or Drive owners might still delete/edit them.
- Google Vault holds may provide actual retention in a qualifying Workspace deployment, if an authorized administrator explicitly applies and verifies them to eligible files; a Drive folder alone cannot promise legal hold. A hold may still permit file edits while versions are preserved. 
- GitHub Actions itself is **a build executor, not an accredited certification agency**. An artifact attestation is supply-chain provenance, not a regulatory conformance certificate. External review needs the relevant standard, scope, qualified independent assessor, report ID and verified signature.

## Lifecycle FOFI → FIFO (no endless loops)

`RESULT → FOFI(source_sha, run, inputs, missing evidence) → FIFO(exact source, allowed steps, receipt) → HOLD`.

Condition of completion for **this scoped cloud pipeline**: exact head CI completed and actual artifact exists; safe receipt hash recorded and Drive custody readback completed if possible. If Drive write permission unavailable, state BLOCKED_DRIVE_CUSTODY; no promise of continuous bridging. Independently certified and immutable retention remain BLOCKED until authorized provider gates.

Rollback: close unmerged branch or PR; existing Drive folders and historical receipts remain unchanged. No automatic merging, moving, deleting or revoking.

R3 = <CI source and checks, provider-enforced retention and identity gaps, exact-head CI + mirror + independent approval gate>.
