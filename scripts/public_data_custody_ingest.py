#!/usr/bin/env python3
"""Public official-data custody ingestion with privacy-minimized projections.

This module intentionally does not investigate people. It snapshots approved
public endpoints/files, strips person-level identifiers from public artifacts,
hashes source/projection bytes, and emits claim-bounded receipts.
"""
from __future__ import annotations

import argparse
import csv
import hashlib
import io
import json
import os
import re
import statistics
import sys
import urllib.parse
import urllib.request
from collections import defaultdict
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

CONFIG_PATH = Path(__file__).resolve().parents[1] / "configs" / "public-data-custody-ingestion.v1.json"
SCHEMA = "rafgittools.public-data-custody-receipt/v1"

FORBIDDEN_PAT_SECRETS = {
    "PAT_ENV", "PAT_ACTIONS", "PAT_AGENTS", "PAT_CODESPACES",
    "PAT_DEPENDABOT", "PAT_PRIVATE_PROCESSING",
}


def utc_now() -> str:
    return datetime.now(timezone.utc).isoformat()


def sha256_bytes(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def canonical_json_bytes(value: Any) -> bytes:
    return (json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":")) + "\n").encode("utf-8")


def load_config(path: Path = CONFIG_PATH) -> dict[str, Any]:
    cfg = json.loads(path.read_text(encoding="utf-8"))
    if cfg.get("schema") != "rafgittools.public-data-custody-ingestion/v1":
        raise ValueError("config_schema_mismatch")
    fallbacks = set(cfg["secret_policy"]["forbidden_fallbacks"])
    if not FORBIDDEN_PAT_SECRETS.issubset(fallbacks):
        raise ValueError("secret_fallback_policy_incomplete")
    return cfg


def normalize_key(key: str) -> str:
    return re.sub(r"[^a-z0-9]+", "_", key.casefold()).strip("_")


def forbidden_keys(cfg: dict[str, Any]) -> set[str]:
    return {normalize_key(k) for k in cfg["privacy"]["public_output_forbidden_person_fields"]}


def sanitize_public(value: Any, cfg: dict[str, Any]) -> Any:
    blocked = forbidden_keys(cfg)
    if isinstance(value, dict):
        clean: dict[str, Any] = {}
        for key, nested in value.items():
            if normalize_key(str(key)) in blocked:
                continue
            clean[str(key)] = sanitize_public(nested, cfg)
        return clean
    if isinstance(value, list):
        return [sanitize_public(item, cfg) for item in value]
    return value


def validate_source(cfg: dict[str, Any], source_id: str) -> dict[str, Any]:
    source = cfg["sources"].get(source_id)
    if not isinstance(source, dict):
        raise ValueError("source_not_allowlisted")
    if source.get("state") not in {"CONFIGURED", "READY_FOR_SELECTED_PUBLIC_FILE"}:
        raise ValueError("source_not_executable")
    return source


def build_url(source: dict[str, Any], query: dict[str, Any]) -> str:
    base = source.get("base_url")
    endpoint = source.get("endpoint")
    if not base or not endpoint:
        raise ValueError("source_has_no_api_endpoint")
    parsed = urllib.parse.urlparse(base)
    if parsed.scheme != "https":
        raise ValueError("https_required")
    query_string = urllib.parse.urlencode([(str(k), str(v)) for k, v in sorted(query.items())], doseq=True)
    return base.rstrip("/") + endpoint + ("?" + query_string if query_string else "")


def fetch_api(cfg: dict[str, Any], source_id: str, query: dict[str, Any], timeout: int = 30) -> tuple[bytes, str]:
    source = validate_source(cfg, source_id)
    url = build_url(source, query)
    headers = {"Accept": "application/json", "User-Agent": "RafGitTools-PublicCustody/1.0"}
    secret_ref = source.get("secret_reference")
    if secret_ref:
        if secret_ref in FORBIDDEN_PAT_SECRETS:
            raise ValueError("github_pat_forbidden_for_public_data")
        token = os.environ.get(secret_ref)
        if not token:
            raise RuntimeError(f"required_secret_missing:{secret_ref}")
        headers[str(source.get("auth_header"))] = token
    req = urllib.request.Request(url, headers=headers, method="GET")
    with urllib.request.urlopen(req, timeout=timeout) as response:
        raw = response.read()
        final = response.geturl()
    if urllib.parse.urlparse(final).hostname != urllib.parse.urlparse(source["base_url"]).hostname:
        raise RuntimeError("redirect_host_changed")
    return raw, final


def first_difference_direction(series: list[float]) -> dict[str, Any]:
    if len(series) < 2:
        return {"state": "TOKEN_VAZIO_INSUFFICIENT_SERIES", "direction": None, "delta": None}
    delta = series[-1] - series[-2]
    direction = "UP" if delta > 0 else "DOWN" if delta < 0 else "FLAT"
    return {
        "state": "BASELINE_CONTROL_ONLY",
        "direction": direction,
        "delta": delta,
        "claim_allowed": False,
        "canonical_plect": False,
    }


