#!/usr/bin/env python3
"""Hosted source/ELF adapter. NOT part of the freestanding L0 ABI.

Only Python stdlib used. Clang, readelf and the host dynamic loader are
explicit build/test-time interfaces, NOT L0 runtime dependencies.
"""
import argparse
import ctypes
import json
import pathlib
import re
import shutil
import subprocess
import tempfile

BASE = {
    "gradle": "9.5.1", "agp": "8.13.2", "kotlin": "1.9.24",
    "ksp": "1.9.24-1.0.20", "compose": "1.5.14", "jdk": "17",
}
PATTERNS = {
    "gradle": ("gradle/wrapper/gradle-wrapper.properties",
               r"^distributionUrl=.*gradle-([0-9.]+)-bin\.zip\s*$"),
    "agp": ("build.gradle",
            r"^\s*classpath ['\"]com\.android\.tools\.build:gradle:([0-9.]+)['\"]"),
    "kotlin": ("build.gradle",
               r"^\s*classpath ['\"]org\.jetbrains\.kotlin:kotlin-gradle-plugin:([0-9.]+)['\"]"),
    "ksp": ("build.gradle",
            r"^\s*id ['\"]com\.google\.devtools\.ksp['\"] version ['\"]([0-9.]+-[0-9.]+)['\"]"),
    "compose": ("app/build.gradle",
                r"^\s*kotlinCompilerExtensionVersion ['\"]([0-9.]+)['\"]"),
    "jdk": ("app/build.gradle",
            r"^\s*sourceCompatibility JavaVersion\.VERSION_([0-9]+)"),
}

def source_values(root):
    out = {}
    for key, (path, rx) in PATTERNS.items():
        hits = re.findall(rx, (root / path).read_text(encoding="utf-8"), re.M)
        if len(hits) != 1:
            raise ValueError("TOKEN_VAZIO_%s_UNIQUE_SOURCE_MATCH" % key.upper())
        out[key] = hits[0]
    source = (root / "app/build.gradle").read_text(encoding="utf-8")
    if len(re.findall(r'^\s*abiFilters\s+"armeabi-v7a",\s*"arm64-v8a"\s*$',
                      source, re.M)) != 1:
        raise ValueError("TOKEN_VAZIO_EXACT_ABI_PAIR")
    workflow = (root / ".github/workflows/START.yml").read_text(encoding="utf-8")
    if (workflow.count("\n  coherence:\n") != 1 or
            workflow.count("sh scripts/validate_rafaelia_workflow.sh") != 1):
        raise ValueError("TOKEN_VAZIO_START_COHERENCE_BRIDGE")
    return out

def matrix(values):
    b = bytearray(64)  # Hosted input preparation; eight 8-byte source rows.
    for key, base in (("gradle", 0), ("agp", 8), ("kotlin", 16), ("compose", 32)):
        nums = list(map(int, values[key].split(".")))
        if len(nums) != 3 or any(x > 255 or x < 0 for x in nums):
            raise ValueError("TOKEN_VAZIO_INVALID_%s_TUPLE" % key.upper())
        b[base:base + 3] = bytes(nums)
    left, right = values["ksp"].split("-")
    nums = list(map(int, left.split(".") + right.split(".")))
    if len(nums) != 6 or any(x > 255 or x < 0 for x in nums):
        raise ValueError("TOKEN_VAZIO_INVALID_KSP_TUPLE")
    b[24:30] = bytes(nums)
    jdk = int(values["jdk"])
    if not 0 <= jdk <= 255:
        raise ValueError("TOKEN_VAZIO_INVALID_JDK")
    b[40] = jdk
    b[48] = 3  # Both ARMv7 and AArch64, source preflight above.
    return b

def cmd(args):
    p = subprocess.run(args, capture_output=True, text=True, check=False)
    if p.returncode:
        raise RuntimeError("BUILD_GATE_FAILED(%s): %s" % (p.returncode, p.stderr[-800:]))
    return p.stdout

