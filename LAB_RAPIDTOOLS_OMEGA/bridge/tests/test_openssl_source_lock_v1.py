"""Source-lock regression: OpenSSL fork attribution and no elevated runtime claims."""
import json
from pathlib import Path
import re
import sys
import unittest

BRIDGE = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(BRIDGE))
import route_relay_v1 as relay

MANIFEST = BRIDGE / "contracts" / "OPENSSL_RAFCODE_SOURCE_LOCK_V1.json"


class OpenSSLSourceLockTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.m = json.loads(MANIFEST.read_text(encoding="utf-8"))

    def test_lock_is_pinned_to_verified_merged_head(self):
        self.assertEqual(self.m["source"]["commit"],
                         "914c1e2b90beec80022902e715313e7ebeb396f7")
        self.assertEqual(self.m["source"]["repository"],
                         "rafaelmeloreisnovo/openssl")
        self.assertEqual(self.m["source"]["fork_parent"], "openssl/openssl")
        self.assertEqual(self.m["source"]["merged_prs"], [9, 10])
        for route_id in (self.m["bridge"]["read_route"],
                         self.m["bridge"]["verify_route"]):
            route = relay.ROUTES[route_id]
            self.assertEqual(route["repository"], self.m["source"]["repository"])
            self.assertEqual(route["expected_head_sha"], self.m["source"]["commit"])
            self.assertEqual(route["workflow"],
                             self.m["modules"]["rafcode/freestanding/"]["workflow"])

    def test_all_source_blob_checksums_are_git_sha1_shape(self):
        for key, item in self.m["modules"].items():
            for path, sha in item["file_blobs_git_sha1"].items():
                with self.subTest(path=path):
                    self.assertTrue(path.startswith(key))
                    self.assertRegex(sha, r"^[0-9a-f]{40}$")

    def test_licensing_is_not_transferred_to_upstream(self):
        self.assertEqual(self.m["source"]["upstream_license"], "Apache-2.0")
        self.assertEqual(self.m["modules"]["rafcode/freestanding/"]["license_declaration"],
                         "MIT")
        self.assertEqual(self.m["modules"]["rafcode/l0/"]["license_declaration"],
                         "Apache-2.0")
        self.assertEqual(self.m["modules"]["rmr/"]["provenance_review"],
                         "TOKEN_VAZIO_PER_COMPONENT")
        self.assertEqual(self.m["source"]["authorial_scope"],
                         "ONLY_VERIFIED_FILE_DELTAS")

    def test_crypto_and_device_evidence_not_inferred(self):
        l1 = self.m["modules"]["rafcode/freestanding/"]
        self.assertEqual(l1["wire_bytes"], 24)
        self.assertFalse(l1["claims"]["crypto"])
        self.assertFalse(l1["claims"]["tls"])
        self.assertFalse(l1["claims"]["authentication"])
        self.assertFalse(l1["claims"]["provider_integration"])
        self.assertEqual(l1["claims"]["device_execution"], "TOKEN_VAZIO")
        rmr = self.m["modules"]["rmr/"]
        self.assertFalse(rmr["claims"]["fips"])
        self.assertFalse(rmr["claims"]["upstream_replacement"])
        self.assertEqual(rmr["claims"]["performance_gain"], "TOKEN_VAZIO")

    def test_bridge_policy_never_proclaims_execution(self):
        gate = self.m["bridge"]
        for prop in ("workflow_dispatch_permitted_without_broker",
                     "input_signal_is_authority", "claim_allowed"):
            self.assertFalse(gate[prop])
        for prop in ("requires_authenticated_provider",
                     "requires_standing_grant", "requires_pin_match"):
            self.assertTrue(gate[prop])
        self.assertFalse(relay.ROUTES[gate["verify_route"]]["crypto_claim_allowed"])

    def test_no_code_copied_into_source_lock(self):
        self.assertTrue(all("file_blobs_git_sha1" in item
                            for item in self.m["modules"].values()))
        self.assertEqual(self.m["bridge"]["producer"], "rafaelmeloreisnovo/RafGitTools")
        self.assertEqual(self.m["source"]["branch"], "master")


if __name__ == "__main__":
    unittest.main()
