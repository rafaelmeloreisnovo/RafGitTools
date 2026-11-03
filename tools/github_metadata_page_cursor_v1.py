#!/usr/bin/env python3
"""RAFAELIA GitHub metadata page -> local hash-chained checkpoint.

Hosted POSIX/Termux adapter, stdlib only. No provider fetch, credentials, SQL,
raw corpus, application runtime, deployment or independent evidence claim.
The caller must authenticate provider responses outside this parser.
"""
import argparse
import fcntl
import hashlib
import json
import os
import re
import stat
import sys
from urllib.parse import quote

PAGE_SCHEMA = "rafaelia.github_metadata_page.v1"
RECEIPT_SCHEMA = "rafaelia.github_metadata_cursor_receipt.v1"
TOKEN = "TOKEN_VAZIO"
ZERO = "0" * 64
SHA = re.compile(r"^[0-9a-f]{40}$")
REPO = re.compile(r"^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$")
MAX_PAGE = 262144
MAX_LEDGER = 16 * 1024 * 1024


def canonical(obj):
    return json.dumps(obj, sort_keys=True, separators=(",", ":"),
                      ensure_ascii=True, allow_nan=False).encode("ascii")


def digest(data):
    return hashlib.sha256(data).hexdigest()


def deny(reason):
    raise ValueError("DENY_" + reason)


def page_from_bytes(raw, expected_owner):
    if len(raw) > MAX_PAGE:
        deny("PAGE_LIMIT")
    page = json.loads(raw)
    if not isinstance(page, dict) or page.get("schema") != PAGE_SCHEMA:
        deny("SCHEMA")
    if page.get("provider") != "github" or page.get("source_owner") != expected_owner:
        deny("PROVIDER_AUTHORITY")
    index = page.get("page_index")
    if type(index) is not int or index < 0:
        deny("PAGE_INDEX")
    incoming, outgoing = page.get("cursor_in"), page.get("cursor_out")
    if index == 0 and incoming is not None:
        deny("INITIAL_CURSOR")
    for token in (incoming, outgoing):
        if token is not None and (not isinstance(token, str) or
                                  not token.strip() or len(token) > 256):
            deny("CURSOR")
    rows = page.get("repositories")
    if not isinstance(rows, list) or not 1 <= len(rows) <= 100:
        deny("BOUNDED_PAGE")
    ids = set()
    names = set()
    normalized = []
    for row in rows:
        if not isinstance(row, dict):
            deny("REPO_SHAPE")
        ident = row.get("id")
        if type(ident) is int and ident > 0:
            ident = str(ident)
        if not isinstance(ident, str) or not re.fullmatch(r"[1-9][0-9]*", ident):
            deny("REPO_ID")
        name, branch = row.get("full_name"), row.get("default_branch")
        if (not isinstance(name, str) or not REPO.fullmatch(name) or
            name.split("/")[0].casefold() != expected_owner.casefold() or
            not isinstance(branch, str) or not branch or len(branch) > 200 or
            any(c.isspace() for c in branch)):
            deny("REPO_OWNER_BRANCH")
        if ident in ids or name.casefold() in names:
            deny("DUPLICATE_REPOSITORY")
        ids.add(ident)
        names.add(name.casefold())
        head, evidence = row.get("head_sha", TOKEN), row.get("head_source_url")
        if head is None or head == "":
            deny("UNTYPED_UNKNOWN_HEAD")
        if head == TOKEN:
            if evidence is not None:
                deny("UNVERIFIED_HEAD")
        elif not isinstance(head, str) or not SHA.fullmatch(head):
            deny("HEAD_SHA")
        else:
            expected_url = ("https://api.github.com/repos/" + name +
                            "/branches/" + quote(branch, safe=""))
            if evidence != expected_url:
                deny("BRANCH_SOURCE_REF")
        normalized.append({"provider_repo_id": ident, "repository": name,
                           "branch": branch, "head_sha": head,
                           "head_source_url": evidence if head != TOKEN else TOKEN})
    return {"page_index": index, "source_owner": expected_owner,
            "cursor_in": incoming, "cursor_out": outgoing,
            "page_sha256": digest(raw), "repo_refs": normalized}


def replay(raw):
    if not raw:
        return []
    if not raw.endswith(b"\n"):
        deny("TRUNCATED_LEDGER")
    receipts = []
    seen_ids, seen_names, seen_cursors, last_hash = set(), set(), set(), ZERO
    for line in raw.splitlines():
        if len(line) > MAX_PAGE:
            deny("RECEIPT_LIMIT")
        rec = json.loads(line)
        if not isinstance(rec, dict):
            deny("LEDGER_SHAPE")
        claimed = rec.get("receipt_sha256")
        body = {key: value for key, value in rec.items() if key != "receipt_sha256"}
        if (rec.get("schema") != RECEIPT_SCHEMA or
            rec.get("seq") != len(receipts) or
            rec.get("previous_receipt_sha256") != last_hash or
            not isinstance(claimed, str) or claimed != digest(canonical(body))):
            deny("LEDGER_CHAIN")
        if rec.get("claim_allowed") is not False:
            deny("CLAIM_PROMOTION")
        if rec.get("page_index") != len(receipts):
            deny("LEDGER_PAGE_SEQUENCE")
        if rec.get("page_index") == 0:
            if rec.get("cursor_in") is not None:
                deny("LEDGER_START")
        else:
            previous = receipts[-1]
            if (previous["cursor_out"] is None or
                rec.get("cursor_in") != previous["cursor_out"] or
                rec.get("source_owner") != previous["source_owner"]):
                deny("LEDGER_CURSOR")
        outgoing = rec.get("cursor_out")
        if outgoing is not None:
            if (not isinstance(outgoing, str) or not outgoing or
                outgoing == rec.get("cursor_in") or outgoing in seen_cursors):
                deny("LEDGER_CURSOR_CYCLE")
            seen_cursors.add(outgoing)
        refs = rec.get("repo_refs")
        if not isinstance(refs, list):
            deny("LEDGER_REFS")
        for row in refs:
            if not isinstance(row, dict):
                deny("LEDGER_REFS")
            ident, name = row.get("provider_repo_id"), row.get("repository")
            if not isinstance(name, str) or ident in seen_ids or name.casefold() in seen_names:
                deny("LEDGER_DUPLICATE")
            seen_ids.add(ident)
            seen_names.add(name.casefold())
        receipts.append(rec)
        last_hash = claimed
    return receipts


