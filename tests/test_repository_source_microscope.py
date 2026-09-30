import importlib.util
import pathlib
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]
SPEC = importlib.util.spec_from_file_location("repository_source_microscope", ROOT / "scripts" / "repository_source_microscope.py")
MOD = importlib.util.module_from_spec(SPEC)
assert SPEC.loader
SPEC.loader.exec_module(MOD)


class RepositorySourceMicroscopeTests(unittest.TestCase):
    def setUp(self):
        self.cfg = MOD.load_config(ROOT / "configs" / "repository-source-microscope.v1.json")

    def test_pat_actions_is_exact_auth_binding(self):
        self.assertEqual(self.cfg["auth"]["secret_reference"], "PAT_ACTIONS")
        self.assertTrue(self.cfg["auth"]["no_fallback"])

    def test_exact_sha_is_required(self):
        self.assertEqual(MOD.validate_commit("a" * 40), "a" * 40)
        for bad in ["main", "ABC", "a" * 39, "g" * 40]:
            with self.assertRaises(ValueError):
                MOD.validate_commit(bad)

    def test_code_precedes_document_compass(self):
        entries = [
            {"type": "blob", "path": "docs/idea.md", "sha": "1", "size": 10},
            {"type": "blob", "path": "src/engine.c", "sha": "2", "size": 10},
            {"type": "blob", "path": "tests/test_engine.py", "sha": "3", "size": 10},
        ]
        ranked = MOD.ranked_blobs(self.cfg, entries, 100)
        self.assertEqual([x["class"] for x in ranked], ["TEST_OR_VERIFIER", "SOURCE_CODE", "DOCUMENT_COMPASS"])

    def test_line_level_match(self):
        found = MOD.find_matches("zero\nPoincare prime graph\nother", ["poincare", "prime"])
        self.assertEqual(found[0]["line"], 2)
        self.assertEqual(found[0]["needles"], ["poincare", "prime"])

    def test_private_science_repositories_are_allowlisted(self):
        repos = {x["repository"] for x in self.cfg["repositories"]}
        for repo in [
            "rafaelmeloreisnovo/papers", "rafaelmeloreisnovo/Matem-tica-",
            "rafaelmeloreisnovo/ChipQuantum", "rafaelmeloreisnovo/teoremas",
            "rafaelmeloreisnovo/Clima", "rafaelmeloreisnovo/Cosmos"
        ]:
            self.assertIn(repo, repos)


if __name__ == "__main__":
    unittest.main()
