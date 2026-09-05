#!/usr/bin/env python3
"""RAFAELIA FIAT_LUX Evidence Database Federation V1.

Extends RAFAELIA_NAVIGATOR.sqlite3 with append-only logical database planes:
root, START_HERE, ∅/TOKEN_VAZIO, evidence, gates, receipts, invariants,
L/O/T memory projections, routes, hot_pathway, MOD9 one-hot bindings,
steps TODO/DONE, roadmap and ATLAS urgency.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import sqlite3
import tempfile
from datetime import datetime, timezone
from pathlib import Path

HERE = Path(__file__).resolve().parent
SQL_PATH = HERE / "fiat_lux_evidence_db_v1.sql"
SCHEMA_ID = "RAFAELIA_FIAT_LUX_EVIDENCE_DATABASE_V1"


def now() -> str:
    return datetime.now(timezone.utc).isoformat().replace("+00:00", "Z")


def canon(value) -> str:
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":"))


def event_id(prefix: str, value) -> str:
    digest = hashlib.sha256(canon(value).encode("utf-8")).hexdigest()[:24]
    return f"{prefix}-{digest}"


def open_db(path: Path) -> sqlite3.Connection:
    path.parent.mkdir(parents=True, exist_ok=True)
    con = sqlite3.connect(path)
    con.execute("PRAGMA foreign_keys=ON")
    con.execute("PRAGMA journal_mode=WAL")
    return con


def install(con: sqlite3.Connection) -> None:
    con.executescript(SQL_PATH.read_text(encoding="utf-8"))
    con.execute(
        "INSERT OR IGNORE INTO schema_meta(schema_id,version,created_at,source_authority,state,claim_allowed) VALUES(?,?,?,?,?,0)",
        (SCHEMA_ID, 1, now(), "RafGitTools+Mapa+Drive/ATLAS", "IMPLEMENTED"),
    )
    con.commit()


ROOTS = [
    ("DRIVE_ATLAS", "GOOGLE_DRIVE", "drive:1yqrafV9KvQ2C-wz8nDCrYeVEyQo_TdQZ", "EDITORIAL_CATALOG_AND_MEMORY", "Drive/RAFAELIA_ATLAS_ROTAS_CATALOGO_OMEGA", "Mapa:data/catalog/RAFAELIA_ATLAS_DIRECTORY_MANIFEST_V1.json"),
    ("DRIVE_DATA_NAVIGATOR", "GOOGLE_DRIVE", "drive:1cwL1d-4KpVHkbYDtZwCyDzExOYYOFxzV", "SOURCE_MANIFEST_REPORT_CONTROL_PLANE", "Drive/RAFAELIA_DATA_NAVIGATOR", "Drive:RAFAELIA DATA NAVIGATOR architecture"),
    ("GITHUB_MAPA", "GITHUB", "github:rafaelmeloreisnovo/Mapa", "ONTOLOGY_ROUTING_GAPS_RECEIPTS", "repository-domain", "GitHub:rafaelmeloreisnovo/Mapa"),
    ("GITHUB_RAFGITTOOLS", "GITHUB", "github:rafaelmeloreisnovo/RafGitTools", "LOCAL_NAVIGATOR_EXECUTION", "repository-domain", "GitHub:rafaelmeloreisnovo/RafGitTools"),
]

ROUTES = [
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


def seed_canonical(con: sqlite3.Connection) -> None:
    ts = now()
    for root_id, provider, locator, role, authority, source_id in ROOTS:
        payload = {"root_id": root_id, "provider": provider, "locator": locator}
        con.execute(
            'INSERT OR IGNORE INTO "databaseroot"(event_id,root_id,provider,locator,role,authority,epistemic_state,source_id,created_at,claim_allowed,payload_json) VALUES(?,?,?,?,?,?,?,?,?,0,?)',
            (event_id("ROOT", payload), root_id, provider, locator, role, authority, "OBSERVED_SOURCE_TEXT", source_id, ts, canon(payload)),
        )
    for key, ordinal, kind, target, purpose in ROUTES:
        payload = {"key": key, "ordinal": ordinal, "target": target}
        con.execute(
            'INSERT OR IGNORE INTO "databaseStarthere"(event_id,route_id,route_key,route_order,target_kind,target_ref,purpose,source_id,created_at,epistemic_state,claim_allowed) VALUES(?,?,?,?,?,?,?,?,?,?,0)',
            (event_id("START", payload), "ATLAS_CANONICAL_V1", key, ordinal, kind, target, purpose, "Drive:RAFAELIA_START_HERE_OMEGA_V1", ts, "OBSERVED_SOURCE_TEXT"),
        )
    for invariant_id, scope, statement, severity, enforcement in INVARIANTS:
        payload = {"id": invariant_id, "statement": statement}
        con.execute(
            "INSERT OR IGNORE INTO database_invariants(invariant_event_id,invariant_id,scope,statement,severity,enforcement,falsifier,source_ref,created_at,epistemic_state,claim_allowed) VALUES(?,?,?,?,?,?,?,?,?,?,0)",
            (event_id("INV", payload), invariant_id, scope, statement, severity, enforcement, f"contradictory evidence to {invariant_id}", "Mapa:data/control-plane/TOKEN_VAZIO_PRIORITY_QUEUE.v3.json", ts, "FORMALIZED"),
        )
    con.commit()


def import_urgency(con: sqlite3.Connection, queue_path: Path, source_ref: str | None = None) -> int:
    data = json.loads(queue_path.read_text(encoding="utf-8-sig"))
    items = data.get("active_items")
    if not isinstance(items, list):
        raise ValueError("active_items missing")
    src = source_ref or str(queue_path)
    ts = now()
    count = 0
    for ordinal, raw in enumerate(items):
        if not isinstance(raw, dict) or not raw.get("id"):
            continue
        item = {
            "id": str(raw["id"]),
            "priority": str(raw.get("priority") or "UNSET"),
            "state": str(raw.get("state") or "TOKEN_VAZIO"),
            "scope": raw.get("scope") or [],
            "authority": raw.get("authority"),
            "evidence": raw.get("evidence") or [],
            "uncertainty": raw.get("uncertainty"),
            "falsifier": raw.get("falsifier"),
            "closure_gate": raw.get("closure_gate") or [],
            "next_action": raw.get("next_action"),
        }
        urg_event = event_id("URG", {**item, "source": src})
        con.execute(
            "INSERT OR IGNORE INTO atlas_urgency_queue(urgency_event_id,urgency_id,priority,state,scope_json,authority,evidence_json,uncertainty,falsifier,closure_gate_json,next_action,source_ref,created_at,claim_allowed) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,0)",
            (urg_event, item["id"], item["priority"], item["state"], canon(item["scope"]), item["authority"], canon(item["evidence"]), item["uncertainty"], item["falsifier"], canon(item["closure_gate"]), item["next_action"], src, ts),
        )
        con.execute(
            'INSERT OR IGNORE INTO "database_∅"(event_id,gap_id,subject_id,priority,state,authority,evidence_required,uncertainty,falsifier,next_action,source_ref,created_at,claim_allowed) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,0)',
            (event_id("GAP", {**item, "source": src}), item["id"], item["id"], item["priority"], item["state"], item["authority"], canon(item["closure_gate"]), item["uncertainty"], item["falsifier"], item["next_action"], src, ts),
        )
        step_state = "BLOCKED" if item["priority"] == "P0" else "TODO"
        con.execute(
            'INSERT OR IGNORE INTO "databaseSTEPStoDo&done"(step_event_id,step_id,roadmap_id,subject_id,state,urgency,action,gap_id,source_ref,created_at,claim_allowed,payload_json) VALUES(?,?,?,?,?,?,?,?,?,?,0,?)',
            (event_id("STEP", {**item, "source": src}), f"STEP:{item['id']}", "ROADMAP:ATLAS_URGENCY", item["id"], step_state, item["priority"], item["next_action"] or "resolve evidence-bound gap", item["id"], src, ts, canon(item)),
        )
        con.execute(
            "INSERT OR IGNORE INTO roadmapDatabase(roadmap_event_id,roadmap_id,item_id,ordinal,priority,state,title,objective,dependency_ids_json,gate_ids_json,evidence_ids_json,gap_ids_json,next_action,source_ref,created_at,claim_allowed) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,0)",
            (event_id("ROAD", {**item, "source": src}), "ROADMAP:ATLAS_URGENCY", item["id"], ordinal, item["priority"], item["state"], item["id"], item["uncertainty"] or item["id"], "[]", canon(item["closure_gate"]), canon(item["evidence"]), canon([item["id"]]), item["next_action"], src, ts),
        )
        count += 1
    con.commit()
    return count


def add_binding(con: sqlite3.Connection, binding_id: str, slot: int, role: str, runtime_target: str | None, state: str, source_ref: str, evidence_id: str | None = None, gate_id: str | None = None) -> str:
    if not 0 <= slot <= 8:
        raise ValueError("slot must be 0..8")
    bank = slot // 3
    payload = {"binding_id": binding_id, "slot": slot, "bank": bank, "role": role, "runtime_target": runtime_target, "state": state, "source_ref": source_ref, "evidence_id": evidence_id, "gate_id": gate_id}
    eid = event_id("BIND", payload)
    con.execute(
        "INSERT OR IGNORE INTO database_one_hot_binding(binding_event_id,binding_id,slot,bank,semantic_role,runtime_target,bind_state,source_ref,evidence_id,gate_id,created_at,claim_allowed) VALUES(?,?,?,?,?,?,?,?,?,?,?,0)",
        (eid, binding_id, slot, bank, role, runtime_target, state, source_ref, evidence_id, gate_id, now()),
    )
    con.commit()
    return eid


def walk(slot: int, direction: str) -> int:
    if not 0 <= slot <= 8:
        raise ValueError("slot must be 0..8")
    return {"forward": (slot + 1) % 9, "reverse": (slot + 8) % 9, "bank": (slot + 3) % 9}[direction]


def selftest() -> None:
    with tempfile.TemporaryDirectory() as td:
        root = Path(td)
        db = root / "RAFAELIA_NAVIGATOR.sqlite3"
        con = open_db(db)
        install(con)
        seed_canonical(con)
        assert con.execute('SELECT count(*) FROM "databaseroot"').fetchone()[0] == 4
        assert con.execute('SELECT count(*) FROM "databaseStarthere"').fetchone()[0] == 9
        assert con.execute("SELECT count(*) FROM database_invariants").fetchone()[0] == 8
        fixture = root / "urgency.json"
        fixture.write_text(json.dumps({"active_items":[{"priority":"P0","id":"P0-TEST","state":"TOKEN_VAZIO_TEST","scope":["selftest"],"authority":"fixture","evidence":["fixture:e1"],"uncertainty":"fixture uncertainty","falsifier":"fixture falsifier","closure_gate":["fixture gate"],"next_action":"fixture action"}]}), encoding="utf-8")
        assert import_urgency(con, fixture, "fixture") == 1
        assert con.execute('SELECT count(*) FROM "database_∅"').fetchone()[0] == 1
        assert con.execute('SELECT count(*) FROM "databaseSTEPStoDo&done"').fetchone()[0] == 1
        assert con.execute("SELECT count(*) FROM roadmapDatabase").fetchone()[0] == 1
        add_binding(con, "MOD9-SELFTEST", 8, "CONTROL_SYNC", None, "TOKEN_VAZIO_RUNTIME_BINDING", "fixture")
        assert con.execute("SELECT next_slot,prev_slot,bank_next_slot FROM v_one_hot_walk WHERE binding_id='MOD9-SELFTEST'").fetchone() == (0, 7, 2)
        assert walk(8, "forward") == 0 and walk(0, "reverse") == 8 and walk(7, "bank") == 1
        try:
            con.execute('UPDATE "database_∅" SET state="BAD"')
            raise AssertionError("append-only update unexpectedly allowed")
        except sqlite3.IntegrityError as exc:
            assert "APPEND_ONLY" in str(exc)
        con.close()
    print("FIAT_LUX_EVIDENCE_DB_V1_SELFTEST_PASS")


def main() -> int:
    parser = argparse.ArgumentParser()
    sub = parser.add_subparsers(dest="cmd", required=True)
    p = sub.add_parser("init"); p.add_argument("db", type=Path)
    p = sub.add_parser("seed-canonical"); p.add_argument("db", type=Path)
    p = sub.add_parser("import-urgency"); p.add_argument("db", type=Path); p.add_argument("queue_json", type=Path); p.add_argument("--source-ref")
    p = sub.add_parser("walk"); p.add_argument("slot", type=int); p.add_argument("direction", choices=["forward","reverse","bank"])
    p = sub.add_parser("bind"); p.add_argument("db", type=Path); p.add_argument("binding_id"); p.add_argument("slot", type=int); p.add_argument("role"); p.add_argument("--runtime-target"); p.add_argument("--state", default="TOKEN_VAZIO_RUNTIME_BINDING"); p.add_argument("--source-ref", required=True); p.add_argument("--evidence-id"); p.add_argument("--gate-id")
    sub.add_parser("selftest")
    args = parser.parse_args()
    if args.cmd == "selftest": selftest(); return 0
    if args.cmd == "walk": print(walk(args.slot, args.direction)); return 0
    con = open_db(args.db); install(con)
    if args.cmd == "init": print(f"FIAT_LUX_DB_INITIALIZED {args.db}")
    elif args.cmd == "seed-canonical": seed_canonical(con); print(f"FIAT_LUX_CANONICAL_SEEDED {args.db}")
    elif args.cmd == "import-urgency": print(f"FIAT_LUX_URGENCY_IMPORTED items={import_urgency(con,args.queue_json,args.source_ref)}")
    elif args.cmd == "bind": print(f"FIAT_LUX_BINDING_APPENDED event_id={add_binding(con,args.binding_id,args.slot,args.role,args.runtime_target,args.state,args.source_ref,args.evidence_id,args.gate_id)}")
    con.close(); return 0


if __name__ == "__main__":
    raise SystemExit(main())
