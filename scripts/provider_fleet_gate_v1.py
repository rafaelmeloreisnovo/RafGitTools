#!/usr/bin/env python3
"""RAFAELIA P0 — hosted provider gate. One repository; no bulk administration."""
from __future__ import annotations
import argparse
import json
import os
from pathlib import Path
import re
import sys
import urllib.error
import urllib.request

API="https://api.github.com"
VERSION="2026-03-10"
SCHEMA="rafaelia.provider-fleet-security.v1"
REPO=re.compile(r"[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+\Z")
SHA=re.compile(r"[a-f0-9]{40}\Z")
POLICY={
    "required_status_checks":None,
    "enforce_admins":True,
    "required_pull_request_reviews":{
        "dismiss_stale_reviews":True,
        "require_code_owner_reviews":False,
        "required_approving_review_count":1,
        "require_last_push_approval":True,
    },
    "restrictions":None,
    "allow_force_pushes":False,
    "allow_deletions":False,
    "required_conversation_resolution":True,
}

class Hold(Exception):
    pass

class APIClient:
    def __init__(self,token):
        if not token or "\r" in token or "\n" in token:
            raise Hold("TOKEN_VAZIO_PAT_ENV")
        self.token=token
    def request(self,method,path,payload=None):
        if method not in ("GET","PUT"):
            raise Hold("METHOD_DENIED")
        if not re.fullmatch(r"/repos/[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+(?:/branches/[A-Za-z0-9_.-]+(?:/protection)?|/rulesets|/collaborators/[A-Za-z0-9_.-]+/permission)?",path):
            raise Hold("API_TARGET_DENIED")
        if method=="PUT" and (not path.endswith("/protection") or payload!=POLICY):
            raise Hold("UNAPPROVED_MUTATION")
        class NoRedirect(urllib.request.HTTPRedirectHandler):
            def redirect_request(self,*_args,**_kwargs):
                return None
        body=None if payload is None else json.dumps(payload,separators=(",",":")).encode()
        req=urllib.request.Request(API+path,data=body,method=method,headers={
            "Authorization":"Bearer "+self.token,
            "Accept":"application/vnd.github+json",
            "X-GitHub-Api-Version":VERSION,
            "Content-Type":"application/json",
            "User-Agent":"rafaelia-provider-fleet-p0-v1",
        })
        try:
            with urllib.request.build_opener(NoRedirect).open(req,timeout=15) as response:
                raw=response.read(131073)
                if len(raw)>131072:
                    raise Hold("PROVIDER_RESPONSE_TOO_LARGE")
                return response.status,json.loads(raw.decode()) if raw else {}
        except urllib.error.HTTPError as exc:
            return exc.code,None
        except (urllib.error.URLError,TimeoutError,ValueError,UnicodeError):
            raise Hold("PROVIDER_UNREACHABLE") from None

def load_manifest(path):
    data=json.loads(Path(path).read_text(encoding="utf-8"))
    if data.get("schema")!=SCHEMA or data.get("claim_allowed") is not False:
        raise Hold("MANIFEST_SCHEMA_OR_CLAIM")
    entries=data.get("repositories")
    if not isinstance(entries,list) or not entries:
        raise Hold("MANIFEST_EMPTY")
    result={}; ids=set()
    for entry in entries:
        if not isinstance(entry,dict):
            raise Hold("MANIFEST_ENTRY_NOT_OBJECT")
        name=entry.get("name"); branch=entry.get("default_branch"); sha=entry.get("head_sha")
        if not (isinstance(name,str) and REPO.fullmatch(name)
                and isinstance(branch,str) and re.fullmatch(r"[A-Za-z0-9_.-]+",branch)
                and isinstance(sha,str) and SHA.fullmatch(sha)
                and isinstance(entry.get("id"),int) and entry["id"]>0
                and entry.get("visibility") in ("public","private")):
            raise Hold("MANIFEST_ENTRY_INVALID")
        if name in result or entry["id"] in ids:
            raise Hold("MANIFEST_DUPLICATE")
        result[name]=entry;ids.add(entry["id"])
    return result

def check(api,entry,sha,reviewer):
    name=entry["name"];branch=entry["default_branch"]
    base="/repos/"+name;bp=base+"/branches/"+branch
    if sha!=entry["head_sha"]:
        raise Hold("MANIFEST_EXACT_SHA_MISMATCH")
    status,meta=api.request("GET",base)
    if status!=200 or not isinstance(meta,dict):
        raise Hold("PROVIDER_METADATA_UNKNOWN")
    if (meta.get("id")!=entry["id"] or meta.get("full_name")!=name
            or meta.get("default_branch")!=branch
            or meta.get("visibility")!=entry["visibility"]
            or not isinstance(meta.get("permissions"),dict)
            or meta["permissions"].get("admin") is not True):
        raise Hold("PROVIDER_ID_OR_ADMIN_DRIFT")
    status,branchinfo=api.request("GET",bp)
    if status!=200 or not isinstance(branchinfo,dict) or branchinfo.get("commit",{}).get("sha")!=sha:
        raise Hold("PROVIDER_BRANCH_SHA_DRIFT")
    protected=branchinfo.get("protected")
    if type(protected) is not bool:
        raise Hold("PROVIDER_PROTECTION_UNKNOWN")
    status,_=api.request("GET",bp+"/protection")
    if status not in (200,404):
        raise Hold("PROVIDER_PROTECTION_UNREADABLE")
    if protected!=(status==200):
        raise Hold("PROVIDER_PROTECTION_INCONSISTENT")
    status,rules=api.request("GET",base+"/rulesets")
    if status!=200 or not isinstance(rules,list):
        raise Hold("PROVIDER_RULESETS_UNREADABLE_OR_PLAN")
    if rules:
        raise Hold("PROVIDER_RULESET_CONFLICT_REVIEW")
    if protected:
        raise Hold("PREEXISTING_PROTECTION_HOLD")
    if entry["visibility"]!="public":
        raise Hold("PRIVATE_POLICY_PLAN_OR_REVIEW_REQUIRED")
    if not reviewer or reviewer.lower()==name.split("/")[0].lower() or not re.fullmatch(r"[A-Za-z0-9-]+",reviewer):
        raise Hold("INDEPENDENT_REVIEWER_NOT_VERIFIED")
    status,permission=api.request("GET",base+"/collaborators/"+reviewer+"/permission")
    if status!=200 or not isinstance(permission,dict) or permission.get("permission") not in ("write","maintain","admin"):
        raise Hold("INDEPENDENT_REVIEWER_NOT_VERIFIED")
    return {"repository":name,"repository_id":entry["id"],"branch":branch,
            "sha":sha,"protected_before":False,"rulesets_before":0,"reviewer_probed":True}

