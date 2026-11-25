#!/usr/bin/env python3
"""Read-only PAT_AGENTS preflight; never claims repository-creation permission."""
import argparse
import json
import os
import re
import sys
from pathlib import Path

from repository_factory_v1 import OWNERS, provider_client


class ReadOnlyTransport:
    def __init__(self, client):
        self.client = client

    def __call__(self, method, path):
        if method != "GET" or path not in (
            "/user",
            "/user/memberships/orgs/instituto-Rafael",
        ):
            raise ValueError("endpoint_not_allowlisted")
        return self.client("GET", path)


def preflight(owner, client=None):
    result = {
        "schema": "rafaelia.repo-factory-pat-agents-preflight.v1",
        "owner": owner,
        "status": "BLOCKED",
        "operation": "identity_and_org_membership_get_only",
        "secret_id": "PAT_AGENTS",
        "http_mutation_allowed": False,
        "repository_creation_permission": "TOKEN_VAZIO_NOT_TESTED",
        "actor_identity": "TOKEN_VAZIO",
        "organization_membership": "NOT_APPLICABLE",
        "claim_allowed": False,
    }
    if owner.casefold() not in OWNERS:
        result["status"] = "BLOCKED_OWNER"
        return result
    if client is None:
        result["status"] = "BLOCKED_SECRET_UNAVAILABLE"
        return result

    client = ReadOnlyTransport(client)
    status, actor = client("GET", "/user")
    if status != 200 or not isinstance(actor, dict):
        result["status"] = "PROVIDER_IDENTITY_TOKEN_VAZIO"
        return result
    login = actor.get("login")
    if not isinstance(login, str) or not login.strip():
        result["status"] = "PROVIDER_IDENTITY_TOKEN_VAZIO"
        return result
    if owner.casefold() == "rafaelmeloreisnovo":
        if login.casefold() != owner.casefold():
            result["status"] = "BLOCKED_ACTOR_MISMATCH"
            return result
        result["actor_identity"] = "MATCHED_EXACT_OWNER"
        result["status"] = "IDENTITY_READBACK_ONLY"
        return result

    # For the institute, an org membership observation is not creation authority.
    result["actor_identity"] = "AUTHENTICATED_LOGIN_OBSERVED"
    status, membership = client("GET", "/user/memberships/orgs/instituto-Rafael")
    if status != 200 or not isinstance(membership, dict):
        result["status"] = "ORG_MEMBERSHIP_TOKEN_VAZIO"
        result["organization_membership"] = "TOKEN_VAZIO"
        return result
    if membership.get("state") != "active":
        result["status"] = "ORG_MEMBERSHIP_TOKEN_VAZIO"
        result["organization_membership"] = "TOKEN_VAZIO"
        return result
    if membership.get("role") == "admin":
        result["organization_membership"] = "ACTIVE_ORG_ADMIN_OBSERVED"
    elif membership.get("role") == "member":
        result["organization_membership"] = "ACTIVE_ORG_MEMBER_OBSERVED"
    else:
        result["status"] = "ORG_MEMBERSHIP_TOKEN_VAZIO"
        result["organization_membership"] = "TOKEN_VAZIO"
        return result
    result["status"] = "MEMBERSHIP_READBACK_ONLY"
    return result


# Public targets only; private reads stay in the existing private-CI lane.
PUBLIC_TARGETS = {
    ("rafaelmeloreisnovo", "RafGitTools"): "rafaelmeloreisnovo/RafGitTools",
    ("instituto-rafael", "relativity-living-light"): "instituto-Rafael/relativity-living-light",
}


def target_preflight(owner, name, expected_main_sha, client=None):
    """GET-only exact public main readback; not PAT scope or write proof."""
    out = {"schema": "rafaelia.pat-agents-public-target-preflight.v1",
           "status": "BLOCKED", "claim_allowed": False,
           "http_mutation_allowed": False, "secret_id": "PAT_AGENTS",
           "effective_pat_scope": "TOKEN_VAZIO_PUBLIC_READ_NOT_SCOPE_PROOF",
           "write_permission": "TOKEN_VAZIO_NOT_TESTED"}
    repo = PUBLIC_TARGETS.get((owner.casefold(), name))
    if repo is None or not re.fullmatch(r"[0-9a-f]{40}", expected_main_sha):
        out["status"] = "BLOCKED_TARGET_OR_SHA"
        return out
    identity = preflight(owner, client)
    out["identity_status"] = identity["status"]
    if identity["status"] not in ("IDENTITY_READBACK_ONLY", "MEMBERSHIP_READBACK_ONLY"):
        return out
    # No arbitrary URL, host, method, ref, or target from caller.
    base = "/repos/" + repo
    paths = (base, base + "/git/ref/heads/main",
             base + "/git/commits/" + expected_main_sha)
    out["repository"] = repo
    out["expected_main_sha"] = expected_main_sha
    status, body = client("GET", paths[0])
    if (status != 200 or not isinstance(body, dict) or body.get("full_name") != repo
            or body.get("private") is not False or body.get("visibility") != "public"):
        out["status"] = "BLOCKED_PUBLIC_TARGET_IDENTITY"
        return out
    status, body = client("GET", paths[1])
    if (status != 200 or not isinstance(body, dict) or body.get("ref") != "refs/heads/main"
            or not isinstance(body.get("object"), dict)
            or body["object"].get("type") != "commit"
            or body["object"].get("sha") != expected_main_sha):
        out["status"] = "BLOCKED_TARGET_MAIN_DRIFT"
        return out
    status, body = client("GET", paths[2])
    if status != 200 or not isinstance(body, dict) or body.get("sha") != expected_main_sha:
        out["status"] = "BLOCKED_COMMIT_READBACK"
        return out
    out["status"] = "PUBLIC_EXACT_MAIN_READBACK_ONLY"
    return out


def main(argv=None):
    parser = argparse.ArgumentParser()
    parser.add_argument("--owner", required=True)
    parser.add_argument("--receipt", default="")
    parser.add_argument("--target-name", default="")
    parser.add_argument("--target-main-sha", default="")
    args = parser.parse_args(argv)
    token = os.environ.get("RAFAELIA_PREFLIGHT_TOKEN", "")
    transport = provider_client(token) if token else None
    observation = (target_preflight(args.owner, args.target_name, args.target_main_sha, transport)
                   if args.target_name or args.target_main_sha
                   else preflight(args.owner, client=transport))
    output = json.dumps(observation, sort_keys=True, ensure_ascii=True) + "\n"
    if args.receipt:
        path = Path(args.receipt)
        path.parent.mkdir(parents=True, exist_ok=True)
        with path.open("x", encoding="utf-8") as fd:
            fd.write(output)
    sys.stdout.write(output)
    return 0 if observation["status"] in (
        "IDENTITY_READBACK_ONLY", "MEMBERSHIP_READBACK_ONLY", "PUBLIC_EXACT_MAIN_READBACK_ONLY"
    ) else 3


if __name__ == "__main__":
    raise SystemExit(main())
