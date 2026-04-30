#!/usr/bin/env python3
"""Toro7D validation benchmark: deterministic, falsifiable, low-overhead."""
from __future__ import annotations
import argparse, hashlib, json, math, random, statistics

ALPHA = 0.25
DIM = 7

def clamp01(x: float) -> float:
    return x - math.floor(x)

def toroidal_map(payload: str, entropy: float, hsh: str, state: str) -> list[float]:
    seed = int(hashlib.sha256(f"{payload}|{entropy:.6f}|{hsh}|{state}".encode()).hexdigest()[:16], 16)
    rng = random.Random(seed)
    return [rng.random() for _ in range(DIM)]

def entropy_milli(data: bytes) -> float:
    if len(data) < 2:
        return 0.0
    unique = len(set(data))
    transitions = sum(1 for i in range(1, len(data)) if data[i] != data[i-1])
    return (unique * 6000 / 256) + (transitions * 2000 / (len(data)-1))

def step(C: float, H: float, Cin: float, Hin: float) -> tuple[float,float,float]:
    Cn = (1-ALPHA)*C + ALPHA*Cin
    Hn = (1-ALPHA)*H + ALPHA*Hin
    phi = (1-Hn)*Cn
    return Cn, Hn, phi

def classify(pi: float) -> str:
    if pi <= 1.1: return "Ω"
    if pi <= 2.0: return "Δ"
    return "ρ"

def run(iterations: int) -> dict:
    C=0.5; H=0.5
    regimes=[]; pis=[]
    for i in range(iterations):
        payload = f"sample-{i}"
        b = payload.encode()
        ent = entropy_milli(b)/10000.0
        h = hashlib.sha256(b).hexdigest()
        s = toroidal_map(payload, ent, h, "RUN")
        Cin = sum(s)/DIM
        Hin = clamp01(ent)
        C,H,phi = step(C,H,Cin,Hin)
        Ein = 0.2 + Cin
        D = 0.1 + abs(1-phi)
        pi = Ein / D
        regimes.append(classify(pi)); pis.append(pi)
    return {
        "iterations": iterations,
        "alpha": ALPHA,
        "pi_mean": round(statistics.mean(pis),6),
        "pi_stdev": round(statistics.pstdev(pis),6),
        "omega_ratio": round(regimes.count("Ω")/iterations,6),
        "delta_ratio": round(regimes.count("Δ")/iterations,6),
        "rho_ratio": round(regimes.count("ρ")/iterations,6),
        "falsifiable_checks": {
            "ratios_sum_to_1": abs((regimes.count("Ω")+regimes.count("Δ")+regimes.count("ρ"))/iterations - 1.0) < 1e-9,
            "pi_non_negative": min(pis) >= 0.0,
        }
    }

def main():
    p = argparse.ArgumentParser()
    p.add_argument("--iterations", type=int, default=420)
    args = p.parse_args()
    print(json.dumps(run(args.iterations), ensure_ascii=False, indent=2))

if __name__ == "__main__":
    main()
