#!/usr/bin/env python3
"""Validate the GovernanceGate registry against concrete ToolRouter handlers.

This is a structural source gate only. It does not execute Git operations,
WorkManager, provider calls, runtime/device paths or claim promotion.
"""

from __future__ import annotations

import argparse
import json
import re
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
REGISTRY_PATH = ROOT / "app/src/main/assets/kernel/protocol/tool_registry.json"
ROUTER_PATH = ROOT / "app/src/main/kotlin/com/rafgittools/kernel/ToolRouter.kt"

HANDLER_RE = re.compile(r'"([^"]+)"\s*->\s*handle[A-Za-z0-9_]+\(call\)')


def evaluate(registry: dict[str, Any], router_text: str) -> dict[str, Any]:
    tools = registry.get("tools")
    if not isinstance(tools, dict):
        return {
            "state": "FAIL",
            "reason": "registry.tools must be an object",
            "claim_allowed": False,
        }

    registered = set(tools)
    handlers = set(HANDLER_RE.findall(router_text))
    allowed = {
        name for name, config in tools.items()
        if isinstance(config, dict) and config.get("allowed") is True
    }
    allowed_without_handler = sorted(allowed - handlers)
    handler_without_registry = sorted(handlers - registered)

    bad_handler_state: list[str] = []
    for name in sorted(allowed):
        config = tools[name]
        state = config.get("handler_state") if isinstance(config, dict) else None
        if not isinstance(state, str) or not state.startswith("IMPLEMENTED"):
            bad_handler_state.append(name)

    durable_contract_missing: list[str] = []
    if "git.push" in allowed:
        if "SyncOperation.GitPush" not in router_text or 'tool = "git.push"' not in router_text:
            durable_contract_missing.append("git.push")
    if "git.pull" in allowed:
        if "SyncOperation.GitPull" not in router_text or 'tool = "git.pull"' not in router_text:
            durable_contract_missing.append("git.pull")
    if {"git.push", "git.pull"} & allowed and "queue.enqueue(operation)" not in router_text:
        durable_contract_missing.append("durable_queue_boundary")

    problems = {
        "allowed_without_handler": allowed_without_handler,
        "handler_without_registry": handler_without_registry,
        "allowed_without_implemented_state": bad_handler_state,
        "durable_contract_missing": durable_contract_missing,
    }
    passed = all(not values for values in problems.values())

    return {
        "schema": "rafgittools.tool-router-registry-report.v1",
        "state": "PASS" if passed else "FAIL",
        "registry_version": registry.get("version", "TOKEN_VAZIO"),
        "registered_count": len(registered),
        "allowed_count": len(allowed),
        "handler_count": len(handlers),
        "registered_tools": sorted(registered),
        "allowed_tools": sorted(allowed),
        "router_handlers": sorted(handlers),
        **problems,
        "execution_evidence": "NOT_RUN",
        "claim_allowed": False,
    }


def validate() -> dict[str, Any]:
    registry = json.loads(REGISTRY_PATH.read_text(encoding="utf-8"))
    router_text = ROUTER_PATH.read_text(encoding="utf-8")
    return evaluate(registry, router_text)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--report", type=Path)
    args = parser.parse_args()

    report = validate()
    rendered = json.dumps(report, ensure_ascii=False, indent=2, sort_keys=True) + "\n"
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(rendered, encoding="utf-8")
    print(rendered, end="")
    return 0 if report.get("state") == "PASS" else 2


if __name__ == "__main__":
    raise SystemExit(main())
