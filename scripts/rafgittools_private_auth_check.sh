#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

OWNER="${RAFGITTOOLS_GITHUB_OWNER:-rafaelmeloreisnovo}"
REPO="${RAFGITTOOLS_GITHUB_REPO:-RafGitTools}"
STAMP="$(date -u '+%Y%m%dT%H%M%SZ')"
OUT_DIR="${RAFGITTOOLS_RECEIPTS_DIR:-$ROOT/.rafgittools/receipts}"
OUT="$OUT_DIR/private-auth-$STAMP.tsv"
mkdir -p "$OUT_DIR"

printf 'gate\tstate\tdetail\n' > "$OUT"
record() {
  local gate="$1" state="$2" detail="$3"
  detail="${detail//$'\t'/ }"
  detail="${detail//$'\n'/ }"
  printf '%s\t%s\t%s\n' "$gate" "$state" "$detail" | tee -a "$OUT"
}

record META PASS "schema=RAFGITTOOLS_PRIVATE_AUTH_RECEIPT_V1 observed_at=$STAMP repository=$OWNER/$REPO"

if ! command -v gh >/dev/null 2>&1; then
  record AUTH TOKEN_VAZIO "gh CLI unavailable; install/authenticate it or use the app PAT/OAuth path"
  exit_code=2
elif ! gh auth status -h github.com >/dev/null 2>&1; then
  record AUTH TOKEN_VAZIO "gh CLI is present but no valid github.com authentication was confirmed"
  exit_code=2
else
  login="$(gh api user --jq '.login' 2>/dev/null || true)"
  if [[ -z "$login" ]]; then
    record AUTH FAIL "GitHub authenticated-user API could not be read"
    exit_code=1
  else
    record AUTH PASS "authenticated_login=$login token_not_printed=true"

    repo_json="$(gh api "repos/$OWNER/$REPO" --jq '[.private, (.permissions.pull // false), (.permissions.push // false), (.permissions.admin // false)] | @tsv' 2>/dev/null || true)"
    if [[ -z "$repo_json" ]]; then
      record PRIVATE_REPO_ACCESS FAIL "authenticated account cannot read repository metadata for $OWNER/$REPO"
      exit_code=1
    else
      IFS=$'\t' read -r is_private can_pull can_push is_admin <<< "$repo_json"
      if [[ "$is_private" == "true" && "$can_pull" == "true" ]]; then
        record PRIVATE_REPO_ACCESS PASS "private=true pull=$can_pull push=$can_push admin=$is_admin"
        exit_code=0
      else
        record PRIVATE_REPO_ACCESS FAIL "expected private repository with pull access; private=$is_private pull=$can_pull push=$can_push admin=$is_admin"
        exit_code=1
      fi
    fi
  fi
fi

if command -v sha256sum >/dev/null 2>&1; then
  sha256sum "$OUT" > "$OUT.sha256"
  printf 'receipt=%s\nreceipt_sha256=%s\n' "$OUT" "$(awk '{print $1}' "$OUT.sha256")"
else
  printf 'receipt=%s\nreceipt_sha256=TOKEN_VAZIO\n' "$OUT"
fi

exit "$exit_code"
