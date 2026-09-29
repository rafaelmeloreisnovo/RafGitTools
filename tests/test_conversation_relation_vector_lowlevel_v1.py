import importlib.util
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PATH = ROOT / "tools" / "corpus_logistics" / "conversation_relation_vector_lowlevel_v1.py"
SPEC = importlib.util.spec_from_file_location("conversation_relation_vector_lowlevel_v1", PATH)
M = importlib.util.module_from_spec(SPEC)
assert SPEC and SPEC.loader
SPEC.loader.exec_module(M)


class ConversationRelationVectorLowLevelV1Tests(unittest.TestCase):
    def test_embedded_selftest(self):
        M._selftest()

    def test_zero_import_source_contract(self):
        text = PATH.read_text(encoding="utf-8")
        lines = text.splitlines()
        executable_imports = []
        for line in lines:
            s = line.strip()
            if s.startswith("import ") or s.startswith("from "):
                executable_imports.append(s)
        self.assertEqual(executable_imports, [])

    def test_runtime_profile(self):
        plan = {
            "schema": M._INPUT_SCHEMA,
            "raw_body_embedded": False,
            "claim_allowed": False,
            "manifest_sha256": "fixture",
            "chunks": [],
            "edges": [],
        }
        out = M.build_lowlevel(plan)
        self.assertEqual(out["runtime_profile"]["imports"], 0)
        self.assertEqual(out["runtime_profile"]["stdlib_modules"], 0)
        self.assertEqual(out["runtime_profile"]["third_party_modules"], 0)
        self.assertEqual(out["runtime_profile"]["ffi_calls"], 0)
        self.assertEqual(out["runtime_profile"]["native_extension_calls"], 0)
        planes = out["state_planes"]
        self.assertEqual(planes["PROGRAM_STATE"], "PROGRAM_OUTPUT_MATERIALIZED")
        self.assertEqual(planes["MODEL_INFERENCE_STATE"], "NOT_RUN")
        self.assertEqual(planes["PARAMETER_UPDATE_STATE"], "NOT_RUN")
        self.assertEqual(planes["AI_TRAINING"], "NOT_RUN")
        self.assertEqual(planes["parameter_update_evidence"], [])
        self.assertEqual(planes["training_gate"], "BLOCKED_NO_PARAMETER_UPDATE_EVIDENCE")
        self.assertFalse(out["claim_allowed"])
        self.assertFalse(out["raw_body_embedded"])


if __name__ == "__main__":
    unittest.main()
