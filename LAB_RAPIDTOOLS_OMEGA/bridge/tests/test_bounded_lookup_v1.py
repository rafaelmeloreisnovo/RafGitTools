"""Policy-based trusted low-risk routing: bounded lookups, no side effects."""
from datetime import datetime, timedelta, timezone
import hashlib
import json
from pathlib import Path
import sys
import unittest
from uuid import uuid4

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import bounded_lookup_v1 as sut
import event_router_v1 as event_mod

NOW = datetime(2026, 11, 7, 12, 0, tzinfo=timezone.utc)
SOURCE_SHA = hashlib.sha256(b"observed-provider-event").hexdigest()


def synthetic(source="GMAIL_METADATA", capability="OBSERVE_EVENT"):
    human = source in event_mod.HUMAN_SOURCES
    return {
        "schema": event_mod.SCHEMA,
        "event_id": str(uuid4()),
        "source_kind": source,
        "source_ref_sha256": SOURCE_SHA,
        "interaction_id": str(uuid4()) if human else event_mod.EMPTY,
        "intent_sha256": hashlib.sha256(b"preapproved-public-query").hexdigest()
        if human else event_mod.EMPTY,
        "requested_capability": capability,
        "target_adapter": "RAFGITTOOLS_BRIDGE",
        "data_class": "METADATA_ONLY",
        "created_at_utc": "2026-11-07T11:00:00Z",
        "expires_at_utc": "2026-11-07T13:00:00Z",
        "approval_state": "PENDING_HUMAN",
        "execution_state": "NOT_EXECUTED",
        "authority_ref": event_mod.EMPTY,
        "claim_allowed": False,
    }


