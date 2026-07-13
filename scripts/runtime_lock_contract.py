#!/usr/bin/env python3
"""Validate and query the RAFAELIA cross-repository runtime lock.

The contract deliberately separates source pinning from artifact promotion:
source commits must always be concrete; artifact hashes may remain TOKEN_VAZIO
until a build produces them, but release gates can require them explicitly.
"""

from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path
from typing import Any

SCHEMA = "rafaelia.runtime-lock.v1"
TOKEN_VAZIO = "TOKEN_VAZIO"
SHA40_RE = re.compile(r"^[0-9a-f]{40}$")
SHA256_RE = re.compile(r"^[0-9a-f]{64}$")

REQUIRED_REPOSITORIES = {
    "rafaelmeloreisnovo/termux-app-rafacodephi": "master",
    "rafaelmeloreisnovo/CONVERSATIONS_CHUNKS_PRIVATE": "main",
    "rafaelmeloreisnovo/llamaRafaelia": "master",
    "rafaelmeloreisnovo/RafPolimata": "main",
}

OUTPUT_NAMES = {
    "rafaelmeloreisnovo/termux-app-rafacodephi": "termux_commit",
    "rafaelmeloreisnovo/CONVERSATIONS_CHUNKS_PRIVATE": "conversations_commit",
    "rafaelmeloreisnovo/llamaRafaelia": "llama_commit",
    "rafaelmeloreisnovo/RafPolimata": "rafpolimata_commit",
}


class ContractError(ValueError):
    """Raised when the lock violates the canonical contract."""


def load_lock(path: Path) -> dict[str, Any]:
    try:
        with path.open("r", encoding="utf-8") as handle:
            data = json.load(handle)
    except FileNotFoundError as exc:
        raise ContractError(f"lock file not found: {path}") from exc
    except json.JSONDecodeError as exc:
        raise ContractError(f"invalid JSON in {path}: {exc}") from exc

    if not isinstance(data, dict):
        raise ContractError("lock root must be a JSON object")
    return data


def repository_map(data: dict[str, Any]) -> dict[str, dict[str, Any]]:
    repositories = data.get("repositories")
    if not isinstance(repositories, list):
        raise ContractError("repositories must be an array")

    mapped: dict[str, dict[str, Any]] = {}
    for index, item in enumerate(repositories):
        if not isinstance(item, dict):
            raise ContractError(f"repositories[{index}] must be an object")
        name = item.get("name")
        if not isinstance(name, str) or not name:
            raise ContractError(f"repositories[{index}].name must be a non-empty string")
        if name in mapped:
            raise ContractError(f"duplicate repository entry: {name}")
        mapped[name] = item
    return mapped


def validate(data: dict[str, Any], require_artifact_hashes: bool = False) -> dict[str, dict[str, Any]]:
    errors: list[str] = []

    if data.get("schema") != SCHEMA:
        errors.append(f"schema must be {SCHEMA!r}")

    integration = data.get("integration_repository")
    if not isinstance(integration, dict):
        errors.append("integration_repository must be an object")
    else:
        if integration.get("name") != "rafaelmeloreisnovo/RafGitTools":
            errors.append("integration_repository.name must be rafaelmeloreisnovo/RafGitTools")
        if integration.get("branch") != "main":
            errors.append("integration_repository.branch must be main")
        if integration.get("commit") != "SELF":
            errors.append("integration_repository.commit must be SELF to avoid a recursive self-lock")

    try:
        mapped = repository_map(data)
    except ContractError as exc:
        errors.append(str(exc))
        mapped = {}

    missing = sorted(set(REQUIRED_REPOSITORIES) - set(mapped))
    extra = sorted(set(mapped) - set(REQUIRED_REPOSITORIES))
    if missing:
        errors.append("missing required repositories: " + ", ".join(missing))
    if extra:
        errors.append("unexpected repositories: " + ", ".join(extra))

    for name, expected_branch in REQUIRED_REPOSITORIES.items():
        entry = mapped.get(name)
        if entry is None:
            continue
        if entry.get("branch") != expected_branch:
            errors.append(f"{name}.branch must be {expected_branch!r}")
        commit = entry.get("commit")
        if not isinstance(commit, str) or not SHA40_RE.fullmatch(commit):
            errors.append(f"{name}.commit must be a concrete lowercase 40-hex SHA")

        hashes = entry.get("expected_hashes")
        if not isinstance(hashes, dict):
            errors.append(f"{name}.expected_hashes must be an object")
            continue
        for field in ("manifest_sha256", "bundle_sha256"):
            value = hashes.get(field)
            if value == TOKEN_VAZIO and not require_artifact_hashes:
                continue
            if not isinstance(value, str) or not SHA256_RE.fullmatch(value):
                qualifier = "concrete " if require_artifact_hashes else "SHA-256 or TOKEN_VAZIO "
                errors.append(f"{name}.expected_hashes.{field} must be a {qualifier}value")

    platform = data.get("platform")
    if not isinstance(platform, dict):
        errors.append("platform must be an object")
    else:
        abis = platform.get("abis")
        if abis != ["arm64-v8a", "armeabi-v7a"]:
            errors.append("platform.abis must be exactly ['arm64-v8a', 'armeabi-v7a']")

    if errors:
        raise ContractError("\n".join(f"- {error}" for error in errors))
    return mapped


def emit_github_outputs(path: Path, mapped: dict[str, dict[str, Any]]) -> None:
    lines: list[str] = []
    for name, output_name in OUTPUT_NAMES.items():
        lines.append(f"{output_name}={mapped[name]['commit']}")
    with path.open("a", encoding="utf-8") as handle:
        handle.write("\n".join(lines) + "\n")


def command_validate(args: argparse.Namespace) -> int:
    data = load_lock(args.lock_file)
    mapped = validate(data, require_artifact_hashes=args.require_artifact_hashes)
    if args.github_output is not None:
        emit_github_outputs(args.github_output, mapped)
    print(f"[OK] {args.lock_file}: source lock contract valid")
    if not args.require_artifact_hashes:
        print("[INFO] artifact hashes may remain TOKEN_VAZIO until promotion")
    return 0


def command_get(args: argparse.Namespace) -> int:
    data = load_lock(args.lock_file)
    mapped = validate(data, require_artifact_hashes=False)
    if args.repository == data["integration_repository"]["name"]:
        entry = data["integration_repository"]
    else:
        entry = mapped.get(args.repository)
    if entry is None:
        raise ContractError(f"repository not found: {args.repository}")
    value = entry.get(args.field)
    if not isinstance(value, str):
        raise ContractError(f"field {args.field!r} is not a string in {args.repository}")
    print(value)
    return 0


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description=__doc__)
    subparsers = parser.add_subparsers(dest="command", required=True)

    validate_parser = subparsers.add_parser("validate", help="validate the complete lock contract")
    validate_parser.add_argument("lock_file", type=Path)
    validate_parser.add_argument("--github-output", type=Path)
    validate_parser.add_argument("--require-artifact-hashes", action="store_true")
    validate_parser.set_defaults(handler=command_validate)

    get_parser = subparsers.add_parser("get", help="read one validated string field")
    get_parser.add_argument("lock_file", type=Path)
    get_parser.add_argument("repository")
    get_parser.add_argument("field", choices=("branch", "commit", "url"))
    get_parser.set_defaults(handler=command_get)
    return parser


def main() -> int:
    parser = build_parser()
    args = parser.parse_args()
    try:
        return args.handler(args)
    except ContractError as exc:
        print(f"[FALHA] runtime lock contract:\n{exc}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
