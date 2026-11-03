# RAFAELIA Ω — Driver Map V1

State: `SCOPED_PROTOTYPE / HOLD_REMOTE_EXECUTION`; `claim_allowed=false`.

This mobile-friendly, dependency-free screen lives in the existing `LAB_RAPIDTOOLS_OMEGA` rather than creating another runtime or competing Android app. Open `index.html` as a local file or in a reviewed static host. Choose a driver, choose a Drive account alias, load a local `rafaelia.cross-source-record/v1` JSONL/JSON file, view nodes and typed edges, inspect L9, and export a metadata-only request. It makes **no network requests** and contains **no tokens/credentials**.

## Current provider-side implementation

The producer source for the SQL is the separate `rafaelmeloreisnovo/supabase-Rafcodephi` review branch `review/driver-map-l9-20261103`, file `rafcodephi/governance/sql/mapa_driver_l9_v1.DRAFT.sql`. It creates private tables:
- `raf_private.mapa_driver_catalog_v1` — `driver_pk uuid PRIMARY KEY`, versioned driver key, readiness.
- `raf_private.mapa_driver_projection_v1` — `projection_pk uuid PRIMARY KEY`, unique driver/source/record, L9 fields, provenance and graph degrees.
- `raf_private.mapa_driver_run_receipts_v1` — `run_pk uuid PRIMARY KEY`, append-only provider execution receipts.

Each table has **one** primary key. These new UUID keys do not replace the composite primary keys in existing source registry and edges tables.

Only the preloaded, checked source mirror is `READY_SCOPED`. The private `SECURITY INVOKER` function `raf_private.populate_mapa_driver_v1(driver_key,expected_blob_sha)` rejects unknown/blocked drivers, source-blob mismatches, or absent registry source. It projects only already-approved provider rows; no arbitrary SQL, dynamic DDL, private Drive download, or cross-database replication. Its function EXECUTE is not granted to `anon` or `authenticated`, and the tables have forced RLS and no client grants.

## Driver states

| Key | State | Source / next gate |
| --- | --- | --- |
| `mapa_github_jsonl` | READY_SCOPED | existing private registry mirror, exact provider blob |
| `google_drive_official` | BLOCKED_AUTH | scoped OAuth/owner file permission and executor |
| `supabase_rafcodephi` | BLOCKED_REVIEW | anti-loop contract and cross-project mapping |
| `geolm_q16` | BLOCKED_NO_GEOMETRY | geospatial features/coordinates and adapter |

GEOLM is an existing geometric on-device module in `Livro/geolm.txt`; it is **not** an established GIS source. Empty latitude/longitude is an explicit typed unknown, not a zero coordinate.

## Start and continuation

The owner-private Google Drive root entry is named `001_START_HERE_RAFAELIA_DRIVER_MAP_OMEGA_V1`. It routes to the provider's CURRENT_STATE and NEXUS μWRITE without copying private details to this public repository.

Sequence: `CURRENT_STATE → DRIVER → SOURCE → VALIDATE → PREVIEW → PRIVILEGED_EXECUTION → PROVIDER_READBACK → μWRITE → R3`. This HTML screen implements the **first five steps only**. It cannot make the final three happen without an authenticated, reviewed executor. The connected ChatGPT Supabase provider can execute the function after gates. Do not embed a Supabase service-role key or Google Drive OAuth token into client JavaScript.

## Evidence boundary

Observed in provider: 12 preexisting registry nodes, 32 typed relations; one scoped driver populate stored 12 L9 projection rows and a provider receipt. Replayed inside `BEGIN...ROLLBACK` inserted 0 new projections. Unknown driver and wrong blob failed closed. Source HTML was read back from GitHub and its JavaScript parsed successfully; live mobile browser E2E remains `NOT_RUN`. Historical PG15 dumps and Storage were not imported. Backups, API-level negative tests, independent review, and credential-scoped multi-provider runner remain open gates.

Rollback: leave review branch unmerged; no source removal. Provider migration rollback needs an independent data-safe migration and backup, not deletion of custody history.