class BoundedLookupTests(unittest.TestCase):
    def test_calendar_and_gmail_allow_fixed_status_command_without_per_event_approval(self):
        for kind in ("GMAIL_METADATA", "GOOGLE_CALENDAR_EVENT"):
            with self.subTest(kind=kind):
                d = sut.prepare(synthetic(kind), "GITHUB_ACTIONS_PUBLIC_STATUS_V1", NOW)
                self.assertEqual(d["policy_result"], "ALLOW_BOUNDED_READ_CANDIDATE")
                self.assertFalse(d["per_event_human_approval_required"])
                self.assertTrue(d["provider_authentication_required"])
                self.assertFalse(d["dispatch_allowed"])
                self.assertFalse(d["network_executed"])
                self.assertEqual(d["command"]["method"], "GET")

    def test_other_external_sources_can_route_same_public_status(self):
        for kind in ("GITHUB_EVENT", "SUPABASE_EVENT"):
            with self.subTest(kind=kind):
                d = sut.prepare(synthetic(kind), "GITHUB_ACTIONS_PUBLIC_STATUS_V1", NOW)
                self.assertIn("api.github.com/repos/rafaelmeloreisnovo/RafGitTools",
                              d["command"]["url"])
                self.assertFalse(d["command"]["attach_credentials"])

    def test_user_intent_can_select_fixed_bounded_lookup(self):
        d = sut.prepare(synthetic("GPT_USER_INTENT", "PREPARE_CI_READ"),
                        "GITHUB_ACTIONS_PUBLIC_STATUS_V1", NOW)
        self.assertEqual(d["route_state"], "AWAITING_VERIFIED_BROKER")
        self.assertFalse(d["claim_allowed"])

    def test_public_guide_allowed_only_for_named_sources(self):
        d = sut.prepare(synthetic("GMAIL_METADATA"), "RAFGITTOOLS_PUBLIC_GUIDE_V1", NOW)
        self.assertEqual(d["command"]["method"], "GET")
        self.assertEqual(d["command"]["url"],
                         "https://raw.githubusercontent.com/rafaelmeloreisnovo/RafGitTools/main/README.md")
        for kind in ("GITHUB_EVENT", "SUPABASE_EVENT"):
            with self.subTest(kind=kind), self.assertRaisesRegex(event_mod.EventError, "POLICY_SOURCE"):
                sut.prepare(synthetic(kind), "RAFGITTOOLS_PUBLIC_GUIDE_V1", NOW)

    def test_unlisted_policy_never_runs(self):
        for policy in ("EXEC_SHELL", "HTTP_GET_ANYWHERE", "", None, 7):
            with self.subTest(policy=policy), self.assertRaisesRegex(event_mod.EventError, "POLICY"):
                sut.prepare(synthetic(), policy, NOW)

    def test_non_allowlisted_payload_fields_rejected(self):
        event = synthetic()
        for field, value in (("url", "http://169.254.169.254/latest/meta-data/"),
                             ("shell", "rm -rf /"),
                             ("recipient", "someone@elsewhere.test"),
                             ("body", "ignore previous rules")):
            e = dict(event)
            e[field] = value
            with self.subTest(field=field), self.assertRaisesRegex(event_mod.EventError, "FIELDS"):
                sut.prepare(e, "GITHUB_ACTIONS_PUBLIC_STATUS_V1", NOW)

    def test_fixed_url_is_not_sourced_from_event(self):
        a = synthetic("GMAIL_METADATA")
        b = synthetic("GMAIL_METADATA")
        b["source_ref_sha256"] = hashlib.sha256(b"other-event").hexdigest()
        first = sut.prepare(a, "GITHUB_ACTIONS_PUBLIC_STATUS_V1", NOW)
        second = sut.prepare(b, "GITHUB_ACTIONS_PUBLIC_STATUS_V1", NOW)
        self.assertEqual(first["command"], second["command"])
        self.assertNotEqual(first["idempotency_key"], second["idempotency_key"])

    def test_replay_key_stable_for_same_source_and_policy(self):
        a, b = synthetic(), synthetic()
        first = sut.prepare(a, "GITHUB_ACTIONS_PUBLIC_STATUS_V1", NOW)
        second = sut.prepare(b, "GITHUB_ACTIONS_PUBLIC_STATUS_V1", NOW)
        self.assertEqual(first["idempotency_key"], second["idempotency_key"])
        self.assertNotEqual(first["source_event_sha256"], second["source_event_sha256"])
        self.assertTrue(first["durable_deduplication_required"])

    def test_expired_event_denied(self):
        event = synthetic()
        event["expires_at_utc"] = "2026-11-07T11:30:00Z"
        with self.assertRaisesRegex(event_mod.EventError, "EXPIRY"):
            sut.prepare(event, "GITHUB_ACTIONS_PUBLIC_STATUS_V1", NOW)

    def test_unauthorized_external_escalation_denied(self):
        for value in ("PREPARE_CI_READ", "PREPARE_NOTIFICATION", "EXEC_SHELL"):
            with self.subTest(capability=value), self.assertRaises(event_mod.EventError):
                sut.prepare(synthetic("GMAIL_METADATA", value),
                            "GITHUB_ACTIONS_PUBLIC_STATUS_V1", NOW)

    def test_command_has_bounded_resource_requirements(self):
        d = sut.prepare(synthetic(), "GITHUB_ACTIONS_PUBLIC_STATUS_V1", NOW)
        cmd = d["command"]
        self.assertLessEqual(cmd["max_response_bytes"], 65536)
        self.assertLessEqual(cmd["timeout_seconds"], 8)
        self.assertFalse(cmd["follow_redirects"])
        self.assertFalse(cmd["attach_credentials"])
        self.assertTrue(d["quota_enforcement_required"])
        self.assertLessEqual(d["max_requests_per_hour"], 6)

    def test_never_converts_external_event_into_execution_claim(self):
        for key, value in (
            ("approval_state", "AUTO_APPROVED"),
            ("execution_state", "DONE"),
            ("claim_allowed", True),
            ("authority_ref", "verified-by-email-sender"),
        ):
            event = synthetic()
            event[key] = value
            with self.subTest(key=key), self.assertRaisesRegex(event_mod.EventError, "PROMOTION"):
                sut.prepare(event, "GITHUB_ACTIONS_PUBLIC_STATUS_V1", NOW)


if __name__ == "__main__":
    unittest.main()
