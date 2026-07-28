#!/usr/bin/env sh
# Canonical dependency-free gate for RAFAELIA longitudinal, content validity,
# toroidal research-cycle, GitHub Actions execution-evidence, platform assurance,
# evidence-backed compliance, and human-AI bivalent privacy contracts.
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

python3 -m unittest discover -s tests -p 'test_toroidal_research_cycle.py' -v
python3 scripts/toroidal_research_cycle.py validate-contract \
  configs/toroidal_research_cycle_contract.json
python3 scripts/toroidal_research_cycle.py validate-manifest \
  configs/toroidal_research_cycle_contract.json \
  examples/toroidal_research_cycle.example.json
python3 scripts/toroidal_research_cycle.py summarize \
  configs/toroidal_research_cycle_contract.json \
  examples/toroidal_research_cycle.example.json

python3 -m unittest discover -s tests -p 'test_actions_execution_evidence.py' -v
python3 scripts/actions_execution_evidence.py validate-contract \
  configs/actions_execution_evidence_contract.json
python3 scripts/actions_execution_evidence.py validate-manifest \
  configs/actions_execution_evidence_contract.json \
  examples/actions_execution_evidence.example.json
python3 scripts/actions_execution_evidence.py summarize \
  configs/actions_execution_evidence_contract.json \
  examples/actions_execution_evidence.example.json

python3 -m unittest discover -s tests \
  -p 'test_platform_assurance_control_plane.py' -v
python3 scripts/platform_assurance_control_plane.py \
  configs/platform-assurance/index.json \
  --write-report artifacts/platform-assurance-report.json

python3 -m unittest discover -s tests \
  -p 'test_compliance_evidence_boundary.py' -v
python3 scripts/validate_compliance_evidence_boundary.py \
  --strict --write-report

# Human-AI bivalent privacy middleware: tests and semantic validation are part
# of the existing canonical gate, not a competing workflow.  The generated
# report proves contract evaluation only; it never promotes target runtime.
python3 -m unittest discover -s tests \
  -p 'test_human_ai_middleware.py' -v
python3 scripts/validate_human_ai_middleware.py \
  examples/human-ai-middleware/request.safe.json \
  --adapters configs/human-ai-middleware/adapters.v1.json \
  --report artifacts/human-ai-middleware-checkout-validation.json

make -C rafaelia/block1 clean check
make -C rafaelia/block1 clean
