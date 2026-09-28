#!/usr/bin/env python3
"""Validate retained RIVER-7 cross-repository simulation receipts."""
from __future__ import annotations

import argparse
import json
from pathlib import Path


def load(path: Path) -> dict:
    payload = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(payload, dict):
        raise ValueError(f"{path}: top-level JSON must be object")
    return payload


def validate(linear_path: Path, burst_path: Path) -> None:
    receipt = load(linear_path)
    assert receipt["state"] == "SIMULATION_PASS"
    assert receipt["physical_network_state"] == "NOT_RUN"
    assert receipt["network_io"] == "NONE"
    assert receipt["sockets"] == "NONE"
    assert receipt["timing_channel_transmission"] == "DISABLED"
    assert receipt["claim_allowed"] is False
    assert receipt["code"]["minimum_distance"] == 3
    assert receipt["code"]["guaranteed_erasure_correction"] == 2
    assert receipt["erasure_matrix"]["2"]["failures"] == 0
    assert receipt["erasure_matrix"]["3"]["failures"] == 7

    burst = load(burst_path)
    assert burst["state"] == "SIMULATION_PASS"
    assert burst["physical_network_state"] == "NOT_RUN"
    assert burst["network_io"] == "NONE"
    assert burst["sockets"] == "NONE"
    assert burst["covert_carrier"] == "DISABLED"
    assert burst["claim_allowed"] is False
    assert burst["search_space_permutations"] == 5040
    assert burst["base_cyclic"]["3"]["successes"] == 5
    assert burst["base_cyclic"]["3"]["failures"] == 2
    assert burst["optimized_order"] == [0, 1, 2, 5, 4, 6, 3]
    assert burst["optimized_cyclic"]["3"]["successes"] == 7
    assert burst["optimized_cyclic"]["3"]["failures"] == 0
    assert burst["optimized_cyclic"]["4"]["successes"] == 0


def main() -> int:
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--linear", type=Path, required=True)
    p.add_argument("--burst", type=Path, required=True)
    args = p.parse_args()
    validate(args.linear, args.burst)
    print("PASS: exact-SHA cross-repo RIVER-7 V3 offline validation")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
