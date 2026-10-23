import copy,importlib.util,json
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]
P=ROOT/"scripts/route_observer_conduct_correction_v1.py"
spec=importlib.util.spec_from_file_location("r",P); m=importlib.util.module_from_spec(spec); spec.loader.exec_module(m)
C=json.loads((ROOT/"contracts/observer_conduct_correction_router.v1.json").read_text())
F=json.loads((ROOT/"tests/fixtures/observer_correction_request.v1.json").read_text())
BASE={"schema":"rafaelia.observer-observation-intake/v1","claim_allowed":False,"epistemic_state":"OBSERVED","conduct":["ATTENTION"],"science_routes":[{"id":"SCI-SWE-FORMAL","role":"PRIMARY"}]}
class T(unittest.TestCase):
 def test_attention_no_mutation(self): self.assertEqual(m.decide(BASE,{},C)["state"],"OBSERVATION_ONLY_NO_MUTATION")
 def test_unknown_holds(self):
  x=copy.deepcopy(BASE); x["epistemic_state"]="UNKNOWN"; x["conduct"]=["BUG"]
  self.assertEqual(m.decide(x,F,C)["state"],"REVIEW_ONLY_HOLD")
 def test_refuse_preserved(self):
  x=copy.deepcopy(BASE); x["conduct"]=["REFUSE"]; self.assertEqual(m.decide(x,F,C)["state"],"OBSERVATION_REFUSED_PRESERVED")
 def test_bug_without_envelope_holds(self):
  x=copy.deepcopy(BASE); x["conduct"]=["BUG"]; self.assertEqual(m.decide(x,{},C)["state"],"HOTFIX_HOLD_TARGET_REQUIRED")
 def test_bug_complete_is_eligible(self):
  x=copy.deepcopy(BASE); x["conduct"]=["BUG"]; self.assertEqual(m.decide(x,F,C)["state"],"HOTFIX_ELIGIBLE_BOUNDED")
 def test_auto_merge_blocks(self):
  x=copy.deepcopy(BASE); x["conduct"]=["ERROR"]; f=copy.deepcopy(F); f["bounded_patch"]["auto_merge"]=True
  self.assertEqual(m.decide(x,f,C)["state"],"BLOCKED")
if __name__=="__main__": unittest.main()
