"""Negative/contract tests for private selected-account catalog wrapper."""
import io
import json
from pathlib import Path
from tempfile import TemporaryDirectory
from unittest import TestCase
import sys
ROOT_DIR = Path(__file__).resolve().parents[1]
SCRIPT_DIR = ROOT_DIR / "scripts" if (ROOT_DIR / "scripts").is_dir() else Path(__file__).parent
sys.path.insert(0, str(SCRIPT_DIR))
import drive_account_catalog_v1 as c

ROOT = "1abcdefghijABCDEFGHIJ"
CHILD = "1childFGhijkABCDEFGHI"
EMAIL = "owner@example.com"

class FakeEngine:
    @staticmethod
    def fixture_pages(_):
        return {ROOT: [[{"id":CHILD,"name":"doc.md","mimeType":"text/markdown","parents":[ROOT],"size":"4","modifiedTime":"2026-11-04T00:00:00Z"}]]}
    @staticmethod
    def walk(root, loader, output, **kwargs):
        assert root == ROOT
        pages=list(loader(root))
        assert len(pages)==1
        payload=json.dumps({"source_file_id":CHILD,"source_parent_id":ROOT,"source_name":"doc.md"})+"\n"
        output.write(payload)
        import hashlib
        return {"status":"METADATA_STREAM_COMPLETE","source_root_id":root,
                "counts":{"observed":1,"admitted":1,"rejected":{}},
                "metadata_jsonl_sha256":hashlib.sha256(payload.encode()).hexdigest(),
                "file_contents_read":False,"supabase_written":False,"claim_allowed":False}

class FailedEngine(FakeEngine):
    @staticmethod
    def walk(root, loader, output, **kwargs):
        output.write("PARTIAL\n")
        raise c.GateError("INCOMPLETE_PROVIDER_PAGE")

class Tests(TestCase):
    def test_success_atomic_private_coverage(self):
        with TemporaryDirectory() as parent:
            out=Path(parent)/"CATALOG_001"
            result=c.execute("drive01",EMAIL,ROOT,out,fixture="fixture.json",engine=FakeEngine)
            self.assertTrue((out/"CATALOG.jsonl").exists())
            self.assertEqual(result["mode"],"FIXTURE_ONLY")
            self.assertFalse(result["supabase_written"])
            self.assertFalse(result["archive_members_scanned"])
            self.assertNotIn(EMAIL,(out/"RECEIPT.json").read_text())
            self.assertIn("NOT_RUN",(out/"START_HERE.md").read_text())
            self.assertEqual(len(list(out.iterdir())),3)
            self.assertFalse(Path(str(out)+".lock").exists())
    def test_no_overwrite(self):
        with TemporaryDirectory() as parent:
            out=Path(parent)/"C1"
            c.execute("drive01",EMAIL,ROOT,out,fixture="f",engine=FakeEngine)
            with self.assertRaisesRegex(c.GateError,"ALREADY_EXISTS"):
                c.execute("drive01",EMAIL,ROOT,out,fixture="f",engine=FakeEngine)
    def test_partial_unpublished(self):
        with TemporaryDirectory() as parent:
            out=Path(parent)/"C2"
            with self.assertRaisesRegex(c.GateError,"INCOMPLETE"):
                c.execute("drive01",EMAIL,ROOT,out,fixture="f",engine=FailedEngine)
            self.assertFalse(out.exists())
            self.assertFalse(Path(str(out)+".lock").exists())
            self.assertEqual(list(Path(parent).iterdir()),[])
    def test_invalid_identity(self):
        with TemporaryDirectory() as parent:
            for alias in ["x@example.com","../public",""]:
                with self.assertRaises(c.GateError):
                    c.execute(alias,EMAIL,ROOT,Path(parent)/"C3",fixture="f",engine=FakeEngine)
    def test_principal_mismatch(self):
        class Reply:
            def __enter__(self): return io.BytesIO(b'{"user":{"emailAddress":"someoneelse@example.com"}}')
            def __exit__(self,*args): return None
        with self.assertRaisesRegex(c.GateError,"MISMATCH"):
            c.verify_oauth_subject("opaque",EMAIL,opener=lambda req,timeout:Reply())
    def test_reject_public_parent(self):
        with TemporaryDirectory() as parent:
            p=Path(parent)/"public"
            p.mkdir(mode=0o755)
            with self.assertRaisesRegex(c.GateError,"0700"):
                c.execute("drive01",EMAIL,ROOT,p/"C4",fixture="f",engine=FakeEngine)
