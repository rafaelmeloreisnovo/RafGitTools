"""Static regression checks: a single cloud folder, never Android SAF in Home/Setup.

These tests prove SOURCE UI routing only; Google OAuth, Drive API and device
runtime remain separate evidence gates.
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
UI = ROOT / "app/src/main/kotlin/com/rafgittools/ui/screens/setupwizard/GoogleDriveCloudCard.kt"
HOME = ROOT / "app/src/main/kotlin/com/rafgittools/ui/screens/home/HomeScreen.kt"
SETUP = ROOT / "app/src/main/kotlin/com/rafgittools/ui/screens/setupwizard/SetupWizardScreen.kt"
WORKFLOW = ROOT / ".github/workflows/rafaelia-agent-workspace-scoped.yml"


class DriveOneFolderCloudUiTests(unittest.TestCase):
    def test_one_folder_only_is_the_source_and_destination(self):
        text = UI.read_text(encoding="utf-8")
        self.assertIn("drive.publish(token, selectedFolder, selectedFolder)", text)
        self.assertIn('Text("Indexar esta pasta no Google Drive")', text)
        self.assertNotIn("Usar esta pasta do Drive como origem", text)
        self.assertNotIn("Usar esta pasta do Drive como destino", text)

    def test_active_navigation_is_cloud_not_phone(self):
        home = HOME.read_text(encoding="utf-8")
        setup = SETUP.read_text(encoding="utf-8")
        self.assertIn("HomeViewModel.HomeTab.DRIVE, HomeViewModel.HomeTab.LOCAL ->", home)
        self.assertIn("GoogleDriveCloudCard()", home)
        self.assertIn("GoogleDriveCloudCard()", setup)
        self.assertNotIn("showLocalFallback", home[:home.index("private suspend fun stageDriveDocument")])
        cloud = UI.read_text(encoding="utf-8")
        self.assertNotIn("ActivityResultContracts.OpenDocumentTree()", cloud)
        self.assertIn("Identity.getAuthorizationClient(context)", cloud)

    def test_cloud_ui_changes_trigger_real_apk_canary(self):
        flow = WORKFLOW.read_text(encoding="utf-8")
        self.assertIn("'app/src/main/kotlin/com/rafgittools/ui/screens/setupwizard/GoogleDriveCloudCard.kt'", flow)
        self.assertIn("'app/src/main/kotlin/com/rafgittools/data/drive/GoogleDriveCloudRepository.kt'", flow)
        self.assertIn("assembleDevDebug", flow)


if __name__ == "__main__":
    unittest.main()
