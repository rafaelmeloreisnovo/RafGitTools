# RAFAELIA Ω — Event Router / Inbox V1 (DRAFT · HOLD)

Status: **IMPLEMENTED_UNTESTED** until an exact-HEAD CI receipt is observed.
Producer: `rafaelmeloreisnovo/RafGitTools`; authority for federated routing: `Mapa`.
Scope: metadata-only, offline validator and negative security tests. **No broker deployed.**
`SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM`; `claim_allowed=false`.

## Purpose

Connect future event *sources* to the already-existing Agent Workspace V1 and
Middlefield bridge without creating an unreviewed executable command lane.

- ChatGPT/user and RafGitTools APK may submit a **proposal** referencing a
  separate, source-bound interaction ID and intent digest.
- Gmail, Google Calendar, GitHub and Supabase may contribute **observations**.
  Message bodies, event descriptions, free-form instructions, tokens and private
  attachments MUST NOT become executable command payloads.
- The current gate validates shape, bounds, expiry and authority placeholders;
  it neither verifies identity nor dispatches anything.
- Canonical navigation: Drive CURRENT_STATE Ω → START HERE Ω V2.2 → Mapa route
  → RafGitTools executor. This file does not supersede any of those authorities.

## Actual first slice

`event_router_v1.py`: Python standard-library **host adapter**, NOT a
freestanding C core, Edge Function, APK service, OAuth client or live watcher.

```sh
python3 -m unittest discover -s LAB_RAPIDTOOLS_OMEGA/bridge/tests -p 'test_event_router_v1.py' -v
python3 LAB_RAPIDTOOLS_OMEGA/bridge/event_router_v1.py synthetic-envelope.json
```

An envelope has a fixed JSON key set. Origin values:
`GMAIL_METADATA`, `GOOGLE_CALENDAR_EVENT`, `GITHUB_EVENT`,
`SUPABASE_EVENT`, `GPT_USER_INTENT`, `RAFGITTOOLS_APK`.
Only human sources can request `PREPARE_CI_READ` or
`PREPARE_NOTIFICATION`; these still **do not dispatch**.
Every other source permits only `OBSERVE_EVENT`.
All inputs demand `PENDING_HUMAN`, `NOT_EXECUTED`,
`METADATA_ONLY`, `authority_ref=TOKEN_VAZIO`, `claim_allowed=false`.
Receipt includes event SHA-256 and an *advisory* idempotency key.
A digest is an integrity address, not source authentication or authorization.

## Proposed runtime architecture (NOT INSTALLED)

1. **Collectors:** Gmail API watch/history or bounded authorized polling;
   Google Calendar events/watch or authorized polling; GitHub signed webhook
   (verify HMAC and delivery ID); Supabase database trigger/webhook/queue.
   ChatGPT Automations can generate scheduled reports or interactive actions,
   but do not provide an always-open external message loop for this application.
2. **Private broker:** Supabase Edge Function or trusted service with verified
   provider identity, signature, tenant/owner scope, allowlist and rate limits.
   Never give service-role keys, personal tokens or webhook signing secrets to
   the APK, email, public repo, Drive or a client.
3. **Durable inbox:** private schema, append-only source receipt, unique
   `(origin_kind, origin_event_id_digest, operation)`, statuses
   `OBSERVED → PROPOSED → WAITING_APPROVAL → APPROVED → QUEUED →
   LEASED → FINISHED/FAILED → RECEIPTED`; TTL, max attempts, dead-letter
   queue, lease expiry, transactional idempotency and compensating successors.
   The present `raf_private.rafdb_federated_events_v1` is metadata history,
   **not** a durable execution queue.
4. **Approval:** human acts in an authenticated UI; broker verifies actor,
   scope, expiry, exact intent digest, repository/commit, anti-replay token,
   authorized capability and selected executor. Gmail sender or calendar
   organizer alone is not execution authority. Never infer approval from a
   string in a document or notification.
5. **Executor:** allowlisted `workflow_dispatch`/repository_dispatch or
   installed RafGitTools/Polimata/Browser adapter with platform-specific
   permissions. Return a source-bound run ID, status, SHA, environment, bounded
   logs, exact artifact reference and evidence scope. No `sh -c` of inbox text.
