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
    terminal = (ROOT / "app/src/main/kotlin/com/rafgittools/terminal/TerminalEmulator.kt").read_text(encoding="utf-8")
    queue = (ROOT / "app/src/main/kotlin/com/rafgittools/offline/OfflineQueue.kt").read_text(encoding="utf-8")
    atomic = (ROOT / "app/src/main/kotlin/com/rafgittools/offline/AtomicFileQueueStorage.kt").read_text(encoding="utf-8")
    providers = (ROOT / "app/src/main/kotlin/com/rafgittools/platform/MultiPlatformManager.kt").read_text(encoding="utf-8")
    jgit = (ROOT / "app/src/main/kotlin/com/rafgittools/data/git/JGitService.kt").read_text(encoding="utf-8")
    token_lifecycle = (ROOT / "app/src/main/kotlin/com/rafgittools/data/auth/TokenRefreshManager.kt").read_text(encoding="utf-8")
    auth_interceptor = (ROOT / "app/src/main/kotlin/com/rafgittools/data/auth/AuthInterceptor.kt").read_text(encoding="utf-8")
    interactive_path = ROOT / "app/src/main/kotlin/com/rafgittools/data/git/InteractiveStagingService.kt"
    interactive = interactive_path.read_text(encoding="utf-8") if interactive_path.is_file() else ""
    diff_screen = (ROOT / "app/src/main/kotlin/com/rafgittools/ui/screens/diff/DiffViewerScreen.kt").read_text(encoding="utf-8")
    diff_view_model = (ROOT / "app/src/main/kotlin/com/rafgittools/ui/screens/diff/DiffViewerViewModel.kt").read_text(encoding="utf-8")
    current = (ROOT / "docs/RAFGITTOOLS_CURRENT_STATE.md").read_text(encoding="utf-8")
    readiness_path = ROOT / "docs/RAFGITTOOLS_READINESS_2026-08-11.md"
    readiness = readiness_path.read_text(encoding="utf-8") if readiness_path.is_file() else ""

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

    # Token lifecycle is invalidation + re-authentication for the auth methods
    # currently modeled by the app. Ban the former always-failing refresh stub,
    # undocumented PAT-expiry header assumption, and Android client-secret path.
    for forbidden in (
        "refreshOAuthToken(",
        "GitHub-Authentication-Token-Expiry",
        "clientSecret: String",
        "refreshToken: String",
    ):
        require(forbidden not in token_lifecycle,
                f"token lifecycle contains unsupported refresh contract: {forbidden}")
    for anchor in (
        "suspend fun handleHttpResponse(",
        "TokenState.InvalidCredential",
        "TokenState.RateLimited",
        "TokenState.Forbidden",
        "authRepository.clearAuthState().isSuccess",
        "fun parseScopesFromHeader(",
    ):
        require(anchor in token_lifecycle, f"token lifecycle invariant missing: {anchor}")
    require("private val tokenRefreshManager: TokenRefreshManager" in auth_interceptor,
            "AuthInterceptor is not integrated with TokenRefreshManager")
    require("tokenRefreshManager.handleHttpResponse(" in auth_interceptor,
            "AuthInterceptor does not dispatch 401/403 lifecycle handling")
    require("response.code == 401 || response.code == 403" in auth_interceptor,
            "AuthInterceptor lifecycle response gate is missing")
    require("authTokenCache.token = null" in auth_interceptor,
            "AuthInterceptor does not fail closed in memory after 401")
    require('response.header("X-RateLimit-Remaining")' in auth_interceptor,
            "AuthInterceptor does not pass rate-limit evidence")

    # Interactive hunk staging invariants.
    require(interactive_path.is_file(), "interactive hunk staging service is missing")
    for anchor in (
        "suspend fun stageHunk(",
        "suspend fun unstageHunk(",
        "Selected hunk is stale",
        "repository.lockDirCache()",
        "DirCacheEditor.PathEdit",
        "editor.commit()",
        "contentEquals(workBytesBefore)",
        "setUpdateNeeded(true)",
        "CancellationException",
    ):
        require(anchor in interactive, f"interactive staging invariant missing: {anchor}")
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
