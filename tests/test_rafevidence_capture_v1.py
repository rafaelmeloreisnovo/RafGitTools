import contextlib
import io
import json
import os
import pathlib
import sys
import tempfile
import unittest

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parents[1] / 'scripts'))
import rafevidence_capture_v1 as m

GOOD = {
    "schema": "rafevidence.capture.v1",
    "source_kind": "USER_SCREENSHOT",
    "source_ref": "local:capture",
    "source_sha256": "a" * 64,
    "observed_at": "2026-11-04T19:37:00-03:00",
    "operation": "UI_TOOL_ARGS",
    "capture_state": "UI_OBSERVED",
    "classification": "PRIVATE_METADATA",
    "owner_attested": True,
    "contains_child_data": False,
    "contains_sensitive_data": False,
    "contains_secrets": False,
    "claim_allowed": False,
}


class EvidenceTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.input = os.path.join(self.temp.name, 'request.json')
        self.ledger = os.path.join(self.temp.name, 'ledger.jsonl')

    def request(self, update=None):
        obj = dict(GOOD)
        if update:
            obj.update(update)
        with open(self.input, 'w', encoding='utf-8') as f:
            json.dump(obj, f)
        return obj

    def run_cli(self, commit=False):
        with contextlib.redirect_stdout(io.StringIO()) as out, contextlib.redirect_stderr(io.StringIO()):
            rc = m.main(['--input', self.input] + (['--ledger', self.ledger, '--commit'] if commit else []))
        return rc, out.getvalue()

    def test_dry_run_no_write(self):
        self.request()
        self.assertEqual(self.run_cli()[0], 0)
        self.assertFalse(os.path.exists(self.ledger))

    def test_append_dedupe_private_mode(self):
        self.request()
        self.assertEqual(self.run_cli(True)[0], 0)
        self.assertIn('DUPLICATE_NO_WRITE', self.run_cli(True)[1])
        with open(self.ledger, encoding="utf-8") as f:
            self.assertEqual(sum(1 for _ in f), 1)
        self.assertEqual(os.stat(self.ledger).st_mode & 0o777, 0o600)

    def test_chain_second_record(self):
        self.request()
        self.run_cli(True)
        self.request({'observed_at':'2026-11-04T19:38:00-03:00'})
        self.assertEqual(self.run_cli(True)[0], 0)
        with open(self.ledger) as f:
            lines = f.readlines()
        prev, events = m.verify_chain(lines)
        self.assertEqual(len(events), 2)
        self.assertEqual(prev, json.loads(lines[-1])['record_sha256'])

    def test_tamper_fails_closed(self):
        self.request()
        self.run_cli(True)
        with open(self.ledger, 'r+') as f:
            lines = f.read().replace('UI_OBSERVED', 'NOT_VISIBLE')
            f.seek(0)
            f.write(lines)
            f.truncate()
        self.request({'observed_at':'2026-11-04T19:38:00-03:00'})
        self.assertEqual(self.run_cli(True)[0], 2)

    def test_minor_sensitive_secrets_blocked(self):
        for key in ('contains_child_data', 'contains_sensitive_data', 'contains_secrets'):
            with self.subTest(key=key):
                self.request({key:True})
                self.assertEqual(self.run_cli(True)[0], 2)

    def test_unknown_payload_field_blocked(self):
        self.request({'cookie':'session-token'})
        self.assertEqual(self.run_cli(True)[0], 2)

    def test_duplicate_consent_key_rejected(self):
        self.request()
        with open(self.input, encoding="utf-8") as f:
            payload = f.read()
        self.assertIn('"owner_attested": true', payload)
        payload = payload.replace(
            '"owner_attested": true',
            '"owner_attested": false, "owner_attested": true',
            1,
        )
        with open(self.input, "w", encoding="utf-8") as f:
            f.write(payload)
        self.assertEqual(self.run_cli(True)[0], 2)
        self.assertFalse(os.path.exists(self.ledger))

    def test_duplicate_nested_key_rejected(self):
        self.request()
        with open(self.input, encoding="utf-8") as f:
            payload = f.read()
        payload = payload.replace(
            '"source_sha256":',
            '"extra": {"consent": false, "consent": true}, "source_sha256":',
            1,
        )
        with open(self.input, "w", encoding="utf-8") as f:
            f.write(payload)
        self.assertEqual(self.run_cli(True)[0], 2)
        self.assertFalse(os.path.exists(self.ledger))

    def test_owner_consent_mandatory(self):
        self.request({'owner_attested':False})
        self.assertEqual(self.run_cli(True)[0], 2)

    def test_claim_must_remain_false(self):
        self.request({'claim_allowed':True})
        self.assertEqual(self.run_cli(True)[0], 2)

    def test_bad_ref_blocked(self):
        self.request({'source_ref':'https://example.com?token=secret'})
        self.assertEqual(self.run_cli(True)[0], 2)

    def test_provider_readback_gate(self):
        self.request({'source_kind':'GITHUB_READBACK','source_ref':'github:owner/repo@'+'b'*40})
        self.assertEqual(self.run_cli(True)[0], 2)
        self.request({'source_kind':'GITHUB_READBACK','source_ref':'github:owner/repo@'+'b'*40,'capture_state':'PROVIDER_READBACK'})
        self.assertEqual(self.run_cli(True)[0], 0)

    def test_reject_public_permissions(self):
        self.request()
        pathlib.Path(self.ledger).write_text('')
        os.chmod(self.ledger, 0o644)
        self.assertEqual(self.run_cli(True)[0], 2)

    def test_no_raw_content_in_receipt(self):
        data=self.request()
        saved=m.receipt(data,None)
        self.assertNotIn('raw_capture', saved)
        self.assertFalse(saved['claim_allowed'])
        self.assertIn('ATTESTED_METADATA_ONLY',saved['evidence_scope'])


if __name__ == '__main__':
    unittest.main()
