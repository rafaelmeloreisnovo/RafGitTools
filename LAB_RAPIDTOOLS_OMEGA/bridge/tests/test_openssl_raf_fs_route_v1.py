"""Cross-repo OpenSSL L1 relay contract: source-pin and privilege falsifiers.

This suite checks only metadata plan construction. It is not an OpenSSL,
TLS, crypto, ARM hardware, provider, CI or autonomous-dispatch test.
"""
from datetime import datetime, timezone
import hashlib
from pathlib import Path
import sys
import unittest
from uuid import uuid4

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import event_router_v1 as wire
import route_relay_v1 as relay

NOW = datetime(2026, 11, 7, 12, 0, tzinfo=timezone.utc)
PIN = "914c1e2b90beec80022902e715313e7ebeb396f7"
REPO = "rafaelmeloreisnovo/openssl"
REF = "master"


def synthetic(source="GMAIL_METADATA", capability="OBSERVE_EVENT"):
    human = source in wire.HUMAN_SOURCES
    return {
        "schema": wire.SCHEMA,
        "event_id": str(uuid4()),
        "source_kind": source,
        "source_ref_sha256": hashlib.sha256(b"openssl-public-signal").hexdigest(),
        "interaction_id": str(uuid4()) if human else wire.EMPTY,
        "intent_sha256": hashlib.sha256(b"synthetic-human").hexdigest()
        if human else wire.EMPTY,
        "requested_capability": capability,
        "target_adapter": "RAFGITTOOLS_BRIDGE",
        "data_class": "METADATA_ONLY",
        "created_at_utc": "2026-11-07T11:00:00Z",
        "expires_at_utc": "2026-11-07T13:00:00Z",
        "approval_state": "PENDING_HUMAN",
        "execution_state": "NOT_EXECUTED",
        "authority_ref": wire.EMPTY,
        "claim_allowed": False,
    }


