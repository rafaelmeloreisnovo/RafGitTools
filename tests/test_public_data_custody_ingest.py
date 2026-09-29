from __future__ import annotations

import importlib.util
import json
import os
import sys
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


def test_config_keeps_github_pats_out_of_public_data_auth() -> None:
    cfg = json.loads(CONFIG.read_text(encoding="utf-8"))
    assert cfg["secret_policy"]["allowed_secret_references"] == ["PORTAL_TRANSPARENCIA_API_KEY"]
    forbidden = set(cfg["secret_policy"]["forbidden_fallbacks"])
    for secret in ["PAT_ENV", "PAT_ACTIONS", "PAT_AGENTS", "PAT_CODESPACES", "PAT_DEPENDABOT", "PAT_PRIVATE_PROCESSING"]:
        assert secret in forbidden


def test_person_fields_are_removed_recursively() -> None:
    mod = load_module()
    cfg = mod.load_config()
    value = {
        "NOME": "Pessoa Publica",
        "CPF": "***",
        "orgao": "Orgao X",
        "nested": {"email": "x@example.invalid", "valor": 10},
    }
    clean = mod.sanitize_public(value, cfg)
    assert "NOME" not in clean
    assert "CPF" not in clean
    assert clean["orgao"] == "Orgao X"
    assert "email" not in clean["nested"]
    assert clean["nested"]["valor"] == 10


def test_portal_source_is_allowlisted_and_https() -> None:
    mod = load_module()
    cfg = mod.load_config()
    source = mod.validate_source(cfg, "PORTAL_DESPESAS_POR_ORGAO")
    url = mod.build_url(source, {"ano": 2025, "pagina": 1})
    assert url.startswith("https://api.portaldatransparencia.gov.br/api-de-dados/despesas/por-orgao?")
    try:
        mod.validate_source(cfg, "PORTAL_SERVIDORES_REMUNERACAO_INDIVIDUAL")
    except ValueError as exc:
        assert str(exc) == "source_not_allowlisted"
    else:
        raise AssertionError("individual remuneration endpoint unexpectedly allowlisted")


def test_plect_remains_unpromoted_and_has_baseline_only() -> None:
    mod = load_module()
    cfg = mod.load_config()
    assert cfg["plect_bridge"]["state"] == "TOKEN_VAZIO_CANONICAL_OPERATOR"
    baseline = mod.first_difference_direction([10.0, 12.0])
    assert baseline["direction"] == "UP"
    assert baseline["canonical_plect"] is False
    assert baseline["claim_allowed"] is False


def test_salary_aggregation_suppresses_small_groups() -> None:
    mod = load_module()
    rows = [
        {"orgao": "A", "cargo": "X", "valor": "100,00"},
        {"orgao": "A", "cargo": "X", "valor": "110,00"},
        {"orgao": "A", "cargo": "X", "valor": "120,00"},
        {"orgao": "A", "cargo": "X", "valor": "130,00"},
        {"orgao": "A", "cargo": "X", "valor": "140,00"},
        {"orgao": "B", "cargo": "Y", "valor": "999,00"},
    ]
    out = mod.aggregate_rows(rows, ["orgao", "cargo"], ["valor"], 5)
    assert len(out) == 1
    assert out[0]["orgao"] == "A"
    assert out[0]["count"] == 5
    assert out[0]["valor_sum"] == 600.0


def test_receipt_hashes_raw_and_projection_without_persisting_raw(tmp_path: Path) -> None:
    mod = load_module()
    raw = b'[{"orgao":"A","valor":1}]'
    receipt = mod.write_snapshot_receipt(
        source_id="TEST",
        source_url="https://example.gov/public",
        raw=raw,
        projection=[{"orgao": "A", "valor": 1}],
        output_dir=tmp_path,
    )
    assert receipt["source_sha256"] == mod.sha256_bytes(raw)
    assert receipt["raw_source_persisted"] is False
    assert receipt["claim_allowed"] is False
    assert (tmp_path / "projection.json").exists()
    assert not (tmp_path / "raw.json").exists()


def test_secret_is_required_but_never_added_to_url(monkeypatch) -> None:
    mod = load_module()
    cfg = mod.load_config()
    source = cfg["sources"]["PORTAL_DESPESAS_POR_ORGAO"]
    url = mod.build_url(source, {"ano": 2025})
    assert "PORTAL_TRANSPARENCIA_API_KEY" not in url
