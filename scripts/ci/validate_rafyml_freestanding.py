#!/usr/bin/env python3
from __future__ import annotations
import hashlib
import json
import pathlib
import shutil
import subprocess
import sys
import tempfile

ROOT = pathlib.Path(__file__).resolve().parents[2]
COMPILER = ROOT / "tools" / "rafymlc" / "rafymlc.py"
INPUT = ROOT / "examples" / "rafyml" / "config.yml"


def run(command: list[str]) -> None:
    subprocess.run(command, cwd=ROOT, check=True)


def main() -> int:
    run([sys.executable, "-m", "unittest", "discover", "-s", "tests", "-p", "test_rafymlc.py", "-v"])
    with tempfile.TemporaryDirectory() as tmp_raw:
        tmp = pathlib.Path(tmp_raw)
        out_a = tmp / "a"
        out_b = tmp / "b"
        run([sys.executable, str(COMPILER), "emit-c", str(INPUT), "--out", str(out_a), "--prefix", "config"])
        run([sys.executable, str(COMPILER), "emit-c", str(INPUT), "--out", str(out_b), "--prefix", "config"])
        for name in ("config.generated.h", "config.generated.c"):
            if out_a.joinpath(name).read_bytes() != out_b.joinpath(name).read_bytes():
                raise SystemExit(f"non-deterministic output: {name}")
        compiler = shutil.which("clang") or shutil.which("cc") or shutil.which("gcc")
        if not compiler:
            raise SystemExit("no C compiler found")
        common = [compiler, "-std=c11", "-ffreestanding", "-fno-builtin", "-fno-stack-protector", "-I", str(ROOT / "include"), "-I", str(out_a), "-c"]
        run(common + [str(ROOT / "src" / "rafyml_runtime.c"), "-o", str(tmp / "runtime.o")])
        run(common + [str(out_a / "config.generated.c"), "-o", str(tmp / "config.o")])
        checks = ["unit", "determinism", "host-freestanding-object"]
        for target in ("armv7a-linux-gnueabihf", "aarch64-linux-gnu"):
            if pathlib.Path(compiler).name.startswith("clang"):
                run(common[:1] + [f"--target={target}"] + common[1:] + [str(out_a / "config.generated.c"), "-o", str(tmp / f"{target}.o")])
                checks.append(f"{target}-object")
        receipt = {
            "schema": "rafgittools.rafyml-freestanding-receipt/v1",
            "state": "LOCAL_PASS",
            "claim_allowed": False,
            "input_sha256": hashlib.sha256(INPUT.read_bytes()).hexdigest(),
            "generated_c_sha256": hashlib.sha256((out_a / "config.generated.c").read_bytes()).hexdigest(),
            "generated_h_sha256": hashlib.sha256((out_a / "config.generated.h").read_bytes()).hexdigest(),
            "checks": checks,
        }
        print(json.dumps(receipt, indent=2, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
