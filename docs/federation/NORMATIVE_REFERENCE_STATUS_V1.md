# Normative Reference Status v1

Status: `REFERENCE_ONLY / claim_allowed=false`  
Observed: 2026-08-03T00:35:24-03:00

The registry records which official editions were observed and why they are relevant. It does not assert implementation, conformity, audit or certification.

| Reference | Observed state | Boundary |
|---|---|---|
| ISO 9000:2026 | published current vocabulary | not certifiable by itself |
| ISO 9001:2015 + Amd 1:2024 | current requirements; replacement expected September 2026 | future edition is not yet the current requirement |
| ISO/IEC 27001:2022 + Amd 1:2024 | published ISMS requirements | mapping is not certification |
| ISO/IEC 27002:2022 | published control guidance | guidance itself is not certifiable |
| IEEE 1012-2024 | active; supersedes 1012-2016 | reference requires project-scoped V&V evidence |
| IEEE 730-2026 | active page; official title still says approved draft; supersedes 730-2014 | retain the exact authority wording |
| ISO/IEC/IEEE 29148:2018 | requirements-engineering reference | no repo conformance claimed |
| W3C PROV-O | structural provenance mapping | mapping is not formal conformance |

Machine source: `configs/normative-reference-registry.v1.json`.

Validation:

```bash
python3 scripts/federation/validate_normative_reference_registry.py
python3 -m unittest tests/federation/test_normative_reference_registry.py -v
```

Every entry keeps `conformance_claim=false`, `certification_claim=false` and `implementation_receipt=TOKEN_VAZIO` until scoped evidence exists.
