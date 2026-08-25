#!/usr/bin/env python3
"""Local evidence validator for RafGitTools.

This validator intentionally does not invoke GitHub Actions or claim Android
runtime success. It checks contracts, state semantics and source-level
invariants using only Python's standard library.
"""

from __future__ import annotations

import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def load_json(relative: str) -> dict:
    path = ROOT / relative
    require(path.is_file(), f"missing required file: {relative}")
    return json.loads(path.read_text(encoding="utf-8"))


def read_text(relative: str) -> str:
    path = ROOT / relative
    require(path.is_file(), f"missing required source file: {relative}")
    return path.read_text(encoding="utf-8")


def check_workmanager_startup_contract(manifest: str, application: str) -> None:
    """Reject a launch-time WorkManager initialization gap.

    WorkManager's default initializer may only be removed when the application
    supplies a custom configuration or explicitly initializes the singleton.
    RafGitTools schedules periodic work from Application.onCreate(), so this
    relationship is a cold-start contract rather than an optional feature.
    """
    removes_default_initializer = (
        "androidx.work.WorkManagerInitializer" in manifest
        and 'tools:node="remove"' in manifest
    )
    provides_alternative_initialization = (
        "Configuration.Provider" in application
        or "WorkManager.initialize(" in application
    )
    require(
        not removes_default_initializer or provides_alternative_initialization,
        "WorkManager default initialization is removed without a replacement; app launch can crash",
    )
    require(
        "WorkManager.getInstance(this).enqueueUniquePeriodicWork" in application,
        "application startup must keep the explicit periodic-work scheduling contract",
    )


def check_runtime_state() -> None:
    state = load_json("ECOSYSTEM_RUNTIME_STATE.json")
    require(state.get("schema") == "raf.ecosystem-runtime-state.v1", "invalid runtime state schema")
    ci = state.get("ci_execution", {})
    require(
        ci.get("state") in {"OUT_OF_SCOPE_NO_CREDIT", "AVAILABLE", "BLOCKED_INFRA", "TOKEN_VAZIO"},
        "invalid CI execution state",
    )
    require(bool(ci.get("reason")), "CI execution state requires a reason")

    components = state.get("components")
    require(isinstance(components, list) and components, "runtime state requires components")
    ids = [component.get("id") for component in components]
    require(len(ids) == len(set(ids)), "component ids must be unique")

    valid_implementation_states = {
        "IMPLEMENTED", "PARTIAL", "ADAPTER_IMPLEMENTED", "STUB",
        "EXPERIMENTAL", "DEVICE_REQUIRED", "REFERENCE",
    }
    valid_evidence_states = {"VERIFIED", "DECLARED_BY_AUTHOR", "TOKEN_VAZIO", "CONTRADICTION"}

    for component in components:
        require(component.get("implementation_state") in valid_implementation_states,
                f"invalid implementation state for {component.get('id')}")
        require(component.get("evidence_state") in valid_evidence_states,
                f"invalid evidence state for {component.get('id')}")
        require(isinstance(component.get("evidence"), list), "evidence must be a list")
        require(isinstance(component.get("gaps"), list), "gaps must be a list")
        require(bool(component.get("next_action")), "next_action is required")


def check_contracts() -> None:
    job = load_json("contracts/job-v1.schema.json")
    runtime = load_json("contracts/ecosystem-runtime-state.schema.json")
    require(job.get("properties", {}).get("schema", {}).get("const") == "raf.job.v1",
            "job.v1 schema const is missing")
    require("idempotency_key" in job.get("required", []), "job.v1 must require idempotency_key")
    require(runtime.get("properties", {}).get("ci_execution"), "runtime schema must encode CI state")
    ci_states = (
        runtime.get("properties", {})
        .get("ci_execution", {})
        .get("properties", {})
        .get("state", {})
        .get("enum", [])
    )
    require("BLOCKED_INFRA" in ci_states, "runtime schema must represent blocked infrastructure explicitly")


