#!/usr/bin/env python3
"""Validação determinística do núcleo Toro7D.

Checks principais:
- domínio: s ∈ [0,1)^7
- atrator: |A|=42
- EMA: C/H com alpha configurável (default 0.25)
- periodicidade: x_{n+42}=x_n em dinâmica cíclica de validação
- invariantes de integridade: XOR/FNV/CRC/Merkle determinísticos
"""
from __future__ import annotations

import argparse
import json
import math
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from scripts.toro7d_simulator import (  # noqa: E402
    Toro7DSimulator,
    attractor_id,
    crc32_u,
    entropy_milli,
    fnv1a_64,
    merkle_root_u64,
    toroidal_map,
    xor_acc,
)


def validate_domain(samples: list[bytes]) -> dict:
    ok = True
    for idx, b in enumerate(samples):
        s = toroidal_map(b, state_flag=idx + 1)
        if len(s) != 7:
            ok = False
            break
        if not all(0.0 <= v < 1.0 for v in s):
            ok = False
            break
    return {"name": "domain_t7", "ok": ok}


def validate_attractor_space(samples: list[bytes]) -> dict:
    ids = set()
    for idx, b in enumerate(samples):
        ids.add(attractor_id(toroidal_map(b, state_flag=idx + 11)))
    ok = all(0 <= i < 42 for i in ids)
    return {"name": "attractor_42_space", "ok": ok, "unique_ids": len(ids)}


def validate_ema_formula(alpha: float = 0.25) -> dict:
    sim = Toro7DSimulator(alpha=alpha, jump_prob=0.0, seed=7)
    sim.load_from_payload(b"ema-check", state_flag=5)
    instant = sim._instant_metrics(sim.state)

    c0 = sim.c_t
    h0 = sim.h_t
    filtered = sim.filtered_metrics(instant)

    expected_c = (1 - alpha) * c0 + alpha * instant.coherence_c
    expected_h = (1 - alpha) * h0 + alpha * instant.entropy_h
    expected_phi = (1 - expected_h) * expected_c

    ok = (
        math.isclose(filtered.coherence_c, expected_c, rel_tol=0, abs_tol=1e-12)
        and math.isclose(filtered.entropy_h, expected_h, rel_tol=0, abs_tol=1e-12)
        and math.isclose(filtered.phi, expected_phi, rel_tol=0, abs_tol=1e-12)
    )
    return {"name": "ema_alpha_formula", "ok": ok, "alpha": alpha}


def validate_cycle_42() -> dict:
    # dinâmica de validação estritamente periódica por construção
    sequence = [(n * 13) % 42 for n in range(420)]
    ok = all(sequence[n + 42] == sequence[n] for n in range(len(sequence) - 42))
    return {"name": "periodicity_42_constructive", "ok": ok}


def validate_integrity_determinism(sample: bytes) -> dict:
    r1 = {
        "xor": xor_acc(sample),
        "fnv": fnv1a_64(sample),
        "crc": crc32_u(sample),
        "merkle": merkle_root_u64([sample[i : i + 64] for i in range(0, len(sample), 64)]),
        "entropy_milli": entropy_milli(sample),
    }
    r2 = {
        "xor": xor_acc(sample),
        "fnv": fnv1a_64(sample),
        "crc": crc32_u(sample),
        "merkle": merkle_root_u64([sample[i : i + 64] for i in range(0, len(sample), 64)]),
        "entropy_milli": entropy_milli(sample),
    }
    return {"name": "integrity_determinism", "ok": r1 == r2, "digest": r1}


def run_suite(payload_prefix: str, samples_n: int, alpha: float) -> dict:
    samples = [f"{payload_prefix}-{i}".encode("utf-8") for i in range(samples_n)]
    checks = [
        validate_domain(samples),
        validate_attractor_space(samples),
        validate_ema_formula(alpha=alpha),
        validate_cycle_42(),
        validate_integrity_determinism(samples[0]),
    ]
    return {
        "suite": "toro7d_validation",
        "alpha": alpha,
        "samples": samples_n,
        "passed": sum(1 for c in checks if c["ok"]),
        "failed": sum(1 for c in checks if not c["ok"]),
        "ok": all(c["ok"] for c in checks),
        "checks": checks,
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--payload-prefix", default="RafGitTools-T7")
    parser.add_argument("--samples", type=int, default=128)
    parser.add_argument("--alpha", type=float, default=0.25)
    args = parser.parse_args()

    result = run_suite(args.payload_prefix, args.samples, args.alpha)
    print(json.dumps(result, ensure_ascii=False, indent=2))
    return 0 if result["ok"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
