#!/usr/bin/env python3
from __future__ import annotations
import json
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
P=ROOT/"configs/knowledge-campus/gymnasiums/aviation-radial-hybrid-propulsion.v1.json"

def validate(v):
    e=[]
    rules=set(v.get("relation_rules",[]))
    for rule in [
        "RADIAL_ENGINE != ROTARY_ENGINE",
        "HYDRAULIC_LOCK != HYDRAULIC_LIFTER",
        "ROLLING_ELEMENT_BEARING != UNIVERSAL_REPLACEMENT_FOR_PLAIN_BEARING",
        "COMBUSTION_TRIAD != REACTIVE_MIXTURE_RECIPE",
        "CANDIDATE != MATERIALIZED"
    ]:
        if rule not in rules:e.append("missing_rule:"+rule)
    systems=v.get("systems",{})
    for key in [
        "SYS-RADIAL-RECIPROCATING","SYS-ROTARY-HISTORICAL","SYS-AVIATION-REDUNDANCY",
        "SYS-VALVE-THERMAL","SYS-BEARING-ARCHITECTURE","SYS-HYBRID-TORQUE-MANAGEMENT",
        "SYS-DRIVEN-BOOST-CVT","SYS-START_TRANSIENT_BUFFER","SYS-TORQUE_CURVE-STAGING",
        "SYS-FUEL-THERMAL-PRECONDITIONING","SYS-COMBUSTION-TRIAD"
    ]:
        if key not in systems:e.append("missing_system:"+key)
    raw=json.dumps(v).lower()
    for forbidden in [
        "h2_o2_ratio","n2o_ratio","oxygen_enrichment_percent","ignition_timing_deg",
        "boost_target_psi","accumulator_pressure_psi","fuel_temperature_target"
    ]:
        if forbidden in raw:e.append("hazardous_setpoint:"+forbidden)
    if v.get("materialization_state")!="IMPLEMENTED_UNTESTED":e.append("state")
    return e

def main():
    v=json.loads(P.read_text(encoding="utf-8"))
    e=validate(v)
    if e:
        print("FAIL aviation-gymnasium-v1 "+",".join(e)); return 1
    print("PASS aviation-gymnasium-v1"); return 0

if __name__=="__main__":
    raise SystemExit(main())
