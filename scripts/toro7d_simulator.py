#!/usr/bin/env python3
"""Simulador do Núcleo RAFAELIA em T^7 (execução direta, baixo overhead).

Implementa versões operacionais das equações:
- s ∈ T^7 = [0,1)^7
- x=(dados, entropia, hash, estado) -> ToroidalMap(x)
- C/H com EMA (alpha=0.25)
- phi=(1-H)*C
- attractor_id em 42 classes
- integridade: XOR + FNV-1a + CRC32 + raiz Merkle
"""

from __future__ import annotations

import argparse
import math
import zlib
import random
from dataclasses import dataclass


FNV_OFFSET_64 = 0xCBF29CE484222325
FNV_PRIME_64 = 0x100000001B3


@dataclass
class Metrics:
    phi: float
    entropy_h: float
    coherence_c: float


def xor_acc(data: bytes) -> int:
    acc = 0
    for b in data:
        acc ^= b
    return acc


def fnv1a_64(data: bytes) -> int:
    h = FNV_OFFSET_64
    for b in data:
        h ^= b
        h = (h * FNV_PRIME_64) & 0xFFFFFFFFFFFFFFFF
    return h


def crc32_u(data: bytes) -> int:
    return zlib.crc32(data) & 0xFFFFFFFF


def merkle_root_u64(chunks: list[bytes]) -> int:
    if not chunks:
        return 0
    layer = [fnv1a_64(c).to_bytes(8, "big") for c in chunks]
    while len(layer) > 1:
        if len(layer) & 1:
            layer.append(layer[-1])
        nxt: list[bytes] = []
        for i in range(0, len(layer), 2):
            nxt.append(fnv1a_64(layer[i] + layer[i + 1]).to_bytes(8, "big"))
        layer = nxt
    return int.from_bytes(layer[0], "big")


def entropy_milli(data: bytes) -> float:
    n = len(data)
    if n == 0:
        return 0.0
    unique = len(set(data))
    transitions = sum(1 for i in range(1, n) if data[i] != data[i - 1])
    part_u = unique * 6000.0 / 256.0
    part_t = 0.0 if n == 1 else transitions * 2000.0 / (n - 1)
    return part_u + part_t


def toroidal_map(data: bytes, state_flag: int) -> list[float]:
    em = entropy_milli(data)
    acc = xor_acc(data)
    fnv = fnv1a_64(data)
    crc = crc32_u(data)
    merkle = merkle_root_u64([data[i:i + 64] for i in range(0, len(data), 64)])
    bits_geom = max(1.0, math.log2(max(1, len(data)) * 7))

    # x=(dados,entropia,hash,estado) -> s=(u,v,psi,chi,rho,delta,sigma)
    s = [
        (em % 10000.0) / 10000.0,
        (acc & 0xFF) / 256.0,
        (fnv & 0xFFFFFFFF) / 2**32,
        (crc & 0xFFFFFFFF) / 2**32,
        (merkle & 0xFFFFFFFF) / 2**32,
        min(1.0, em / 12000.0) * 0.9,
        ((state_flag & 0xFFFF) / 65536.0 + 1.0 / bits_geom) % 1.0,
    ]
    return [v % 1.0 for v in s]


class Toro7DSimulator:
    def __init__(self, alpha: float = 0.25, jump_prob: float = 0.03, seed: int = 42) -> None:
        self.alpha = alpha
        self.jump_prob = jump_prob
        self.rng = random.Random(seed)
        self.state = [0.5] * 7
        self.history = [self.state.copy()]
        self.c_t = 0.5
        self.h_t = 0.5

    def load_from_payload(self, data: bytes, state_flag: int = 1) -> None:
        self.state = toroidal_map(data, state_flag)
        self.history = [self.state.copy()]

    def evolve_step(self, step: int) -> list[float]:
        next_state = [0.0] * len(self.state)
        for i in range(7):
            base = 0.9 + 0.2 * math.sin(step * (i + 1) * math.pi / 7.0 + self.state[i])
            if self.rng.random() < self.jump_prob:
                base += 1.5
            next_state[i] = (self.state[i] * base) % 1.0
        self.state = next_state
        self.history.append(self.state.copy())
        return self.state

    @staticmethod
    def _instant_metrics(state: list[float]) -> Metrics:
        mean = sum(state) / len(state)
        var = sum((x - mean) ** 2 for x in state) / len(state)
        h = min(1.0, var * 10.0)
        c = 1.0 - (sum(abs(x - 0.5) * 2.0 for x in state) / len(state))
        return Metrics(phi=(1.0 - h) * c, entropy_h=h, coherence_c=c)

    def filtered_metrics(self, instant: Metrics) -> Metrics:
        self.c_t = (1 - self.alpha) * self.c_t + self.alpha * instant.coherence_c
        self.h_t = (1 - self.alpha) * self.h_t + self.alpha * instant.entropy_h
        return Metrics(phi=(1.0 - self.h_t) * self.c_t, entropy_h=self.h_t, coherence_c=self.c_t)

    def run(self, steps: int) -> tuple[list[list[float]], list[Metrics], list[Metrics]]:
        instant_m = [self._instant_metrics(self.state)]
        filtered_m = [self.filtered_metrics(instant_m[0])]
        for step in range(steps):
            self.evolve_step(step)
            im = self._instant_metrics(self.state)
            fm = self.filtered_metrics(im)
            instant_m.append(im)
            filtered_m.append(fm)
        return self.history, instant_m, filtered_m


def attractor_id(s: list[float]) -> int:
    # |A| = 42
    return int(math.floor(sum(s) * 42.0)) % 42


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Simulação toroidal T^7 com integridade e atrator 42")
    parser.add_argument("--steps", type=int, default=500)
    parser.add_argument("--seed", type=int, default=42)
    parser.add_argument("--alpha", type=float, default=0.25)
    parser.add_argument("--jump-prob", type=float, default=0.03)
    parser.add_argument("--payload", type=str, default="RafGitTools-Toro7D")
    parser.add_argument("--state-flag", type=int, default=1)
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    payload = args.payload.encode("utf-8")
    sim = Toro7DSimulator(alpha=args.alpha, jump_prob=args.jump_prob, seed=args.seed)
    sim.load_from_payload(payload, state_flag=args.state_flag)
    history, _, filtered = sim.run(args.steps)

    avg_phi = sum(m.phi for m in filtered) / len(filtered)
    avg_h = sum(m.entropy_h for m in filtered) / len(filtered)
    avg_c = sum(m.coherence_c for m in filtered) / len(filtered)

    print(f"steps={args.steps} seed={args.seed} alpha={args.alpha} jump_prob={args.jump_prob}")
    print("init_state=[" + ", ".join(f"{x:.6f}" for x in history[0]) + "]")
    print("final_state=[" + ", ".join(f"{x:.6f}" for x in history[-1]) + "]")
    print(f"phi_mean={avg_phi:.6f} H_mean={avg_h:.6f} C_mean={avg_c:.6f}")
    print(f"attractor_id={attractor_id(history[-1])}")
    print(f"periodic_42_check={(args.steps >= 42)}")


if __name__ == "__main__":
    main()
