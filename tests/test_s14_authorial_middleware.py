"""Negative tests for an advisory producer/consumer contract; HOSTED only."""
import copy
import json
import sys
import unittest
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/"scripts"))
from validate_s14_authorial_middleware import validate
P=Path(__file__).resolve().parents[1]/"contracts"/"authorial_middleware_s14_handoff.v1.json"
class Gate(unittest.TestCase):
    @classmethod
    def setUpClass(cls): cls.g=json.loads(P.read_text("utf8"))
    def reject(self,edit,reason):
        d=copy.deepcopy(self.g);edit(d);self.assertEqual(validate(d),reason)
    def test_scoped(self): self.assertEqual(validate(self.g),"PASS_SCOPED_HOLD")
    def test_sha(self): self.reject(lambda d:d["producer"].update(head="0"*40),"BLOCK_PRODUCER_SHA")
    def test_c_blob(self): self.reject(lambda d:d["producer"].update(blob_sha1="a"*40),"BLOCK_PRODUCER_SHA")
    def test_no_core_claim(self): self.reject(lambda d:d["pure_core"].update(output_executed_always_zero=False),"BLOCK_ABI")
    def test_heap(self): self.reject(lambda d:d["pure_core"].update(heap=True),"BLOCK_EXTERNAL_RUNTIME")
    def test_jni(self): self.reject(lambda d:d["pure_core"].update(jni=True),"BLOCK_EXTERNAL_RUNTIME")
    def test_actual_watchdogs(self): self.reject(lambda d:d["safety"].update(watchdogs="REAL_OS_THREAD"),"BLOCK_FALSE_EXECUTION")
    def test_rollback(self): self.reject(lambda d:d["safety"].update(rollback="AUTOMATICALLY_COMPLETED"),"BLOCK_FALSE_EXECUTION")
    def test_gpu(self): self.reject(lambda d:d["evidence"].update(actual_gpu_numa="PASS"),"BLOCK_FALSE_PROMOTION")
    def test_arm_device(self): self.reject(lambda d:d["evidence"].update(physical_arm_device="PASS"),"BLOCK_FALSE_PROMOTION")
    def test_adapter(self): self.reject(lambda d:d["android"].update(adapter="IMPLEMENTED"),"BLOCK_UNVERIFIED_ADAPTER")
    def test_user_review(self): self.reject(lambda d:d["safety"].update(human_review="PASS"),"BLOCK_FALSE_EXECUTION")
    def test_no_merge(self): self.reject(lambda d:d.update(rollback="MERGED"),"BLOCK_NO_REVERSIBILITY")
if __name__=="__main__":unittest.main()
