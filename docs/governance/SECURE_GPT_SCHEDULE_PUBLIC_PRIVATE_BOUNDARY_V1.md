# RAFAELIA | GPT schedule, public CI and private custody V1

Date: 2026-10-27. Status: HOLD / branch-only candidate; no activation.
Authority: RafGitTools executor; Mapa private router; Google Drive private custody.
PV: SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM.

## Boundary
Public RafGitTools Actions run only public source. A public hosted runner must never fetch, decrypt, process, or upload private repo/source data. Encrypting CI artifacts or masking secrets is NOT a substitute for private execution. No private source identifiers, raw user messages, keys, PAT values, signed cookies, agent input, file paths, or private hashes in public logs, caches, annotations, summary, artifacts, PRs or workflow inputs.

Eight existing secret-bearing jobs are disabled explicitly in the START.yml candidate; historical content remains for review/rollback. The existing schedule 35 3 * * * UTC is retained. Only START.yml remains an active workflow. GitHub scheduled actions run only after merge to the default branch; there is no automatic merge in this change.

## Routes
1. GPT scheduled observation reads minimum CURRENT_STATE and authorized exact-reference GitHub metadata. This is planning, not autonomous authorization or credential custody.
2. Private Drive/GitHub keep all sensitive sources, encrypted payloads and full receipts. Mapa private stores typed references and audit state.
3. Termux/owner-controlled worker is the only future private executor: scoped GitHub App installation token (preferred), or owner-managed fine-grained, short-lived PAT scoped to an exact repository. Secrets stay within private vault or Android keystore. A proposal may reference PAT_AGENTS as a role, never transmit its value. Use maintained authenticated cryptography; no ad hoc crypto.
4. RafGitTools public CI verifies public source plus fail-closed static boundary and redacted local scan. Only bounded public counters may be disclosed.
5. A private result can only be exported after explicit human-approved sanitization and reidentification/correlation review.

## Private queue contract (proposed, not implemented)
request_id | exact_ref | source_hash | allowed_action | actor | expiry | nonce | human_authority | data_class | receipt_ref.
Require signed scoped envelope, replay protection, expiry and effective permission readback before execution. Missing authority => BLOCKED/TOKEN_VAZIO. No GitHub Actions execution in private repositories.

## F-gaps
- The new tracked-tree scanner now returns INCOMPLETE+nonzero for oversize, unreadable, symlink or binary. This does NOT repair historical Mapa scanner; it closes only the new scanner's silent-skip failure mode.
- History scanning, Git remotes, provider secret alerts, complete large-file inspection, PAT permission metadata and private Android runtime remain NOT_RUN / TOKEN_VAZIO.
- Public structural CI test is not verification of effective provider protection, private handoff encryption, legal compliance or physical execution.

## Gate before production
Human review of exact-HEAD diff and secrets, offline tests, public provider branch/ruleset enforcement and negative merge test, private clone history scan, private executor and secret-provider readback, legal rights check. No auto-merge. claim_allowed=false.

## Rollback
Leave HOLD branch unmerged. If merged by separate authorized review, revert via documented successor without reactivating legacy private jobs or publishing data. Preserve receipts append-only.
