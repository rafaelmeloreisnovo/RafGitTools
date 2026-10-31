from __future__ import annotations

import copy
import importlib.util
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SPEC = importlib.util.spec_from_file_location("assurance", ROOT / "scripts/private_ci_assurance_preflight_v1.py")
assert SPEC and SPEC.loader
module = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(module)


def registry():
    return {"schema": "rafgittools.private-ci-execution-registry.v1", "claim_allowed": False,
            "secret_value_persisted": False,
            "targets": [{"target_id": "synthetic", "allowed_workflow_ids": ["unit"]}]}


def packet():
    return {"schema": module.SCHEMA, "intent_id": "synthetic_01",
            "control_repo": "rafaelmeloreisnovo/RafGitTools", "control_commit": "a" * 40,
            "target_id": "synthetic", "target_commit": "b" * 40,
            "workflow_id": "unit", "purpose": "synthetic_preflight", "actor_type": "human",
            "mutation": False, "publish": False, "uses_real_data": False, "claim_allowed": False,
            "gates": {g: "PASS_WITH_RECEIPT" for g in module.GATES},
            "evidence_refs": {g: f"github:fixture/{g}@{'b'*40}" for g in module.GATES}}


class FalsificationTests(unittest.TestCase):
    def test_baseline_never_authorizes(self):
        r = module.assess(packet(), registry())
        self.assertEqual(r["state"], "REVIEW_REQUIRED")
        self.assertFalse(r["claim_allowed"])
        self.assertFalse(r["operation_permitted"])

    def test_every_gate_mutated_fail_closed(self):
        baseline = packet()
        for gate in module.GATES:
            for value in ("TOKEN_VAZIO", "NOT_RUN", "FAIL", "PASS", None, 0, False, "", "    ", "\x00"):
                with self.subTest(gate=gate, value=repr(value)):
                    x = copy.deepcopy(baseline)
                    x["gates"][gate] = value
                    result = module.assess(x, registry())
                    self.assertNotEqual(result["state"], "REVIEW_REQUIRED")
                    self.assertFalse(result["operation_permitted"])

    def test_missing_or_forged_receipts_fail_closed(self):
        for value in (None, 0, "", "   ", "PASS", "https://example.org/secret"):
            with self.subTest(value=repr(value)):
                x = packet()
                x["evidence_refs"][module.GATES[0]] = value
                self.assertEqual(module.assess(x, registry())["state"], "HOLD")

    def test_field_mutations_never_authorize(self):
        for name in ("intent_id", "control_repo", "control_commit", "target_id", "target_commit", "workflow_id", "purpose", "actor_type"):
            for value in (None, False, 0, "", " ", "\x00"):
                with self.subTest(name=name, value=repr(value)):
                    x = packet()
                    x[name] = value
                    self.assertNotEqual(module.assess(x, registry())["state"], "REVIEW_REQUIRED")

    def test_publication_mutation_and_private_data_never_authorize(self):
        for name in ("mutation", "publish", "uses_real_data", "claim_allowed"):
            x = packet()
            x[name] = True
            self.assertEqual(module.assess(x, registry())["state"], "BLOCKED")

    def test_unknown_extra_fields_and_removed_fields(self):
        x = packet()
        x["secret_value"] = "SYNTHETIC_ONLY"
        self.assertEqual(module.assess(x, registry())["state"], "BLOCKED")
        for key in packet():
            x = packet()
            x.pop(key)
            self.assertNotEqual(module.assess(x, registry())["state"], "REVIEW_REQUIRED")

    def test_inverse_order_equivalence(self):
        x = packet()
        y = dict(reversed(list(x.items())))
        y["gates"] = dict(reversed(list(x["gates"].items())))
        y["evidence_refs"] = dict(reversed(list(x["evidence_refs"].items())))
        self.assertEqual(module.assess(x, registry()), module.assess(y, registry()))

    def test_unknown_is_not_zero_or_empty(self):
        self.assertEqual(module._symbolic_void(None), "TOKEN_VAZIO_NULL")
        self.assertEqual(module._symbolic_void(False), "TOKEN_VAZIO_FALSE")
        self.assertEqual(module._symbolic_void(0), "TOKEN_VAZIO_ZERO")
        self.assertEqual(module._symbolic_void(""), "TOKEN_VAZIO_EMPTY")
        self.assertEqual(module._symbolic_void("    "), "TOKEN_VAZIO_WHITESPACE")
        self.assertEqual(module._symbolic_void("a\x00b"), "TOKEN_VAZIO_NUL_BYTE")

    def test_malformed_nested_values_do_not_crash(self):
        for key in ("purpose", "actor_type", "workflow_id", "intent_id", "control_commit"):
            for v in ([], {}, ["synthetic"], {"raw": "value"}):
                with self.subTest(key=key, value=repr(v)):
                    x = packet()
                    x[key] = v
                    self.assertNotEqual(module.assess(x, registry())["state"], "REVIEW_REQUIRED")
        for v in ([], {}, {"PASS_WITH_RECEIPT": True}):
            x = packet()
            x["gates"]["process_tree_cleanup"] = v
            self.assertNotEqual(module.assess(x, registry())["state"], "REVIEW_REQUIRED")

    def test_registry_cannot_expand_target_silently(self):
        x = packet()
        x["target_id"] = "other-private"
        self.assertEqual(module.assess(x, registry())["state"], "BLOCKED")
        x = packet()
        x["workflow_id"] = "admin"
        self.assertEqual(module.assess(x, registry())["state"], "BLOCKED")

    def test_gate_purpose_never_infers_runtime_truth(self):
        x = packet()
        for gate in ("process_tree_cleanup", "bounded_stdout_stderr", "filesystem_isolation"):
            x["gates"][gate] = "TOKEN_VAZIO"
            x["evidence_refs"][gate] = None
        out = module.assess(x, registry())
        self.assertEqual(out["state"], "HOLD")
        self.assertTrue(any("PROCESS_TREE_CLEANUP" in s for s in out["unknown"]))


if __name__ == "__main__":
    unittest.main()
