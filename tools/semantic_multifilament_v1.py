#!/usr/bin/env python3
"""RAFAELIA semantic multi-filament V1: read-only, bounded, NON-EVIDENTIARY routing.

Extends the existing complex-network-design-relations.v1 without replacing Mapa
authority or inferring scientific, cryptographic, legal or release claims.
stdlib-only HOSTED inspector; not freestanding or an ML/neural implementation.
"""
import argparse
import hashlib
import json
import math
import sys
from collections import deque
from pathlib import Path

SCHEMA = "rafaelia.complex-network-design-relations.v1"
REPORT_SCHEMA = "rafaelia.semantic-multifilament-inspection.v1"
MAX_INPUT = 262144
MAX_NODES = 4096
MAX_EDGES = 8192
MAX_DEPTH = 5
MAX_PATHS = 32
EFFECTS = {"NONE", "ROUTING_ONLY", "EVIDENCES_BOUNDED", "CONTRADICTS", "SUPPORT_LIMITED"}
REQUIRED_POLICY = {
    "decorative_edges_allowed": False,
    "source_ref_required": True,
    "boundary_or_falsifier_required": True,
    "next_gate_required": True,
    "analogy_evidence_effect": "NONE",
    "cooccurrence_evidence_effect": "NONE",
}
NO_EVIDENCE_KINDS = {"ANALOGY_OF", "COOCCURS_WITH"}
NO_PROMOTION = {
    "claim_allowed": False,
    "release_allowed": False,
    "automatic_promotion": False,
    "source_receipt_authenticated": False,
}

class Invalid(ValueError):
    pass

def unique_pairs(pairs):
    result = {}
    for key, value in pairs:
        if key in result:
            raise Invalid("DUPLICATE_JSON_KEY:" + key)
        result[key] = value
    return result

def nonempty(value, label):
    if not isinstance(value, str) or not value.strip() or len(value) > 2048:
        raise Invalid("INVALID_" + label)
    return value

def read_graph(path):
    p = Path(path)
    if p.is_symlink() or not p.is_file():
        raise Invalid("GRAPH_MISSING_OR_SYMLINK")
    raw = p.read_bytes()
    if len(raw) > MAX_INPUT:
        raise Invalid("GRAPH_TOO_LARGE")
    try:
        data = json.loads(raw.decode("utf-8"), object_pairs_hook=unique_pairs,
                          parse_constant=lambda x: (_ for _ in ()).throw(Invalid("NONFINITE_JSON")))
    except (UnicodeError, json.JSONDecodeError) as exc:
        raise Invalid("GRAPH_NOT_JSON") from exc
    nodes, edges = validate_graph(data)
    return nodes, edges, hashlib.sha256(raw).hexdigest()

