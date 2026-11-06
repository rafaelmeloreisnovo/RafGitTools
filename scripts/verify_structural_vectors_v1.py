#!/usr/bin/env python3
"""Verify the seven structural counts of the recovered-shard V1 artifact.

Hosted Python/std-library boundary. Inputs are private local readbacks with
externally supplied SHA-256 pins. The receipt contains aggregate evidence.
"""
import argparse
import hashlib
import json
from pathlib import Path
import re
import sys

SCHEMA = "rafaelia.novoexport.018.structural-feature-index.v1"
FEATURES = [
    "mapping_node_count", "message_object_count", "user_role_count",
    "assistant_role_count", "other_role_count", "parent_link_count",
    "dangling_parent_link_count",
]
AGGREGATES = [
    "nodes", "message_objects", "user", "assistant", "other",
    "parent_links", "dangling_parent_links",
]
HEX64 = re.compile(r"[0-9a-f]{64}")
MAX_SOURCE_BYTES = 64 * 1024 * 1024
MAX_ARTIFACT_BYTES = 2 * 1024 * 1024


class LineageError(ValueError):
    pass


def unique_object(pairs):
    result = {}
    for key, value in pairs:
        if key in result:
            raise LineageError("DUPLICATE_JSON_KEY")
        result[key] = value
    return result


def reject_nonfinite(_value):
    raise LineageError("NONFINITE_JSON_NUMBER")


def read_bounded(path, limit):
    with Path(path).open("rb") as stream:
        data = stream.read(limit + 1)
    if len(data) > limit:
        raise LineageError("INPUT_LIMIT_REACHED")
    return data


def pinned_json(data, expected, limit):
    if len(data) > limit:
        raise LineageError("INPUT_LIMIT_REACHED")
    if not isinstance(expected, str) or not HEX64.fullmatch(expected):
        raise LineageError("INVALID_DIGEST_PIN")
    if hashlib.sha256(data).hexdigest() != expected:
        raise LineageError("BYTE_HASH_MISMATCH")
    try:
        return json.loads(data, object_pairs_hook=unique_object,
                          parse_constant=reject_nonfinite)
    except (UnicodeError, json.JSONDecodeError, RecursionError):
        raise LineageError("INVALID_JSON") from None


def structural_counts(conversation):
    if not isinstance(conversation, dict):
        raise LineageError("INVALID_CONVERSATION")
    mapping = conversation.get("mapping")
    if not isinstance(mapping, dict):
        raise LineageError("INVALID_MAPPING")
    counts = [len(mapping), 0, 0, 0, 0, 0, 0]
    for node in mapping.values():
        if not isinstance(node, dict):
            raise LineageError("INVALID_NODE")
        message = node.get("message")
        if message is not None:
            if not isinstance(message, dict):
                raise LineageError("INVALID_MESSAGE")
            author = message.get("author") or {}
            if not isinstance(author, dict):
                raise LineageError("INVALID_AUTHOR")
            role = author.get("role")
            counts[1] += 1
            role_index = {"user": 2, "assistant": 3}.get(role, 4)
            counts[role_index] += 1
        parent = node.get("parent")
        if parent is not None:
            if not isinstance(parent, str):
                raise LineageError("INVALID_PARENT")
            counts[5] += 1
            counts[6] += int(parent not in mapping)
    return counts


def verify(source_bytes, artifact_bytes, source_sha256, artifact_sha256):
    source = pinned_json(source_bytes, source_sha256, MAX_SOURCE_BYTES)
    artifact = pinned_json(artifact_bytes, artifact_sha256, MAX_ARTIFACT_BYTES)
    if not isinstance(source, list) or not source or not isinstance(artifact, dict):
        raise LineageError("INVALID_INPUT_SHAPE")
    if (artifact.get("schema") != SCHEMA
            or artifact.get("unit") != "per_conversation_in_source_order"
            or artifact.get("scope") != "ONE_RECOVERED_JSON_SHARD_ONLY"
            or artifact.get("feature_dictionary") != FEATURES):
        raise LineageError("UNSUPPORTED_FEATURE_CONTRACT")
    provenance = artifact.get("provenance")
    if not isinstance(provenance, dict):
        raise LineageError("MISSING_PROVENANCE")
    size = provenance.get("source_size_bytes")
    if (provenance.get("source_sha256") != source_sha256
            or type(size) is not int or size != len(source_bytes)):
        raise LineageError("SOURCE_PROVENANCE_MISMATCH")
    records = artifact.get("records")
    if not isinstance(records, list) or len(records) != len(source):
        raise LineageError("RECORD_COUNT_MISMATCH")
    totals = [0] * len(FEATURES)
    for ordinal, (conversation, record) in enumerate(zip(source, records)):
        if not isinstance(record, dict) or type(record.get("ordinal")) is not int:
            raise LineageError("INVALID_ORDINAL")
        if record["ordinal"] != ordinal:
            raise LineageError("SOURCE_ORDER_MISMATCH")
        vector = record.get("features_7d_integer")
        if (not isinstance(vector, list) or len(vector) != len(FEATURES)
                or any(type(value) is not int or value < 0 for value in vector)):
            raise LineageError("INVALID_FEATURE_VECTOR")
        expected = structural_counts(conversation)
        if vector != expected:
            raise LineageError("STRUCTURAL_VECTOR_MISMATCH")
        ref = record.get("conversation_ref_sha256")
        if not isinstance(ref, str) or not HEX64.fullmatch(ref):
            raise LineageError("INVALID_REFERENCE_FORMAT")
        totals = [total + value for total, value in zip(totals, expected)]
    aggregate = artifact.get("aggregate")
    expected_aggregate = dict(zip(AGGREGATES, totals))
    expected_aggregate["conversations"] = len(source)
    if (not isinstance(aggregate, dict)
            or any(type(value) is not int for value in aggregate.values())
            or aggregate != expected_aggregate):
        raise LineageError("AGGREGATE_MISMATCH")
    return {
        "schema": "rafaelia.structural-lineage-receipt/v1",
        "state": "VERIFIED_LIMITED",
        "source_sha256": source_sha256,
        "artifact_sha256": artifact_sha256,
        "records_verified": len(records),
        "feature_values_verified": len(records) * len(FEATURES),
        "aggregate": aggregate,
        "structural_derivation": "VERIFIED",
        "reference_derivation": "TOKEN_VAZIO_DERIVATION_RECIPE",
        "claim_allowed": False,
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", required=True)
    parser.add_argument("--artifact", required=True)
    parser.add_argument("--source-sha256", required=True)
    parser.add_argument("--artifact-sha256", required=True)
    args = parser.parse_args()
    try:
        receipt = verify(
            read_bounded(args.source, MAX_SOURCE_BYTES),
            read_bounded(args.artifact, MAX_ARTIFACT_BYTES),
            args.source_sha256, args.artifact_sha256,
        )
    except LineageError as exc:
        print(json.dumps({"state": "BLOCKED", "error": str(exc)}), file=sys.stderr)
        return 3
    except (OSError, TypeError, ValueError, RecursionError):
        print(json.dumps({"state": "BLOCKED", "error": "INPUT_ERROR"}), file=sys.stderr)
        return 3
    print(json.dumps(receipt, sort_keys=True, separators=(",", ":")))
    return 0


if __name__ == "__main__":
    sys.exit(main())
