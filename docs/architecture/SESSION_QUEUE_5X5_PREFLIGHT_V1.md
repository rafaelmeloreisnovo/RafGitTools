# Session Queue 5x5 — preflight V1

**Status:** IMPLEMENTED_SOURCE / LOCAL_SYNTHETIC_TESTED; CI exact-head pending.
**Claim gate:** claim_allowed=false. No deployment or GitHub Actions dispatch.

## Architecture

Reuse the preexisting RafGitTools session resolver, Mapa as route authority,
and private Drive for canonical queue custody. Avoid a second general-purpose
job envelope: this module checks only a minimal, public-safe, synthetic
PLAN_ONLY intent. The GitHub public repository never contains private Drive
folder IDs, private repository names, payloads, tokens, raw logs or cryptographic
keys. Every private/public crossing requires a separately approved broker.

## Five checks — personalized GPT engineering contract

1. G1 ROLE/OBJECTIVE: Mapa routes; RafGitTools checks; owner repository executes.
2. G2 AUTHORITY/PROVENANCE: pin Mapa source commit and Git blob.
3. G3 SECURITY/RIGHTS: unknown classification blocks, child protection and
   human dignity are non-compensatory; no personal material crosses a boundary.
4. G4 RISK/REVERSIBILITY: branch-only, no destructive deletion or automatic
   promotion, with an explicit rollback path.
5. G5 ACCEPTANCE/EVIDENCE: SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM;
   passing a structural test does not authorize a private runner.

## Five checks — isolated project sessions

1. S1 IDENTITY: opaque fixed-shape session, target and envelope IDs.
2. S2 CONTEXT BOUNDARY: idempotency digest includes session ID; no cross-session
   replay accepted with unchanged digest.
3. S3 ROUTE: Mapa Session AI Dispatch remains external authority, pinned to
   commit ad2efc2b9a4599bdcc37ab416c84bd99dc2a1e09 and blob
   c1d1d8f1692c74957f8b70968fa863f855d93cb0.
4. S4 EXECUTION: assignment != execution; PLAN_ONLY and
   execution_enabled=false are mandatory.
5. S5 RETURN: private receipts and reconstructible event chain are designed for
   a later broker; preflight cannot invent completion evidence.

The full private text of GPT/Project settings is not retrievable through a
verified settings API here. These are operational passes over known contracts,
not ten automatically launched chat sessions.

## Flow

    session -> bounded intent -> Mapa route -> Drive private queue
            -> RafGitTools offline preflight -> HOLD/gate
            -> private authenticated broker (NOT IMPLEMENTED)
            -> authorized execution (NOT RUN)
            -> private verified receipt (NOT RUN)

Drive folders are state views (IN, GATES, PROCESSING, OUT, PROCESSED, ERROR,
HOLD), not database transactions. No file-moving worker is implemented.

## Preflight V1 implemented here

- scripts/validate_session_queue_preflight_v1.py
- tests/test_session_queue_preflight_v1.py

No network calls, subprocess, GitHub API, Google Drive API, CI trigger or file
writes are performed by the validator. It reads at most 4096 input bytes,
allows exactly fifteen fields, returns bounded error codes without echoing
input, and refuses states PROCESSING, OUT and PROCESSED.
Only IN, GATES and HOLD can be represented here.

The domain-separated SHA-256 idempotency digest is ONLY a correlation checksum;
it is NOT a signature, MAC, secret proof, encryption or origin authentication.
Opaque ID format is NOT evidence of randomness/anonymity. A future broker must
generate identifiers with a CSPRNG and enforce authorization, expiration,
nonce uniqueness, replay fences, least privilege and separated key custody.

## Existing assets and risks

- RafGitTools AGENTS.md: Q01-Q12, local authority, minimal source and rollback.
- RafGitTools configs/session-agent-dispatch.v1.json +
  scripts/resolve_session_ai_work_packet.py: routing structure only.
- Mapa data/control-plane/SESSION_AI_WORK_DISPATCH_V1.json: federated authority.
- RafGitTools docs/PRIVATE_CI_BRIDGE_METHOD_V1.md:
  IMPLEMENTED_UNTESTED_NETWORK_ISOLATION_V2.
- PAT_ACTIONS documented as WIRED_MAIN_ONESHOT_READ_ONLY; user's changed
  Actions workflow read/write setting does NOT prove permissions for PAT_ACTIONS
  or GitHub App. No automatic CI dispatch authorized here.
- ZIPRAF container != validated encryption. A future private broker may use
  standardized authenticated encryption with managed keys, after a proper
  test/approval gate. None is implemented by this PR.

## Verification and acceptance

    python3 -m unittest discover -s tests -p test_session_queue_preflight_v1.py -v

Test cases: correct synthetic intent, cross-session replay, unauthorized
extra fields, private body, execution flag, Mapa pin mismatch, unknown
privacy, illegal processing state, non-opaque ID, state transitions and
boolean type confusion. All are synthetic. Hosted-runner exact-head proof,
broker integration, cryptographic envelope, replay against provider and
private CI isolation are separate evidence gates.

No changes to AGENTS.md, START.yml, PAT secrets, branch protection or private
repositories are within this change's scope.

## Rollback / R3

Rollback: close this Draft PR without merging; if merged later after human
review, revert the exact scoped files through a new reviewed commit. No previous
object is deleted. This proposal changes no active queue or CI deployment.

F_ok: minimal preflight source and synthetic local checks.
F_gap: E2E private broker, provider permission readback, runner isolation,
AEAD/signature, confidential round-trip, fail/retry receipt and independent
review remain TOKEN_VAZIO.
F_next: run focused tests at the exact PR SHA in permitted CI, compare
local/remote results, then validate a synthetic private broker separately.
