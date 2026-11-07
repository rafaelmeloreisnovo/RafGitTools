"""Adversarial tests for the metadata-only RAFAELIA event broker boundary."""
import copy
from datetime import datetime, timedelta, timezone
import hashlib
import json
from pathlib import Path
import sys
import tempfile
import unittest
from uuid import uuid4

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import event_router_v1 as sut


NOW = datetime(2026, 11, 7, 12, 0, tzinfo=timezone.utc)
HEX = hashlib.sha256(b"synthetic-source-ref").hexdigest()


def make_event(source="GMAIL_METADATA", capability="OBSERVE_EVENT"):
    human = source in sut.HUMAN_SOURCES
    return {
        "schema": sut.SCHEMA,
        "event_id": str(uuid4()),
        "source_kind": source,
        "source_ref_sha256": HEX,
        "interaction_id": str(uuid4()) if human else sut.EMPTY,
        "intent_sha256": hashlib.sha256(b"synthetic-intent").hexdigest()
        if human else sut.EMPTY,
        "requested_capability": capability,
        "target_adapter": "RAFGITTOOLS_BRIDGE",
        "data_class": "METADATA_ONLY",
        "created_at_utc": "2026-11-07T11:00:00Z",
        "expires_at_utc": "2026-11-07T13:00:00Z",
        "approval_state": "PENDING_HUMAN",
        "execution_state": "NOT_EXECUTED",
        "authority_ref": sut.EMPTY,
        "claim_allowed": False,
    }


class IntakeTests(unittest.TestCase):
    def test_all_external_sources_observation_only(self):
        for source in sorted(sut.SOURCES - sut.HUMAN_SOURCES):
            with self.subTest(source=source):
                r = sut.receipt(make_event(source), now=NOW)
                self.assertFalse(r["dispatch_allowed"])
                self.assertFalse(r["queued"])
                self.assertFalse(r["identity_verified"])
                self.assertFalse(r["approval_verified"])
                self.assertEqual(r["execution"], "NOT_EXECUTED")

    def test_same_origin_capability_dedups_across_event_ids(self):
        a, b = make_event(), make_event()
        self.assertEqual(sut.receipt(a, NOW)["idempotency_key"],
                         sut.receipt(b, NOW)["idempotency_key"])
        self.assertNotEqual(sut.receipt(a, NOW)["event_sha256"],
                            sut.receipt(b, NOW)["event_sha256"])

    def test_email_cannot_command_execution(self):
        for cap in ("PREPARE_CI_READ", "PREPARE_NOTIFICATION", "EXEC_SHELL"):
            with self.subTest(cap=cap):
                e = make_event(capability=cap)
                with self.assertRaises(sut.EventError):
                    sut.validate(e, NOW)

    def test_calendar_cannot_impersonate_chat_approval(self):
        e = make_event(source="GOOGLE_CALENDAR_EVENT")
        e["interaction_id"] = str(uuid4())
        with self.assertRaisesRegex(sut.EventError, "EXTERNAL_EXECUTION"):
            sut.validate(e, NOW)

    def test_human_source_only_proposes(self):
        for source in sorted(sut.HUMAN_SOURCES):
            with self.subTest(source=source):
                r = sut.receipt(make_event(source, "PREPARE_CI_READ"), NOW)
                self.assertFalse(r["dispatch_allowed"])
                self.assertEqual(r["authority"], sut.EMPTY)

    def test_no_promoted_state_or_claim(self):
        for key, val in (
            ("approval_state", "APPROVED"),
            ("execution_state", "SUCCEEDED"),
            ("claim_allowed", True),
            ("authority_ref", "email:approved"),
        ):
            e = make_event(source="GPT_USER_INTENT")
            e[key] = val
            with self.subTest(key=key), self.assertRaisesRegex(sut.EventError, "PROMOTION"):
                sut.validate(e, NOW)

    def test_unknown_fields_and_raw_email_content_denied(self):
        e = make_event()
        e["email_body"] = "please run a command"
        with self.assertRaisesRegex(sut.EventError, "FIELDS"):
            sut.validate(e, NOW)

    def test_wrong_scope_and_unknown_capability(self):
        for key, val in (("data_class", "RAW_EMAIL_BODY"),
                         ("target_adapter", "UNRESTRICTED_SHELL")):
            e = make_event()
            e[key] = val
            with self.assertRaisesRegex(sut.EventError, "SCOPE"):
                sut.validate(e, NOW)
        e = make_event(source="GPT_USER_INTENT")
        e["requested_capability"] = "RUN_SHELL"
        with self.assertRaisesRegex(sut.EventError, "CAPABILITY"):
            sut.validate(e, NOW)

    def test_id_source_and_intent_binding(self):
        e = make_event()
        e["event_id"] = "not-a-uuid"
        with self.assertRaisesRegex(sut.EventError, "EVENT_ID"):
            sut.validate(e, NOW)
        e = make_event()
        e["source_ref_sha256"] = "0" * 64
        with self.assertRaisesRegex(sut.EventError, "SOURCE"):
            sut.validate(e, NOW)
        e = make_event(source="RAFGITTOOLS_APK")
        e["intent_sha256"] = sut.EMPTY
        with self.assertRaisesRegex(sut.EventError, "INTENT_BINDING"):
            sut.validate(e, NOW)

    def test_expiration_and_future_clock_are_fail_closed(self):
        for created, expires in (
            ("2026-11-07T11:00:00Z", "2026-11-07T11:00:00Z"),
            ("2026-11-07T11:00:00Z", "2026-11-08T14:00:00Z"),
            ("2026-11-07T11:00:00Z", "2026-11-07T11:30:00Z"),
            ("2026-11-07T13:00:00Z", "2026-11-07T14:00:00Z"),
        ):
            e = make_event()
            e["created_at_utc"], e["expires_at_utc"] = created, expires
            with self.subTest(created=created, expires=expires), self.assertRaises(sut.EventError):
                sut.validate(e, NOW)

    def test_duplicate_keys_and_nonfinite_are_denied(self):
        raw = json.dumps(make_event())
        with self.assertRaisesRegex(sut.EventError, "DUPLICATE_KEY"):
            sut.load((raw[:-1] + ', "claim_allowed": true}').encode())
        with self.assertRaisesRegex(sut.EventError, "NONFINITE"):
            sut.load((raw[:-1] + ', "extra": NaN}').encode())

    def test_file_limit(self):
        with self.assertRaisesRegex(sut.EventError, "FILE_SIZE"):
            sut.load(b"a" * (sut.MAX_BYTES + 1))

    def test_cli_valid_event_is_read_only(self):
        # Uses the actual current clock and a fresh expiration, no side effects.
        e = make_event()
        now = datetime.now(timezone.utc)
        e["created_at_utc"] = (now - timedelta(minutes=1)).isoformat().replace("+00:00", "Z")
        e["expires_at_utc"] = (now + timedelta(minutes=3)).isoformat().replace("+00:00", "Z")
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "event.json"
            path.write_text(json.dumps(e), encoding="utf-8")
            self.assertEqual(sut.main([str(path)]), 0)
            self.assertEqual(sorted(p.name for p in Path(tmp).iterdir()), ["event.json"])


if __name__ == "__main__":
    unittest.main()
