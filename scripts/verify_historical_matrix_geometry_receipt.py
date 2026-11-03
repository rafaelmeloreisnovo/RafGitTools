#!/usr/bin/env python3
"""RafGitTools read-only verifier of RafPolimata 8x3x2 geometry receipts.

No network, shell, source-copy, secrets, execution delegation or claim promotion.
The producer owns matrix semantics and its compiler; this is a consumer gate.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import math
from pathlib import Path

EXPECTED_TOPOLOGY = {
    "positions": 8, "layers": 3, "oppositions": 2, "basis": "cube_parity_tetra"
}
REQUIRED = (
    "matrix.output.json", "compose.output.hex", "ifdex.v1.json",
    "receipt.v1.json", "geometry.output.v1.json", "geometry.receipt.v1.json"
)
MAX_INPUT_CELLS = 65536


def canonical(obj):
    return json.dumps(obj, sort_keys=True, separators=(",", ":"),
                      ensure_ascii=False).encode("utf-8")


def digest(data):
    return hashlib.sha256(data).hexdigest()


def verify(directory: Path) -> list[str]:
    errors = []
    try:
        files = {name: directory.joinpath(name).read_bytes() for name in REQUIRED}
    except (OSError, ValueError) as exc:
        return [f"missing or unreadable artifact: {type(exc).__name__}"]

    try:
        matrix = json.loads(files["matrix.output.json"])
        ifdex = json.loads(files["ifdex.v1.json"])
        receipt = json.loads(files["receipt.v1.json"])
        geometry = json.loads(files["geometry.output.v1.json"])
        geo_receipt = json.loads(files["geometry.receipt.v1.json"])
        packed = bytes.fromhex(files["compose.output.hex"].decode("ascii").strip())
    except (UnicodeError, ValueError, TypeError) as exc:
        return [f"malformed artifact: {type(exc).__name__}"]

    if not all(isinstance(x, dict) for x in
               (ifdex, receipt, geometry, geo_receipt)):
        return ["object artifacts must be JSON objects"]

    expected = {"ifdex": "rafaelia.ifdex.v1",
                "receipt": "rafaelia.matrix-compose.receipt.v1",
                "geometry": "rafaelia.historical-matrix-geometry.output.v1",
                "geo_receipt": "rafaelia.historical-matrix-geometry.receipt.v1"}
    for key, obj in (("ifdex", ifdex), ("receipt", receipt),
                     ("geometry", geometry), ("geo_receipt", geo_receipt)):
        if obj.get("schema") != expected[key]:
            errors.append(f"{key} schema mismatch")
    if receipt.get("claim_allowed") is not False or \
       geometry.get("claim_allowed") is not False or \
       geo_receipt.get("claim_allowed") is not False:
        errors.append("claim_allowed must be false in every artifact")

    output = ifdex.get("output")
    if not isinstance(output, dict):
        return errors + ["IFDex missing output digest"]
    matrix_sha = digest(canonical(matrix))
    packed_sha = digest(packed)
    if matrix_sha != output.get("matrix_sha256") or \
       matrix_sha != geo_receipt.get("matrix_sha256"):
        errors.append("matrix digest mismatch")
    if packed_sha != output.get("packed_cell10_sha256") or \
       packed_sha != receipt.get("packed_cell10_sha256") or \
       packed_sha != geo_receipt.get("packed_cell10_sha256"):
        errors.append("packed cell10 digest mismatch")
    if receipt.get("ifdex_sha256") != digest(canonical(ifdex)):
        errors.append("base receipt IFDex digest mismatch")
    if geometry.get("ifdex_sha256") != digest(canonical(ifdex)):
        errors.append("geometry IFDex digest mismatch")
    if geo_receipt.get("geometry_sha256") != digest(canonical(geometry)):
        errors.append("geometry receipt digest mismatch")
    if geometry.get("central_matrix") != matrix:
        errors.append("central matrix mismatch")
    if geometry.get("topology") != EXPECTED_TOPOLOGY:
        errors.append("geometry topology mismatch")
    if geometry.get("projection_is_reversible") is not False or \
       geometry.get("reconstruction_requires_original_48_cells") is not True:
        errors.append("false reconstruction claim")
    if geometry.get("quantum_or_spacetime_model") != "TOKEN_VAZIO_NOT_DERIVED":
        errors.append("physical claim boundary missing")
    if geometry.get("q16_scales") != [
        65536, 56756, (56756 * 56756 + 32768) >> 16
    ]:
        errors.append("Q16 scale mismatch")

    slots = geometry.get("canonical_slots")
    layers = ifdex.get("layers")
    if not isinstance(slots, list) or len(slots) != 48 or \
       not isinstance(layers, list) or len(layers) != 48:
        return errors + ["48 slots and 48 IFDex input layers are mandatory"]
    mirror = geometry.get("mirror_axis")
    if mirror != "TOKEN_VAZIO_NOT_REQUESTED" and \
       (type(mirror) is not int or not 0 <= mirror <= 2):
        errors.append("invalid mirror axis")
        mirror = "TOKEN_VAZIO_NOT_REQUESTED"
    for i, (slot, layer) in enumerate(zip(slots, layers)):
        if not isinstance(slot, dict) or not isinstance(layer, dict):
            errors.append(f"malformed layer {i}")
            continue
        if slot.get("slot") != i or slot.get("position") != i // 6 or \
           slot.get("layer") != (i // 2) % 3 or slot.get("opposition") != i % 2:
            errors.append(f"slot order or identity mismatch {i}")
        expected_source = (i // 6) ^ (
            (1 << mirror) if type(mirror) is int else 0
        )
        if slot.get("source_position") != expected_source:
            errors.append(f"mirror provenance mismatch {i}")
        if layer.get("id") != f"G{i:02d}":
            errors.append(f"IFDex identity mismatch {i}")

    shape = ifdex.get("shape")
    if not isinstance(shape, list) or not shape or \
       len(shape) > 7 or any(type(n) is not int or n < 1 for n in shape):
        return errors + ["invalid matrix shape"]
    count = math.prod(shape)
    if count * 48 > MAX_INPUT_CELLS:
        errors.append("bounded materialization budget exceeded")
    if ifdex.get("cell_count") != count or \
       geo_receipt.get("cells_per_slot") != count or \
       geo_receipt.get("total_input_cells") != count * 48 or \
       geo_receipt.get("slots") != 48:
        errors.append("cell count mismatch")
    for i, layer in enumerate(layers):
        if isinstance(layer, dict) and layer.get("length") != count:
            errors.append(f"IFDex extent mismatch {i}")
    if len(packed) != ((count * 10 + 7) // 8):
        errors.append("packed 10-bit extent mismatch")

    corners = geometry.get("corners")
    if not isinstance(corners, list) or len(corners) != 8:
        errors.append("eight exact corners mandatory")
    else:
        for p, item in enumerate(corners):
            if not isinstance(item, dict) or item != {
                "position": p,
                "xyz": [-1 if p & (1 << axis) else 1 for axis in range(3)],
                "parity": (p & 1) ^ ((p >> 1) & 1) ^ ((p >> 2) & 1),
            }:
                errors.append(f"corner or parity mismatch {p}")
    for key, amount in (("by_opposition", 2), ("by_layer", 3)):
        parts = geometry.get(key)
        if not isinstance(parts, list) or len(parts) != amount:
            errors.append(f"{key} projection count mismatch")
        else:
            for i, item in enumerate(parts):
                if not isinstance(item, dict) or item.get(
                    "opposition" if key == "by_opposition" else "layer"
                ) != i or item.get("sha256") != digest(canonical(item.get("matrix"))):
                    errors.append(f"{key} projection digest mismatch {i}")

    # Validate actual packed 10-bit cells, not only caller-supplied hashes.
    def read_flat(node, dims):
        if not dims:
            if type(node) is not int or not 0 <= node <= 255:
                raise ValueError("invalid uint8 cell")
            return [node]
        if not isinstance(node, list) or len(node) != dims[0]:
            raise ValueError("invalid nested matrix extent")
        result = []
        for child in node:
            result.extend(read_flat(child, dims[1:]))
        return result

    try:
        expected_bytes = read_flat(matrix, shape)
        bits = "".join(f"{value:08b}" for value in packed)
        if any(bit == "1" for bit in bits[count * 10:]):
            errors.append("nonzero packed padding")
        recovered = []
        for i in range(count):
            cell = int(bits[10 * i:10 * i + 10], 2)
            payload = cell & 127
            ext = (cell >> 9) & 1
            parity0 = ext
            parity1 = ext
            for bit in (0, 2, 4, 6):
                parity0 ^= (payload >> bit) & 1
            for bit in (1, 3, 5):
                parity1 ^= (payload >> bit) & 1
            if ((cell >> 7) & 1) != parity0 or ((cell >> 8) & 1) != parity1:
                errors.append(f"invalid 10-bit parity at cell {i}")
            recovered.append(payload | (ext << 7))
        if recovered != expected_bytes:
            errors.append("packed content differs from logical matrix")
    except (ValueError, TypeError):
        errors.append("invalid matrix shape or packed cell stream")

    return errors


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--dir", required=True, type=Path)
    args = parser.parse_args()
    errors = verify(args.dir)
    for error in errors:
        print("FAIL:", error)
    if errors:
        return 1
    print("PASS_SCOPED historical matrix geometry receipt structure and digests")
    print("TOKEN_VAZIO: producer CI, ARM execution, Android device and physics")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
