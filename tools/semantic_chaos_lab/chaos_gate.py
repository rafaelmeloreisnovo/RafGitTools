#!/usr/bin/env python3
"""RAFAELIA Ω semantic-chaos capture V1. Hosted Python; NOT freestanding.
Synthetic-only, reversible bytes, fixed PRNG, fail-closed, no network.
Usage: python3 chaos_gate.py selftest
       python3 chaos_gate.py build INPUT.jsonl NEW_OUT_DIR --seed 42
       python3 chaos_gate.py restore OUT_DIR/chaos.jsonl OUT_DIR/order.json NEW_OUTPUT.jsonl
"""
import argparse
import hashlib
import json
import math
import pathlib
import tempfile
import zlib

MAX_SOURCE = 1 << 20
MAX_LINES = 4096
KINDS = set("""EMPTY_GLYPH EMPTY_STRING SPACE_ONLY NUL_BYTE JSON_NULL
MISSING_FIELD ZERO_NUMBER RAW_TEXT UNKNOWN NOISE ABORTED REVERSE INVERSE
IGNORED UNTOKENIZED CENSORED ANTIDERIVED ANTITENSOR HEURISTIC INVARIANT
UNWEIGHED UNARY ZERO_ORDER RECLUSE INDETERMINATE DETERMINATE""".split())

def sha(data):
    return hashlib.sha256(data).hexdigest()

def canonical(obj):
    return (json.dumps(obj, ensure_ascii=False, sort_keys=True,
                       separators=(",", ":")) + "\n").encode("utf-8")

def entropy(data):
    if not data:
        return 0.0
    counts = [0] * 256
    for byte in data:
        counts[byte] += 1
    return -sum((n/len(data)) * math.log2(n/len(data))
                for n in counts if n)

def validate(data):
    if not data or len(data) > MAX_SOURCE or not data.endswith(b"\n"):
        raise ValueError("SOURCE_BOUNDS_OR_FINAL_NEWLINE")
    lines = data.splitlines(keepends=True)
    if len(lines) > MAX_LINES:
        raise ValueError("TOO_MANY_LINES")
    seen = set()
    for i, raw in enumerate(lines):
        obj = json.loads(raw.decode("utf-8", errors="strict"))
        if not isinstance(obj, dict):
            raise ValueError("NOT_OBJECT")
        if set(obj) - {"id", "kind", "payload", "source", "privacy_class", "language_hint", "note"}:
            raise ValueError("UNKNOWN_FIELD")
        if obj.get("source") != "CREATED_SYNTHETIC" or obj.get("privacy_class") != "SYNTHETIC":
            raise ValueError("PRIVACY_SOURCE_FAIL_CLOSED")
        ident, kind = obj.get("id"), obj.get("kind")
        if not isinstance(ident, str) or not ident or ident in seen or kind not in KINDS:
            raise ValueError("BAD_ID_OR_KIND")
        seen.add(ident)
        present = "payload" in obj
        v = obj.get("payload")
        if kind == "MISSING_FIELD":
            ok = not present
        elif kind == "JSON_NULL":
            ok = present and v is None
        elif kind == "ZERO_NUMBER":
            ok = present and type(v) is int and v == 0
        elif kind == "EMPTY_STRING":
            ok = present and v == "" and type(v) is str
        elif kind == "SPACE_ONLY":
            ok = present and type(v) is str and len(v) > 0 and all(c == " " for c in v)
        elif kind == "NUL_BYTE":
            ok = present and v == "\x00"
        elif kind == "EMPTY_GLYPH":
            ok = present and v == "∅"
        else:
            ok = present and type(v) is str and len(v) > 0
        if not ok:
            raise ValueError("TYPED_TOKEN_COLLISION_AT_LINE_" + str(i+1))
    return lines

def shuffled_indices(count, seed):
    if not 1 <= seed <= 0xffffffff:
        raise ValueError("SEED_RANGE")
    order = list(range(count))
    s = seed
    for i in range(count-1, 0, -1):
        s ^= (s << 13) & 0xffffffff
        s ^= s >> 17
        s ^= (s << 5) & 0xffffffff
        s &= 0xffffffff
        j = s % (i+1)
        order[i], order[j] = order[j], order[i]
    return order

