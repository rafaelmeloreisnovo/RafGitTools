#!/usr/bin/env python3
"""HOSTED TOOLING ONLY, no connection with freestanding C runtime."""
import json
import sys
from pathlib import Path

EXPECTED_SHA="68195995718b9f0653ca4ce4575d375ce60ac756"
EXPECTED_C="e46b1e24191f901edf91d6ce4c5003fa5d74438f"
EXPECTED_H="ad97c24a2c9c3ea291e26ce3d74322f89d84f9d5"

def validate(d):
    if not isinstance(d,dict) or d.get("schema")!="rafaelia.middleware.s14.producer_consumer_handoff.v1":
        return "BLOCK_SCHEMA"
    prod=d.get("producer",{})
    if prod.get("head")!=EXPECTED_SHA or prod.get("blob_sha1")!=EXPECTED_C or prod.get("header_blob_sha1")!=EXPECTED_H:
        return "BLOCK_PRODUCER_SHA"
    p=d.get("pure_core",{})
    if (p.get("one_entry")!="raf_mw_route" or p.get("words")!=16 or
        p.get("byte_size_per_word")!=4 or p.get("output_executed_always_zero") is not True or
        p.get("external_runtime_symbols")!=0):
        return "BLOCK_ABI"
    for key in ("libc","heap","jni","syscalls","gpu_driver","numa_api"):
        if p.get(key) is not False: return "BLOCK_EXTERNAL_RUNTIME"
    s=d.get("safety",{})
    if (s.get("watchdogs")!="TWO_PASSIVE_COUNTER_COMPARATORS"
        or s.get("fallback")!="PLANNED_ROUTE_ONLY"
        or s.get("failover")!="PLANNED_ROUTE_ONLY"
        or s.get("rollback")!="RECOMMENDATION_ONLY"
        or s.get("human_review")!="TOKEN_VAZIO_NOT_COMPLETED"):
        return "BLOCK_FALSE_EXECUTION"
    e=d.get("evidence",{})
    if (e.get("physical_arm_device")!="NOT_RUN" or e.get("actual_gpu_numa")!="NOT_RUN"
        or e.get("actual_neon_simd")!="NOT_RUN" or d.get("allow_claim") is not False):
        return "BLOCK_FALSE_PROMOTION"
    if d.get("android",{}).get("adapter")!="NOT_IMPLEMENTED":
        return "BLOCK_UNVERIFIED_ADAPTER"
    if d.get("rollback")!="BRANCH_ONLY_NO_MERGE_NO_DELETE":
        return "BLOCK_NO_REVERSIBILITY"
    return "PASS_SCOPED_HOLD"

if __name__=="__main__":
    try:
        out=validate(json.loads(Path(sys.argv[1]).read_text("utf8")))
    except (ValueError,OSError,TypeError,IndexError):
        out="BLOCK_INVALID_INPUT"
    print(out)
    raise SystemExit(0 if out=="PASS_SCOPED_HOLD" else 2)
