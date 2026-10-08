# Repository factory — START single-workflow bridge

The START workflow is the only active YAML workflow. Mode `repository_factory` produces an offline planning receipt without requesting a PAT or modifying GitHub. The focused deterministic factory test must execute in the coherence job, including fast source-only PRs.

Repository creation is intentionally NOT wired to the START plan lane. The existing Android personal-private creator remains untouched. Any future application of a PAT to provider write requires operator confirmation of the exact owner/name, source rights, permissions, environment review, readback and a non-ambiguous rollback. No secret value enters Drive, Mapa, artifacts or logs.

Tracking: RafGitTools PR #658 and issue #659. The scoped plan is NOT a proof of repository creation, provider token scopes, org authorization, protection rules, or scientific claims. `claim_allowed=false`.
