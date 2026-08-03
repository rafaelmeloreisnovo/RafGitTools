#!/usr/bin/env python3
"""RAFYML-FREESTANDING-V1: deterministic restricted YAML to static C compiler.

The compiler runs on the host with Python's standard library. Generated C and
its runtime validator are freestanding: no YAML parser, heap, libc, JNI or I/O.
"""
from __future__ import annotations

import argparse
import dataclasses
import hashlib
import json
import pathlib
import re
import sys
from typing import Any, Iterable

VERSION = "1.0.0"
NONE = 0xFFFFFFFF
MAX_DEPTH = 32
MAX_NODES = 4096
MAX_STRING_BYTES = 1 << 20
_KEY_RE = re.compile(r"^[A-Za-z_][A-Za-z0-9_.-]*$")
_INT_RE = re.compile(r"^[+-]?(?:0|[1-9][0-9_]*|0[xX][0-9A-Fa-f_]+)$")
_PLAIN_RE = re.compile(r"^[A-Za-z0-9_./@+%-]+(?: [A-Za-z0-9_./@+%-]+)*$")
_FORBIDDEN_RE = re.compile(r"(^|\s)(?:&|\*|!|<<:|---|\.\.\.)(?=\s|$)")


class RafYmlError(ValueError):
    pass


@dataclasses.dataclass(frozen=True)
class Line:
    number: int
    indent: int
    text: str


def _strip_comment(raw: str) -> str:
    quote: str | None = None
    escaped = False
    out: list[str] = []
    for ch in raw:
        if escaped:
            out.append(ch)
            escaped = False
            continue
        if quote == '"' and ch == "\\":
            out.append(ch)
            escaped = True
            continue
        if quote:
            out.append(ch)
            if ch == quote:
                quote = None
            continue
        if ch in ('"', "'"):
            quote = ch
            out.append(ch)
            continue
        if ch == "#":
            break
        out.append(ch)
    if quote:
        raise RafYmlError("unterminated quoted scalar")
    return "".join(out).rstrip()


def _tokenize(text: str) -> list[Line]:
    lines: list[Line] = []
    for number, raw in enumerate(text.splitlines(), 1):
        if "\t" in raw:
            raise RafYmlError(f"line {number}: tabs are forbidden")
        stripped = _strip_comment(raw)
        if not stripped.strip():
            continue
        indent = len(stripped) - len(stripped.lstrip(" "))
        if indent % 2:
            raise RafYmlError(f"line {number}: indentation must use multiples of two spaces")
        body = stripped[indent:]
        if _FORBIDDEN_RE.search(body):
            raise RafYmlError(f"line {number}: unsupported YAML feature")
        if body.startswith(("|", ">", "? ")):
            raise RafYmlError(f"line {number}: block scalars and complex keys are unsupported")
        lines.append(Line(number, indent, body))
    if not lines:
        raise RafYmlError("document is empty")
    if lines[0].indent != 0:
        raise RafYmlError(f"line {lines[0].number}: root must start at indentation zero")
    return lines


def _split_key_value(line: Line, text: str | None = None) -> tuple[str, str]:
    source = line.text if text is None else text
    quote: str | None = None
    escaped = False
    for index, ch in enumerate(source):
        if escaped:
            escaped = False
            continue
        if quote == '"' and ch == "\\":
            escaped = True
            continue
        if quote:
            if ch == quote:
                quote = None
            continue
        if ch in ('"', "'"):
            quote = ch
            continue
        if ch == ":":
            raw_key = source[:index].strip()
            raw_value = source[index + 1 :].strip()
            if not raw_key:
                raise RafYmlError(f"line {line.number}: empty key")
            return _parse_key(raw_key, line.number), raw_value
    raise RafYmlError(f"line {line.number}: expected 'key: value'")


def _parse_key(raw: str, number: int) -> str:
    if raw.startswith('"'):
        try:
            value = json.loads(raw)
        except json.JSONDecodeError as exc:
            raise RafYmlError(f"line {number}: invalid quoted key") from exc
        if not isinstance(value, str) or not value:
            raise RafYmlError(f"line {number}: key must be a non-empty string")
        return value
    if raw.startswith("'") and raw.endswith("'") and len(raw) >= 2:
        value = raw[1:-1].replace("''", "'")
        if not value:
            raise RafYmlError(f"line {number}: key must be non-empty")
        return value
    if not _KEY_RE.fullmatch(raw):
        raise RafYmlError(f"line {number}: unsupported key syntax: {raw!r}")
    return raw


