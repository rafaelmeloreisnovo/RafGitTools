import copy
import unittest

from tools.verify_chart_source_ancestry_v1 import verify_parent_chain

# Synthetic IDs only. No private provider IDs or source contents.
ROOT=[
 {"id":"source","parents":["chartdir"],"kind":"file"},
 {"id":"chartdir","parents":["project"],"kind":"folder"},
 {"id":"project","parents":["trusted"],"kind":"folder"},
 {"id":"trusted","parents":[],"kind":"folder"},
 {"id":"foreign","parents":[],"kind":"folder"}
]
def v(nodes=None,source="source",root="trusted",depth=64):
    return verify_parent_chain(copy.deepcopy(ROOT if nodes is None else nodes),source,root,depth)

class SourceAncestryTests(unittest.TestCase):
    def test_verified_snapshot_only(self):
        r=v()
        self.assertEqual(r.state,"PASS_STRUCTURAL_ONLY")
        self.assertEqual(r.depth,3)
        self.assertFalse(r.claim_allowed)
        self.assertEqual(r.scope,"STRUCTURAL_METADATA_ONLY")
    def test_foreign_parent_is_not_novoexport_membership(self):
        d=copy.deepcopy(ROOT)
        d[2]["parents"]=["foreign"]
        self.assertEqual(v(d).state,"BLOCK_OUTSIDE_TRUSTED_ROOT")
    def test_missing_parent_no_implicit_root(self):
        d=copy.deepcopy(ROOT)
        d.pop(2)
        self.assertEqual(v(d).state,"BLOCK_MISSING_PARENT")
    def test_duplicate_id_rejected(self):
        d=copy.deepcopy(ROOT)
        d.append(copy.deepcopy(d[0]))
        self.assertEqual(v(d).state,"BLOCK_SCHEMA")
    def test_ambiguous_parent_set_rejected(self):
        d=copy.deepcopy(ROOT)
        d[0]["parents"]=["chartdir","foreign"]
        self.assertEqual(v(d).state,"BLOCK_SCHEMA")
    def test_cycle_rejected(self):
        d=copy.deepcopy(ROOT)
        d[2]["parents"]=["chartdir"]
        self.assertEqual(v(d).state,"BLOCK_CYCLE")
    def test_self_parent_rejected(self):
        d=copy.deepcopy(ROOT)
        d[0]["parents"]=["source"]
        self.assertEqual(v(d).state,"BLOCK_CYCLE")
    def test_parent_is_file_rejected(self):
        d=copy.deepcopy(ROOT)
        d[1]["kind"]="file"
        self.assertEqual(v(d).state,"BLOCK_PARENT_NOT_FOLDER")
    def test_missing_root_rejected(self):
        d=copy.deepcopy(ROOT)
        d.pop(3)
        self.assertEqual(v(d).state,"BLOCK_ROOT_OR_SOURCE_MISSING")
    def test_source_absent_rejected(self):
        self.assertEqual(v(source="other").state,"BLOCK_ROOT_OR_SOURCE_MISSING")
    def test_root_must_be_folder(self):
        d=copy.deepcopy(ROOT)
        d[3]["kind"]="file"
        self.assertEqual(v(d).state,"BLOCK_TRUSTED_ROOT_NOT_FOLDER")
    def test_depth_limit(self):
        self.assertEqual(v(depth=2).state,"BLOCK_DEPTH_LIMIT")
    def test_negative_depth(self):
        self.assertEqual(v(depth=0).state,"BLOCK_BAD_INPUT")
    def test_no_missing_source_id(self):
        self.assertEqual(v(source="").state,"BLOCK_BAD_INPUT")
    def test_no_fabricated_same_source_and_root(self):
        self.assertEqual(v(source="trusted").state,"BLOCK_BAD_INPUT")
    def test_no_schema_extras(self):
        d=copy.deepcopy(ROOT)
        d[0]["approved"]=True
        self.assertEqual(v(d).state,"BLOCK_SCHEMA")
    def test_no_untyped_parent(self):
        d=copy.deepcopy(ROOT)
        d[0]["parents"]=[None]
        self.assertEqual(v(d).state,"BLOCK_SCHEMA")
    def test_source_not_inside_file_as_parent(self):
        d=copy.deepcopy(ROOT)
        d[0]["parents"]=["source2"]
        d.append({"id":"source2","parents":["trusted"],"kind":"file"})
        self.assertEqual(v(d).state,"BLOCK_PARENT_NOT_FOLDER")
    def test_directory_cannot_masquerade_as_chart_source(self):
        d=copy.deepcopy(ROOT)
        d[0]["kind"]="folder"
        self.assertEqual(v(d).state,"BLOCK_SOURCE_NOT_FILE")
    def test_larger_than_bounded_metadata(self):
        d=copy.deepcopy(ROOT)+[
          {"id":"f"+str(i),"parents":[],"kind":"folder"} for i in range(125)
        ]
        self.assertEqual(v(d).state,"BLOCK_BAD_INPUT")
    def test_empty_parent_is_external(self):
        d=copy.deepcopy(ROOT)
        d[2]["parents"]=[]
        self.assertEqual(v(d).state,"BLOCK_OUTSIDE_TRUSTED_ROOT")

if __name__=="__main__":
    unittest.main()
