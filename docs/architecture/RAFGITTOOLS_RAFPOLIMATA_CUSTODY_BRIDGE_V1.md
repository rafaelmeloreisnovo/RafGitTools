# RafGitTools ↔ RafPolimata Custody Bridge V1

Status: `IMPLEMENTED_UNTESTED` until both repository gates execute.

## Purpose

RafGitTools remains the orchestration / custody-envelope producer. RafPolimata consumes the envelope for validation, uncertainty classification and evidence routing. Neither repository may silently promote the other's observation into a stronger authority class.

Core invariants:

- `SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM`
- `TOKEN_VAZIO != 0`
- `IMPLEMENTED_UNTESTED != PASS`
- `producer_receipt != consumer_validation`
- `capability_name != secret_value`

## Bridge sequence

`AUTHORIZED_INTENT -> RafGitTools custody event -> bridge envelope -> RafPolimata validation -> consumer receipt -> provider/runtime readback when applicable -> superseding receipt`

The bridge envelope carries source/artifact/execution/evidence references separately. `claimAllowed` is always `false` in V1.

## Secret boundary

GitHub PAT values are never materialized in the bridge. Only non-sensitive display labels are allowed. Display labels use the canonical presentation rule requested for operator readability:

`upper(first_character) + lower(remaining_characters)`

Examples:

- `pat_eNvir -> Pat_envir`
- `PAT_ACTIONS -> Pat_actions`
- `PAT_CODESPACE -> Pat_codespace`

This is presentation only. It does not rename provider Secrets or mutate credential identifiers.

## Append-only lineage

`predecessorReceipt` points to the previous receipt when known. `supersedesReceipt` identifies the state being explicitly superseded. `TOKEN_VAZIO` is valid when lineage is genuinely unknown; it must not be converted to zero or omitted.

## Authority boundary

RafGitTools: orchestration, repository/provider routing, custody envelope generation.

RafPolimata: validation, gap classification, uncertainty reduction, consumer-side receipt.

External provider/runtime/device: execution/readback authority for its own state.

## Files

- `contracts/rafpolimata-custody-bridge-v1.schema.json`
- `scripts/emit_rafpolimata_custody_bridge.py`
- `tests/test_rafpolimata_custody_bridge.py`

RafPolimata implements the complementary consumer validator under its own repository authority.
