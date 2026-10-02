from __future__ import annotations

import pathlib
import shutil
import subprocess
import sys
import tempfile
import unittest


ROOT = pathlib.Path(__file__).resolve().parents[1]
CORE = ROOT / "freestanding" / "orchestration" / "raf_orchestrator_l0.c"
SELFTEST = ROOT / "freestanding" / "orchestration" / "raf_orchestrator_l0_selftest.c"
INCLUDE = ROOT / "freestanding" / "orchestration"
VALIDATOR = ROOT / "scripts" / "validate_freestanding_jvm_orchestration.py"

COMMON = [
    "-std=c11",
    "-Wall",
    "-Wextra",
    "-Werror",
    "-pedantic",
    "-ffreestanding",
    "-fno-builtin",
    "-fno-stack-protector",
]


class FreestandingOrchestratorL0Test(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.clang = shutil.which("clang")
        cls.nm = shutil.which("nm")
        if cls.clang is None:
            raise AssertionError("clang is required for the canonical L0 gate")
        if cls.nm is None:
            raise AssertionError("nm is required for the canonical L0 gate")

    def run_checked(self, *args: str) -> subprocess.CompletedProcess[str]:
        return subprocess.run(
            [*args],
            cwd=ROOT,
            check=True,
            text=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )

    def test_manifest_contract_is_fail_closed(self) -> None:
        result = self.run_checked(sys.executable, str(VALIDATOR))
        self.assertIn("PASS_SCOPED", result.stdout)
        self.assertIn("root=FREESTANDING_L0", result.stdout)

    def test_exact_source_compiles_for_host_armv7_aarch64_without_externals(self) -> None:
        with tempfile.TemporaryDirectory(prefix="raf-l0-") as temp:
            temp_path = pathlib.Path(temp)
            host = temp_path / "raf_orchestrator_l0.host.o"
            armv7 = temp_path / "raf_orchestrator_l0.armv7.o"
            aarch64 = temp_path / "raf_orchestrator_l0.aarch64.o"

            self.run_checked(
                self.clang,
                *COMMON,
                f"-I{INCLUDE}",
                "-c",
                str(CORE),
                "-o",
                str(host),
            )
            self.run_checked(
                self.clang,
                *COMMON,
                "--target=armv7a-none-eabi",
                f"-I{INCLUDE}",
                "-c",
                str(CORE),
                "-o",
                str(armv7),
            )
            self.run_checked(
                self.clang,
                *COMMON,
                "--target=aarch64-none-elf",
                f"-I{INCLUDE}",
                "-c",
                str(CORE),
                "-o",
                str(aarch64),
            )

            undefined = self.run_checked(self.nm, "-u", str(host))
            self.assertEqual("", undefined.stdout.strip())

    def test_hosted_behavioral_harness_preserves_token_vazio_fail_closed(self) -> None:
        with tempfile.TemporaryDirectory(prefix="raf-l0-selftest-") as temp:
            executable = pathlib.Path(temp) / "raf_orchestrator_l0_selftest"
            self.run_checked(
                self.clang,
                "-std=c11",
                "-Wall",
                "-Wextra",
                "-Werror",
                "-pedantic",
                "-fno-builtin",
                f"-I{INCLUDE}",
                str(CORE),
                str(SELFTEST),
                "-o",
                str(executable),
            )
            self.run_checked(str(executable))


if __name__ == "__main__":
    unittest.main()
