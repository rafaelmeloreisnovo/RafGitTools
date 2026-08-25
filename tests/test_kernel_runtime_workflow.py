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


if __name__ == "__main__":
    unittest.main()
