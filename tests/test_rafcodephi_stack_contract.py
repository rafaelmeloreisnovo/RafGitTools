from __future__ import annotations

import importlib.util
import json
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SPEC = importlib.util.spec_from_file_location(
    "stack_contract", ROOT / "scripts/validate_rafcodephi_stack_contract.py"
)
assert SPEC is not None and SPEC.loader is not None
MODULE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(MODULE)


class RafcodephiStackContractTest(unittest.TestCase):
    def write(self, root: Path, relative: str, content: str) -> None:
        path = root / relative
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(content, encoding="utf-8")

    def fixture(self, root: Path) -> tuple[Path, Path, Path, Path]:
        app = root / "app"
        api = root / "api"
        packages = root / "packages"
        polimata = root / "polimata"
        app_commit = json.loads((ROOT / "runtime-lock.json").read_text())["repositories"][0]["commit"]
        self.write(
            app,
            "termux-shared/src/main/java/com/termux/shared/termux/TermuxConstants.java",
            'TERMUX_PACKAGE_NAME = "com.termux.rafacodephi";\n'
            'TERMUX_API_PACKAGE_NAME = TERMUX_PACKAGE_NAME + ".api";\n'
            'TERMUX_API_CODE_PACKAGE_NAME = "com.termux.api";\n',
        )
        self.write(
            app,
            "scripts/import_rafcodephi_real_bootstrap.py",
            '"arm": {"elf_class": 1, "machine": 40\n'
            '"aarch64": {"elf_class": 2, "machine": 183\n'
            "libexec/termux-api-broadcast\n"
            "termux-api client does not target the RAFCODEPHI API receiver\n"
            "RAFCODEPHI_PACKAGE_REPOSITORY_NOT_PUBLISHED\n"
            "custom-prefix apt repository is not deterministically disabled\n",
        )
        self.write(app, "scripts/prepare_bootstrap_env.sh", "RAF_REAL_BOOTSTRAP_ZIP_ARM\nRAF_REAL_BOOTSTRAP_ZIP_AARCH64\n")
        self.write(
            app,
            "app/src/main/AndroidManifest.xml",
            'android:name="${TERMUX_PACKAGE_NAME}.permission.TERMUX_API"\n'
            'android:protectionLevel="signature"\n',
        )
        self.write(app, "app/build.gradle", "RAFCODEPHI_PAIRED_KEYSTORE_FILE\n")
        self.write(
            api,
            "app/build.gradle",
            f'RAFCODEPHI_APP_PACKAGE_NAME") ?: "com.termux.rafacodephi"\n'
            "applicationId rafcodephiApiPackage\n"
            "RAFCODEPHI_PAIRED_KEYSTORE_FILE\n"
            'rafcodephiSharedMode == "maven-local"\n'
            f'RAFCODEPHI_TERMUX_SHARED_VERSION") ?: "{app_commit}"\n',
        )
        self.write(
            api,
            "app/src/main/AndroidManifest.xml",
            'android:exported="true"\n'
            'android:name="com.termux.api.TermuxApiReceiver"\n'
            'android:permission="${RAFCODEPHI_APP_PACKAGE}.permission.TERMUX_API"\n',
        )
        self.write(
            api,
            "app/src/main/java/com/termux/api/TermuxAPIConstants.java",
            "TERMUX_API_CODE_PACKAGE_NAME\n",
        )
        self.write(
            packages,
            "packages/termux-api/termux-api.c.patch",
            '+    child_argv[5] = "com.termux.rafacodephi.api/com.termux.api.TermuxApiReceiver";\n'
            "RAFCODEPHI_SHARED_UID_FILESYSTEM_SOCKETS\n",
        )
        self.write(
            packages,
            "scripts/build-rafcodephi-real-bootstrap.sh",
            "--add busybox,proot,ca-certificates,termux-api\n"
            "termux_api_cli=EMBEDDED\nlibexec/termux-api-broadcast\n"
            "Enabled: no\nRAFCODEPHI_PACKAGE_REPOSITORY_NOT_PUBLISHED\n"
            "device_runtime_proof=TOKEN_VAZIO\n",
        )
        evidence = (
            "com.termux.rafacodephi\ncom.termux.rafacodephi.api\n"
            "com.termux.rafacodephi.api/com.termux.api.TermuxApiReceiver\n"
            "SIGNATURE_PERMISSION_NO_SHARED_UID\n"
            "RAFCODEPHI_PACKAGE_REPOSITORY_NOT_PUBLISHED\n"
            "STRUCTURAL_PASS_LIMITED\nclaim_allowed\n"
        )
        self.write(polimata, "scripts/validate_rafcodephi_bootstrap_evidence.py", evidence)
        self.write(polimata, "contracts/rafcodephi-bootstrap-evidence.v1.schema.json", evidence)
        return app, api, packages, polimata

    def test_current_five_repository_contract_is_structurally_closed(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            app, api, packages, polimata = self.fixture(Path(temporary))
            report = MODULE.validate_stack(
                ROOT / "runtime-lock.json",
                app,
                api,
                packages,
                polimata,
                check_heads=False,
            )
        self.assertEqual("PASS", report["structural_state"])
        self.assertEqual("TOKEN_VAZIO", report["runtime_boundaries"]["termux_api_call"])
        self.assertFalse(report["claim_allowed"])

        schema = json.loads(
            (ROOT / "contracts/rafcodephi-runtime-stack.v1.schema.json").read_text(encoding="utf-8")
        )
        self.assertEqual(
            ["app", "api", "packages", "polimata"],
            schema["properties"]["locked_commits"]["required"],
        )


if __name__ == "__main__":
    unittest.main()