def _parse_scalar(raw: str, number: int) -> Any:
    if not raw:
        raise RafYmlError(f"line {number}: missing scalar")
    if raw.startswith('"'):
        try:
            value = json.loads(raw)
        except json.JSONDecodeError as exc:
            raise RafYmlError(f"line {number}: invalid JSON-style string") from exc
        if not isinstance(value, str):
            raise RafYmlError(f"line {number}: quoted scalar must be a string")
        return value
    if raw.startswith("'"):
        if not raw.endswith("'") or len(raw) < 2:
            raise RafYmlError(f"line {number}: invalid single-quoted string")
        return raw[1:-1].replace("''", "'")
    lowered = raw.lower()
    if lowered == "true":
        return True
    if lowered == "false":
        return False
    if lowered in ("null", "~"):
        return None
    if _INT_RE.fullmatch(raw):
        compact = raw.replace("_", "")
        sign = -1 if compact.startswith("-") else 1
        unsigned = compact[1:] if compact[:1] in "+-" else compact
        base = 16 if unsigned.lower().startswith("0x") else 10
        value = sign * int(unsigned, base)
        if not -(1 << 63) <= value < (1 << 63):
            raise RafYmlError(f"line {number}: integer outside signed 64-bit range")
        return value
    if any(mark in raw for mark in ("[", "]", "{", "}", "&", "*", "!", "`")):
        raise RafYmlError(f"line {number}: unsupported scalar syntax")
    if not _PLAIN_RE.fullmatch(raw):
        raise RafYmlError(f"line {number}: ambiguous plain scalar; quote it explicitly")
    return raw


class Parser:
    def __init__(self, lines: list[Line]) -> None:
        self.lines = lines

    def parse(self) -> Any:
        value, index = self._block(0, 0, 0)
        if index != len(self.lines):
            line = self.lines[index]
            raise RafYmlError(f"line {line.number}: unexpected indentation")
        return value

    def _block(self, index: int, indent: int, depth: int) -> tuple[Any, int]:
        if depth > MAX_DEPTH:
            raise RafYmlError(f"maximum nesting depth {MAX_DEPTH} exceeded")
        if index >= len(self.lines) or self.lines[index].indent != indent:
            raise RafYmlError("internal parser indentation mismatch")
        if self.lines[index].text == "-" or self.lines[index].text.startswith("- "):
            return self._list(index, indent, depth)
        return self._map(index, indent, depth)

    def _map(self, index: int, indent: int, depth: int, seed: dict[str, Any] | None = None) -> tuple[dict[str, Any], int]:
        out: dict[str, Any] = {} if seed is None else seed
        while index < len(self.lines):
            line = self.lines[index]
            if line.indent < indent:
                break
            if line.indent > indent:
                raise RafYmlError(f"line {line.number}: unexpected indentation")
            if line.text == "-" or line.text.startswith("- "):
                break
            key, raw = _split_key_value(line)
            if key in out:
                raise RafYmlError(f"line {line.number}: duplicate key {key!r}")
            if raw:
                out[key] = _parse_scalar(raw, line.number)
                index += 1
            else:
                index += 1
                if index >= len(self.lines) or self.lines[index].indent != indent + 2:
                    raise RafYmlError(f"line {line.number}: nested value must be indented exactly two spaces")
                child, index = self._block(index, indent + 2, depth + 1)
                out[key] = child
        return out, index

    def _list(self, index: int, indent: int, depth: int) -> tuple[list[Any], int]:
        out: list[Any] = []
        while index < len(self.lines):
            line = self.lines[index]
            if line.indent < indent:
                break
            if line.indent > indent:
                raise RafYmlError(f"line {line.number}: unexpected indentation")
            if not (line.text == "-" or line.text.startswith("- ")):
                break
            body = line.text[1:].strip()
            if not body:
                index += 1
                if index >= len(self.lines) or self.lines[index].indent != indent + 2:
                    raise RafYmlError(f"line {line.number}: list item must be indented exactly two spaces")
                child, index = self._block(index, indent + 2, depth + 1)
                out.append(child)
                continue
            try:
                key, raw = _split_key_value(line, body)
            except RafYmlError:
                out.append(_parse_scalar(body, line.number))
                index += 1
                continue
            item: dict[str, Any] = {}
            if raw:
                item[key] = _parse_scalar(raw, line.number)
                index += 1
            else:
                index += 1
                if index >= len(self.lines) or self.lines[index].indent != indent + 4:
                    raise RafYmlError(f"line {line.number}: nested map value requires four-space continuation")
                child, index = self._block(index, indent + 4, depth + 2)
                item[key] = child
            if index < len(self.lines) and self.lines[index].indent == indent + 2 and not self.lines[index].text.startswith("-"):
                item, index = self._map(index, indent + 2, depth + 1, item)
            out.append(item)
        return out, index