def verify_after(api,entry,sha):
    base="/repos/"+entry["name"]+"/branches/"+entry["default_branch"]
    status,branch=api.request("GET",base)
    if status!=200 or not isinstance(branch,dict) or branch.get("commit",{}).get("sha")!=sha or branch.get("protected") is not True:
        raise Hold("POST_BRANCH_NOT_VERIFIED")
    status,policy=api.request("GET",base+"/protection")
    if status!=200 or not isinstance(policy,dict):
        raise Hold("POST_PROTECTION_NOT_VERIFIED")
    def enabled(key):
        item=policy.get(key)
        return item.get("enabled") if isinstance(item,dict) else item
    if (enabled("enforce_admins") is not True
            or enabled("allow_force_pushes") is not False
            or enabled("allow_deletions") is not False
            or enabled("required_conversation_resolution") is not True):
        raise Hold("POST_SECURITY_FLAGS_MISMATCH")
    reviews=policy.get("required_pull_request_reviews")
    if (not isinstance(reviews,dict)
            or reviews.get("required_approving_review_count")!=1
            or reviews.get("dismiss_stale_reviews") is not True
            or reviews.get("require_last_push_approval") is not True):
        raise Hold("POST_REVIEW_GATE_MISMATCH")

def execute(api,entry,sha,reviewer,apply=False):
    receipt={"schema":SCHEMA,"repo":entry["name"],"head":sha,
             "mode":"APPLY_SINGLE" if apply else "AUDIT_ONE","claim_allowed":False,
             "secret_value_recorded":False,"mutation_attempted":False,
             "provider_write_verified":False,"negative_rejection_test":"NOT_RUN",
             "rollback":"PRESTATE_ABSENT_MANUAL_REVIEW_REQUIRED"}
    try:
        proof=check(api,entry,sha,reviewer)
        receipt.update(proof)
        if not apply:
            receipt["status"]="AUDIT_ELIGIBLE_NOT_APPLIED"
            return receipt
        if check(api,entry,sha,reviewer)!=proof:
            raise Hold("PROVIDER_PRESTATE_CHANGED")
        receipt["mutation_attempted"]=True
        status,_=api.request("PUT","/repos/"+entry["name"]+"/branches/"+entry["default_branch"]+"/protection",POLICY)
        if status!=200:
            raise Hold("APPLY_NOT_CONFIRMED_POSSIBLE_MUTATION")
        verify_after(api,entry,sha)
        receipt["provider_write_verified"]=True
        receipt["status"]="PROVIDER_POLICY_READBACK_PASS_NEGATIVE_TEST_PENDING"
    except Hold as error:
        receipt["status"]="HOLD_"+str(error)
    return receipt

def main(argv=None):
    parser=argparse.ArgumentParser()
    parser.add_argument("--manifest",required=True)
    parser.add_argument("--repo",required=True)
    parser.add_argument("--expected-sha",required=True)
    parser.add_argument("--reviewer",default="")
    parser.add_argument("--receipt",required=True)
    parser.add_argument("--apply-single",action="store_true")
    parser.add_argument("--confirm",default="")
    args=parser.parse_args(argv)
    result={"schema":SCHEMA,"status":"HOLD","claim_allowed":False,
            "secret_value_recorded":False,"repo":args.repo}
    try:
        manifest=load_manifest(args.manifest)
        if args.repo not in manifest:
            raise Hold("REPO_NOT_ALLOWLISTED")
        if not SHA.fullmatch(args.expected_sha):
            raise Hold("SHA_INVALID")
        if args.apply_single and args.confirm!=args.repo+"@"+args.expected_sha:
            raise Hold("EXACT_OPERATOR_CONFIRMATION_MISSING")
        api=APIClient(os.environ.get("PAT_ENV",""))
        result=execute(api,manifest[args.repo],args.expected_sha,args.reviewer,args.apply_single)
    except Hold as error:
        result["status"]="HOLD_"+str(error)
    except FileNotFoundError:
        result["status"]="HOLD_MANIFEST_NOT_FOUND"
    path=Path(args.receipt);path.parent.mkdir(parents=True,exist_ok=True)
    with path.open("x",encoding="utf-8") as output:
        output.write(json.dumps(result,sort_keys=True,separators=(",",":"))+"\n")
    print(result["status"])
    return 0 if result["status"] in ("AUDIT_ELIGIBLE_NOT_APPLIED","PROVIDER_POLICY_READBACK_PASS_NEGATIVE_TEST_PENDING") else 2

if __name__=="__main__":
    sys.exit(main())
