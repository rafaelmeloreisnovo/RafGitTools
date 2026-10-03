import importlib.util
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SCRIPT = ROOT / "scripts" / "validate_tool_router_registry.py"
SPEC = importlib.util.spec_from_file_location("validate_tool_router_registry", SCRIPT)
MODULE = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(MODULE)


class ToolRouterRegistryContractTest(unittest.TestCase):
    def test_current_tree_is_coherent(self):
        report = MODULE.validate()
        self.assertEqual("PASS", report["state"], report)
        self.assertFalse(report["claim_allowed"])
        self.assertEqual("NOT_RUN", report["execution_evidence"])

    def test_allowed_tool_without_handler_fails(self):
        registry = {
            "version": "fixture",
            "tools": {
                "ghost.tool": {
                    "allowed": True,
                    "requires_auth": False,
                    "handler_state": "IMPLEMENTED",
                }
            },
        }
        report = MODULE.evaluate(registry, "return when (tool) { else -> tokenVazio(tool, \"x\") }")
        self.assertEqual("FAIL", report["state"])
        self.assertEqual(["ghost.tool"], report["allowed_without_handler"])

    def test_router_handler_without_registry_fails(self):
        registry = {"version": "fixture", "tools": {}}
        router = 'return when (tool) { "git.status" -> handleGitStatus(call) }'
        report = MODULE.evaluate(registry, router)
        self.assertEqual("FAIL", report["state"])
        self.assertEqual(["git.status"], report["handler_without_registry"])

    def test_allowed_handler_requires_implemented_state(self):
        registry = {
            "version": "fixture",
            "tools": {
                "git.status": {
                    "allowed": True,
                    "requires_auth": False,
                    "handler_state": "TOKEN_VAZIO_HANDLER_MISSING",
                }
            },
        }
        router = 'return when (tool) { "git.status" -> handleGitStatus(call) }'
        report = MODULE.evaluate(registry, router)
        self.assertEqual("FAIL", report["state"])
        self.assertEqual(["git.status"], report["allowed_without_implemented_state"])


if __name__ == "__main__":
    unittest.main()
