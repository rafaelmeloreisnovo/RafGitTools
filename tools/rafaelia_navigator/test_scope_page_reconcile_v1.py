"""Adversarial synthetic page-boundary tests; no external accounts or data."""
import copy
import hashlib
import json
import tempfile
import unittest
from pathlib import Path

from scope_page_reconcile_v1 import reconcile, write_private
from source_scope_manifest_v1 import SCHEMA as SOURCE_SCHEMA


def page(ordinal, *, end=False, names=('a',), method='drive.files.list', incomplete=False):
    return {
        'schema': SOURCE_SCHEMA, 'provider': 'google_drive', 'scope_id': 'fixture-folder',
        'observed_at': '2026-11-07T10:00:00Z', 'complete': False,
        'page': {'ordinal': ordinal, 'method': method, 'terminal': end,
                 'provider_incomplete': incomplete,
                 'next_token_sha256': None if end else hashlib.sha256(str(ordinal).encode()).hexdigest()},
        'items': [{'id': name, 'name': f'{name}.zip', 'mime_type': 'application/zip',
                   'size': '20000000000', 'parents': ['fixture-folder']} for name in names],
    }


class PageReconcileTests(unittest.TestCase):
    def test_two_pages_end_not_snapshot_proof(self):
        rows, r = reconcile([page(0), page(1, end=True, names=('b',))])
        self.assertEqual(len(rows), 2)
        self.assertEqual(r['coverage'], 'ENUMERATED_UNVERIFIED_SNAPSHOT')
        self.assertFalse(r['snapshot_consistency_verified'])
        self.assertFalse(r['content_verified'])

    def test_independent_lower_bound_prevents_false_complete(self):
        _, r = reconcile([page(0, end=True)], minimum_known_count=100)
        self.assertEqual(r['coverage'], 'PARTIAL_OR_CONFLICTED')
        self.assertIn('BELOW_INDEPENDENTLY_OBSERVED_LOWER_BOUND', r['gaps'])

    def test_invalid_lower_bound_rejected(self):
        with self.assertRaisesRegex(ValueError, 'INVALID_MINIMUM_COUNT'):
            reconcile([page(0, end=True)], minimum_known_count=-1)

    def test_missing_terminal_is_partial(self):
        _, r = reconcile([page(0)])
        self.assertIn('NO_TERMINAL_PAGE', r['gaps'])

    def test_provider_reports_incomplete(self):
        _, r = reconcile([page(0, end=True, incomplete=True)])
        self.assertEqual(r['coverage'], 'PARTIAL_OR_CONFLICTED')

    def test_conflicting_id_retained_and_flagged(self):
        p0, p1 = page(0), page(1, end=True)
        p1['items'][0]['size'] = '9'
        rows, r = reconcile([p0, p1])
        self.assertEqual(rows[0]['size_bytes'], '20000000000')
        self.assertIn('CHANGED_OR_DUPLICATED_ID:1', r['gaps'])

    def test_duplicate_id_flagged(self):
        _, r = reconcile([page(0), page(1, end=True)])
        self.assertIn('REPEATED_ID:1', r['gaps'])

    def test_ordinal_gap_refused(self):
        with self.assertRaisesRegex(ValueError, 'NONCONTIGUOUS_PAGE_ORDINAL'):
            reconcile([page(0), page(2, end=True)])

    def test_token_required(self):
        s = page(0)
        s['page']['next_token_sha256'] = None
        with self.assertRaisesRegex(ValueError, 'NONTERMINAL_TOKEN_DIGEST_REQUIRED'):
            reconcile([s])

    def test_pages_after_terminal_refused(self):
        with self.assertRaisesRegex(ValueError, 'PAGES_AFTER_TERMINAL'):
            reconcile([page(0, end=True), page(1, end=True)])

    def test_method_change_refused(self):
        with self.assertRaisesRegex(ValueError, 'MIXED_SOURCE_OR_METHOD'):
            reconcile([page(0), page(1, end=True, method='drive.folder.fetch')])

    def test_private_output_and_append_only(self):
        with tempfile.TemporaryDirectory() as base:
            src, dst = Path(base) / 'pages', Path(base) / 'private-output'
            src.mkdir()
            for i, p in enumerate([page(0), page(1, end=True, names=('b',))]):
                (src / f'page-{i:03d}.json').write_text(json.dumps(p))
            receipt = write_private(src, dst)
            self.assertEqual(hashlib.sha256((dst / 'INDEX.jsonl').read_bytes()).hexdigest(), receipt['index_sha256'])
            with self.assertRaisesRegex(ValueError, 'OUTPUT_EXISTS_OR_NOT_DISJOINT'):
                write_private(src, dst)

    def test_git_checkout_output_refused(self):
        with tempfile.TemporaryDirectory() as base:
            src = Path(base) / 'pages'; src.mkdir()
            (src / 'page-000.json').write_text(json.dumps(page(0, end=True)))
            (Path(base) / '.git').mkdir()
            with self.assertRaisesRegex(ValueError, 'PRIVATE_OUTPUT_INSIDE_GIT_CHECKOUT'):
                write_private(src, Path(base) / 'sensitive')

    def test_owner_scope_organization(self):
        x = page(0, end=True, names=())
        x['provider'] = 'github_installation'
        x['scope_id'] = 'installation:fixture'
        x['page']['method'] = 'github.installation.list'
        x['account'] = {'login': 'institute', 'type': 'Organization'}
        x['items'] = [{'id': '1', 'full_name': 'institute/private', 'visibility': 'private', 'default_branch': 'main'}]
        rows, _ = reconcile([x])
        self.assertEqual(rows[0]['owner_type'], 'Organization')
        self.assertEqual(rows[0]['visibility'], 'private')


if __name__ == '__main__':
    unittest.main()
