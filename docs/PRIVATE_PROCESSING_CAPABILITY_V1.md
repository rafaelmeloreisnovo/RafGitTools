# RafGitTools Private Processing Capability V1

State: IMPLEMENTED_SOURCE / DEVICE_E2E_NOT_RUN / SECRET_LANE_TOKEN_VAZIO
claim_allowed: false

## Intent

Close the existing Drive → private Android staging → catalog → private GitHub custody route without copying raw corpus bytes into RafGitTools or any public repository.

## Existing route reused

1. Android Storage Access Framework opens the user-selected provider document.
2. `stageDriveDocument` streams the entire selected object into app-private storage and computes SHA-256.
3. `DriveStagingGate` writes the local staging receipt.
4. `CorpusIntakeGate` validates readback SHA-256 and performs bounded structural JSON scanning.
5. `PrivateProcessingReceiptGateV1` creates a sanitized activity receipt.
6. `GithubDataRepository.createPrivateProcessingFile` performs live target readback.
7. The write is rejected unless the target is private at that moment.
8. Only the `memory_bridge/private_processing/` namespace is writable through this method.

## Activity receipt

The receipt records:
- source provider authority, never a local Android private path;
- full selected-stream SHA-256 and bytes read;
- intake ID and content type;
- structural vector when applicable;
- Android SDK and ABI list;
- opaque SHA-256 of the private destination repository identity;
- explicit gaps;
- `rawPayloadUploaded=false`;
- `claimAllowed=false`.

The raw file is not sent by this capability.

## Two authorization lanes

### ANDROID_EXPLICIT_USER

Uses the GitHub session already authenticated in the app. A write is an explicit user action. The repository metadata is re-read live before the contents API mutation. This lane does not require a repository Actions secret.

State: `IMPLEMENTED_SOURCE_PENDING_DEVICE`.

### SECRET_AUTOMATION

Reserved for future non-interactive orchestration. It has a dedicated capability name and must use its own least-privilege secret, `PAT_PRIVATE_PROCESSING`.

It must not fall back to:
- `PAT_ENV`;
- `PAT_ACTIONS`;
- `PAT_AGENTS`;
- `PAT_CODESPACES`;
- `PAT_DEPENDABOT`.

The currently available connector cannot provision GitHub secrets, therefore this lane remains:

`TOKEN_VAZIO_UNWIRED_SECRET`

No value is read, printed or persisted.

## Provider/privacy boundary

Public RafGitTools files use aliases only:
- `GOOGLE_DRIVE_NOVOEXPORT_ACTIVE`;
- `PRIVATE_MEMORY_BRIDGE`.

Exact Drive IDs and the exact private destination repository are maintained only in private custody material. This prevents the public control plane from becoming a directory of private provider identities.

## Failure semantics

- non-private target → FAIL before write;
- output path outside the private-processing namespace → FAIL;
- artifact > 512 KiB → FAIL;
- receipt containing a `raw_payload` field → FAIL;
- invalid SHA-256 → FAIL;
- missing physical device run → NOT_RUN, never PASS;
- missing automation secret → TOKEN_VAZIO, never fallback.

## R3

F_ok = Drive full-stream staging + SHA-256 + corpus structural gate + private receipt model + private target live-readback + constrained GitHub contents write are source-materialized.

F_gap = Android APK/build hash is not yet bound into the activity receipt; no physical device E2E publication has been observed for this delta; the dedicated automation secret is not provisioned.

F_next = CI the branch, then on the Android device choose one NOVOexport JSON through Drive/SAF and publish one activity receipt to the private memory bridge; verify returned commit SHA and compare the receipt source hash with local staging evidence.
