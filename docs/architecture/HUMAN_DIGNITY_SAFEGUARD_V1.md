# RafGitTools — Human Dignity Safeguard V1

Status: `IMPLEMENTED_SOURCE / SAFEGUARD_EVIDENCE_UNBOUND / CLAIM_ALLOWED=false`

## Governing principle

Human dignity is a non-compensatory boundary for governed promotion.

```text
HUMAN_DIGNITY > DELIVERY_SPEED
HUMAN_DIGNITY > CI_GREEN
CHILD_SAFETY > FEATURE_COMPLETION
UNKNOWN_HUMAN_IMPACT != SAFE
TOKEN_VAZIO != PASS
AI_ASSESSMENT != HUMAN_AUTHORITY
```

A technical PASS, a successful build, many independent agents, schedule pressure, business value or a low implementation cost MUST NOT compensate for an unresolved or failed human safeguard.

## Safeguards

The Commit Evidence Gate treats the following as required before Draft/Ready/Merge promotion:

1. `HUMAN_DIGNITY`
2. `CHILD_SAFETY`
3. `INCLUSION_NONDISCRIMINATION`
4. `ACCESSIBILITY_INCLUSION`
5. `SAFE_HEALTHY_WORK`

`NOT_APPLICABLE` is not accepted as a bypass for these required safeguards. A no-impact conclusion is represented as `PASS` only after a scoped assessment with traceable human evidence.

A safeguard `PASS` requires:

- a human producer/authority;
- traceable `sourceRef` and `proofRef`;
- typed risk severity and urgency;
- explicit falsifier;
- explicit mitigation/rollback reference;
- `claimAllowed=false` at the evidence-envelope level.

Other AIs, automations and providers may produce supporting evidence, criticism and falsifiers, but they do not self-certify the human safeguard boundary.

## Risk and urgency typing

Risk/urgency values describe governance priority; they are not a finding that harm has already occurred.

Recommended unresolved classifications:

| Safeguard | unresolved risk | urgency |
| --- | --- | --- |
| HUMAN_DIGNITY | CRITICAL | P0 |
| CHILD_SAFETY | CRITICAL | P0 |
| INCLUSION_NONDISCRIMINATION | CRITICAL | P0 |
| ACCESSIBILITY_INCLUSION | HIGH | P1 |
| SAFE_HEALTHY_WORK | CRITICAL | P0 |

All five remain non-compensatory even when the response urgency differs.

## Normative anchors

These are reference anchors, not a claim of certification, legal advice or complete jurisdictional compliance.

- Universal Declaration of Human Rights (United Nations), especially inherent dignity/equality and non-discrimination: https://www.un.org/en/about-us/universal-declaration-of-human-rights
- Convention on the Rights of the Child, Article 3: best interests of the child as a primary consideration: https://www.ohchr.org/en/instruments-mechanisms/instruments/convention-rights-child
- Convention on the Rights of Persons with Disabilities, Article 3: inherent dignity, non-discrimination, full and effective participation/inclusion, equality of opportunity and accessibility: https://www.ohchr.org/en/instruments-mechanisms/instruments/convention-rights-persons-disabilities
- ILO Declaration on Fundamental Principles and Rights at Work, amended 2022: freedom of association/collective bargaining, elimination of forced labour, abolition of child labour, elimination of discrimination, and a safe and healthy working environment: https://www.ilo.org/ilo-declaration-fundamental-principles-and-rights-work

## Fail-closed decision

```text
for safeguard in HUMAN_FIRST_REQUIRED:
    FAIL        -> BLOCK
    TOKEN_VAZIO -> BLOCK
    NOT_RUN     -> BLOCK
    N/A         -> BLOCK
    PASS        -> continue only if human authority + proof + risk + urgency + falsifier + mitigation are present
```

No scalar score can override a human-first blocker.

## Evidence boundary

```text
NORMATIVE_ANCHOR != LOCAL_ASSESSMENT
LOCAL_ASSESSMENT != PROOF
PROOF != LEGAL_CERTIFICATION
AI_GENERATED != HUMAN_APPROVED
NO_IDENTIFIED_HARM != PROOF_OF_ZERO_RISK
```

## Current source state

The source contract is materialized, but the live UI has no connected human-safeguard assessment receipts yet. Therefore the five safeguards are intentionally emitted as `TOKEN_VAZIO`, with risk/urgency typed, and promotion remains blocked until traceable human evidence is supplied.

This is expected fail-closed behavior, not a defect to hide.

## Falsifiers

1. remove one required human safeguard and verify Draft/Ready/Merge are blocked;
2. mark child safety `FAIL` and verify all promotion is blocked;
3. mark a human safeguard `NOT_APPLICABLE` and verify it cannot bypass the boundary;
4. mark a human safeguard `PASS` with an AI producer and verify rejection;
5. mark a human safeguard `PASS` without risk, urgency, proof, falsifier or mitigation and verify `TOKEN_VAZIO`/block;
6. keep all technical evidence PASS while one human safeguard is unresolved and verify promotion remains blocked.

## Non-claims

```yaml
legal_compliance_certified: false
human_rights_certified: false
child_safety_certified: false
accessibility_certified: false
zero_harm_claimed: false
claim_allowed: false
```
