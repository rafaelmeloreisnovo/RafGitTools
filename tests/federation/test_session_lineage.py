import importlib.util, json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SPEC = importlib.util.spec_from_file_location("session_lineage", ROOT / "scripts/federation/session_lineage.py")
MOD = importlib.util.module_from_spec(SPEC)
assert SPEC.loader
SPEC.loader.exec_module(MOD)


def profile():
    return json.loads((ROOT / "docs/federation/session-lineage-v1.json").read_text(encoding="utf-8"))


def test_profile_valid():
    assert MOD.validate(profile()) == []


def test_bridge_cannot_be_current():
    value = profile()
    value["bridge_active_now"] = True
    assert "bridge_active_now" in MOD.validate(value)


def test_token_vazio_not_promoted():
    value = profile()
    value["every_zip_fully_inspected"] = "PASS"
    assert "every_zip_fully_inspected" in MOD.validate(value)
