"""Read-only static contract: provider scope, privacy boundary, no Android runtime claim."""
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
API = ROOT / "app/src/main/kotlin/com/rafgittools/data/github/GithubApiService.kt"
DATA = ROOT / "app/src/main/kotlin/com/rafgittools/data/github/GithubRepository.kt"
HOME = ROOT / "app/src/main/kotlin/com/rafgittools/ui/screens/home/HomeViewModel.kt"
UI = ROOT / "app/src/main/kotlin/com/rafgittools/ui/screens/home/HomeScreen.kt"
PICKER = ROOT / "app/src/main/kotlin/com/rafgittools/ui/components/GithubOwnerPicker.kt"
GUARD = ROOT / "app/src/main/kotlin/com/rafgittools/data/github/GithubOrganizationInventory.kt"


class OrganizationPrivateSourceContract(unittest.TestCase):
    def test_github_affiliation_and_org_endpoint(self):
        src = API.read_text(encoding="utf8")
        auth = src.split('@GET("user/repos")')[1].split('@GET("user/orgs")')[0]
        self.assertIn('"owner,collaborator,organization_member"', auth)
        self.assertNotIn('@Query("type")', auth)
        self.assertIn('@GET("orgs/{organization}/repos")', src)
        self.assertIn('@GET("user/orgs")', src)

    def test_live_inventory_no_cached_success(self):
        src = DATA.read_text(encoding="utf8")
        for marker in ("getUserRepositoriesLivePage", "getOrganizationRepositoriesLivePage",
                       "requireScopedOwner", "getAuthenticatedOrganizationsLivePage"):
            self.assertIn(marker, src)
        org = src.split("suspend fun getOrganizationRepositoriesLivePage", 1)[1].split(
            "fun getCachedRepositoriesFlow", 1
        )[0]
        self.assertNotIn("cachedRepos", org)
        self.assertNotIn("getCachedRepositories", org)

    def test_owner_filter_and_private_destination(self):
        home = UI.read_text(encoding="utf8")
        self.assertIn("onLookupOrganization = { org ->", home)
        self.assertIn("onLoadOrganization(org)", home)
        self.assertIn("GithubOwnerScope.filter(remoteRepositories, selectedOwner)", home)
        # Drive now uses its own existing Google Drive permission; it must not
        # promote GitHub private-repo targets or pass unfiltered repository lists.
        self.assertIn("HomeViewModel.HomeTab.DRIVE, HomeViewModel.HomeTab.LOCAL ->", home)
        self.assertIn("GoogleDriveScannerEntry()", home)
        self.assertNotIn("privateRepositories = remoteRepositories", home)
        self.assertIn("canWriteDiagnosticReceipt = selectedOwner == null ||", home)

    def test_partial_failure_stays_partial_and_cancels_old_job(self):
        src = HOME.read_text(encoding="utf8")
        for marker in ("getUserRepositoriesLivePage", "getOrganizationRepositoriesLivePage",
                       "orgListJob?.cancel()", "authSnapshot != authGeneration",
                       '"ORG_PARTIAL_OR_PERMISSION_BLOCKED"', "requestedOrganization"):
            self.assertIn(marker, src)
        self.assertIn("_organizationStatus.value = \"USER_REPOSITORY_LIST_UNAVAILABLE\"", src)

    def test_org_input_is_explicit_and_scoped(self):
        picker = PICKER.read_text(encoding="utf8")
        guard = GUARD.read_text(encoding="utf8")
        self.assertIn("GithubOrganizationInventory.isValidLogin", picker)
        self.assertIn("onLookupOrganization", picker)
        self.assertIn("require(repositories.all", guard)
        self.assertIn('"ORGANIZATION_OWNER_MISMATCH"', guard)
        self.assertNotIn("github_pat_", guard)

    def test_no_access_promotion(self):
        src = DATA.read_text(encoding="utf8")
        ui = UI.read_text(encoding="utf8")
        self.assertNotIn("setRepositoryVisibility(", src)
        self.assertIn("onBuildPrivateGitHubIndex", ui)


if __name__ == "__main__":
    unittest.main()
