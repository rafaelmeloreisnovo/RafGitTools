"""Source contract checks only; does NOT execute Android, API or credentials."""
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
VM = ROOT / "app/src/main/kotlin/com/rafgittools/ui/screens/home/HomeViewModel.kt"
UI = ROOT / "app/src/main/kotlin/com/rafgittools/ui/screens/home/HomeScreen.kt"
PROOF = ROOT / "app/src/main/kotlin/com/rafgittools/diagnostics/DeviceProofV1.kt"


class LocalDeviceEvidenceSourceTests(unittest.TestCase):
    def test_login_change_observation(self):
        src = VM.read_text(encoding="utf8")
        self.assertIn("authRepository.isAuthenticatedFlow", src)
        self.assertIn("authRepository.usernameFlow", src)
        self.assertIn(".distinctUntilChanged()", src)
        self.assertIn("checkAuthAndLoadData()", src)
        self.assertIn("val epoch = ++sessionEpoch", src)
        self.assertIn("remoteLoadJob?.cancel()", src)
        self.assertIn("authLoadJob?.cancel()", src)
        self.assertIn("if (epoch != sessionEpoch || !isActive", src)
        self.assertIn("if (!wasAuthenticated) _activeTab.value = HomeTab.REMOTE", src)

    def test_export_needs_user_and_saf(self):
        ui = UI.read_text(encoding="utf8")
        self.assertIn("Exportar prova local", ui)
        self.assertIn('CreateDocument("application/json")', ui)
        self.assertIn("captureDeviceEvidence", ui)
        self.assertIn("contentResolver.openOutputStream", ui)
        self.assertIn("contentResolver.openInputStream", ui)
        self.assertIn("MessageDigest.isEqual", ui)
        self.assertNotIn("githubRepository.createPrivateProcessingFile", ui)

    def test_private_self_report_boundary(self):
        src = PROOF.read_text(encoding="utf8")
        for expected in (
            '"rafaelia.public-proof-envelope/v1"',
            '"rafaelia.rafgittools.device-evidence/v1"',
            '"ANDROID_APP_SELF_REPORT"',
            '"TOKEN_VAZIO"',
            'put("claim_allowed", false)',
            'MessageDigest.getInstance("SHA-256")',
        ):
            self.assertIn(expected, src)
        for forbidden in (
            'put("token",', 'put("username",', 'put("email",',
            'put("device_id",', 'Build.SERIAL', 'Build.MODEL', 'Settings.Secure.ANDROID_ID'
        ):
            self.assertNotIn(forbidden, src)


if __name__ == "__main__":
    unittest.main()
