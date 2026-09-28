#!/usr/bin/env python3
from __future__ import annotations
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
P=ROOT/"configs/knowledge-campus/gymnasiums/energy-cascade-recovery.v1.json"

def validate(v):
    e=[]
    rules=set(v.get("relation_rules",[]))
    required=[
      "WASTE_ENERGY != FULLY_RECOVERABLE_ENERGY",
      "ELECTROLYSIS != FREE_HYDROGEN",
      "WATER_RECOVERY != FREE_ENERGY",
      "SUPERCAP_HIGH_POWER != HIGH_ENERGY_DENSITY",
      "ROUND_TRIP_EFFICIENCY < 100_PERCENT_REAL_SYSTEM",
      "N2O_NODE != OXIDIZER_RECIPE",
      "CANDIDATE != MATERIALIZED"
    ]
    for r in required:
        if r not in rules:e.append("missing_rule:"+r)
    sys=v.get("systems",{})
    for s in ["SYS-EXHAUST-ENERGY-RECOVERY","SYS-SUPERCAP-TRANSIENT-BUFFER","SYS-HYDROGEN-WATER-MATERIAL-LOOP","SYS-ENERGY-ACCOUNTING"]:
        if s not in sys:e.append("missing_system:"+s)
    c=v.get("characteristics",[])
    closed=[x for x in c if x.get("name")=="closed_energy_loop_claim"]
    if not closed or closed[0].get("value") is not False:e.append("closed_energy_loop_claim_must_be_false")
    raw=json.dumps(v).lower()
    for forbidden in ["h2_o2_ratio","n2o_ratio","hydrogen_storage_pressure","ignition_timing_deg","boost_target_psi","electrolyzer_cell_voltage_recipe"]:
        if forbidden in raw:e.append("hazardous_recipe:"+forbidden)
    return e

def main():
    v=json.loads(P.read_text(encoding="utf-8"))
    e=validate(v)
    if e:
        print("FAIL energy-cascade-v1 "+",".join(e)); return 1
    print("PASS energy-cascade-v1"); return 0
if __name__=="__main__":
    raise SystemExit(main())
