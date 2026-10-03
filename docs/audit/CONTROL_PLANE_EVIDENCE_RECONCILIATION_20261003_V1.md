# Control Plane Evidence Reconciliation V1 — 2026-10-03

```text
repo=rafaelmeloreisnovo/RafGitTools
branch_base=0a08334809aa6680ccebd3c3acd5d695b93b8560
historical_baseline=8af97a580e535d2015e8211000850e282b031763
bounded_compare_head=3f63ac845fcc4fed99c62087d52b75148dcc9aa1
partition=R642-D1_CONTROL_PLANE_EVIDENCE
state=RECONCILED_BOUNDED_CONTROL_PLANE
historical_transition_replay=TOKEN_VAZIO_NOT_EXHAUSTIVELY_REPLAYED
privileged_lane_runtime=TOKEN_VAZIO_NOT_EXECUTED_IN_REFERENCE_RUN
claim_allowed=false
release_allowed=false
```

## Intent

Reconcile the current authority and evidence semantics of the canonical START control plane without claiming that every historical transition or every conditional/privileged lane executed.

```text
SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM
PARTITIONED != RECONCILED
SKIPPED != PASS
TOKEN_VAZIO != FAIL != PASS
CI_PASS != DEVICE_PASS != RELEASE_PASS
```

## Authority roots

The bounded audit uses the smallest authority set needed to resolve D1:

1. `.github/workflows/START.yml` at `0a08334809aa6680ccebd3c3acd5d695b93b8560`;
2. `.github/actions/workflow-graph-audit/action.yml` and its `audit.py` implementation at the same revision;
3. `docs/ci/START_PIPELINE_V1.md` as documentation cross-check only.

Provider compare `8af97a... -> 3f63ac...` reports 642 commits in the bounded post-baseline interval. In that net compare, `.github/workflows/START.yml` is modified by `+492/-60`; `.github/workflow-archive/scientific-api-source-microscope.yml` is added. The local `workflow-graph-audit` action does not appear as a changed path in that net interval, so this audit does not claim that mechanism was replaced there.

## Current routing semantics

`START.yml` is the canonical active YAML workflow and declares read-only top-level permissions (`contents: read`, `actions: read`). The plan checks out `EXECUTION_SHA` and verifies source identity before routing.

In `auto` mode:

- `android=true`;
- `security=true`;
- federation is selected only when its bounded path families change;
- docs is selected only when documentation/Markdown paths change;
- provider environment/actions/private-CI one-shot lanes require explicit main-push trigger files and their own fail-closed operation/target rules;
- public-data custody is not silently promoted by auto;
- release is a separate release route.

Manual modes are explicit. `full` selects android+federation+security+docs; `release` selects android+security+release. Privileged lanes retain their own event/actor/authority/secret conditions.

Therefore:

```text
AUTO_SUCCESS != ALL_LANES_EXECUTED
OPTIONAL_SKIPPED != OPTIONAL_PASS
PR_START_SUCCESS != PROVIDER_MUTATION_PROVEN
PR_START_SUCCESS != SIGNED_RELEASE_PROVEN
```

## Workflow topology audit boundary

The topology job calls `.github/actions/workflow-graph-audit`, whose implementation identifies itself as a **lexical structural inventory, not a full YAML AST/semantic parser**.

Its bounded checks include:

- workflow/job topology and `needs` relationships;
- undefined/self/cyclic dependencies;
- duplicate job IDs;
- explicit permissions and `write-all` detection;
- concurrency presence;
- `pull_request_target`/`secrets: inherit` advisories;
- mutable remote/local dependency classification;
- strict changed-file ratchet for new regressions.

The action explicitly does not constitute security/compliance certification. Its success must therefore be read as `STRUCTURAL_TOPOLOGY_PASS`, not `FULL_WORKFLOW_SEMANTICS_PASS`.

## Final receipt semantics

The final job `09 · Final receipt / fechamento` has `if: always()` and depends on all declared lanes.

Its source logic:

- maps a GitHub job result `skipped` to `TOKEN_VAZIO_NOT_SELECTED`;
- maps executed result text to uppercase state;
- collects `failure` or `cancelled` stages as failures;
- writes `state=FAIL` when such failures exist;
- otherwise writes `state=PASS_WITH_TYPED_SKIPS`;
- raises a failing exit when failures exist;
- keeps `claim_allowed=false`, `automatic_promotion=false`, `training_executed=false`.

This rule existed at the historical baseline. The current version extends the receipt with explicit `execution_sha`/PR source identity and newer lane fields, while preserving the fail/typed-skip boundary.

## Exact execution anchor — START #596

Reference run:

