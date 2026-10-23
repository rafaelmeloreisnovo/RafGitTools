#!/usr/bin/env python3
"""Read-only PAT_AGENTS preflight; never claims repository-creation permission."""
import argparse
import json
import os
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


def main(argv=None):
    parser = argparse.ArgumentParser()
    parser.add_argument("--owner", required=True)
    parser.add_argument("--receipt", default="")
    args = parser.parse_args(argv)
    token = os.environ.get("RAFAELIA_PREFLIGHT_TOKEN", "")
    transport = provider_client(token) if token else None
    observation = preflight(args.owner, client=transport)
    output = json.dumps(observation, sort_keys=True, ensure_ascii=True) + "\n"
    if args.receipt:
        path = Path(args.receipt)
        path.parent.mkdir(parents=True, exist_ok=True)
        with path.open("x", encoding="utf-8") as fd:
            fd.write(output)
    sys.stdout.write(output)
    return 0 if observation["status"] in (
        "IDENTITY_READBACK_ONLY", "MEMBERSHIP_READBACK_ONLY"
    ) else 3


if __name__ == "__main__":
    raise SystemExit(main())
