# Human-approval envelopes
This directory is EMPTY of approvals. Creating a proposal NEVER implies approval.
Only after explicit chat authorization bound to request SHA, pipeline SHA, scope and short expiry may the connected assistant stage an approval file with exact name REQ-NNNN.json.
Repository-supplied approval records alone do NOT cryptographically prove the human speaker. CI accepts them for bounded LOCAL_CI_READONLY stages only; remote/secret-bearing capabilities remain BLOCKED.
Supersession requires a new file, never copy an expired or stale approval across pipeline revisions.
