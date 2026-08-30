# Human Impact Cross-Repo Gate V1

Status: **IMPLEMENTED_CONTRACT /**
**LIVE_ETHICS_RECEIPT_TOKEN_VAZIO**

## Purpose

RafGitTools transports and validates cross-repository contracts. It does not
assign human worth or decide clinical appropriateness, child welfare, cultural
legitimacy, or legal sufficiency.

```text
LOCAL_PASS != HUMAN_IMPACT_PASS
TECHNICAL_CORRECTNESS != ETHICAL_PERMISSION
MODEL_RECOMMENDATION != HUMAN_VALUE_DECISION
```

The routing authority for the common human-dignity policy is the Mapa
control-plane artifact:

`data/control-plane/HUMAN_DIGNITY_ETHICS_RATCHET_V1.json`

Producer repositories remain authoritative for their own artifacts and
evidence.

## When this gate applies

A producer must emit a human-impact record when a change materially touches
children, health, education, privacy, civil or human rights, culture or belief,
accessibility, livelihoods or essential resources, public safety, humanitarian
contexts, or environmental externalities.

Low-impact internal refactors may remain outside the human-impact payload when
they do not change an exported human-facing contract or consequence radius.

## Fail-closed behavior

A material human-impact record is rejected when:

- required fields are missing;
- `TOKEN_VAZIO` is coerced to `false`, `0`, `null`, or `safe`;
- a local technical PASS is promoted to an ethics PASS;
- a synthetic fixture is promoted to live evidence;
- high-impact final authority is assigned to one actor without review or
  appeal;
- child protection is reduced to an optimization weight;
- privacy, rollback, consequence radius, or affected-group review is silently
  removed;
- a breaking contract change lacks migration and rollback.

## Required payload

The machine-readable contract is:

`contracts/human_impact_cross_repo.v1.json`

For material impact, the record includes affected groups, protected domains,
benefits, harms, unknowns, distribution, privacy surface, child, health,
education, culture and environmental impact, consequence radius,
reversibility or mitigation, appeal, review roles, evidence, falsifier, and
decision state.

## Decision states

```text
ALLOW_LOW_IMPACT
ALLOW_WITH_MONITORING
REQUIRE_PLURAL_REVIEW
REQUIRE_AFFECTED_GROUP_REVIEW
FAILSAFE_HOLD
TOKEN_VAZIO
CONTESTED
INSUFFICIENT_EVIDENCE
BLOCKED_BY_RIGHTS
ROLLBACK_REQUIRED
```

RafGitTools validates state and contract integrity only. It cannot synthesize
independent human, community, professional, legal, or child-safety review
represented by those states.

## Anti-regression

This gate extends `CROSS_REPO_IMPACT_REVIEW_V1` and keeps its migration and
rollback rule.

```text
LATEST != STRONGER
LOCAL_FIXTURE_PASS != LIVE_CROSS_REPO_INTEROP
```

Human-protection fields cannot disappear because a newer producer version is
faster or simpler. An incompatible producer change must version the contract,
provide migration, and preserve a concrete rollback anchor.

## Epistemic boundary

Passing `tools/validate_human_impact_cross_repo.py` proves only that the local
contract retains its declared structural safeguards. It does not prove legal
compliance, ethical adequacy, social acceptance, clinical safety, child safety,
or environmental harmlessness.

The live cross-repository ethics receipt remains:

```text
TOKEN_VAZIO
```

until exact producer and consumer revisions execute together with an actual
human-impact record.