def validate_graph(data):
    if not isinstance(data, dict) or data.get("schema") != SCHEMA:
        raise Invalid("UNTRUSTED_GRAPH_SCHEMA")
    if data.get("claim_allowed") is not False:
        raise Invalid("GRAPH_CLAIM_MUST_BE_FALSE")
    policy = data.get("graph_policy")
    if not isinstance(policy, dict) or any(policy.get(k) != v for k,v in REQUIRED_POLICY.items()):
        raise Invalid("GRAPH_POLICY_WEAKENED")
    node_rows, edge_rows = data.get("nodes"), data.get("relations")
    if not isinstance(node_rows,list) or not 0 < len(node_rows) <= MAX_NODES:
        raise Invalid("BAD_NODE_COUNT")
    if not isinstance(edge_rows,list) or len(edge_rows) > MAX_EDGES:
        raise Invalid("BAD_EDGE_COUNT")
    nodes = {}
    for n in node_rows:
        if not isinstance(n,dict):
            raise Invalid("NODE_NOT_OBJECT")
        identity=nonempty(n.get("id"),"NODE_ID")
        if identity in nodes:
            raise Invalid("DUPLICATE_NODE_ID")
        nonempty(n.get("kind"),"NODE_KIND")
        nonempty(n.get("authority"),"NODE_AUTHORITY")
        nodes[identity]=n
    edges = []
    seen = set()
    for e in edge_rows:
        if not isinstance(e,dict):
            raise Invalid("EDGE_NOT_OBJECT")
        eid = nonempty(e.get("relation_id"),"EDGE_ID")
        if eid in seen:
            raise Invalid("DUPLICATE_EDGE_ID")
        seen.add(eid)
        start=nonempty(e.get("from"),"FROM")
        end=nonempty(e.get("to"),"TO")
        if start not in nodes or end not in nodes:
            raise Invalid("DANGLING_ENDPOINT")
        kind=nonempty(e.get("relation_type"),"RELATION_TYPE")
        source=nonempty(e.get("source_ref"),"SOURCE_REF")
        nonempty(e.get("authority_domain"),"AUTHORITY_DOMAIN")
        state=nonempty(e.get("uncertainty_state"),"UNCERTAINTY_STATE")
        effect=nonempty(e.get("evidence_effect"),"EVIDENCE_EFFECT")
        nonempty(e.get("boundary_or_falsifier"),"BOUNDARY_OR_FALSIFIER")
        nonempty(e.get("next_gate"),"NEXT_GATE")
        if effect not in EFFECTS:
            raise Invalid("UNKNOWN_EVIDENCE_EFFECT")
        if kind in NO_EVIDENCE_KINDS and effect != "NONE":
            raise Invalid("NON_EVIDENTIARY_EDGE_PROMOTED")
        if state.startswith("TOKEN_VAZIO") and effect in {"EVIDENCES_BOUNDED","SUPPORT_LIMITED"}:
            raise Invalid("TOKEN_VAZIO_PROMOTED")
        # A string locator is not an authenticated signature or an executed gate.
        if source == "TOKEN_VAZIO" and effect != "NONE":
            raise Invalid("EMPTY_SOURCE_PROMOTED")
        edges.append(e)
    return nodes, edges

def classify_path(edge_list):
    effects = [e["evidence_effect"] for e in edge_list]
    unresolved = any(e["uncertainty_state"].startswith("TOKEN_VAZIO") or
                     e["evidence_effect"] == "NONE" for e in edge_list)
    contradictions = [e["relation_id"] for e in edge_list
                      if e["evidence_effect"] == "CONTRADICTS"]
    return {
        "epistemic_state": "UNRESOLVED_OR_NON_EVIDENTIARY" if unresolved else
                           "RELATIONSHIP_CANDIDATE_NOT_PROOF",
        "has_contradiction": bool(contradictions),
        "contradiction_edges": contradictions,
        "relation_ids": [e["relation_id"] for e in edge_list],
        "relation_types": [e["relation_type"] for e in edge_list],
        "evidence_effects": effects,
        "gates_to_run": [e["next_gate"] for e in edge_list],
        "not_an_authenticated_evidence_chain": True,
    }

def route(nodes, edges, start, end, max_depth=3, max_paths=12):
    if start not in nodes or end not in nodes:
        raise Invalid("UNKNOWN_ROUTE_NODE")
    if type(max_depth) is not int or not 1 <= max_depth <= MAX_DEPTH:
        raise Invalid("BAD_MAX_DEPTH")
    if type(max_paths) is not int or not 1 <= max_paths <= MAX_PATHS:
        raise Invalid("BAD_MAX_PATHS")
    links = {}
    for edge in edges:
        links.setdefault(edge["from"], []).append(edge)
    for value in links.values():
        value.sort(key=lambda e:(e["relation_id"],e["to"]))
    results, q = [], deque([(start, (start,), ())])
    while q and len(results) < max_paths:
        current, visited, steps = q.popleft()
        if len(steps) >= max_depth:
            continue
        for edge in links.get(current, []):
            dest = edge["to"]
            if dest in visited:
                continue
            path = steps + (edge,)
            if dest == end:
                results.append({"nodes":list(visited+(dest,)), **classify_path(path)})
                if len(results) >= max_paths:
                    break
            else:
                q.append((dest, visited + (dest,), path))
    return results

def nested_log(x, depth=2):
    if type(depth) is not int or not 1 <= depth <= 4:
        raise Invalid("BAD_LOG_DEPTH")
    if not isinstance(x,(int,float)) or isinstance(x,bool) or not math.isfinite(x):
        raise Invalid("NOT_FINITE_REAL")
    result=float(x)
    for _ in range(depth):
        if result <= 0:
            raise Invalid("LOG_DOMAIN_INVALID")
        result=math.log(result)
    return result

