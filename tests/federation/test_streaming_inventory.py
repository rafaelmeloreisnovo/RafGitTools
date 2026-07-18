import importlib.util
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SPEC = importlib.util.spec_from_file_location("inventory", ROOT / "scripts/federation/streaming_inventory.py")
MOD = importlib.util.module_from_spec(SPEC)
assert SPEC.loader
SPEC.loader.exec_module(MOD)


def test_scan_is_deterministic_and_pointer_only(tmp_path):
    (tmp_path / "b.txt").write_text("beta", encoding="utf-8")
    (tmp_path / "a.zip").write_bytes(b"not-extracted")
    first = MOD.scan(tmp_path, 4096)
    second = MOD.scan(tmp_path, 4096)
    assert first == second
    assert [item["relative_path"] for item in first] == ["a.zip", "b.txt"]
    assert first[0]["is_archive_candidate"] is True
    assert all(item["payload_copied"] is False for item in first)


def test_max_files_bounds_scope(tmp_path):
    (tmp_path / "a.txt").write_text("a", encoding="utf-8")
    (tmp_path / "b.txt").write_text("b", encoding="utf-8")
    assert len(MOD.scan(tmp_path, 4096, max_files=1)) == 1
