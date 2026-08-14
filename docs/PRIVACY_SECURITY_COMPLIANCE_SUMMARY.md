# Privacy, security and normative-reference inventory

**Status:** `HISTORICAL_SOURCE_INVENTORY / TOKEN_VAZIO`  
**Claim gate:** `claim_allowed=false`

## Correction of historical wording

Earlier versions of this document promoted implementation inventories and planned controls
into broad conclusions. Those conclusions are withdrawn. A documented file, source class,
configuration entry or checklist is not evidence of operating effectiveness, legal scope or
independent assessment.

The historic artifacts remain in repository history for provenance. Their current reading is
limited to: they describe ideas, mappings and source work that may need verification.

## Inventory, not achievement

| Area | Repository artifacts may include | Current interpretation |
|---|---|---|
| Privacy | Design notes, data-control code paths and configuration. | Source/design inventory; device behavior and legal scope are `TOKEN_VAZIO`. |
| Security | Network/storage/credential design choices and tests. | Candidate controls; deployed effectiveness is `TOKEN_VAZIO`. |
| Normative references | ISO, IEEE, NIST, OWASP, RFC and legal-reference mappings. | Reference material only; no broad conclusion is allowed. |
| Build and CI | Workflows, source checks and historical artifacts. | Evidence only for the exact run and commit, never a project-wide result. |

## Current authoritative boundaries

- [CLAIM_LANGUAGE_POLICY.md](CLAIM_LANGUAGE_POLICY.md) — wording and promotion policy.
- [COMPLIANCE.md](COMPLIANCE.md) — reference use and required evidence record.
- [SECURITY.md](SECURITY.md) — security design/evidence boundary.
- [PRIVACY.md](PRIVACY.md) — privacy design/evidence boundary.
- [COMPLIANCE_EVIDENCE_BOUNDARY.md](COMPLIANCE_EVIDENCE_BOUNDARY.md) — source-model gate.
- [federation/NORMATIVE_REFERENCE_STATUS_V1.md](federation/NORMATIVE_REFERENCE_STATUS_V1.md) — current reference-registry status.

## Next step per proposed control

```yaml
source_or_design_reference: required
named_scope: required
test_or_inspection: required
execution_environment: required
receipt: required
independent_or_legal_review: required_when_applicable
claim_allowed: false
```

Use `TOKEN_VAZIO` when one of these inputs is absent. The project can be friendly to good
practices while it gathers evidence; it must not convert that intent into a broad claim.

