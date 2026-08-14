# RafGitTools — Canonical Readiness Gate — 2026-08-12

Status: **IMPLEMENTED_ADVANCED / VERIFIED_LIMITED**  
Claim boundary: **`claim_allowed=false` until APK hash + physical-device smoke exist for the reviewed head**.

This document supersedes historical "ready for delivery" wording **as an evidence statement**. Historical documents remain preserved for provenance; they are not deleted or rewritten retroactively.

## 1. What "ready" means here

RafGitTools has two distinct readiness levels:

### A. Operational Git/GitHub source readiness

Required:

- repository source/contracts present;
- local truth validator passes;
- PAT login source path present;
- GitHub OAuth Device Flow source path present (Client ID is runtime configuration);
- `gh` CLI import source path present;
- JGit core source path present;
- no known P0 source contradiction.

### B. Verified Android runtime readiness

Requires **all** of A plus:

- JDK 17 evidence;
- Android SDK evidence;
- unit tests PASS on the reviewed head;
- `devDebug` APK produced;
- APK SHA-256 recorded;
- authorized physical device identified;
- explicit install/start smoke PASS on that device.

The local gate does not silently turn missing runtime evidence into success.

## 2. Canonical local gate

Run from the repository root:

```bash
chmod +x scripts/rafgittools_readiness_gate.sh
./scripts/rafgittools_readiness_gate.sh
```

It writes an append-only local evidence receipt under:

```text
.rafgittools/receipts/readiness-<UTC>.tsv
.rafgittools/receipts/readiness-<UTC>.tsv.sha256
```

The directory is intentionally gitignored because receipts may contain local paths, device serials and environment metadata. A curated/sanitized receipt may be copied into a dedicated evidence location only by an explicit action.

### Verify access to the private RafGitTools repository from Termux

This check proves that the local `gh` credential can authenticate and read repository metadata **without printing or persisting the token**:

```bash
chmod +x scripts/rafgittools_private_auth_check.sh
./scripts/rafgittools_private_auth_check.sh
```

It records only authenticated login and non-secret repository permission booleans (`pull/push/admin`) in a local hashed receipt. It never calls `gh auth token`, never prints an Authorization header and never stores the credential.

### Optional physical-device smoke

The gate **never installs an APK by surprise**. To explicitly permit install/start:

```bash
RAFGITTOOLS_DEVICE_SMOKE=1 ./scripts/rafgittools_readiness_gate.sh
```

This requires a previously authorized `adb` device and a successfully built `devDebug` APK.

## 3. Current blocker matrix

| ID | Area | State | Blocking core Git/GitHub use? | Closure |
|---|---|---|---|---|
| R01 | Source/contracts | PRESENT | yes | local gate G0/G1 |
| R02 | PAT login | IMPLEMENTED | yes | runtime authentication on device |
| R03 | `gh` CLI import | IMPLEMENTED | no | `scripts/rafgittools_private_auth_check.sh` + app import smoke |
| R04 | OAuth Device Flow | IMPLEMENTED / CONFIG_REQUIRED | no, PAT/gh remain valid | configure public GitHub OAuth Client ID; never invent/store a client secret in APK |
| R05 | GitHub API | PARTIAL_ADVANCED / RUNTIME_GATED | yes | authenticated device regression |
| R06 | JGit local operations | IMPLEMENTED_ADVANCED / RUNTIME_GATED | yes | local/device regression fixtures |
| R07 | APK build | TOKEN_VAZIO for current reviewed head | yes for Android runtime claim | local configured SDK/JDK or available CI |
| R08 | APK SHA-256 | TOKEN_VAZIO until R07 | yes for runtime claim | readiness gate G8 |
| R09 | physical-device smoke | TOKEN_VAZIO | yes for `VERIFIED_DEVICE` | explicit `RAFGITTOOLS_DEVICE_SMOKE=1` |
| R10 | GitHub Actions | BLOCKED_INFRA when hosted runner is not allocated | **no** | local gate is canonical fallback; CI is supplementary evidence |
| R11 | interactive staging by hunk P33-05 | IMPLEMENTED / RUNTIME_GATED | no | source: stage+unstage per hunk, stale validation, index lock/atomic commit; Android execution remains separate evidence |
| R12 | GPG external runtime | TOKEN_VAZIO_RUNTIME | no | authorized `gpg` binary + fixture |
| R13 | Git LFS external runtime | TOKEN_VAZIO_RUNTIME | no | `git-lfs` + real repo fixture |
| R14 | Worktree runtime matrix | TOKEN_VAZIO_RUNTIME | no | filesystem/device fixtures |
| R15 | Bisect runtime matrix | TOKEN_VAZIO_RUNTIME | no | controlled regression fixture |
| R16 | LLaMA JNI external `llama.h` | BLOCKED_EXTERNAL | no | pin reviewed llama.cpp dependency separately |

