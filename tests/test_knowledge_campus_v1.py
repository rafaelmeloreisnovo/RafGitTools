import importlib.util
import pathlib
import unittest

ROOT=pathlib.Path(__file__).resolve().parents[1]
SPEC=importlib.util.spec_from_file_location("validator",ROOT/"scripts/validate_knowledge_campus_v1.py")
validator=importlib.util.module_from_spec(SPEC); SPEC.loader.exec_module(validator)

class KnowledgeCampusTests(unittest.TestCase):
    def canonical(self):
        return {
          "schema":"rafgittools.knowledge-campus-logistics.v1","object_id":"x","kind":"CAMPUS",
          "source_ref":"s","materialization_state":"IMPLEMENTED_UNTESTED",
          "characteristics":[],"relations":[],"evidence_refs":[],"gaps":[],"routes":[]
        }
    def test_canonical_passes(self): self.assertEqual([],validator.validate(self.canonical()))
    def test_missing_source_fails(self):
        v=self.canonical(); del v["source_ref"]; self.assertTrue(validator.validate(v))
    def test_unknown_kind_fails(self):
        v=self.canonical(); v["kind"]="MAGIC"; self.assertIn("kind",validator.validate(v))
    def test_unknown_relation_fails(self):
        v=self.canonical(); v["relations"]=[{"type":"CAUSES","target":"y"}]
        self.assertIn("relation_type",validator.validate(v))
    def test_parable_cannot_promote_itself_to_evidence(self):
        v=self.canonical(); v["kind"]="PARABLE"; v["relations"]=[{"type":"EVIDENCED_BY","target":"self"}]
        self.assertIn("parable_cannot_self_promote_as_evidence",validator.validate(v))

if __name__=="__main__": unittest.main()
