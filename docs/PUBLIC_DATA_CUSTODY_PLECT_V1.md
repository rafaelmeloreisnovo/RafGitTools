# Public Data Custody + PLECT Bridge V1

State: IMPLEMENTED_SOURCE / PUBLIC_ONLY / PRIVACY_MINIMIZED / claim_allowed=false

## Intent

Create a reproducible path from official public records to audit artifacts without turning publication into unrestricted republication of personal data and without turning statistical anomalies into accusations.

The capability is designed for three questions:

1. What did an official public source publish?
2. Can the public-money/logistics series be reconstructed and independently recomputed?
3. Does a pre-registered PLECT candidate beat simple baselines without violating privacy or claim boundaries?

## PLECT provenance boundary

The NOVOexport reconstruction currently supports two authorial meanings:

- **pre-intention precursor**: a state appearing before an observed action;
- **direction of money flow**: a request to track where movement is going rather than merely observe micro-oscillation.

A later assistant-generated expansion, “Permutation-Linked Correlated Topology”, is a derived candidate and is not treated as the canonical authorial definition.

The recurring corpus formula is recorded as source material:

```text
A(t) = sum_i(
  CLEMAX_i * PLECT_i * Tag14_i * Playmax_i *
  Cluster_i * SC_i * Formulas_i
) ^ Retroalimentacao(n)

Retroalimentacao(n) = 1 + log2(1+n)
```

This repository does **not** invent the missing canonical PLECT operator. Until that formula is authoritatively bound:

```text
plect_state = TOKEN_VAZIO_CANONICAL_OPERATOR
```

A first-difference direction is implemented only as a control/baseline for future falsification.

## Official sources

The initial configured lanes are:

- Portal da Transparência: aggregate expenses by agency;
- Portal da Transparência: aggregate civil-service population by agency;
- Portal bulk expense files for empenho → liquidação → pagamento;
- Portal bulk civil-service files, but public projection is aggregate-only;
- USAspending v2 base is registered for a later endpoint-specific contract.

The Portal API token is referenced only as `PORTAL_TRANSPARENCIA_API_KEY`. GitHub PATs are forbidden as fallback. Secret values are never printed or persisted.

## Privacy contract

Public artifacts exclude direct person fields such as name, CPF/NIS, personal contact/address, bank/account details and stable person identifiers.

For salary/remuneration analysis, the public lane uses aggregates such as:

```text
organization + role/function category + period
+ count + sum/mean/min/max
```

and suppresses groups below the configured minimum group size.

This is intentionally stricter than “the source was public”. Public availability does not remove the need for purpose limitation, necessity, data quality and protection against misuse.

The public repository implements **no deanonymization or reversible identity map**. Any lawful identity resolution for a concrete investigation remains outside this capability and must rely on an independent competent authority and its own legal process.

## Custody receipt

Each run records:

- official source alias and URL;
- retrieval time;
- source byte length + SHA-256;
- sanitized/aggregate projection byte length + SHA-256;
- query parameters;
- privacy mode;
- `raw_source_persisted=false`;
- `claim_allowed=false`;
- current PLECT state.

The raw API response is held only in process memory and is not written to the artifact directory by this capability.

## RLL branch presentation

`scripts/render_rll_branch_atlas.py` presents the RLL promotion topology as:

```text
WORK -> rll/lab -> rll/integration -> rll/release -> main
```

It is read-only and produces a time-stamped branch inventory. Branch presence is governance state, not scientific evidence.

## Falsifiers

The capability fails or abstains when:

- a source is not allowlisted;
- an API key is absent for a keyed source;
- redirect changes the official host;
- a GitHub PAT is proposed as public-data credential;
- person-level endpoint is not allowlisted;
- source response is not JSON for API lanes;
- privacy projection attempts to preserve blocked fields;
- a salary group is below the minimum disclosure size;
- a baseline is mislabeled as canonical PLECT.

## Legal/ethical boundary

```text
ANOMALY != WRONGDOING
SALARY != SUSPICION
CORRELATION != CAUSALITY
PUBLIC_RECORD != UNRESTRICTED_PERSONAL_REPUBLICATION
MISSING_RECORD != ZERO
```

The purpose is auditability, reproducibility and governance. It is not a system for accusing, profiling, harassing, or privately identifying people.

## R3

F_ok = public-source allowlist + dedicated secret boundary + source/projection hashes + aggregate salary privacy + PLECT provenance separation + RLL branch atlas.

F_gap = Portal API key is external account state; USAspending endpoint-specific contract is not yet bound; real-source CI run and historical public-money joins are not yet evidenced; canonical PLECT operator remains TOKEN_VAZIO.

F_next = execute one aggregate Portal source in CI, archive only its sanitized receipt/projection, then cross-check empenho/liquidação/pagamento from bulk data before introducing a PLECT benchmark.
