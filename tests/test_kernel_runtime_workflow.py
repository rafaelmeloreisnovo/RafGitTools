#!/usr/bin/env python3
from __future__ import annotations

import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
WORKFLOW = ROOT / ".github" / "workflows" / "kernel-architecture.yml"


class KernelRuntimeWorkflowTest(unittest.TestCase):
    def setUp(self) -> None:
        self.workflow = WORKFLOW.read_text(encoding="utf-8")

    def test_manifest_collects_android_plugin_and_cmake_outputs(self) -> None:
        self.assertIn(
            "runtime-lock.json \\\n            app \\",
            self.workflow,
        )
        self.assertNotIn(
            "runtime-lock.json \\\n            app/.cxx \\",
            self.workflow,
        )

    def test_wrapper_is_resolved_from_android_plugin_output(self) -> None:
        self.assertIn("find app/build/intermediates/cxx", self.workflow)
        self.assertIn('*/obj/${ABI}/libraf_llama_kernel.so', self.workflow)

    def test_apk_gate_requires_wrapper_and_transitive_runtime(self) -> None:
        for library in (
            "libraf_llama_kernel.so",
            "libllama.so",
            "libggml.so",
            "libggml-base.so",
            "libggml-cpu.so",
        ):
            with self.subTest(library=library):
                self.assertIn(library, self.workflow)

    def test_private_provider_requires_explicit_access_receipt(self) -> None:
        self.assertIn("scripts/private_provider_access_gate.py", self.workflow)
        self.assertIn("private-provider-access-receipt.json", self.workflow)
        self.assertIn("Upload private provider access receipt", self.workflow)
        self.assertIn("--repo rafaelmeloreisnovo/llamaRafaelia", self.workflow)

    def test_private_llama_checkout_has_no_repo_scoped_token_fallback(self) -> None:
        self.assertNotIn("secrets.RAF_CROSS_REPO_TOKEN || github.token", self.workflow)
        self.assertIn("token: ${{ secrets.RAF_CROSS_REPO_TOKEN }}", self.workflow)
        self.assertIn("repository: rafaelmeloreisnovo/RafPolimata", self.workflow)
        self.assertIn("token: ${{ github.token }}", self.workflow)

    def test_private_provider_gate_tests_are_executed(self) -> None:
        self.assertIn("test_private_provider_access_gate.py", self.workflow)
        self.assertIn("python3 -m unittest discover -s tests -p 'test_private_provider_access_gate.py' -v", self.workflow)


if __name__ == "__main__":
    unittest.main()
