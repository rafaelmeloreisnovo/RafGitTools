#!/usr/bin/env python3
# RAFAELIA / RafGitTools
# Conversation Relation Vector Low-Level V1
#
# Runtime boundary:
# - no imports
# - no stdlib modules
# - no third-party packages
# - no hashlib/json/pathlib/argparse/collections/math
# - no FFI / ctypes / JNI / native extension calls
# - authored SHA-256
# - authored canonical serializer
# - integer/rational metrics only
#
# Python itself still requires a Python interpreter/runtime.
# Therefore this file is "low-level pure Python core", not bare metal.

_INPUT_SCHEMA = "rafgittools.corpus-logistics-gymnasia.v1"
_OUTPUT_SCHEMA = "rafgittools.conversation-relation-vector.lowlevel.v1"

_K = [
    0x428a2f98,0x71374491,0xb5c0fbcf,0xe9b5dba5,0x3956c25b,0x59f111f1,0x923f82a4,0xab1c5ed5,
    0xd807aa98,0x12835b01,0x243185be,0x550c7dc3,0x72be5d74,0x80deb1fe,0x9bdc06a7,0xc19bf174,
    0xe49b69c1,0xefbe4786,0x0fc19dc6,0x240ca1cc,0x2de92c6f,0x4a7484aa,0x5cb0a9dc,0x76f988da,
    0x983e5152,0xa831c66d,0xb00327c8,0xbf597fc7,0xc6e00bf3,0xd5a79147,0x06ca6351,0x14292967,
    0x27b70a85,0x2e1b2138,0x4d2c6dfc,0x53380d13,0x650a7354,0x766a0abb,0x81c2c92e,0x92722c85,
    0xa2bfe8a1,0xa81a664b,0xc24b8b70,0xc76c51a3,0xd192e819,0xd6990624,0xf40e3585,0x106aa070,
    0x19a4c116,0x1e376c08,0x2748774c,0x34b0bcb5,0x391c0cb3,0x4ed8aa4a,0x5b9cca4f,0x682e6ff3,
    0x748f82ee,0x78a5636f,0x84c87814,0x8cc70208,0x90befffa,0xa4506ceb,0xbef9a3f7,0xc67178f2
]

_HEX = "0123456789abcdef"


def _u32(x):
    return x & 0xffffffff


def _rotr(x, n):
    return ((x >> n) | ((x << (32 - n)) & 0xffffffff)) & 0xffffffff


def _utf8_bytes(s):
    out = []
    i = 0
    n = len(s)
    while i < n:
        c = ord(s[i])
        if c < 0x80:
            out.append(c)
        elif c < 0x800:
            out.append(0xc0 | (c >> 6))
            out.append(0x80 | (c & 0x3f))
        elif c < 0x10000:
            out.append(0xe0 | (c >> 12))
            out.append(0x80 | ((c >> 6) & 0x3f))
            out.append(0x80 | (c & 0x3f))
        else:
            out.append(0xf0 | (c >> 18))
            out.append(0x80 | ((c >> 12) & 0x3f))
            out.append(0x80 | ((c >> 6) & 0x3f))
            out.append(0x80 | (c & 0x3f))
        i += 1
    return out


