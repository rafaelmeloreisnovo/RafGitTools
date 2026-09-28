# Corpus Logistics Canary V1

Canary: `conversations-027.json` from the current NOVOexport Conversations corpus.

Observed provider identity: `1i02ona4EPpUszTWXAHnfoUqFGSUJgsPV`; observed size: 4,098,254 bytes.

Exact source receipt reused from the private corpus: SHA-256 `32ecf289497a30799358ac53a794c2d198e8449592a28a19bb9bbab6ff254211`, 4,098,254 bytes, provider ID `1i02ona4EPpUszTWXAHnfoUqFGSUJgsPV`. The executor must still read back the exact object and verify byte count/hash before parsing; prior evidence is an anchor, not a substitute for current execution.

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
