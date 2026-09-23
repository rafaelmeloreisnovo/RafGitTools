# RafCode Route ↔ Federation Bridge V1

State: `IMPLEMENTED_SOURCE / CI_PENDING`

This is a freestanding adapter between two already-versioned low-level contracts:

```text
raf_route_receipt (32 B)
→ raf_route_fed_digest()
→ route_digest[4]
→ raf_fed_work (64 B)
→ raf_fed_validate()
```

It does **not** alter either wire ABI. It does not parse JSON, open files, access the network, allocate memory or call Android APIs.

The bridge accepts only a successful V1 route receipt with a valid magic/version, route code in R0001..R0010, one-to-one V1 trigger mapping and a nonzero structural route tag. Invalid receipts produce a zero digest and an explicit error mask.

The generated federation route digest is domain-separated and guaranteed to have a nonzero XOR fold because `rafcode_federation_v1` currently uses that fold as its bounded route-identity presence gate.

## Evidence boundary

```text
STRUCTURAL_DIGEST != CRYPTOGRAPHIC_IDENTITY
ROUTE_RECEIPT != SOURCE_TRUTH
FEDERATION_VALIDATION != EXECUTION
CI_OBJECT_PASS != PHYSICAL_ARM_DEVICE_PASS
```

The bridge test exercises the chain from a valid route request through `raf_route_resolve`, digest binding and `raf_fed_validate`.
