#!/usr/bin/env python3
"""Simulador do Núcleo RAFAELIA em T^7.

Modelo:
- Estado s in [0,1)^7
- Atualização toroidal com saltos raros
- Coerência: phi = (1 - H) * C
"""

from __future__ import annotations

import argparse
import math
from dataclasses import dataclass

import numpy as np


@dataclass
class Metrics:
    phi: float
    entropy_h: float
    coherence_c: float


class Toro7DSimulator:
    def __init__(self, alpha: float = 0.25, jump_prob: float = 0.03, seed: int = 42) -> None:
        self.alpha = alpha
        self.jump_prob = jump_prob
        self.rng = np.random.default_rng(seed)
        self.state = np.full(7, 0.5, dtype=np.float64)
        self.history = [self.state.copy()]
        self.c_t = 0.5
        self.h_t = 0.5

    def evolve_step(self, step: int) -> np.ndarray:
        next_state = np.zeros_like(self.state)
        for i in range(7):
            base = 0.9 + 0.2 * math.sin(step * (i + 1) * math.pi / 7.0 + self.state[i])
            if self.rng.random() < self.jump_prob:
                base += 1.5
            next_state[i] = (self.state[i] * base) % 1.0
        self.state = next_state
        self.history.append(self.state.copy())
        return self.state

    @staticmethod
    def _instant_metrics(state: np.ndarray) -> Metrics:
        mean = float(np.mean(state))
        var = float(np.mean((state - mean) ** 2))
        h = min(1.0, var * 10.0)
        c = 1.0 - float(np.mean(np.abs(state - 0.5) * 2.0))
        phi = (1.0 - h) * c
        return Metrics(phi=phi, entropy_h=h, coherence_c=c)

    def filtered_metrics(self, instant: Metrics) -> Metrics:
        self.c_t = (1 - self.alpha) * self.c_t + self.alpha * instant.coherence_c
        self.h_t = (1 - self.alpha) * self.h_t + self.alpha * instant.entropy_h
        phi = (1.0 - self.h_t) * self.c_t
        return Metrics(phi=phi, entropy_h=self.h_t, coherence_c=self.c_t)

    def run(self, steps: int) -> tuple[np.ndarray, list[Metrics], list[Metrics]]:
        instant_m = [self._instant_metrics(self.state)]
        filtered_m = [self.filtered_metrics(instant_m[0])]
        for step in range(steps):
            self.evolve_step(step)
            im = self._instant_metrics(self.state)
            fm = self.filtered_metrics(im)
            instant_m.append(im)
            filtered_m.append(fm)
        return np.array(self.history), instant_m, filtered_m


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Simulação toroidal T^7 com métrica phi")
    parser.add_argument("--steps", type=int, default=500)
    parser.add_argument("--seed", type=int, default=42)
    parser.add_argument("--alpha", type=float, default=0.25)
    parser.add_argument("--jump-prob", type=float, default=0.03)
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    sim = Toro7DSimulator(alpha=args.alpha, jump_prob=args.jump_prob, seed=args.seed)
    history, instant, filtered = sim.run(args.steps)

    avg_phi = float(np.mean([m.phi for m in filtered]))
    avg_h = float(np.mean([m.entropy_h for m in filtered]))
    avg_c = float(np.mean([m.coherence_c for m in filtered]))

    print(f"steps={args.steps} seed={args.seed} alpha={args.alpha} jump_prob={args.jump_prob}")
    print("final_state=[" + ", ".join(f"{x:.6f}" for x in history[-1]) + "]")
    print(f"phi_mean={avg_phi:.6f}")
    print(f"H_mean={avg_h:.6f}")
    print(f"C_mean={avg_c:.6f}")
    print(f"attractor_hint_unique_states_rounded4={len(set(tuple(np.round(s, 4)) for s in history))}")


if __name__ == "__main__":
    main()
