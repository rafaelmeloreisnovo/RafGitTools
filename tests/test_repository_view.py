import importlib.util
import tempfile
import unittest
from pathlib import Path

MODULE_PATH = Path(__file__).parents[1] / "scripts" / "navigation" / "repository_view.py"
spec = importlib.util.spec_from_file_location("repository_view", MODULE_PATH)
rv = importlib.util.module_from_spec(spec)
assert spec.loader is not None
spec.loader.exec_module(rv)


class RepositoryViewTests(unittest.TestCase):
    def test_classification(self):
        self.assertEqual(rv.classify("src/main.c"), "source")
        self.assertEqual(rv.classify("tests/test_core.py"), "test")
        self.assertEqual(rv.classify(".github/workflows/ci.yml"), "workflow")
        self.assertEqual(rv.classify("docs/ARCHITECTURE.md"), "documentation")
        self.assertEqual(rv.classify("mystery.weirdext"), "unknown")

    def test_yaml_emitter_is_stable_for_basic_types(self):
        payload = {"a": 1, "b": False, "c": ["x", {"y": "z"}]}
        text = "\n".join(rv.emit_yaml(payload)) + "\n"
        self.assertIn("a: 1", text)
        self.assertIn("b: false", text)
        self.assertIn('- "x"', text)
        self.assertIn('y: "z"', text)

    def test_build_records_marks_unindexed_source_directory(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            (root / "src").mkdir()
            (root / "src" / "core.c").write_text("int x;\n", encoding="utf-8")
            (root / "README.md").write_text("# root\n", encoding="utf-8")
            files, dirs = rv.build_records(
                root,
                ["README.md", "src/core.c"],
                root / "docs" / "repository-map",
            )
            self.assertEqual(len(files), 2)
            self.assertTrue(dirs["src"]["source_without_local_index"])
            self.assertEqual(dirs["."]["index_state"], "INDEXED_LOCAL")

    def test_output_directory_is_excluded_from_rescan(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            out = root / "docs" / "repository-map"
            out.mkdir(parents=True)
            (out / "INDEX.yml").write_text("old\n", encoding="utf-8")
            (root / "code.py").write_text("print(1)\n", encoding="utf-8")
            files, _ = rv.build_records(
                root,
                ["code.py", "docs/repository-map/INDEX.yml"],
                out,
            )
            self.assertEqual([f["path"] for f in files], ["code.py"])


if __name__ == "__main__":
    unittest.main()
