import importlib.util, unittest
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
def load(name,path):
    s=importlib.util.spec_from_file_location(name,path); m=importlib.util.module_from_spec(s); assert s and s.loader; s.loader.exec_module(m); return m

P=load('planner_query_fixture',ROOT/'tools'/'corpus_logistics'/'corpus_logistics_gymnasia_v1.py')
Q=load('campus_query',ROOT/'tools'/'corpus_logistics'/'campus_query_v1.py')

class CampusQueryV1Tests(unittest.TestCase):
    def plan(self):
        return P.build([
          {'source_family':'CONVERSATIONS','book_id':'b1','session_id':'s1','text':'Torque formula x = y','metrics':{'access_count':12},'characteristics':[{'name':'gymnasium','value':'GYM-MATHEMATICS'}]},
          {'source_family':'CODEX','book_id':'r1','session_id':'s2','text':'repository component','metrics':{},'characteristics':[{'name':'gymnasium','value':'GYM-SYSTEMS'}]},
        ])

    def test_token_query_hashes_cleartext(self):
        plan=self.plan()
        raw=str(plan['token_index'])
        self.assertNotIn('Torque',raw)
        out=Q.execute(plan,token='torque')
        self.assertEqual(out['count'],1)
        self.assertFalse(out['claim_allowed'])

    def test_gymnasium_and_tier_query(self):
        plan=self.plan()
        out=Q.execute(plan,gymnasium='GYM-MATHEMATICS',tier='HOT')
        self.assertEqual(out['count'],1)

    def test_limit_fail_closed(self):
        with self.assertRaises(ValueError): Q.execute(self.plan(),limit=1001)

if __name__=='__main__': unittest.main()
