# RAFGITTOOLS_CURRENT_STATE

- Status: **ATIVO — source-functional avançado + BUILD verificado / DEVICE ainda gated**
- Estado observado: **2026-08-14**
- Branch observada: `hardening/first-compile-run-triangle-20260814`
- Commit executado: `bbdb556a59c06a23cc2f6df6ba0ae7c98466a4fa`
- PR de reconciliação: **#346 (draft, mergeable na observação)**
- Base `main` na observação: `d62bb58f33624ecad888f86e9f95e33deb2f91be`
- Fonte de verdade operacional: `app/src/` + testes + contratos + `ECOSYSTEM_RUNTIME_STATE.json` + workflow/receipts observados.
- Claim boundary: **`claim_allowed=false`** e **`release_allowed=false`** até o smoke físico do mesmo APK/commit e gates posteriores de release.

## Regra canônica de evidência

```text
ideia != fonte integrada != teste executado != APK produzido != device executado != release
```

Roadmap, README histórico ou contagem de features **não podem rebaixar nem promover** estado técnico sem reconciliação com fonte e evidência executável.

## Evidência executável atual

### GitHub Actions — Android Client Build

- run: `31821491676`
- job: `94835531838`
- conclusão: `success`
- runner alocado e executado: sim
- Java 17: PASS
- Android SDK: PASS
- custody/structural tests: PASS
- authentication unit tests: PASS
- full dev unit tests: PASS
- Android lint: PASS
- `assembleDevDebug`: PASS
- APK verification/build receipt: PASS
- artifact upload: PASS

O estado histórico `BLOCKED_INFRA_BILLING` deixa de descrever o corte atual: houve execução completa e bem-sucedida para o commit acima. Falhas antigas permanecem história, não fonte de verdade atual.

### Artefato observado

- Actions artifact: `RafGitTools-devDebug`
- artifact id: `9227343409`
- archive digest GitHub: `sha256:2f92034fc4a4a1c9242453798c8eae6e1d68b134e8ff39b46ea4c283c976eb09`
- APK interno: `app-dev-debug.apk`
- APK size: `24,672,130` bytes
- APK SHA-256: `115b9cb1e71f53f16b2648924a09549b8e5e0b9e453280cab2e7f183a411ebf6`
- ZIP CRC: PASS
- `armeabi-v7a`: PRESENT
- `arm64-v8a`: PRESENT
- dual ABI gate: PASS
- build receipt schema: `rafgittools.android-build-receipt.v1`
- build receipt SHA-256: `f124ac18a9f1e158aa764a12b49a25dbf54cc870cca8359e0355416bee5219a5`

### Triângulo compile-run

```text
SOURCE  = PASS  (commit exato)
BUILD   = PASS  (tests + lint + APK + SHA + ABI + receipt)
DEVICE  = TOKEN_VAZIO_PHYSICAL_DEVICE_REQUIRED
RELEASE = BLOCKED_BY_DEVICE_AND_RELEASE_GATES
```

O fechamento válido continua exigindo que **os mesmos bytes** do APK sejam instalados/iniciados no aparelho físico e o runtime receipt revalide commit + APK SHA-256.

## Núcleo Android / Git / GitHub

| Componente | Estado atual | Evidência / limite |
|---|---|---|
| Android / Compose / Hilt / Room | `IMPLEMENTED_ADVANCED + BUILD_VERIFIED` | full unit tests, lint e assemble PASS; device pendente |
| P33 roadmap L1 | `33/33 SOURCE_FUNCTIONAL` | fonte completa; runtime por feature continua granular |
| PAT + armazenamento seguro | `IMPLEMENTED + TESTS_EXECUTED` | authentication tests PASS; device regression pendente |
| Token lifecycle P33-25 | `IMPLEMENTED / CAPABILITY_AWARE + TESTS_EXECUTED` | testes executados; refresh real GitHub App exige configuração/fixture |
| OAuth Device Flow | `IMPLEMENTED / CONFIG_REQUIRED` | Client ID real e device flow real ainda necessários |
| GitHub privado HTTPS | `IMPLEMENTED / RUNTIME_GATED` | fixture privada real pendente |
| Force-with-lease | `IMPLEMENTED / RUNTIME_GATED` | contrato fail-closed em fonte; remote fixture pendente |
| Interactive hunk staging | `IMPLEMENTED + UNIT_TESTS_EXECUTED / DEVICE_GATED` | regressão unitária executada; smoke Android pendente |
| API GitHub | `PARTIAL_ADVANCED / RUNTIME_GATED` | fonte extensa; falta matriz end-to-end real |
| Git local via JGit | `IMPLEMENTED_ADVANCED + BUILD_VERIFIED / RUNTIME_GATED` | fonte compilou/testou; rede/credenciais/conflitos reais pedem regressão |
| SSH auth | `PARTIAL / RUNTIME_GATED` | chave/servidor reais pendentes |
| GPG | `ADAPTER_IMPLEMENTED / TOKEN_VAZIO_RUNTIME` | exige `gpg` autorizado e fixture |
| Git LFS | `IMPLEMENTED / TOKEN_VAZIO_RUNTIME` | exige `git-lfs` + repositório real |
| Worktree | `IMPLEMENTED / TOKEN_VAZIO_RUNTIME` | matriz filesystem/device pendente |
| Bisect | `IMPLEMENTED / TOKEN_VAZIO_RUNTIME` | cenário regressivo controlado pendente |
| Terminal | `BOUNDED_EXECUTOR` | deliberadamente não é PTY/VT100 |
| Multi-provider | `IMPLEMENTED / FIXTURE_GATED` | GitLab/Bitbucket/Gitea-Forgejo/Azure DevOps em fonte; credenciais/fixtures reais pendentes |
| Offline queue | `IMPLEMENTED / DEVICE_GATED` | durability em fonte; restart/recovery físico pendente |
| rafaelia JNI | `BRIDGE_IMPLEMENTED + BUILD_VERIFIED` | camada nativa participa do build; sem promover kernel experimental |
| LLaMA kernel JNI | `BRIDGE_IMPLEMENTED / BLOCKED_EXTERNAL` | `llama.h`/runtime externo ainda não pinado/provado |

