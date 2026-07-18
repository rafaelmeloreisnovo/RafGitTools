import importlib.util, json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SPEC = importlib.util.spec_from_file_location("backfill", ROOT / "scripts/federation/validate_backfill_stage1.py")
MOD = importlib.util.module_from_spec(SPEC)
assert SPEC.loader
SPEC.loader.exec_module(MOD)


def test_profile_valid():
    data = json.loads((ROOT / "configs/backfill-stage-1.json").read_text(encoding="utf-8"))
    assert MOD.validate(data) == []
