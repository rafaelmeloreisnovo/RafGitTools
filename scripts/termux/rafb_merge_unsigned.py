#!/usr/bin/env python3
"""Host-only guarded APK overlay. The freestanding C core does not import Python.

Preserves the original ApkC unsigned APK and emits a NEW, unsigned dual-ABI APK.
This does not certify Android installability, signature, direct load, page alignment
or NativeActivity invocation of librafb.so.
"""
import hashlib
import os
import pathlib
import struct
import sys
import tempfile
import zipfile

MAX_TOTAL = 32 * 1024 * 1024
NAMES = ("lib/armeabi-v7a/librafb.so", "lib/arm64-v8a/librafb.so")
BOOT = ("lib/armeabi-v7a/librafgittools_hermetic.so", "lib/arm64-v8a/librafgittools_hermetic.so")


def sha256(path):
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for block in iter(lambda: f.read(1024 * 1024), b""):
            h.update(block)
    return h.hexdigest()


def valid_elf(data, elf_class, machine):
    return (len(data) >= (52 if elf_class == 1 else 64)
            and data[:4] == b"\x7fELF" and data[4] == elf_class
            and data[5:7] == b"\x01\x01"
            and data[7] in (0, 3)
            and struct.unpack_from("<H", data, 16)[0] == 3
            and struct.unpack_from("<H", data, 18)[0] == machine
            and struct.unpack_from("<I", data, 20)[0] == 1
            and struct.unpack_from("<H", data, 40 if elf_class == 1 else 52)[0]
            == (52 if elf_class == 1 else 64))


def run(base, arm32, arm64, dest):
    base, arm32, arm64, dest = map(pathlib.Path, (base, arm32, arm64, dest))
    if dest.exists() or dest.resolve() in (base.resolve(), arm32.resolve(), arm64.resolve()):
        raise ValueError("destination exists or overlaps source; refuse overwrite")
    if not base.name.endswith("-unsigned.apk"):
        raise ValueError("input must be explicitly unsigned ApkC output")
    payloads = (arm32.read_bytes(), arm64.read_bytes())
    if not valid_elf(payloads[0], 1, 40) or not valid_elf(payloads[1], 2, 183):
        raise ValueError("RAFB ELF machine/class/type mismatch")
    with zipfile.ZipFile(base, "r") as src:
        infos = src.infolist()
        paths = [i.filename for i in infos]
        if len(paths) != len(set(paths)):
            raise ValueError("duplicate source ZIP entries")
        if any(p.startswith("/") or ".." in p.split("/") or "\\" in p or not p for p in paths):
            raise ValueError("unsafe source path")
        if any(p.upper().startswith("META-INF/") and
               p.upper().endswith((".RSA", ".DSA", ".EC", ".SF")) for p in paths):
            raise ValueError("signed input, cannot modify signature")
        if any(p in paths for p in NAMES):
            raise ValueError("would replace existing RAFCODE native lib")
        if any(p not in paths for p in BOOT + ("AndroidManifest.xml",)):
            raise ValueError("required native bootstrap/manifest entry missing")
        if sum(i.file_size for i in infos) > MAX_TOTAL or any(i.file_size > MAX_TOTAL for i in infos):
            raise ValueError("source exceeds bounded bootstrap budget")
        for path, cls, machine in ((BOOT[0], 1, 40), (BOOT[1], 2, 183)):
            if not valid_elf(src.read(path), cls, machine):
                raise ValueError("bootstrap library ABI mismatch: " + path)
        dest.parent.mkdir(parents=True, exist_ok=True)
        fd, tmp = tempfile.mkstemp(prefix=".rafb-stage-", dir=dest.parent)
        os.close(fd)
        try:
            with zipfile.ZipFile(tmp, "w") as dst:
                dst.comment = src.comment
                for info in infos:
                    dst.writestr(info, src.read(info), compress_type=info.compress_type)
                for path, payload in zip(NAMES, payloads):
                    dst.writestr(path, payload, compress_type=zipfile.ZIP_STORED)
            with zipfile.ZipFile(tmp) as verify:
                if verify.testzip() is not None:
                    raise ValueError("failed CRC after ZIP stage")
                if any(verify.read(name) != blob for name, blob in zip(NAMES, payloads)):
                    raise ValueError("byte-level lib readback failed")
            # Atomic and no-overwrite on same filesystem. Do not mutate original APK.
            os.link(tmp, dest)
        finally:
            os.unlink(tmp)
    receipt = (f"schema=raf.rafb.apk-overlay.v1\n"
               f"state=UNSIGNED_STRUCTURAL_ONLY\n"
               f"source_sha256={sha256(base)}\n"
               f"arm32_sha256={hashlib.sha256(payloads[0]).hexdigest()}\n"
               f"arm64_sha256={hashlib.sha256(payloads[1]).hexdigest()}\n"
               f"output_sha256={sha256(dest)}\n"
               "apk_signature=TOKEN_VAZIO_UNSIGNED\n"
               "android_install=TOKEN_VAZIO\n"
               "jni=NOT_LINKED_BY_RAFB\n"
               "nativeactivity_rafb_invocation=TOKEN_VAZIO\n"
               "source_provenance=CALLER_MUST_PIN_PRODUCER\n"
               "claim_allowed=false\n")
    try:
        # Exclusive append-free creation. Never overwrite an existing receipt.
        with open(str(dest) + ".receipt.env", "x", encoding="utf-8") as f:
            f.write(receipt)
            f.flush()
            os.fsync(f.fileno())
    except Exception:
        dest.unlink()  # Only our newly-created artifact; original base remains.
        raise
    print("OVERLAY_STRUCTURAL=PASS; SIGNED=NO; APK_RUNTIME=TOKEN_VAZIO")
    print("APK_SHA256=" + sha256(dest))


if __name__ == "__main__":
    if len(sys.argv) != 5:
        sys.exit("usage: rafb_merge_unsigned.py input-unsigned.apk arm32.so arm64.so output-unsigned.apk")
    try:
        run(*sys.argv[1:])
    except (ValueError, OSError, zipfile.BadZipFile, EOFError) as exc:
        sys.exit("BLOCKED: " + str(exc))
