"""Differential CI falsifiers for authorial freestanding L0 draft routing.

Compiles Linux x86_64 syscall ELF with no libc and compares every result with
the legacy Python oracle. Do not replace legacy route until this gate passes.
"""
import importlib.util
from pathlib import Path
import platform
import random
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "scripts/ci/native/draft_route_l0.c"
LEGACY = ROOT / "scripts/ci/draft_lane_guard.py"
spec = importlib.util.spec_from_file_location("legacy_route", LEGACY)
assert spec and spec.loader
oracle = importlib.util.module_from_spec(spec)
spec.loader.exec_module(oracle)

class NativeDraftRouteParityTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        if platform.system() != "Linux" or platform.machine() != "x86_64":
            raise unittest.SkipTest("Native prototype restricted to Ubuntu x86_64")
        cls.temp = tempfile.TemporaryDirectory(prefix="l0-route-")
        cls.binary = Path(cls.temp.name) / "raf-route"
        subprocess.run(
            ["cc", "-std=c11", "-O2", "-ffreestanding", "-fno-builtin",
             "-fno-stack-protector", "-nostdlib", "-no-pie", "-Wl,-e,_start",
             "-Wl,--build-id=none", "-o", str(cls.binary), str(SRC)],
            check=True, capture_output=True
        )
    @classmethod
    def tearDownClass(cls):
        if hasattr(cls,"temp"): cls.temp.cleanup()
    def run_native(self, payload):
        return subprocess.run([str(self.binary)], input=payload,
                              capture_output=True, check=False, timeout=3)
    def test_valid_known(self):
        for paths in ([],["docs/a.md"],["app/Some.kt"],
                      [".github/workflows/START.yml"],["src/main/Foo.java"],
                      ["docs/maçã.md"],["docs/a.md","app/Foo.kt"],
                      ["app/Foo.kt",".github/actions/foo/action.yml"]):
            with self.subTest(paths=paths):
                expected = oracle.select_lane(paths).encode()+b"\n"
                payload=b"".join(x.encode()+b"\0" for x in paths)
                got=self.run_native(payload)
                self.assertEqual((got.returncode,got.stdout),(0,expected))
    def test_adversarial_path_rejected(self):
        invalid=(b"no-terminator",b"../secret\0",b"/etc/passwd\0",
                 b"a//b\0",b"a/./b\0",b"a/../b\0",b"docs\\x\0",
                 b"docs\nname\0",b"bad\xffutf8\0",b"bad\xc0\xaf\0",
                 b"a/" + b"x"*4097+b"\0")
        for payload in invalid:
            with self.subTest(payload=payload[:20]):
                got=self.run_native(payload)
                self.assertEqual(got.returncode,2)
                self.assertEqual(got.stdout,b"")
    def test_random_differential_2000(self):
        rng=random.Random(20261031)
        names=["docs/a.md","app/Foo.kt",".github/workflows/X.yml",
               "configs/atlas.json","scripts/ci/router.py","gradle/wrapper/a",
               "README.md","tests/ci/a.py","src/main/X.java","notes/maçã.md",
               ".github/dependabot.yml","gradlew","src/a.c","tests/test_a.py"]
        for _ in range(2000):
            paths=rng.choices(names,k=rng.randrange(0,8))
            expected=oracle.select_lane(paths).encode()+b"\n"
            actual=self.run_native(b"".join(x.encode()+b"\0" for x in paths))
            self.assertEqual((actual.returncode,actual.stdout),(0,expected),paths)
    def test_no_interpreter_and_no_heap_symbols(self):
        p=subprocess.run(["readelf","-l",str(self.binary)],capture_output=True,
                         text=True,check=True).stdout
        self.assertNotIn("INTERP",p)
        p=subprocess.run(["nm","-u",str(self.binary)],capture_output=True,
                         text=True,check=True).stdout
        self.assertEqual(p.strip(),"")
if __name__ == "__main__": unittest.main()