def build(input_path, out_dir, seed):
    data = pathlib.Path(input_path).read_bytes()
    lines = validate(data)
    order = shuffled_indices(len(lines), seed)
    scrambled = b"".join(lines[i] for i in order)
    mapping = {"schema": "RAFAELIA-CHAOS-ORDER-V1", "seed": seed, "count": len(lines),
               "order": order, "source_sha256": sha(data),
               "chaos_sha256": sha(scrambled)}
    restored = b"".join(x for _, x in sorted(zip(order, [lines[i] for i in order])))
    if restored != data:
        raise AssertionError("ROUNDTRIP_FAIL")
    receipt = {
        "schema": "RAFAELIA-SEMCHAOS-RECEIPT-V1", "source_kind": "SYNTHETIC",
        "source_sha256": sha(data), "chaos_sha256": sha(scrambled),
        "restored_sha256": sha(restored), "order_sha256": sha(canonical(mapping)),
        "byte_length": len(data), "records": len(lines), "seed": seed,
        "source_entropy_bits_per_byte": round(entropy(data), 8),
        "chaos_entropy_bits_per_byte": round(entropy(scrambled), 8),
        "source_zlib9_bytes": len(zlib.compress(data, 9)),
        "chaos_zlib9_bytes": len(zlib.compress(scrambled, 9)),
        "roundtrip_byte_exact": True, "claim_allowed": False,
        "real_model_training": "NOT_RUN", "semantic_compression": "UNMEASURED",
        "security_proof": "NOT_RUN", "state": "EXPERIMENTAL_SOURCE_CAPTURE"
    }
    dst = pathlib.Path(out_dir)
    dst.mkdir(parents=False, exist_ok=False)
    for name, contents in [
        ("chaos.jsonl", scrambled), ("order.json", canonical(mapping)),
        ("receipt.json", canonical(receipt))
    ]:
        with (dst/name).open("xb") as f:
            f.write(contents)
    return receipt

def restore(chaos_path, order_path, output):
    data = pathlib.Path(chaos_path).read_bytes()
    mapping = json.loads(pathlib.Path(order_path).read_text(encoding="utf-8"))
    if mapping.get("schema") != "RAFAELIA-CHAOS-ORDER-V1":
        raise ValueError("WRONG_SCHEMA")
    if sha(data) != mapping.get("chaos_sha256"):
        raise ValueError("CHAOS_HASH_MISMATCH")
    lines = validate(data)
    order = mapping["order"]
    if type(order) is not list or len(order) != len(lines) or sorted(order) != list(range(len(lines))):
        raise ValueError("INVALID_ORDER")
    restored = b"".join(x for _, x in sorted(zip(order, lines)))
    if sha(restored) != mapping.get("source_sha256"):
        raise ValueError("RESTORED_HASH_MISMATCH")
    with pathlib.Path(output).open("xb") as f:
        f.write(restored)
    return sha(restored)

