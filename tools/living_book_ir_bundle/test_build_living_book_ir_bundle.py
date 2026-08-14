#!/usr/bin/env python3
from __future__ import annotations

import importlib.util
from pathlib import Path
import unittest

HERE = Path(__file__).resolve().parent
SPEC = importlib.util.spec_from_file_location("bundle", HERE / "build_living_book_ir_bundle.py")
bundle = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(bundle)


def ir_fixture():
    body = {
        "intent_id": "INT-MUSIC-0001",
        "cell_id": "LBC-MUSIC-0001",
        "cell_digests": {"sha256": "a" * 64, "sha3_256": "b" * 64, "blake2b_256": "c" * 64},
        "module_id": "support.math",
        "module_kind": "SUPPORT",
        "action": "PROPOSE_ANALYSIS",
        "capabilities": ["read_declared_metadata", "produce_proposal", "translate_to_domain"],
        "forbidden_capabilities": ["network", "publish", "merge", "delete", "disclose_private", "execute_untrusted", "shell_eval"],
        "output_contract": {"language": "pt-BR", "translate_to_domain": True, "raw_seed_text_in_output": False, "private_content_in_output": False},
        "policy_gates": {"execution_allowed": False, "publication_allowed": False, "claim_allowed": False},
        "expected_receipt": ["intent_id", "object_id", "outputs", "tests", "rollback", "F_ok", "F_gap", "F_next"],
        "state": "COMPILED_NON_EXECUTABLE_IR"
    }
    return {
        "schema": "rafpolimata.living-book-domain-ir/v1",
        "ir": body,
        "integrity": {"digests": bundle.digests(body)}
    }


def make():
    return bundle.build_bundle(
        ir_fixture(), "LBB-MUSIC-0001",
        "instituto-Rafael/LivroVivo_ThisBookLives", "6e51364d43642cdd65d6d4d50d52c7124394b07a",
        "rafaelmeloreisnovo/RafPolimata", "480dee81b397c9f5a716aed203e67292829d8e82"
    )


class BundleTests(unittest.TestCase):
    def test_valid_descriptor_only_bundle(self):
        value = make()
        self.assertEqual([], bundle.validate_bundle(value))
        self.assertFalse(value["payload"]["ir_embedded"])
        self.assertFalse(value["payload"]["private_source_embedded"])

    def test_execute_action_rejected(self):
        ir = ir_fixture()
        ir["ir"]["action"] = "EXECUTE"
        ir["integrity"]["digests"] = bundle.digests(ir["ir"])
        with self.assertRaisesRegex(ValueError, "action forbidden"):
            bundle.build_bundle(ir, "LBB-X-0001", "p", "r", "c", "r")

    def test_seed_key_rejected(self):
        ir = ir_fixture()
        ir["ir"]["seed"] = "private"
        ir["integrity"]["digests"] = bundle.digests(ir["ir"])
        with self.assertRaisesRegex(ValueError, "forbidden key"):
            bundle.build_bundle(ir, "LBB-X-0001", "p", "r", "c", "r")

    def test_digest_tamper_detected(self):
        value = make()
        value["payload"]["module_id"] = "tampered"
        self.assertTrue(any("digest mismatch" in e for e in bundle.validate_bundle(value)))

    def test_dispatch_cannot_be_enabled(self):
        value = make()
        value["policy"]["dispatch_allowed"] = True
        value["integrity"]["digests"] = bundle.digests({k: value[k] for k in ("source", "payload", "policy")})
        self.assertTrue(any("dispatch_allowed" in e for e in bundle.validate_bundle(value)))

    def test_network_target_rejected(self):
        value = make()
        value["policy"]["network_target"] = "https://example.invalid"
        value["integrity"]["digests"] = bundle.digests({k: value[k] for k in ("source", "payload", "policy")})
        self.assertTrue(any("network target" in e for e in bundle.validate_bundle(value)))

    def test_approval_digest_forbidden_before_review(self):
        value = make()
        value["policy"]["human_approval_digest"] = "d" * 64
        value["integrity"]["digests"] = bundle.digests({k: value[k] for k in ("source", "payload", "policy")})
        self.assertTrue(any("approval digest" in e for e in bundle.validate_bundle(value)))

    def test_private_content_flag_rejected(self):
        value = make()
        value["payload"]["private_source_embedded"] = True
        value["integrity"]["digests"] = bundle.digests({k: value[k] for k in ("source", "payload", "policy")})
        self.assertTrue(any("private source" in e for e in bundle.validate_bundle(value)))

    def test_bundle_contains_only_descriptors(self):
        value = make()
        text = str(value).lower()
        self.assertNotIn("conversation", text)
        self.assertEqual(value["integrity"]["digests"], bundle.digests({k: value[k] for k in ("source", "payload", "policy")}))


if __name__ == "__main__":
    unittest.main()
