# Middlefield capability matrix

| Producer | Inspect | Stage proposal | Execute CI | Remote WRITE/admin |
|---|---|---|---|---|
| RafGitTools LAB | YES | Chat+GitHub connector | LOCAL_CI_READONLY | BLOCKED |
| Mapa/Federação | YES | POINTER_ONLY | NO | BLOCKED |
| RLL | YES | POINTER_ONLY | NO | BLOCKED |
| Termux private phone | User local only | Synthetic contract | NO REMOTE | BLOCKED |
| Google Drive | Connected documents | INBOX/APPROVAL/OUTBOX docs | Not executable | Authenticated in-chat write only |

GitHub K-Secrets never become global identity. Repo, path, action, version, expiry, revocation and provider verification are separate.
Request state is PROPOSED then REVIEWED then HUMAN_APPROVED then CI_LOCAL_EXECUTED then EVIDENCED then RECONSTRUCTED.
This prototype demonstrates PROPOSED/HOLD and safe CI, not provider-enforced human approval or Android broker.
Pipeline hash changes invalidate old proposals; forged approval file cannot grant privileged actions.
All remote writes, destructive file operations, shell execution, third party data copying, bypass of branch protection and auto merge are DENIED.
Credits for foreign UI libraries require a screen-specific notice and version-identified SBOM; no third-party code copied into bridge.
CLAIM_ALLOWED=false. TOKEN_VAZIO marks missing authority and evidence.
