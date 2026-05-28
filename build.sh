#!/usr/bin/env bash
set -euo pipefail

# Canonical local build entrypoint used by AGENTS.md and CI docs.
# Produces debug APKs, unsigned release APKs for internal validation,
# and signed release APKs only when release signing credentials are present.

exec ./scripts/native/build_apks_signed_unsigned.sh