6. **Feedback:** result → Supabase private receipt → Drive μWRITE and index
   → queried by the GPT in an authorized user turn or by a configured,
   permitted scheduled summarizer. A notification opens a reviewed Android
   deep link; it does not run arbitrary CLI commands just by opening the app.

## Illustrative run, NOT production evidence

```text
Calendar "revisar build" → metadata event digest → OBSERVED
  → RafaGitTools proposal "PREPARE_CI_READ" → WAITING_APPROVAL
  → authenticated approval for exact source+action+expiry
  → authorized GitHub Actions read-only job → run_id+SHA receipt
  → Supabase state → Drive μWRITE → GPT reads result on next turn
```

Scheduled Gmail *sending* is a separate authenticated, permissioned action:
draft → explicit recipient/scope/approval → schedule → revalidate approval at
send time → Gmail provider receipt. No email was sent or draft created by
this PR. Google Calendar events are reminders/data, not direct OS interrupts;
Android notifications/deep links require separate user- and OS-authorized code.

## Failures the V1 tests actively reject

Unknown/extra JSON fields, raw email bodies, executable capabilities from
external sources, spoofed user intent, invalid digests/UUIDs, missing expiry,
time-travel, duplicate keys, NaN, replay-equivalent source identifiers and
promotion states that claim approval or execution.

## Evidence / rollback / next gate

- Source: PR branch HEAD and three paths (validator, adversarial tests, doc).
- Local test + exact-head provider CI + actual Android test: **TOKEN_VAZIO**
  until independently observed. A PR merge does not prove E2E functionality.
- Rollback: close this draft PR; `main` and existing agents, Android Manifest,
  approvals, CI executor and Supabase tables remain unchanged.
- Next isolated step: authenticated broker with **synthetic** events and
  dedicated private queue migration, staged behind explicit review; install
  actual Gmail/Calendar collectors only after their permission/privacy gates.
- No automatic merge, sending, scheduling, device launch, remote command,
  CI privileged runner or secrets management in this scope.

Reference: Supabase Database Webhooks, Cron/pg_net, Scheduling Edge Functions.
Provider docs must be rechecked before deploying: API behavior can change.

## Successor clarification: policy-preauthorized read-only triggers (V1.1)

An external signal **may select a trusted, predefined low-risk command** without
a separate human approval for every event. Trust is scoped to the *operation*,
not to arbitrary prose inside an email or calendar event.

Implementation: `bounded_lookup_v1.py` plus
`tests/test_bounded_lookup_v1.py`. It validates the existing
`OBSERVE_EVENT` envelope, selects an immutable policy and emits a typed
bounded `GET` command descriptor. Current policies:
- `GITHUB_ACTIONS_PUBLIC_STATUS_V1`: fixed public GitHub Actions status URL,
  at most five runs per response.
- `RAFGITTOOLS_PUBLIC_GUIDE_V1`: fixed public RafGitTools README URL.

The output is `ALLOW_BOUNDED_READ_CANDIDATE` with
`per_event_human_approval_required=false`. This permits an authenticated
broker to use **standing human-approved policy** rather than interrupting the
user for each read. It does not imply `dispatch_allowed=true`: the current
offline source has NO provider authentication, durable deduplication, enforced
rate limit, runtime sandbox or network execution. Those remain required in the
separately trusted broker before a live request is executed.

Risk-based trust matrix:
- **L0** public fixed-target metadata lookup: standing policy + verified
  origin/tenant + replay/rate controls; no per-event human confirmation.
- **L1** private read / limited diagnostic: explicit scoped capability,
  least-privilege credentials and data classification, preapproval if allowed.
- **L2** sending mail, modifying files, running native program or creating jobs:
  authenticated actor and specific authorization for the action/scope.
- **L3** deletion, external publication, releases, secrets or rights changes:
  strengthened human/provider authorization, irreversible-effect gate and
  rollback/failback where possible.

