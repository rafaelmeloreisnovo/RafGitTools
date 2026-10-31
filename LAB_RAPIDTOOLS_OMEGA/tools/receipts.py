#!/usr/bin/env python3
"""Local append-only custody. SHA256 linked records, NOT signed attestation."""
import hashlib
import json
import os
from pathlib import Path
import time


def canonical(obj):
    return json.dumps(obj, ensure_ascii=False, sort_keys=True,
                      separators=(",", ":")).encode("utf-8")


def verify(directory):
    folder = Path(directory)
    files = sorted(folder.glob("*.json")) if folder.exists() else []
    previous = "GENESIS"
    for file in files:
        record = json.loads(file.read_text(encoding="utf-8"))
        assert set(record) == {"id", "previous", "payload", "hash"}, "RECEIPT_FIELDS"
        assert file.stem == str(record["id"]), "RECEIPT_FILENAME"
        assert record["previous"] == previous, "CHAIN_BROKEN_LINK"
        digest = hashlib.sha256(canonical({
            "id": record["id"], "previous": previous, "payload": record["payload"]
        })).hexdigest()
        assert digest == record["hash"], "CHAIN_BROKEN_HASH"
        previous = digest
    return len(files), previous


def append(directory, payload):
    folder = Path(directory)
    folder.mkdir(parents=True, exist_ok=True)
    count, prev = verify(folder)
    last = max((int(p.stem) for p in folder.glob("*.json")), default=0)
    uid = max(last + 1, time.time_ns())
    record = {"id": uid, "previous": prev, "payload": payload}
    record["hash"] = hashlib.sha256(canonical(record)).hexdigest()
    dest = folder / (str(uid) + ".json")
    # O_EXCL prevents overwriting old receipts; limited to single local process.
    fd = os.open(dest, os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o600)
    try:
        with os.fdopen(fd, "w", encoding="utf-8") as handle:
            json.dump(record, handle, ensure_ascii=False, sort_keys=True)
            handle.write("\n")
            handle.flush()
            os.fsync(handle.fileno())
    except Exception:
        raise
    verify(folder)
    return dest


if __name__ == "__main__":
    import argparse
    p = argparse.ArgumentParser()
    p.add_argument("directory", type=Path)
    a = p.parse_args()
    n, tail = verify(a.directory)
    print("PASS_LOCAL_CHAIN", n, tail, "EXTERNAL_ANCHOR=TOKEN_VAZIO")
