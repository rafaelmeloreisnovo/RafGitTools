#!/usr/bin/env python3
"""Hosted TOOLING_ONLY: standard library, never linked into PURE_CORE."""
import json
import sys
from pathlib import Path

REQUIRED = ("schema", "kernel_profile", "runtime_external_dependencies", "binary_prefix_families", "friction_lanes", "matrix_cells", "constraints", "claim_allowed", "promotion_requires", "test_evidence", "rollback", "module_admission")
FORBIDDEN = ("libc", "heap", "syscalls", "jni", "java", "jit", "platform_functions", "external_symbols")
FAMILIES = ("ELF", "DEX", "CDEX", "OAT", "VDEX", "ART", "ZIP", "AR", "WASM", "PNG", "GZIP", "MZ", "MACHO")
LANES = ("JNI_JAVA", "ART_JIT", "ANDROID_SDK_NDK", "HEAP_GC", "GRADLE_R8_D8_JDK", "ELF_LINKER_SYSCALL", "FLAGS_BRANCH_TAIL_SHADOW_HEX")

def validate(d):
    if not isinstance(d, dict) or any(k not in d for k in REQUIRED):
        return "MISSING_CONTRACT_FIELDS"
    if d["schema"] != "rafaelia.freestanding-service-s12.v1" or d["kernel_profile"] != "PURE_CORE":
        return "WRONG_SCOPE"
    if d["claim_allowed"] is not False or d["runtime_external_dependencies"] != []:
        return "PROMOTION_OR_DEPENDENCY_LEAK"
    if not isinstance(d["constraints"], dict) or any(d["constraints"].get(k) is not False for k in FORBIDDEN):
        return "CORE_EXTERNAL_CAPABILITY_LEAK"
    if d["binary_prefix_families"] != list(FAMILIES) or d["friction_lanes"] != list(LANES):
        return "MATRIX_TOPOLOGY_DRIFT"
    if len(d["matrix_cells"]) != 8 or len(set(d["matrix_cells"])) != 8:
        return "ANONYMOUS_OR_INVALID_MATRIX"
    if len(d["promotion_requires"]) < 5 or "human_review" not in d["promotion_requires"]:
        return "HUMAN_PROMOTION_GATE_MISSING"
    if d["test_evidence"].get("physical_android") != "TOKEN_VAZIO_NOT_RUN":
        return "UNSUPPORTED_ANDROID_CLAIM"
    adm=d.get("module_admission")
    if (not isinstance(adm,dict) or adm.get("implemented_pure_module_ids") != [1,2]
        or adm.get("reserved_platform_module_ids") != [3,4]
        or adm.get("external_capability_mask") != 511
        or adm.get("runtime_dispatch_implemented") is not False
        or adm.get("verified_execution") is not False):
        return "MODULE_ADMISSION_DRIFT"
    if d["rollback"] != "NO_MERGE_APPEND_ONLY":
        return "ROLLBACK_MISSING"
    return "PASS_SCOPED_CONTRACT_ONLY"

if __name__ == "__main__":
    try:
        obj=json.loads(Path(sys.argv[1]).read_text(encoding="utf8"))
        result=validate(obj)
    except (IndexError, OSError, ValueError, TypeError):
        result="UNREADABLE_OR_MALFORMED"
    print(result)
    sys.exit(0 if result == "PASS_SCOPED_CONTRACT_ONLY" else 2)
