# Privacy design notice and evidence boundary

**Status:** `DESIGN_REFERENCE / TOKEN_VAZIO`  
**Claim gate:** `claim_allowed=false`

## Important scope note

This is an engineering design note, not a legal privacy notice, guarantee, or determination
of any jurisdictional obligation. It may help the project design data handling that is friendly
to applicable good practices, but legal applicability and operational effectiveness require a
scoped review.

For the repository-wide terminology rule, see
[CLAIM_LANGUAGE_POLICY.md](CLAIM_LANGUAGE_POLICY.md).

## Design questions

Before a data-handling feature is treated as ready for use, record:

- data categories and purpose;
- collection, storage, synchronization and deletion paths;
- user-facing controls and consent/authorization basis where applicable;
- retention and backup behavior;
- third-party processors and transfer locations;
- the exact app version and execution environment; and
- tests, failures and review evidence.

The absence of a recorded answer is `TOKEN_VAZIO`.

## Intended user-control directions

The codebase contains source-level components and plans related to settings, credential
handling, local data, export and deletion. Their presence does not demonstrate that a user can
exercise a right or that a complete data lifecycle has been verified on a released Android
build.

Any user-visible privacy statement must be reconciled with the exact shipped version, enabled
features, service providers and a qualified legal review. Do not infer outcomes from a class
name, a UI mock-up or a roadmap item.

## Verification route

```text
declared flow
  -> source mapping
  -> build + device execution
  -> data-path test and deletion/recovery test
  -> scoped legal/operational review where applicable
```

Until this route is documented for a defined scope, `claim_allowed=false`.

## Contact and reporting

Before publishing contact addresses or a public privacy notice, verify that the mailbox,
responsible party, scope and response process actually exist. Otherwise record them as
`TOKEN_VAZIO` rather than presenting them as an operating service.

