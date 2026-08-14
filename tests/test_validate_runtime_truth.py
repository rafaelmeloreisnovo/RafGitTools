from __future__ import annotations

import unittest
from pathlib import Path

from scripts.validate_runtime_truth import check_workmanager_startup_contract


class WorkManagerStartupContractTest(unittest.TestCase):
    def setUp(self) -> None:
        root = Path(__file__).resolve().parents[1]
        self.manifest = (root / "app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
        self.application = (
            root / "app/src/main/kotlin/com/rafgittools/RafGitToolsApplication.kt"
        ).read_text(encoding="utf-8")

    def test_current_startup_contract_passes(self) -> None:
        check_workmanager_startup_contract(self.manifest, self.application)

    def test_removed_default_initializer_requires_replacement(self) -> None:
        disabled_initializer = self.manifest.replace(
            "</application>",
            """        <provider
            android:name=\"androidx.startup.InitializationProvider\"
            android:authorities=\"${applicationId}.androidx-startup\"
            android:exported=\"false\"
            tools:node=\"merge\">
            <meta-data
                android:name=\"androidx.work.WorkManagerInitializer\"
                android:value=\"androidx.startup\"
                tools:node=\"remove\" />
        </provider>
    </application>""",
            1,
        )

        with self.assertRaises(AssertionError):
            check_workmanager_startup_contract(disabled_initializer, self.application)


if __name__ == "__main__":
    unittest.main()
