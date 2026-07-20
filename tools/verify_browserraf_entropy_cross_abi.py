#!/usr/bin/env python3
"""Compile the BrowserRaf entropy boundary for supported Android ABIs.

This verifier is intentionally compile-only. A PASS proves that the exact
repository headers compile into the expected relocatable ELF architecture. It
does not prove Android installation, syscall availability at runtime, TLS
interoperability, HTTPS support or certification.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import struct
import subprocess
import tempfile
from datetime import datetime, timezone
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
INCLUDE_DIR = ROOT / "BrowserRaf" / "internal"
SOURCES = (
    INCLUDE_DIR / "br_types.h",
    INCLUDE_DIR / "br_sys.h",
    INCLUDE_DIR / "br_entropy.h",
)
TARGETS = (
    {
        "abi": "armeabi-v7a",
        "triple": "armv7a-linux-androideabi21",
        "elf_class": 1,
        "machine": 40,
    },
    {
        "abi": "arm64-v8a",
        "triple": "aarch64-linux-android21",
        "elf_class": 2,
        "machine": 183,
    },
    {
        "abi": "x86_64",
        "triple": "x86_64-linux-android21",
        "elf_class": 2,
        "machine": 62,
    },
)
IGNORED_WARNINGS = ("unused-function",)
PROBE = """#include \"br_entropy.h\"\nint browserraf_entropy_probe(void){u8 b[32];return BR_RANDOM_FILL(b,32u);}\n"""


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def compiler_version(compiler: str) -> str:
    completed = subprocess.run(
        [compiler, "--version"],
        check=True,
        capture_output=True,
        text=True,
    )
    return completed.stdout.splitlines()[0]


def inspect_elf(path: Path, expected_class: int, expected_machine: int) -> dict[str, Any]:
    data = path.read_bytes()
    if len(data) < 20 or data[:4] != b"\x7fELF":
        raise ValueError(f"{path} is not an ELF object")
    if data[4] != expected_class:
        raise ValueError(
            f"{path} ELF class {data[4]} does not match expected {expected_class}"
        )
    if data[5] != 1:
        raise ValueError(f"{path} is not little-endian ELF")
    machine = struct.unpack_from("<H", data, 18)[0]
    if machine != expected_machine:
        raise ValueError(
            f"{path} e_machine {machine} does not match expected {expected_machine}"
        )
    return {
        "bytes": len(data),
        "sha256": hashlib.sha256(data).hexdigest(),
        "elf_class": data[4],
        "elf_machine": machine,
    }


def verify(compiler: str) -> dict[str, Any]:
    missing = [str(path.relative_to(ROOT)) for path in SOURCES if not path.is_file()]
    if missing:
        raise FileNotFoundError(f"missing source files: {', '.join(missing)}")

    source_hashes = {
        str(path.relative_to(ROOT)): sha256_file(path)
        for path in SOURCES
    }
    results: list[dict[str, Any]] = []

    with tempfile.TemporaryDirectory(prefix="browserraf-csprng-") as tmp:
        tmp_path = Path(tmp)
        probe = tmp_path / "probe.c"
        probe.write_text(PROBE, encoding="utf-8")

        for target in TARGETS:
            output = tmp_path / f"{target['abi']}.o"
            command = [
                compiler,
                f"--target={target['triple']}",
                "-std=c11",
                "-ffreestanding",
                "-fno-builtin",
                "-nostdlib",
                "-Wall",
                "-Wextra",
                "-Werror",
                "-Wno-unused-function",
                "-O2",
                "-c",
                str(probe),
                "-I",
                str(INCLUDE_DIR),
                "-o",
                str(output),
            ]
            completed = subprocess.run(
                command,
                check=False,
                capture_output=True,
                text=True,
            )
            result: dict[str, Any] = {
                "abi": target["abi"],
                "triple": target["triple"],
                "command": command,
                "returncode": completed.returncode,
                "stdout": completed.stdout,
                "stderr": completed.stderr,
            }
            if completed.returncode != 0:
                result["status"] = "FAIL"
                results.append(result)
                continue

            try:
                result.update(
                    inspect_elf(
                        output,
                        expected_class=int(target["elf_class"]),
                        expected_machine=int(target["machine"]),
                    )
                )
                result["status"] = "PASS"
            except ValueError as exc:
                result["status"] = "FAIL"
                result["error"] = str(exc)
            results.append(result)

    passed = all(item["status"] == "PASS" for item in results)
    return {
        "schema": "raf.browserraf-csprng-cross-abi-proof.v1",
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "repository": "rafaelmeloreisnovo/RafGitTools",
        "scope": "compile_only",
        "claim_allowed": False,
        "runtime_proved": False,
        "https_enabled": False,
        "compiler": compiler,
        "compiler_version": compiler_version(compiler),
        "ignored_warnings": list(IGNORED_WARNINGS),
        "source_hashes": source_hashes,
        "results": results,
        "status": "PASS" if passed else "FAIL",
    }


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--compiler", default="clang")
    parser.add_argument("--output", type=Path)
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    try:
        manifest = verify(args.compiler)
    except (FileNotFoundError, OSError, subprocess.SubprocessError) as exc:
        manifest = {
            "schema": "raf.browserraf-csprng-cross-abi-proof.v1",
            "generated_at": datetime.now(timezone.utc).isoformat(),
            "repository": "rafaelmeloreisnovo/RafGitTools",
            "scope": "compile_only",
            "claim_allowed": False,
            "runtime_proved": False,
            "https_enabled": False,
            "status": "FAIL",
            "error": str(exc),
        }

    rendered = json.dumps(manifest, indent=2, sort_keys=True) + "\n"
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        temporary = args.output.with_suffix(args.output.suffix + ".tmp")
        temporary.write_text(rendered, encoding="utf-8")
        temporary.replace(args.output)
    print(rendered, end="")
    return 0 if manifest["status"] == "PASS" else 1


if __name__ == "__main__":
    raise SystemExit(main())
