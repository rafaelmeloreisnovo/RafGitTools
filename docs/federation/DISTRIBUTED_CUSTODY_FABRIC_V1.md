# RAFAELIA Distributed Custody Fabric V1

**State:** `IMPLEMENTED_UNTESTED`  
**Control plane:** `rafaelmeloreisnovo/RafGitTools`  
**Successor scope:** extends federation routing with custody/reconstruction semantics; it does not supersede repository-local authority.

## 1. Operational model

The phone/device is an **edge buffer and bounded executor**, not the durable source of truth.

Durable state is reconstructed from independently addressable planes:

1. **Git/GitHub source-control plane** — commit/tree/blob, branch, patch, PR, workflow and artifact lineage.
2. **Google Drive documentary-custody plane** — indexes, manifests, receipts, CURRENT_STATE, NOVOexport and cross-plane pointers.
3. **Execution/evidence plane** — actual local/CI/device execution observations, outputs, exit state and receipt hashes.
4. **Restricted fragment domain** — owner-controlled key/reconstruction binding that is never copied into public/shared Git, Drive documentation or logs.

The fourth element is deliberately asymmetric with the other three: externally visible records contain only enough information to bind and audit the event, not enough to reconstruct protected state by themselves.

## 2. Core invariant

```text
EDGE BUFFER
    |
    +--> GIT/GITHUB --------+
    |                       |
    +--> DRIVE -------------+--> CROSS-PLANE READBACK --> EVIDENCE/GAP
    |                       |
    +--> EXECUTION ---------+
                            |
                 RESTRICTED FRAGMENT
                 (resolved only by authorized domain)
```

No single external plane is authoritative for the whole protected state.

`SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM` remains mandatory.

## 3. What travels between planes

A custody event carries **references and commitments**, not unrestricted copies:

```text
event_id
producer
source_ref
parent_ref
patch_or_delta_ref
before_digest
after_digest
drive_receipt_ref
execution_ref
evidence_ref
key_slot_ref
commitment_digest
privacy_class
authority
state
gap
rollback_ref
successor_ref
```

The restricted value itself is not a field in the envelope.

## 4. Reconstruction rule

A protected reconstruction is allowed only when the required refs agree:

```text
source-control lineage
AND documentary-custody linkage
AND execution/evidence linkage when execution is claimed
AND authorized restricted-fragment resolution
```

If a required ref is absent, the state is `TOKEN_VAZIO` or `BLOCKED`.
If two observed digests disagree, the state is `CONTRADICTION`.
A successful source or CI gate cannot promote a device, scientific or provider claim without the corresponding evidence bridge.

## 5. Edge-buffer rule

Device-local processing may ingest, transform and emit bounded results, but durable operations must externalize a typed receipt to at least the authority plane required by the operation.

The device must not become an implicit single point of custody simply because it performed the transformation.

This permits high-volume processing where the handset keeps only the active working set while the reconstructible lineage remains distributed across durable stores.

## 6. Restricted fragment rule

The restricted fragment may resolve through an Android/platform secure keystore, an authorized provider secret store, or an owner-controlled offline secret domain.

The repository stores only an opaque `key_slot_ref` and non-secret commitment metadata. No plaintext key, recovery phrase, token or complete encrypted-secret package is required in public/shared documentation.

Cryptographic algorithm, signature scheme and rotation policy remain `TOKEN_VAZIO` until a concrete implementation is selected, reviewed and tested. This contract therefore does **not** claim that a cryptographic mechanism is already implemented.

## 7. Privacy and ethics by design

Every producer must bind:

- purpose;
- minimum necessary data;
- authority;
- privacy class;
- retention/rollback rule;
- evidence boundary;
- redaction rule for public/shared receipts.

A private repository may emit a minimal verifiable projection: exact digest, opaque locator, gate state and receipt reference without publishing the protected source.

## 8. Compliance boundary

This architecture is a control framework for traceability, minimization, accountability and reproducible audit. It is **not**, by itself, a legal certification.

Dataset-specific legal basis, controller/operator roles, retention periods and disclosure obligations remain separately bound evidence. Unknown values remain `TOKEN_VAZIO`.

## 9. Relationship to current federation

`configs/ecosystem-federation.v1.json` continues to define repository-local roles. This contract adds a second dimension: **where each event is durably anchored and how the same event can be reconstructed across planes without merging authorities**.

Examples already fitting the model include:

- `Rafaelia_Private` as restricted evidence/promotion custody;
- `GAIA_phi` as deterministic data/index custody;
- `RafPolimata` as semantic/governance authority;
- `Vectras-VM-Android` and Termux surfaces as execution/runtime producers;
- `RafGitTools` as federated control and dispatch plane.

Additional repositories such as Userland, PCR/audio/TTS or other specialized producers must not be guessed into the registry. They join through an exact repository identity plus local authority contract and rollback anchor.

## 10. Minimal gate

The V1 validator checks structural safety only:

- required planes exist;
- edge is non-authoritative;
- no secret material is allowed in the envelope;
- the restricted fragment is not materialized in repository/Drive/logs;
- reconstruction cannot be satisfied by one external plane;
- `claim_allowed=false`;
- cryptographic implementation gaps remain explicit rather than inferred.

A structural PASS is not an execution or cryptographic PASS.

## R3

`F_ok`: custody topology and asymmetric reconstruction boundary are materialized as a machine-readable successor contract.  
`F_gap`: concrete cryptographic implementation, key lifecycle, Drive write/readback integration and end-to-end cross-plane execution are not yet run.  
`F_next`: validate the contract, bind one real custody envelope to a non-sensitive canary event, then verify Git/Drive/execution readback before any wider rollout.
