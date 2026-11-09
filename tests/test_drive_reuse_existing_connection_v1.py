"""Single system folder picker -> automatic same-tree Drive scanner contract.

Source-only regression, NOT proof that Motorola DocumentsUI exposes Drive folders.
"""
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ENTRY = ROOT / "app/src/main/kotlin/com/rafgittools/ui/screens/setupwizard/DriveFirstRunIndexCard.kt"
HOME = ROOT / "app/src/main/kotlin/com/rafgittools/ui/screens/home/HomeScreen.kt"
WIZARD = ROOT / "app/src/main/kotlin/com/rafgittools/ui/screens/setupwizard/SetupWizardScreen.kt"
WORKFLOW = ROOT / ".github/workflows/rafaelia-agent-workspace-scoped.yml"


class DirectDrivePickerTests(unittest.TestCase):
    def test_exactly_one_system_folder_picker_no_extra_oauth(self):
        text = ENTRY.read_text(encoding="utf-8").split("fun DriveLegacyAdvancedIndexCard()", 1)[0]
        self.assertEqual(1, text.count("ActivityResultContracts.OpenDocumentTree()"))
        self.assertIn("drivePicker.launch(previousDriveFolder)", text)
        self.assertIn("DocumentsContract.isTreeUri(uri)", text)
        self.assertIn("GoogleDriveSafGate.isGoogleDrive(context, uri)", text)
        self.assertNotIn("Identity.getAuthorizationClient", text)
        self.assertNotIn("GoogleDriveCloudCard()", text)
        self.assertNotIn("DriveAlreadyConnectedScanCard(", text)

    def test_selection_automatically_scans_then_writes_to_same_drive_tree(self):
        text = ENTRY.read_text(encoding="utf-8").split("fun DriveLegacyAdvancedIndexCard()", 1)[0]
        self.assertIn("NovoexportSafInventory.scan(", text)
        self.assertIn("DriveAtlasSafPublisherV3.publish(", text)
        self.assertIn("DriveAtlasSafPublisherV3.publish(resolver, uri, inventory)", text)
        self.assertLess(text.index("NovoexportSafInventory.scan("),
                        text.index("DriveAtlasSafPublisherV3.publish("))
        self.assertNotIn("privateDestinationConfirmed", text)
        self.assertNotIn("optimizedFolderPicker", text)
        self.assertIn("DRIVE_HYPERATLAS_INDEX_READBACK_VERIFIED", text)

    def test_rejects_phone_provider_and_reuses_initial_drive_location_hint(self):
        text = ENTRY.read_text(encoding="utf-8").split("fun DriveLegacyAdvancedIndexCard()", 1)[0]
        self.assertIn("BLOCKED_SELECTED_FOLDER_NOT_GOOGLE_DRIVE", text)
        self.assertIn("persistedUriPermissions", text)
        self.assertIn("Intent.FLAG_GRANT_READ_URI_PERMISSION", text)
        self.assertIn("Intent.FLAG_GRANT_WRITE_URI_PERMISSION", text)
        self.assertIn("takePersistableUriPermission(", text)
        self.assertIn("BLOCKED_DRIVE_SAF_READ_", text)
        self.assertIn("BLOCKED_DRIVE_SAF_WRITE_", text)
        self.assertLess(text.index("NovoexportSafInventory.scan("),
                        text.index("DriveAtlasSafPublisherV3.publish("))
        self.assertNotIn("ActivityResultContracts.CreateDocument", text)

    def test_visible_home_and_wizard_only_use_direct_entry(self):
        for path in (HOME, WIZARD):
            self.assertIn("DriveFirstRunIndexCard()", path.read_text(encoding="utf-8"))
        self.assertNotIn("showLocalFallback", HOME.read_text(encoding="utf-8"))

    def test_google_drive_provider_identity_keeps_fail_closed_fallback(self):
        gate = (ROOT / "app/src/main/kotlin/com/rafgittools/setupwizard/GoogleDriveSafGate.kt").read_text(encoding="utf-8")
        self.assertIn("resolveContentProvider(authority, 0)", gate)
        self.assertIn("getPackageInfo(", gate)
        self.assertIn("PackageManager.GET_PROVIDERS", gate)
        self.assertIn("isDeclaredByGoogleDrive(authority,", gate)
        self.assertNotIn('authority.contains("google")', gate)

    def test_cloud_documents_can_have_unknown_size(self):
        inventory = (ROOT / "app/src/main/kotlin/com/rafgittools/navigator/NovoexportSafInventory.kt").read_text(encoding="utf-8")
        self.assertIn("getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)", inventory)
        self.assertIn("sizeIndex < 0 || it.isNull(sizeIndex)", inventory)
        self.assertNotIn("getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)", inventory)

    def test_scan_failure_does_not_hide_successful_read(self):
        text = ENTRY.read_text(encoding="utf-8")
        self.assertIn("DRIVE_SCAN_PASS_INDEX_WRITE_BLOCKED", text)
        self.assertIn("BLOCKED_DRIVE_SAF_READ_", text)
        self.assertIn("uri, Intent.FLAG_GRANT_READ_URI_PERMISSION", text)
        self.assertNotIn("takePersistableUriPermission(uri, permissionFlags)", text)

    def test_source_changes_trigger_real_apk_build(self):
        yaml = WORKFLOW.read_text(encoding="utf-8")
        self.assertIn("DriveFirstRunIndexCard.kt", yaml)
        self.assertIn("assembleDevDebug", yaml)


if __name__ == "__main__":
    unittest.main()