def parse_document(text: str) -> Any:
    return Parser(_tokenize(text)).parse()


def canonicalize(value: Any) -> Any:
    if isinstance(value, dict):
        return {key: canonicalize(value[key]) for key in sorted(value)}
    if isinstance(value, list):
        return [canonicalize(item) for item in value]
    return value


@dataclasses.dataclass
class Node:
    key_offset: int = 0
    value_offset: int = 0
    first_child: int = NONE
    next_sibling: int = NONE
    child_count: int = 0
    int_value: int = 0
    type_id: int = 0
    bool_value: int = 0


class Emitter:
    def __init__(self) -> None:
        self.pool = bytearray(b"\0")
        self.offsets: dict[str, int] = {}
        self.nodes: list[Node] = []

    def intern(self, value: str) -> int:
        found = self.offsets.get(value)
        if found is not None:
            return found
        encoded = value.encode("utf-8")
        if b"\0" in encoded:
            raise RafYmlError("NUL bytes are forbidden in strings")
        if len(self.pool) + len(encoded) + 1 > MAX_STRING_BYTES:
            raise RafYmlError("string pool limit exceeded")
        offset = len(self.pool)
        self.pool.extend(encoded)
        self.pool.append(0)
        self.offsets[value] = offset
        return offset

    def add(self, value: Any, key: str | None = None) -> int:
        if len(self.nodes) >= MAX_NODES:
            raise RafYmlError(f"node limit {MAX_NODES} exceeded")
        index = len(self.nodes)
        node = Node(key_offset=self.intern(key) if key is not None else 0)
        self.nodes.append(node)
        if value is None:
            node.type_id = 0
        elif isinstance(value, bool):
            node.type_id = 1
            node.bool_value = int(value)
        elif isinstance(value, int):
            node.type_id = 2
            node.int_value = value
        elif isinstance(value, str):
            node.type_id = 3
            node.value_offset = self.intern(value)
        elif isinstance(value, dict):
            node.type_id = 4
            children = [self.add(value[name], name) for name in sorted(value)]
            self._link(node, children)
        elif isinstance(value, list):
            node.type_id = 5
            children = [self.add(item) for item in value]
            self._link(node, children)
        else:
            raise RafYmlError(f"unsupported internal value type: {type(value).__name__}")
        return index

    def _link(self, node: Node, children: list[int]) -> None:
        node.child_count = len(children)
        if children:
            node.first_child = children[0]
            for left, right in zip(children, children[1:]):
                self.nodes[left].next_sibling = right


def _identifier(raw: str) -> str:
    value = re.sub(r"[^A-Za-z0-9_]", "_", raw)
    if not value or value[0].isdigit():
        value = "raf_" + value
    return value.lower()


def _guard(prefix: str) -> str:
    return re.sub(r"[^A-Za-z0-9]", "_", prefix).upper() + "_GENERATED_H"


def _bytes_literal(data: bytes) -> str:
    rows = []
    for start in range(0, len(data), 12):
        rows.append("    " + ", ".join(f"0x{byte:02x}u" for byte in data[start:start + 12]))
    return ",\n".join(rows)