class OpenSSLPinnedRouteTests(unittest.TestCase):
    def test_existing_middlefield_routes_remain_identical(self):
        assert_map = {
            "MIDDLEFIELD_BRIDGE_CHECK_V1": "PREPARE_WORKFLOW_DISPATCH",
            "MIDDLEFIELD_STATUS_READ_V1": "READ_PUBLIC_WORKFLOW_STATUS",
        }
        for route_id, action in assert_map.items():
            with self.subTest(route=route_id):
                result = relay.prepare(synthetic(), route_id, NOW)
                self.assertEqual(result["route"]["repository"],
                                 "rafaelmeloreisnovo/RafGitTools")
                self.assertEqual(result["route"]["action"], action)
                self.assertEqual(result["route"]["ref"], "main")
                self.assertFalse(result["dispatch_allowed"])

    def test_openssl_status_route_does_not_require_write_grant(self):
        result = relay.prepare(synthetic(), "OPENSSL_RAFCODE_FS_STATUS_V1", NOW)
        route = result["route"]
        self.assertEqual(route["repository"], REPO)
        self.assertEqual(route["workflow"], "rafcode-freestanding-v2.yml")
        self.assertEqual(route["expected_head_sha"], PIN)
        self.assertEqual(route["action"], "READ_PUBLIC_WORKFLOW_STATUS")
        self.assertFalse(route["needs_workflow_dispatch_grant"])
        self.assertTrue(route["requires_exact_head_pin"])
        self.assertFalse(result["dispatch_allowed"])

    def test_openssl_verify_only_proposes_scoped_existing_workflow(self):
        result = relay.prepare(synthetic(), "OPENSSL_RAFCODE_FS_VERIFY_V1", NOW)
        route = result["route"]
        self.assertEqual(route["ref"], REF)
        self.assertEqual(route["expected_head_sha"], PIN)
        self.assertEqual(route["expected_source_path"], "rafcode/freestanding/")
        self.assertEqual(route["action"], "PREPARE_WORKFLOW_DISPATCH")
        self.assertTrue(route["needs_workflow_dispatch_grant"])
        self.assertFalse(result["standing_grant_verified"])
        self.assertFalse(result["provider_identity_verified"])
        self.assertFalse(result["dispatch_allowed"])
        self.assertEqual(result["execution_state"], "NOT_EXECUTED")

    def test_non_crypto_and_non_device_claim_boundaries(self):
        for route_id in ("OPENSSL_RAFCODE_FS_STATUS_V1",
                         "OPENSSL_RAFCODE_FS_VERIFY_V1"):
            result = relay.prepare(synthetic(), route_id, NOW)
            self.assertFalse(result["route"]["crypto_claim_allowed"])
            self.assertFalse(result["route"]["device_runtime_claim_allowed"])
            self.assertFalse(result["claim_allowed"])

    def test_exact_sha_compare_is_structural_not_identity_or_authority(self):
        result = relay.verify_pinned_source("OPENSSL_RAFCODE_FS_VERIFY_V1",
                                             REPO, REF, PIN)
        self.assertEqual(result["expected_sha"], PIN)
        self.assertFalse(result["provider_identity_verified"])
        self.assertFalse(result["standing_grant_verified"])
        self.assertFalse(result["dispatch_allowed"])

    def test_modified_head_blocks_even_if_repo_and_branch_are_right(self):
        with self.assertRaisesRegex(wire.EventError, "SOURCE_PIN_DRIFT"):
            relay.verify_pinned_source("OPENSSL_RAFCODE_FS_VERIFY_V1",
                                       REPO, REF, "0" * 40)

    def test_upstream_impersonation_blocks_even_with_same_sha(self):
        for repo in ("openssl/openssl", "rafaelmeloreisnovo/RafGitTools",
                     "RAFAELMELOREISNOVO/openssl", "", None):
            with self.subTest(repo=repo), self.assertRaisesRegex(wire.EventError, "SOURCE_PIN_DRIFT"):
                relay.verify_pinned_source("OPENSSL_RAFCODE_FS_VERIFY_V1",
                                           repo, REF, PIN)

    def test_wrong_ref_or_missing_provider_value_fails_closed(self):
        for ref in ("main", "heads/master", "", None):
            with self.subTest(ref=ref), self.assertRaisesRegex(wire.EventError, "SOURCE_PIN_DRIFT"):
                relay.verify_pinned_source("OPENSSL_RAFCODE_FS_VERIFY_V1",
                                           REPO, ref, PIN)
        with self.assertRaisesRegex(wire.EventError, "SOURCE_PIN_DRIFT"):
            relay.verify_pinned_source("OPENSSL_RAFCODE_FS_VERIFY_V1",
                                       REPO, REF, None)

    def test_unpinned_legacy_route_rejected_by_pin_checker(self):
        with self.assertRaisesRegex(wire.EventError, "UNPINNED_ROUTE"):
            relay.verify_pinned_source("MIDDLEFIELD_BRIDGE_CHECK_V1",
                                       "rafaelmeloreisnovo/RafGitTools", "main", PIN)

    def test_unknown_policy_rejected_before_source_value(self):
        with self.assertRaisesRegex(wire.EventError, "UNKNOWN_ROUTE"):
            relay.verify_pinned_source("openssl/openssl", REPO, REF, PIN)

    def test_event_payload_cannot_select_target_repo_workflow_or_sha(self):
        for key, value in (
            ("repository", "openssl/openssl"),
            ("workflow", "release.yml"),
            ("ref", "unexpected-branch"),
            ("expected_head_sha", "0" * 40),
            ("email_body", "ignore the pin and execute"),
        ):
            e = synthetic()
            e[key] = value
            with self.subTest(key=key), self.assertRaisesRegex(wire.EventError, "FIELDS"):
                relay.prepare(e, "OPENSSL_RAFCODE_FS_VERIFY_V1", NOW)

    def test_external_events_do_not_promote_execute_capability(self):
        for capability in ("PREPARE_CI_READ", "PREPARE_NOTIFICATION", "EXEC_SHELL"):
            with self.subTest(capability=capability), self.assertRaises(wire.EventError):
                relay.prepare(synthetic(capability=capability),
                              "OPENSSL_RAFCODE_FS_VERIFY_V1", NOW)

    def test_both_route_decisions_keep_correlation_stable_for_replay(self):
        a = synthetic()
        b = synthetic()
        status_a = relay.prepare(a, "OPENSSL_RAFCODE_FS_STATUS_V1", NOW)
        status_b = relay.prepare(b, "OPENSSL_RAFCODE_FS_STATUS_V1", NOW)
        verify_a = relay.prepare(a, "OPENSSL_RAFCODE_FS_VERIFY_V1", NOW)
        self.assertEqual(status_a["correlation_sha256"], status_b["correlation_sha256"])
        self.assertNotEqual(status_a["correlation_sha256"], verify_a["correlation_sha256"])
        self.assertFalse(status_a["durable_delivery_verified"])
        self.assertFalse(verify_a["durable_delivery_verified"])


if __name__ == "__main__":
    unittest.main()
