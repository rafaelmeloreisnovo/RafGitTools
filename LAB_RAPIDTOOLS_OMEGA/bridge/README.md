# RAFAELIA Ω — Meio de campo operacional V1

INTENT → DRIVE_INBOX → REVIEW → CHAT_APPROVAL → GITHUB_PIPELINE_VERSION → CI_EXECUTION → RECEIPT → DRIVE_OUTBOX → MAPA_INDEX.

## Real interactive chat path
1. User tells ChatGPT to propose/change a LAB pipeline.
2. In the same interactive conversation, ChatGPT can write an official Drive proposal using the connected Drive tool and verify provider readback.
3. ChatGPT can write a matching versioned pipeline + typed request into this GitHub LAB branch using the GitHub connector.
4. User says AUTORIZO REQ-... [exact scope] here. Only then can a fresh matching approval reference be recorded by the ChatGPT connector.
5. CI validates the SHA-256-bound request/pipeline and executes ONLY known LOCAL_CI_READONLY commands. A new pipeline version invalidates older request/approval hashes.
6. ChatGPT reads CI logs and writes an indexed receipt to Drive during an authorized turn. No automatic Drive listener or always-on GPT process exists.

## Operational files
- pipelines/lab-quality.v1.json: versioned editable pipeline steps, not arbitrary shell.
- inbox/REQ-0001.json: first real PROPOSED / HOLD request; never a permission.
- bridge.py: request, pipeline, approval and receipt contracts.
- tests/test_bridge.py: nine strict falsifiers for authority, replay, fields, expiry and stale request.
- docs/CAPABILITIES.md: producer access and per-repository boundaries.
- .github/workflows/rafaelia-middlefield-bridge-scoped.yml: read-only CI that emits real receipts.
- Drive: Aplicações Rafael IA/RafGitTools/LAB_RAPIDTOOLS_OMEGA — ARM32 ARM64 Atlas/02_MEIO_DE_CAMPO_GPT_DRIVE_GITHUB.

## Execution modes
python3 LAB_RAPIDTOOLS_OMEGA/bridge/bridge.py --check-request
  => HOLD_APPROVAL_REQUIRED, no requested action executed.
python3 LAB_RAPIDTOOLS_OMEGA/bridge/bridge.py --run-safe
  => CI-local lab validation only, with receipt; not user request fulfillment.

## Limits / security
- Remote GitHub admin, agents, Termux, private RLL/Mapa writes and application deployment are BLOCKED.
- K-Secrets/PATs are never read, copied or shared. A secret alias is not authorization.
- Chat approval record in a repository is not cryptographic proof of human identity; provider-enforced authority is needed for privileged actions.
- Drive does not automatically transmit files to GitHub Actions; the connector writes are live chat operations only.
- No false claims of a bidirectional real-time bridge, 100% third-party telemetry or Android runtime proof.
- SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM. TOKEN_VAZIO != 0.
- claim_allowed=false; release_allowed=false; no auto-merge.
