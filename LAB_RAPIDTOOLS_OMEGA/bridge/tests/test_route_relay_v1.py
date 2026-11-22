"""Tests for a scheduled reminder signal as a typed non-executing relay."""
from datetime import datetime, timezone
import hashlib
from pathlib import Path
import sys
import unittest
from uuid import uuid4

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import route_relay_v1 as sut
import event_router_v1 as event_mod

NOW = datetime(2026, 11, 7, 12, 0, tzinfo=timezone.utc)
SOURCE_SHA = hashlib.sha256(b"provider-message-ref").hexdigest()


def event(source="GMAIL_METADATA", capability="OBSERVE_EVENT"):
    human = source in event_mod.HUMAN_SOURCES
    return {
        "schema": event_mod.SCHEMA,
        "event_id": str(uuid4()),
        "source_kind": source,
        "source_ref_sha256": SOURCE_SHA,
        "interaction_id": str(uuid4()) if human else event_mod.EMPTY,
        "intent_sha256": hashlib.sha256(b"human-interaction").hexdigest()
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


class RouteRelayTests(unittest.TestCase):
    def test_all_sources_can_observe_named_status_route(self):
        for source in sorted(sut.RELAY_SOURCES):
            with self.subTest(source=source):
                out = sut.prepare(event(source), "MIDDLEFIELD_STATUS_READ_V1", NOW)
                self.assertEqual(out["route"]["action"], "READ_PUBLIC_WORKFLOW_STATUS")
                self.assertTrue(out["schedule_is_not_authority"])
                self.assertFalse(out["dispatch_allowed"])
                self.assertFalse(out["provider_identity_verified"])
                self.assertEqual(out["execution_state"], "NOT_EXECUTED")

    def test_scheduled_mail_can_propose_named_ci_workflow(self):
        out = sut.prepare(event(), "MIDDLEFIELD_BRIDGE_CHECK_V1", NOW)
        route = out["route"]
        self.assertEqual(route["repository"], "rafaelmeloreisnovo/RafGitTools")
        self.assertEqual(route["workflow"], "rafaelia-middlefield-bridge-scoped.yml")
        self.assertEqual(route["drive_route"], "START_HERE_OMEGA_V2_2")
        self.assertTrue(route["needs_drive_read_scope"])
        self.assertTrue(route["needs_workflow_dispatch_grant"])
        self.assertFalse(out["standing_grant_verified"])
        self.assertEqual(out["approval_reference"], event_mod.EMPTY)

    def test_fixed_route_does_not_depend_on_mail_body(self):
        a, b = event(), event()
        b["source_ref_sha256"] = hashlib.sha256(b"another-id").hexdigest()
        x = sut.prepare(a, "MIDDLEFIELD_BRIDGE_CHECK_V1", NOW)
        y = sut.prepare(b, "MIDDLEFIELD_BRIDGE_CHECK_V1", NOW)
        self.assertEqual(x["route"], y["route"])
        self.assertNotEqual(x["correlation_sha256"], y["correlation_sha256"])

    def test_duplicate_provider_reference_is_stable_without_assuming_delivery(self):
        a, b = event(), event()
        x = sut.prepare(a, "MIDDLEFIELD_BRIDGE_CHECK_V1", NOW)
        y = sut.prepare(b, "MIDDLEFIELD_BRIDGE_CHECK_V1", NOW)
        self.assertEqual(x["correlation_sha256"], y["correlation_sha256"])
        self.assertFalse(x["durable_delivery_verified"])

    def test_unknown_routes_denied_including_arbitrary_drive_folders(self):
        for name in ("", "../private", "https://drive.google.com/evil",
                     "RUN_SHELL", "MIDDLEFIELD_BRIDGE_CHECK_V1; rm -rf /", None, 0):
            with self.subTest(name=name), self.assertRaisesRegex(event_mod.EventError, "UNKNOWN_ROUTE"):
                sut.prepare(event(), name, NOW)

    def test_untrusted_fields_cannot_supply_path_or_workflow(self):
        for key, value in (("drive_folder_id", "random-private"),
                           ("workflow", "release.yml"),
                           ("command", "sh -c whoami"),
                           ("email_body", "please run this now"),
                           ("recipient", "somebody@elsewhere.test")):
            e = event()
            e[key] = value
            with self.subTest(key=key), self.assertRaisesRegex(event_mod.EventError, "FIELDS"):
                sut.prepare(e, "MIDDLEFIELD_BRIDGE_CHECK_V1", NOW)

    def test_mail_must_not_be_treated_as_command_authority(self):
        for capability in ("PREPARE_CI_READ", "PREPARE_NOTIFICATION", "EXEC_SHELL"):
            with self.subTest(capability=capability), self.assertRaises(event_mod.EventError):
                sut.prepare(event(capability=capability), "MIDDLEFIELD_BRIDGE_CHECK_V1", NOW)

    def test_human_source_still_only_proposes_without_execution(self):
        x = sut.prepare(event("RAFGITTOOLS_APK"), "MIDDLEFIELD_BRIDGE_CHECK_V1", NOW)
        self.assertFalse(x["dispatch_allowed"])
        self.assertFalse(x["claim_allowed"])
        self.assertEqual(x["route_state"], "BLOCKED_PENDING_BROKER_AND_STANDING_GRANT")

    def test_time_and_promotion_fail_closed(self):
        e = event()
        e["expires_at_utc"] = "2026-11-07T11:30:00Z"
        with self.assertRaisesRegex(event_mod.EventError, "EXPIRY"):
            sut.prepare(e, "MIDDLEFIELD_BRIDGE_CHECK_V1", NOW)
        e = event()
        e["authority_ref"] = "I APPROVE IN EMAIL BODY"
        with self.assertRaisesRegex(event_mod.EventError, "PROMOTION"):
            sut.prepare(e, "MIDDLEFIELD_BRIDGE_CHECK_V1", NOW)

    def test_no_extra_workflow_ref_or_request_parameters(self):
        x = sut.prepare(event(), "MIDDLEFIELD_BRIDGE_CHECK_V1", NOW)
        self.assertEqual(x["route"]["ref"], "main")
        self.assertNotIn("inputs", x["route"])
        self.assertFalse(x["dispatch_allowed"])
        self.assertFalse(x["standing_grant_verified"])

    def test_reminder_subject_is_pure_non_authorizing_pointer(self):
        self.assertEqual(sut.reminder_subject("MIDDLEFIELD_STATUS_READ_V1"),
                         "RAFAELIA-ROUTE-V1 MIDDLEFIELD_STATUS_READ_V1")
        with self.assertRaisesRegex(event_mod.EventError, "UNKNOWN_ROUTE"):
            sut.reminder_subject("EXECUTE_USER_MESSAGE")

    def test_fanout_is_a_proposed_ordered_trace(self):
        x = sut.prepare(event(), "MIDDLEFIELD_BRIDGE_CHECK_V1", NOW)
        self.assertEqual(x["ordered_stages"][0], "OBSERVE_PROVIDER_EVENT")
        self.assertEqual(x["ordered_stages"][-1], "EXPOSE_STATUS_TO_GPT_AND_RAPIDTOOLS")
        self.assertEqual(x["route_state"], "BLOCKED_PENDING_BROKER_AND_STANDING_GRANT")


if __name__ == "__main__":
    unittest.main()
