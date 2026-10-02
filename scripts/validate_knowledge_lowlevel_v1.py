#!/usr/bin/env python3
from __future__ import annotations
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
PROFILE=ROOT/"configs/knowledge-campus/lowlevel-profile.v1.json"
MECH=ROOT/"configs/knowledge-campus/gymnasiums/mechanical-powertrain.v1.json"

def validate_profile(v):
    e=[]
    l1=v.get("profiles",{}).get("L1_KOTLIN_JAVA_RESTRICTED",{})
    if l1.get("freestanding_claim") is not False:e.append("kotlin_java_freestanding_claim_must_be_false")
    if l1.get("managed_runtime_required") is not True:e.append("managed_runtime_required")
    if l1.get("zero_heap_claim") is not False:e.append("zero_heap_claim_must_be_false")
    if l1.get("zero_gc_claim") is not False:e.append("zero_gc_claim_must_be_false")
    l0=v.get("profiles",{}).get("L0_FREESTANDING_REAL",{})
    if l0.get("authority")!="native/silicon_light_v1":e.append("l0_authority")
    return e

def validate_mech(v):
    e=[]
    fuel=v.get("systems",{}).get("SYS-FUEL-AIR-OXIDIZER",{})
    if "safety_boundary" not in fuel:e.append("missing_hazard_boundary")
    raw=json.dumps(v).lower()
    forbidden=["stoichiometric_ratio","h2_o2_ratio","n2o_ratio","ignition_timing_recipe","injection_pressure_recipe"]
    for x in forbidden:
        if x in raw:e.append("operational_hazard_recipe:"+x)
    if "ANALOGY != FACT" not in v.get("relation_rules",[]):e.append("analogy_boundary")
    return e

def main():
    p=json.loads(PROFILE.read_text(encoding="utf-8"))
    m=json.loads(MECH.read_text(encoding="utf-8"))
    errors=validate_profile(p)+validate_mech(m)
    if errors:
        print("FAIL "+",".join(errors)); return 1
    print("PASS lowlevel+mechanical-gymnasium-v1"); return 0
if __name__=="__main__":
    raise SystemExit(main())
