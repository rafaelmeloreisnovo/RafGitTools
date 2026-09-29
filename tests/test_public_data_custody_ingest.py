from __future__ import annotations

import importlib.util
import json
import sys
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MODULE = ROOT / "scripts" / "public_data_custody_ingest.py"
CONFIG = ROOT / "configs" / "public-data-custody-ingestion.v1.json"


def load_module():
    spec = importlib.util.spec_from_file_location("public_data_custody_ingest_test", MODULE)
    assert spec and spec.loader
    mod = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = mod
    spec.loader.exec_module(mod)
    return mod


class PublicDataCustodyIngestTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.mod = load_module()
        cls.cfg = cls.mod.load_config()

    def test_config_keeps_github_pats_out_of_public_data_auth(self) -> None:
        self.assertEqual(
            self.cfg["secret_policy"]["allowed_secret_references"],
            ["PORTAL_TRANSPARENCIA_API_KEY"],
        )
        forbidden = set(self.cfg["secret_policy"]["forbidden_fallbacks"])
        for secret in [
            "PAT_ENV", "PAT_ACTIONS", "PAT_AGENTS", "PAT_CODESPACES",
            "PAT_DEPENDABOT", "PAT_PRIVATE_PROCESSING",
        ]:
            self.assertIn(secret, forbidden)

    def test_person_fields_are_removed_recursively(self) -> None:
        value = {
            "NOME": "Pessoa Publica",
            "CPF": "***",
            "orgao": "Orgao X",
            "nested": {"email": "x@example.invalid", "valor": 10},
        }
        clean = self.mod.sanitize_public(value, self.cfg)
        self.assertNotIn("NOME", clean)
        self.assertNotIn("CPF", clean)
        self.assertEqual(clean["orgao"], "Orgao X")
        self.assertNotIn("email", clean["nested"])
        self.assertEqual(clean["nested"]["valor"], 10)

    def test_portal_source_is_allowlisted_and_https(self) -> None:
        source = self.mod.validate_source(self.cfg, "PORTAL_DESPESAS_POR_ORGAO")
        url = self.mod.build_url(source, {"ano": 2025, "pagina": 1})
        self.assertTrue(
            url.startswith(
                "https://api.portaldatransparencia.gov.br/api-de-dados/despesas/por-orgao?"
            )
        )
        with self.assertRaisesRegex(ValueError, "source_not_allowlisted"):
            self.mod.validate_source(
                self.cfg,
                "PORTAL_SERVIDORES_REMUNERACAO_INDIVIDUAL",
            )

    def test_plect_remains_unpromoted_and_has_baseline_only(self) -> None:
        self.assertEqual(
            self.cfg["plect_bridge"]["state"],
            "TOKEN_VAZIO_CANONICAL_OPERATOR",
        )
        baseline = self.mod.first_difference_direction([10.0, 12.0])
        self.assertEqual(baseline["direction"], "UP")
        self.assertFalse(baseline["canonical_plect"])
        self.assertFalse(baseline["claim_allowed"])

    def test_salary_aggregation_suppresses_small_groups(self) -> None:
        rows = [
            {"orgao": "A", "cargo": "X", "valor": "100,00"},
            {"orgao": "A", "cargo": "X", "valor": "110,00"},
            {"orgao": "A", "cargo": "X", "valor": "120,00"},
            {"orgao": "A", "cargo": "X", "valor": "130,00"},
            {"orgao": "A", "cargo": "X", "valor": "140,00"},
            {"orgao": "B", "cargo": "Y", "valor": "999,00"},
        ]
        out = self.mod.aggregate_rows(rows, ["orgao", "cargo"], ["valor"], 5)
        self.assertEqual(len(out), 1)
        self.assertEqual(out[0]["orgao"], "A")
        self.assertEqual(out[0]["count"], 5)
        self.assertEqual(out[0]["valor_sum"], 600.0)

    def test_receipt_hashes_raw_and_projection_without_persisting_raw(self) -> None:
        raw = b'[{"orgao":"A","valor":1}]'
        with tempfile.TemporaryDirectory() as tmp:
            out = Path(tmp)
            receipt = self.mod.write_snapshot_receipt(
                source_id="TEST",
                source_url="https://example.gov/public",
                raw=raw,
                projection=[{"orgao": "A", "valor": 1}],
                output_dir=out,
            )
            self.assertEqual(receipt["source_sha256"], self.mod.sha256_bytes(raw))
            self.assertFalse(receipt["raw_source_persisted"])
            self.assertFalse(receipt["claim_allowed"])
            self.assertTrue((out / "projection.json").exists())
            self.assertFalse((out / "raw.json").exists())

    def test_secret_name_is_never_added_to_query_url(self) -> None:
        source = self.cfg["sources"]["PORTAL_DESPESAS_POR_ORGAO"]
        url = self.mod.build_url(source, {"ano": 2025})
        self.assertNotIn("PORTAL_TRANSPARENCIA_API_KEY", url)


if __name__ == "__main__":
    unittest.main()
