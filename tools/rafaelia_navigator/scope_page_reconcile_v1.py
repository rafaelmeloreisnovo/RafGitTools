#!/usr/bin/env python3
"""Reconcile *private, pre-authorized* provider metadata pages without network I/O.

No ZIP bytes, OAuth tokens, GitHub API calls, deletes, copies, or uploads.
A terminal page proves only observed traversal, never a consistent snapshot.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
from pathlib import Path

from source_scope_manifest_v1 import MAX_INPUT_BYTES, normalize

SCHEMA = 'rafgittools.hyperatlas.page-reconcile/v1'
HEX64 = re.compile(r'^[0-9a-f]{64}$')
MAX_PAGES = 128
MAX_ROWS = 20000
METHODS = {'drive.files.list', 'drive.folder.fetch', 'github.installation.list'}


def load_pages(input_dir: Path) -> list[dict]:
    if input_dir.is_symlink() or not input_dir.is_dir():
        raise ValueError('INPUT_DIRECTORY_REQUIRED')
    files = sorted(input_dir.glob('page-*.json'))
    if not files or len(files) > MAX_PAGES:
        raise ValueError('PAGE_COUNT_BOUND')
    pages = []
    for path in files:
        if path.is_symlink() or not path.is_file() or path.stat().st_size > MAX_INPUT_BYTES:
            raise ValueError('INVALID_PAGE_FILE')
        with path.open('r', encoding='utf-8') as fh:
            pages.append(json.load(fh))
    return pages


def reconcile(pages: list[dict]) -> tuple[list[dict], dict]:
    if not pages or len(pages) > MAX_PAGES:
        raise ValueError('PAGE_COUNT_BOUND')
    baseline = None
    rows_by_id: dict[str, dict] = {}
    gaps: list[str] = []
    terminal_seen = False
    rows_observed = 0
    for ordinal, snapshot in enumerate(pages):
        meta = snapshot.get('page') if isinstance(snapshot, dict) else None
        if not isinstance(meta, dict) or meta.get('ordinal') != ordinal or type(meta.get('ordinal')) is not int:
            raise ValueError('NONCONTIGUOUS_PAGE_ORDINAL')
        method = meta.get('method')
        if method not in METHODS:
            raise ValueError('METHOD_UNKNOWN')
        expected_provider = 'google_drive' if method.startswith('drive.') else 'github_installation'
        if snapshot.get('provider') != expected_provider:
            raise ValueError('METHOD_PROVIDER_MISMATCH')
        token_digest = meta.get('next_token_sha256')
        if type(meta.get('terminal')) is not bool or type(meta.get('provider_incomplete')) is not bool:
            raise ValueError('PAGE_FLAGS_REQUIRED')
        if meta['terminal']:
            if token_digest is not None:
                raise ValueError('TERMINAL_WITH_TOKEN')
            if ordinal != len(pages) - 1:
                raise ValueError('PAGES_AFTER_TERMINAL')
            terminal_seen = True
        elif not isinstance(token_digest, str) or not HEX64.fullmatch(token_digest):
            raise ValueError('NONTERMINAL_TOKEN_DIGEST_REQUIRED')
        if meta['provider_incomplete']:
            gaps.append(f'PROVIDER_INCOMPLETE_PAGE:{ordinal}')
        identity = (snapshot.get('provider'), snapshot.get('scope_id'), method,
                    json.dumps(snapshot.get('account'), sort_keys=True))
        if baseline is None:
            baseline = identity
        elif identity != baseline:
            raise ValueError('MIXED_SOURCE_OR_METHOD')
        # normalize enforces private metadata fields, explicit owner type and bounds.
        per_page = dict(snapshot)
        per_page['complete'] = False
        rows, _ = normalize(per_page)
        rows_observed += len(rows)
        if rows_observed > MAX_ROWS:
            raise ValueError('ROW_COUNT_BOUND')
        for row in rows:
            old = rows_by_id.get(row['id'])
            if old is None:
                rows_by_id[row['id']] = row
            elif old != row:
                gaps.append('CHANGED_OR_DUPLICATED_ID:' + str(ordinal))
            else:
                gaps.append('REPEATED_ID:' + str(ordinal))
    if not terminal_seen:
        gaps.append('NO_TERMINAL_PAGE')
    rows = sorted(rows_by_id.values(), key=lambda r: (r['provider'], r['id']))
    receipt = {
        'schema': SCHEMA, 'source_provider': baseline[0], 'scope_id': baseline[1],
        'method': baseline[2], 'pages_observed': len(pages),
        'entries_observed': rows_observed, 'unique_entries': len(rows),
        'terminal_page_observed': terminal_seen,
        'coverage': 'PARTIAL_OR_CONFLICTED' if gaps else 'ENUMERATED_UNVERIFIED_SNAPSHOT',
        'gaps': sorted(set(gaps)), 'source_bytes_read': False,
        'content_verified': False, 'snapshot_consistency_verified': False,
        'claim_allowed': False,
    }
    return rows, receipt


def write_private(input_dir: Path, output: Path) -> dict:
    src = input_dir.resolve(strict=True)
    dst = output.resolve()
    if dst.exists() or dst == src or dst in src.parents or src in dst.parents:
        raise ValueError('OUTPUT_EXISTS_OR_NOT_DISJOINT')
    # Refuse to put sensitive metadata in a tracked Git working tree.
    if any((p / '.git').exists() for p in [dst, *dst.parents]):
        raise ValueError('PRIVATE_OUTPUT_INSIDE_GIT_CHECKOUT')
    rows, receipt = reconcile(load_pages(src))
    data = ''.join(json.dumps(r, sort_keys=True, separators=(',', ':'), ensure_ascii=False) + '\n' for r in rows).encode('utf-8')
    receipt['index_sha256'] = hashlib.sha256(data).hexdigest()
    dst.mkdir(mode=0o700, parents=True, exist_ok=False)
    os.chmod(dst, 0o700)
    with (dst / 'INDEX.jsonl').open('xb') as fh:
        fh.write(data)
    with (dst / 'RECEIPT.json').open('x', encoding='utf-8') as fh:
        json.dump(receipt, fh, sort_keys=True, indent=2)
        fh.write('\n')
    os.chmod(dst / 'INDEX.jsonl', 0o600)
    os.chmod(dst / 'RECEIPT.json', 0o600)
    return receipt


def main() -> None:
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--input-pages', type=Path, required=True)
    p.add_argument('--output', type=Path, required=True)
    args = p.parse_args()
    r = write_private(args.input_pages, args.output)
    print(json.dumps({k: r[k] for k in ('coverage', 'pages_observed', 'unique_entries', 'claim_allowed')}))


if __name__ == '__main__':
    main()
