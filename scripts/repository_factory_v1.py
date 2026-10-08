#!/usr/bin/env python3
"""Manual, private-only GitHub repository creation adapter (hosted stdlib)."""
import argparse
import json
import os
import re
import sys
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.request import HTTPRedirectHandler, Request, build_opener

OWNERS = frozenset(("rafaelmeloreisnovo", "instituto-rafael"))
NAME = re.compile(r"[A-Za-z0-9][A-Za-z0-9._-]{0,99}\Z")
API = "https://api.github.com"


class NoAuthRedirect(HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        raise ValueError("redirect_denied")


def check_target(owner, name):
    if owner.casefold() not in OWNERS:
        return "UNAUTHORIZED_OWNER"
    if not NAME.fullmatch(name) or name in {".", ".."} or name.endswith(".git"):
        return "INVALID_NAME"
    return None


def receipt(status, owner, name, detail=None, observed=None):
    data = {
        "schema": "rafaelia.repository-factory-receipt.v1",
        "repository": owner + "/" + name,
        "visibility": "private",
        "status": status,
        "claim_allowed": False,
        "created_verified": status == "CREATED_READBACK_VERIFIED",
    }
    if detail:
        data["gap"] = detail
    if observed is not None and status == "CREATED_READBACK_VERIFIED":
        data["provider_repo_id"] = observed.get("id")
        data["provider_html_url"] = observed.get("html_url")
    return data


def plan_or_create(owner, name, operation="plan", confirm="", rights=False,
                   approval=False, client=None):
    error = check_target(owner, name)
    if error:
        return receipt("BLOCKED_INPUT", owner, name, error)
    if operation == "plan":
        return receipt("PLAN_ONLY", owner, name, "no_network_no_token_no_mutation")
    if operation != "create" or not rights or not approval or confirm != "CREATE:" + owner + "/" + name:
        return receipt("BLOCKED_APPROVAL", owner, name, "confirmation_rights_and_provider_gate_required")
    if client is None:
        return receipt("BLOCKED_TOKEN", owner, name, "dedicated_capability_unavailable")

    status, actor = client("GET", "/user")
    if status != 200 or not isinstance(actor, dict):
        return receipt("BLOCKED_PROVIDER_IDENTITY", owner, name)
    if owner.casefold() == "rafaelmeloreisnovo" and (
        str(actor.get("login", "")).casefold() != owner.casefold()
    ):
        return receipt("BLOCKED_ACTOR_MISMATCH", owner, name)

    target = "/repos/" + owner + "/" + name
    status, before = client("GET", target)
    if status == 200:
        if not isinstance(before, dict) or before.get("private") is not True:
            return receipt("BLOCKED_EXISTING_VISIBILITY", owner, name)
        if str(before.get("full_name", "")).casefold() != (owner + "/" + name).casefold():
            return receipt("BLOCKED_EXISTING_IDENTITY", owner, name)
        return receipt("ALREADY_EXISTS_PRIVATE_NO_MUTATION", owner, name)
    if status != 404:
        return receipt("BLOCKED_PRECHECK", owner, name, "absence_not_authoritatively_proven")

    endpoint = "/user/repos" if owner.casefold() == "rafaelmeloreisnovo" else "/orgs/" + owner + "/repos"
    create_status, _ = client("POST", endpoint, {
        "name": name, "private": True, "auto_init": True,
        "has_issues": True, "has_wiki": False,
    })
    if create_status != 201:
        return receipt("PROVIDER_REJECTED_OR_UNKNOWN", owner, name,
                       "HTTP_" + str(create_status) + ";no_automatic_retry")
    read_status, after = client("GET", target)
    if read_status != 200 or not isinstance(after, dict):
        return receipt("CREATED_READBACK_TOKEN_VAZIO", owner, name,
                       "POST_201_but_readback_missing;no_automatic_retry")
    if (str(after.get("full_name", "")).casefold() != (owner + "/" + name).casefold()
            or after.get("private") is not True
            or str((after.get("owner") or {}).get("login", "")).casefold() != owner.casefold()):
        return receipt("CREATED_READBACK_MISMATCH", owner, name,
                       "identity_or_visibility_mismatch;no_automatic_retry")
    return receipt("CREATED_READBACK_VERIFIED", owner, name, observed=after)


def provider_client(token):
    opener = build_opener(NoAuthRedirect())

    def request(method, path, payload=None):
        if not path.startswith("/") or "://" in path or ".." in path:
            return 0, None
        data = json.dumps(payload, sort_keys=True).encode("utf-8") if payload is not None else None
        req = Request(
            API + path, data=data, method=method,
            headers={
                "Accept": "application/vnd.github+json",
                "Authorization": "Bearer " + token,
                "X-GitHub-Api-Version": "2022-11-28",
                "Content-Type": "application/json",
            },
        )
        try:
            with opener.open(req, timeout=15) as result:
                raw = result.read(65537)
                if len(raw) > 65536:
                    return 0, None
                return result.status, json.loads(raw.decode("utf-8")) if raw else None
        except HTTPError as exc:
            return exc.code, None  # No body, headers, token or token metadata in receipts.
        except (URLError, OSError, ValueError, UnicodeError):
            return 0, None

    return request


def main(argv=None):
    parser = argparse.ArgumentParser()
    parser.add_argument("--owner", required=True)
    parser.add_argument("--name", required=True)
    parser.add_argument("--operation", choices=("plan", "create"), default="plan")
    parser.add_argument("--confirm", default="")
    parser.add_argument("--rights-attested", action="store_true")
    parser.add_argument("--receipt", default="")
    args = parser.parse_args(argv)

    approval = os.environ.get("RAFAELIA_REPO_FACTORY_APPROVED") == "yes"
    token = os.environ.get("RAFAELIA_REPO_FACTORY_TOKEN", "")
    client = provider_client(token) if args.operation == "create" and token else None
    outcome = plan_or_create(
        args.owner, args.name, operation=args.operation,
        confirm=args.confirm, rights=args.rights_attested,
        approval=approval, client=client,
    )
    output = json.dumps(outcome, sort_keys=True, ensure_ascii=False) + "\n"
    if args.receipt:
        path = Path(args.receipt)
        path.parent.mkdir(parents=True, exist_ok=True)
        with path.open("x", encoding="utf-8") as fd:
            fd.write(output)
    sys.stdout.write(output)
    return 0 if outcome["status"] in (
        "PLAN_ONLY", "ALREADY_EXISTS_PRIVATE_NO_MUTATION", "CREATED_READBACK_VERIFIED"
    ) else 3


if __name__ == "__main__":
    raise SystemExit(main())
