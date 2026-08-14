from __future__ import annotations

import shutil
import tempfile
import unittest
from pathlib import Path

from scripts.validate_rafgitfs_industrial_closeout import FILES, ValidationError, validate

class IndustrialCloseoutGateTest(unittest.TestCase):
    def setUp(self):
        self.source = Path(__file__).resolve().parents[1]
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)
        for relative in FILES:
            target = self.root / relative
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(self.source / relative, target)

    def tearDown(self): self.temp.cleanup()

    def mutate(self, relative, old, new):
        path = self.root / relative
        text = path.read_text(encoding="utf-8")
        self.assertIn(old, text)
        path.write_text(text.replace(old, new, 1), encoding="utf-8")

    def test_current_tree_passes(self):
        self.assertEqual("PASS", validate(self.root)["status"])

    def test_runtime_security_bypass_is_rejected(self):
        self.mutate(FILES[4], "securityGate.assessAfterExactApproval(plan)", "securityGate.toString()")
        with self.assertRaises(ValidationError): validate(self.root)

    def test_accessibility_live_region_removal_is_rejected(self):
        self.mutate(FILES[5], "LiveRegionMode.Polite", "LiveRegionMode.None")
        with self.assertRaises(ValidationError): validate(self.root)

    def test_machine_claim_promotion_is_rejected(self):
        self.mutate(FILES[13], '"claim_allowed": false', '"claim_allowed": true')
        with self.assertRaises(ValidationError): validate(self.root)

    def test_production_ready_promotion_is_rejected(self):
        self.mutate(FILES[13], '"production_ready": false', '"production_ready": true')
        with self.assertRaises(ValidationError): validate(self.root)

    def test_android_gap_removal_is_rejected(self):
        self.mutate(FILES[13], '"android_device_execution",', '"removed_android_gap",')
        with self.assertRaises(ValidationError): validate(self.root)

    def test_unmeasured_metrics_must_stay_token_vazio(self):
        self.mutate(FILES[2], "sample.observedMillis == null", "false")
        with self.assertRaises(ValidationError): validate(self.root)

    def test_security_control_removal_is_rejected(self):
        self.mutate(FILES[0], '"SEC-FORCE-010"', '"REMOVED-FORCE-CONTROL"')
        with self.assertRaises(ValidationError): validate(self.root)

if __name__ == "__main__": unittest.main()
