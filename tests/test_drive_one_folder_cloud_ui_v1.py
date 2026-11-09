"""Single-folder Google Drive index flow, one Android folder picker.

No second picker/login/write button; no mobile-local destination.
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
UI = ROOT / "app/src/main/kotlin/com/rafgittools/ui/screens/setupwizard/DriveFirstRunIndexCard.kt"
HOME = ROOT / "app/src/main/kotlin/com/rafgittools/ui/screens/home/HomeScreen.kt"
SETUP = ROOT / "app/src/main/kotlin/com/rafgittools/ui/screens/setupwizard/SetupWizardScreen.kt"
WORKFLOW = ROOT / ".github/workflows/rafaelia-agent-workspace-scoped.yml"


class DriveOneFolderCloudUiTests(unittest.TestCase):
    def test_one_folder_picker_runs_scan_and_publish_automatically(self):
        text = UI.read_text(encoding="utf-8").split("fun DriveLegacyAdvancedIndexCard()", 1)[0]
        self.assertEqual(1, text.count("ActivityResultContracts.OpenDocumentTree()"))
        self.assertIn("NovoexportSafInventory.scan(", text)
        self.assertIn("DriveAtlasSafPublisherV3.publish(", text)
        self.assertIn("DriveAtlasSafPublisherV3.publish(resolver, uri, inventory)", text)
        self.assertNotIn("privateDestinationConfirmed", text)
        self.assertNotIn("GoogleDriveCloudCard()", text)

    def test_active_navigation_is_direct_picker_not_local_fallback(self):
        home = HOME.read_text(encoding="utf-8")
        setup = SETUP.read_text(encoding="utf-8")
        self.assertIn("HomeViewModel.HomeTab.DRIVE, HomeViewModel.HomeTab.LOCAL ->", home)
        self.assertIn("DriveFirstRunIndexCard()", home)
        self.assertIn("DriveFirstRunIndexCard()", setup)
        self.assertIn('Text(if (busy) "Indexando no Google Drive…" else "Selecionar pasta do Google Drive")',
                      UI.read_text(encoding="utf-8"))
        self.assertNotIn("showLocalFallback", home[:home.index("private suspend fun stageDriveDocument")])

    def test_scoped_workflow_covers_direct_picker_changes(self):
        flow = WORKFLOW.read_text(encoding="utf-8")
        self.assertIn("DriveFirstRunIndexCard.kt", flow)
        self.assertIn("assembleDevDebug", flow)


if __name__ == "__main__":
    unittest.main()