def _sha256_bytes(data):
    m = []
    i = 0
    n = len(data)
    while i < n:
        m.append(data[i] & 255)
        i += 1

    bit_len = n * 8
    m.append(0x80)
    while (len(m) & 63) != 56:
        m.append(0)

    shift = 56
    while shift >= 0:
        m.append((bit_len >> shift) & 255)
        shift -= 8

    h0 = 0x6a09e667
    h1 = 0xbb67ae85
    h2 = 0x3c6ef372
    h3 = 0xa54ff53a
    h4 = 0x510e527f
    h5 = 0x9b05688c
    h6 = 0x1f83d9ab
    h7 = 0x5be0cd19

    off = 0
    total = len(m)
    while off < total:
        w = [0] * 64
        t = 0
        p = off
        while t < 16:
            w[t] = ((m[p] << 24) | (m[p+1] << 16) | (m[p+2] << 8) | m[p+3]) & 0xffffffff
            t += 1
            p += 4
        while t < 64:
            x = w[t-15]
            y = w[t-2]
            s0 = _rotr(x, 7) ^ _rotr(x, 18) ^ (x >> 3)
            s1 = _rotr(y, 17) ^ _rotr(y, 19) ^ (y >> 10)
            w[t] = _u32(w[t-16] + s0 + w[t-7] + s1)
            t += 1

        a = h0
        b = h1
        c = h2
        d = h3
        e = h4
        f = h5
        g = h6
        h = h7

        t = 0
        while t < 64:
            s1 = _rotr(e, 6) ^ _rotr(e, 11) ^ _rotr(e, 25)
            ch = (e & f) ^ ((~e) & g)
            temp1 = _u32(h + s1 + ch + _K[t] + w[t])
            s0 = _rotr(a, 2) ^ _rotr(a, 13) ^ _rotr(a, 22)
            maj = (a & b) ^ (a & c) ^ (b & c)
            temp2 = _u32(s0 + maj)

            h = g
            g = f
            f = e
            e = _u32(d + temp1)
            d = c
            c = b
            b = a
            a = _u32(temp1 + temp2)
            t += 1

        h0 = _u32(h0 + a)
        h1 = _u32(h1 + b)
        h2 = _u32(h2 + c)
        h3 = _u32(h3 + d)
        h4 = _u32(h4 + e)
        h5 = _u32(h5 + f)
        h6 = _u32(h6 + g)
        h7 = _u32(h7 + h)
        off += 64

    words = [h0,h1,h2,h3,h4,h5,h6,h7]
    out = ""
    i = 0
    while i < 8:
        x = words[i]
        sh = 28
        while sh >= 0:
            out += _HEX[(x >> sh) & 15]
            sh -= 4
        i += 1
    return out


def _sha256_text(s):
    return _sha256_bytes(_utf8_bytes(s))


def _int_text(v):
    if v == 0:
        return "0"
    neg = v < 0
    if neg:
        v = -v
    digits = ""
    while v:
        q = v // 10
        r = v - q * 10
        digits = "0123456789"[r] + digits
        v = q
    if neg:
        digits = "-" + digits
    return digits


def _hex4(v):
    return (
        _HEX[(v >> 12) & 15] +
        _HEX[(v >> 8) & 15] +
        _HEX[(v >> 4) & 15] +
        _HEX[v & 15]
    )


def _quote(s):
    out = '"'
    i = 0
    n = len(s)
    while i < n:
        c = ord(s[i])
        ch = s[i]
        if ch == '"':
            out += '\\"'
        elif ch == "\\":
            out += "\\\\"
        elif c == 8:
            out += "\\b"
        elif c == 9:
            out += "\\t"
        elif c == 10:
            out += "\\n"
        elif c == 12:
            out += "\\f"
        elif c == 13:
            out += "\\r"
        elif c < 32:
            out += "\\u" + _hex4(c)
        else:
            out += ch
        i += 1
    return out + '"'


def _keys_sorted(d):
    keys = []
    for k in d:
        keys.append(k)
    i = 1
    while i < len(keys):
        x = keys[i]
        j = i - 1
        while j >= 0 and keys[j] > x:
            keys[j+1] = keys[j]
            j -= 1
        keys[j+1] = x
        i += 1
    return keys


def _canon(v):
    if v is None:
        return "null"
    if v is True:
        return "true"
    if v is False:
        return "false"
    if type(v) is int:
        return _int_text(v)
    if type(v) is str:
        return _quote(v)
    if type(v) is list:
        out = "["
        i = 0
        n = len(v)
        while i < n:
            if i:
                out += ","
            out += _canon(v[i])
            i += 1
        return out + "]"
    if type(v) is dict:
        out = "{"
        keys = _keys_sorted(v)
        i = 0
        n = len(keys)
        while i < n:
            if i:
                out += ","
            k = keys[i]
            out += _quote(k) + ":" + _canon(v[k])
            i += 1
        return out + "}"
    raise ValueError("unsupported canonical type")


