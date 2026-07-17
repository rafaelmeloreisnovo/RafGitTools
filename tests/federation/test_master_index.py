#!/usr/bin/env python3
from __future__ import annotations
import importlib.util, json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]

def module(name,path):
 s=importlib.util.spec_from_file_location(name,path);m=importlib.util.module_from_spec(s);assert s and s.loader;s.loader.exec_module(m);return m
validator=module('validator',ROOT/'scripts'/'federation'/'validate_master_index.py')
recovery=module('recovery',ROOT/'scripts'/'federation'/'recovery_drill.py')
INDEX=ROOT/'configs'/'workflow-master-index.json'

def load():return json.loads(INDEX.read_text(encoding='utf-8'))
def test_index_valid():assert validator.validate(load())==[]
def test_required_ontology_present():
 d=load();assert validator.REQUIRED_NODE_TYPES.issubset(set(d['ontology']['node_types']))
def test_all_unresolved_have_owner_action_exit():
 for n in load()['nodes']:
  if n['states']['operational_status'] in validator.EMPTY_STATES or validator.expand_node(load(),n)['runtime_evidence']['status'] in validator.EMPTY_STATES:
   assert n['owner'] and n['next_action'] and n['exit_criteria']
def test_no_allowed_claim_without_runtime():
 data=load()
 for raw in data['nodes']:
  n=validator.expand_node(data,raw)
  if n['states']['claim_gate']=='ALLOWED':assert n['runtime_evidence']['status'] in {'VERIFIED','TESTED'}
def test_temporal_fields_present():
 data=load()
 for raw in data['nodes']:
  n=validator.expand_node(data,raw)
  assert set(n['temporal'])=={'observed_at','valid_from','valid_until','superseded_at','event_sequence'}
def test_relation_contract_complete():
 required={'id'}|validator.REQUIRED_RELATION_FIELDS
 for rel in load()['relations']:assert set(rel)==required
def test_local_rollback_is_real():
 result=recovery.local_file_rollback();assert result['pass'];assert result['before_sha256']==result['after_sha256'];assert result['before_sha256']!=result['mutated_sha256']
def test_remote_recovery_remains_bounded():
 result=recovery.simulate(load(),'vectras');assert result['status']=='SAFE_STATE_REACHED';assert result['remote_runtime_recovery']=='TOKEN_VAZIO';assert result['claim_policy']=='failure cannot promote claims'
