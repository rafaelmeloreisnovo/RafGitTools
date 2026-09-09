#!/usr/bin/env python3
"""Fail-closed validator for RAFAELIA memory-epoch receipts (V1)."""

from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path
from typing import Any

SCHEMA_ID = "rafaelia.memory_epoch_receipt"
SCHEMA_VERSION = 1
TOKEN_VAZIO = "TOKEN_VAZIO"
SHA40 = re.compile(r"^[0-9a-f]{40}$")
SHA256 = re.compile(r"^[0-9a-f]{64}$")

SCOPES = (
    "PROCESS",
    "RUNTIME",
    "CONTAINER",
    "VM",
    "EXTERNAL_MEMORY",
    "LEARNING_STATE",
    "HOST",
    "PHYSICAL_EPOCH",
)
SCOPE_RANK = {name: rank for rank, name in enumerate(SCOPES)}
TRISTATE = {True, False, TOKEN_VAZIO}
DECISIONS = {"PROMOTE", "QUARANTINE", "BLOCK"}


class ValidationError(ValueError):
    pass


def require(condition: bool, message: str) -> None:
    if not condition:
        raise ValidationError(message)


def require_keys(obj: dict[str, Any], *, required: set[str], allowed: set[str], where: str) -> None:
    require(set(obj) >= required, f"{where}: missing keys {sorted(required - set(obj))}")
    require(set(obj) <= allowed, f"{where}: unknown keys {sorted(set(obj) - allowed)}")


def require_scope(value: Any, where: str) -> str:
    require(isinstance(value, str) and value in SCOPE_RANK, f"{where}: invalid scope")
    return value


def require_sha256(value: Any, where: str) -> str:
    require(isinstance(value, str) and SHA256.fullmatch(value) is not None, f"{where}: invalid sha256")
    return value


def require_nonempty(value: Any, where: str) -> str:
    require(isinstance(value, str) and bool(value.strip()), f"{where}: must be non-empty string")
    return value


