#!/usr/bin/env python3
"""Dependency-free ContextBundle V1 -> V2 adapter and semantic validator.

The adapter is intentionally fail-closed:
- it never invents missing intent, timestamps, privacy, hashes or source generation;
- missing values become TOKEN_VAZIO or null where the V2 contract allows it;
- unknown V1 payload values are not copied blindly into annotations;
- model/context materialization remains separate from source-reference adaptation.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
from pathlib import Path
from typing import Any, Dict, Iterable, List, Mapping, MutableMapping, Optional

SCHEMA_V1 = "rafaelia.context_bundle.v1"
SCHEMA_V2 = "rafaelia.context_bundle.v2"

VARIANT_RAFGITTOOLS = "rafgittools.docs-v1"
VARIANT_LLAMA = "llamarafaelia.docs-v1"
VARIANT_PRIVATE = "conversation-chunks-private.docs-v1"
VARIANT_NATIVE = "native-v2"

ALLOWED_PRIVACY = {"PUBLIC", "INTERNAL", "PRIVATE", "SENSITIVE", "TOKEN_VAZIO"}
ALLOWED_VISIBILITY = {"LOCAL_ONLY", "PRIVATE", "INTERNAL", "PUBLIC", "TOKEN_VAZIO"}
ALLOWED_EPISTEMIC = {"SOURCE_OBSERVED", "INDEX_REF", "MATERIALIZED", "TOKEN_VAZIO"}
HEX64 = re.compile(r"^[0-9a-fA-F]{64}$")


class ContextBundleError(ValueError):
    pass


def _require_mapping(value: Any, label: str) -> Mapping[str, Any]:
    if not isinstance(value, Mapping):
        raise ContextBundleError(f"{label} must be an object")
    return value


def _require_string_list(value: Any, label: str) -> List[str]:
    if not isinstance(value, list):
        raise ContextBundleError(f"{label} must be an array")
    out: List[str] = []
    for index, item in enumerate(value):
        if not isinstance(item, str) or not item:
            raise ContextBundleError(f"{label}[{index}] must be a non-empty string")
        out.append(item)
    return out


def _token_vazio_intent() -> Dict[str, Any]:
    return {"objective": "TOKEN_VAZIO", "request_id": None, "state": "TOKEN_VAZIO"}


def _extract_intent(metadata: Mapping[str, Any]) -> Dict[str, Any]:
    explicit = metadata.get("intent")
    if isinstance(explicit, str) and explicit.strip():
        return {
            "objective": explicit.strip(),
            "request_id": metadata.get("request_id") if isinstance(metadata.get("request_id"), str) else None,
            "state": "OBSERVED",
        }
    if isinstance(explicit, Mapping):
        objective = explicit.get("objective")
        if isinstance(objective, str) and objective.strip():
            request_id = explicit.get("request_id")
            return {
                "objective": objective.strip(),
                "request_id": request_id if isinstance(request_id, str) else None,
                "state": "OBSERVED",
            }
    return _token_vazio_intent()


def _extract_privacy(metadata: Mapping[str, Any]) -> str:
    value = metadata.get("privacy_class")
    if isinstance(value, str) and value in ALLOWED_PRIVACY:
        return value
    return "TOKEN_VAZIO"


def detect_v1_variant(document: Mapping[str, Any]) -> str:
    if document.get("schema") != SCHEMA_V1:
        raise ContextBundleError("input is not rafaelia.context_bundle.v1")

    signatures = []
    if "chunks" in document or "created_at" in document or "metadata" in document:
        signatures.append(VARIANT_RAFGITTOOLS)
    if "conversation_chunks" in document or "generated_at" in document:
        signatures.append(VARIANT_LLAMA)
    if "chunk_refs" in document or "intent_candidates" in document:
        signatures.append(VARIANT_PRIVATE)

    signatures = list(dict.fromkeys(signatures))
    if not signatures:
        raise ContextBundleError("unable to identify V1 variant")
    if len(signatures) != 1:
        raise ContextBundleError(
            "ambiguous V1 variant: " + ", ".join(signatures)
        )
    return signatures[0]


def _unmapped_keys(document: Mapping[str, Any], known: Iterable[str]) -> List[str]:
    known_set = set(known)
    return sorted(str(key) for key in document.keys() if key not in known_set)


def adapt_v1(document: Mapping[str, Any], source_hint: Optional[str] = None) -> Dict[str, Any]:
    document = _require_mapping(document, "document")
    variant = source_hint or detect_v1_variant(document)
    if variant not in {VARIANT_RAFGITTOOLS, VARIANT_LLAMA, VARIANT_PRIVATE}:
        raise ContextBundleError(f"unsupported source variant: {variant}")

    if document.get("schema") != SCHEMA_V1:
        raise ContextBundleError("source schema must be rafaelia.context_bundle.v1")

    bundle_id = document.get("bundle_id")
    if not isinstance(bundle_id, str) or not bundle_id:
        raise ContextBundleError("bundle_id must be a non-empty string")

    annotations: Dict[str, Any] = {}
    evidence_refs: List[str] = []
    constraints: List[str] = []
    unresolved: List[str] = []
    source_generation = None

    if variant == VARIANT_RAFGITTOOLS:
        chunks = _require_string_list(document.get("chunks"), "chunks")
        created_at = document.get("created_at")
        if not isinstance(created_at, str) or not created_at:
            created_at = "TOKEN_VAZIO"
            unresolved.append("created_at")
        metadata = document.get("metadata")
        if metadata is None:
            metadata_map: Mapping[str, Any] = {}
        else:
            metadata_map = _require_mapping(metadata, "metadata")
            annotations["legacy_metadata"] = dict(metadata_map)
        intent = _extract_intent(metadata_map)
        privacy_class = _extract_privacy(metadata_map)
        if intent["state"] == "TOKEN_VAZIO":
            unresolved.append("intent")
        if privacy_class == "TOKEN_VAZIO":
            unresolved.append("privacy_class")
        known = {"schema", "bundle_id", "chunks", "created_at", "metadata"}

    elif variant == VARIANT_LLAMA:
        chunks = _require_string_list(document.get("conversation_chunks"), "conversation_chunks")
        created_at = document.get("generated_at")
        if not isinstance(created_at, str) or not created_at:
            created_at = "TOKEN_VAZIO"
            unresolved.append("created_at")
        evidence_refs = _require_string_list(document.get("evidence_refs", []), "evidence_refs")
        constraints = _require_string_list(document.get("constraints", []), "constraints")
        intent = _token_vazio_intent()
        privacy_class = "TOKEN_VAZIO"
        unresolved.extend(["intent", "privacy_class"])
        known = {
            "schema", "bundle_id", "conversation_chunks", "generated_at",
            "evidence_refs", "constraints"
        }

    else:
        chunks = _require_string_list(document.get("chunk_refs"), "chunk_refs")
        created_at = "TOKEN_VAZIO"
        intent = _token_vazio_intent()
        privacy_class = "TOKEN_VAZIO"
        unresolved.extend(["created_at", "intent", "privacy_class"])

        candidates = document.get("intent_candidates")
        if candidates is not None:
            if not isinstance(candidates, list):
                raise ContextBundleError("intent_candidates must be an array")
            annotations["legacy_intent_candidates"] = candidates

        legacy_annotations = document.get("annotations")
        if legacy_annotations is not None:
            annotations["legacy_annotations"] = dict(
                _require_mapping(legacy_annotations, "annotations")
            )
        known = {"schema", "bundle_id", "chunk_refs", "intent_candidates", "annotations"}

    result: Dict[str, Any] = {
        "schema": SCHEMA_V2,
        "bundle_id": bundle_id,
        "created_at": created_at,
        "intent": intent,
        "privacy_class": privacy_class,
        "source_generation": source_generation,
        "resources": [],
        "chunk_refs": chunks,
        "segments": [],
        "evidence_refs": evidence_refs,
        "constraints": constraints,
        "annotations": annotations,
        "compatibility": {
            "source_schema": SCHEMA_V1,
            "source_variant": variant,
            "adapter": "scripts/context_bundle_v2.py",
            "unresolved_fields": sorted(set(unresolved)),
            "unmapped_keys": _unmapped_keys(document, known),
        },
    }
    validate_v2(result)
    return result


def _validate_sha(value: Any, label: str, allow_null: bool) -> None:
    if value is None and allow_null:
        return
    if value == "TOKEN_VAZIO":
        return
    if not isinstance(value, str) or not HEX64.fullmatch(value):
        raise ContextBundleError(f"{label} must be SHA-256, TOKEN_VAZIO" + (", or null" if allow_null else ""))


def validate_v2(document: Mapping[str, Any]) -> None:
    document = _require_mapping(document, "document")
    required = {
        "schema", "bundle_id", "created_at", "intent", "privacy_class",
        "resources", "chunk_refs", "segments", "evidence_refs",
        "constraints", "annotations", "compatibility"
    }
    missing = sorted(required - set(document.keys()))
    if missing:
        raise ContextBundleError("missing required fields: " + ", ".join(missing))

    if document.get("schema") != SCHEMA_V2:
        raise ContextBundleError("schema must be rafaelia.context_bundle.v2")
    if not isinstance(document.get("bundle_id"), str) or not document["bundle_id"]:
        raise ContextBundleError("bundle_id must be a non-empty string")
    created_at = document.get("created_at")
    if not isinstance(created_at, str) or not created_at:
        raise ContextBundleError("created_at must be a non-empty string or TOKEN_VAZIO")

    intent = _require_mapping(document.get("intent"), "intent")
    if set(intent.keys()) != {"objective", "request_id", "state"}:
        raise ContextBundleError("intent fields must be exactly objective, request_id, state")
    if not isinstance(intent.get("objective"), str) or not intent["objective"]:
        raise ContextBundleError("intent.objective must be a non-empty string")
    if intent.get("request_id") is not None and not isinstance(intent.get("request_id"), str):
        raise ContextBundleError("intent.request_id must be string or null")
    if intent.get("state") not in {"OBSERVED", "DECLARED", "TOKEN_VAZIO"}:
        raise ContextBundleError("invalid intent.state")

    if document.get("privacy_class") not in ALLOWED_PRIVACY:
        raise ContextBundleError("invalid privacy_class")

    source_generation = document.get("source_generation")
    if source_generation is not None:
        source_generation = _require_mapping(source_generation, "source_generation")
        for key in ("provider", "generation_id"):
            if not isinstance(source_generation.get(key), str) or not source_generation[key]:
                raise ContextBundleError(f"source_generation.{key} must be a non-empty string")
        _validate_sha(source_generation.get("manifest_sha256"), "source_generation.manifest_sha256", True)

    resources = document.get("resources")
    if not isinstance(resources, list):
        raise ContextBundleError("resources must be an array")
    resource_ids = set()
    for index, resource in enumerate(resources):
        resource = _require_mapping(resource, f"resources[{index}]")
        for key in ("resource_id", "provider", "source", "locator"):
            if not isinstance(resource.get(key), str) or not resource[key]:
                raise ContextBundleError(f"resources[{index}].{key} must be a non-empty string")
        rid = resource["resource_id"]
        if rid in resource_ids:
            raise ContextBundleError(f"duplicate resource_id: {rid}")
        resource_ids.add(rid)
        if resource.get("visibility") not in ALLOWED_VISIBILITY:
            raise ContextBundleError(f"invalid resources[{index}].visibility")
        if resource.get("epistemic_state") not in ALLOWED_EPISTEMIC:
            raise ContextBundleError(f"invalid resources[{index}].epistemic_state")
        _validate_sha(resource.get("sha256"), f"resources[{index}].sha256", True)

    _require_string_list(document.get("chunk_refs"), "chunk_refs")
    _require_string_list(document.get("evidence_refs"), "evidence_refs")
    _require_string_list(document.get("constraints"), "constraints")

    segments = document.get("segments")
    if not isinstance(segments, list):
        raise ContextBundleError("segments must be an array")
    segment_ids = set()
    for index, segment in enumerate(segments):
        segment = _require_mapping(segment, f"segments[{index}]")
        for key in ("segment_id", "source_ref", "text"):
            if not isinstance(segment.get(key), str):
                raise ContextBundleError(f"segments[{index}].{key} must be a string")
        sid = segment["segment_id"]
        if not sid:
            raise ContextBundleError(f"segments[{index}].segment_id must be non-empty")
        if sid in segment_ids:
            raise ContextBundleError(f"duplicate segment_id: {sid}")
        segment_ids.add(sid)
        if len(segment["text"]) > 32768:
            raise ContextBundleError(f"segments[{index}].text exceeds 32768 characters")
        if segment.get("privacy_class") not in ALLOWED_PRIVACY:
            raise ContextBundleError(f"invalid segments[{index}].privacy_class")
        _validate_sha(segment.get("text_sha256"), f"segments[{index}].text_sha256", False)

    _require_mapping(document.get("annotations"), "annotations")

    compatibility = _require_mapping(document.get("compatibility"), "compatibility")
    for key in ("source_schema", "source_variant", "adapter"):
        if not isinstance(compatibility.get(key), str) or not compatibility[key]:
            raise ContextBundleError(f"compatibility.{key} must be a non-empty string")
    if compatibility.get("source_variant") not in {
        VARIANT_NATIVE, VARIANT_RAFGITTOOLS, VARIANT_LLAMA, VARIANT_PRIVATE
    }:
        raise ContextBundleError("invalid compatibility.source_variant")
    _require_string_list(compatibility.get("unresolved_fields"), "compatibility.unresolved_fields")
    _require_string_list(compatibility.get("unmapped_keys"), "compatibility.unmapped_keys")


def load_json(path: Path) -> Mapping[str, Any]:
    with path.open("r", encoding="utf-8") as handle:
        return _require_mapping(json.load(handle), str(path))


def write_json(path: Path, value: Mapping[str, Any]) -> None:
    payload = json.dumps(value, ensure_ascii=False, indent=2, sort_keys=True) + "\n"
    if str(path) == "-":
        sys.stdout.write(payload)
        return
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(payload, encoding="utf-8")


def command_adapt(args: argparse.Namespace) -> int:
    source = load_json(Path(args.input))
    result = adapt_v1(source, args.source_hint)
    write_json(Path(args.output), result)
    return 0


def command_validate(args: argparse.Namespace) -> int:
    validate_v2(load_json(Path(args.input)))
    print("CONTEXT_BUNDLE_V2_VALID")
    return 0


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest="command", required=True)

    adapt = sub.add_parser("adapt", help="adapt one known V1 variant to V2")
    adapt.add_argument("input")
    adapt.add_argument("output")
    adapt.add_argument(
        "--source-hint",
        choices=[VARIANT_RAFGITTOOLS, VARIANT_LLAMA, VARIANT_PRIVATE],
        default=None,
    )
    adapt.set_defaults(func=command_adapt)

    validate = sub.add_parser("validate", help="validate V2 semantic invariants")
    validate.add_argument("input")
    validate.set_defaults(func=command_validate)
    return parser


def main(argv: Optional[List[str]] = None) -> int:
    parser = build_parser()
    args = parser.parse_args(argv)
    try:
        return int(args.func(args))
    except (ContextBundleError, json.JSONDecodeError, OSError) as error:
        print(f"ERROR: {error}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
