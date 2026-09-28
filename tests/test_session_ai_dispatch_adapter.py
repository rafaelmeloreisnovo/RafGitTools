import json,unittest,importlib.util
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
SPEC=importlib.util.spec_from_file_location('m',ROOT/'scripts/validate_session_ai_dispatch_adapter.py'); M=importlib.util.module_from_spec(SPEC); SPEC.loader.exec_module(M)
class T(unittest.TestCase):
 @classmethod
 def setUpClass(cls): cls.d=json.loads((ROOT/'configs/session-ai-dispatch-adapter.v1.json').read_text())
 def test_valid(self): self.assertEqual(M.validate(self.d)['status'],'PASS')
 def test_no_direct_main(self): self.assertIn('NO_DEFAULT_BRANCH_DIRECT_MUTATION',self.d['invariants'])
 def test_packets_bounded(self):
  for p in self.d['packet_bindings']: self.assertLessEqual(len(p['source_min']),3)
 def test_assignment_not_execution(self): self.assertIn('AGENT_ASSIGNMENT != AGENT_EXECUTION',self.d['invariants'])
if __name__=='__main__': unittest.main()
