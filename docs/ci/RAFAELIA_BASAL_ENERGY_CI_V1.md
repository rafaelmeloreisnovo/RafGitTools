# RAFAELIA Ω — CI basal energy V1 (draft / HOLD)

Status: `IMPLEMENTED_UNTESTED` until exact-HEAD GitHub Actions checks finish. This document is a source contract, **not** permission to merge or to skip a required check.

Authority: repository-local `AGENTS.md` and `START.yml`; Mapa routes federated state. The producer owns its CI; a GitHub receipt is not independent scientific, Android-device, or provider-approval evidence.

## Objective and measured baseline

Avoid redundant Gradle bootstrap and unnecessary simultaneous dependency-update requests without reducing the required gates.

| Dimension | Baseline source | Scoped proposed delta | Post-change measured runtime |
| --- | --- | --- | --- |
| Post-APK Android Gradle invocations | Three separate invocations: `testDevDebugUnitTest`, `assembleDevDebugAndroidTest`, `lintDevDebug` | One invocation with all three tasks (3 → 1, two JVM/Gradle starts eliminated per executed Android lane) | `TOKEN_VAZIO_NOT_MEASURED` |
| Dependabot version review windows | Gradle Monday 09:00 and Actions Monday 09:00 America/Sao_Paulo | Gradle Monday 09:00; Actions Tuesday 11:00; 5-day version-only cooldown | `TOKEN_VAZIO_NOT_MEASURED` |
| Scheduled full CI / CodeQL / Android builds | Existing START triggers and conditional gates | **Unchanged** until verified impact-based reuse exists | No false saving claimed |

**Savings are structural, not yet measured in minutes, joules, quota, or cost.** A single Gradle command shares task graph and local incremental inputs but must execute all applicable Gradle tasks; any nonzero exit code blocks the entire Android lane. APK compile, APK byte digest, DEX/ABI checks, artifact publication order, native gates, and negative tests are unchanged.

## Reuse eligibility (future, not enabled here)

A previous gate receipt may be considered as a *candidate* only if all the following are independently verified:

1. **Producer authority:** actual GitHub workflow run and job conclusion from GitHub API, immutable artifact identity, verified upload digest, source repo, target branch, and trusted runner scope. User-provided `PASS` or SHA text is not authority.
2. **Source closure:** source file hashes, referenced imports, tool scripts, matrix/configuration, dependency lockfiles, action SHA, toolchain tuple, runner OS/architecture, build variables, privacy/license-policy epoch, and all upstream gate outcomes identical for the specific gate.
3. **Time and revocation:** receipt not expired, superseded, revoked, permission-invalidated, or derived from untrusted cache/artifact. `TOKEN_VAZIO` in any mandatory dimension forces execution.
4. **Consumer readback:** consumer verifies the actual provider artifact and signed/attested producer context, logs consumption via append-only receipt, checks the exact dependent outputs, and preserves failure diagnostics.
5. **Provider/status requirement:** universally required START jobs still execute and report GitHub check conclusions for the new commit; a previous job status cannot be fabricated or transplanted to another commit. No skip of CodeQL, security, privacy, release, or Android/device evidence without distinct reviewed gates and positive/negative proof.

Pseudo-contract, not an executable authority:

```text
GATE_KEY = SHA256(canonical_json({
  gate_id, source_closure_digest, toolchain_digest, workflow_digest,
  environment_digest, policy_digest, dependency_receipts
}))
eligible = (key_equal AND authenticated_producer AND verified_artifact
            AND not_expired AND not_revoked AND consumer_readback)
if not eligible: execute_gate()
# eligible != permission to mark the new GitHub check successful.
```

### Invalidation matrix

| Change | Invalidates |
| --- | --- |
| Kotlin, Gradle, AGP, KSP, Hilt, Room, JDK, wrapper | Android unit, instrumented APK, lint, Kotlin CodeQL, applicable ABI gates |
| GitHub Actions SHA, workflow YAML, CI script, permission/policy | CI topology, coherence, relevant CodeQL and CI receipt |
| Production Kotlin/Java source or dependencies | Android and Kotlin CodeQL; security checks remain active |
| CodeQL init/analyze digest, queries or extraction config | Both paired CodeQL stages; no mixed-version reuse |
| Source-custody, privacy, license or federation contract | All affected dependent gates and receipts |
| Docs only, with no runtime or governance dependencies | May follow existing narrow draft route, but required universal START gates still run |
| Unknown dependency relationship or expired producer evidence | `TOKEN_VAZIO_UNSAFE_REUSE` ⇒ rerun |

## Existing controls deliberately preserved

- START continues to own global topology, exact source checkout, and fail-closed final receipt; no new active workflow or broader provider permission.
- All Kotlin/JDK17/dual-ABI requirements remain unchanged. No device-test claim from CI-only compilation.
- Both CodeQL `init` and `analyze` remain pinned to the **same 40-character SHA**; security scans and CodeQL SARIF uploads remain enabled.
- Dependabot stays review-only; groups are restricted to version-updates and minor/patch for grouped changes. Major changes and security alerts are not suppressed by cooldown.
- Dependabot automatic rebase remains enabled: disabling it risks aging already-open dependency PRs and creating manual conflict work.
- No `pull_request_target`, no auto-merge, no GitHub ruleset modification, no force push, no deletions, no public sensitive receipt or private corpus export.

## Falsifiers and exit criteria

1. Test the exact source workflow ordering: APK verified before batch; batch contains exactly the three required Gradle tasks; no `continue-on-error` or bypass.
2. Test Dependabot schedules are disjoint; version-only cooldown exists on both ecosystems; security groups/alerts are not ignored.
3. Test the CodeQL `init` and `analyze` digests are identical and no conditional stage is configured as a universal provider-required status.
4. Observe exact-HEAD START jobs, Android build, Python tests, CodeQL and final receipt; distinguish skip due to scoped lane from runtime PASS. Record failed jobs and negative results.
5. Compare end-to-end CI durations against a matched prior source/runner case; otherwise `ENERGY_SAVING_OBSERVED=TOKEN_VAZIO`.

Rollback: revert the isolated source commits in the DRAFT branch (append successor, no rewrite), or restore the preceding source files at `main@a07efb3fe4def800a61514fd255c66cbb4243e31`; no provider or production state was changed by this proposal.

R3 = ⟨F_ok: bounded structural optimization proposed; F_gap: exact-HEAD CI, quantified saving, authenticated cross-run receipt reuse not proved; F_next: verify CI and measure, then authorize per-gate reuse with negative tests⟩.
