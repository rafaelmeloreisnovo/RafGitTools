"""Synthetic, no-network adversarial contract checks."""
import unittest
from scripts.validate_session_queue_preflight_v1 import (
    SCHEMA, MAPA_COMMIT, MAPA_BLOB, digest, validate, allowed_transition,
)


def candidate():
    item = dict(
        schema=SCHEMA, envelope_id="1" * 32, session_id="2" * 32,
        target_id="3" * 32, packet_id="SP08_SESSION_TO_WORK_PACKETS",
        mapa_commit=MAPA_COMMIT, mapa_blob=MAPA_BLOB,
        operation="PLAN_ONLY", state="IN", privacy_class="UNKNOWN_REVIEW_REQUIRED",
        payload_mode="NONE", execution_enabled=False,
        human_approved=False, claim_allowed=False, idempotency_key="",
    )
    item["idempotency_key"] = digest(item)
    return item


class SessionQueuePreflightTests(unittest.TestCase):
    def test_synthetic_valid_still_cannot_dispatch(self):
        item = candidate()
        self.assertEqual(validate(item), [])
        self.assertFalse(item["execution_enabled"])
        self.assertFalse(item["claim_allowed"])

    def test_idempotency_survives_status_and_privacy_classification(self):
        item = candidate()
        key = item["idempotency_key"]
        item["state"] = "GATES"
        item["privacy_class"] = "PRIVATE_REQUIRED"
        self.assertEqual(digest(item), key)
        self.assertEqual(validate(item), [])

    def test_replay_idempotency_is_session_bound(self):
        item = candidate()
        item["session_id"] = "4" * 32
        self.assertIn("IDEMPOTENCY_BLOCKED", validate(item))

    def test_unknown_field_or_payload_rejected(self):
        item = candidate()
        item["private_repo"] = "sensitive"
        self.assertEqual(validate(item), ["SCHEMA_FIELDS_BLOCKED"])

    def test_private_body_cannot_enter_preflight(self):
        item = candidate()
        item["payload_mode"] = "RAW"
        item["idempotency_key"] = digest(item)
        self.assertIn("PAYLOAD_BLOCKED", validate(item))

    def test_execution_flag_never_authorized(self):
        item = candidate()
        item["execution_enabled"] = True
        item["idempotency_key"] = digest(item)
        self.assertIn("PROMOTION_BLOCKED", validate(item))

    def test_authority_pin_mismatch(self):
        item = candidate()
        item["mapa_blob"] = "0" * 40
        item["idempotency_key"] = digest(item)
        self.assertIn("AUTHORITY_PIN_BLOCKED", validate(item))

    def test_unrecognized_privacy_blocks(self):
        item = candidate()
        item["privacy_class"] = "PUBLIC"
        item["idempotency_key"] = digest(item)
        self.assertIn("PRIVACY_BLOCKED", validate(item))

    def test_no_processing_or_terminal_claim(self):
        item = candidate()
        item["state"] = "PROCESSING"
        item["idempotency_key"] = digest(item)
        self.assertIn("EXECUTION_STATE_BLOCKED", validate(item))

    def test_isolated_session_id_is_opaque(self):
        item = candidate()
        item["session_id"] = "chat-with-personal-data"
        item["idempotency_key"] = digest(item)
        self.assertIn("OPAQUE_ID_BLOCKED", validate(item))

    def test_transition_requires_later_broker(self):
        self.assertTrue(allowed_transition("IN", "GATES"))
        self.assertTrue(allowed_transition("IN", "HOLD"))
        self.assertFalse(allowed_transition("GATES", "PROCESSING"))
        self.assertFalse(allowed_transition("HOLD", "PROCESSED"))

    def test_wrong_field_types_cannot_pass(self):
        item = candidate()
        item["claim_allowed"] = 0
        item["idempotency_key"] = digest(item)
        self.assertIn("PROMOTION_BLOCKED", validate(item))


    def test_array_types_fail_cleanly(self):
        item = candidate()
        item["state"] = ["IN"]
        item["privacy_class"] = ["PRIVATE_REQUIRED"]
        item["idempotency_key"] = digest(item)
        result = validate(item)
        self.assertIn("EXECUTION_STATE_BLOCKED", result)
        self.assertIn("PRIVACY_BLOCKED", result)

    def test_duplicate_json_keys_blocked_without_echo(self):
        import contextlib
        import io
        import json
        import tempfile
        from pathlib import Path
        from scripts.validate_session_queue_preflight_v1 import main
        with tempfile.TemporaryDirectory() as tmp:
            src = Path(tmp) / "intent.json"
            raw = json.dumps(candidate())
            raw = raw.replace('"operation": "PLAN_ONLY"',
                              '"operation": "PLAN_ONLY", "operation": "PLAN_ONLY"')
            src.write_text(raw, encoding="utf-8")
            out = io.StringIO()
            with contextlib.redirect_stdout(out):
                code = main(["preflight", str(src)])
            self.assertEqual(code, 2)
            self.assertIn('"state":"HOLD"', out.getvalue())
            self.assertNotIn("PLAN_ONLY", out.getvalue())

    def test_large_input_is_rejected(self):
        import contextlib
        import io
        import tempfile
        from pathlib import Path
        from scripts.validate_session_queue_preflight_v1 import main
        with tempfile.TemporaryDirectory() as tmp:
            src = Path(tmp) / "large.json"
            src.write_bytes(b"X" * 6000)
            with contextlib.redirect_stdout(io.StringIO()):
                self.assertEqual(main(["preflight", str(src)]), 2)



if __name__ == "__main__":
    unittest.main()
