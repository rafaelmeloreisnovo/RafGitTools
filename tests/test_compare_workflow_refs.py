import unittest
from scripts.compare_workflow_refs import compare


def row(unit='build', target='example/tool/.github/workflows/check.yml', ref='a'*40):
    return {'unit': unit, 'target': target, 'ref': ref}


class ComparatorTests(unittest.TestCase):
    def test_exact(self):
        r = compare([row()], [row()])
        self.assertEqual(r['state'], 'MATCH_SCOPED')
        self.assertFalse(r['claim_allowed'])
        self.assertFalse(r['provider_execution_verified'])
        self.assertFalse(r['automatic_promotion'])

    def test_order_independent(self):
        a, b = row(), row('lint')
        self.assertEqual(compare([a,b], [b,a])['state'], 'MATCH_SCOPED')

    def test_same_target_other_sha(self):
        self.assertEqual(compare([row()], [row(ref='b'*40)])['drift'], ['build'])

    def test_changed_target(self):
        self.assertEqual(compare([row()], [row(target='example/other/.github/workflows/check.yml')])['drift'], ['build'])

    def test_missing_and_extra(self):
        r = compare([row()], [row('lint')])
        self.assertEqual((r['missing'],r['extra']), (['build'],['lint']))
        self.assertEqual(r['state'], 'BLOCKED_REFERENCE_DRIFT')

    def test_empty(self):
        self.assertEqual(compare([row()], [])['state'], 'BLOCKED_INPUT')

    def test_duplicate(self):
        self.assertEqual(compare([row()], [row(),row()])['state'], 'BLOCKED_INPUT')

    def test_mutable(self):
        self.assertEqual(compare([row()], [row(ref='main')])['state'], 'BLOCKED_INPUT')

    def test_unknown(self):
        self.assertEqual(compare([row()], [row(ref=None)])['state'], 'BLOCKED_INPUT')

    def test_extra_fields(self):
        self.assertEqual(compare([row()], [dict(row(), claim_allowed=True)])['state'], 'BLOCKED_INPUT')

    def test_bad_target(self):
        self.assertEqual(compare([row()], [row(target='https://example.org')])['state'], 'BLOCKED_INPUT')

    def test_invalid_expected(self):
        self.assertEqual(compare([row(ref='TOKEN_VAZIO')], [row()])['state'], 'BLOCKED_INPUT')

if __name__ == '__main__':
    unittest.main()