def _sha_obj(v):
    return _sha256_text(_canon(v) + "\n")


def _contains(xs, x):
    i = 0
    while i < len(xs):
        if xs[i] == x:
            return True
        i += 1
    return False


def _unique_token_ids(chunk):
    out = []
    refs = chunk.get("token_refs") or []
    i = 0
    while i < len(refs):
        tid = refs[i].get("token_id")
        if tid is not None and not _contains(out, tid):
            out.append(tid)
        i += 1
    # authored insertion sort for deterministic order
    i = 1
    while i < len(out):
        x = out[i]
        j = i - 1
        while j >= 0 and out[j] > x:
            out[j+1] = out[j]
            j -= 1
        out[j+1] = x
        i += 1
    return out


def _count_intersection(a, b):
    count = 0
    i = 0
    while i < len(a):
        if _contains(b, a[i]):
            count += 1
        i += 1
    return count


def _count_union(a, b):
    count = len(a)
    i = 0
    while i < len(b):
        if not _contains(a, b[i]):
            count += 1
        i += 1
    return count


def _count_added(a, b):
    count = 0
    i = 0
    while i < len(b):
        if not _contains(a, b[i]):
            count += 1
        i += 1
    return count


def _count_removed(a, b):
    count = 0
    i = 0
    while i < len(a):
        if not _contains(b, a[i]):
            count += 1
        i += 1
    return count


def _find_chunk(chunks, cid):
    i = 0
    while i < len(chunks):
        if chunks[i].get("chunk_id") == cid:
            return chunks[i]
        i += 1
    return None


def _edge_count(edges, cid, inbound):
    count = 0
    i = 0
    while i < len(edges):
        e = edges[i]
        if inbound:
            if e.get("to") == cid:
                count += 1
        else:
            if e.get("from") == cid:
                count += 1
        i += 1
    return count


def _edge_type_out_count(edges, cid, typ):
    count = 0
    i = 0
    while i < len(edges):
        e = edges[i]
        if e.get("from") == cid and e.get("type") == typ:
            count += 1
        i += 1
    return count


def _token_signature(ids):
    s = ""
    i = 0
    while i < len(ids):
        if i:
            s += "\n"
        s += ids[i]
        i += 1
    return _sha256_text(s)


