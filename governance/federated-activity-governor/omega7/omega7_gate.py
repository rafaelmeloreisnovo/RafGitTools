#!/usr/bin/env python3
"""RAFAELIA Ω7: offline seven-direction last-condition contract. No provider writes."""
import hashlib
import json
import re
import sys
from pathlib import Path

SCHEMA="RAFAELIA_OMEGA7_LAST_CONDITION_V1"
AXES=("direta","inversa","derivativa","antiderivativa","recursiva","ortogonal","integrativa")
REQUIRED={
 "rll_promotion":("refs_match","all_applicable_checks_green","scientific_gates_green",
                   "required_review_approved","provider_merge_enforcement_proven",
                   "negative_bypass_test_proven","no_unreviewed_path_regression",
                   "rollback_ref_available","claim_boundary_false"),
 "inventory_pointer":("source_predecessor_match","partition_readback_verified",
                      "receipt_parent_valid","dedup_identity_scope_reconciled",
                      "same_revision_cas_proven","single_writer_exclusivity_proven",
                      "cursor_valid_for_exact_query","rollback_ref_available",
                      "claim_boundary_false")
}
MAX_BYTES=65536
class Invalid(ValueError): pass

def canonical(x):
    return json.dumps(x,ensure_ascii=False,sort_keys=True,separators=(",",":"),allow_nan=False)
def equal(a,b):
    return canonical(a)==canonical(b)
def sha(x):
    return hashlib.sha256(canonical(x).encode("utf-8")).hexdigest()
def validate_state(obj):
    if not isinstance(obj,dict) or len(obj)>128: raise Invalid("INVALID_STATE")
    for key in obj:
        if not isinstance(key,str) or not key or len(key)>128: raise Invalid("INVALID_KEY")
    def walk(x,depth=0):
        if depth>8: raise Invalid("DEPTH")
        if x is None or type(x) in (str,int,bool): return
        if isinstance(x,list):
            for y in x: walk(y,depth+1)
        elif isinstance(x,dict):
            if any(not isinstance(k,str) for k in x): raise Invalid("KEY_TYPE")
            for y in x.values(): walk(y,depth+1)
        else: raise Invalid("NONCANONICAL_VALUE")
    walk(obj)
    if len(canonical(obj).encode("utf-8"))>MAX_BYTES: raise Invalid("STATE_TOO_LARGE")
    return obj

def direta(gate):
    if not isinstance(gate,dict) or gate.get("kind") not in REQUIRED: raise Invalid("KIND")
    kind=gate["kind"]
    p=gate.get("predicates")
    if not isinstance(p,dict) or set(p)!=set(REQUIRED[kind]): raise Invalid("PREDICATES")
    if any(p[k] is not True for k in REQUIRED[kind]): raise Invalid("PREDICATE_FALSE_OR_UNKNOWN")
    src,obs=(("source_sha","observed_sha") if kind=="rll_promotion" else
             ("source_checkpoint","observed_checkpoint"))
    pattern=r"[a-f0-9]{40}" if kind=="rll_promotion" else r"B[0-9]{4}_COMMITTED_[0-9]{8}"
    a,b=gate.get(src),gate.get(obs)
    if not isinstance(a,str) or not isinstance(b,str) or not re.fullmatch(pattern,a) or a!=b:
        raise Invalid("SOURCE_ID_MISMATCH")
    return a

def derivativa(before,after):
    validate_state(before);validate_state(after)
    out=[]
    for k in sorted(before.keys() | after.keys()):
        x,y=k in before,k in after
        if x==y and (not x or equal(before[k],after[k])): continue
        out.append({"key":k,"before_exists":x,"before":before.get(k),
                    "after_exists":y,"after":after.get(k)})
    return out

def antiderivativa(before,changes):
    validate_state(before)
    if not isinstance(changes,list) or len(changes)>128: raise Invalid("DELTA_SIZE")
    out=dict(before);seen=set()
    for c in changes:
        keys={"key","before_exists","before","after_exists","after"}
        if not isinstance(c,dict) or set(c)!=keys: raise Invalid("DELTA_SHAPE")
        k=c["key"]
        if not isinstance(k,str) or not k or len(k)>128 or k in seen: raise Invalid("DELTA_KEY")
        seen.add(k)
        be,ae=c["before_exists"],c["after_exists"]
        if type(be) is not bool or type(ae) is not bool: raise Invalid("DELTA_EXISTENCE_TYPE")
        if be!=(k in out) or (be and not equal(out[k],c["before"])):
            raise Invalid("DELTA_PRECONDITION")
        if (not be and c["before"] is not None) or (not ae and c["after"] is not None):
            raise Invalid("DELTA_ABSENCE")
        if ae: out[k]=c["after"]
        else: out.pop(k,None)
    return validate_state(out)

def inversa(after,changes):
    if not isinstance(changes,list): raise Invalid("INVERSE_DELTA")
    rev=[]
    for c in reversed(changes):
        if not isinstance(c,dict) or set(c)!={"key","before_exists","before","after_exists","after"}:
            raise Invalid("INVERSE_ENTRY")
        rev.append({"key":c["key"],"before_exists":c["after_exists"],
                    "before":c["after"],"after_exists":c["before_exists"],"after":c["before"]})
    return antiderivativa(after,rev)

