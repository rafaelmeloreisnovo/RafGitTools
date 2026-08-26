# Normative references and evidence boundary

**Status:** `REFERENCE_FRIENDLY / TOKEN_VAZIO`  
**Claim gate:** `claim_allowed=false`

## What this document is

RafGitTools uses ISO, IEEE, NIST, OWASP, W3C, RFC and applicable privacy-law materials as
references for design, implementation and review. It aims to keep its engineering choices
friendly to the good practices those materials describe.

This document is not an audit report, a certificate, a legal opinion, an attestation, or a
statement that any requirement set has been attained. A source file, mapping table, internal
test or planned control is not a substitute for scoped evidence and recurring assessment.

For project-wide wording rules, see [CLAIM_LANGUAGE_POLICY.md](CLAIM_LANGUAGE_POLICY.md).

For privacy/data/AI/location questions, the current federated legal-semantic authority is the
Mapa evidence-first canon:

- `rafaelmeloreisnovo/Mapa/docs/legal/GLOBAL_DATA_PRIVACY_GNSS_AI_GOVERNANCE_V1.md`
- `rafaelmeloreisnovo/Mapa/data/normative-graph/GLOBAL_DATA_PRIVACY_GNSS_AI_SEMANTIC_ATLAS_V1.json`
- `rafaelmeloreisnovo/Mapa/data/receipts/legal/GLOBAL_DATA_PRIVACY_GNSS_AI_GOVERNANCE_RECEIPT_20260826_V2.json`

RafGitTools must not duplicate or silently reinterpret that legal layer. It consumes it as a
versioned `REFERENCE` and produces implementation/execution receipts for the concrete scope.

## Current evidence state

| Area | What is present | What is not established |
|---|---|---|
| Reference registry | Official-edition references are recorded in `configs/normative-reference-registry.v1.json`. | Applicability, implementation and assessed effectiveness for each reference. |
| Source model | `ComplianceManager` can hold scoped evidence and defaults to `NOT_ASSESSED`. | Any positive normative or regulatory conclusion. |
| Control catalogues | Proposed security, privacy and quality controls are documented. | That a control operates effectively in a declared scope. |
| Local and CI gates | Some source/build gates can generate receipts. | Independent review, device/runtime coverage and any formal result. |
| Federated privacy canon | Mapa contains a versioned privacy/GNSS/AI legal-semantic canon and receipt chain. | Automatic applicability to any RafGitTools execution. |

All unbound cells above are `TOKEN_VAZIO`.

## Reference use by family

| Family | Intended use | Required evidence before a narrower statement |
|---|---|---|
| ISO quality and security references | Design/review checklist and traceability vocabulary. | Declared scope, current edition, control mapping, execution receipts and assessment. |
| IEEE lifecycle and verification references | Engineering-process and test-planning guidance. | Process records, review records and repeatable execution evidence. |
| NIST and OWASP references | Threat-model and defensive-design guidance. | Threat model, configuration, adversarial tests and deployed-runtime evidence. |
| RFC/W3C references | Interoperability and protocol/design reference. | Versioned test vectors, interoperability results and relevant external review. |
| GDPR, CCPA/CPRA, LGPD, Marco Civil and related laws | Legal design considerations and data-flow review prompts, routed through the Mapa canon. | Jurisdiction, factual scope, current authority, applicable legal basis and qualified legal review where needed. |
| GNSS/geolocation and sensor data | Treat location fix, raw GNSS measurements, NMEA, satellite status and network-derived location as distinct data surfaces. | Runtime receipt proving actual collection, permission, recipient, purpose, minimization, retention and transfer. |
| SPDX and license material | Dependency/license metadata and review prompts. | Complete dependency inventory and qualified legal review. |

## Evidence record required for a control

```yaml
reference: ""
edition: ""
scope: ""
jurisdiction: "TOKEN_VAZIO"
data_flow: "TOKEN_VAZIO"
requirement_or_control: ""
implementation_receipt: "TOKEN_VAZIO"
execution_receipt: "TOKEN_VAZIO"
environment: "TOKEN_VAZIO"
review_or_audit: "TOKEN_VAZIO"
falsifier: ""
claim_allowed: false
```

An evidence record must identify the exact artifact and environment. A percentage calculated
inside the repository has no meaning outside its stated denominator and does not turn into a
formal conclusion.

## Working approach

1. Verify the applicable edition/authority in the normative-reference registry and Mapa canon.
2. Declare the product, release, jurisdiction and feature scope.
3. For personal/device data, map `source -> permission -> collection -> purpose -> recipient -> retention -> transfer`.
4. Map a single concrete requirement or control to source and test artifacts.
5. Execute the relevant test in a named environment and retain a receipt.
6. Preserve failures, exceptions and absent evidence as `TOKEN_VAZIO`.
7. Obtain the required independent, legal or accredited review when the claim type needs it.

Until all relevant steps are complete, project language remains reference-friendly and
`claim_allowed=false`.

## Hard invariants

```text
OS_PERMISSION != LAWFUL_BASIS
data_exists_on_device != data_reaches_service != data_reaches_model
precise_location != raw_GNSS_telemetry
standard != law
project_policy != universal_conflict_of_laws_rule
TOKEN_VAZIO != false
```

## Next verifiable actions

- Keep the normative-reference registry current through official sources.
- Bind each proposed control to a versioned source/test/runtime receipt.
- Add physical Android evidence to Android-specific controls.
- For any location/sensor path, capture the exact field-level data flow rather than infer from permission names.
- Route legal, accessibility and certification questions to the appropriate qualified review.

Historical planning language is superseded by this boundary. It remains recoverable through
Git history, but is not current evidence.
