# Authority Pyramid Debug + Security Gate V1

Status: `FAIL_CLOSED / claim_allowed=false`

RafGitTools is the executor/tool-router, not the owner of every producer's runtime truth. This gate prevents a debugger, CI signal, receipt or routing decision from crossing an authority boundary without the evidence required by the target claim.

## Failure state is not one bit

Track two orthogonal dimensions.

**Evidence state** may be `TOKEN_VAZIO`, `OBSERVED`, `PARTIAL`, `EVIDENCED_SCOPED`, `FAILURE`, `BUG_CONFIRMED`, `REGRESSION`, `SECURITY_WEAKNESS`, `VULNERABILITY_SUSPECTED`, `VULNERABILITY_CONFIRMED`, `PRIVACY_RISK`, `COMPLIANCE_GAP`, `GOVERNANCE_GAP`, `DEBUG_BLOCKER`, `NEAR_MISS`, `INCIDENT`, `SUPERSEDED`, or `NOT_APPLICABLE_WITH_EVIDENCE`.

**Attention state** may be `ACTIVE`, `URGENT`, `IGNORED_DISCOVERED`, `FORGOTTEN_REDISCOVERED`, `UNDERPRIORITIZED`, `DEFERRED_WITH_OWNER`, `ABORTED_WITH_REASON`, or `BLOCKED_EXTERNAL`.

An ignored item is not automatically a bug. A bug is not automatically a vulnerability. A security control is not automatically effective. A compliance reference is not certification.

## Debugger authority rule

```text
symptom -> reproduction -> owning component -> exact ref/hash -> local falsifier
        -> producer evidence -> edge evidence -> receipt -> federated state
```

Stop and emit `TOKEN_VAZIO` when the owning authority, exact artifact, environment or required privacy/security classification cannot be resolved.

## P0 non-compensatory gates

The following block promotion even if unrelated tests are green:

- exposed secret/credential or unbounded sensitive log;
- unknown authorization on a critical execution/IPC surface;
- direct authoritative-branch mutation without demonstrated server barrier;
- child/vulnerable-subject data with unresolved purpose/data flow/authority/best-interest context;
- raw guest/user payload crossing a public receipt boundary;
- high-impact mutation without rollback;
- runtime/security claim without the device/artifact evidence required by that scope.

## Debug receipt minimum

Record exact repo/ref/commit, symptom, reproduction, owner, evidence state, attention state, data/privacy/security classification, falsifier, rollback, command/test, environment, output/artifact hashes, edge dependencies and `F_ok/F_gap/F_next`.

Do not store private payload when a digest or typed reference is sufficient.

## Falsifier

A routed promotion that bypasses the owning producer or treats an unresolved P0 governance/privacy/security state as PASS falsifies this gate.
