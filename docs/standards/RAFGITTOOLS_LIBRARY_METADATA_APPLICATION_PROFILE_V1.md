# RafGit Tools — Library Metadata Application Profile V1

Status: ALIGNMENT_PROFILE / NOT_CERTIFICATION / claim_allowed=false

## Purpose

This profile makes the RafGit Tools library model interoperable with established library and digital-preservation concepts while keeping the RAFAELIA evidence/custody boundaries explicit.

It is an application profile, not a claim of formal compliance, certification, cataloging accreditation, or complete MARC/RDA implementation.

## Conceptual bibliographic layer

The canonical conceptual chain is:

```text
Work
 -> Expression
 -> Manifestation
 -> Item
```

RafGit Tools retains `LibraryEditionRecord` only as a local convenience projection because project artifacts commonly use labels such as V1/V2/release/draft. Edition does not replace Expression or Manifestation.

### Mapping

| RafGit Tools | Conceptual role |
|---|---|
| LibraryWorkRecord | Work |
| LibraryExpressionRecord | Expression |
| LibraryManifestationRecord | Manifestation |
| LibraryItemRecord | Item/exemplar |
| LibraryAuthorityRecord | controlled agent/name/term authority |
| LibraryRelationRecord | typed relationship |
| shelf/collection/section/session | discovery/holdings projection, not identity |

## Descriptive metadata profile

DCMI-style concepts are used as a lightweight cross-system vocabulary:

| Local field | DCMI-style concept |
|---|---|
| preferredTitle / displayName | title |
| authorityRefs | creator/contributor relation |
| subjectRefs | subject |
| languageTags | language |
| mediaType / formatLabel | format |
| source IDs / hashes | identifier / source |
| predecessor/supersedes | isVersionOf / replaces |
| access class | accessRights |
| evidence/provenance refs | provenance / relation |

This is a semantic crosswalk, not a claim that every DCMI constraint is implemented.

## MARC 21 role

MARC 21 is treated as an interchange/export target, not as the internal storage model.

Future exporters may map:
- bibliographic record;
- authority record;
- holdings/location projection;
- classification.

No `MARC_COMPLIANT` state exists until exact field/indicator/subfield mappings and round-trip tests are implemented.

## Preservation metadata

Digital preservation is modeled separately from descriptive cataloging.

`LibraryPreservationEventRecord` records:
- event type;
- object reference;
- time;
- outcome;
- responsible agent refs;
- evidence refs.

`LibraryRightsRecord` records:
- object;
- rights/policy basis;
- access statement;
- retention rule;
- evidence state.

This supports the preservation pattern:
```text
Object <-> Event <-> Agent / Rights
```

Hashing, migration, validation, quarantine, restore and supersession can therefore be recorded as preservation events rather than silently changing item metadata.

## Provenance boundary

```text
catalog record != source object
shelf location != bibliographic identity
hash equality != semantic equivalence
metadata crosswalk != standards certification
preservation event != legal authorization
```

## Version pinning

The application profile records the external family/version used for design review. Because standards evolve, mappings are append-only and versioned; a future update creates a successor profile rather than rewriting the old profile.

## Current gaps

- full RDA element mapping: TOKEN_VAZIO / not implemented;
- MARC 21 field/indicator/subfield exporter: NOT_IMPLEMENTED;
- MARC authority/holdings/classification round-trip: NOT_IMPLEMENTED;
- linked-data RDF serialization: NOT_IMPLEMENTED;
- controlled subject scheme selection: TOKEN_VAZIO;
- identifier registries such as DOI/ISBN/ORCID are optional external authorities and must not be inferred.