def aggregate_rows(
    rows: list[dict[str, str]],
    group_fields: list[str],
    value_fields: list[str],
    min_group_size: int,
) -> list[dict[str, Any]]:
    buckets: dict[tuple[str, ...], dict[str, Any]] = {}
    for row in rows:
        key = tuple(row.get(field, "") for field in group_fields)
        bucket = buckets.setdefault(key, {"count": 0, "values": defaultdict(list)})
        bucket["count"] += 1
        for field in value_fields:
            raw = (row.get(field) or "").strip().replace(".", "").replace(",", ".")
            try:
                bucket["values"][field].append(float(raw))
            except ValueError:
                pass

    out: list[dict[str, Any]] = []
    for key in sorted(buckets):
        bucket = buckets[key]
        if bucket["count"] < min_group_size:
            continue
        item: dict[str, Any] = {field: value for field, value in zip(group_fields, key)}
        item["count"] = bucket["count"]
        for field in value_fields:
            values = bucket["values"].get(field, [])
            if not values:
                continue
            item[field + "_sum"] = sum(values)
            item[field + "_mean"] = statistics.fmean(values)
            item[field + "_min"] = min(values)
            item[field + "_max"] = max(values)
        out.append(item)
    return out


def sanitize_csv_aggregate(
    raw: bytes,
    *,
    group_fields: list[str],
    value_fields: list[str],
    min_group_size: int,
) -> list[dict[str, Any]]:
    text = raw.decode("utf-8-sig", errors="strict")
    sample = text[:8192]
    dialect = csv.Sniffer().sniff(sample, delimiters=";,\t|")
    rows = list(csv.DictReader(io.StringIO(text), dialect=dialect))
    return aggregate_rows(rows, group_fields, value_fields, min_group_size)


def write_snapshot_receipt(
    *,
    source_id: str,
    source_url: str,
    raw: bytes,
    projection: Any,
    output_dir: Path,
    query: dict[str, Any] | None = None,
) -> dict[str, Any]:
    output_dir.mkdir(parents=True, exist_ok=True)
    projection_bytes = canonical_json_bytes(projection)
    projection_path = output_dir / "projection.json"
    projection_path.write_bytes(projection_bytes)
    receipt = {
        "schema": SCHEMA,
        "observed_at": utc_now(),
        "source_id": source_id,
        "source_url": source_url,
        "query": query or {},
        "source_bytes": len(raw),
        "source_sha256": sha256_bytes(raw),
        "projection_bytes": len(projection_bytes),
        "projection_sha256": sha256_bytes(projection_bytes),
        "privacy_mode": "AGGREGATE_OR_IDENTIFIER_STRIPPED",
        "raw_source_persisted": False,
        "claim_allowed": False,
        "plect_state": "TOKEN_VAZIO_CANONICAL_OPERATOR",
        "invariants": [
            "SOURCE != ARTIFACT != EVIDENCE != CLAIM",
            "ANOMALY != WRONGDOING",
            "SALARY != SUSPICION",
            "PLECT_BASELINE != PLECT_CANONICAL",
        ],
    }
    (output_dir / "receipt.json").write_bytes(canonical_json_bytes(receipt))
    return receipt


def api_snapshot(args: argparse.Namespace) -> int:
    cfg = load_config()
    query = json.loads(args.query_json or "{}")
    if not isinstance(query, dict):
        raise ValueError("query_must_be_object")
    raw, final_url = fetch_api(cfg, args.source, query)
    try:
        decoded = json.loads(raw)
    except json.JSONDecodeError as exc:
        raise RuntimeError("official_api_returned_non_json") from exc
    projection = sanitize_public(decoded, cfg)
    receipt = write_snapshot_receipt(
        source_id=args.source,
        source_url=final_url,
        raw=raw,
        projection=projection,
        output_dir=Path(args.output),
        query=query,
    )
    print(json.dumps(receipt, ensure_ascii=False, sort_keys=True))
    return 0


def aggregate_file(args: argparse.Namespace) -> int:
    cfg = load_config()
    source = validate_source(cfg, args.source)
    if source.get("mode") != "LOCAL_PUBLIC_FILE":
        raise ValueError("source_not_local_public_file_lane")
    raw = Path(args.input).read_bytes()
    projection = sanitize_csv_aggregate(
        raw,
        group_fields=[x for x in args.group_fields.split(",") if x],
        value_fields=[x for x in args.value_fields.split(",") if x],
        min_group_size=max(args.min_group_size, int(cfg["privacy"]["salary_policy"]["minimum_group_size"])),
    )
    receipt = write_snapshot_receipt(
        source_id=args.source,
        source_url=str(source["landing_url"]),
        raw=raw,
        projection=projection,
        output_dir=Path(args.output),
    )
    print(json.dumps(receipt, ensure_ascii=False, sort_keys=True))
    return 0


def main() -> int:
    parser = argparse.ArgumentParser()
    sub = parser.add_subparsers(dest="command", required=True)

    api = sub.add_parser("api-snapshot")
    api.add_argument("--source", required=True)
    api.add_argument("--query-json", default="{}")
    api.add_argument("--output", required=True)
    api.set_defaults(func=api_snapshot)

    bulk = sub.add_parser("aggregate-file")
    bulk.add_argument("--source", required=True)
    bulk.add_argument("--input", required=True)
    bulk.add_argument("--group-fields", required=True)
    bulk.add_argument("--value-fields", required=True)
    bulk.add_argument("--min-group-size", type=int, default=5)
    bulk.add_argument("--output", required=True)
    bulk.set_defaults(func=aggregate_file)

    args = parser.parse_args()
    return int(args.func(args))


if __name__ == "__main__":
    raise SystemExit(main())
