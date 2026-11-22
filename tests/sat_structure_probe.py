#!/usr/bin/env python3
"""Host-only deterministic harness; libc/ctypes/subprocess never enter freestanding L0.

No private corpus I/O. Measured time is host-specific; operation counters are method-specific.
The positive witness is independently checked; small cases get an exhaustive Python oracle.
"""
import ctypes
import hashlib
import json
import os
from pathlib import Path
import platform
import random
import statistics
import subprocess
import tempfile
import time

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "native/sat_structure_l0_v1/sat_structure_l0.c"
HEADER = SRC.with_suffix(".h")
MAX_N, MAX_M, MAX_F, MAX_W = 32, 160, 224, 8

class Clause(ctypes.Structure):
    _fields_ = [("size", ctypes.c_ubyte), ("vars", ctypes.c_ubyte*3), ("neg", ctypes.c_ubyte*3)]
class Formula(ctypes.Structure):
    _fields_ = [("n", ctypes.c_uint), ("m", ctypes.c_uint), ("clauses", Clause*MAX_M)]
class Factor(ctypes.Structure):
    _fields_ = [("mask", ctypes.c_uint), ("alive", ctypes.c_ubyte), ("truth", ctypes.c_ubyte*(1<<MAX_W))]
class Workspace(ctypes.Structure):
    _fields_ = [("factors", Factor*MAX_F)]
class Stats(ctypes.Structure):
    _fields_ = [("factor_checks", ctypes.c_ulonglong), ("clause_checks", ctypes.c_ulonglong), ("max_width", ctypes.c_uint), ("search_nodes", ctypes.c_uint), ("factors_created", ctypes.c_uint), ("terminal_status", ctypes.c_uint)]

def compile_kernel(directory):
    cc = os.environ.get("CC", "cc")
    lib_path = Path(directory)/"sat_l0.so"
    obj = Path(directory)/"sat_l0.o"
    common = ["-std=c11", "-O2", "-ffreestanding", "-nostdinc", "-fno-builtin", "-fno-stack-protector", "-Wall", "-Wextra", "-Werror"]
    subprocess.run([cc, *common, "-c", str(SRC), "-o", str(obj)], check=True)
    undef = subprocess.check_output(["nm", "-u", str(obj)], text=True).strip()
    if undef:
        raise AssertionError("L0 undefined symbols: " + undef)
    subprocess.run([cc, *common, "-fPIC", "-shared", "-nostdlib", str(SRC), "-o", str(lib_path)], check=True)
    lib = ctypes.CDLL(str(lib_path))
    lib.raf_sat_eliminate.argtypes = [ctypes.POINTER(Formula), ctypes.POINTER(Workspace), ctypes.POINTER(Stats)]
    lib.raf_sat_eliminate.restype = ctypes.c_int
    lib.raf_sat_dpll.argtypes = [ctypes.POINTER(Formula), ctypes.POINTER(Stats)]
    lib.raf_sat_dpll.restype = ctypes.c_int
    lib.raf_sat_check_certificate.argtypes = [ctypes.POINTER(Formula), ctypes.c_uint]
    lib.raf_sat_check_certificate.restype = ctypes.c_int
    return lib

def as_formula(n, clauses):
    if not (0 <= n <= MAX_N and len(clauses) <= MAX_M):
        raise ValueError("bounded fixture exceeded")
    f = Formula()
    f.n, f.m = n, len(clauses)
    for i, clause in enumerate(clauses):
        f.clauses[i].size = len(clause)
        for k, (v, neg) in enumerate(clause):
            f.clauses[i].vars[k] = v
            f.clauses[i].neg[k] = neg
    return f

def verify_py(clauses, mask):
    return all(any(((mask >> v)&1)^neg for v,neg in clause) for clause in clauses)

def exhaustive(clauses, n):
    for mask in range(1<<n):
        if verify_py(clauses, mask):
            return True
    return False

def planted_fixture(n, structure, seed, density=2):
    rng=random.Random(seed)
    witness=rng.getrandbits(n)
    clauses=[]
    for _ in range(n*density):
        if structure=="banded":
            start=rng.randrange(n-2)
            vars_=list(range(start,start+3))
        elif structure=="uniform":
            vars_=rng.sample(range(n),3)
        else:
            raise ValueError(structure)
        signs=[rng.getrandbits(1) for _ in range(3)]
        if not any(((witness>>v)&1)^sg for v,sg in zip(vars_,signs)):
            signs[0]^=1
        clauses.append(list(zip(vars_,signs)))
    assert verify_py(clauses,witness)
    return clauses,witness

def run(lib, f, method, repeats=1):
    results=[]
    for _ in range(repeats):
        s=Stats()
        start=time.perf_counter_ns()
        if method=="ve":
            w=Workspace()
            ans=lib.raf_sat_eliminate(ctypes.byref(f),ctypes.byref(w),ctypes.byref(s))
        else:
            ans=lib.raf_sat_dpll(ctypes.byref(f),ctypes.byref(s))
        elapsed=time.perf_counter_ns()-start
        results.append((ans,elapsed,s))
    assert len(set(p[0] for p in results))==1
    s=results[0][2]
    return {"state":{1:"SAT",0:"UNSAT",-2:"WIDTH_BUDGET",-3:"NODE_BUDGET",-4:"INVALID"}.get(results[0][0],"ERROR"),
            "median_ns":int(statistics.median(t for _,t,_ in results)),
            "factor_checks":s.factor_checks,"clause_checks":s.clause_checks,
            "observed_width":s.max_width,"search_nodes":s.search_nodes,
            "factors_created":s.factors_created}

