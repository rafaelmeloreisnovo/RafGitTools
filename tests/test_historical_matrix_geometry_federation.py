"""Synthetic no-network consumer tests; producer/runtime claims remain blocked."""
import json
import tempfile
import unittest
from pathlib import Path

from scripts.verify_historical_matrix_geometry_receipt import (
    EXPECTED_TOPOLOGY, canonical, digest, verify,
)


def cell10(byte):
    payload = byte & 127
    ext = (byte >> 7) & 1
    p0 = ext
    p1 = ext
    for bit in (0, 2, 4, 6):
        p0 ^= (payload >> bit) & 1
    for bit in (1, 3, 5):
        p1 ^= (payload >> bit) & 1
    return payload | (p0 << 7) | (p1 << 8) | (ext << 9)


def packed_bytes(values):
    bits = "".join(f"{cell10(b):010b}" for b in values)
    bits += "0" * ((-len(bits)) % 8)
    return bytes(int(bits[i:i + 8], 2) for i in range(0, len(bits), 8))


def write_fixture(folder):
    matrix = [[1, 130]]
    packed = packed_bytes([1, 130])
    by_context = []
    for o, partial in enumerate(([[0, 0]], matrix)):
        by_context.append({
            "opposition": o, "matrix": partial, "sha256": digest(canonical(partial))
        })
    by_layer = []
    for l, partial in enumerate(([[0, 0]], [[0, 0]], matrix)):
        by_layer.append({
            "layer": l, "matrix": partial, "sha256": digest(canonical(partial))
        })
    ifdex = {
        "schema": "rafaelia.ifdex.v1",
        "shape": [1, 2], "cell_count": 2, "operator": "xor",
        "layers": [{"id": f"G{i:02d}", "length": 2,
                    "source_sha256": "0" * 64} for i in range(48)],
        "output": {"matrix_sha256": digest(canonical(matrix)),
                   "packed_cell10_sha256": digest(packed)},
    }
    receipt = {
        "schema": "rafaelia.matrix-compose.receipt.v1",
        "claim_allowed": False,
        "ifdex_sha256": digest(canonical(ifdex)),
        "packed_cell10_sha256": digest(packed),
    }
    geometry = {
        "schema": "rafaelia.historical-matrix-geometry.output.v1",
        "topology": dict(EXPECTED_TOPOLOGY),
        "mirror_axis": "TOKEN_VAZIO_NOT_REQUESTED",
        "canonical_slots": [
            {"slot": i, "position": i // 6, "layer": (i // 2) % 3,
             "opposition": i % 2, "source_position": i // 6} for i in range(48)
        ],
        "corners": [{
            "position": p,
            "xyz": [-1 if (p >> b) & 1 else 1 for b in range(3)],
            "parity": (p & 1) ^ ((p >> 1) & 1) ^ ((p >> 2) & 1),
        } for p in range(8)],
        "q16_scales": [65536, 56756, (56756 * 56756 + 32768) >> 16],
        "central_matrix": matrix,
        "by_opposition": by_context,
        "by_layer": by_layer,
        "ifdex_sha256": digest(canonical(ifdex)),
        "projection_is_reversible": False,
        "reconstruction_requires_original_48_cells": True,
        "quantum_or_spacetime_model": "TOKEN_VAZIO_NOT_DERIVED",
        "claim_allowed": False,
    }
    gr = {
        "schema": "rafaelia.historical-matrix-geometry.receipt.v1",
        "slots": 48, "cells_per_slot": 2, "total_input_cells": 96,
        "matrix_sha256": digest(canonical(matrix)),
        "packed_cell10_sha256": digest(packed),
        "geometry_sha256": digest(canonical(geometry)),
        "claim_allowed": False,
    }
    blobs = {
        "matrix.output.json": canonical(matrix) + b"\n",
        "compose.output.hex": (packed.hex() + "\n").encode("ascii"),
        "ifdex.v1.json": canonical(ifdex) + b"\n",
        "receipt.v1.json": canonical(receipt) + b"\n",
        "geometry.output.v1.json": canonical(geometry) + b"\n",
        "geometry.receipt.v1.json": canonical(gr) + b"\n",
    }
    for name, data in blobs.items():
        (folder / name).write_bytes(data)
    return blobs


class HistoricalGeometryFederationTests(unittest.TestCase):
    def test_valid_scoped_receipt(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp)
            write_fixture(path)
            self.assertEqual(verify(path), [])

    def test_negative_corruption_and_claim(self):
        for name, updated in (
            ("matrix.output.json", b"[[99,130]]\n"),
            ("compose.output.hex", b"ffff\n"),
            ("geometry.output.v1.json", None),
            ("receipt.v1.json", None),
            ("ifdex.v1.json", None),
        ):
            with self.subTest(name=name), tempfile.TemporaryDirectory() as tmp:
                path = Path(tmp)
                blobs = write_fixture(path)
                if updated is None:
                    obj = json.loads(blobs[name])
                    if name == "geometry.output.v1.json":
                        obj["canonical_slots"][1] = obj["canonical_slots"][0]
                    elif name == "receipt.v1.json":
                        obj["claim_allowed"] = True
                    else:
                        obj["layers"].pop()
                    updated = canonical(obj) + b"\n"
                (path / name).write_bytes(updated)
                self.assertTrue(verify(path))
                updated = None

    def test_unknown_missing_and_bad_parity(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp)
            blobs = write_fixture(path)
            (path / "geometry.receipt.v1.json").unlink()
            self.assertTrue(verify(path))
            write_fixture(path)
            packed = bytes.fromhex(blobs["compose.output.hex"].decode().strip())
            damage = bytes([packed[0] ^ 0x10]) + packed[1:]
            (path / "compose.output.hex").write_text(damage.hex() + "\n")
            self.assertTrue(verify(path))


if __name__ == "__main__":
    unittest.main()