def build_lowlevel(plan):
    if plan.get("schema") != _INPUT_SCHEMA:
        raise ValueError("input schema mismatch")
    if plan.get("raw_body_embedded") is not False:
        raise ValueError("raw bodies forbidden")
    if plan.get("claim_allowed") is not False:
        raise ValueError("claim_allowed must be false")

    chunks = plan.get("chunks") or []
    edges = plan.get("edges") or []

    # Validate chunk ids and edge endpoints without set/dict indexing.
    i = 0
    while i < len(chunks):
        cid = chunks[i].get("chunk_id")
        if cid is None:
            raise ValueError("chunk_id missing")
        j = i + 1
        while j < len(chunks):
            if chunks[j].get("chunk_id") == cid:
                raise ValueError("duplicate chunk_id")
            j += 1
        i += 1

    i = 0
    while i < len(edges):
        a = edges[i].get("from")
        b = edges[i].get("to")
        if _find_chunk(chunks, a) is None or _find_chunk(chunks, b) is None:
            raise ValueError("edge references unknown chunk")
        i += 1

    vectors = []
    i = 0
    while i < len(chunks):
        c = chunks[i]
        cid = c.get("chunk_id")
        ids = _unique_token_ids(c)
        v = {
            "chunk_id": cid,
            "source_family": c.get("source_family"),
            "book_id": c.get("book_id"),
            "session_id": c.get("session_id"),
            "text_sha256": c.get("text_sha256"),
            "tier": c.get("tier"),
            "materialization_state": c.get("materialization_state"),
            "bytes": c.get("bytes") or 0,
            "token_count": c.get("token_count") or 0,
            "token_signature_sha256": _token_signature(ids),
            "relations_out": _edge_count(edges, cid, False),
            "relations_in": _edge_count(edges, cid, True),
            "next_count": _edge_type_out_count(edges, cid, "NEXT"),
            "previous_count": _edge_type_out_count(edges, cid, "PREVIOUS"),
            "semantic_embedding_state": "TOKEN_VAZIO",
            "causal_state": "TOKEN_VAZIO",
            "truth_state": "TOKEN_VAZIO",
            "claim_allowed": False,
        }
        v["vector_id"] = "VEC-" + _sha_obj(v)[:32]
        vectors.append(v)
        i += 1

    microdeltas = []
    i = 0
    while i < len(edges):
        e = edges[i]
        if e.get("type") == "NEXT":
            a = _find_chunk(chunks, e.get("from"))
            b = _find_chunk(chunks, e.get("to"))
            ta = _unique_token_ids(a)
            tb = _unique_token_ids(b)
            inter = _count_intersection(ta, tb)
            union = _count_union(ta, tb)
            if union == 0:
                jn = 1
                jd = 1
            else:
                jn = inter
                jd = union

            d = {
                "from_chunk_id": a.get("chunk_id"),
                "to_chunk_id": b.get("chunk_id"),
                "relation_type": "NEXT",
                "token_jaccard_num": jn,
                "token_jaccard_den": jd,
                "lexical_divergence_num": jd - jn,
                "lexical_divergence_den": jd,
                "token_added": _count_added(ta, tb),
                "token_removed": _count_removed(ta, tb),
                "token_count_delta": (b.get("token_count") or 0) - (a.get("token_count") or 0),
                "bytes_delta": (b.get("bytes") or 0) - (a.get("bytes") or 0),
                "convergence_state": "OBSERVED_LEXICAL_OVERLAP_ONLY",
                "novelty_state": "OBSERVED_TOKEN_SET_DELTA",
                "semantic_state": "TOKEN_VAZIO",
                "causal_state": "TOKEN_VAZIO",
                "evidence_state": "DERIVED_FROM_GOVERNED_CHUNK_METADATA",
                "claim_allowed": False,
            }
            d["microdelta_id"] = "MDELTA-" + _sha_obj(d)[:32]
            microdeltas.append(d)
        i += 1

    out = {
        "schema": _OUTPUT_SCHEMA,
        "source_schema": _INPUT_SCHEMA,
        "source_manifest_sha256": plan.get("manifest_sha256"),
        "raw_body_embedded": False,
        "claim_allowed": False,
        "runtime_profile": {
            "imports": 0,
            "stdlib_modules": 0,
            "third_party_modules": 0,
            "ffi_calls": 0,
            "native_extension_calls": 0,
            "hash_impl": "AUTHORIAL_SHA256_PYTHON",
            "canonical_serializer": "AUTHORIAL_CANONICAL_JSON_SUBSET",
            "floating_point_metrics": 0,
        },
        "state_planes": {
            "PROGRAM_STATE": "PROGRAM_OUTPUT_MATERIALIZED",
            "MODEL_INFERENCE_STATE": "NOT_RUN",
            "PARAMETER_UPDATE_STATE": "NOT_RUN",
            "AI_TRAINING": "NOT_RUN",
            "parameter_update_evidence": [],
            "training_gate": "BLOCKED_NO_PARAMETER_UPDATE_EVIDENCE",
        },
        "vectors": vectors,
        "microdeltas": microdeltas,
        "boundaries": [
            "PYTHON_RUNTIME_REQUIRED",
            "LOWLEVEL_PURE_PYTHON_CORE!=BARE_METAL",
            "LEXICAL_OVERLAP!=SEMANTIC_EQUIVALENCE",
            "CONVERGENCE!=TRUTH",
            "DIVERGENCE!=ERROR",
            "COOCCURRENCE!=CAUSALITY",
            "SOURCE!=ARTIFACT!=EXECUTION!=EVIDENCE!=CLAIM",
            "PROGRAM_EXECUTION!=MODEL_INFERENCE",
            "MODEL_INFERENCE!=PARAMETER_UPDATE",
            "PARAMETER_UPDATE!=AI_TRAINING_CLAIM",
            "PARAMETER_UPDATE_EVIDENCE_REQUIRED_FOR_AI_TRAINING",
            "TOKEN_VAZIO!=0",
        ],
    }
    out["manifest_sha256"] = _sha_obj(out)
    return out


