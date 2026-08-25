# RAFAELIA Vertical Slice V1 — RafGitTools control-plane adapter

**Canonical authority:** `rafaelmeloreisnovo/Mapa`, draft PR #95.  
**Circuit:** `INGEST → VALIDATE → EXECUTE → TEST → RECEIPT → DECIDE → INDEX`.  
**Policy:** dry-run by default, least privilege, append-only evidence, `claim_allowed=false`.

RafGitTools is responsible only for:

1. resolving the authority route for each operational record;
2. validating schema, source identity, falsifier and requested operation;
3. producing a dry-run plan before execution;
4. requiring explicit human authorization for irreversible actions;
5. passing work to RafPolimata or Termux without claiming their results;
6. returning receipt pointers and gaps to Mapa.

It must reject or preserve as `TOKEN_VAZIO`:

- missing source hash;
- missing falsifier;
- ambiguous authority;
- secret/private payload publication;
- mutation without authorization;
- automatic promotion from PASS to scientific or production claim.

## Minimal contract

```json
{
  "schema": "rafaelia_control_request_v1",
  "event_id": "RAFAELIA-VERTICAL-SLICE-V1-20260730T041200Z",
  "canonical_map_pr": 95,
  "operation": "ROUTE_AND_PREFLIGHT",
  "dry_run": true,
  "irreversible": false,
  "claim_allowed": false,
  "required_outputs": ["authority_route", "preflight", "gaps", "next_verifiable_step"]
}
```

This adapter does not duplicate the Mapa registry or the reference receipt.
