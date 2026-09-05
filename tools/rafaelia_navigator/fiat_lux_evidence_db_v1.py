#!/usr/bin/env python3
"""FIAT_LUX Evidence Database Federation V1.

Adds append-only evidence/gate/receipt/roadmap planes to an existing
RAFAELIA_NAVIGATOR.sqlite3 or to a new SQLite database.

No network access, no model training, no mutation of source corpora.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import sqlite3
import tempfile
from datetime import datetime, timezone
from pathlib import Path

SCHEMA_ID = "RAFAELIA_FIAT_LUX_EVIDENCE_DATABASE_V1"
HERE = Path(__file__).resolve().parent
SQL_PATH = HERE / "fiat_lux_evidence_db_v1.sql"


def now() -> str:
    return datetime.now(timezone.utc).isoformat().replace("+00:00", "Z")


def canon(obj) -> str:
    return json.dumps(obj, ensure_ascii=False, sort_keys=True, separators=(",", ":"))


def hid(prefix: str, obj) -> str:
    return f"{prefix}-{hashlib.sha256(canon(obj).encode('utf-8')).hexdigest()[:24]}"


def connect(path: Path) -> sqlite3.Connection:
    path.parent.mkdir(parents=True, exist_ok=True)
    c = sqlite3.connect(path)
    c.execute("PRAGMA foreign_keys=ON")
    c.execute("PRAGMA journal_mode=WAL")
    return c


def install_schema(c: sqlite3.Connection) -> None:
    c.executescript(SQL_PATH.read_text(encoding="utf-8"))
    c.execute(
        "INSERT OR IGNORE INTO schema_meta VALUES(?,?,?,?,?,0)",
        (SCHEMA_ID, 1, now(), "RafGitTools+Mapa+Drive/ATLAS", "IMPLEMENTED"),
    )
    c.commit()


CANONICAL_ROOTS = [
    {
        "root_id": "DRIVE_ATLAS",
        "provider": "GOOGLE_DRIVE",
        "locator": "drive:1yqrafV9KvQ2C-wz8nDCrYeVEyQo_TdQZ",
        "role": "EDITORIAL_CATALOG_AND_MEMORY",
        "authority": "Drive/RAFAELIA_ATLAS_ROTAS_CATALOGO_OMEGA",
        "source_id": "Mapa:data/catalog/RAFAELIA_ATLAS_DIRECTORY_MANIFEST_V1.json",
    },
    {
        "root_id": "DRIVE_DATA_NAVIGATOR",
        "provider": "GOOGLE_DRIVE",
        "locator": "drive:1cwL1d-4KpVHkbYDtZwCyDzExOYYOFxzV",
        "role": "SOURCE_MANIFEST_REPORT_CONTROL_PLANE",
        "authority": "Drive/RAFAELIA_DATA_NAVIGATOR",
        "source_id": "Drive:RAFAELIA DATA NAVIGATOR architecture",
    },
    {
        "root_id": "GITHUB_MAPA",
        "provider": "GITHUB",
        "locator": "github:rafaelmeloreisnovo/Mapa",
        "role": "ONTOLOGY_ROUTING_GAPS_RECEIPTS",
        "authority": "repository-domain",
        "source_id": "GitHub:rafaelmeloreisnovo/Mapa",
    },
    {
        "root_id": "GITHUB_RAFGITTOOLS",
        "provider": "GITHUB",
        "locator": "github:rafaelmeloreisnovo/RafGitTools",
        "role": "LOCAL_NAVIGATOR_EXECUTION",
        "authority": "repository-domain",
        "source_id": "GitHub:rafaelmeloreisnovo/RafGitTools",
    },
]

CANONICAL_ROUTES = [
    ("ATLAS", 0, "ROOT", "drive:1UZyuoEaoun19_peraI7sqoR_ojj54jD1y_BRGEgvchE", "tema→fontes→memórias→relações→escalas→evidência→gaps→next"),
    ("NOVO", 1, "SOURCE", "Drive/NOVOexport", "literal/source first; preserve provenance"),
    ("L", 2, "MEMORY_AXIS", "drive:1XHkixpMruPqrv22EJ5X0TeWUn7fWEXgmjnGSlxtaGzk", "longitudinal evolution and deltas"),
    ("O", 3, "MEMORY_AXIS", "drive:1HXDWFSmHGNo7pouUge3AIGhTq4SL_EkfrurXdJzM6-E", "independent axes, falsifiers and measurements"),
    ("T", 4, "MEMORY_AXIS", "drive:13oy53b9OAm7Fomyt_FANl7pAORrPtBAfnkZny7Kx-f8", "cross-domain bridges with boundaries"),
    ("REL", 5, "ONTOLOGY", "drive:1zBRetiSqlpgZep4OU8Qqd8-tgktVMhzGxJP2IeeBkV8", "typed structural relations"),
    ("SCALE", 6, "SCALE", "drive:1ExbWhj2dNsF-_4P5tjvB0StN55ll5T_ZtjleNTr3jY4", "meta→token and physical scale navigation"),
    ("EVID", 7, "GATE", "drive:1ZOIgUdffE9xoW_erxaeozNOqbC2M8KOkiumEYuTaPM8", "evidence, receipts, failures and TOKEN_VAZIO"),
    ("DELTA", 8, "RECEIPT", "drive:1AxTvlDsU4V_rnOsMYeRf7aG9r1IMFg8R", "append-only custody/learning delta"),
]

INVARIANTS = [
    ("INV-001", "GLOBAL", "VISÃO != ARTEFATO != EXECUÇÃO != EVIDÊNCIA != CLAIM", "CRITICAL", "FAIL_CLOSED"),
    ("INV-002", "GLOBAL", "TOKEN_VAZIO != 0", "CRITICAL", "FAIL_CLOSED"),
    ("INV-003", "MEMORY", "resolved != deleted_history", "HIGH", "APPEND_ONLY"),
    ("INV-004", "ROUTING", "latest_evidence_wins_for_current_routing", "HIGH", "CURRENT_VIEW_ONLY"),
    ("INV-005", "EXECUTION", "provider_scope_reproduction != exact_canonical_binding", "HIGH", "FAIL_CLOSED"),
    ("INV-006", "RUNTIME", "host_runtime != physical_device_runtime", "HIGH", "FAIL_CLOSED"),
    ("INV-007", "PROVENANCE", "hash_integrity != authorship_or_scientific_truth", "HIGH", "FAIL_CLOSED"),
    ("INV-008", "SEMANTICS", "analogy != identity; symbol != evidence; cooccurrence != causality", "HIGH", "FAIL_CLOSED"),
]


def seed_canonical(c: sqlite3.Connection) -> None:
    ts = now()
    for item in CANONICAL_ROOTS:
        event = {"kind": "root", **item}
        c.execute(
            'INSERT OR IGNORE INTO "databaseroot" VALUES(?,?,?,?,?,?,?,?,?,?,0,?)',
            (
                hid("ROOT", event), item["root_id"], item["provider"], item["locator"],
                item["role"], item["authority"], None, "OBSERVED_SOURCE_TEXT",
                item["source_id"], ts, canon(item),
            ),
        )
    for key, order, kind, target, purpose in CANONICAL_ROUTES:
        event = {"kind": "route", "key": key, "order": order, "target": target}
        c.execute(
            'INSERT OR IGNORE INTO "databaseStarthere" VALUES(?,?,?,?,?,?,?,?,?,?,?,0)',
            (
                hid("START", event), "ATLAS_CANONICAL_V1", key, order, kind, target,
                purpose, None, "Drive:RAFAELIA_START_HERE_OMEGA_V1", ts,
                "OBSERVED_SOURCE_TEXT",
            ),
        )
    for inv_id, scope, statement, severity, enforcement in INVARIANTS:
        event = {"id": inv_id, "statement": statement}
        c.execute(
            "INSERT OR IGNORE INTO database_invariants VALUES(?,?,?,?,?,?,?,?,?,?,0)",
            (
                hid("INV", event), inv_id, scope, statement, severity, enforcement,
                f"contradictory evidence to {inv_id}",
                "Mapa:data/control-plane/TOKEN_VAZIO_PRIORITY_QUEUE.v3.json",
                None, ts, "FORMALIZED",
            ),
        )
    c.commit()


def import_urgency(c: sqlite3.Connection, path: Path, source_ref: str | None = None) -> int:
    data = json.loads(path.read_text(encoding="utf-8-sig"))
    items = data.get("active_items")
    if not isinstance(items, list):
        raise ValueError("active_items missing")
    src = source_ref or str(path)
    ts = now()
    n = 0
    for item in items:
        if not isinstance(item, dict) or not item.get("id"):
            continue
        normalized = {
            "id": str(item["id"]),
            "priority": str(item.get("priority") or "UNSET"),
            "state": str(item.get("state") or "TOKEN_VAZIO"),
            "scope": item.get("scope") or [],
            "authority": item.get("authority"),
            "evidence": item.get("evidence") or [],
            "uncertainty": item.get("uncertainty"),
            "falsifier": item.get("falsifier"),
            "closure_gate": item.get("closure_gate") or [],
            "next_action": item.get("next_action"),
            "source_ref": src,
        }
        eid = hid("URG", normalized)
        c.execute(
            "INSERT OR IGNORE INTO atlas_urgency_queue VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,0)",
            (
                eid, normalized["id"], normalized["priority"], normalized["state"],
                canon(normalized["scope"]), normalized["authority"], canon(normalized["evidence"]),
                normalized["uncertainty"], normalized["falsifier"], canon(normalized["closure_gate"]),
                normalized["next_action"], None, src, ts,
            ),
        )
        gap_event = hid("GAP", normalized)
        c.execute(
            'INSERT OR IGNORE INTO "database_∅" VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,0)',
            (
                gap_event, normalized["id"], normalized["id"], normalized["priority"],
                normalized["state"], normalized["authority"], canon(normalized["closure_gate"]),
                normalized["uncertainty"], normalized["falsifier"], normalized["next_action"],
                None, src, ts,
            ),
        )
        step = {
            "step_id": f"STEP:{normalized['id']}",
            "state": "BLOCKED" if normalized["priority"] == "P0" else "TODO",
            "urgency": normalized["priority"],
            "action": normalized["next_action"] or "resolve evidence-bound gap",
        }
        c.execute(
            'INSERT OR IGNORE INTO "databaseSTEPStoDo&done" VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,0,?)',
            (
                hid("STEP", {**normalized, **step}), step["step_id"], "ROADMAP:ATLAS_URGENCY",
                normalized["id"], step["state"], step["urgency"], step["action"],
                None, None, None, normalized["id"], None, src, ts, canon(normalized),
            ),
        )
        c.execute(
            "INSERT OR IGNORE INTO roadmapDatabase VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,0)",
            (
                hid("ROAD", normalized), "ROADMAP:ATLAS_URGENCY", normalized["id"], n,
                normalized["priority"], normalized["state"], normalized["id"],
                normalized["uncertainty"] or normalized["id"], "[]", canon(normalized["closure_gate"]),
                canon(normalized["evidence"]), canon([normalized["id"]]), normalized["next_action"],
                None, src, ts,
            ),
        )
        n += 1
    c.commit()
    return n


def add_binding(c: sqlite3.Connection, binding_id: str, slot: int, role: str,
                runtime_target: str | None, state: str, source_ref: str,
                evidence_id: str | None = None, gate_id: str | None = None) -> str:
    if not 0 <= slot <= 8:
        raise ValueError("slot must be 0..8")
    bank = slot // 3
    item = {
        "binding_id": binding_id, "slot": slot, "bank": bank, "role": role,
        "runtime_target": runtime_target, "state": state, "source_ref": source_ref,
        "evidence_id": evidence_id, "gate_id": gate_id,
    }
    event_id = hid("BIND", item)
    c.execute(
        "INSERT OR IGNORE INTO database_one_hot_binding VALUES(?,?,?,?,?,?,?,?,?,?,?,?,0)",
        (
            event_id, binding_id, slot, bank, role, runtime_target, state, source_ref,
            evidence_id, gate_id, None, now(),
        ),
    )
    c.commit()
    return event_id


def walk(slot: int, direction: str) -> int:
    if not 0 <= slot <= 8:
        raise ValueError("slot must be 0..8")
    if direction == "forward":
        return (slot + 1) % 9
    if direction == "reverse":
        return (slot + 8) % 9
    if direction == "bank":
        return (slot + 3) % 9
    raise ValueError("direction must be forward|reverse|bank")


def selftest() -> None:
    with tempfile.TemporaryDirectory() as td:
        root = Path(td)
        db = root / "RAFAELIA_NAVIGATOR.sqlite3"
        c = connect(db)
        install_schema(c)
        seed_canonical(c)
        assert c.execute('SELECT count(*) FROM "databaseroot"').fetchone()[0] == 4
        assert c.execute('SELECT count(*) FROM "databaseStarthere"').fetchone()[0] == 9
        assert c.execute("SELECT count(*) FROM database_invariants").fetchone()[0] == 8
        fixture = {
            "active_items": [
                {
                    "priority": "P0",
                    "id": "P0-TEST-GATE",
                    "state": "TOKEN_VAZIO_TEST",
                    "scope": ["selftest"],
                    "authority": "fixture",
                    "evidence": ["fixture:e1"],
                    "uncertainty": "test uncertainty",
                    "falsifier": "test falsifier",
                    "closure_gate": ["fixture gate"],
                    "next_action": "run fixture action",
                }
            ]
        }
        fp = root / "urgency.json"
        fp.write_text(json.dumps(fixture), encoding="utf-8")
        assert import_urgency(c, fp, "fixture") == 1
        assert c.execute('SELECT count(*) FROM "database_∅"').fetchone()[0] == 1
        assert c.execute('SELECT count(*) FROM "databaseSTEPStoDo&done"').fetchone()[0] == 1
        assert c.execute("SELECT count(*) FROM roadmapDatabase").fetchone()[0] == 1
        add_binding(c, "MOD9-SELFTEST", 8, "CONTROL_SYNC", None, "TOKEN_VAZIO_RUNTIME_BINDING", "fixture")
        row = c.execute("SELECT next_slot,prev_slot,bank_next_slot FROM v_one_hot_walk WHERE binding_id='MOD9-SELFTEST'").fetchone()
        assert row == (0, 7, 2)
        assert walk(8, "forward") == 0
        assert walk(0, "reverse") == 8
        assert walk(7, "bank") == 1
        try:
            c.execute('UPDATE "database_∅" SET state="BAD"')
            raise AssertionError("append-only update unexpectedly allowed")
        except sqlite3.IntegrityError as exc:
            assert "APPEND_ONLY" in str(exc)
        c.close()
    print("FIAT_LUX_EVIDENCE_DB_V1_SELFTEST_PASS")


def main() -> int:
    p = argparse.ArgumentParser()
    sub = p.add_subparsers(dest="cmd", required=True)
    pi = sub.add_parser("init")
    pi.add_argument("db", type=Path)
    ps = sub.add_parser("seed-canonical")
    ps.add_argument("db", type=Path)
    pu = sub.add_parser("import-urgency")
    pu.add_argument("db", type=Path)
    pu.add_argument("queue_json", type=Path)
    pu.add_argument("--source-ref")
    pw = sub.add_parser("walk")
    pw.add_argument("slot", type=int)
    pw.add_argument("direction", choices=["forward", "reverse", "bank"])
    pb = sub.add_parser("bind")
    pb.add_argument("db", type=Path)
    pb.add_argument("binding_id")
    pb.add_argument("slot", type=int)
    pb.add_argument("role")
    pb.add_argument("--runtime-target")
    pb.add_argument("--state", default="TOKEN_VAZIO_RUNTIME_BINDING")
    pb.add_argument("--source-ref", required=True)
    pb.add_argument("--evidence-id")
    pb.add_argument("--gate-id")
    sub.add_parser("selftest")
    a = p.parse_args()

    if a.cmd == "selftest":
        selftest()
        return 0
    if a.cmd == "walk":
        print(walk(a.slot, a.direction))
        return 0

    c = connect(a.db)
    install_schema(c)
    if a.cmd == "init":
        print(f"FIAT_LUX_DB_INITIALIZED {a.db}")
    elif a.cmd == "seed-canonical":
        seed_canonical(c)
        print(f"FIAT_LUX_CANONICAL_SEEDED {a.db}")
    elif a.cmd == "import-urgency":
        n = import_urgency(c, a.queue_json, a.source_ref)
        print(f"FIAT_LUX_URGENCY_IMPORTED items={n}")
    elif a.cmd == "bind":
        event_id = add_binding(c, a.binding_id, a.slot, a.role, a.runtime_target,
                               a.state, a.source_ref, a.evidence_id, a.gate_id)
        print(f"FIAT_LUX_BINDING_APPENDED event_id={event_id}")
    c.close()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
