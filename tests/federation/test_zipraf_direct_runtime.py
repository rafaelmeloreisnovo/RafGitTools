import copy, importlib.util, pathlib, unittest

MODULE=pathlib.Path(__file__).parents[2]/'scripts'/'federation'/'zipraf_direct_runtime.py'
spec=importlib.util.spec_from_file_location('zipraf_runtime',MODULE)
mod=importlib.util.module_from_spec(spec); spec.loader.exec_module(mod)
ROUTES=[{'repository':r,'authority':'runtime','body_copy_allowed':False,'claim_allowed':False} for r in ['ZIPRAF_CORE','ZIPRAF_OMEGA_FULL','llamaRafaelia','Vectras-VM-Android','Rafaelia_Private','GAIA_phi','RafGitTools']]
BASE={'profile':'RAFAELIA-ZIPRAF-DIRECT-RUNTIME-FEDERATION-1','claim_allowed':False,'invariants':{'storage_method':'STORE','decompression_required':False,'stages':['BUFFER','L1_HOT','L2_SHARED'],'core_count_max':8,'physical_cache_control_claimed':False,'fixed_bits_required':True},'routes':ROUTES}

class ZiprafRuntimeFederationTests(unittest.TestCase):
    def test_valid_profile(self): self.assertEqual(mod.validate(BASE),[])
    def test_hidden_decompression_is_rejected(self):
        x=copy.deepcopy(BASE); x['invariants']['decompression_required']=True; self.assertIn('store',mod.validate(x))
    def test_missing_authority_is_rejected(self):
        x=copy.deepcopy(BASE); x['routes']=x['routes'][:-1]; self.assertTrue(any(e.startswith('missing:') for e in mod.validate(x)))
    def test_core_limit_is_eight(self):
        x=copy.deepcopy(BASE); x['invariants']['core_count_max']=16; self.assertIn('cores',mod.validate(x))
    def test_fixed_bits_are_mandatory(self):
        x=copy.deepcopy(BASE); x['invariants']['fixed_bits_required']=False; self.assertIn('fixed_bits',mod.validate(x))

if __name__=='__main__': unittest.main()
