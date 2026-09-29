#!/usr/bin/env python3
"""Read-only collector for the full-session MC-W01..MC-W18 gap/F_next route set."""
from __future__ import annotations

import argparse
import json
import subprocess
import sys
from pathlib import Path

CONFIG_PATH = Path("configs/session-full-gap-fnext.v1.json")


class Blocked(Exception):
    pass


def _git(checkout: Path, *args: str) -> str:
    try:
        return subprocess.check_output(
            ["git", "-C", str(checkout), *args], stderr=subprocess.DEVNULL, text=True
        ).strip()
    except (OSError, subprocess.CalledProcessError) as exc:
        raise Blocked(f"git verification failed: {exc}") from exc


def _load_config(path: Path = CONFIG_PATH) -> dict:
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, ValueError) as exc:
        raise Blocked(f"cannot load collector config: {exc}") from exc
    if data.get("claim_allowed") is not False:
        raise Blocked("collector config must keep claim_allowed=false")
    return data


def _verify_source(checkout: Path, cfg: dict) -> None:
    expected = cfg["authority"]["commit"]
    head = _git(checkout, "rev-parse", "HEAD")
    if head != expected:
        raise Blocked(f"Mapa HEAD mismatch: expected {expected}, got {head}")
    for name, spec in cfg["authority"]["files"].items():
        blob = _git(checkout, "rev-parse", f"HEAD:{spec['path']}")
        if blob != spec["blob_sha1"]:
            raise Blocked(
                f"{name} blob mismatch: expected {spec['blob_sha1']}, got {blob}"
            )


def _read_routes(checkout: Path, cfg: dict) -> list[dict]:
    path = checkout / cfg["authority"]["files"]["routes"]["path"]
    rows = []
    for line_no, raw in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        if not raw.strip():
            continue
        try:
            row = json.loads(raw)
        except ValueError as exc:
            raise Blocked(f"invalid route JSON at line {line_no}: {exc}") from exc
        rows.append(row)
    expected_count = int(cfg["semantics"]["exact_route_count"])
    if len(rows) != expected_count:
        raise Blocked(f"route count mismatch: expected {expected_count}, got {len(rows)}")
    required = set(cfg["semantics"]["required_route_fields"])
    route_ids = set()
    workstream_ids = set()
    for row in rows:
        missing = sorted(k for k in required if k not in row or row[k] in (None, ""))
        if missing:
            raise Blocked(f"{row.get('route_id','TOKEN_VAZIO')}: missing {','.join(missing)}")
        if row["claim_allowed"] is not False:
            raise Blocked(f"{row['route_id']}: claim_allowed must be false")
        if row["route_id"] in route_ids:
            raise Blocked(f"duplicate route_id: {row['route_id']}")
        if row["workstream_id"] in workstream_ids:
            raise Blocked(f"duplicate workstream_id: {row['workstream_id']}")
        route_ids.add(row["route_id"])
        workstream_ids.add(row["workstream_id"])
    expected_ws = {f"MC-W{i:02d}" for i in range(1, 19)}
    if workstream_ids != expected_ws:
        raise Blocked("workstream coverage mismatch")
    return rows


def collect(checkout: str | Path, workstream: str | None = None, wave: str | None = None,
            config_path: str | Path = CONFIG_PATH) -> dict:
    checkout = Path(checkout)
    cfg = _load_config(Path(config_path))
    _verify_source(checkout, cfg)
    routes = _read_routes(checkout, cfg)

    selected = routes
    selection = {"kind": "all", "id": "MC-W01..MC-W18"}
    if workstream:
        selected = [r for r in routes if r["workstream_id"] == workstream]
        if not selected:
            raise Blocked(f"unknown workstream: {workstream}")
        selection = {"kind": "workstream", "id": workstream}
    elif wave is not None:
        ids = cfg["waves"].get(str(wave))
        if not ids:
            raise Blocked(f"unknown or empty wave: {wave}")
        selected = [r for r in routes if r["workstream_id"] in ids]
        if {r["workstream_id"] for r in selected} != set(ids):
            raise Blocked(f"wave {wave} route coverage mismatch")
        selection = {"kind": "wave", "id": str(wave)}

    return {
        "state": "ROUTE_COLLECTION_RESOLVED",
        "source": {
            "repo": cfg["authority"]["repo"],
            "commit": cfg["authority"]["commit"],
        },
        "selection": selection,
        "count": len(selected),
        "routes": selected,
        "assignment_is_execution": False,
        "evidence_state": "STRUCTURAL_COLLECTION_ONLY",
        "claim_allowed": False,
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--mapa-checkout", required=True)
    parser.add_argument("--config", default=str(CONFIG_PATH))
    group = parser.add_mutually_exclusive_group()
    group.add_argument("--workstream")
    group.add_argument("--wave")
    args = parser.parse_args()
    try:
        result = collect(
            args.mapa_checkout,
            workstream=args.workstream,
            wave=args.wave,
            config_path=args.config,
        )
    except (Blocked, OSError, ValueError, KeyError) as exc:
        print(json.dumps({
            "state": "ROUTE_STATE_BLOCKED",
            "reason": str(exc),
            "claim_allowed": False
        }, ensure_ascii=False, indent=2))
        return 2
    print(json.dumps(result, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    sys.exit(main())
