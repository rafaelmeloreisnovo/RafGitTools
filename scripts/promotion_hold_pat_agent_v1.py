#!/usr/bin/env python3
"""RafGitTools P0 promotion-hold agent. Never merges, approves or changes rulesets.

Only an owner-dispatched, environment-gated run may attempt PATCH title on an
exact-head allowlisted PR. Provider capability and effective merge enforcement
remain separately unproven until independent native readback.
"""
from __future__ import annotations

import argparse
import json
import os
import re
import sys
import urllib.error
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DEFAULT = ROOT / "configs/promotion-hold-agent.v1.json"
SHA = re.compile(r"^[0-9a-f]{40}$")
REPO = re.compile(r"^rafaelmeloreisnovo/(?:Mapa|RafPolimata)$")
VALID_OPERATIONS = ("validate", "inspect", "mark")
API_HOST = "https://api.github.com"
OWNER = "rafaelmeloreisnovo"
EXECUTOR = "rafaelmeloreisnovo/RafGitTools"
TARGET_BINDINGS = {"mapa750": ("rafaelmeloreisnovo/Mapa", 750),
                   "rafpolimata431": ("rafaelmeloreisnovo/RafPolimata", 431),
                   "rafpolimata432": ("rafaelmeloreisnovo/RafPolimata", 432)}


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        raise ValueError("REDIRECT_FORBIDDEN")


def validate_contract(config):
    errors = []
    if not isinstance(config, dict):
        return ["INVALID_CONTRACT_ROOT"]
    expected = {
        "schema": "rafaelia.promotion-hold-agent.v1",
        "authority": EXECUTOR,
        "credential": "PAT_AGENTS",
        "claims_allowed": False,
        "automatic_merge": False,
        "promotion_authority": False,
        "allowed_mutation": "PATCH_PR_TITLE_ONLY_EXPLICIT_MANUAL",
        "execution": "MAIN_OWNER_MANUAL_PROTECTED_ENVIRONMENT",
        "approval_or_ruleset_mutation": False,
    }
    for name, value in expected.items():
        if config.get(name) != value or type(config.get(name)) is not type(value):
            errors.append("INVALID_CONTRACT_" + name.upper())
    targets = config.get("targets")
    if not isinstance(targets, list) or len(targets) != 3:
        return errors + ["TARGET_ALLOWLIST_INVALID"]
    expected_keys = {"mapa750", "rafpolimata431", "rafpolimata432"}
    seen = set()
    for item in targets:
        if not isinstance(item, dict):
            errors.append("TARGET_NOT_OBJECT")
            continue
        target = item.get("id")
        if not isinstance(target, str) or target in seen or target not in expected_keys:
            errors.append("TARGET_ID_INVALID")
        if isinstance(target, str):
            seen.add(target)
        if not isinstance(item.get("repository"), str) or not REPO.fullmatch(item["repository"]):
            errors.append("TARGET_REPOSITORY_INVALID")
        if type(item.get("pr")) is not int or item["pr"] < 1:
            errors.append("TARGET_PR_INVALID")
        if isinstance(target, str) and target in TARGET_BINDINGS and (
                item.get("repository"), item.get("pr")) != TARGET_BINDINGS[target]:
            errors.append("TARGET_BINDING_INVALID")
        if not isinstance(item.get("head_sha"), str) or not SHA.fullmatch(item["head_sha"]):
            errors.append("TARGET_SHA_INVALID")
        title = item.get("title")
        if (not isinstance(title, str) or not title.startswith("[NÃO PROMOVER — P0: ")
                or len(title) > 180 or "\n" in title or "\r" in title
                or "] " not in title):
            errors.append("NON_PROMOTION_TITLE_INVALID")
        gap_ids = item.get("gap_ids")
        if (not isinstance(gap_ids, list) or len(gap_ids) < 2
                or any(not isinstance(g, str) or not re.fullmatch(r"[A-Z][A-Z0-9_]+", g) for g in gap_ids)):
            errors.append("GAP_PROVENANCE_INVALID")
        evidence = item.get("evidence")
        if (not isinstance(evidence, list) or not evidence
                or any(not isinstance(e, str) or not
                       e.startswith("https://github.com/rafaelmeloreisnovo/") for e in evidence)):
            errors.append("EVIDENCE_LOCATOR_INVALID")
    if seen != expected_keys:
        errors.append("TARGET_ALLOWLIST_MISSING")
    return sorted(set(errors))


