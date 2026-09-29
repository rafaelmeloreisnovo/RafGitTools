# Private CI Bridge V1 — public control plane, private source

**State:** `IMPLEMENTED_UNTESTED_PRIVATE_REPLAY_V1`  
**Authority:** RafGitTools executes; Mapa describes the route; each private repository owns its private replay manifest.  
**Claim boundary:** `SOURCE != EXECUTION != EVIDENCE != CLAIM`.

## Purpose

Run selected CI-equivalent checks for private repositories from the public RafGitTools Actions control plane when the private repository's own GitHub Actions execution is unavailable, without publishing private source or credential values.

The bridge does **not** make a private workflow public and does **not** blindly interpret arbitrary YAML. A public allowlist is intersected with a manifest stored inside the private repository. The private manifest contains replayable command vectors as argv arrays; no shell string is accepted.

## V1 route

```text
human workflow_dispatch
  -> RafGitTools START.yml
  -> public execution registry
  -> exact private target SHA
  -> PAT_ACTIONS access/checkout only
  -> private manifest validation
  -> secretless argv subprocess replay
  -> hash-only sanitized receipt
  -> public Actions artifact
```

### Security boundary

1. `PAT_ACTIONS` is referenced only by the provider access and checkout steps.
2. It is **not** a job-wide environment variable.
3. Before private commands run, the executor rejects provider-token variables if any is present in its process environment.
4. Private commands run via `subprocess.run(argv, shell=False)`.
5. Raw private stdout/stderr are hashed in memory and discarded. The public receipt stores only byte counts, SHA-256 digests, return codes and explicitly declared artifact hashes.
6. Private source is never uploaded as an artifact.
7. Only an exact lowercase 40-hex commit SHA is accepted.
8. Public registry permission and private manifest permission must both agree on the workflow id.
9. CWD and artifact paths are constrained to the checked-out source root.
10. Secret-like environment keys are rejected from private replay steps.

This protects the provider credential and now also wraps replayed commands in a Linux user+network namespace. The executor runs a fail-closed network probe before private steps; if the namespace cannot block outbound IPv4 TCP, replay does not start. This does not claim protection against kernel-level escape or host compromise.

## Why not just execute the YAML?

GitHub workflow YAML contains provider semantics: `uses:` actions, matrices, secrets, permissions, services, environments and provider expressions. Replaying it as a generic shell file would erase those boundaries and could execute more than intended.

V1 therefore uses the YAML as a provenance pointer and the private manifest as the explicit replay contract. A workflow is promoted to replayable only after its core commands are mapped.

## Public registry

`configs/private-ci/execution-registry.v1.json` contains target id, repository locator, private manifest path, allowed workflow ids, allowed executable names, state and invariants, plus the **name** of the required credential slot — never its value.

Repository/workflow names are metadata, not credentials. If a future target name itself must remain confidential, add an opaque locator mode; do not invent a custom cipher.

## Private repository manifest

Canonical private path:

```text
.rafaelia/private-ci/manifest.v1.json
```

Each workflow record carries the original `.github/workflows/*.yml` provenance path, replay `steps[]` as argv arrays, bounded `cwd`, bounded timeout, non-secret environment values and optional artifact paths to **hash**, not publish.

The private repository remains authority for its source and replay mapping.

## Receipt

Public receipt schema: `rafgittools.private-ci-replay-receipt.v1`.

It records exact target commit, original workflow path/hash, manifest hash, runner identity, command-vector hashes, exit codes and hash-only artifact evidence. It explicitly records that credential values, private source and raw private stdout/stderr are not persisted publicly.

## ZIPRAF relation

ZIPRAF is suitable as a **container / preservation / integrity envelope** for receipts, but it is not treated as encryption merely because the extension is `.zipraf`.

V1 intentionally does not reuse a PAT as an HMAC/signing key and does not invent a new encryption primitive. A future `PRIVATE_CI_RECEIPT.zipraf` profile may package the sanitized receipt plus hashes and an external signature. The signing/private key must remain outside the container.

Current state:

```text
ZIPRAF_RECEIPT_PROFILE = TOKEN_VAZIO_NOT_IMPLEMENTED
CONFIDENTIAL_RAW_LOG_ENVELOPE = TOKEN_VAZIO_NOT_IMPLEMENTED
EXTERNAL_SIGNATURE_KEY = TOKEN_VAZIO_NOT_CONFIGURED
```

## Rollback

The bridge is additive. Rollback consists of removing the `private_ci` START lane and these new registry/script/test files. Existing `provider_actions` behavior remains independently recoverable.

## R3

`F_ok`: bounded registry + secretless replay executor + hash-only receipt contract are implemented on the delta branch.

`F_gap`: target-private manifest merge, the first real private replay receipt and ZIPRAF signed receipt profile are not yet proven. Network namespace isolation is implemented and awaits the fresh public runner gate/replay evidence.

`F_next`: merge the private manifest for one target, then run one exact-SHA manual replay and promote only that workflow from `IMPLEMENTED_UNTESTED` to `PASS` if its receipt closes all required gates.
