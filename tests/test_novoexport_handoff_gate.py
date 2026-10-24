import copy
import unittest
from scripts.novoexport_handoff_gate import check

GOOD = {
 "schema":"rafaelia.novoexport-four-node-handoff.v1",
 "intent":"NAME_CATALOG_METADATA",
 "origin":{"provider":"GOOGLE_DRIVE","file_name":"receipt","source_ref":"drive:example",
           "source_sha256":"TOKEN_VAZIO_UNVERIFIED","role":"CUSTODY_POINTER"},
 "snapshot":{"novoexport_repository":"owner/private","novoexport_commit":"0123456789abcdef0123456789abcdef01234567",
             "rafgittools_repository":"rafaelmeloreisnovo/RafGitTools",
             "rafgittools_commit":"fedcba9876543210fedcba9876543210fedcba98","drive_receipt_ref":"drive:example"},
 "data_boundary":{"transfer_mode":"METADATA_ONLY","raw_corpus_in_payload":False,
                  "secret_values_in_payload":False,"destination_visibility":"private"},
 "plan":{"state":"PLAN_ONLY","write_requires_human_approval":True,"execution_authorized":False,
         "name_relation_state":"CANDIDATE_ONLY","rollback":"APPEND_ONLY_SUCCESSOR"},
 "gates":{"physical_device":"TOKEN_VAZIO_NOT_RUN","github_exact_head_ci":"TOKEN_VAZIO_UNVERIFIED",
          "drive_readback":"TOKEN_VAZIO_UNVERIFIED","independent_review":"TOKEN_VAZIO_NOT_RUN"},
 "claim_allowed":False
}
class HandoffGateTests(unittest.TestCase):
 def test_valid_only_as_plan(self):
  result=check(copy.deepcopy(GOOD))
  self.assertEqual(result["state"],"PASS_SCOPED_PLAN_ONLY")
  self.assertFalse(result["execution_authorized"])
 def test_repeat_deterministic(self):
  self.assertEqual(check(GOOD)["packet_sha256"],check(copy.deepcopy(GOOD))["packet_sha256"])
 def reject(self,section,key,value):
  p=copy.deepcopy(GOOD)
  p[section][key]=value
  with self.assertRaises(ValueError):check(p)
 def test_no_execute(self):self.reject("plan","execution_authorized",True)
 def test_no_public_recipient(self):self.reject("data_boundary","destination_visibility","public")
 def test_no_fake_ci_pass(self):self.reject("gates","github_exact_head_ci","PASS")
 def test_no_unpinned_commit(self):self.reject("snapshot","rafgittools_commit","main")
 def test_no_raw_android_capability(self):self.reject("origin","source_ref","content://private")
 def test_no_equivalence_claim(self):self.reject("plan","name_relation_state","VERIFIED")
 def test_unknown_is_not_zero(self):self.reject("origin","source_sha256","0")

if __name__=="__main__":unittest.main()
