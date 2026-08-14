# Security design references and evidence boundary

**Status:** `SOURCE_AND_DESIGN_REFERENCE / TOKEN_VAZIO`  
**Claim gate:** `claim_allowed=false`

## Scope

This document describes how RafGitTools intends to use security references in design and
review. It does not provide a security assurance, a certification, a penetration-test result,
or a statement that a given deployment is safe.

See [CLAIM_LANGUAGE_POLICY.md](CLAIM_LANGUAGE_POLICY.md) for the repository-wide wording
rule and [COMPLIANCE.md](COMPLIANCE.md) for the evidence record required before a narrow,
scoped conclusion can be considered.

## Design references

The project may consult NIST, OWASP MASVS/MASTG, ISO/IEC security materials, Android platform
guidance, and protocol specifications. These references are used to identify questions and
possible controls; they do not establish that the corresponding property holds in the app.

## Source-level practices to verify

| Practice | Source-level intent | Evidence still required |
|---|---|---|
| Credential handling | Avoid placing tokens, client secrets and private keys in source or receipts. | Runtime inspection, storage review and negative tests on the reviewed build. |
| Network traffic | Prefer authenticated HTTPS and restrict cleartext according to app configuration. | Device traffic capture, certificate/hostname tests and configuration receipt. |
| Local storage | Prefer app-private storage for sensitive operational data. | Device/file-system review, migration tests and recovery evidence. |
| Input and path handling | Validate repository, path and mutation inputs; fail closed when contracts do not match. | Adversarial tests and device/runtime receipts. |
| High-impact GitHub operations | Require explicit confirmation and retain redacted receipts. | Controlled-account execution and review of the resulting receipts. |
| Dependencies | Inspect declared dependencies and their metadata. | Complete inventory, advisory review and a qualified license/security assessment. |

`SOURCE_PRESENT` or a passing unit test is not operating-effectiveness evidence.

## Incident and vulnerability handling

Do not place secrets or exploitable details in public issues. Report suspected vulnerabilities
through the project maintainer's designated private channel, include a minimal reproduction,
and preserve versions, device/OS context and logs with sensitive data redacted.

Any remediation must bind the defect, affected version, patch, test result and retest result.
An unresolved or untested item stays `TOKEN_VAZIO` rather than being presented as closed.

## Review cadence

Security-relevant changes should be reviewed against their declared threat model and tested in
their actual deployment context. The cadence and scope of any review must be recorded in its
receipt; the existence of this document does not prove that a review occurred.

## Current boundary

```text
source exists != build verified != device executed != independent assessment
claim_allowed=false until the applicable evidence record is complete
```

