# RAFAELIA Ω — typed TOKEN_VAZIO bridge / structural HOLD V1

Authority: `Mapa` federated routing; `RafGitTools` control-plane executor.
Producer ownership is not transferred. The private RLL research repo, Supabase
database and private Drive docs remain in their original authorities.

## Minimal navigation

- Machine manifest: `configs/omega-cross-provider-gap-bridge.v1.json`
- Structural validator: `scripts/omega_typed_gap_gate_v1.py`
- Falsifiers: `tests/test_omega_typed_gap_gate_v1.py`
- Current Supabase candidate: `rafaelmeloreisnovo/supabase-Rafcodephi` PR #6 (draft/HOLD).
- Current historical custody is in private Drive START HERE V2.2 / HOTSTATE;
  the public manifest deliberately contains no private Drive document IDs.

## Non-compensatory rules

1. Literal `NULL`, missing property, empty string, four spaces, numeric zero,
   boolean false, symbolic `∅` and typed `TOKEN_VAZIO` are distinct.
2. User-supplied Ω00..Ω15 are **test vectors**. No glyph, translation, name,
   homonym, origin or anecdote is scientific/historical/provider evidence.
3. No raw Google Drive payload, Supabase user row, child/health record, PAT
   value, credential hash or credential length goes into this public bridge.
4. `VERIFIED_SCOPED` cross-repo edges need producer AND consumer receipts and
   exact SHAs. Even then `claim_allowed=false` until independent domain gates.
5. PAT identifiers are names, NOT permission readbacks; RLL credential plane
   differs from RafGitTools. Do not cross-wire secrets/fallback.
6. `PROPOSED` graph edges are routes, not running synchronization. DDL is
   reviewed and executed by the producer via a versioned migration, not by an
   untrusted row trigger. PG15 predecessor databases are not altered.
7. Append a successor receipt rather than overwrite historical records. If
   independent gate evidence is absent, keep typed gap, falsifier, owner,
   stop condition and rollback.

## Scoped acceptance

Run the validator and unit tests (stdlib Python 3; offline). A pass proves
only that this metadata graph satisfies structural invariants. It does not
prove cross-provider auth, RLL cosmology, runtime database integration, GitHub
ruleset protection, legal authority, or independent scientific reproduction.

Rollback: close the unmerged review PR; the base branch is unchanged.
Stop: do not merge, deploy, synchronize or ingest until exact-head CI,
independent review, source/consumer receipts, minimal lawful data scope,
provider permissions and required RLS/negative authorization gates are met.

R3 = <typed gap grammar and scoped falsifiers, unknown provider/consumer
execution, provider readback and independent domain gate>.
