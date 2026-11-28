#!/usr/bin/env python3
"""Hosted offline comparator for normalized workflow-reference observations.
No network access, YAML parsing, credentials, execution or promotion authority.
The caller binds the source and normalizes its workflow graph to this contract.
"""
import argparse
import json
import re
import sys

SHA = re.compile(r'[0-9a-f]{40}\Z')
TARGET = re.compile(r'[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+/\.github/workflows/[A-Za-z0-9_.-]+\.ya?ml\Z')
UNIT = re.compile(r'[A-Za-z0-9_.-]{1,128}\Z')


def normalize(rows, label):
    if not isinstance(rows, list) or not rows or len(rows) > 10000:
        raise ValueError(label + ': invalid or empty observations')
    result = {}
    for row in rows:
        if not isinstance(row, dict) or set(row) != {'unit', 'target', 'ref'}:
            raise ValueError(label + ': invalid fields')
        unit, target, ref = row['unit'], row['target'], row['ref']
        if not all(isinstance(x, str) for x in (unit, target, ref)):
            raise ValueError(label + ': invalid types')
        if not UNIT.fullmatch(unit) or not TARGET.fullmatch(target) or not SHA.fullmatch(ref):
            raise ValueError(label + ': unknown or non-immutable reference')
        if unit in result:
            raise ValueError(label + ': duplicate unit')
        result[unit] = (target, ref)
    return result


def compare(expected, observed):
    base = {'schema': 'workflow-reference-comparison/v1', 'claim_allowed': False,
            'provider_execution_verified': False, 'automatic_promotion': False}
    try:
        e, o = normalize(expected, 'expected'), normalize(observed, 'observed')
    except ValueError as exc:
        return dict(base, state='BLOCKED_INPUT', error=str(exc))
    missing = sorted(e.keys() - o.keys())
    extra = sorted(o.keys() - e.keys())
    drift = sorted(k for k in e.keys() & o.keys() if e[k] != o[k])
    return dict(base, state='BLOCKED_REFERENCE_DRIFT' if missing or extra or drift else 'MATCH_SCOPED',
                expected_count=len(e), observed_count=len(o), missing=missing, extra=extra, drift=drift)


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('expected')
    p.add_argument('observed')
    args = p.parse_args()
    try:
        def read(path):
            with open(path, encoding='utf-8') as f:
                raw = f.read(4_000_001)
            if len(raw) > 4_000_000:
                raise ValueError('input too large')
            return json.loads(raw)
        result = compare(read(args.expected), read(args.observed))
    except (OSError, ValueError):
        result = {'state': 'BLOCKED_INPUT', 'claim_allowed': False, 'provider_execution_verified': False, 'automatic_promotion': False}
    print(json.dumps(result, sort_keys=True))
    return 0 if result['state'] == 'MATCH_SCOPED' else 2


if __name__ == '__main__':
    sys.exit(main())
