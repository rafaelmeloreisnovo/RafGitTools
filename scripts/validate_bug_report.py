#!/usr/bin/env python3
import argparse
import re
import subprocess
from pathlib import Path

FINDING_ID_RE = re.compile(r"^[A-Z]\d+$")


def run_rg_files(repo_root: Path) -> set[str]:
    result = subprocess.run(
        ["rg", "--files"],
        cwd=repo_root,
        check=True,
        capture_output=True,
        text=True,
    )
    return {line.strip() for line in result.stdout.splitlines() if line.strip()}


def parse_table_row(line: str) -> list[str] | None:
    stripped = line.strip()
    if not (stripped.startswith("|") and stripped.endswith("|")):
        return None
    return [part.strip() for part in stripped.strip("|").split("|")]


def resolve_path(raw_path: str, repo_files: set[str]) -> tuple[str | None, str | None]:
    cleaned = raw_path.strip().strip("`")
    cleaned = cleaned.split()[0]
    if ":" in cleaned:
        cleaned = cleaned.split(":", 1)[0]

    if not cleaned:
        return None, "empty-path"

    if "/" in cleaned:
        if cleaned in repo_files:
            return cleaned, None
        return None, "missing"

    basename_matches = [path for path in repo_files if Path(path).name == cleaned]
    if len(basename_matches) == 1:
        return basename_matches[0], None
    if len(basename_matches) > 1:
        return None, "ambiguous"
    return None, "missing"


def main() -> int:
    parser = argparse.ArgumentParser(description="Validate BUG_REPORT findings against current tree")
    parser.add_argument("--input", default="BUG_REPORT.md")
    parser.add_argument("--output", default="BUG_REPORT_VALIDATED.md")
    parser.add_argument("--threshold", type=float, default=0.05)
    args = parser.parse_args()

    repo_root = Path.cwd()
    input_path = repo_root / args.input
    output_path = repo_root / args.output

    if not input_path.exists():
        raise SystemExit(f"Input report not found: {input_path}")

    repo_files = run_rg_files(repo_root)

    total = 0
    actionable: list[dict] = []
    mismatches: list[dict] = []

    for line in input_path.read_text(encoding="utf-8").splitlines():
        cols = parse_table_row(line)
        if not cols or len(cols) < 4:
            continue

        finding_id = cols[0]
        if not FINDING_ID_RE.match(finding_id):
            continue

        raw_path = cols[1]
        bug = cols[2]
        fix = cols[3]

        total += 1
        resolved, reason = resolve_path(raw_path, repo_files)
        if resolved:
            actionable.append(
                {
                    "id": finding_id,
                    "input_path": raw_path,
                    "resolved_path": resolved,
                    "bug": bug,
                    "fix": fix,
                }
            )
        else:
            mismatches.append(
                {
                    "id": finding_id,
                    "input_path": raw_path,
                    "reason": reason or "missing",
                    "bug": bug,
                    "fix": fix,
                    "status": "out-of-scope snapshot mismatch",
                }
            )

    missing = len(mismatches)
    ratio = (missing / total) if total else 0.0
    failed = ratio > args.threshold

    lines: list[str] = []
    lines.append("# BUG Report (Validated Against Current Snapshot)")
    lines.append("")
    lines.append(f"- Source report: `{args.input}`")
    lines.append("- Source file list: `rg --files` from current repository root")
    lines.append(f"- Total findings parsed: **{total}**")
    lines.append(f"- Actionable findings: **{len(actionable)}**")
    lines.append(f"- Non-resolvable findings: **{missing}**")
    lines.append(f"- Missing ratio: **{ratio:.2%}** (threshold: {args.threshold:.2%})")
    lines.append(f"- Audit status: **{'FAIL' if failed else 'PASS'}**")
    lines.append("")

    lines.append("## Actionable findings (resolved to current tree)")
    lines.append("")
    lines.append("| # | Report Path | Resolved Path | Bug | Fix |")
    lines.append("|---|---|---|---|---|")
    for item in actionable:
        lines.append(
            f"| {item['id']} | {item['input_path']} | `{item['resolved_path']}` | {item['bug']} | {item['fix']} |"
        )
    if not actionable:
        lines.append("| - | - | - | - | - |")

    lines.append("")
    lines.append("## Out-of-scope snapshot mismatch")
    lines.append("")
    lines.append("| # | Report Path | Status | Reason |")
    lines.append("|---|---|---|---|")
    for item in mismatches:
        lines.append(
            f"| {item['id']} | {item['input_path']} | {item['status']} | {item['reason']} |"
        )
    if not mismatches:
        lines.append("| - | - | - | - |")

    output_path.write_text("\n".join(lines) + "\n", encoding="utf-8")

    if failed:
        return 2
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
