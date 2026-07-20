# Compliance evidence boundary

## Problem corrected

The previous implementation could display `FULLY_COMPLIANT` from values written
inside the source code:

```text
implemented = true
hasQAProcess = true
testCoverage = 85.0
criticalVulnerabilities = 0
```

Those values were not linked to an audit package, tool output, artifact, commit,
device, assessor or observation date. They were therefore declarations, not
compliance evidence.

The safe rule is:

```text
control name != implementation evidence
source presence != operational proof
percentage != certification
no evidence != zero-percent non-compliance
```

## Default state

`ComplianceManager()` now uses `EmptyComplianceEvidenceProvider`.

For every enum value:

```text
level            = NOT_ASSESSED
assessmentState  = TOKEN_VAZIO
percentage       = 0
lastAuditDate    = epoch marker, not current time
claimAllowed     = false
evidenceRefs     = []
```

The `percentage=0` field remains for API compatibility. It must be interpreted
together with `assessmentState=TOKEN_VAZIO`; it is not a measured zero.

## Evidence package

A standard can be evaluated only through `ComplianceEvidence`:

```text
standard
satisfiedCriteria
totalCriteria
evidenceRefs
observedAt
findings
fullThreshold
substantialThreshold
```

The package is rejected when:

- `totalCriteria <= 0`;
- satisfied criteria are outside `0..totalCriteria`;
- evidence references are empty or blank;
- thresholds are outside `1..100`;
- the substantial threshold exceeds the full threshold.

Even an observed result keeps:

```text
claimAllowed=false
```

because an internal calculation does not certify conformity.

## Enum coverage

```kotlin
ComplianceStandard.entries.associateWith(::evaluateStandard)
```

This removes map/enum drift. Adding a future enum value automatically produces
`NOT_ASSESSED` until an evidence provider supplies a valid package.

## Control catalogues

Security and privacy controls remain useful as catalogues, but all currently
ship with:

```text
implemented=false
evidenceRefs=[]
```

That means **not evidenced in this process**, not a legal conclusion that the
organization is non-compliant.

## Block1 build correction

The new Makefile is C-only:

```text
C11
-Wall -Wextra -Wpedantic
no C++-only -fno-exceptions
no -lm in CFLAGS
separate CPPFLAGS/CFLAGS/LDFLAGS/LDLIBS
```

`make check` builds the static library and demo and verifies deterministic
output structure plus `tick=42` and the attractor enum.

## Canonical gate

```bash
python3 -m unittest discover -s tests \
  -p 'test_compliance_evidence_boundary.py' -v
python3 scripts/validate_compliance_evidence_boundary.py \
  --strict --write-report
make -C rafaelia/block1 clean check
make -C rafaelia/block1 clean
```

It is integrated into `scripts/validate_rafaelia_workflow.sh`; no competing
workflow YAML was added.

## Evidence state

```text
Kotlin logical harness       = PASS locally
block1 C build/check         = PASS locally
source mutation tests        = IMPLEMENTED
GitHub Actions               = ZERO_STEP_NO_LOGS
Android application build    = TOKEN_VAZIO on final head
independent compliance audit = TOKEN_VAZIO
certification claimed        = false
claim_allowed                = false
```
