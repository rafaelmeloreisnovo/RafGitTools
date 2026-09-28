import base64
import json
import tempfile
import unittest
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

import engine


class EngineTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.cfg = json.loads((ROOT / "config.v1.json").read_text(encoding="utf-8"))

    def test_schedule_advances_only_on_pass(self):
        state = dict(self.cfg["root_state"])
        held = engine.advance_schedule(self.cfg, state, "FAIL")
        self.assertEqual(held["phase_index"], 0)
        self.assertEqual(held["current_interval_minutes"], 1)
        advanced = engine.advance_schedule(self.cfg, state, "PASS")
        self.assertEqual(advanced["phase_index"], 1)
        self.assertEqual(advanced["current_interval_minutes"], 2)

    def test_boot_promotes_to_ramp(self):
        state = {"phase":"BOOT_1_5","phase_index":4,"phase_success_count":0,"current_interval_minutes":5,"last_outcome":"PASS"}
        new = engine.advance_schedule(self.cfg, state, "PASS")
        self.assertEqual(new["phase"], "RAMP_1_29")
        self.assertEqual(new["current_interval_minutes"], 1)

    def test_hourly_promotion_remains_token_vazio(self):
        state = {"phase":"HOURLY","phase_index":0,"phase_success_count":0,"current_interval_minutes":60,"last_outcome":"PASS"}
        new = engine.advance_schedule(self.cfg, state, "PASS")
        self.assertEqual(new["phase"], "HOURLY")
        self.assertEqual(new["advance_state"], "HELD_PROMOTION_RULE_TOKEN_VAZIO")

    def test_github_actions_blocks_sub_five_minute_cadence(self):
        gate = engine.backend_gate(self.cfg, "GITHUB_ACTIONS_SCHEDULE", 1)
        self.assertEqual(gate["state"], "BLOCKED")
        self.assertEqual(gate["effective_minutes"], engine.TOKEN_VAZIO)

    def test_intake_rejects_raw_content(self):
        pointer = {k:"x" for k in self.cfg["intake_contract"]["required_fields"]}
        pointer["claim_allowed"] = False
        pointer["raw_content"] = "secret text"
        out = engine.validate_intake(pointer, self.cfg)
        self.assertEqual(out["state"], "FAIL")

    def test_visual_anchor_does_not_ocr(self):
        png = base64.b64decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9Y9ZfM8AAAAASUVORK5CYII=")
        with tempfile.TemporaryDirectory() as d:
            p = Path(d)/"one.png"
            p.write_bytes(png)
            out = engine.visual_anchor(p)
        self.assertFalse(out["ocr_performed"])
        self.assertFalse(out["text_extracted"])
        self.assertEqual(out["width"], 1)
        self.assertEqual(out["height"], 1)
        self.assertEqual(len(out["digest_vector_u32"]), 8)


if __name__ == "__main__":
    unittest.main()
