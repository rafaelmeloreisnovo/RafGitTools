import importlib.util
import os
import stat
import subprocess
import tempfile
import unittest
from pathlib import Path
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parents[1]
SCRIPT = ROOT / "scripts/runtime/capture_android_runtime_receipt.py"
spec = importlib.util.spec_from_file_location("runtime_receipt", SCRIPT)
mod = importlib.util.module_from_spec(spec)
assert spec.loader is not None
spec.loader.exec_module(mod)


def make_apk(path: Path, both=True):
    with ZipFile(path, "w") as zf:
        zf.writestr("AndroidManifest.xml", b"manifest")
        zf.writestr("lib/arm64-v8a/librafaelia_native.so", b"arm64")
        if both:
            zf.writestr("lib/armeabi-v7a/librafaelia_native.so", b"arm32")


def write_exe(path: Path, body: str):
    path.write_text("#!/bin/sh\n" + body, encoding="utf-8")
    path.chmod(path.stat().st_mode | stat.S_IEXEC)


class ReceiptTests(unittest.TestCase):
    def setUp(self):
        self.td = tempfile.TemporaryDirectory()
        self.root = Path(self.td.name)
        self.apk = self.root / "app.apk"
        make_apk(self.apk)
        self.repo = self.root / "repo"
        self.repo.mkdir()
        subprocess.run(["git", "init", "-q", str(self.repo)], check=True)
        subprocess.run(["git", "-C", str(self.repo), "config", "user.email", "test@example.invalid"], check=True)
        subprocess.run(["git", "-C", str(self.repo), "config", "user.name", "test"], check=True)
        (self.repo / "x").write_text("x")
        subprocess.run(["git", "-C", str(self.repo), "add", "x"], check=True)
        subprocess.run(["git", "-C", str(self.repo), "commit", "-qm", "x"], check=True)
        self.commit = subprocess.check_output(["git", "-C", str(self.repo), "rev-parse", "HEAD"], text=True).strip()
        self.bin = self.root / "bin"
        self.bin.mkdir()

    def tearDown(self):
        self.td.cleanup()

    def args(self, **kw):
        base = dict(apk=str(self.apk), output=str(self.root / "out.json"), repo=str(self.repo), expected_commit=self.commit,
                    adb=None, apksigner=None, package=None, activity=None, install=False, launch=False, require_runtime_pass=False)
        base.update(kw)
        return type("Args", (), base)()

    def test_observation_only_never_promotes_runtime(self):
        old = os.environ.get("PATH", "")
        os.environ["PATH"] = "/nonexistent"
        try:
            r = mod.build_receipt(self.args())
        finally:
            os.environ["PATH"] = old
        self.assertFalse(r["claim_allowed"])
        self.assertEqual(mod.BLOCKED, r["gates"]["runtime"])
        self.assertEqual(mod.NOT_MEASURED, r["install"]["state"])
        self.assertEqual(mod.NOT_MEASURED, r["launch"]["state"])

    def test_dual_abi_missing_fails_gate(self):
        make_apk(self.apk, both=False)
        r = mod.build_receipt(self.args())
        self.assertEqual(mod.FAIL, r["apk"]["dual_abi_gate"])
        self.assertFalse(r["claim_allowed"])

    def test_commit_mismatch_is_fail_closed(self):
        r = mod.build_receipt(self.args(expected_commit="0" * 40))
        self.assertEqual(mod.FAIL, r["repo"]["commit_gate"])
        self.assertFalse(r["claim_allowed"])

    def test_full_fake_device_path_can_pass(self):
        apksigner = self.bin / "apksigner"
        adb = self.bin / "adb"
        write_exe(apksigner, 'exit 0\n')
        write_exe(adb, '''case "$*" in
"get-state") echo device ;;
"get-serialno") echo SERIAL123 ;;
"shell getprop ro.product.cpu.abi") echo armeabi-v7a ;;
"shell getprop ro.product.cpu.abilist") echo armeabi-v7a,arm64-v8a ;;
"shell getprop ro.build.version.sdk") echo 35 ;;
"shell getprop ro.build.version.release") echo 15 ;;
"shell getconf PAGESIZE") echo 16384 ;;
install*) echo Success ;;
"shell am start -W -n org.rafaelia/.MainActivity") echo Status: ok ;;
*) exit 9 ;;
esac\n''')
        r = mod.build_receipt(self.args(adb=str(adb), apksigner=str(apksigner), package="org.rafaelia", activity=".MainActivity", install=True, launch=True))
        self.assertEqual(mod.PASS, r["gates"]["runtime"])
        self.assertTrue(r["claim_allowed"])
        self.assertEqual("16384", r["device"]["page_size"])

    def test_x86_device_cannot_promote_arm_runtime(self):
        apksigner = self.bin / "apksigner"
        adb = self.bin / "adb"
        write_exe(apksigner, 'exit 0\n')
        write_exe(adb, '''case "$*" in
"get-state") echo device ;;
"get-serialno") echo X86SERIAL ;;
"shell getprop ro.product.cpu.abi") echo x86_64 ;;
"shell getprop ro.product.cpu.abilist") echo x86_64,x86 ;;
"shell getprop ro.build.version.sdk") echo 35 ;;
"shell getprop ro.build.version.release") echo 15 ;;
"shell getconf PAGESIZE") echo 4096 ;;
install*) echo Success ;;
"shell am start -W -n org.rafaelia/.MainActivity") echo Status: ok ;;
*) exit 9 ;;
esac\n''')
        r = mod.build_receipt(self.args(adb=str(adb), apksigner=str(apksigner), package="org.rafaelia", activity=".MainActivity", install=True, launch=True))
        self.assertEqual(mod.FAIL, r["gates"]["device_abi"])
        self.assertEqual(mod.BLOCKED, r["gates"]["runtime"])
        self.assertFalse(r["claim_allowed"])

    def test_signature_failure_blocks_promotion(self):
        apksigner = self.bin / "apksigner"
        adb = self.bin / "adb"
        write_exe(apksigner, 'exit 1\n')
        write_exe(adb, 'if [ "$1" = get-state ]; then echo device; exit 0; fi\nexit 0\n')
        r = mod.build_receipt(self.args(adb=str(adb), apksigner=str(apksigner), package="org.rafaelia", activity=".MainActivity", install=True, launch=True))
        self.assertEqual(mod.FAIL, r["signature"]["state"])
        self.assertFalse(r["claim_allowed"])


if __name__ == "__main__":
    unittest.main()
