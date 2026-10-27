import importlib.util
from pathlib import Path
import tempfile
import unittest
ROOT=Path(__file__).resolve().parents[1]
def load(path,name):
    spec=importlib.util.spec_from_file_location(name,ROOT/path)
    mod=importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod
guard=load("scripts/ci/verify_public_boundary_v1.py","guard")
scanner=load("scripts/ci/scan_tracked_secrets_v1.py","scanner")

class PublicOnlyTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.src=(ROOT/".github/workflows/START.yml").read_text(encoding="utf-8")
    def test_locked(self):
        self.assertEqual(guard.validate(self.src),[])
    def test_missing_schedule(self):
        self.assertIn("NO_SCHEDULE",guard.validate(self.src.replace("  schedule:","  absent:",1)))
    def test_reenabled_job(self):
        bad=self.src.replace("    if: \${{ false }}","    if: \${{ true }}",1)
        self.assertTrue(any(x.startswith("ENABLED_") for x in guard.validate(bad)))
    def test_missing_route_guard(self):
        self.assertIn("NO_ROUTE_GUARD",guard.validate(self.src.replace('if [[ "$private_ci" == true ]]; then',"NEVER",1)))
    def test_oversize(self):
        with tempfile.TemporaryDirectory() as d:
            (Path(d)/"big").write_bytes(b"x"*2049)
            report=scanner.scan(d,["big"],1024)
            self.assertEqual(report["state"],"INCOMPLETE")
            self.assertFalse(report["claim_repository_clean"])
    def test_token_redacted(self):
        with tempfile.TemporaryDirectory() as d:
            token=b"ghp_"+b"A"*36
            (Path(d)/"example.txt").write_bytes(token)
            report=scanner.scan(d,["example.txt"])
            self.assertEqual(report["state"],"BLOCKED")
            self.assertNotIn(token.decode(),str(report))
            self.assertNotIn("example.txt",str(report))
    def test_scoped_not_full_history(self):
        with tempfile.TemporaryDirectory() as d:
            (Path(d)/"x").write_text("public data")
            report=scanner.scan(d,["x"])
            self.assertEqual(report["state"],"PASS_SCOPED")
            self.assertEqual(report["history"],"NOT_SCANNED")
    def test_symlink_fails_closed(self):
        with tempfile.TemporaryDirectory() as d:
            (Path(d)/"link").symlink_to("/absent")
            self.assertEqual(scanner.scan(d,["link"])["state"],"INCOMPLETE")
if __name__=="__main__":
    unittest.main()
