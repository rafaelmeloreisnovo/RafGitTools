import subprocess
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


class DeliveryMapValidatorTest(unittest.TestCase):
    def test_delivery_map_contract(self):
        result = subprocess.run(
            [sys.executable, str(ROOT / "scripts" / "validate_delivery_map.py")],
            cwd=ROOT,
            text=True,
            capture_output=True,
            check=False,
        )
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)
        self.assertIn("development/delivery gate: PASS", result.stdout)


if __name__ == "__main__":
    unittest.main()
