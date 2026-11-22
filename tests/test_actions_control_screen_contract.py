"""Static and adversarial interface contracts for Raf Actions console."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "app/src/main/kotlin/com/rafgittools"
API = APP / "data/github/GithubApiService.kt"
MODELS = APP / "data/github/ActionsControlModels.kt"
VM = APP / "ui/screens/actions/ActionsControlViewModel.kt"
UI = APP / "ui/screens/actions/ActionsControlScreen.kt"
ENTRY = APP / "ActionsControlActivity.kt"
MANIFEST = ROOT / "app/src/main/AndroidManifest.xml"
HOME = APP / "ui/screens/home/HomeScreen.kt"
NAV = APP / "MainActivity.kt"


class ActionsControlContractTests(unittest.TestCase):
    def test_four_writes_all_pin_to_numeric_provider_id(self):
        api = API.read_text()
        for route in (
            "/actions/workflows/{workflow_id}/dispatches",
            "/actions/runs/{run_id}/cancel",
            "/actions/runs/{run_id}/rerun",
            "/actions/runs/{run_id}/rerun-failed-jobs",
        ):
            self.assertIn(route, api)
        self.assertIn('@GET("repos/{owner}/{repo}/actions/workflows")', api)
        self.assertIn('@GET("repos/{owner}/{repo}/actions/runs")', api)
        self.assertIn('@GET("repos/{owner}/{repo}/actions/runs/{run_id}")', api)
        self.assertIn("retrofit2.Response<Unit>", api)

    def test_preflight_and_receipt_before_mutation(self):
        vm = VM.read_text()
        for call in (
            "api.getAuthenticatedUser().login != ActionsControlPolicy.OWNER",
            "api.getActionsRun(owner, repository, targetId)",
            "latest.id != targetId",
            "latest.headSha != run?.headSha",
            "prepareIntent(repository, operation, targetId",
            'state = "INTENT_PREPARED"',
            "it.fd.sync()",
            "REQUEST_ACCEPTED",
            "PROVIDER_DENIED",
            "run_completed",
            "claim_allowed",
            "RECEIPT_TOKEN_VAZIO",
        ):
            self.assertIn(call, vm)
        self.assertLess(vm.index("if (!prepareIntent(repository, operation, targetId"), vm.index("api.dispatchActionsWorkflow("))
        self.assertLess(vm.index('latest.id != targetId'), vm.index("api.cancelActionsRun("))
        self.assertLess(vm.index("if (!prepareIntent(repository, operation, targetId", vm.index("val latest =")), vm.index("api.cancelActionsRun("))
        self.assertNotIn("secrets.PAT_ACTIONS", vm)
        self.assertNotIn("Authorization: Bearer", vm)
        self.assertNotIn("log.d(", vm.lower())

    def test_confirmed_ui_and_home_launch_are_not_auto_mutations(self):
        ui = UI.read_text()
        for token in (
            "DISPARAR $id",
            "CANCELAR $id",
            "REEXECUTAR $id",
            "REEXECUTAR FALHOS $id",
            "typed == action.phrase",
            "Confirmar",
            "viewModel.mutate(action.operation, action.id, inputs)",
            "ActionsControlPolicy.repositories",
            "GitHub",
        ):
            self.assertIn(token, ui)
        self.assertIn("ActionsControlActivity", ENTRY.read_text())
        self.assertIn(".ActionsControlActivity", MANIFEST.read_text())
        self.assertIn('android:label="Raf Actions"', MANIFEST.read_text())
        self.assertIn("onNavigateToActions", HOME.read_text())
        self.assertIn("ActionsControlActivity::class.java", NAV.read_text())

    def test_policy_rejects_unbound_identity(self):
        model = MODELS.read_text()
        for token in (
            "repository in repositories",
            "workflow.state == \"active\"",
            'run.status == "completed" && isExactSha(run.headSha)',
            "isExactSha(run.headSha)",
            'Regex("^[0-9a-f]{40}$")',
            '!ref.contains("..")',
            'run.status in setOf("queued", "requested", "pending", "waiting", "in_progress")',
        ):
            self.assertIn(token, model)
        self.assertNotIn("PAT_ACTIONS =", model)


if __name__ == "__main__":
    unittest.main()
