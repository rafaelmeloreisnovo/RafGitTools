import csv, importlib.util
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SPEC = importlib.util.spec_from_file_location("lineage2", ROOT / "scripts/federation/validate_lineage_stage2.py")
MOD = importlib.util.module_from_spec(SPEC)
assert SPEC.loader
SPEC.loader.exec_module(MOD)


def test_table_valid():
    with (ROOT / "configs/lineage-stage-2.tsv").open(encoding="utf-8", newline="") as handle:
        rows = list(csv.DictReader(handle, delimiter="\t"))
    assert MOD.validate(rows) == []
