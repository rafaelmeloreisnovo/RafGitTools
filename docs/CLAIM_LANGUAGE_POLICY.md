# Claim and Reference Language Policy / Política de linguagem de evidência

**Status:** `REFERENCE_FRIENDLY`  
**Default:** `claim_allowed=false`  
**Effective date:** 2026-08-14

## Purpose

RafGitTools may use normative references and established engineering practices as
design aids. It is **reference-friendly**: it aims to preserve applicable good
practices while the relevant source, build, runtime and review evidence is collected.

That is not a declaration of conformity, certification, legal/regulatory attainment,
accessibility level, security assurance or release readiness.

## Required language

Use bounded expressions such as:

- “uses _X_ as a reference for design and review”;
- “aims to follow applicable good practices from _X_”;
- “maps a proposed control to _X_; evidence remains pending”;
- “source-level implementation exists; operating effectiveness is `TOKEN_VAZIO`”; and
- “requires a scoped assessment and independent review where applicable.”

Do not describe an implementation, a checklist, a CI job, a dependency scan, an
internal percentage or a documentation file as proof that the project has attained a
norm, certification or regulatory result.

## Evidence ladder

```text
NARRATIVE
  -> DOCUMENTED
  -> IMPLEMENTED_SOURCE
  -> MEASURED_LOCAL
  -> REPRODUCED
  -> CROSS_HOST / INDEPENDENT_ASSESSMENT (when applicable)
```

Each step is narrower than the next. A passing source or CI gate does not establish
operating effectiveness in a device, organization or legal scope. Missing evidence is
recorded as `TOKEN_VAZIO`, not converted into a positive statement.

## Claim gate

A limited statement can be considered only when its record binds all of the following:

```yaml
claim_allowed: false
scope: required
reference_edition: required
control_or_requirement: required
implementation_receipt: required
execution_receipt: required
environment: required
review_or_audit: required_when_applicable
falsifier: required
```

Until then, the project keeps `claim_allowed=false`. Legal, certification and formal
assessment decisions belong to the appropriately qualified and independent review
process; they are not inferred from this repository.

## Source of reference editions

The edition registry is [`configs/normative-reference-registry.v1.json`](../configs/normative-reference-registry.v1.json).
It records references, not achievements. See also
[COMPLIANCE_EVIDENCE_BOUNDARY.md](COMPLIANCE_EVIDENCE_BOUNDARY.md) and
[federation/NORMATIVE_REFERENCE_STATUS_V1.md](federation/NORMATIVE_REFERENCE_STATUS_V1.md).

## Maintenance rule

When a document is updated, remove unsupported promotional wording and preserve the
missing proof as `TOKEN_VAZIO` with the next verifiable action. A recurring audit may
increase evidence for one bounded scope; it never retroactively proves unrelated
features or documents.
