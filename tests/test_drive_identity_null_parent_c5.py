"""Adversarial synthetic tests for actual Google Drive null-parent metadata cases."""
import copy
import importlib.util
from pathlib import Path
import unittest

spec=importlib.util.spec_from_file_location("reconcile", Path(__file__).resolve().parents[1]/"scripts"/"drive_identity_partition_reconcile_v1.py")
core=importlib.util.module_from_spec(spec)
spec.loader.exec_module(core)


def fixture():
    return {
        "schema":core.SCHEMA,"provider":"google_drive",
        "account_id":"123456789012345678901",
        "query_scope":"mimeType=folder;trashed=false", "expected_positions":2,
        "pages":[
            {"page_index":0,"incomplete_search":False,"next_page_present":True,
             "observed_at":"2026-11-21T15:31:31Z","items":[{"id":"1abcdefabcdefabcdef1234",
             "modified_time":"2026-11-21T15:00:00Z","parent_ids":None}]},
            {"page_index":1,"incomplete_search":False,"next_page_present":False,
             "observed_at":"2026-11-21T15:31:35Z","items":[{"id":"1bbbbbbbbbbbbbbbbbbbbb",
             "modified_time":"2026-11-21T15:00:00Z","parent_ids":[]}]}
        ]
    }

class C5NullParentTests(unittest.TestCase):
    def test_null_parent_admitted_with_scoped_hash(self):
        x=core.capture(fixture())
        self.assertEqual(x['unique_identities'],2)
        self.assertEqual(x['parent_explicit_null_positions'],1)
        self.assertEqual(x['parent_field_absent_positions'],0)
        self.assertNotEqual(x['metadata_manifest_sha256'],'TOKEN_VAZIO')
        self.assertFalse(x['claim_allowed'])

    def test_consistent_null_parent_two_snapshots(self):
        a=fixture();b=copy.deepcopy(a)
        out=core.reconcile(a,b)
        self.assertTrue(out['same_identity_set'])
        self.assertTrue(out['same_metadata_if_comparable'])
        self.assertEqual(out['b0160_promotion'],'BLOCKED')

    def test_null_vs_empty_array_is_not_equal(self):
        a=fixture();b=copy.deepcopy(a)
        b['pages'][0]['items'][0]['parent_ids']=[]
        out=core.reconcile(a,b)
        self.assertTrue(out['same_identity_set'])
        self.assertFalse(out['same_metadata_if_comparable'])
        self.assertNotEqual(out['capture_a']['metadata_manifest_sha256'],out['capture_b']['metadata_manifest_sha256'])

    def test_null_vs_present_parent_is_not_equal(self):
        a=fixture();b=copy.deepcopy(a)
        b['pages'][0]['items'][0]['parent_ids']=['1valid_parent_abcdef01234']
        self.assertFalse(core.reconcile(a,b)['same_metadata_if_comparable'])

    def test_missing_parent_is_token_vazio_not_explicit_null(self):
        x=fixture();del x['pages'][0]['items'][0]['parent_ids']
        a=core.capture(x)
        self.assertEqual(a['parent_field_absent_positions'],1)
        self.assertEqual(a['parent_explicit_null_positions'],0)
        self.assertEqual(a['metadata_manifest_sha256'],'TOKEN_VAZIO')

    def test_invalid_parent_type_still_rejected(self):
        x=fixture();x['pages'][0]['items'][0]['parent_ids']='null'
        with self.assertRaisesRegex(core.GateError,'PARENT_IDS_INVALID'):
            core.capture(x)

    def test_nonterminal_last_page_still_blocked(self):
        x=fixture();x['pages'][-1]['next_page_present']=True
        with self.assertRaisesRegex(core.GateError,'CURSOR_CHAIN_INCOMPLETE'):
            core.capture(x)

    def test_missing_provider_completeness_still_blocked(self):
        x=fixture();del x['pages'][0]['incomplete_search']
        with self.assertRaisesRegex(core.GateError,'PROVIDER_COMPLETENESS_UNVERIFIED'):
            core.capture(x)

    def test_content_is_unproven(self):
        x=core.reconcile(fixture(),fixture())
        self.assertEqual(x['capture_a']['source_content_sha256'],'TOKEN_VAZIO')
        self.assertFalse(x['claim_allowed'])
        self.assertEqual(x['global_drive_completeness'],'TOKEN_VAZIO')

if __name__=='__main__':unittest.main()