def selftest():
    samples = [
        ("EMPTY_GLYPH", "∅"), ("EMPTY_STRING", ""), ("SPACE_ONLY", "    "),
        ("NUL_BYTE", "\x00"), ("JSON_NULL", None), ("MISSING_FIELD", None),
        ("ZERO_NUMBER", 0), ("RAW_TEXT", "molho 🍅 龍 ش ש あ ก λ"),
        ("ABORTED", "token_abortado"), ("REVERSE", "origem_sem_origem"),
        ("ANTIDERIVED", "∫Ω⁻¹"), ("NOISE", "[ruído][verdade?][ruído]"),
    ]
    records = []
    for i, (kind, payload) in enumerate(samples):
        obj = {"id": "T" + str(i).zfill(3), "kind": kind,
               "source": "CREATED_SYNTHETIC", "privacy_class": "SYNTHETIC"}
        if kind != "MISSING_FIELD":
            obj["payload"] = payload
        records.append(obj)
    original = b"".join(canonical(x) for x in records)
    assert len(validate(original)) == len(samples)
    with tempfile.TemporaryDirectory() as d:
        root = pathlib.Path(d)
        src = root/"source.jsonl"
        src.write_bytes(original)
        receipt = build(src, root/"run1", 42)
        build(src, root/"run2", 42)
        assert (root/"run1/chaos.jsonl").read_bytes() == (root/"run2/chaos.jsonl").read_bytes()
        assert receipt["source_sha256"] == restore(root/"run1/chaos.jsonl", root/"run1/order.json", root/"restored.jsonl")
        assert (root/"restored.jsonl").read_bytes() == original
        assert receipt["source_entropy_bits_per_byte"] == receipt["chaos_entropy_bits_per_byte"]
        # Fail-closed type and privacy negative controls.
        common = {"id":"bad","source":"CREATED_SYNTHETIC","privacy_class":"SYNTHETIC"}
        negatives = [
            dict(common,kind="JSON_NULL"),                              # absent != null
            dict(common,kind="MISSING_FIELD",payload=None),            # null != absent
            dict(common,kind="ZERO_NUMBER",payload=False),             # false != integer zero
            dict(common,kind="EMPTY_STRING",payload=" "),             # one space != empty
            dict(common,kind="SPACE_ONLY",payload="\t"),              # tab != ASCII U+0020
            dict(common,kind="EMPTY_GLYPH",payload=""),                # glyph != empty
            dict(common,kind="UNKNOWN",payload=""),                    # unknown tag needs data
            dict(common,kind="RAW_TEXT",payload="private",privacy_class="PRIVATE"),
        ]
        for negative in negatives:
            try:
                validate(canonical(negative))
                raise AssertionError("NEGATIVE_TOKEN_GATE_DID_NOT_REJECT")
            except ValueError:
                pass
        try:
            validate(canonical(records[0]) + canonical(records[0]))
            raise AssertionError("DUPLICATE_SOURCE_ID_NOT_REJECTED")
        except ValueError:
            pass
        try:
            shuffled_indices(2, 0)
            raise AssertionError("INVALID_SEED_NOT_REJECTED")
        except ValueError:
            pass
        order_bad = json.loads((root/"run1/order.json").read_text())
        order_bad["order"] = list(reversed(order_bad["order"]))
        (root/"order_bad.json").write_bytes(canonical(order_bad))
        try:
            restore(root/"run1/chaos.jsonl", root/"order_bad.json", root/"badrestore.jsonl")
            raise AssertionError("MODIFIED_ORDER_NOT_REJECTED")
        except ValueError:
            pass
        # Mutation: whitespace-class collision must fail.
        try:
            validate(canonical({"id":"bad","kind":"SPACE_ONLY","payload":"",
                "source":"CREATED_SYNTHETIC","privacy_class":"SYNTHETIC"}))
            raise AssertionError("EXPECTED_NEGATIVE_FAILURE")
        except ValueError:
            pass
        # Mutation: tamper chaos content must fail.
        (root/"run1/chaos.jsonl").write_bytes(b"{}\n")
        try:
            restore(root/"run1/chaos.jsonl", root/"run1/order.json", root/"tampered.jsonl")
            raise AssertionError("EXPECTED_HASH_REJECTION")
        except ValueError:
            pass
    print("SEMCHAOS_HOST_SELFTEST PASS 12 cases + 13 negative gates + deterministic replay")

def main():
    ap = argparse.ArgumentParser()
    sub = ap.add_subparsers(dest="op", required=True)
    b = sub.add_parser("build")
    b.add_argument("input")
    b.add_argument("output_dir")
    b.add_argument("--seed", type=int, default=42)
    r = sub.add_parser("restore")
    r.add_argument("chaos")
    r.add_argument("order")
    r.add_argument("output")
    sub.add_parser("selftest")
    args = ap.parse_args()
    if args.op == "selftest":
        selftest()
    elif args.op == "build":
        result = build(args.input, args.output_dir, args.seed)
        print(json.dumps(result, sort_keys=True, ensure_ascii=False))
    else:
        print(restore(args.chaos, args.order, args.output))

if __name__ == "__main__":
    main()
