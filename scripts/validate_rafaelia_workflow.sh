#!/usr/bin/env sh
# Canonical dependency-free gate for RAFAELIA longitudinal and content validity contracts.
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$ROOT"

python3 -m unittest discover -s tests -p 'test_workflow_session_contract.py' -v
python3 scripts/workflow_session_contract.py validate-index workflow-master-index.json
python3 scripts/workflow_session_contract.py validate-session \
  workflow-master-index.json examples/workflow_session.example.json
python3 scripts/workflow_session_contract.py summarize \
  workflow-master-index.json examples/workflow_session.example.json

python3 -m unittest discover -s tests -p 'test_content_validity_contract.py' -v
python3 scripts/content_validity_contract.py validate-contract \
  configs/content_validity_contract.json
python3 scripts/content_validity_contract.py validate-manifest \
  configs/content_validity_contract.json examples/content_validity.example.json
python3 scripts/content_validity_contract.py summarize \
  configs/content_validity_contract.json examples/content_validity.example.json
