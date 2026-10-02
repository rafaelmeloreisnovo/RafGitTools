# RAFAELIA Ecosystem Operational Hotfix — 2026-09-30

Canonical machine-readable state for this hotfix:
`configs/ecosystem-operational-state.v2.json`.

This file is a federation router. Repository-local contracts remain authoritative.

## Materialized

The hotfix lineage contains the five-member freestanding registry/validator, seven
repository member manifests, per-repository receipts, the shared-component registry
for `raf_bl0.c`, the path-independent RafGitTools gates, the RafPolimata closure
fix, and the Vectras ARM32 policy fix.

## Errors, urgency and current handling

| Scope | State | Urgency | Handling |
|---|---|---:|---|
| Rafaelia_Private CI | BLOCKED_EXTERNAL | P1 | no speculative source patch; runner steps/logs unavailable |
| GAIA_phi CI | BLOCKED_EXTERNAL | P1 | same fail-closed handling; source cause not proven |
| RafPolimata TOKEN_VAZIO gate | **PASS_HOTFIX_VERIFIED** | closed P0 | CLOSURE_L11 binding verified by CI run 36705649532 |
| Vectras APK Wizard ARM32 | **PASS_HOTFIX_VERIFIED** | closed P0 | APK Wizard run 36705651907 passed after arm32-debug parity fix |
| Termux provider protection | FAIL / EXTERNAL_ADMIN | P0 | apply live ruleset admin delta, then rerun 10_PROVIDER |
| termux-packages D3-D8 | TOKEN_VAZIO | P1 | execute D3 before any later gate |
| raf_bl0 authority | AUDIT / TOKEN_VAZIO | P1 | establish producer provenance before deduplication |

## Logic and evidence boundary

```text
SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM
TOKEN_VAZIO != 0
IMPLEMENTED_UNTESTED != PASS
RELATIONSHIP != AUTHORITY_MERGE
```

A failed job with `steps=[]` and unavailable logs is evidence of a provider/runner
failure state, not evidence of a source defect.

A green host gate does not prove ARM, Android or device execution.

## Confirmed hotfixes

### RafPolimata — verified PASS

CI run `36705649532`, job `109855143660`, completed successfully. The `Validate TOKEN_VAZIO gates (Hotfix H1)` step is now PASS, together with the remaining exposed CI steps.

The prior CI failure at step `Validate TOKEN_VAZIO gates (Hotfix H1)` was causal to two
new federation files. The new references are now bound to
`CLOSURE_L11_OPERATIONAL_GAP_TOPOLOGY`.

The earlier `SOURCE_CONTRACT_FAIL` lines in the log belong to the one-bit mutation
rejection test; those source-contract steps actually passed and are not classified
as a repository defect.

### Vectras — verified PASS

APK Wizard run `36705651907`, job `109855151297`, completed successfully, including bootstrap contract checks, shell-loader smoke, all APK wizard lanes and artifact upload.

`tools/ci/build_apk_wizard.sh`, the root/app ABI registry and the Moto E7 profile
already use `arm32-debug`. `terminal-emulator/build.gradle` had not implemented
that governed case and rejected it. The hotfix adds:

- exact `armeabi-v7a` requirement;
- `CI_INTERNAL_VALIDATION=true` requirement;
- no release/store promotion.

Physical Moto E7 execution remains a separate gate.

## Provider protection

The live ruleset was read again on 2026-09-30:

- ruleset ID: `21908888`;
- `updated_at=2026-09-26T20:05:43.907-03:00`;
- no `required_status_checks` rule;
- PR policy still differs from target v3;
- always-bypass actors: `RepositoryRole:5`, `Integration:20150`,
  `29110`, `73253`, `1144995`.

The existing administrative delta is therefore still version-current, not stale.
The available connector can read but cannot mutate rulesets, so
`apply_state=BLOCKED_EXTERNAL_ADMIN_AUTHORITY`.

## Rollback

The first seven federation PRs were closed without merge. That preserves each default
branch as a clean rollback anchor.

Hotfix-specific rollback:

- RafPolimata: revert `64ad3a6c` and `1d747af1`.
- Vectras: revert `c0a997aa` and `d24bd32c`.
- Provider: no live mutation was made, so there is nothing to revert yet.
- Federation: close the successor PR or revert only its repository-local commits.

Never perform a cross-repository history rewrite as a federation rollback.

## Next gate sequence

```text
RafPolimata closure rerun
-> Vectras APK Wizard rerun
-> provider administration + 10_PROVIDER receipt
-> termux-packages D3 .deb
-> D4 APT metadata
-> D5 bootstrap
-> D6 installed prefix + bash
-> D7 apt/pkg transaction
-> D8 ARM32 physical receipt
-> D8 ARM64 physical receipt
```

`claim_allowed=false` until the specific claim's complete gate path is satisfied.


## Executable federation verification

When all seven repository roots are available locally, run `scripts/federation/validate_ecosystem_federation_v2.py` with one `--root owner/repo=/path` per member. It verifies the exact seven-member set, local authority manifests, successor receipts, rollback presence, `claim_allowed=false`, and the Git blob identity of each declared `raf_bl0.c` consumer.

This validator intentionally fails when a repository root is absent; partial availability is not silently promoted to full federation PASS.