def emit_c(value: Any, prefix: str) -> tuple[str, str, dict[str, Any]]:
    canonical = canonicalize(value)
    emitter = Emitter()
    root = emitter.add(canonical)
    ident = _identifier(prefix)
    guard = _guard(prefix)
    header = f"""/* Generated by rafymlc {VERSION}; deterministic, do not edit. */
#ifndef {guard}
#define {guard}

#include \"rafyml_runtime.h\"

extern const rafyml_node {ident}_nodes[{len(emitter.nodes)}u];
extern const raf_u8 {ident}_strings[{len(emitter.pool)}u];
extern const rafyml_document {ident}_document;

#endif
"""
    node_rows = []
    for node in emitter.nodes:
        node_rows.append(
            "    { %uu, %uu, %uu, %uu, %uu, %dLL, %uu, %uu, 0u }" % (
                node.key_offset,
                node.value_offset,
                node.first_child,
                node.next_sibling,
                node.child_count,
                node.int_value,
                node.type_id,
                node.bool_value,
            )
        )
    source = f"""/* Generated by rafymlc {VERSION}; deterministic, do not edit. */
#include \"{prefix}.generated.h\"

const rafyml_node {ident}_nodes[{len(emitter.nodes)}u] = {{
{',\n'.join(node_rows)}
}};

const raf_u8 {ident}_strings[{len(emitter.pool)}u] = {{
{_bytes_literal(bytes(emitter.pool))}
}};

const rafyml_document {ident}_document = {{
    {ident}_nodes,
    {ident}_strings,
    {len(emitter.nodes)}u,
    {len(emitter.pool)}u,
    {root}u,
    RAFYML_FORMAT_VERSION
}};
"""
    canonical_bytes = json.dumps(canonical, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode("utf-8")
    metadata = {
        "compiler": f"rafymlc/{VERSION}",
        "profile": "RAFYML-FREESTANDING-V1",
        "canonical_sha256": hashlib.sha256(canonical_bytes).hexdigest(),
        "node_count": len(emitter.nodes),
        "string_bytes": len(emitter.pool),
        "root_index": root,
    }
    return header, source, metadata


def _write_if_changed(path: pathlib.Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    data = content.encode("utf-8")
    if path.exists() and path.read_bytes() == data:
        return
    path.write_bytes(data)


def compile_file(input_path: pathlib.Path, output_dir: pathlib.Path, prefix: str) -> dict[str, Any]:
    raw = input_path.read_bytes()
    try:
        text = raw.decode("utf-8")
    except UnicodeDecodeError as exc:
        raise RafYmlError("input must be valid UTF-8") from exc
    value = parse_document(text)
    header, source, metadata = emit_c(value, prefix)
    header_path = output_dir / f"{prefix}.generated.h"
    source_path = output_dir / f"{prefix}.generated.c"
    _write_if_changed(header_path, header)
    _write_if_changed(source_path, source)
    metadata.update(
        {
            "input": str(input_path),
            "input_sha256": hashlib.sha256(raw).hexdigest(),
            "header_sha256": hashlib.sha256(header.encode()).hexdigest(),
            "source_sha256": hashlib.sha256(source.encode()).hexdigest(),
            "claim_allowed": False,
            "state": "GENERATED_REQUIRES_COMPILE_GATE",
        }
    )
    receipt = output_dir / f"{prefix}.receipt.json"
    _write_if_changed(receipt, json.dumps(metadata, indent=2, sort_keys=True) + "\n")
    return metadata


def main(argv: Iterable[str] | None = None) -> int:
    parser = argparse.ArgumentParser(prog="rafymlc")
    sub = parser.add_subparsers(dest="command", required=True)
    validate = sub.add_parser("validate", help="validate restricted YAML")
    validate.add_argument("input", type=pathlib.Path)
    emit = sub.add_parser("emit-c", aliases=["compile"], help="emit deterministic freestanding C")
    emit.add_argument("input", type=pathlib.Path)
    emit.add_argument("--out", type=pathlib.Path, required=True)
    emit.add_argument("--prefix", default=None)
    args = parser.parse_args(list(argv) if argv is not None else None)
    try:
        if args.command == "validate":
            parse_document(args.input.read_text(encoding="utf-8"))
            print(json.dumps({"profile": "RAFYML-FREESTANDING-V1", "valid": True}, sort_keys=True))
        else:
            prefix = args.prefix or args.input.stem
            metadata = compile_file(args.input, args.out, prefix)
            print(json.dumps(metadata, sort_keys=True))
    except (OSError, RafYmlError) as exc:
        print(f"rafymlc: {exc}", file=sys.stderr)
        return 2
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
