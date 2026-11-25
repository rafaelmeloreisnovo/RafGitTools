import copy
import pathlib
import sys
import unittest
sys.path.insert(0,str(pathlib.Path(__file__).resolve().parents[1]/'scripts'/'ci'))
from reconcile_start_full_pr_v1 import reconcile, EXPECTED, WORKFLOW_BLOB, REPO, WORKFLOW_ID

class ReconcileStartTests(unittest.TestCase):
    def setUp(self):
        h='a'*40
        self.c=dict(repository=REPO,workflow_blob=WORKFLOW_BLOB,event='pull_request',mode='full',base_ref='main',execution_sha=h,attempt=2,pagination_complete=True)
        self.r=dict(id=1,workflow_id=WORKFLOW_ID,status='completed',conclusion='success',run_attempt=2,head_sha=h,event='pull_request')
        self.j=[dict(id=i+1,name=n,run_id=1,status='completed',conclusion=v,run_attempt=2,head_sha=h) for i,(n,v) in enumerate(EXPECTED.items())]
        self.a=dict(id=1,name='start-09-receipt-1',expired=False,digest='sha256:'+'b'*64,run_attempt=2,workflow_run=dict(id=1,head_sha=h,repository_id=1131213900,head_repository_id=1131213900))
    def run_gate(self):return reconcile(self.c,self.r,self.j,self.a)
    def deny(self):self.assertEqual(self.run_gate()['state'],'REJECTED')
    def test_complete_still_observation_only(self):
        x=self.run_gate();self.assertTrue(x['metadata_complete']);self.assertEqual(x['state'],'OBSERVATION_ONLY');self.assertFalse(x['lifecycle_binding_ready']);self.assertFalse(x['skip_allowed']);self.assertFalse(x['claim_allowed'])
    def test_missing_attempts_and_pagination(self):
        self.j[0].pop('run_attempt');self.a.pop('run_attempt');self.c.pop('pagination_complete');x=self.run_gate();self.assertEqual(x['state'],'OBSERVATION_ONLY');self.assertFalse(x['metadata_complete']);self.assertIn('ARTIFACT:run_attempt',x['reasons'])
    def test_wrong_job_context(self):
        original=copy.deepcopy(self.j[0])
        for k,v in [('run_id',True),('run_id',9),('run_attempt',1),('head_sha','c'*40),('status','queued'),('conclusion','skipped')]:
            with self.subTest(k=k,v=v):self.j[0][k]=v;self.deny();self.j[0]=copy.deepcopy(original)
    def test_duplicate_omitted_extra_jobs(self):
        original=copy.deepcopy(self.j);self.j.pop();self.deny();self.j=copy.deepcopy(original);self.j[-1]=self.j[0];self.deny();self.j=copy.deepcopy(original);self.j[-1]['name']='invented';self.deny()
    def test_matrix_and_unmapped_jobs_must_match(self):
        for n in ('07 · CodeQL / actions','09Y · PAT_AGENTS / identity and org preflight (read-only)'):
            j=next(j for j in self.j if j['name']==n);v=j['conclusion'];j['conclusion']='failure';self.deny();j['conclusion']=v
    def test_wrong_artifact_or_workflow(self):
        for k,v in [('expired',True),('run_attempt',1),('name','other')]:
            old=self.a[k];self.a[k]=v;self.deny();self.a[k]=old
        self.c['workflow_blob']='c'*40;self.deny()
    def test_no_self_declared_stage_map(self):
        self.c['expected_stages']={'android':'TOKEN_VAZIO_NOT_SELECTED'};x=self.run_gate();self.assertEqual(x['expected_stages']['android'],'SUCCESS')
