#!/usr/bin/env python3
"""Local evidence validator for RafGitTools.

This validator intentionally does not invoke GitHub Actions or claim Android
runtime success. It checks contracts, state semantics and the source-level
invariants introduced by the audit using only Python's standard library.
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
    require("não estão pendentes de integração" in current,
            "fazer/ source-of-truth contradiction was not resolved")
    require(
        "OUT_OF_SCOPE_NO_CREDIT" in current or "BLOCKED_INFRA" in readiness,
        "documentation must not imply unobserved GitHub Actions success",
    )


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
