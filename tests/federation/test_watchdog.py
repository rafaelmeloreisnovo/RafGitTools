#!/usr/bin/env python3
from __future__ import annotations

import importlib.util
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
WATCHDOG_PATH = ROOT / "scripts" / "federation" / "watchdog.py"
MANIFEST_PATH = ROOT / "configs" / "rafaelia-federation.json"

spec = importlib.util.spec_from_file_location("federation_watchdog", WATCHDOG_PATH)
assert spec and spec.loader
watchdog = importlib.util.module_from_spec(spec)
spec.loader.exec_module(watchdog)


def load_manifest() -> dict:
    return json.loads(MANIFEST_PATH.read_text(encoding="utf-8"))


def test_manifest_is_valid() -> None:
    assert watchdog.validate_manifest(load_manifest()) == []


def test_digest_is_independent_of_repository_order() -> None:
    manifest = load_manifest()
    result = watchdog.blind_order_test(manifest, seed=144000)
    assert result["pass"] is True
    assert result["baseline_digest"] == result["observed_digest"]


def test_all_critical_repositories_have_safe_state() -> None:
    manifest = load_manifest()
    critical = [repo for repo in manifest["repositories"] if repo["critical"]]
    assert critical
    assert all(repo["safe_state"].strip() for repo in critical)
    assert all(repo["rollback"].strip() for repo in critical)


def test_failure_simulation_never_promotes_claims() -> None:
    manifest = load_manifest()
    for repo in manifest["repositories"]:
        result = watchdog.simulate_failure(manifest, repo["name"])
        assert result["status"] in {"FAILOVER_AVAILABLE", "FAIL_SAFE_ONLY"}
        assert result["claim_policy"] == "failure cannot promote claims"


def test_unknown_repository_failure_is_blocked() -> None:
    result = watchdog.simulate_failure(load_manifest(), "TOKEN_VAZIO/unknown")
    assert result["status"] == "BLOCKED"
