#!/usr/bin/env python3
"""HOSTED_ONLY: enforce truthful, exact-head S15 cross-repository evidence."""
import json
import sys
from pathlib import Path

EXPECTED_CONSUMER_HEAD = "0d7fd37eb100f3d0310b1621f0a6603eaa2f935c"
EXPECTED_PRODUCER_BLOBS = ("e46b1e24191f901edf91d6ce4c5003fa5d74438f",
                           "ad97c24a2c9c3ea291e26ce3d74322f89d84f9d5")
def validate(v):
    if v.get("schema") != "rafaelia.middleware.s15.android_hosted_handoff.v1":
        return "SCHEMA_MISMATCH"
    if v.get("claim_allowed") is not False:
        return "FALSE_CLAIM"
    p=v.get("producer",{});c=v.get("consumer",{});e=v.get("evidence",{})
    if (p.get("c_source_blob"),p.get("h_source_blob")) != EXPECTED_PRODUCER_BLOBS:
        return "UNBOUND_SOURCE"
    if p.get("abi_words") != 16 or p.get("reported_executed_value") != 0:
        return "FALSE_EXECUTION_OR_ABI"
    if c.get("exact_head") != EXPECTED_CONSUMER_HEAD:
        return "HEAD_DRIFT"
    if c.get("default_enabled") is not False or c.get("abi_profiles") != ["armeabi-v7a","arm64-v8a"]:
        return "UNSAFE_DEFAULT_OR_ABI"
    for name in ("host_contract","ndk_shared_link","android_mk_module"):
        if e.get(name) != "PASS_SCOPED": return "UNVERIFIED_BUILD_GATE"
    if e.get("run_id") != 40140221144:
        return "UNPINNED_WORKFLOW_RUN"
    for name in ("android_apk","device_install","jni_on_device","watchdog_live","gpu_numa_execution","authorization_to_release"):
        if not str(e.get(name,"")).startswith("TOKEN_VAZIO_"):
            return "UNSUPPORTED_PHYSICAL_CLAIM"
    if not v.get("rollback",{}).get("code") == "RAFAELIA_S15_MIDDLEWARE=0":
        return "ROLLBACK_MISSING"
    if v.get("claim_gate") != "BLOCKED_PHYSICAL_RUNTIME_AND_RELEASE":
        return "PROMOTION_NOT_AUTHORIZED"
    return "PASS_SCOPED_DOCUMENTARY_HOFF"

if __name__=="__main__":
    try:
        data=json.loads(Path(sys.argv[1]).read_text(encoding="utf8"))
        state=validate(data)
    except (IndexError, OSError, ValueError, UnicodeError, TypeError):
        state="MISSING_OR_MALFORMED"
    print(state)
    raise SystemExit(0 if state=="PASS_SCOPED_DOCUMENTARY_HOFF" else 2)