def selected(config, target):
    return next((x for x in config["targets"] if x["id"] == target), None)


def api_path(target):
    owner, repo = target["repository"].split("/")
    return "/repos/" + owner + "/" + repo + "/pulls/" + str(target["pr"])


def receipt(target, operation, state, *, readback=False, attempted=False, reason=None):
    return {
        "schema": "rafaelia.promotion-hold-receipt.v1",
        "agent": "RafGitTools/PAT_AGENTS",
        "target": target.get("id", "TOKEN_VAZIO_TARGET"),
        "repository": target.get("repository", "TOKEN_VAZIO_REPOSITORY"),
        "pr": target.get("pr", 0),
        "expected_head": target.get("head_sha", "TOKEN_VAZIO_SHA"),
        "required_title": target.get("title", "TOKEN_VAZIO_TITLE"),
        "gap_ids": target.get("gap_ids", []),
        "operation": operation,
        "state": state,
        "reason": reason or state,
        "provider_title_readback": readback,
        "provider_write_attempted": attempted,
        "credential_id": "PAT_AGENTS",
        "claim_allowed": False,
        "promotion_allowed": False,
        "automatic_merge": False,
        "independent_approval": "TOKEN_VAZIO_NOT_ATTESTED",
        "server_merge_enforcement": "TOKEN_VAZIO_NOT_ATTESTED",
    }


def owner_manual_guard(env):
    return (env.get("GITHUB_EVENT_NAME") == "workflow_dispatch"
            and env.get("GITHUB_REF") == "refs/heads/main"
            and env.get("GITHUB_ACTOR") == OWNER
            and env.get("GITHUB_REPOSITORY") == EXECUTOR)


def check_snapshot(snapshot, target):
    if not isinstance(snapshot, dict):
        return "PROVIDER_MALFORMED"
    head = snapshot.get("head")
    if not isinstance(head, dict) or head.get("sha") != target["head_sha"]:
        return "BLOCKED_HEAD_DRIFT"
    base = snapshot.get("base")
    if not isinstance(base, dict) or base.get("ref") != "main":
        return "BLOCKED_BASE_DRIFT"
    if snapshot.get("number") != target["pr"]:
        return "BLOCKED_ID_DRIFT"
    if snapshot.get("state") != "open" or snapshot.get("merged_at") is not None:
        return "BLOCKED_PR_NOT_OPEN"
    if not isinstance(snapshot.get("title"), str):
        return "PROVIDER_MALFORMED"
    return ""


def evaluate(target, operation, transport, env):
    if not owner_manual_guard(env):
        return receipt(target, operation, "BLOCKED_UNTRUSTED_DISPATCH"), 3
    if operation == "mark" and env.get("PROMOTION_HOLD_CONFIRM") != "EXPLICIT_HOLD_ONLY":
        return receipt(target, operation, "BLOCKED_EXPLICIT_CONFIRMATION"), 3
    path = api_path(target)
    status, before = transport("GET", path, None)
    if status != 200:
        return receipt(target, operation, "BLOCKED_PROVIDER_READ", reason="HTTP_" + str(status)), 3
    violation = check_snapshot(before, target)
    if violation:
        return receipt(target, operation, violation), 3
    if before["title"] == target["title"]:
        return receipt(target, operation, "HOLD_TITLE_CONFIRMED_WRITE_PERMISSION_UNPROVEN",
                       readback=True), 0
    if operation != "mark":
        return receipt(target, operation, "BLOCKED_HOLD_TITLE_DRIFT", readback=True), 3
    # The sole provider write surface. Never alter state, protection, labels,
    # reviews, permissions, merge routes or workflow definitions.
    status, _ = transport("PATCH", path, {"title": target["title"]})
    if status != 200:
        return receipt(target, operation, "BLOCKED_PROVIDER_TITLE_PATCH",
                       readback=True, attempted=True, reason="HTTP_" + str(status)), 3
    status, after = transport("GET", path, None)
    if status != 200:
        return receipt(target, operation, "BLOCKED_POSTPATCH_READBACK",
                       attempted=True, reason="HTTP_" + str(status)), 3
    violation = check_snapshot(after, target)
    if violation:
        return receipt(target, operation, violation, attempted=True), 3
    if after["title"] != target["title"]:
        return receipt(target, operation, "BLOCKED_POSTPATCH_TITLE_MISMATCH",
                       attempted=True), 3
    return receipt(target, operation, "HOLD_TITLE_PATCHED_AND_READBACK",
                   readback=True, attempted=True), 0