def validate(data: Any) -> dict[str, Any]:
    require(isinstance(data, dict), "root: expected object")
    required = {
        "schema_id", "schema_version", "producer", "artifact", "memory_epoch", "reset",
        "test_corpus", "policy", "observer", "cross_epoch_probe", "residual_state",
        "replay_binding", "decision", "claim_allowed",
    }
    require_keys(data, required=required, allowed=required, where="root")
    require(data["schema_id"] == SCHEMA_ID, "schema_id: unsupported")
    require(data["schema_version"] == SCHEMA_VERSION, "schema_version: unsupported")

    producer = data["producer"]
    require(isinstance(producer, dict), "producer: expected object")
    producer_keys = {"repo", "commit", "adapter_id", "adapter_version"}
    require_keys(producer, required=producer_keys, allowed=producer_keys, where="producer")
    require(isinstance(producer["repo"], str) and "/" in producer["repo"], "producer.repo: invalid")
    require(isinstance(producer["commit"], str) and SHA40.fullmatch(producer["commit"]) is not None, "producer.commit: invalid")
    require_nonempty(producer["adapter_id"], "producer.adapter_id")
    require(
        isinstance(producer["adapter_version"], int)
        and not isinstance(producer["adapter_version"], bool)
        and producer["adapter_version"] >= 1,
        "producer.adapter_version: invalid",
    )

    artifact = data["artifact"]
    require(isinstance(artifact, dict), "artifact: expected object")
    require_keys(artifact, required={"sha256"}, allowed={"sha256"}, where="artifact")
    artifact_sha = require_sha256(artifact["sha256"], "artifact.sha256")

    epoch = data["memory_epoch"]
    require(isinstance(epoch, dict), "memory_epoch: expected object")
    epoch_keys = {"id", "claimed_scope", "evidenced_scope"}
    require_keys(epoch, required=epoch_keys, allowed=epoch_keys, where="memory_epoch")
    epoch_id = require_nonempty(epoch["id"], "memory_epoch.id")
    claimed = require_scope(epoch["claimed_scope"], "memory_epoch.claimed_scope")
    evidenced = require_scope(epoch["evidenced_scope"], "memory_epoch.evidenced_scope")
    require(
        SCOPE_RANK[claimed] <= SCOPE_RANK[evidenced],
        "memory_epoch: claimed scope exceeds evidenced scope",
    )

    reset = data["reset"]
    require(isinstance(reset, dict), "reset: expected object")
    require_keys(reset, required={"class", "evidence_refs"}, allowed={"class", "evidence_refs"}, where="reset")
    reset_class = require_scope(reset["class"], "reset.class")
    require(
        SCOPE_RANK[reset_class] <= SCOPE_RANK[evidenced],
        "reset.class: exceeds evidenced reset scope",
    )
    refs = reset["evidence_refs"]
    require(
        isinstance(refs, list) and refs and all(isinstance(x, str) and x.strip() for x in refs),
        "reset.evidence_refs: require one or more non-empty refs",
    )

    corpus = data["test_corpus"]
    require(isinstance(corpus, dict), "test_corpus: expected object")
    require_keys(corpus, required={"sha256"}, allowed={"sha256"}, where="test_corpus")
    corpus_sha = require_sha256(corpus["sha256"], "test_corpus.sha256")

    policy = data["policy"]
    require(isinstance(policy, dict), "policy: expected object")
    require_keys(policy, required={"version", "sha256"}, allowed={"version", "sha256"}, where="policy")
    policy_version = require_nonempty(policy["version"], "policy.version")
    require_sha256(policy["sha256"], "policy.sha256")

    observer = data["observer"]
    require(isinstance(observer, dict), "observer: expected object")
    observer_keys = {"id", "independent", "runtime_can_modify_control_plane"}
    require_keys(observer, required=observer_keys, allowed=observer_keys, where="observer")
    require_nonempty(observer["id"], "observer.id")
    require(observer["independent"] is True, "observer.independent: must be true")
    require(
        observer["runtime_can_modify_control_plane"] is False,
        "observer.runtime_can_modify_control_plane: must be false",
    )

    probe = data["cross_epoch_probe"]
    require(isinstance(probe, dict), "cross_epoch_probe: expected object")
    probe_keys = {"marker_owner", "authorized_interface", "unexpected_correlation", "reproduced"}
    require_keys(probe, required=probe_keys, allowed=probe_keys, where="cross_epoch_probe")
    require(
        probe["marker_owner"] == "EXTERNAL_OBSERVER",
        "cross_epoch_probe.marker_owner: marker/canary must be externally owned",
    )
    require(probe["authorized_interface"] is True, "cross_epoch_probe.authorized_interface: must be true")
    require(probe["unexpected_correlation"] in TRISTATE, "cross_epoch_probe.unexpected_correlation: invalid tristate")
    require(probe["reproduced"] in TRISTATE, "cross_epoch_probe.reproduced: invalid tristate")

    residual = data["residual_state"]
    require(residual in TRISTATE, "residual_state: invalid tristate")

    replay = data["replay_binding"]
    require(isinstance(replay, dict), "replay_binding: expected object")
    replay_keys = {"artifact_sha256", "epoch_id", "reset_class", "test_corpus_sha256", "policy_version"}
    require_keys(replay, required=replay_keys, allowed=replay_keys, where="replay_binding")
    require(replay["artifact_sha256"] == artifact_sha, "replay_binding.artifact_sha256: mismatch")
    require(replay["epoch_id"] == epoch_id, "replay_binding.epoch_id: mismatch")
    require(replay["reset_class"] == reset_class, "replay_binding.reset_class: mismatch")
    require(replay["test_corpus_sha256"] == corpus_sha, "replay_binding.test_corpus_sha256: mismatch")
    require(replay["policy_version"] == policy_version, "replay_binding.policy_version: mismatch")

    decision = data["decision"]
    require(decision in DECISIONS, "decision: invalid")
    require(data["claim_allowed"] is False, "claim_allowed: V1 must remain false")

    mandatory_unknown = (
        residual == TOKEN_VAZIO
        or probe["unexpected_correlation"] == TOKEN_VAZIO
        or probe["reproduced"] == TOKEN_VAZIO
    )
    if mandatory_unknown:
        require(decision != "PROMOTE", "decision: TOKEN_VAZIO blocks promotion")

    if probe["unexpected_correlation"] is True and probe["reproduced"] is True:
        require(
            decision == "QUARANTINE",
            "decision: reproducible unexpected cross-boundary correlation requires QUARANTINE",
        )

    if residual is True:
        require(decision != "PROMOTE", "decision: observed residual state blocks promotion")

    return {
        "schema_id": SCHEMA_ID,
        "schema_version": SCHEMA_VERSION,
        "producer_repo": producer["repo"],
        "producer_commit": producer["commit"],
        "adapter_id": producer["adapter_id"],
        "epoch_id": epoch_id,
        "claimed_scope": claimed,
        "evidenced_scope": evidenced,
        "reset_class": reset_class,
        "decision": decision,
        "claim_allowed": False,
        "promotion_allowed": decision == "PROMOTE",
    }


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("receipt", type=Path)
    parser.add_argument("--json", action="store_true", help="emit compact validator output")
    args = parser.parse_args(argv)
    try:
        data = json.loads(args.receipt.read_text(encoding="utf-8"))
        result = validate(data)
    except (OSError, json.JSONDecodeError, ValidationError) as exc:
        print(f"FAIL memory_epoch_receipt: {exc}", file=sys.stderr)
        return 2
    if args.json:
        print(json.dumps(result, sort_keys=True, separators=(",", ":")))
    else:
        print(
            "PASS memory_epoch_receipt "
            f"epoch={result['epoch_id']} reset={result['reset_class']} "
            f"decision={result['decision']} claim_allowed=false"
        )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
