import importlib.util, json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]

SPEC = importlib.util.spec_from_file_location("backfill", ROOT / "scripts/federation/validate_backfill_stage1.py")
MOD = importlib.util.module_from_spec(SPEC)
assert SPEC.loader
SPEC.loader.exec_module(MOD)

INV_SPEC = importlib.util.spec_from_file_location("inventory", ROOT / "scripts/federation/streaming_inventory.py")
INV = importlib.util.module_from_spec(INV_SPEC)
assert INV_SPEC.loader
INV_SPEC.loader.exec_module(INV)


def test_profile_valid():
    data = json.loads((ROOT / "configs/backfill-stage-1.json").read_text(encoding="utf-8"))
    assert MOD.validate(data) == []


def test_streaming_inventory_is_deterministic(tmp_path):
    (tmp_path / "b.txt").write_text("beta", encoding="utf-8")
    (tmp_path / "a.zip").write_bytes(b"not-extracted")
    first = INV.scan(tmp_path, 4096)
    second = INV.scan(tmp_path, 4096)
    assert first == second
    assert [item["relative_path"] for item in first] == ["a.zip", "b.txt"]
    assert first[0]["is_archive_candidate"] is True
    assert all(item["payload_copied"] is False for item in first)
