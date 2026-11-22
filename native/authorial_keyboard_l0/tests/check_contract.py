#!/usr/bin/env python3
"""Hosted, non-product verifier of RAFAELIA L0 object/table contract.

This verifier is not part of the executable keyboard core.
"""
import pathlib
import re
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
LAYOUT = ROOT / 'src' / 'layout.inc'


def table_bytes():
    text = LAYOUT.read_text(encoding='utf-8')
    rows = re.findall(r'^\.ascii "([^"]*)"$', text, re.M)
    assert len(rows) == 4 and all(len(row) == 10 for row in rows), 'layout rows'
    bytes_line = re.search(r'^\.byte (.+)$', text, re.M)
    assert bytes_line, 'fifth row'
    last = bytes(int(p.strip(), 0) for p in bytes_line.group(1).split(','))
    assert len(last) == 10, 'control row'
    return ''.join(rows).encode('ascii') + last


def section_bytes(obj):
    output = subprocess.check_output(['readelf', '-x', '.text', str(obj)], text=True)
    blob = bytearray()
    for line in output.splitlines():
        cols = line.strip().split()
        if not cols or not cols[0].startswith('0x'):
            continue
        for col in cols[1:]:
            if not re.fullmatch('[0-9a-fA-F]{2,8}', col) or len(col) % 2:
                break
            blob.extend(bytes.fromhex(col))
    return bytes(blob)


def probe_reference(x, y, layout):
    # This reference checks the authored contract, not native CPU execution.
    if not (0 <= x < 320 and 0 <= y < 160):
        return 0xffffffff
    return layout[(y >> 5) * 10 + (x >> 5)]


def main():
    layout = table_bytes()
    assert len(layout) == 50
    assert layout[40] == 8 and layout[41] == 9 and layout[46] == 10
    checks = 0
    for row in range(5):
        for col in range(10):
            expected = layout[row * 10 + col]
            for dx, dy in ((0, 0), (15, 15), (31, 31)):
                assert probe_reference(col * 32 + dx, row * 32 + dy, layout) == expected
                checks += 1
    for x, y in ((320, 0), (0, 160), (320, 160), (0xffffffff, 2), (2, 0xffffffff), (-1, 0)):
        assert probe_reference(x, y, layout) == 0xffffffff
        checks += 1
    for obj in map(pathlib.Path, sys.argv[1:]):
        assert section_bytes(obj).endswith(layout), 'compiled table bytes mismatch'
        assert not subprocess.check_output(['nm', '-u', str(obj)]).strip(), 'external symbols'
        assert 'There are no relocations' in subprocess.check_output(['readelf', '-r', str(obj)], text=True), 'relocations'
        checks += 3
    assert len(sys.argv) == 3, 'expected two target objects'
    print(f'PASS: {checks} scoped checks; 50 key bytes identical in both objects; undefined symbols=0; relocations=0')
    print('NOTE: HOST_REFERENCE_ONLY / ISA_ASSEMBLED; physical Android/touch/display NOT_RUN')


if __name__ == '__main__':
    main()
