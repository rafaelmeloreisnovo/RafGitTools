# Repository factory — START single-workflow bridge

The START workflow is the only active YAML workflow. Mode `repository_factory` produces an offline planning receipt without requesting a PAT or modifying GitHub. The focused deterministic factory test must execute in the coherence job, including fast source-only PRs.

Repository creation is intentionally NOT wired to the START plan lane. The existing Android personal-private creator remains untouched. Any future application of a PAT to provider write requires operator confirmation of the exact owner/name, source rights, permissions, environment review, readback and a non-ambiguous rollback. No secret value enters Drive, Mapa, artifacts or logs.

Tracking: RafGitTools PR #658 and issue #659. The scoped plan is NOT a proof of repository creation, provider token scopes, org authorization, protection rules, or scientific claims. `claim_allowed=false`.


## PAT_AGENTS provider-readback candidate (successor, October 8, 2026)

Mode `repository_factory_pat_preflight` is proposed on the same `START.yml`, in a **separate manual owner-only job** protected by the existing `Pat_environments` Environment. It references only `secrets.PAT_AGENTS` for this explicit preflight; it does not reuse, alias, print, hash, or fall back to `PAT_ACTIONS`, `PAT_ENV` or `PAT_ENVIRONMENTS`.

What the job performs: bounded `GET /user` for authenticated identity and, only for owner `instituto-Rafael`, `GET /user/memberships/orgs/instituto-Rafael` for membership status. The transport forbids any POST, PUT, PATCH, DELETE, redirects and non-allowlisted paths. No repository names or source code are sent to the provider. Receipts contain typed states and identifier names only, not token values or returned identity details.

Observed org-admin membership does **not** prove organization repository-creation authority, PAT `Administration: write` grants, or org policy allowance. Those gates remain `TOKEN_VAZIO_NOT_TESTED` until independently established. A missing or rejected secret is `BLOCKED_SECRET_UNAVAILABLE` / typed readback gap; never convert that into an implicit fallback.

Execution requires a real manual `workflow_dispatch` from `main`, GitHub actor `rafaelmeloreisnovo`, source topology and coherence PASS, and applicable environment protections. Only the permitted `PAT_AGENTS` credential is passed to the provider-readback step, not to the Python unit tests. The connection in this chat does not expose an Actions dispatch operation; **SOURCE_IMPLEMENTED does not imply PREFLIGHT_EXECUTED**.

To check the code in PR CI, run `python3 -m unittest discover -s tests -p 'test_repository_factory_pat_agents_preflight_v1.py' -v` (also in the START coherence lane). Follow-up: perform an authorized preflight, verify the receipt, and only then consider a separately governed create-permission gate. Manual `repository_factory` mode continues to be offline PLAN ONLY.

R3: source and deterministic tests proposed; existing PAT access unchanged; no additional repo, publication, provider write, license change or scientific claim performed.