def live_transport(token):
    if not token:
        return None
    opener = urllib.request.build_opener(NoRedirect)

    def transport(method, path, payload):
        if (method not in ("GET", "PATCH") or not isinstance(path, str)
                or not re.fullmatch(r"/repos/rafaelmeloreisnovo/(?:Mapa|RafPolimata)/pulls/[1-9][0-9]*", path)
                or (method == "GET" and payload is not None)
                or (method == "PATCH" and (not isinstance(payload, dict)
                    or set(payload) != {"title"} or
                    not isinstance(payload["title"], str) or
                    not payload["title"].startswith("[NÃO PROMOVER — P0: ")))):
            raise ValueError("PROVIDER_ENDPOINT_OR_PAYLOAD_NOT_ALLOWLISTED")
        data = None if method == "GET" else json.dumps(payload).encode("utf-8")
        req = urllib.request.Request(
            API_HOST + path, data=data, method=method,
            headers={"Authorization": "Bearer " + token,
                     "Accept": "application/vnd.github+json",
                     "X-GitHub-Api-Version": "2022-11-28",
                     "User-Agent": "RafGitTools-PromotionHold-V1",
                     "Content-Type": "application/json"})
        try:
            with opener.open(req, timeout=10) as response:
                raw = response.read(131073)
                if len(raw) > 131072:
                    return 413, None
                return response.status, json.loads(raw)
        except urllib.error.HTTPError as exc:
            return exc.code, None
        except (ValueError, OSError, json.JSONDecodeError):
            return 599, None

    return transport


def main(argv=None):
    ap = argparse.ArgumentParser()
    ap.add_argument("--operation", choices=VALID_OPERATIONS, default="validate")
    ap.add_argument("--target", default="")
    ap.add_argument("--contract", type=Path, default=DEFAULT)
    ap.add_argument("--receipt", type=Path)
    args = ap.parse_args(argv)
    try:
        config = json.loads(args.contract.read_text(encoding="utf-8"))
        errors = validate_contract(config)
    except (OSError, ValueError) as exc:
        errors = ["CONTRACT_READ_OR_PARSE_ERROR:" + type(exc).__name__]
        config = None
    if errors:
        result = {"schema": "rafaelia.promotion-hold-receipt.v1",
                  "state": "BLOCKED_CONTRACT", "errors": errors,
                  "claim_allowed": False, "promotion_allowed": False}
        status = 3
    elif args.operation == "validate":
        result = {"schema": "rafaelia.promotion-hold-receipt.v1",
                  "state": "ALLOWLIST_SOURCE_VALIDATED_NOT_PROVIDER_PROOF",
                  "targets": [x["id"] for x in config["targets"]],
                  "claim_allowed": False, "promotion_allowed": False}
        status = 0
    else:
        target = selected(config, args.target)
        if target is None:
            result = {"schema": "rafaelia.promotion-hold-receipt.v1",
                      "state": "BLOCKED_TARGET_NOT_ALLOWLISTED",
                      "claim_allowed": False, "promotion_allowed": False}
            status = 3
        elif not owner_manual_guard(os.environ):
            result, status = receipt(target, args.operation,
                                     "BLOCKED_UNTRUSTED_DISPATCH"), 3
        else:
            transport = live_transport(os.environ.get("RAFAELIA_AGENTS_TOKEN", ""))
            if transport is None:
                result, status = receipt(target, args.operation,
                                         "BLOCKED_SECRET_UNAVAILABLE"), 3
            else:
                result, status = evaluate(target, args.operation, transport, os.environ)
    output = json.dumps(result, sort_keys=True, ensure_ascii=False) + "\n"
    if args.receipt:
        args.receipt.parent.mkdir(parents=True, exist_ok=True)
        with args.receipt.open("x", encoding="utf-8") as f:
            f.write(output)
    sys.stdout.write(output)
    return status


if __name__ == "__main__":
    raise SystemExit(main())
