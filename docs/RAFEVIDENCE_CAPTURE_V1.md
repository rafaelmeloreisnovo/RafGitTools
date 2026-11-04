# RAFEVIDENCE v1 — consented local evidence custody for RafGitTools

**Producer:** `rafaelmeloreisnovo/RafGitTools`; contract sibling: `docs/HUMAN_AI_BIVALENT_PRIVACY_MIDDLEWARE_V1.md`.

**Scope:** user-owned, voluntarily exported/captured **metadata**, never private browser/app instrumentation. Not a monitor of ChatGPT, not a network interceptor, not a recovery tool for platform-internal data. Missing or invisible tool records are `NOT_VISIBLE`, **not** proof of deletion, exfiltration or misappropriation.

## Minimal chain

`OWNER_ATTESTS -> MINIMIZED_JSON -> STRICT_VALIDATION -> LOCAL_SHA256_RECEIPT -> PRIVATE_APPEND_ONLY_LEDGER -> HUMAN_REVIEW -> OPTIONAL_SEPARATE_PROVIDER_READBACK -> EXTERNAL_CUSTODY_GATE`

The `source_sha256` describes a capture file the owner has locally already hashed. This program **does not** open that capture, read its contents, or independently verify the digest. It accepts a metadata claim as attestation, not provider evidence. No auto-uploads, background tasks, telemetry, remote fetches, Frida injection, HTTP browser hooks, credentials or cookies.

`SHA256 chain != authenticated timestamp != digital signature != WORM storage`. A person with write access can rewrite a SHA-only chain. Tampering after a normal append is detected when the next append verifies the existing chain; independent offline signing/third-party timestamps need separate authorization.

## Minimal usage (Termux/Unix, Python stdlib)

Create `request.json` **locally** containing this strictly permitted schema (fixture digest only):

```json
{
  "schema":"rafevidence.capture.v1",
  "source_kind":"USER_SCREENSHOT",
  "source_ref":"local:capture",
  "source_sha256":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
  "observed_at":"2026-11-04T19:37:00-03:00",
  "operation":"UI_TOOL_ARGS",
  "capture_state":"UI_OBSERVED",
  "classification":"PRIVATE_METADATA",
  "owner_attested":true,
  "contains_child_data":false,
  "contains_sensitive_data":false,
  "contains_secrets":false,
  "claim_allowed":false
}
```

`python3 scripts/rafevidence_capture_v1.py --input request.json` is a read-only dry run. `python3 scripts/rafevidence_capture_v1.py --input request.json --ledger ./private-ledger.jsonl --commit` appends one receipt with owner-only file mode. Do not commit the ledger or request containing real data to Git. Validate with `python3 -m unittest discover -s tests -p 'test_rafevidence_capture_v1.py'`.

## Privacy, rights and interoperability

- Children, sensitive data, secrets and uncertain classifications are **blocked** even as hashes until an independent human-reviewed privacy/rights gate exists. No child profiles or surveillance. Always prioritize dignity and the best interests of children.
- Source refs are a restricted `local:capture`, `github:owner/repo@40hex`, or `drive:<file-id>`; query strings, bearer URLs and local file paths are forbidden. Raw text, JSON tool payloads, cookies and tokens cannot enter a valid receipt.
- Metadata classification `PRIVATE_METADATA` is still sensitive to linkage and must not be published; use a private, access-reviewed destination. SHA hashes are fingerprints and can potentially correlate records.
- Supabase: `raf_private` is a possible **private custody destination** via authorized administrative workflow. It must not expose `service_role` credentials to Android clients or accept untrusted write claims as verified facts. Migration/API endpoint and Android integration are NOT implemented in v1.
- Drive: documentary pointer/receipt only after access and visibility review. GitHub: producer code/tests only. RLL: reads authorized evidence links; no automatic cross-project claims.
- Future RafBrowser/RafGitTools Android wrapper should use the Android system document picker + minimal local permissions; Frida only in an app controlled and legally instrumented by its operator, never third-party session interception.
- Policy mapping is **alignment targets, not certification**: LGPD and ANPD child best-interest guidance; Brazilian digital child protections; NIST Privacy Framework; ISO/IEC 27001, ISO 8000, ISO 9001, ISO/IEC 42001, W3C accessibility standards, and relevant IETF RFCs require independent jurisdiction-specific reviews.
- Human rights, indigenous cultures, language, accessibility and environmental impact are review dimensions: avoid profiling protected traits, provide accessible interfaces, language choice and a non-digital/manual evidence route. No organization named here endorses this prototype.

## Acceptance/falsifiers

- Unknown fields, missing owner attestation, child/sensitive/secret flags, invalid references, unversioned schema, public writable ledger, chain corruption => fail closed.
- Duplicate canonical observation => `DUPLICATE_NO_WRITE`; tampered chain => `BLOCKED`.
- Network bytes transmitted by this script must be **zero**; verify with independent runtime observation if required. Code inspection suggests none; no Android/network runtime audit completed.
- `CLAIM_ALLOWED_FALSE` is invariant. **Success of unit tests proves only narrow Python logic**, not Android, provider internals, privacy certification, cross-device reliability, provider delivery, or data loss.
- Rollback: keep draft branch isolated, remove local test-only ledger by owner decision (not a production deletion), and add compensating successor record to immutable governance logs rather than erasing custody history.

**R3:** F_ok=local gated receipt; F_gap=actual Android/Frida/APK, signed WORM custody, consent UX, external provider gate, cultural consultation; F_next=independent security review + authorized on-device data-picker prototype.
