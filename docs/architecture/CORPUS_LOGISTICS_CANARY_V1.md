# Corpus Logistics Canary V1

Canary: `conversations-027.json` from the current NOVOexport Conversations corpus.

Observed provider identity: `1i02ona4EPpUszTWXAHnfoUqFGSUJgsPV`; observed size: 4,098,254 bytes.

This binding does not claim the current content SHA-256. The executor must read the exact source, verify byte count, calculate SHA-256, then parse.

Pipeline:

```text
Drive read-only source
→ DriveStagingGate/local readonly mount
→ SHA-256
→ RAFAELIA Navigator
→ Navigator→Gymnasia adapter
→ content-addressed logistics plan
→ 00_INDEX..09_CONVERSATION_CHUNKS tree
→ RafGitFS preview + dry-run + exact approval
→ private branch/PR
→ GitHub readback
→ receipt
```

Raw source body is not committed by this canary. The publication contains derived private metadata/chunks/indexes according to the corpus contract.