def recursiva(before,chain):
    validate_state(before)
    if not isinstance(chain,list) or not 1<=len(chain)<=42: raise Invalid("CHAIN_LIMIT")
    state=before;seen={sha(before)}
    for step in chain:
        if not isinstance(step,dict) or set(step)!={"predecessor_digest","successor_digest","delta"}:
            raise Invalid("CHAIN_SHAPE")
        if step["predecessor_digest"]!=sha(state): raise Invalid("BROKEN_PARENT")
        nxt=antiderivativa(state,step["delta"])
        if sha(nxt)!=step["successor_digest"]: raise Invalid("BROKEN_SUCCESSOR")
        if sha(nxt) in seen: raise Invalid("CYCLE")
        seen.add(sha(nxt));state=nxt
    return state

def ortogonal(witnesses,source):
    if not isinstance(source,str) or not re.fullmatch(r"[a-f0-9]{40}",source):
        raise Invalid("WITNESS_SOURCE")
    if not isinstance(witnesses,list) or len(witnesses)!=4: raise Invalid("WITNESSES")
    axes=set();receipts=set()
    for w in witnesses:
        if not isinstance(w,dict) or set(w)!={"axis","provider","receipt","source_identity"}:
            raise Invalid("WITNESS_SHAPE")
        axis,ref,provider=w["axis"],w["receipt"],w["provider"]
        if axis not in ("source","execution","review","rollback") or axis in axes:
            raise Invalid("WITNESS_AXIS")
        if not isinstance(ref,str) or not ref or len(ref)>256 or ref in receipts:
            raise Invalid("WITNESS_RECEIPT")
        if not isinstance(provider,str) or not provider or len(provider)>128:
            raise Invalid("WITNESS_PROVIDER")
        if w["source_identity"]!=source: raise Invalid("STALE_WITNESS")
        axes.add(axis);receipts.add(ref)
    return True # Structural separation, NOT provider attestation or review independence.

def evaluate(payload):
    directions={axis:{"state":"BLOCKED"} for axis in AXES};reasons=[]
    try:
        if not isinstance(payload,dict) or set(payload)!={"schema","gate","before","after","chain","witnesses"}:
            raise Invalid("ENVELOPE")
        if payload["schema"]!=SCHEMA: raise Invalid("SCHEMA")
        before,after=payload["before"],payload["after"]
        source=direta(payload["gate"])
        directions["direta"]={"state":"VALIDATED_SCOPED"}
        delta=derivativa(before,after)
        directions["derivativa"]={"state":"VALIDATED_SCOPED","changes":len(delta)}
        rebuilt=antiderivativa(before,delta)
        if not equal(rebuilt,after): raise Invalid("FORWARD_ROUNDTRIP")
        directions["antiderivativa"]={"state":"VALIDATED_SCOPED","digest":sha(rebuilt)}
        if not equal(inversa(after,delta),before): raise Invalid("INVERSE_ROUNDTRIP")
        directions["inversa"]={"state":"VALIDATED_SCOPED"}
        if not equal(recursiva(before,payload["chain"]),after): raise Invalid("CHAIN_END")
        directions["recursiva"]={"state":"VALIDATED_SCOPED","length":len(payload["chain"])}
        witness_source=source if payload["gate"]["kind"]=="rll_promotion" else before.get("source_identity")
        ortogonal(payload["witnesses"],witness_source)
        directions["ortogonal"]={"state":"VALIDATED_SCOPED","independence_proven":False}
    except (Invalid,ValueError,TypeError,KeyError,OverflowError,RecursionError) as exc:
        reasons.append(str(exc) if isinstance(exc,Invalid) else "MALFORMED_INPUT")
    ready=(not reasons and all(directions[a]["state"]=="VALIDATED_SCOPED" for a in AXES[:-1]))
    directions["integrativa"]={"state":"VALIDATED_SCOPED" if ready else "BLOCKED"}
    return {"schema":SCHEMA,"directions":directions,
            "decision":"READY_FOR_INDEPENDENT_PROVIDER_RECHECK" if ready else "BLOCKED",
            "reasons":reasons,"claim_allowed":False,"provider_mutation_authorized":False,
            "note":"Offline structural evidence validation only; provider truth/authorization not proved."}

def reject_duplicates(pairs):
    out={}
    for k,v in pairs:
        if k in out: raise Invalid("DUPLICATE_JSON_KEY")
        out[k]=v
    return out

def main():
    if len(sys.argv)!=2:
        print("usage: python3 omega7_gate.py evidence.json",file=sys.stderr);return 2
    try:
        path=Path(sys.argv[1])
        if path.stat().st_size>MAX_BYTES: raise Invalid("INPUT_LIMIT")
        raw=path.read_text(encoding="utf-8")
        payload=json.loads(raw,object_pairs_hook=reject_duplicates,
                           parse_constant=lambda x: (_ for _ in ()).throw(Invalid("NONFINITE")))
        result=evaluate(payload)
    except (OSError,UnicodeError,ValueError) as exc:
        result={"decision":"BLOCKED","reasons":["UNREADABLE_OR_INVALID_INPUT"],
                "claim_allowed":False,"provider_mutation_authorized":False}
    print(json.dumps(result,sort_keys=True,ensure_ascii=False,indent=2))
    return 0 if result["decision"]=="READY_FOR_INDEPENDENT_PROVIDER_RECHECK" else 3

if __name__=="__main__": sys.exit(main())
