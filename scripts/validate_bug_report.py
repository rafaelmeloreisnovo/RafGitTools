#!/usr/bin/env python3
from __future__ import annotations

import argparse
import pathlib
import re
import subprocess
import sys
from collections import defaultdict

FILE_REF_RE = re.compile(r"(?<![\w/.-])([A-Za-z0-9_./-]+\.(?:kt|kts|xml|md|gradle|json|java|groovy|properties))(?::\d+)?")


def rg_files(repo_root: pathlib.Path) -> list[str]:
    result = subprocess.run(
        ["rg", "--files"],
        cwd=repo_root,
        text=True,
        capture_output=True,
        check=True,
    )
    return [line.strip() for line in result.stdout.splitlines() if line.strip()]


def resolve_ref(ref: str, file_set: set[str], basename_index: dict[str, list[str]]) -> tuple[bool, str]:
    if ref in file_set:
        return True, ref

    matches = basename_index.get(pathlib.PurePosixPath(ref).name, [])
    if len(matches) == 1:
        return True, matches[0]

    if len(matches) > 1:
        return False, f"ambiguous basename ({len(matches)} matches)"

    return False, "missing from current snapshot"


def main() -> int:
    parser = argparse.ArgumentParser(description="Validate BUG_REPORT findings against current repository snapshot")
    parser.add_argument("--input", default="BUG_REPORT.md")
    parser.add_argument("--output", default="BUG_REPORT_VALIDATED.md")
    parser.add_argument("--threshold", type=float, default=0.05, help="Max tolerated mismatch ratio")
    args = parser.parse_args()

    repo_root = pathlib.Path.cwd()
    input_path = repo_root / args.input
    output_path = repo_root / args.output

    if not input_path.exists():
        print(f"Input report not found: {input_path}", file=sys.stderr)
        return 1

    tracked_files = rg_files(repo_root)
    tracked_set = set(tracked_files)
    basename_index: dict[str, list[str]] = defaultdict(list)
    for file_path in tracked_files:
        basename_index[pathlib.PurePosixPath(file_path).name].append(file_path)

    source_lines = input_path.read_text(encoding="utf-8").splitlines()

    findings: list[dict[str, str | int]] = []
    mismatches: list[dict[str, str | int]] = []

    for line_number, line in enumerate(source_lines, start=1):
        refs = sorted(set(FILE_REF_RE.findall(line)))
        if not refs:
            continue

        for ref in refs:
            ok, resolved = resolve_ref(ref, tracked_set, basename_index)
            if ok:
                findings.append(
                    {
                        "line": line_number,
                        "original": ref,
                        "resolved": resolved,
                        "context": line.strip(),
                    }
                )
            else:
                mismatches.append(
                    {
                        "line": line_number,
                        "original": ref,
                        "reason": resolved,
                        "status": "out-of-scope snapshot mismatch",
                    }
                )

    total_refs = len(findings) + len(mismatches)
    mismatch_ratio = (len(mismatches) / total_refs) if total_refs else 0.0
    status = "PASS" if mismatch_ratio <= args.threshold else "FAIL"

    lines: list[str] = []
    lines.append("# Bug Report — Validated Findings")
    lines.append("")
    lines.append(f"- Source report: `{args.input}`")
    lines.append("- Source file inventory command: `rg --files`")
    lines.append(f"- Total file references scanned: **{total_refs}**")
    lines.append(f"- Actionable (resolvable) references: **{len(findings)}**")
    lines.append(f"- Non-resolvable references: **{len(mismatches)}**")
    lines.append(f"- Mismatch ratio: **{mismatch_ratio:.2%}**")
    lines.append(f"- Threshold: **{args.threshold:.2%}**")
    lines.append(f"- Pre-check status: **{status}**")
    lines.append("")

    if findings:
        lines.append("## Actionable Findings (resolved against current tree)")
        lines.append("")
        lines.append("| Source Line | Original Reference | Resolved Path | Context |")
        lines.append("|---:|---|---|---|")
        for item in findings:
            context = str(item["context"]).replace("|", "\\|")
            lines.append(
                f"| {item['line']} | `{item['original']}` | `{item['resolved']}` | {context} |"
            )
        lines.append("")

    if mismatches:
        lines.append("## Out-of-Scope Snapshot Mismatches")
        lines.append("")
        lines.append("| Source Line | Original Reference | Status | Reason |")
        lines.append("|---:|---|---|---|")
        for item in mismatches:
            lines.append(
                f"| {item['line']} | `{item['original']}` | {item['status']} | {item['reason']} |"
            )
        lines.append("")

    output_path.write_text("\n".join(lines) + "\n", encoding="utf-8")

    if status == "FAIL":
        print(
            f"Audit pre-check failed: {len(mismatches)}/{total_refs} references are missing or ambiguous "
            f"({mismatch_ratio:.2%} > {args.threshold:.2%})."
        )
        return 2

    print(
        f"Audit pre-check passed: {len(findings)}/{total_refs} references resolved "
        f"({mismatch_ratio:.2%} mismatches)."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