def _selftest():
    # SHA-256 known-answer tests.
    if _sha256_text("") != "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855":
        raise AssertionError("sha256 empty KAT failed")
    if _sha256_text("abc") != "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad":
        raise AssertionError("sha256 abc KAT failed")

    plan = {
        "schema": _INPUT_SCHEMA,
        "raw_body_embedded": False,
        "claim_allowed": False,
        "manifest_sha256": "fixture",
        "chunks": [
            {
                "chunk_id": "CHK-A",
                "source_family": "CONVERSATIONS",
                "book_id": "B",
                "session_id": "S",
                "text_sha256": "a",
                "tier": "HOT",
                "materialization_state": "MATERIALIZED",
                "bytes": 10,
                "token_count": 2,
                "token_refs": [{"token_id": "TOK-X"}, {"token_id": "TOK-Y"}],
            },
            {
                "chunk_id": "CHK-B",
                "source_family": "CONVERSATIONS",
                "book_id": "B",
                "session_id": "S",
                "text_sha256": "b",
                "tier": "HOT",
                "materialization_state": "MATERIALIZED",
                "bytes": 12,
                "token_count": 2,
                "token_refs": [{"token_id": "TOK-Y"}, {"token_id": "TOK-Z"}],
            },
        ],
        "edges": [
            {"type": "NEXT", "from": "CHK-A", "to": "CHK-B"},
            {"type": "PREVIOUS", "from": "CHK-B", "to": "CHK-A"},
        ],
    }

    a = build_lowlevel(plan)
    b = build_lowlevel(plan)
    if _canon(a) != _canon(b):
        raise AssertionError("determinism failed")
    if len(a["vectors"]) != 2:
        raise AssertionError("vector count failed")
    if len(a["microdeltas"]) != 1:
        raise AssertionError("microdelta count failed")
    d = a["microdeltas"][0]
    if d["token_jaccard_num"] != 1 or d["token_jaccard_den"] != 3:
        raise AssertionError("jaccard rational failed")
    if d["token_added"] != 1 or d["token_removed"] != 1:
        raise AssertionError("token delta failed")
    if d["semantic_state"] != "TOKEN_VAZIO":
        raise AssertionError("fail-closed semantic boundary failed")
    if a["raw_body_embedded"] is not False or a["claim_allowed"] is not False:
        raise AssertionError("governance boundary failed")
    planes = a["state_planes"]
    if planes["PROGRAM_STATE"] != "PROGRAM_OUTPUT_MATERIALIZED":
        raise AssertionError("program state boundary failed")
    if planes["MODEL_INFERENCE_STATE"] != "NOT_RUN":
        raise AssertionError("model inference boundary failed")
    if planes["PARAMETER_UPDATE_STATE"] != "NOT_RUN":
        raise AssertionError("parameter update boundary failed")
    if planes["AI_TRAINING"] != "NOT_RUN":
        raise AssertionError("ai training boundary failed")
    if len(planes["parameter_update_evidence"]) != 0:
        raise AssertionError("unexpected parameter update evidence")
    print("CONVERSATION_RELATION_VECTOR_LOWLEVEL_V1_SELFTEST_PASS")


if __name__ == "__main__":
    _selftest()
