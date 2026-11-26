#!/usr/bin/env python3
"""Synthetic adversarial tests; no private IDs, source bytes or network I/O."""
import importlib.util
from pathlib import Path
import unittest

src=Path(__file__).resolve().parents[1]/'scripts'/'drive_folder_metadata_normalize_v1.py'
spec=importlib.util.spec_from_file_location('drvmeta',src)
m=importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)
ID='1synthetic_id_123456789'
PARENT='1synthetic_parent_123456789'
START='2026-11-20T08:00:00Z'
END='2026-11-20T09:00:00Z'


def model():
    return {'id':ID,'created_time':START,'modified_time':END,'parent_ids':[PARENT]}

class NormalizeTests(unittest.TestCase):
    def blocked(self,record,reason):
        with self.assertRaisesRegex(m.MetadataGateError,reason):
            m.normalize(record)

    def test_created_updated_provider_alias(self):
        got,state=m.normalize({'id':ID,'created_at':START,'updated_at':END,'parent_ids':[PARENT]})
        self.assertEqual(got,model())
        self.assertEqual(state['parent_state'],'OBSERVED_ARRAY')

    def test_raw_drive_v3_alias(self):
        got,_=m.normalize({'id':ID,'createdTime':START,'modifiedTime':END,'parents':[PARENT]})
        self.assertEqual(got,model())

    def test_stored_manifest_alias(self):
        got,_=m.normalize(model())
        self.assertEqual(got,model())

    def test_conflicting_date_aliases(self):
        self.blocked({**model(),'updated_at':'2026-11-19T09:00:00Z'},'CONFLICTING_MODIFIED_ALIASES')

    def test_conflicting_parent_aliases(self):
        self.blocked({**model(),'parents':[]},'CONFLICTING_PARENT_ALIASES')

    def test_explicit_parent_null(self):
        out,state=m.normalize({**model(),'parent_ids':None})
        self.assertIsNone(out['parent_ids'])
        self.assertEqual(state['parent_state'],'PROVIDER_EXPLICIT_NULL')
        self.assertFalse(state['claim_allowed'])

    def test_missing_parent_distinct(self):
        data=model();data.pop('parent_ids')
        out,state=m.normalize(data)
        self.assertIsNone(out['parent_ids'])
        self.assertEqual(state['parent_state'],'TOKEN_VAZIO_PARENT_FIELD_ABSENT')

    def test_empty_array_distinct(self):
        out,state=m.normalize({**model(),'parent_ids':[]})
        self.assertEqual(out['parent_ids'],[])
        self.assertEqual(state['parent_state'],'EMPTY_ARRAY')

    def test_sort_parent_set(self):
        second='1synthetic_second_parent'
        a,_=m.normalize({**model(),'parent_ids':[second,PARENT]})
        b,_=m.normalize({**model(),'parent_ids':[PARENT,second]})
        self.assertEqual(a,b)

    def test_duplicate_parent_rejected(self):
        self.blocked({**model(),'parent_ids':[PARENT,PARENT]},'INVALID_PARENT_ID_SET')

    def test_missing_modified_blocked(self):
        data=model();data.pop('modified_time')
        self.blocked(data,'MISSING_MODIFIED_TIME')

    def test_invalid_timezone_blocked(self):
        self.blocked({**model(),'modified_time':'2026-11-20T09:00:00'},'TIMEZONE_MISSING')

    def test_invalid_id_bool_blocked(self):
        self.blocked({**model(),'id':True},'INVALID_STABLE_ID')

    def test_nonarray_parent_blocked(self):
        self.blocked({**model(),'parent_ids':'[]'},'INVALID_PARENT_TYPE')

    def test_content_hash_not_proven(self):
        _,state=m.normalize(model())
        self.assertEqual(state['source_content_sha256'],'TOKEN_VAZIO')
        self.assertFalse(state['claim_allowed'])

if __name__=='__main__':
    unittest.main()
