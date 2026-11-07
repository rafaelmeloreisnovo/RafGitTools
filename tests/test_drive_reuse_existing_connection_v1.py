"""Static contract for previously authorized Google Drive folder grants.

This is a source regression gate, NOT runtime validation on a Motorola or
evidence of a Drive account connected inside the currently installed APK.
"""
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ENTRY = ROOT / "app/src/main/kotlin/com/rafgittools/ui/screens/setupwizard/DriveExistingGrantScanCard.kt"
CLOUD = ROOT / "app/src/main/kotlin/com/rafgittools/ui/screens/setupwizard/GoogleDriveCloudCard.kt"
HOME = ROOT / "app/src/main/kotlin/com/rafgittools/ui/screens/home/HomeScreen.kt"
WIZARD = ROOT / "app/src/main/kotlin/com/rafgittools/ui/screens/setupwizard/SetupWizardScreen.kt"
WORKFLOW = ROOT / ".github/workflows/rafaelia-agent-workspace-scoped.yml"


class ReuseExistingDriveConnectionTests(unittest.TestCase):
    def test_reuses_persisted_google_drive_tree_only(self):
        text = ENTRY.read_text(encoding="utf-8")
        for token in (
            "persistedUriPermissions", "it.isReadPermission",
            "DocumentsContract.isTreeUri(uri)",
            "GoogleDriveSafGate.isGoogleDrive(context, uri)",
            "DriveAtlasSafPublisherV3.publish(",
            "NovoexportSafInventory.scan(",
        ):
            self.assertIn(token, text)
        self.assertNotIn("ActivityResultContracts.OpenDocumentTree()", text)

    def test_existing_grant_never_falls_through_new_oauth(self):
        text = ENTRY.read_text(encoding="utf-8")
        self.assertIn("if (grants.isNotEmpty())", text)
        self.assertIn("DriveAlreadyConnectedScanCard(grants)", text)
        self.assertIn("} else {", text)
        self.assertIn("GoogleDriveCloudCard()", text)
        self.assertIn('Text("Escanear esta pasta do Google Drive")', text)

    def test_home_and_setup_route_through_one_existing_connection_entry(self):
        for file in (HOME, WIZARD):
            self.assertIn("GoogleDriveScannerEntry()", file.read_text(encoding="utf-8"))
        self.assertNotIn("showLocalFallback", HOME.read_text(encoding="utf-8"))

    def test_google_oauth_reuse_is_silent_until_consent_needed(self):
        text = CLOUD.read_text(encoding="utf-8")
        self.assertIn("LaunchedEffect(Unit)", text)
        self.assertIn("if (!answer.hasResolution())", text)
        self.assertIn("acceptAuthorization(answer.accessToken)", text)
        self.assertIn("DRIVE_GOOGLE_CONSENT_REQUIRED_FOR_THIS_APP", text)
        self.assertNotIn("Reconectar / trocar conta Google", text)

    def test_affected_source_triggers_real_apk_build(self):
        yaml = WORKFLOW.read_text(encoding="utf-8")
        self.assertIn("DriveExistingGrantScanCard.kt", yaml)
        self.assertIn("GoogleDriveCloudCard.kt", yaml)
        self.assertIn("assembleDevDebug", yaml)


if __name__ == "__main__":
    unittest.main()
