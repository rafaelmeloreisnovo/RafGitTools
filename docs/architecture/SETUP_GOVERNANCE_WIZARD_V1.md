# Setup Governance Wizard V1

State: **IMPLEMENTED_SOURCE / UNMERGED / RUNTIME_NOT_YET_PROVEN**  
Owner: `rafaelmeloreisnovo/RafGitTools`  
Claim gate: `claim_allowed=false`

## Purpose

Provide a post-install configuration surface that is comfortable, clear and reversible where possible. The user sees material information before making each decision. There is no separate fine-print layer for material risk.

Every step shows:

- what the option enables;
- risks and limits;
- which data is touched;
- rollback boundary;
- Zero Trust rule;
- explicit choice: **Concordo**, **Não concordo**, or **Decidir depois**.

## Safety boundary

A wizard decision records **intent/configuration only**. It does not itself execute a privileged provider operation.

`INTENT != EXECUTION != EVIDENCE != CLAIM`.

Privileged operations remain behind their own capability-specific gate, target binding, provider readback and receipt.

## Privacy

The wizard stores only local decision metadata in app-private storage.

It never writes PATs, access tokens, passwords or provider secret values to the custody ledger.

The custody ledger is an append-only JSONL hash chain under:

`filesDir/setup_wizard/custody.jsonl`

Corrections and local rollback are new events; prior records are not rewritten.

## Screen order

1. Welcome
2. Privacy and data governance
3. Identity and access
4. External modules
5. Read/write boundary
6. Audit and custody
7. Automation
8. Review and rollback
9. Final review

## Rollback model

Local wizard preferences can be reset from the final screen.

This does **not** pretend to revert remote provider changes. Remote rollback must be separately declared by the owning capability and must have an observable result/readback.

## Entry point

Dedicated `SetupWizardActivity`, isolated from the main navigation graph. The first implementation is intentionally callable as its own application surface and by `rafgittools://setup` so users can review it again later.

Automatic first-launch forcing is deferred until this isolated flow has build/runtime evidence.

## Accessibility and clarity

The UI uses normal headline/body typography for material information. Risk, data use and rollback are primary cards, not secondary notes. No material consent is hidden behind a link or collapsed disclosure.

## F_next

Compile and test the source, inspect the APK activity entry, launch the wizard on a device/emulator, and only then consider making it the automatic first-launch experience.
