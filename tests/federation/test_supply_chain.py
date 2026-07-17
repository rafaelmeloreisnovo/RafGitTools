#!/usr/bin/env python3
import json,re
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]

def test_actions_are_full_sha_pins():
    data=json.loads((ROOT/'.github'/'actions-lock.json').read_text())
    assert data['schema_version']=='1.0.0'
    for item in data['actions']:
        assert re.fullmatch(r'[0-9a-f]{40}',item['sha'])

def test_requirements_are_exactly_pinned():
    lines=[x.strip() for x in (ROOT/'requirements'/'federation-audit.lock').read_text().splitlines() if x.strip() and not x.startswith('#')]
    assert lines and all('==' in x for x in lines)
