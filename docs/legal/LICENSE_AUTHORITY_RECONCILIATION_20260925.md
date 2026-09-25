# RafGitTools License Authority Reconciliation — 2026-09-25

**state:** `DOCUMENTATION_CORRECTION / GPL_AUTHORITY_PRESERVED`  
**claim_allowed:** false  
**observed producer head:** `b0c395087d4de2be66cf9458a0df090da83dad75`

## Observed license surfaces

### Root `LICENSE`

Blob: `f0433f024df053a0e11a5b8eac35a9d73da99b70`

This is the repository's conventional GNU GPL v3-or-later license surface.

### Root `LICENSE.md`

Blob: `7cf9196dbc645504ebb8e86a89386bd9cd5e95b8`

Despite its filename, the observed file is a C source artifact
(`core_rafaelia_matriz_supralegal.c`) wrapped in extensive symbolic/legal
commentary. It contains statements about "supralegal" authority, automatic
acceptance, sanctions and other assertions that are not the repository's GPL
license text.

Therefore:

```text
ROOT_LICENSE_GPL3 != LICENSE_MD_C_ARTIFACT
FILENAME != LEGAL_AUTHORITY
SYMBOLIC_LEGAL_ASSERTION != ENFORCEABILITY
```

## Current routing rule

For covered RafGitTools program distribution:

```text
root LICENSE / applicable file-level notices = controlling license route
LICENSE.md                                    = historical/code artifact
```

This correction does not delete the historical artifact and does not decide
whether every line/file is GPL-covered. File-level and third-party notices
still require their own inventory.

## Relationship to RAFAELIA v1000

No noncommercial restriction from RAFAELIA v1000 is added to GPL-covered
RafGitTools code through this documentation.

New material that is genuinely independent/separate may have a separate license
only after compatibility and distribution-boundary review.

## Required cleanup

1. rename or archive the misleading `LICENSE.md` artifact under a provenance
   preserving successor rather than treating it as a license;
2. retain the original blob/history;
3. inventory file-level SPDX/third-party licenses;
4. ensure README/legal navigation points to root GPL license for covered code;
5. keep symbolic/legal prose separate from executable source and actual
   contractual instruments.

## Invariants

```text
GPL_COVERED_CODE != V1000_RESTRICTED_CODE
LICENSE_FILENAME != LICENSE_AUTHORITY_BY_ITSELF
CODE_COMMENT != CONTRACT
SYMBOLIC_SANCTION != AUTOMATIC_LEGAL_REMEDY
```

## R3

**F_ok:** root GPL authority and misleading secondary artifact are separated.

**F_gap:** file-level inventory and final placement/name of the historical
artifact remain open.

**F_next:** provenance-preserving relocation/renaming plus anti-regression
license navigation test.