def execute(values, native_src):
    cc = shutil.which("clang")
    readelf = shutil.which("readelf")
    if not cc or not readelf:
        raise RuntimeError("TOKEN_VAZIO_BUILD_TOOL_CLANG_OR_READELF")
    flags = ["-O2", "-ffreestanding", "-fno-builtin", "-fno-stack-protector",
             "-fno-unwind-tables", "-fno-asynchronous-unwind-tables", "-nostdlib"]
    result = {"source_versions": values, "l0_runtime_imports": "ZERO_IN_ELF_OBJECTS",
              "host_execution": "NOT_RUN", "arm_physical": "NOT_RUN"}
    with tempfile.TemporaryDirectory(prefix="rmr_l0_") as td:
        work = pathlib.Path(td)
        for key, target, arch in (
            ("armv7", "armv7a-none-eabi", "ARM"),
            ("aarch64", "aarch64-none-elf", "AArch64"),
            ("x86_64", "x86_64-unknown-linux-gnu", "X86-64"),
        ):
            path = work / (key + ".o")
            cmd([cc, "--target=" + target, *flags,
                 "-c", str(native_src), "-o", str(path)])
            hdr = cmd([readelf, "-h", str(path)])
            symbols = cmd([readelf, "-Ws", str(path)])
            if "Machine:" not in hdr or arch not in hdr:
                raise RuntimeError("TOKEN_VAZIO_ELF_MACHINE_" + key.upper())
            if re.search(r"\b(?:GLOBAL|WEAK)\b[^\n]*\bUND\b", symbols):
                raise RuntimeError("IMPORTED_EXTERNAL_SYMBOL_" + key.upper())
            result[key] = {"kind": "ELF_OBJECT_SCOPED", "bytes": path.stat().st_size,
                           "global_undefined": 0, "device_execution": "NOT_RUN"}
        host_so = work / "l0_host.so"
        cmd([cc, *flags, "-shared", "-fPIC", str(native_src), "-o", str(host_so)])
        func = ctypes.CDLL(str(host_so)).rmr_toolchain_l0
        func.argtypes = [ctypes.POINTER(ctypes.c_ubyte), ctypes.POINTER(ctypes.c_ubyte)]
        func.restype = None

        def sample(data):
            raw = (ctypes.c_ubyte * 64).from_buffer_copy(data)
            output = (ctypes.c_ubyte * 2)()
            func(raw, output)
            return (output[0], output[1])

        baseline = matrix(values)
        if sample(baseline) != (1, 1):
            raise RuntimeError("EXPECTED_GRADLE9_KOTLIN1_BLOCK_NOT_OBSERVED")
        for offset, changed_byte, expected_mask in (
            (24, 2, 3), (34, 15, 5), (40, 21, 9),
            (48, 1, 17), (56, 1, 33),
        ):
            negative = bytearray(baseline)
            negative[offset] = changed_byte
            if sample(negative) != (expected_mask, 1):
                raise RuntimeError("NEGATIVE_FIXTURE_FAILED_%d" % offset)
        result.update({"host_execution": "PASS_SCOPED_6_CASES",
                       "conflict_mask": 1, "build_readiness":
                       "BLOCKED_UNSUPPORTED_GRADLE9_KOTLIN1",
                       "claim_allowed": False})
    return result

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=pathlib.Path,
                        default=pathlib.Path(__file__).resolve().parents[1])
    parser.add_argument("--report", type=pathlib.Path)
    opts = parser.parse_args()
    result = {"schema": "rafaelia.l0.toolchain-matrix.v1",
              "status": "FAIL_CLOSED", "claim_allowed": False}
    try:
        values = source_values(opts.root)
        result["source_versions"] = values
        if values != BASE:
            raise RuntimeError("CONFIG_CHANGED_REQUIRES_COORDINATED_MATRIX_REVIEW")
        result.update(execute(values, opts.root /
                              "native/authorial_toolchain_l0/toolchain_l0.c"))
        result["status"] = "PASS_SCOPED_DETECTS_BLOCKED_BASELINE"
    except (OSError, ValueError, RuntimeError) as exc:
        result["error"] = str(exc)
    serialized = json.dumps(result, sort_keys=True, indent=2) + "\n"
    if opts.report:
        opts.report.parent.mkdir(parents=True, exist_ok=True)
        opts.report.write_text(serialized, encoding="utf-8")
    print(serialized, end="")
    if result["status"] == "FAIL_CLOSED":
        raise SystemExit(1)

if __name__ == "__main__":
    main()
