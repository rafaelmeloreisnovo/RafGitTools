#!/usr/bin/env python3
import argparse
import os
import re
import subprocess
import sys

FILE_REF_RE = re.compile(r"(?<![\w/.-])([A-Za-z0-9_./-]+\.(?:kt|kts|xml|md|gradle|json|java|groovy|properties))(?::\d+)?")


def run_rg_files(repo_root):
    proc = subprocess.Popen(
        ["rg", "--files"],
        cwd=repo_root,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True,
    )
    out, err = proc.communicate()
    if proc.returncode != 0:
        raise RuntimeError(f"rg --files failed ({proc.returncode}): {err.strip()}")
    files = []
    for line in out.splitlines():
        line = line.strip()
        if line:
            files.append(line)
    return files


def build_basename_index(paths):
    idx = {}
    for p in paths:
        base = os.path.basename(p)
        if base not in idx:
            idx[base] = []
        idx[base].append(p)
    return idx


def resolve_ref(ref, tracked_set, basename_index):
    if ref in tracked_set:
        return True, ref
    base = os.path.basename(ref)
    matches = basename_index.get(base, [])
    if len(matches) == 1:
        return True, matches[0]
    if len(matches) > 1:
        return False, f"ambiguous basename ({len(matches)} matches)"
    return False, "missing from current snapshot"


def main():
    parser = argparse.ArgumentParser(description="Validate BUG_REPORT findings against current repository snapshot")
    parser.add_argument("--input", default="BUG_REPORT.md")
    parser.add_argument("--output", default="BUG_REPORT_VALIDATED.md")
    parser.add_argument("--threshold", type=float, default=0.05)
    args = parser.parse_args()

    repo_root = os.getcwd()
    input_path = os.path.join(repo_root, args.input)
    output_path = os.path.join(repo_root, args.output)

    if not os.path.exists(input_path):
        print(f"Input report not found: {input_path}", file=sys.stderr)
        return 1

    tracked_files = run_rg_files(repo_root)
    tracked_set = set(tracked_files)
    basename_index = build_basename_index(tracked_files)

    with open(input_path, "r", encoding="utf-8") as f:
        source_lines = f.read().splitlines()

    findings = []
    mismatches = []

    ln = 0
    for line in source_lines:
        ln += 1
        refs = sorted(set(FILE_REF_RE.findall(line)))
        if not refs:
            continue
        for ref in refs:
            ok, resolved = resolve_ref(ref, tracked_set, basename_index)
            if ok:
                findings.append((ln, ref, resolved, line.strip()))
            else:
                mismatches.append((ln, ref, "out-of-scope snapshot mismatch", resolved))

    total_refs = len(findings) + len(mismatches)
    mismatch_ratio = (float(len(mismatches)) / float(total_refs)) if total_refs else 0.0
    status = "PASS" if mismatch_ratio <= args.threshold else "FAIL"

    out = []
    out.append("# Bug Report — Validated Findings")
    out.append("")
    out.append(f"- Source report: `{args.input}`")
    out.append("- Source file inventory command: `rg --files`")
    out.append(f"- Total file references scanned: **{total_refs}**")
    out.append(f"- Actionable (resolvable) references: **{len(findings)}**")
    out.append(f"- Non-resolvable references: **{len(mismatches)}**")
    out.append(f"- Mismatch ratio: **{mismatch_ratio:.2%}**")
    out.append(f"- Threshold: **{args.threshold:.2%}**")
    out.append(f"- Pre-check status: **{status}**")
    out.append("")

    if findings:
        out.append("## Actionable Findings (resolved against current tree)")
        out.append("")
        out.append("| Source Line | Original Reference | Resolved Path | Context |")
        out.append("|---:|---|---|---|")
        for ln, original, resolved, context in findings:
            safe_context = context.replace('|', '\\|')
            out.append(f"| {ln} | `{original}` | `{resolved}` | {safe_context} |")
        out.append("")

    if mismatches:
        out.append("## Out-of-Scope Snapshot Mismatches")
        out.append("")
        out.append("| Source Line | Original Reference | Status | Reason |")
        out.append("|---:|---|---|---|")
        for ln, original, st, reason in mismatches:
            out.append(f"| {ln} | `{original}` | {st} | {reason} |")
        out.append("")

    with open(output_path, "w", encoding="utf-8") as f:
        f.write("\n".join(out) + "\n")

    if status == "FAIL":
        print(
            f"Audit pre-check failed: {len(mismatches)}/{total_refs} references are missing or ambiguous "
            f"({mismatch_ratio:.2%} > {args.threshold:.2%})."
        )
        return 2

    print(f"Audit pre-check passed: {len(findings)}/{total_refs} references resolved ({mismatch_ratio:.2%} mismatches).")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