Example:
`Calendar review event → authenticated provider event ID → fixed GitHub
Actions status lookup → bounded response → typed receipt → optional GPT
summary`. A future broker can run the lookup at the scheduled time without
manual repetition. It must never reinterpret the calendar *description* as
`sh -c` or allow an arbitrary event URL, recipient or GitHub repository.

The policy file is public; the user's specific authorization, account mapping,
credentials, callback verification and quota counter belong in private runtime
config, not this repository. This change creates no unattended monitor or
message listener. CLAIM scope remains `IMPLEMENTED_UNTESTED` until exact-head
CI. User intent and source authentication are separate from content parsing.

## Broadcast / scheduled self-reminder as communication pointer (V1.2)

The word **broadcast** here means a constrained event fan-out, not trust in
email content. An email scheduled to oneself, a Calendar event, a GitHub event
or a Supabase event can signal a known semantic route. The durable state is
stored in Supabase receipts and indexed in Drive; **Gmail is not the queue**.
It is also possible to trigger the route from RafGitTools or an authorized
GPT conversation without email.

`route_relay_v1.py` adds two hardcoded, reviewable route IDs:
- `MIDDLEFIELD_STATUS_READ_V1` -> scoped public workflow status read,
  **no** GitHub dispatch grant needed.
- `MIDDLEFIELD_BRIDGE_CHECK_V1` -> resolve the verified canonical
  START HERE Ω V2.2 document ID, then **propose** a dispatch of the actual
  `.github/workflows/rafaelia-middlefield-bridge-scoped.yml` workflow on
  `main`. This workflow has `workflow_dispatch`, but its invocation is
  NOT a public read: it compiles, runs tests and uploads artifacts. It
  requires separately verified standing authorization, GitHub permission
  and exact repository/workflow/ref policy before any dispatch.

A self-reminder's subject may be a non-authorizing *hint*:
`RAFAELIA-ROUTE-V1 MIDDLEFIELD_BRIDGE_CHECK_V1`. The subject is not a
signed capability, so the authenticated broker must map its **verified**
Gmail provider account, event ID and approved sender/recipient scope to an
already installed policy and deduplicate repeated deliveries. No mailbox
search, scheduled message, message body or header gives permissions alone.

Proposed delivery path, not deployed:

```text
scheduled self-email or Calendar event [signal]
  -> authenticated Gmail/Calendar collector with verified account scope
  -> private Supabase inbox: unique provider-event-id digest, expiry, quota
  -> fixed route ID: known Drive doc + known GitHub repo/workflow/ref
  -> validate standing grant and exact policy version
  -> [status GET] OR [workflow dispatch with scoped permission]
  -> GitHub run_id/head_sha/conclusion + provider request receipt
  -> Supabase append-only ledger -> Drive NEXUS μWRITE and CURRENT_STATE
  -> RapidTools Android inbox notification; GPT reads status on user turn
```

The inbox can be shared as a stateful message channel; **GPT is not a
perpetually running process**. Scheduled mail is best-effort provider
delivery and cannot be relied upon as an exactly-once clock. Prefer
Supabase/Google scheduler for authoritative timing, using email as
optional user-readable notice and catch-up signal. A scheduled self-email
must never start privileged execution simply because its subject matched.

The offline module's `per_event_confirmation_needed_if_standing_grant_valid=false`
means the same **specific approved workflow** could be run again without
individual approvals while the separate private grant is valid. This module
does not create or verify a grant; it reports
`BLOCKED_PENDING_BROKER_AND_STANDING_GRANT`,
`dispatch_allowed=false`, `NOT_EXECUTED`, `claim_allowed=false`.
No Gmail email scheduled or sent, no Drive read triggered, no GitHub pipeline
dispatched by this code, and no Android notification delivered.
`tests/test_route_relay_v1.py` contains synthetic security tests.

Never place Gmail OAuth tokens, Google Drive private file contents, Supabase
service role keys, or GitHub write tokens in a public repository or an email.
Use per-provider scoped credentials in the private broker, typed receipts,
anti-replay and privileged-action limits. SOURCE≠ARTIFACT≠EXECUTION≠EVIDENCE≠CLAIM.
