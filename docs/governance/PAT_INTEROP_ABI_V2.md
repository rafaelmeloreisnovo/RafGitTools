# PAT interoperability + ARM32/ARM64 freestanding — V2

**State:** `SOURCE_CANDIDATE / PROVIDER_TOKEN_METADATA=TOKEN_VAZIO / DEVICE_RUNTIME=TOKEN_VAZIO / claim_allowed=false`.
Executor: `rafaelmeloreisnovo/RafGitTools`. Federation routing authority: `rafaelmeloreisnovo/Mapa`.

## Canonical provider labels — no secret values

`PAT_ACTIONS`, `PAT_AGENTS`, `PAT_CODESPACES`, `PAT_DEPENDABOT`, `PAT_ENV`, `PAT_ENVIRONMENTS`.

**Distinct:** `PAT_ENV != PAT_ENVIRONMENTS`. The current START.yml uses GitHub Environment display `Pat_environments`, containing the separately named secret `PAT_ENV`. The upper-case `PAT_ENVIRONMENTS` display is the proposed canonical naming policy, **not** proof the Environment has been renamed. The separately reported `PAT_ENVIRONMENTS` **secret** is unwired, scope/readback unknown. Preserve exact historic bindings and never silently substitute one PAT for another.

`PAT_DEPENDABOT` is a proposed privileged alert-access role, **not** automatically a secret for Dependabot update jobs. Dependabot-triggered workflows do not receive ordinary Actions secrets. For private registries, use distinct Dependabot secrets scoped to the registry; normal update runs should use the built-in `GITHUB_TOKEN` and review-only updates. GitHub App installation tokens with exact repository scoping are preferable to broad long-lived personal tokens for automation.

## Minimal wizard policy

1. Read-only inventory of reachable repositories, with metadata kept private. A connected GitHub list returned **97 owner-affiliated repositories** on 27 Oct 2026; exhaustive organization coverage and actual PAT scope have not been proven.
2. For each repository and action, show only detected **secret identifier, storage scope, provider permission metadata, expiration, exact target SHA, and provider readback state**; never expose token values.
3. Automatically plan eligible read-only checks. Gate all modifications behind explicit one-time owner approval, separate restricted Environment, least privileges, rollback plan, and exact provider readback. No implicit all-repo mutation or credential fallback.
4. Failure, timeout, unavailable PAT or missing permissions yields `TOKEN_VAZIO/HOLD` (local metadata-only inspection is allowed). Never convert a failed operation into success or route privileged credentials to untrusted PRs.

## System boundary

```text
Android DEX/Kotlin thin UI shell (system APIs/OAuth; no hosted PAT baked into APK)
          ↓ request intent, preview, explicit approval
Trusted GitHub Actions + capability broker (HTTPS, GitHub API, protected environment)
          ↓ typed operation and externally attested metadata
Freestanding C11 L0: ELF32 ARM EABI / ELF64 AArch64
          ↓ branchless-in-C bit masks, no libc / heap / GC / I/O / syscalls
Result: READ-CANDIDATE or HOLD — never provider authorization or mutation
```

Low-level source: `native/pat_interop_l0_v2/`. Validation: `scripts/validate_pat_interop_abi_v2.py`, `tests/test_pat_interop_abi_v2.py`. The sole START.yml now verifies both code objects in an **offline source-only** step, without tokens. There is no claim of branchlessness at the machine instruction level, zero overhead, physical performance or secret access. Keep a stable ABI and meaningful symbol names/comments for review. ASM/NEON/inline may be introduced **only** after differential benchmarks and ARM32/ARM64 device evidence, not as decorative complexity.

**Hard boundary:** an actual Android APK, GitHub integration, networking, storage or credentials necessarily uses Android/OS/HTTPS interfaces. No-syscall applies only to the pure compute/decision core. Never invent a custom cryptographic scheme to transport credentials.

## Exact criteria

- 6 unique canonical names, with no implicit alias and no action behind an unproven permission;
- positive and negative metadata-only contract tests;
- `ELF32 ARM EABI5` and `ELF64 AArch64` objects built using `-ffreestanding -fno-builtin`, no external undefined symbol;
- actual provider existence/permissions and GitHub Ruleset/Environment enforcement remain `TOKEN_VAZIO` until a permitted readback;
- instrumented ARM32/ARM64 device execution and application functionality remain `TOKEN_VAZIO`;
- merge/promotion requires full exact-head CI + independently authorized reviewer + provider branch-gate readback, no bypass.

This change is stored on an isolated candidate branch, **not** merged to main. No PAT value, remote secret, repository visibility, branch rule, release or external repo was modified. Historical contracts remain intact. Rollback: abandon the branch or create explicit revert commit; never rewrite evidence.
