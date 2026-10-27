# Repository Privacy Manager — V2 (owner/visibility UI extension)

RafGitTools has a **Raf Privacy** launcher surface for authenticated inventory and fail-closed repository visibility changes. The new UI offers a company/organization/account dropdown (derived from authenticated repository owners) and a **Private / Public** target dropdown. The target defaults to **Private** for backward compatibility. Selecting a new owner or target clears prior checkbox selections.

## V3 — organization checklist + explicit origin/destination (scoped candidate)

The top of **Raf Privacy** now has four separate choices:

1. **Accounts/organizations (checkbox multi-select)**: the dropdown lists the authenticated owner set, with **All**, **None**, and independent owner checkboxes. The default is All. An empty selection displays zero repositories and cannot execute a mutation.
2. **Source visibility (De)**: Public or Private.
3. **Target visibility (Para)**: Private or Public. Source and target are always opposite. Changing either recomputes the other; an invalid same-state transformation is not representable.
4. **Repository selection**: independent repo checkboxes, owner-section bulk checkboxes, and "select eligible" restricted to the active owner selection and target.

Changing the owner scope or either direction clears previously selected repository IDs. A confirmation dialog takes the selected IDs only after re-filtering by the current owner scope and eligible target. The ViewModel repeats that check before passing the immutable candidate list to the existing manager. The manager continues live identity/permission preflight and durable receipt checkpoints before any GitHub PATCH.

**Source ≠ runtime.** The changes in branch `feat/privacy-multi-owner-source-target-20261027` are an additive UI/ViewModel extension; the GitHub API and authorization/mutation kernel are unchanged. New isolated JVM test *source* covers owner multi-selection, empty/unknown organization behavior, scoped selection, and symmetric source-target consistency. Tests, Android build, physical UI and authenticated canary must be observed on the exact candidate commit before any promotion claim. No visibility changes are performed by committing source.

Rollback: leave the new branch unmerged; if merged only after human review, revert the source commit in a separate governed change. Any actual GitHub visibility mutation requires its own explicit reverse action and fresh receipt, not merely code rollback.

## Inventory

The feature calls `GET /user/repos` with:

- `visibility=all`
- `affiliation=owner,organization_member`
- `per_page=100` with full pagination

This intentionally includes repositories owned by the authenticated personal account and repositories reachable through organization membership while excluding collaborator-only repositories.

Pagination has a defensive upper bound. Exceeding it is an explicit failure; partial inventory is never silently presented as complete.

## Eligibility gate

A repository is selectable for a target only when all conditions hold:

1. its confirmed visibility differs from the selected target (`public` or `private`);
2. it is not a fork;
3. it is not archived or disabled;
4. GitHub explicitly reports `permissions.admin=true`;
5. `private` and `visibility` fields agree; unknown or `internal` visibility is blocked (TOKEN_VAZIO).

The owner dropdown filters the already-loaded repository inventory; it does not grant access to a new organization. The app only lists owners present in GitHub's authenticated response. Selecting all is scoped to the active owner filter and visibility target.

Missing admin evidence is represented as `TOKEN_VAZIO`, never guessed.

## Live preflight / TOCTOU protection

The inventory shown to the user is not sufficient authority for a destructive mutation. Immediately before every PATCH the app calls:

```text
GET /repos/{owner}/{repo}
```

and revalidates:

- repository numeric identity (`id`) still matches the selected object;
- repository is still in the opposite visibility state required for the selected target;
- repository is still not a fork;
- repository is still not archived/disabled;
- GitHub still reports `permissions.admin=true`;
- repository ID, owner and full name match the selected object;
- the response has consistent, supported visibility fields.

If any invariant changed between selection and execution, that repository is skipped fail-closed and no PATCH is sent.

## Mutation

The app sends only:

```json
{"visibility":"private"}
```

or, exclusively after the user selects **Public** and passes separate confirmation:

```json
{"visibility":"public"}
```

to `PATCH /repos/{owner}/{repo}`. Mutations run sequentially. A `401` aborts the remaining queue; `403`, `404`, `422` and other HTTP/runtime failures are recorded with sanitized messages.

For fine-grained PATs, the token must have repository administration write authority. Organization policy can independently restrict visibility changes to organization owners.

## Confirmation and consequences

Before mutation the user must type `PRIVATIZAR N` or `PUBLICAR N` exactly, depending on the selected target. The UI warns that public-to-private changes can affect public social metadata, forks, Pages and availability. **Private-to-public exposes the repository's files, commit history and potentially sensitive data to the internet**, so users must independently review secrets, licenses and personal information. No mutation occurs from selection, preview or code merge alone.

