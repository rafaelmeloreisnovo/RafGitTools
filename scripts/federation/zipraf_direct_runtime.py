#!/usr/bin/env python3
import hashlib, json, sys
from pathlib import Path

REQUIRED_REPOS={'ZIPRAF_CORE','ZIPRAF_OMEGA_FULL','llamaRafaelia','Vectras-VM-Android','Rafaelia_Private','GAIA_phi','RafGitTools'}

def validate(doc):
    errors=[]
    if doc.get('profile')!='RAFAELIA-ZIPRAF-DIRECT-RUNTIME-FEDERATION-1': errors.append('profile')
    if doc.get('claim_allowed') is not False: errors.append('claim')
    inv=doc.get('invariants',{})
    if inv.get('storage_method')!='STORE' or inv.get('decompression_required') is not False: errors.append('store')
    if inv.get('stages')!=['BUFFER','L1_HOT','L2_SHARED']: errors.append('stages')
    if inv.get('core_count_max')!=8: errors.append('cores')
    if inv.get('physical_cache_control_claimed') is not False: errors.append('cache_claim')
    if inv.get('fixed_bits_required') is not True: errors.append('fixed_bits')
    repos={r.get('repository') for r in doc.get('routes',[])}
    missing=REQUIRED_REPOS-repos
    if missing: errors.append('missing:'+','.join(sorted(missing)))
    for route in doc.get('routes',[]):
        if route.get('claim_allowed') is not False: errors.append('route_claim:'+str(route.get('repository')))
        if route.get('body_copy_allowed') is not False: errors.append('body_copy:'+str(route.get('repository')))
    return errors

def digest(doc): return hashlib.sha256(json.dumps(doc,sort_keys=True,separators=(',',':')).encode()).hexdigest()

def main():
    doc=json.loads(Path(sys.argv[1]).read_text()); errors=validate(doc)
    out={'status':'PASS' if not errors else 'FAIL','errors':errors,'semantic_sha256':digest(doc),'claim_allowed':False}
    print(json.dumps(out,sort_keys=True)); return 0 if not errors else 1
if __name__=='__main__': raise SystemExit(main())