def nested_log_derivative(x, depth=2):
    if type(depth) is not int or not 1 <= depth <= 4:
        raise Invalid("BAD_LOG_DEPTH")
    if not isinstance(x,(int,float)) or isinstance(x,bool) or not math.isfinite(x):
        raise Invalid("NOT_FINITE_REAL")
    value=float(x)
    product=1.0
    # d(log^n(x))/dx = 1/(x*log(x)*...*log^(n-1)(x))
    for i in range(depth):
        if value <= 0:
            raise Invalid("LOG_DOMAIN_INVALID")
        product *= value
        if not math.isfinite(product) or product == 0:
            raise Invalid("DERIVATIVE_NUMERIC_RANGE")
        value = math.log(value)
    if not math.isfinite(value):
        raise Invalid("LOG_DOMAIN_INVALID")
    return 1.0/product

def nested_exp_inverse(y, depth=2):
    if type(depth) is not int or not 1 <= depth <= 4:
        raise Invalid("BAD_LOG_DEPTH")
    if not isinstance(y,(int,float)) or isinstance(y,bool) or not math.isfinite(y):
        raise Invalid("NOT_FINITE_REAL")
    value=float(y)
    for _ in range(depth):
        try:
            value=math.exp(value)
        except OverflowError as exc:
            raise Invalid("INVERSE_NUMERIC_OVERFLOW") from exc
        if not math.isfinite(value):
            raise Invalid("INVERSE_NUMERIC_OVERFLOW")
    return value

def loglog_derivative_antiderivative(x, constant=0.0):
    # Only the antiderivative of d/dx log(log(x)), NOT integral of log(log(x)).
    if not isinstance(constant,(int,float)) or isinstance(constant,bool) or not math.isfinite(constant):
        raise Invalid("NONFINITE_INTEGRATION_CONSTANT")
    return nested_log(x,2) + constant

def math_probe(x, depth=2):
    y=nested_log(x,depth)
    derivative=nested_log_derivative(x,depth)
    inverse=nested_exp_inverse(y,depth)
    residual=abs(inverse-float(x))/max(1.0,abs(float(x)))
    if not math.isfinite(residual):
        raise Invalid("NONFINITE_RESIDUAL")
    return {
        "depth":depth,"x":float(x),"nested_log":y,"derivative":derivative,
        "inverse_roundtrip_residual":residual,
        "near_domain_boundary_heuristic":depth==2 and x<1.1,
        "mathematical_domain_validated":True,
        "inverse_is_not_unique_antiderivative":True,
        **NO_PROMOTION,
    }

def inspect(graph_path, start, end, max_depth=3):
    nodes, edges, digest=read_graph(graph_path)
    paths=route(nodes,edges,start,end,max_depth=max_depth)
    return {
        "schema": REPORT_SCHEMA,
        "input_sha256":digest,
        "source":str(graph_path),
        "source_locator_authentication":"NOT_PERFORMED",
        "paths":paths,
        "path_count":len(paths),
        "no_path_state":"NOT_OBSERVED_NOT_NEGATIVE_PROOF" if not paths else "NOT_APPLICABLE",
        "transitive_evidence_inference":False,
        "graph_schema_verified_only":True,
        **NO_PROMOTION,
    }

def main(argv=None):
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument("--graph",default=str(Path(__file__).resolve().parents[1]/
        "data/governance/complex-network-design-relations.v1.json"))
    p.add_argument("--from-id",dest="start",required=True)
    p.add_argument("--to-id",dest="end",required=True)
    p.add_argument("--max-depth",type=int,default=3)
    p.add_argument("--math-x",type=float,default=None)
    p.add_argument("--math-depth",type=int,default=2)
    args=p.parse_args(argv)
    try:
        report=inspect(args.graph,args.start,args.end,args.max_depth)
        if args.math_x is not None:
            report["math_probe"]=math_probe(args.math_x,args.math_depth)
        print(json.dumps(report,sort_keys=True,ensure_ascii=True,indent=2,allow_nan=False))
        return 0
    except (Invalid,OSError,TypeError,OverflowError) as exc:
        print("FAIL_CLOSED:"+str(exc),file=sys.stderr)
        return 2

if __name__=="__main__":
    sys.exit(main())
