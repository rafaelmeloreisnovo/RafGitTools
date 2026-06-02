#!/usr/bin/env python3
import json
import os
import re
import sys

DOC = os.path.join("docs", "ASSEMBLER_ORQUESTRACAO_HARDWARE_SOFTWARE.md")
OUT_DIR = os.path.join("build", "ci-validation")


def ensure_out_dir(path):
    if not os.path.isdir(path):
        os.makedirs(path, exist_ok=True)


def read_text(path):
    with open(path, "r", encoding="utf-8") as f:
        return f.read()


def write_text(path, data):
    with open(path, "w", encoding="utf-8") as f:
        f.write(data)


def main():
    if not os.path.exists(DOC):
        print(f"Missing document: {DOC}", file=sys.stderr)
        return 2

    ensure_out_dir(OUT_DIR)
    text = read_text(DOC)

    required = ["F1", "F2", "F3", "F4", "flowchart TD", "arm64-v8a", "armeabi-v7a"]
    missing = []
    for token in required:
        if token not in text:
            missing.append(token)

    formulas = re.findall(r"\*\*F(\d)\*\*", text)
    unique_formulas = sorted(set(formulas))
    graph_nodes = re.findall(r"\b([A-H])\[", text)
    edges = re.findall(r"([A-H]) --> ([A-H])", text)

    status = "PASS" if (not missing and len(unique_formulas) >= 4 and len(edges) >= 6) else "FAIL"
    result = {
        "document": DOC,
        "missing_tokens": missing,
        "formula_count": len(unique_formulas),
        "formulas": ["F" + x for x in unique_formulas],
        "node_count": len(set(graph_nodes)),
        "edge_count": len(edges),
        "status": status,
    }

    write_text(os.path.join(OUT_DIR, "formula_graph_validation.json"), json.dumps(result, indent=2) + "\n")
    write_text(os.path.join(OUT_DIR, "formula_graph_edges.txt"), "\n".join([f"{a}->{b}" for a, b in edges]) + "\n")

    if status != "PASS":
        print("Validation failed", file=sys.stderr)
        return 1

    print("Validation PASS")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