def run_test_and_benchmark():
    with tempfile.TemporaryDirectory() as temp:
        lib=compile_kernel(temp)
        cases=0
        # Independent exhaustive check across different formulas, SAT and UNSAT.
        hand=[(0,[]),(1,[[(0,0)],[(0,1)]]),(2,[[(0,0)],[(1,0)]]),
              (2,[[(0,0),(1,0)],[(0,1)],[(1,1)]])]
        for n,cs in hand:
            f=as_formula(n,cs)
            expected=exhaustive(cs,n)
            for method in ("ve","dpll"):
                got=run(lib,f,method)["state"]
                assert got==("SAT" if expected else "UNSAT"),(n,cs,method,got)
            cases+=1
        # Adversarial exhaustive oracle on random SAT/UNSAT CNFs; separate Python evaluator.
        for n in range(2,9):
            rng=random.Random(1907+n)
            for sample in range(18):
                cs=[]
                for _ in range(1+n*3):
                    width=rng.randrange(1,min(n,3)+1)
                    vars_=rng.sample(range(n),width)
                    cs.append([(v,rng.randrange(2)) for v in vars_])
                f=as_formula(n,cs)
                expected=exhaustive(cs,n)
                for method in ("ve","dpll"):
                    got=run(lib,f,method)["state"]
                    assert got==("SAT" if expected else "UNSAT"),(n,sample,method,got)
                cases+=1
        # Explicitly check that INVALID inputs fail closed, never alias UNSAT.
        for invalid in [as_formula(2,[[(0,0),(0,1)]]),as_formula(1,[[(3,0)]]),as_formula(1,[[(0,2)]])]:
            for method in ("ve","dpll"):
                assert run(lib,invalid,method)["state"]=="INVALID"
        # Empty formula is satisfiable; empty != zero != missing observation.
        assert run(lib,as_formula(0,[]),"ve")["state"]=="SAT"
        # Unknown rather than UNSAT for width-bounded elimination on dense graph.
        rows=[]
        for n in (6,8,10,12,16,20,24,30):
            for structure in ("banded","uniform"):
                for sample in range(3):
                    seed=1748365262+n*101+sample*7
                    cs,witness=planted_fixture(n,structure,seed)
                    f=as_formula(n,cs)
                    assert lib.raf_sat_check_certificate(ctypes.byref(f),witness)==1
                    if n<=12:
                        assert exhaustive(cs,n)
                    ve=run(lib,f,"ve",3)
                    dpll=run(lib,f,"dpll",3)
                    assert dpll["state"]=="SAT",(n,structure,sample,dpll)
                    assert ve["state"] in ("SAT","WIDTH_BUDGET"),(n,structure,sample,ve)
                    rows.append({"n":n,"m":len(cs),"structure":structure,"sample":sample,"seed":seed,"witness_checked":True,"ve":ve,"dpll":dpll})
                    cases+=1
        invalid=as_formula(2,[[(0,0),(0,1)]])
        assert run(lib,invalid,"ve")["state"]=="INVALID"
        assert run(lib,invalid,"dpll")["state"]=="INVALID"
        # Reproducible summary counts, machine/platform-bound times.
        report={"experiment":"SAT_STRUCTURE_L0_V1","scope":"synthetic planted 3CNF + exhaustive small instances", "cases_checked":cases,
            "source_sha256":hashlib.sha256(SRC.read_bytes()).hexdigest(),
            "header_sha256":hashlib.sha256(HEADER.read_bytes()).hexdigest(),
            "platform":platform.platform(),"compiler":os.environ.get("CC","cc"),
            "restrictions":{"no_private_shards":True,"no_general_P_equals_NP_claim":True,"bounds":{"n_max":MAX_N,"m_max":MAX_M,"width_max":MAX_W},
                "ve_width_budget_means_unsolved":True,"host_measurements_not_device_proof":True},
            "rows":rows}
        return report

if __name__=="__main__":
    report=run_test_and_benchmark()
    out=ROOT/"receipts/SAT_STRUCTURE_LOCAL_MEASUREMENT_20261016.json"
    out.write_text(json.dumps(report,indent=2,sort_keys=True)+"\n",encoding="utf-8")
    summary={"cases":report["cases_checked"],"ve_sat":sum(r["ve"]["state"]=="SAT" for r in report["rows"]),
             "ve_budget":sum(r["ve"]["state"]=="WIDTH_BUDGET" for r in report["rows"]),
             "dpll_sat":sum(r["dpll"]["state"]=="SAT" for r in report["rows"]),
             "source_sha256":report["source_sha256"],"receipt":str(out)}
    print(json.dumps(summary,sort_keys=True))