## 4. Interactive staging truth boundary

P33-05 is no longer a source gap. The implementation is deliberately bounded to a representation the current diff model can reproduce losslessly:

- tracked `MODIFY` files;
- strict UTF-8 text up to 2 MiB;
- final newline present;
- one hunk staged/unstaged per operation;
- exact stale-hunk revalidation;
- HEAD and index object revalidation;
- `lockDirCache` + `DirCacheEditor.commit()`;
- working tree is never rewritten;
- binary, non-UTF-8, unmerged/multi-stage and missing-final-newline inputs fail closed.

Those rejected cases are **contract boundaries**, not `TOKEN_VAZIO`. Extending them requires a richer patch/diff representation and its own evidence chain.

## 5. Authentication truth

OAuth Client ID is **not a secret**, but it is installation/application configuration and must come from a real GitHub OAuth App. `TOKEN_VAZIO` is the correct state when it has not been configured.

RafGitTools remains usable without OAuth Device Flow through PAT and the existing `gh` CLI import path. Therefore an absent OAuth Client ID must not be misclassified as "app cannot authenticate".

For a private repository, credential validity and repository accessibility are separate evidence gates. `rafgittools_private_auth_check.sh` checks both using the GitHub CLI while deliberately excluding token material from output and receipts.

Never place a GitHub OAuth client secret, PAT, fine-grained token, SSH private key, keystore password or authorization header in source, logs or receipts.

## 6. Private repository / CI boundary

A private repository does not invalidate the code. If hosted Actions cannot execute because a runner is not allocated or because of account/repository infrastructure, classify the evidence as:

```text
BLOCKED_INFRA
```

not as source PASS and not as source FAIL.

The canonical fallback is the local readiness gate, which intentionally uses the already configured local toolchain and never downloads an Android SDK silently on Termux.

## 7. Privacy-manager hardening integrated; runtime still gated

The fail-closed repository-visibility manager from PR #337 is present on `main`. Its source-level P0 safety invariants are:

- live repository preflight immediately before mutation;
- `permissions.admin=true` required;
- forks/archived/disabled/already-private blocked fail-closed;
- durable receipt exists before external mutation;
- no token/header/repository content in receipts;
- explicit high-impact confirmation on device.

The merge establishes source integration, **not** Android/runtime proof. Bulk visibility execution, authenticated organization behavior and physical-device receipts remain separate evidence gates until exercised on the reviewed build.

## 8. Residual TOKEN_VAZIO policy

A `TOKEN_VAZIO` is closed only by a corresponding artifact/evidence pair. Examples:

```text
APK             -> file + SHA-256 + build command + exit status
physical device -> serial/ABI/SDK + install/start transcript
OAuth config    -> non-placeholder Client ID + successful device flow
private auth    -> authenticated login + private repo read permission, no token output
GPG             -> tool version + signed fixture + positive/negative verification
LFS             -> git-lfs version + fixture repo + push/pull transcript
```

No documentation sentence may promote absence of those artifacts to PASS.

## 9. Priority

### P0 — required before calling Android runtime verified

1. local readiness gate G0-G8;
2. prove private-repository authentication locally where relevant;
3. produce and hash `devDebug` APK;
4. physical-device G9/G10;
5. record sanitized receipt for the exact reviewed commit.

### P1 — operational hardening

1. authenticated GitHub regression (personal + organization repository inventory);
2. exercise the integrated fail-closed private/public visibility flow on a controlled repository set;
3. clone/fetch/pull/push/force-with-lease regression on disposable repositories;
4. interactive hunk stage/unstage regression on Android;
5. credential/log redaction regression.

### P2 — external/non-core runtime capabilities

GPG, LFS, worktree, bisect and LLaMA external runtime.

## 10. Invariant

```text
source exists
!= source integrated
!= tests passed
!= APK built
!= APK hashed
!= device executed
!= production claim
```

The readiness gate exists to preserve those boundaries while still making local/private-repository development practical.
