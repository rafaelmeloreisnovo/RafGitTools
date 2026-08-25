#!/usr/bin/env python3
"""Validate pinned GitHub Actions and emit a minimal SPDX evidence document."""
from __future__ import annotations
import argparse, json, re
from pathlib import Path
SHA=re.compile(r'^[0-9a-f]{40}$')
def main()->int:
    p=argparse.ArgumentParser();p.add_argument('--lock',type=Path,required=True);p.add_argument('--workflow',type=Path,required=True);p.add_argument('--requirements',type=Path,required=True);p.add_argument('--report',type=Path,required=True);a=p.parse_args()
    lock=json.loads(a.lock.read_text(encoding='utf-8'));wf=a.workflow.read_text(encoding='utf-8');req=[x.strip() for x in a.requirements.read_text(encoding='utf-8').splitlines() if x.strip() and not x.lstrip().startswith('#')]
    errors=[];packages=[]
    for item in lock.get('actions',[]):
        sha=item.get('sha','');ref=f"{item.get('name')}@{sha}"
        if not SHA.fullmatch(sha):errors.append(f"invalid action SHA: {item.get('name')}")
        if ref not in wf:errors.append(f"workflow does not use pinned action: {ref}")
        packages.append({'SPDXID':'SPDXRef-Action-'+item['name'].replace('/','-'),'name':item['name'],'versionInfo':item['version'],'downloadLocation':item['source'],'checksums':[{'algorithm':'SHA1','checksumValue':sha}]})
    for line in req:
        if '==' not in line:errors.append(f"unpinned Python requirement: {line}");continue
        name,version=line.split('==',1);packages.append({'SPDXID':'SPDXRef-Python-'+name,'name':name,'versionInfo':version,'downloadLocation':'https://pypi.org/project/'+name+'/'})
    out={'spdxVersion':'SPDX-2.3','dataLicense':'CC0-1.0','SPDXID':'SPDXRef-DOCUMENT','name':'RafGitTools federation audit supply chain','documentNamespace':'https://github.com/rafaelmeloreisnovo/RafGitTools/federation-audit/v2','creationInfo':{'creators':['Tool: scripts/federation/supply_chain.py'],'created':'2026-07-16T22:00:00Z'},'packages':packages,'annotations':[{'annotationType':'OTHER','annotator':'Tool: supply_chain.py','annotationDate':'2026-07-16T22:00:00Z','comment':'Attestation/signature remains TOKEN_VAZIO until an external signing step is configured.'}],'validation':{'status':'PASS' if not errors else 'FAIL','errors':errors}}
    a.report.parent.mkdir(parents=True,exist_ok=True);a.report.write_text(json.dumps(out,indent=2,ensure_ascii=False)+'\n',encoding='utf-8');print(json.dumps(out['validation'],indent=2))
    return 1 if errors else 0
if __name__=='__main__':raise SystemExit(main())
