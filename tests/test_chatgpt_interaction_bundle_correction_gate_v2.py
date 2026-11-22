import copy, importlib.util, json
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]
P=ROOT/"scripts/route_chatgpt_interaction_bundle_hotfix_v2.py"
spec=importlib.util.spec_from_file_location("router",P)
m=importlib.util.module_from_spec(spec); spec.loader.exec_module(m)
C=json.loads((ROOT/"contracts/chatgpt_interaction_bundle_correction_gate.v2.json").read_text())
F=json.loads((ROOT/"tests/fixtures/chatgpt_interaction_bundle_correction_request.v2.json").read_text())
B={
 "schema":"rafaelia.chatgpt-interaction-bundle/v1",
 "state":"OBSERVED_CONSOLIDATED_NON_TRAINING",
 "training_use":False,"dataset_use":False,"operational_evidence_use":True,
 "claim_allowed":False,"promotion_allowed":False,
 "urgency":[
  {"id":"URG-P0-SCI-FALSE-POSITIVE","level":"P0","provenance":["I1","I2"]},
  {"id":"URG-P0-SESSION-MISCLASSIFICATION","level":"P0","provenance":["I3"]},
  {"id":"URG-P0-AUTO-CORRECT-SAFETY","level":"P0","provenance":["I1","I2","I3"]}
 ],
 "gates":C["required_bundle_gates"],
 "token_vazios":[{"id":"TOKEN_VAZIO_X","type":"NOT_EXECUTED","closure":"run gate"}]
}
class T(unittest.TestCase):
 def test_no_target_holds_without_mutation(self):
  r=m.decide(B,{},C); self.assertEqual(r["state"],"HOTFIX_HOLD_TARGET_REQUIRED"); self.assertIn("DRY_RUN",r["allowed_actions"])
 def test_complete_target_is_eligible(self):
  r=m.decide(B,F,C); self.assertEqual(r["state"],"HOTFIX_ELIGIBLE_BOUNDED"); self.assertFalse(r["merge_allowed"])
 def test_p0_without_provenance_blocks(self):
  b=copy.deepcopy(B); b["urgency"][0]["provenance"]=[]
  self.assertEqual(m.decide(b,F,C)["state"],"BLOCKED")
 def test_token_void_cannot_be_untyped(self):
  b=copy.deepcopy(B); b["token_vazios"][0]["closure"]=""
  self.assertEqual(m.decide(b,F,C)["state"],"BLOCKED")
 def test_direct_main_blocks(self):
  f=copy.deepcopy(F); f["bounded_patch"]["direct_main_write"]=True
  self.assertEqual(m.decide(B,f,C)["state"],"BLOCKED")
 def test_auto_merge_blocks(self):
  f=copy.deepcopy(F); f["bounded_patch"]["auto_merge"]=True
  self.assertEqual(m.decide(B,f,C)["state"],"BLOCKED")
if __name__=="__main__": unittest.main()