```text
run_id=37094037607
candidate_execution_sha=2a0e54cfd3565a31ff0a0d26ffa2b216ff18b100
event=pull_request
mode=auto
workflow_conclusion=success
final_receipt_job=111121591699:SUCCESS
```

Provider job evidence:

- Plan `111120177564`: SUCCESS;
- Workflow topology `111120195298`: SUCCESS, including single-root assertion + graph audit + topology receipt upload;
- Coherence `111120212592`: SUCCESS;
- Python deterministic tests `111120272105`: SUCCESS;
- Documentation `111120272170`: SUCCESS;
- Android `111120272106`: SUCCESS;
- CodeQL Actions `111120272151`: SUCCESS;
- CodeQL Java/Kotlin `111120272086`: SUCCESS;
- Federation `111120273004`: SKIPPED;
- Provider environments `111120272869`: SKIPPED;
- Provider actions `111120272987`: SKIPPED;
- Private-CI bridge `111120272958`: SKIPPED;
- Public-data custody `111120272933`: SKIPPED;
- Signed release `111121592606`: SKIPPED;
- Final receipt `111121591699`: SUCCESS.

The Python job checked out the exact candidate SHA and ran `python3 -m unittest discover -s tests -p 'test_*.py' -v`: **609 tests / OK**. This included control-plane tests for global required jobs, conditional lanes, provider routing, private-CI routing, access gating and no silent claim promotion. Unit-test PASS validates source contracts; it does not substitute for executing privileged provider lanes.

## Physical receipt artifact

Run #596 produced artifact:

```text
name=start-09-receipt-37094037607
artifact_id=11263043351
artifact_digest=sha256:eee1d640055a96817a323d571ade2e07d3225ec20646c3aaa42c93d3ad2423e1
expired=false
```

The extracted `START_RECEIPT.json` binds:

```text
execution_sha=2a0e54cfd3565a31ff0a0d26ffa2b216ff18b100
mode=auto
state=PASS_WITH_TYPED_SKIPS
claim_allowed=false
automatic_promotion=false
training_executed=false
```

Successful stages in that receipt: `plan`, `topology`, `coherence`, `python_tests`, `docs`, `android`, `security`.

Typed not-selected stages: `federation`, `provider_actions`, `private_ci_bridge`, `provider_environments`, `release`, `public_data_custody`.

This artifact closes the ambiguity between “skipped because not selected” and “passed”. It does **not** close the skipped lanes.

## Historical interval classification

The baseline and current net source were compared, but the 642 intermediate commits were not replayed one by one. Therefore:

```text
current_control_plane_authority = RECONCILED_BOUNDED
baseline_to_current_net_semantics = RECONCILED_BOUNDED
intermediate_transition_replay = TOKEN_VAZIO_NOT_EXHAUSTIVELY_REPLAYED
privileged_lane_runtime = TOKEN_VAZIO_NOT_EXECUTED_IN_REFERENCE_RUN
physical_device = TOKEN_VAZIO_PHYSICAL_DEVICE_REQUIRED
signed_release = TOKEN_VAZIO_RELEASE
```

D1 is considered reconciled only for **control-plane authority, routing semantics, structural audit boundary, final-receipt semantics and the exact reference execution**. Provider authority/mutations remain D3 evidence; runtime/device/release remain their own gates.

## Reconciliation result

`R642-D1 CONTROL_PLANE_EVIDENCE` moves from `PARTITIONED_UNRECONCILED` to:

```text
RECONCILED_BOUNDED_CONTROL_PLANE
```

This state is deliberately narrower than `PASS_ALL`.

Residuals:

- `TOKEN_VAZIO_D1_HISTORICAL_TRANSITION_REPLAY` — intermediate control-plane revisions not replayed exhaustively;
- `TOKEN_VAZIO_D1_PRIVILEGED_LANE_RUNTIME` — provider/private/public-data lanes not executed by reference run #596;
- `TOKEN_VAZIO_RELEASE` — signed release was not selected;
- downstream D2–D7 remain independently gated.

## Next

Per BR-4 leverage order, after this documentation candidate itself passes exact-head START and is promoted, the next semantic domain is `R642-D3 PROVIDER_GOVERNANCE`. D3 must use authoritative provider readback and must not inherit runtime proof from D1.

## R3

- **F_ok:** current START authority, routing semantics, topology-audit scope, fail-closed final receipt, 609-test contract suite and exact #596 receipt are reconciled with explicit typed skips.
- **F_gap:** historical intermediate transitions were not exhaustively replayed; privileged/provider/public-data/release lanes skipped in #596 remain unproved runtime states.
- **F_next:** exact-head gate this documentation reconciliation; after promotion move to D3 provider governance, without altering START or opening feature expansion.