def check_source_invariants() -> None:
    manifest = read_text("app/src/main/AndroidManifest.xml")
    application = read_text("app/src/main/kotlin/com/rafgittools/RafGitToolsApplication.kt")
    terminal = read_text("app/src/main/kotlin/com/rafgittools/terminal/TerminalEmulator.kt")
    queue = read_text("app/src/main/kotlin/com/rafgittools/offline/OfflineQueue.kt")
    atomic = read_text("app/src/main/kotlin/com/rafgittools/offline/AtomicFileQueueStorage.kt")
    providers = read_text("app/src/main/kotlin/com/rafgittools/platform/MultiPlatformManager.kt")
    jgit = read_text("app/src/main/kotlin/com/rafgittools/data/git/JGitService.kt")
    auth_repository = read_text("app/src/main/kotlin/com/rafgittools/data/auth/AuthRepository.kt")
    oauth_flow = read_text("app/src/main/kotlin/com/rafgittools/data/auth/OAuthDeviceFlowManager.kt")
    token_lifecycle = read_text("app/src/main/kotlin/com/rafgittools/data/auth/TokenRefreshManager.kt")
    auth_interceptor = read_text("app/src/main/kotlin/com/rafgittools/data/auth/AuthInterceptor.kt")
    interactive = read_text("app/src/main/kotlin/com/rafgittools/data/git/InteractiveStagingService.kt")
    diff_screen = read_text("app/src/main/kotlin/com/rafgittools/ui/screens/diff/DiffViewerScreen.kt")
    diff_view_model = read_text("app/src/main/kotlin/com/rafgittools/ui/screens/diff/DiffViewerViewModel.kt")
    current = read_text("docs/RAFGITTOOLS_CURRENT_STATE.md")
    readiness = read_text("docs/RAFGITTOOLS_READINESS_2026-08-11.md")

    check_workmanager_startup_contract(manifest, application)

    require("readerThread.start()" in terminal, "terminal output is not drained concurrently")
    require("READ_ONLY_GIT_SUBCOMMANDS" in terminal, "terminal does not restrict Git subcommands")
    require("unclosed quote" in terminal, "terminal quote validation is missing")
    require("OfflineQueueStorage" in queue, "durable queue boundary is missing")
    require("stream.fd.sync()" in atomic, "atomic queue storage does not fsync")
    require("queryGitLabProjects" in providers and "queryAzureDevOpsRepos" in providers,
            "multi-provider implementation anchors are missing")

    # GitHub HTTPS/JGit invariants.
    require('private const val TOKEN_USERNAME = "x-access-token"' in jgit,
            "JGit token username marker is missing")
    require('UsernamePasswordCredentialsProvider(it.token, "")' not in jgit,
            "JGit still places token in username with empty password")
    require('UsernamePasswordCredentialsProvider(TOKEN_USERNAME, it.token)' in jgit,
            "JGit token-as-password mapping is missing")

    # Force-with-lease invariants.
    require("val lsRemoteCommand = git.lsRemote()" in jgit,
            "force-with-lease does not have an explicit lsRemote preflight")
    require("lsRemoteCommand.setCredentialsProvider" in jgit,
            "force-with-lease lsRemote preflight does not receive HTTPS credentials")
    require("lsRemoteCommand.setTransportConfigCallback" in jgit,
            "force-with-lease lsRemote preflight does not receive SSH transport")
    require(".setRefLeaseSpecs(RefLeaseSpec(branchRef, expectedOldObjectId))" in jgit,
            "force-with-lease must protect the destination branch ref")
    require(".setRefLeaseSpecs(RefLeaseSpec(refSpec, expectedOldObjectId))" not in jgit,
            "force-with-lease still protects a src:dst refspec instead of destination ref")
    require('Regex("^[0-9a-fA-F]{40}$")' in jgit,
            "force-with-lease expected object id validation is missing")

    # Credential persistence must keep refresh state separate and erase it when
    # switching to a non-refreshable PAT/OAuth access token.
    for anchor in (
        'stringPreferencesKey("encrypted_refresh_token")',
        'longPreferencesKey("access_token_expires_at_ms")',
        'longPreferencesKey("refresh_token_expires_at_ms")',
        'private const val REFRESH_TOKEN_KEY_ALIAS = "github_refresh_token"',
        "suspend fun saveOAuthSession(",
        "suspend fun getRefreshToken()",
        "suspend fun getRefreshTokenExpiresAt()",
        "preferences.remove(ENCRYPTED_REFRESH_TOKEN_KEY)",
        "preferences.remove(REFRESH_TOKEN_EXPIRES_AT_KEY)",
    ):
        require(anchor in auth_repository, f"refresh credential persistence invariant missing: {anchor}")
    require(auth_repository.count("preferences.remove(ENCRYPTED_REFRESH_TOKEN_KEY)") >= 2,
            "refresh token must be removed both on non-refreshable credential save and logout/clear")

    # Device Flow refresh is capability-aware. The refresh endpoint may use
    # client_id + refresh_token, but never embeds or accepts a client secret.
    for forbidden in (
        '@Field("client_secret")',
        "clientSecret: String",
        "client_secret",
        "GitHub-Authentication-Token-Expiry",
    ):
        require(forbidden not in oauth_flow,
                f"OAuth flow contains unsupported/unsafe contract: {forbidden}")
    for anchor in (
        "class GitHubOAuthApiClient",
        "class GitHubOAuthConfig",
        "private val refreshMutex = Mutex()",
        "suspend fun refreshStoredSession(",
        "rejectedAccessToken: String? = null",
        "currentAccess != rejectedAccessToken",
        'grantType = "refresh_token"',
        "authRepository.saveOAuthSession(",
        '@Field("refresh_token") refreshToken: String',
        "val refresh_token: String? = null",
        "val refresh_token_expires_in: Long? = null",
    ):
        require(anchor in oauth_flow, f"Device Flow refresh invariant missing: {anchor}")

    # Ban the old always-failing fake refresh API and require the serialized
    # refresh->decision->invalidation recovery transaction.
    require("refreshOAuthToken(" not in token_lifecycle,
            "legacy always-failing refreshOAuthToken stub returned")
    require("GitHub-Authentication-Token-Expiry" not in token_lifecycle,
            "token lifecycle still relies on an unsupported PAT-expiry header")
    require("private val recoveryMutex = Mutex()" in token_lifecycle,
            "token lifecycle recovery transaction is not serialized")
    for anchor in (
        "TokenState.Refreshed",
        "TokenState.InvalidCredential",
        "TokenState.RateLimited",
        "TokenState.Forbidden",
        "refreshStoredSession(rejectedAccessToken)",
        "recoveryMutex.withLock",
        "invalidateSessionUnlocked()",
        "authRepository.clearAuthState().isSuccess",
    ):
        require(anchor in token_lifecycle, f"token lifecycle invariant missing: {anchor}")

    # Interceptor must retry exactly once after successful rotation, close the
    # superseded response, and invalidate on the second 401 without refresh loop.
    for anchor in (
        "private val tokenRefreshManager: TokenRefreshManager",
        "rejectedAccessToken = if (firstResponse.code == 401) token else null",
        "firstState is TokenRefreshManager.TokenState.Refreshed",
        "authTokenCache.token = firstState.accessToken",
        "firstResponse.close()",
        "val retryResponse = chain.proceed(",
        "boundedInvalidation()",
        "authTokenCache.token = null",
        'response.header("X-RateLimit-Remaining")',
        "withTimeoutOrNull(LIFECYCLE_TIMEOUT_MS)",
    ):
        require(anchor in auth_interceptor, f"AuthInterceptor lifecycle invariant missing: {anchor}")
    require(auth_interceptor.count("tokenRefreshManager.handleHttpResponse(") == 1,
            "interceptor must have one refresh decision point per request chain")

    # Interactive hunk staging invariants.
    for anchor in (
        "suspend fun stageHunk(",
        "suspend fun unstageHunk(",
        "Selected hunk is stale",
        "repository.lockDirCache()",
        "DirCacheEditor.PathEdit",
        "editor.commit()",
        "setUpdateNeeded(true)",
        "CancellationException",
    ):
        require(anchor in interactive, f"interactive staging invariant missing: {anchor}")
    require(
        "val expectedWorkBytes = requireNotNull(workBytesBefore)" in interactive
        and interactive.count("contentEquals(expectedWorkBytes)") >= 2,
        "interactive staging does not revalidate working-tree bytes before index commit",
    )
    require("writeText(" not in interactive and "writeBytes(" not in interactive,
            "interactive staging service must not rewrite the working tree")
    require("DiffChangeType.MODIFY" in interactive,
            "interactive staging must preserve tracked-MODIFY scope boundary")
    require("Missing-final-newline" in interactive and "Binary file cannot" in interactive,
            "interactive staging must fail closed on non-lossless text representations")
    require('"Stage hunk ${index + 1}"' in diff_screen,
            "DiffViewer does not expose explicit per-hunk stage control")
    require('"Unstage hunk ${index + 1}"' in diff_screen,
            "DiffViewer does not expose explicit per-hunk unstage control")
    require("interactiveStagingService.stageHunk" in diff_view_model and
            "interactiveStagingService.unstageHunk" in diff_view_model,
            "DiffViewerViewModel is not wired to both hunk mutations")

    require("não é fonte de verdade" in current,
            "fazer/ source-of-truth boundary is missing")
    require("BLOCKED_INFRA_BILLING" in current,
            "current-state documentation must preserve exact CI infrastructure classification")
    require("BLOCKED_INFRA" in readiness,
            "readiness documentation must not imply unobserved GitHub Actions success")


def main() -> int:
    checks = [check_runtime_state, check_contracts, check_source_invariants]
    for check in checks:
        check()
        print(f"PASS {check.__name__}")
    print("PASS runtime-truth-local")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (AssertionError, json.JSONDecodeError, OSError) as error:
        print(f"FAIL runtime-truth-local: {error}", file=sys.stderr)
        raise SystemExit(1)
