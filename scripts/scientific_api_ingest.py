#!/usr/bin/env python3
"""Bounded official scientific API ingestion for climate and space data.

The lane preserves exact source bytes only in-memory, stores a canonical JSON
projection plus a receipt, and never reuses GitHub PAT capabilities as external
API credentials.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import urllib.parse
import urllib.request
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
CONFIG_PATH = ROOT / "configs" / "scientific-api-sources.v1.json"
SCHEMA = "rafgittools.scientific-api-custody-receipt/v1"
FORBIDDEN_GITHUB_PATS = {
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
    if cfg.get("schema") != "rafgittools.scientific-api-sources/v1":
        raise ValueError("config_schema_mismatch")
    forbidden = set(cfg["secret_policy"]["forbidden_github_pat_fallbacks"])
    if not FORBIDDEN_GITHUB_PATS.issubset(forbidden):
        raise ValueError("github_pat_fallback_policy_incomplete")
    allowed = set(cfg["secret_policy"]["allowed_external_secret_references"])
    if allowed & FORBIDDEN_GITHUB_PATS:
        raise ValueError("external_secret_allowlist_contains_github_pat")
    return cfg


def get_source(cfg: dict[str, Any], source_id: str) -> dict[str, Any]:
    source = cfg.get("sources", {}).get(source_id)
    if not isinstance(source, dict):
        raise ValueError("source_not_allowlisted")
    if source.get("state") not in {"CONFIGURED", "CONFIGURED_SECRET_REQUIRED"}:
        raise ValueError("source_not_executable")
    if source.get("method") != "GET":
        raise ValueError("only_get_supported")
    return source


def _scalar(value: Any) -> str:
    if isinstance(value, bool):
        return "true" if value else "false"
    if isinstance(value, (str, int, float)):
        return str(value)
    raise ValueError("query_values_must_be_scalar")


def validate_query(source: dict[str, Any], query: dict[str, Any]) -> dict[str, str]:
    if not isinstance(query, dict):
        raise ValueError("query_must_be_object")
    allowed = set(source.get("allowed_query_keys", []))
    unexpected = sorted(set(query) - allowed)
    if unexpected:
        raise ValueError("query_key_not_allowlisted:" + ",".join(unexpected))
    out = {str(k): _scalar(v) for k, v in query.items()}
    for key, value in source.get("forced_query", {}).items():
        forced = _scalar(value)
        if key in out and out[key].casefold() != forced.casefold():
            raise ValueError(f"forced_query_mismatch:{key}")
        out[key] = forced
    missing = [key for key in source.get("required_query_keys", []) if not out.get(key)]
    if missing:
        raise ValueError("required_query_missing:" + ",".join(missing))
    return out


def build_request(cfg: dict[str, Any], source_id: str, query: dict[str, Any]) -> tuple[str, dict[str, str], set[str]]:
    source = get_source(cfg, source_id)
    params = validate_query(source, query)
    base = source["base_url"].rstrip("/")
    endpoint = source["endpoint"]
    parsed = urllib.parse.urlparse(base)
    if parsed.scheme != "https" or not parsed.hostname:
        raise ValueError("https_base_url_required")

    headers = {"Accept": "application/json", "User-Agent": "RafGitTools-ScientificCustody/1.0"}
    secret_query_names: set[str] = set()
    auth = source.get("auth", {"mode": "NONE"})
    mode = auth.get("mode", "NONE")
    if mode != "NONE":
        secret_ref = str(auth.get("secret_reference", ""))
        if secret_ref in FORBIDDEN_GITHUB_PATS:
            raise ValueError("github_pat_forbidden_for_external_api")
        allowed_external = set(cfg["secret_policy"]["allowed_external_secret_references"])
        if secret_ref not in allowed_external:
            raise ValueError("external_secret_reference_not_allowlisted")
        secret_value = os.environ.get(secret_ref, "")
        if not secret_value:
            raise RuntimeError(f"required_secret_missing:{secret_ref}")
        name = str(auth.get("name", ""))
        if not name:
            raise ValueError("auth_name_missing")
        if mode == "HEADER":
            headers[name] = secret_value
        elif mode == "QUERY":
            params[name] = secret_value
            secret_query_names.add(name)
        else:
            raise ValueError("unsupported_auth_mode")

    query_string = urllib.parse.urlencode(sorted(params.items()))
    return base + endpoint + ("?" + query_string if query_string else ""), headers, secret_query_names


def redact_url(url: str, secret_query_names: set[str]) -> str:
    parsed = urllib.parse.urlsplit(url)
    pairs = urllib.parse.parse_qsl(parsed.query, keep_blank_values=True)
    redacted = [(k, "REDACTED") if k in secret_query_names else (k, v) for k, v in pairs]
    return urllib.parse.urlunsplit((parsed.scheme, parsed.netloc, parsed.path, urllib.parse.urlencode(redacted), parsed.fragment))


def fetch_json(cfg: dict[str, Any], source_id: str, query: dict[str, Any], timeout: int = 30) -> tuple[bytes, Any, str, int]:
    source = get_source(cfg, source_id)
    url, headers, secret_query_names = build_request(cfg, source_id, query)
    request = urllib.request.Request(url, headers=headers, method="GET")
    with urllib.request.urlopen(request, timeout=timeout) as response:
        raw = response.read()
        final_url = response.geturl()
        status = int(getattr(response, "status", 200))
    source_host = urllib.parse.urlparse(source["base_url"]).hostname
    final_host = urllib.parse.urlparse(final_url).hostname
    if final_host != source_host:
        raise RuntimeError("redirect_host_changed")
    try:
        decoded = json.loads(raw)
    except json.JSONDecodeError as exc:
        raise RuntimeError("official_api_returned_non_json") from exc
    return raw, decoded, redact_url(final_url, secret_query_names), status


def write_receipt(source_id: str, redacted_url: str, raw: bytes, projection: Any, status: int, output_dir: Path) -> dict[str, Any]:
    output_dir.mkdir(parents=True, exist_ok=True)
    projection_bytes = canonical_json_bytes(projection)
    (output_dir / "projection.json").write_bytes(projection_bytes)
    receipt = {
        "schema": SCHEMA,
        "observed_at": utc_now(),
        "source_id": source_id,
        "source_url_redacted": redacted_url,
        "http_status": status,
        "source_bytes": len(raw),
        "source_sha256": sha256_bytes(raw),
        "projection_bytes": len(projection_bytes),
        "projection_sha256": sha256_bytes(projection_bytes),
        "raw_source_persisted": False,
        "execution_state": "OBSERVED_SOURCE_SNAPSHOT",
        "scientific_claim_state": "OBSERVED_UNPROMOTED",
        "claim_allowed": False,
        "invariants": [
            "SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM",
            "GITHUB_PAT != EXTERNAL_API_CREDENTIAL",
            "HTTP_200 != SCIENTIFIC_VALIDATION",
            "TOKEN_VAZIO != PASS"
        ]
    }
    (output_dir / "receipt.json").write_bytes(canonical_json_bytes(receipt))
    return receipt


def snapshot(args: argparse.Namespace) -> int:
    cfg = load_config()
    query = json.loads(args.query_json)
    raw, projection, redacted_url, status = fetch_json(cfg, args.source, query, timeout=args.timeout)
    receipt = write_receipt(args.source, redacted_url, raw, projection, status, Path(args.output))
    print(json.dumps(receipt, ensure_ascii=False, sort_keys=True))
    return 0


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", required=True)
    parser.add_argument("--query-json", default="{}")
    parser.add_argument("--output", required=True)
    parser.add_argument("--timeout", type=int, default=30)
    args = parser.parse_args()
    return snapshot(args)


if __name__ == "__main__":
    raise SystemExit(main())
