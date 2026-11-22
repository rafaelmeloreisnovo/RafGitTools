# RafGitTools · Raf Actions Control Screen V1

**Status:** SOURCE_IMPLEMENTED / CI_AND_DEVICE_TOKEN_VAZIO. This file does not assert the app was installed or that any GitHub Actions mutation was executed.

## Human navigation

1. Open RafGitTools > top bar Play icon **Raf Actions**, or Android app drawer > **Raf Actions**.
2. Use the existing in-app GitHub sign-in/PAT for the device account. GitHub workflow runner repository Secret `PAT_ACTIONS` is **not** available to the Android app and must never be bundled in an APK or exported from CI to the phone.
3. Choose a repository: `RafGitTools`, `termux-app-rafacodephi`, `termux-packages`, `RafPolimata`, `Vectras-VM-Android`. Read workflow/runs listings for that repository (provider page 1 only).
4. For **Disparar**, select an active workflow, pick a real branch/tag (defaults to provider's default branch), optionally supply string-only JSON inputs supported by the YAML, then confirm `DISPARAR <workflow_id>`.
5. For a queued/running run, confirm `CANCELAR <run_id>`.
6. For a completed exact-40-hex-SHA run, confirm `REEXECUTAR <run_id>`. On failed/cancelled/timed-out runs, optional `REEXECUTAR FALHOS <run_id>`.
7. Read `REQUEST_ACCEPTED` / `PROVIDER_DENIED` receipt. Use **Atualizar** or **Ver execução/logs** to read later provider state. HTTP 204/201 proves only accepted request, never terminal success.
8. A typed confirmation and current repository/reference match are mandatory. A fresh provider GET binds workflow ID or run ID, exact SHA and status. Repo selection cannot be set to arbitrary third-party URL; device session owner guard applies before every mutation.

## Gate and authority

```text
User UI → authorized device OAuth/PAT session → user actor=rafaelmeloreisnovo
   → repo allowlist → exact-ID current provider preflight → INPUTS/STATUS guards
   → append local INTENT_PREPARED (private JSONL + fd.sync, fail-closed)
   → POST workflow_dispatch / cancel / rerun / rerun-failed-jobs
   → append local outcome (HTTP status only; no token/inputs)
   → explicit refresh/read GitHub run → later device proof (separate gate)
```

The existing `AuthRepository` encrypts device PAT/session data. No CI Secrets or raw token data enter this new UI or its local receipts. The API client follows the existing domain network guard + AuthInterceptor + sanitized HTTP logs. GitHub (provider) additionally enforces repository and token permissions. Minimal requested fine-grained PAT scopes: **Actions read and write on selected repositories** as applicable; release/administration/delete permissions are *not* required to list/dispatch/cancel/rerun. Actual device token scope is **TOKEN_VAZIO until observed**.

This UI has no global cancel-all, mass dispatch, workflow deletion, release publication, secret retrieval, token management or privilege escalation. It cannot bypass GitHub Actions billing/quota, disabled workflows, pending jobs, branch restrictions or repository authorization. Non-`workflow_dispatch` workflows may show as active but GitHub rejects POST with its own HTTP status; no synthetic PASS is generated.

## Implementation map

- `app/src/main/kotlin/com/rafgittools/data/github/ActionsControlModels.kt`: source model + pure refusal policy.
- `app/src/main/kotlin/com/rafgittools/data/github/GithubApiService.kt`: 3 reads and 4 writes via authenticated Retrofit/HTTPS.
- `app/src/main/kotlin/com/rafgittools/ui/screens/actions/ActionsControlViewModel.kt`: single-flight, preflight, receipts, no background polling.
- `app/src/main/kotlin/com/rafgittools/ui/screens/actions/ActionsControlScreen.kt`: Compose dashboard, explicit phrase confirmation and GitHub logs links.
- `app/src/main/kotlin/com/rafgittools/ActionsControlActivity.kt` + `AndroidManifest.xml`: independent Android launcher.
- `app/src/main/kotlin/com/rafgittools/ui/screens/home/HomeScreen.kt` + `MainActivity.kt`: primary Home entry.
- `app/src/test/kotlin/com/rafgittools/data/github/ActionsControlPolicyTest.kt` and `tests/test_actions_control_screen_contract.py`: falsifiers.

**Receipt path:** Android app-private `files/actions-control-receipts.jsonl`; append-only by design, one line per intent and one line per response, with synchronized file descriptor. No claim of cryptographic tamper-proof journal: a separate signed/Merkle receipt gate would be necessary for stronger custody.

## Remaining F_gap

- Kotlin/Gradle Android compilation and real Android UX test at the exact PR head: **NOT_RUN/QUEUED** until observed.
- Token permissions/repository authorization, network quota and billing from a physical signed-in device: **TOKEN_VAZIO**.
- Dashboard currently lists the first 100 workflows and 50 recent runs per repository, not an exhaustive or paginated historical inventory.
- Successful POST is not a new run ID or result; verify provider state by refresh.
- Previous PR #655 protects RafGitTools `PAT_ENV` workflow permissions, a **separate CI-side secret capability**, not the mobile authentication channel.

`R3 = ⟨F_ok: source UI + API + preflight + durable request receipts, F_gap: exact-head Android tests + device auth/provider scopes, F_next: CI→APK→real operator request/readback receipt⟩`
