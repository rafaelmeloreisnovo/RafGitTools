import copy
import hashlib
import pathlib
import sys
import unittest
from datetime import datetime, timezone

HERE = pathlib.Path(__file__).resolve().parent
sys.path[:0] = [str(HERE.parent / "scripts" / "ci"), str(HERE)]
from dependency_closure_v1 import assess, digest

NOW = datetime(2026, 11, 25, 12, tzinfo=timezone.utc)
DIRECTIONS = ("direct", "inverse", "derivative", "antiderivative", "recursive", "orthogonal", "integrative")


def fixture():
    m = {"root": "A", "nodes": {k: {"receipt_sha256": hashlib.sha256(k.encode()).hexdigest(), "requires": deps}
                                  for k, deps in (("A", ["B", "C"]), ("B", ["D"]), ("C", ["D"]), ("D", []))}}
    o = {k: dict(receipt_sha256=n["receipt_sha256"], revoked=False, superseded=False,
                 permission_invalidated=False, artifact_verified=True, conclusion="success",
                 issued_at="2026-11-25T11:00:00Z", expires_at="2026-11-25T13:00:00Z") for k,n in m["nodes"].items()}
    return m,o


def run_case(round_number, direction):
    m,o = fixture()
    d = digest(m)
    good = False
    expected_invalid = None
    if round_number == 1:
        if direction == "direct": good = True
        elif direction == "inverse": o["D"]["revoked"] = True; expected_invalid = ["A","B","C","D"]
        elif direction == "derivative": o["D"]["receipt_sha256"] = "f"*64; expected_invalid = ["A","B","C","D"]
        elif direction == "antiderivative": del o["D"]; expected_invalid = ["A","B","C","D"]
        elif direction == "recursive": m["nodes"]["D"]["requires"] = ["A"]; d = digest(m)
        elif direction == "orthogonal": m["nodes"]["A"]["requires"].reverse(); good = True
        elif direction == "integrative": o["C"]["artifact_verified"] = False; expected_invalid = ["A","C"]
    elif round_number == 2:
        if direction == "direct": d = "0"*64
        elif direction == "inverse": del o["D"]["revoked"]
        elif direction == "derivative": o["D"]["expires_at"] = "2026-11-25T12:00:00Z"
        elif direction == "antiderivative": m["nodes"]["B"]["requires"] = ["MISSING"]; d = digest(m)
        elif direction == "recursive": m["nodes"]["D"]["requires"] = ["D"]; d = digest(m)
        elif direction == "orthogonal": o["D"]["revoked"] = 0
        elif direction == "integrative": o["A"]["conclusion"] = "skipped"
    elif round_number == 3:
        if direction == "direct": m["nodes"] = dict(reversed(list(m["nodes"].items()))); good = True
        elif direction == "inverse": o["B"]["superseded"] = True; expected_invalid = ["A","B"]
        elif direction == "derivative": o["D"]["issued_at"] = "2026-11-25T12:01:00Z"
        elif direction == "antiderivative": m["nodes"]["E"] = copy.deepcopy(m["nodes"]["D"]); d = digest(m)
        elif direction == "recursive": m["nodes"]["A"]["requires"] = ["B","B"]; d = digest(m)
        elif direction == "orthogonal": o["D"]["artifact_verified"] = 1
        elif direction == "integrative": o["D"]["permission_invalidated"] = None
    elif round_number == 4:
        if direction == "direct": m["nodes"]["B"]["requires"] = []; m["nodes"]["C"]["requires"] = []; d = digest(m)
        elif direction == "inverse": o["D"]["permission_invalidated"] = True; expected_invalid = ["A","B","C","D"]
        elif direction == "derivative": o["D"]["issued_at"] = "TOKEN_VAZIO_TIME"
        elif direction == "antiderivative": o["E"] = copy.deepcopy(o["D"])
        elif direction == "recursive": m["nodes"]["D"]["receipt_sha256"] = "TOKEN_VAZIO_HASH"; d = digest(m)
        elif direction == "orthogonal": o["D"]["revoked"] = "false"
        elif direction == "integrative": good = True
    out = assess(m,d,o,NOW)
    return out, good, expected_invalid


class ClosureTests(unittest.TestCase):
    def test_28_cells(self):
        for round_number in range(1,5):
            for direction in DIRECTIONS:
                with self.subTest(round=round_number, direction=direction):
                    out, good, invalid = run_case(round_number,direction)
                    self.assertEqual(out["state"], "STRUCTURAL_CANDIDATE_ONLY" if good else "RERUN_OR_BLOCK")
                    for flag in ("skip_allowed","claim_allowed","check_status_write_allowed","producer_authenticated","dependency_closure_verified"):
                        self.assertIs(out[flag],False)
                    if invalid is not None:
                        self.assertEqual(out["invalidated"],invalid)
                    self.assertEqual(out["dependency_receipts"] is not None,good)


if __name__ == "__main__":
    unittest.main()
