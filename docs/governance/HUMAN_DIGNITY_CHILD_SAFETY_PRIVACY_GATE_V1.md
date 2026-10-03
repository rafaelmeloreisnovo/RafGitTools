# RAFAELIA Human Dignity, Child Safety & Privacy Gate V1

Status: `DRAFT_GOVERNANCE_CONTRACT`

This document defines a fail-closed governance gate for research, software, AI, automation and data processing that may affect people. It is a project control, not a certification, legal opinion, regulatory approval or deployment authorization.

## Core invariant

```text
SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM
TOKEN_VAZIO != PASS
IMPLEMENTED_UNTESTED != PASS
GATE_PASS != DEPLOYMENT_AUTHORIZATION
```

The machine-readable gate may produce only:

```text
BLOCKED
READY_FOR_AUTHORIZED_REVIEW
```

It never emits `DEPLOYMENT_AUTHORIZED=true`.

## Human dignity and agency baseline

Every human-facing case MUST document:

- a specific, understandable purpose;
- an accountable human owner;
- non-manipulation and prohibition of dark-pattern coercion;
- accessible notice appropriate to the affected population;
- a route for questions, contestation and redress;
- human override for consequential uses;
- rollback and incident response;
- evidence references sufficient to reproduce the gate decision.

Autonomy is treated as a protected design condition. The system must not optimize for covert behavioral control, involuntary persuasion, deception, dependency or suppression of lawful human choice.

## Children and adolescents

If children or adolescents may reasonably use or be affected by the system, the gate becomes stricter and remains fail-closed until there is documented evidence for:

- best-interest assessment;
- child-rights and lawful-basis review;
- proportional age-assurance assessment, when age assurance is relevant;
- child-appropriate explanations;
- protection from behavioral manipulation;
- respect for progressive autonomy;
- safeguarding escalation;
- privacy/data-protection impact analysis when personal data is processed.

Parental or legal-guardian consent is not hard-coded as the only possible lawful basis. The applicable legal basis must be reviewed in context, while the best interests of the child remain a mandatory project gate.

The design must not use child protection as a pretext for generic or indiscriminate surveillance.

## Privacy and data protection

When personal data is processed, the gate requires documented:

- lawful-basis review;
- data minimization;
- retention rules;
- security controls;
- privacy impact assessment;
- purpose limitation and evidence traceability.

Age-assurance data, when used, must be purpose-limited and minimized. The system should prefer the least intrusive mechanism capable of satisfying the actual risk and legal requirement.

## Local cultures, Indigenous Peoples and traditional knowledge

When Indigenous Peoples or culturally distinct communities may be materially affected, the gate requires:

- meaningful consultation;
- identification of the community's own representative institutions;
- documented assessment of whether free, prior and informed consent (FPIC) applies;
- local-language/accessibility planning;
- protection of traditional knowledge and culturally sensitive information;
- respect for a community decision to withdraw or decline participation when applicable.

FPIC is not mechanically declared applicable to every context. The applicability assessment itself may not be omitted.

The project must not treat cultural diversity as a dataset to be extracted. Participation, attribution, benefit sharing, context and community governance must be addressed before reuse of culturally sensitive material.

## Equity, accessibility and social benefit

A claim that a system reduces social inequality is not accepted from intent alone.

For public-impact or vulnerable-population cases, the gate requires:

- equity impact assessment;
- accessibility plan;
- expected distribution of benefits and harms;
- measurable outcomes defined before deployment;
- post-deployment monitoring capable of detecting unequal error, exclusion or burden.

A project may be well-intentioned and still fail this gate.

## High-risk and regulated use

For `HIGH` or `CRITICAL` risk tiers, the gate additionally requires:

- a documented safety case;
- independent review;
- applicable authority, ethics or regulatory review evidence;
- fail-safe behavior;
- monitoring;
- tested rollback;
- incident response.

`READY_FOR_AUTHORIZED_REVIEW` means the machine-readable prerequisites are complete enough for accountable review. It does not mean a regulator, ethics committee, institution, parent, community or affected person has approved deployment.

## Standards and public references used as design inputs

These references are used as governance inputs; no certification or full conformity is claimed:

- ISO/IEC 42001:2023 — AI management systems.
- ISO/IEC 23894:2023 — AI risk management guidance.
- ISO/IEC 27001:2022 — information security management systems.
- ISO/IEC 27701:2025 — privacy information management systems.
- NIST AI RMF 1.0 — Govern, Map, Measure, Manage; current published framework while a revision is in progress.
- UNESCO Recommendation on the Ethics of Artificial Intelligence — human rights, dignity, diversity, inclusion, transparency and human oversight.
- Convention on the Rights of the Child and General Comment No. 25 (2021) on children's rights in the digital environment.
- UNICEF Guidance on AI and children, Version 3.0 — child-centred AI requirements.
- Brazil LGPD, including Article 14 and ANPD Enunciado No. 1/2023 on children's and adolescents' data.
- Brazil Law 15.211/2025 (ECA Digital) and its 2026 regulatory implementation.
- United Nations Declaration on the Rights of Indigenous Peoples, including good-faith consultation and FPIC-related protections in applicable contexts.

Normative applicability is case-specific. If applicability or a clause/control mapping has not been verified, record `TOKEN_VAZIO_NORMATIVE` rather than claiming compliance.

## Operational chain

```text
INTENT
-> POPULATION_MAP
-> PURPOSE_AND_AUTHORITY
-> DATA_MAP
-> RIGHTS_IMPACT
-> CHILD_SAFETY
-> CULTURAL_AND_COMMUNITY_REVIEW
-> SECURITY_AND_PRIVACY
-> EQUITY_AND_ACCESSIBILITY
-> SAFETY_CASE
-> EVIDENCE
-> HUMAN/AUTHORITY_REVIEW
-> EXECUTION
-> MONITORING
-> INCIDENT/ROLLBACK
-> RECEIPT
```

No stage may inherit `PASS` from another stage without evidence.

## Falsifiers

The gate MUST block when any applicable requirement is missing, including:

- `TOKEN_VAZIO` owner or purpose;
- manipulative behavior or dark patterns;
- missing redress or human override for consequential use;
- personal-data use without documented lawful-basis/privacy review;
- child impact without best-interest and safeguarding evidence;
- Indigenous/community impact without consultation and FPIC applicability assessment;
- public/vulnerable impact without equity/accessibility analysis;
- high-risk operation without safety case, independent review and authority review;
- missing rollback, incident response or evidence references.

## Receipt boundary

A receipt should record at least:

```text
case_id
source_ref
source_hash
owner
purpose
population_triggers
data_triggers
risk_tier
applicable_controls
blockers
expected_observable
actual_observable
evidence_refs
review_authority
rollback_ref
observed_at
gate_state
deployment_authorized=false
claim_allowed=false
```

Receipts are append-only. A later authorization must be a separate authority artifact referencing this gate result; it must never rewrite the original gate receipt.