def process(source, destination, expected_owner):
    if not REPO.fullmatch(expected_owner + "/example"):
        deny("OWNER")
    if os.path.abspath(source) == os.path.abspath(destination):
        deny("SELF_LEDGER")
    flags = os.O_RDONLY | getattr(os, "O_NOFOLLOW", 0)
    sfd = os.open(source, flags)
    try:
        before = os.fstat(sfd)
        if not stat.S_ISREG(before.st_mode) or before.st_size > MAX_PAGE:
            deny("SOURCE_TYPE_OR_SIZE")
        raw = os.read(sfd, MAX_PAGE + 1)
        after = os.fstat(sfd)
        if before.st_size != after.st_size or before.st_mtime_ns != after.st_mtime_ns:
            deny("SOURCE_DRIFT")
    finally:
        os.close(sfd)
    candidate = page_from_bytes(raw, expected_owner)
    os.makedirs(os.path.dirname(os.path.abspath(destination)), exist_ok=True)
    fd = os.open(destination, os.O_RDWR | os.O_CREAT | getattr(os, "O_NOFOLLOW", 0), 0o600)
    try:
        fcntl.flock(fd, fcntl.LOCK_EX)
        size = os.fstat(fd).st_size
        if size > MAX_LEDGER:
            deny("LEDGER_LIMIT")
        prior = replay(os.read(fd, size))
        index = candidate["page_index"]
        if index < len(prior):
            if (index == len(prior) - 1 and
                prior[index]["page_sha256"] == candidate["page_sha256"]):
                return {"state": "ALREADY_COMMITTED", "page_count": len(prior),
                        "ledger_receipt_sha256": prior[-1]["receipt_sha256"],
                        "claim_allowed": False}
            deny("HISTORICAL_REPLAY_DRIFT")
        if index != len(prior):
            deny("CURSOR_GAP")
        if prior:
            if (prior[-1]["cursor_out"] is None or
                prior[-1]["cursor_out"] != candidate["cursor_in"] or
                prior[-1]["source_owner"] != expected_owner):
                deny("CURSOR_MISMATCH")
            used = {row["provider_repo_id"] for rec in prior for row in rec["repo_refs"]}
            names = {row["repository"].casefold() for rec in prior for row in rec["repo_refs"]}
            if any(row["provider_repo_id"] in used or
                   row["repository"].casefold() in names for row in candidate["repo_refs"]):
                deny("REPEATED_REPOSITORY")
        if (candidate["cursor_out"] is not None and
            (candidate["cursor_out"] == candidate["cursor_in"] or
             candidate["cursor_out"] in {r["cursor_out"] for r in prior})):
            deny("CURSOR_CYCLE")
        body = dict(candidate, schema=RECEIPT_SCHEMA, seq=index,
                    previous_receipt_sha256=prior[-1]["receipt_sha256"] if prior else ZERO,
                    claim_allowed=False, execution_state="NOT_RUN",
                    evidence_state="PROVIDER_INPUT_NOT_AUTHENTICATED")
        receipt_hash = digest(canonical(body))
        encoded = canonical(dict(body, receipt_sha256=receipt_hash)) + b"\n"
        if size + len(encoded) > MAX_LEDGER:
            deny("LEDGER_LIMIT")
        os.lseek(fd, 0, os.SEEK_END)
        if os.write(fd, encoded) != len(encoded):
            deny("SHORT_APPEND")
        os.fsync(fd)
        return {"state": "PAGE_CHECKPOINTED", "page_count": index + 1,
                "repos_in_page": len(candidate["repo_refs"]),
                "heads_observed": sum(x["head_sha"] != TOKEN for x in candidate["repo_refs"]),
                "heads_token_vazio": sum(x["head_sha"] == TOKEN for x in candidate["repo_refs"]),
                "next_cursor": candidate["cursor_out"] or TOKEN,
                "ledger_receipt_sha256": receipt_hash, "claim_allowed": False}
    finally:
        os.close(fd)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--page-json", required=True)
    parser.add_argument("--ledger", required=True)
    parser.add_argument("--expected-owner", required=True)
    args = parser.parse_args()
    try:
        result = process(args.page_json, args.ledger, args.expected_owner)
    except (ValueError, OSError, TypeError) as exc:
        print(json.dumps({"state": "ROUTE_STATE_BLOCKED", "reason": str(exc),
                          "claim_allowed": False}, sort_keys=True))
        return 2
    print(json.dumps(result, sort_keys=True))
    return 0


if __name__ == "__main__":
    sys.exit(main())