## Provenance journal — V2

The mutation path uses `RAFGITTOOLS_REPOSITORY_PRIVACY_RECEIPT_V2`.

The journal is **append-only** and stored under Android app-private storage. Every checkpoint receives a unique operation ID + sequence number and is written via `AtomicFile` with `flush` + `fsync` before publication.

Critical invariant:

```text
no durable PLANNED receipt -> zero GitHub PATCH requests
```

Before every external PATCH, the selected repository is recorded as `ATTEMPTING` in a durable checkpoint. If the process/device stops after GitHub accepts the PATCH but before the local result is recorded, that durable `ATTEMPTING` state identifies the exact repository requiring reconciliation.

After each result another checkpoint is attempted. If provenance persistence fails, remaining mutations abort rather than continuing without an audit trail.

Receipt content is deliberately minimized:

- operation/checkpoint identity;
- repository numeric ID + `owner/name`;
- previous and target visibility;
- mutation status;
- sanitized HTTP/status message.

It contains **no PAT/OAuth token, Authorization header, SSH key, repository file contents or secret material**.

## Provenance states

- `DURABLE` — checkpoints remained durable;
- `DURABLE_RECOVERED` — a transient checkpoint failed but a final complete checkpoint was later persisted;
- `FAILED_BEFORE_MUTATION` — initial journal could not be created; zero mutations attempted;
- `PARTIAL_DURABILITY_LOSS` — latest durable checkpoint must be used for reconciliation and remaining mutations were stopped.

## Evidence boundary

Source implementation and newly added target-path tests are in branch `feat/privacy-owner-visibility-select-20261027`. They are not considered executed until a CI exact-head run and Android device/instrumentation evidence exist. Build/device execution remain separate evidence gates. No repository visibility is changed by CI or by merely merging this code; execution still requires authenticated user selection and explicit confirmation on the Android device.

## Rollback / release gate

- Feature is isolated in a branch; do not auto-merge or auto-install an APK.
- No remote visibility mutation occurs during unit tests or builds (test API is mocked).
- Before promotion: confirm Android build, compose preview/instrumentation for both dropdowns, exact-head unit tests, authority/SSO checks, and a manual canary on a non-sensitive test repository.
- Reverting this source feature does **not** automatically reverse a repository visibility change already accepted by GitHub; treat those as separately authorized mutations with separate receipts.


## Hotfix Wizard V4 — outubro de 2026

Raf Privacy agora opera como um assistente de **3 passos** (não uma planilha de controles):
1. **Conta ou organização:** escolha uma conta no menu ou todas. O inventário GitHub é carregado automaticamente.
2. **Privacidade desejada:** selecione **Privado** ou **Público** uma única vez; o estado de origem é deduzido automaticamente como o oposto.
3. **Prévia automática:** todos os repositórios elegíveis do escopo entram no plano sem clicar em checkboxes; são exibidos total, nomes, bloqueios e riscos. Exceções individuais permanecem opcionais, ocultas por padrão.

A execução continua **somente após** a ação explícita "Revisar e confirmar" e "Sim, tornar privado/público". A frase técnica é montada internamente depois do clique; o usuário não precisa digitá-la, mas o `ViewModel` valida os IDs e o destino do plano que foi revisado. O `RepositoryPrivacyManager` mantém os bloqueios de fork/arquivado/sem admin, preflight **live** por repositório, journal/recibo durável antes do PATCH e falha fechada. A seleção automática é **preparação**, jamais aplicação silenciosa.

**YAML/Android:** o START único chama validação da matriz de Kotlin/KSP/Compose/MockK e o novo guard Hilt/Gradle/lint antes dos jobs Android e CodeQL Kotlin. Hilt runtime/processor voltaram ao **2.48**, alinhados ao plugin já definido no `build.gradle`, removendo a incompatibilidade observada em Hilt 2.60.1 com Kotlin 1.9.24. `abortOnError true` faz erros do lint reprovarem, sem esconder warnings (`warningsAsErrors false`).

**Provas requeridas:** `testDevDebugUnitTest`, `assembleDevDebugAndroidTest`, `lintDevDebug`, `assembleDevDebug`, CodeQL Kotlin e recibo final no **HEAD exato**, mais validação física Android. Testes dos guards são negativos e independentes da rede; não inferir compilação ou deployment de sucesso antes do CI. `CLAIM_ALLOWED=false`; sem auto-merge, sem alteração de visibilidade remota durante CI. Reversão: um novo commit revertendo apenas este hotfix, preservando trilha de evidências.