## Correções de documentação deste corte

1. `GitHub Actions = BLOCKED_INFRA_BILLING` era estado histórico; **o run 31821491676 executou e passou**.
2. `APK devDebug = TOKEN_VAZIO` era histórico; agora há APK hash-bound e dual ABI verificado no BUILD.
3. `Multi-provider = STUB_TYPED` está obsoleto para a fonte atual; adapters estão implementados, embora fixtures reais permaneçam pendentes.
4. `OfflineQueue codec/WorkManager pendentes` não representa a fonte atual; existem armazenamento durável/workers, com evidence física ainda aberta.
5. P33 permanece `33/33 SOURCE_FUNCTIONAL`, sem confundir isso com `33/33 DEVICE_VERIFIED`.
6. Percentuais antigos do roadmap permanecem **baseline de planejamento**, não medição automática do estado atual do código.

## Autenticação — contrato canônico

- Access token cifrado com Android Keystore/AES-GCM.
- Refresh token, quando fornecido por Device Flow, usa alias separado.
- `savePat()` elimina refresh state incompatível de sessão anterior.
- `clearAuthState()` remove access/refresh/expiries/identidade de sessão.
- PAT/OAuth sem refresh capability: 401 -> fail-closed -> reautenticação.
- GitHub App Device Flow com refresh capability: rotação serializada, sem `client_secret`, retry único.
- 403 rate-limit não é confundido com invalid credential.

A regressão de autenticação do head observado foi executada com sucesso no run `31821491676`; **serviços reais/credenciais reais continuam um gate separado**.

## Interactive staging — boundary

P33-05 permanece limitado a tracked `MODIFY`, UTF-8 <= 2 MiB e newline final. Stage/unstage é index-only, revalida hunk/HEAD/index e falha fechado para inputs fora do contrato. O corte de 2026-08-14 também corrigiu a regressão `MissingObjectException` mantendo scan + format no mesmo `DiffFormatter`; os unit tests completos do `devDebug` passaram.

## Fonte de verdade ordenada

1. commit/branch exatos observados;
2. código compilado em `app/src/`;
3. testes e lint efetivamente executados;
4. APK + SHA-256 + ABI + build receipt;
5. `ECOSYSTEM_RUNTIME_STATE.json`;
6. receipts de device quando existirem;
7. documentos de estado atual;
8. roadmap e documentação histórica.

## TOKEN_VAZIO preservado

```text
physical-device install/start       = TOKEN_VAZIO_PHYSICAL_DEVICE_REQUIRED
physical runtime receipt same APK   = TOKEN_VAZIO
private Git remote fixtures         = TOKEN_VAZIO_RUNTIME
PAT/OAuth real device regression    = TOKEN_VAZIO_RUNTIME
GitHub App refresh real             = CONFIG_REQUIRED / TOKEN_VAZIO_RUNTIME
SSH provider matrix                 = TOKEN_VAZIO_RUNTIME
GPG/LFS/worktree/bisect fixtures    = TOKEN_VAZIO_RUNTIME
release signing/release acceptance  = TOKEN_VAZIO_RELEASE
claim_allowed                       = false
release_allowed                     = false
```

## Próximo gate

Executar o APK de SHA-256 `115b9cb1e71f53f16b2648924a09549b8e5e0b9e453280cab2e7f183a411ebf6` em aparelho físico ARM suportado e produzir runtime receipt que vincule:

```text
commit bbdb556a59c06a23cc2f6df6ba0ae7c98466a4fa
+ APK SHA-256
+ package/application id observado
+ ABI/device
+ install result
+ launch result
+ timestamp
```

Somente depois fechar `triangle_closure=PASS`.

## Retroalimentar[4] — 2026-08-14

- **F_ok:** source + testes + lint + APK + SHA + dual ABI + build receipt estão materialmente provados no candidato.
- **F_gap:** device físico, fixtures externas e release continuam sem prova.
- **F_next:** fechar DEVICE usando exatamente o APK hash-bound; depois integrar #346 sem regredir a cadeia de custódia.
