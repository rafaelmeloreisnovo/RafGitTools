# RafGitTools — Relatório de Status

**Data:** 2026-08-14  
**Estado geral:** 🟡 **source-functional avançado + BUILD verificável; DEVICE físico ainda pendente**  
**Branch observada:** `hardening/first-compile-run-triangle-20260814`  
**Commit executado:** `bbdb556a59c06a23cc2f6df6ba0ae7c98466a4fa`  
**PR:** #346 (draft na observação)  
**Matriz executável:** `ECOSYSTEM_RUNTIME_STATE.json`

## Regra de evidência

```text
arquivo existente
!= código integrado
!= teste executado
!= APK gerado
!= runtime em aparelho
!= release
```

Esta regra permanece válida. O que mudou em 2026-08-14 é que os estágios **TEST/BUILD deixaram de ser TOKEN_VAZIO** para o commit observado.

## Execução observada

GitHub Actions `Android Client Build`:

- run `31821491676` — `success`;
- job `94835531838` — `Test, lint and build devDebug APK` — `success`;
- custody/structural tests — PASS;
- authentication unit tests — PASS;
- full dev unit tests — PASS;
- Android lint — PASS;
- `assembleDevDebug` — PASS;
- APK verification + build receipt — PASS;
- artifact upload — PASS.

Portanto, o antigo estado `BLOCKED_INFRA_BILLING` permanece apenas como histórico de runs anteriores e **não descreve o corte atual**.

## Artefato BUILD

| Campo | Valor observado |
|---|---|
| Actions artifact | `RafGitTools-devDebug` |
| Artifact id | `9227343409` |
| Archive digest | `sha256:2f92034fc4a4a1c9242453798c8eae6e1d68b134e8ff39b46ea4c283c976eb09` |
| APK | `app-dev-debug.apk` |
| APK bytes | `24,672,130` |
| APK SHA-256 | `115b9cb1e71f53f16b2648924a09549b8e5e0b9e453280cab2e7f183a411ebf6` |
| ZIP CRC | PASS |
| `armeabi-v7a` | PRESENT |
| `arm64-v8a` | PRESENT |
| build receipt schema | `rafgittools.android-build-receipt.v1` |
| build receipt SHA-256 | `f124ac18a9f1e158aa764a12b49a25dbf54cc870cca8359e0355416bee5219a5` |
| physical runtime | `TOKEN_VAZIO_PHYSICAL_DEVICE_REQUIRED` |
| `claim_allowed` | `false` |
| `release_allowed` | `false` |

## Classificação técnica atual

| Componente | Status | Evidência/limite |
|---|---|---|
| Android + Compose + Hilt + Room | `IMPLEMENTED_ADVANCED / BUILD_VERIFIED` | unit tests + lint + assemble PASS; device pendente |
| P33 L1 | `33/33 SOURCE_FUNCTIONAL` | não equivale a 33/33 runtime |
| Login PAT | `IMPLEMENTED / TESTS_EXECUTED` | regressão física ainda necessária |
| Token lifecycle | `IMPLEMENTED / TESTS_EXECUTED / CONFIG_GATED` | rotação real GitHub App requer Client ID/fixture |
| OAuth Device Flow | `IMPLEMENTED / CONFIG_REQUIRED` | exige Client ID público real |
| Importação `gh` / Termux | `IMPLEMENTED / RUNTIME_GATED` | prova no device/Termux pendente |
| SSH | `PARTIAL / RUNTIME_GATED` | matriz real chave/servidor pendente |
| API GitHub | `PARTIAL_ADVANCED / RUNTIME_GATED` | falta matriz E2E completa |
| Git local via JGit | `IMPLEMENTED_ADVANCED / BUILD_VERIFIED / RUNTIME_GATED` | fonte compilada/testada; remote/conflict fixtures faltam |
| Interactive staging | `IMPLEMENTED / TESTS_EXECUTED / DEVICE_GATED` | unit regression PASS; smoke Android pendente |
| UI GitHub/Git | `PARTIAL_ADVANCED` | telas/fluxos existem; device E2E ainda necessário |
| Fila offline | `IMPLEMENTED / DEVICE_GATED` | storage/workers presentes; restart/recovery físico pendente |
| Multi-provider | `IMPLEMENTED / FIXTURE_GATED` | GitLab/Bitbucket/Gitea-Forgejo/Azure DevOps requerem fixtures reais |
| Git LFS | `IMPLEMENTED / TOKEN_VAZIO_RUNTIME` | exige `git-lfs` + repositório real |
| Worktree | `IMPLEMENTED / TOKEN_VAZIO_RUNTIME` | filesystem/device matrix pendente |
| Bisect | `IMPLEMENTED / TOKEN_VAZIO_RUNTIME` | regressão controlada pendente |
| GPG | `ADAPTER_IMPLEMENTED / TOKEN_VAZIO_RUNTIME` | exige binário/fixture autorizados |
| Terminal | `BOUNDED_EXECUTOR` | por contrato não é PTY/VT100 |
| rafaelia JNI bridge | `BRIDGE_IMPLEMENTED / BUILD_VERIFIED` | participa do build atual; kernel experimental não promovido |
| LLaMA kernel JNI | `BRIDGE_IMPLEMENTED / BLOCKED_EXTERNAL` | headers/runtime externos não pinados/provados |
| APK verificável | `BUILD_VERIFIED` | hash/ABI/receipt observados |
| DEVICE físico | `TOKEN_VAZIO` | mesmos bytes ainda não instalados/iniciados com receipt |
| Release | `BLOCKED` | depende de DEVICE + signing/release gates |

## Métricas: como ler corretamente

O repositório possui um roadmap amplo de 288 features. A contagem histórica `130 concluídas / 35 em progresso / 123 pendentes` é **baseline de planejamento**, não medição automática do código atual. O código avançou por commits posteriores e várias capacidades mudaram de stub/parcial para implementação integrada.

Para estado atual, usar três dimensões separadas:

1. **source capability** — o que está integrado em `app/src/`;
2. **build evidence** — o que foi compilado/testado no commit exato;
3. **runtime evidence** — o que foi executado em device/serviço real.

Não derivar um único percentual sem declarar o denominador.

## Correções de divergência documentação ↔ fonte

- Actions não está atualmente sem execução: run `31821491676` executou integralmente.
- APK atual não é mais `TOKEN_VAZIO`: o BUILD possui APK + SHA-256 + dual ABI + receipt.
- Multi-provider não deve continuar rotulado como `STUB_TYPED`; os adapters estão implementados em fonte.
- Offline queue não deve continuar descrita como sem workers/storage; esses elementos existem, faltando evidence física de recovery.
- P33 `33/33` é estado de fonte, não autorização para claim de runtime.
- PTY real, fixtures externas, device físico e release permanecem explicitamente abertos.

## Fonte de verdade

Ordem de precedência:

1. commit exato observado;
2. código integrado;
3. testes/lint realmente executados;
4. APK/hash/ABI/build receipt;
5. runtime/device receipts;
6. `ECOSYSTEM_RUNTIME_STATE.json`;
7. `docs/RAFGITTOOLS_CURRENT_STATE.md`;
8. este relatório;
9. roadmap/documentos históricos.

## Triângulo atual

```text
SOURCE  -> PASS
BUILD   -> PASS
DEVICE  -> TOKEN_VAZIO_PHYSICAL_DEVICE_REQUIRED
SOURCE' -> só fecha após runtime receipt revalidar commit + APK SHA
```

## Próximo gate

Instalar e iniciar **exatamente** o APK SHA-256
`115b9cb1e71f53f16b2648924a09549b8e5e0b9e453280cab2e7f183a411ebf6`
em aparelho físico ARM suportado e registrar receipt com commit, APK SHA, package id, ABI/device, install e launch.

## Retroalimentar[3]

- **F_ok:** fonte avançada, regressões executadas, lint PASS, APK compilado e dual ABI comprovado.
- **F_gap:** DEVICE, fixtures externas e release continuam auditavelmente abertos.
- **F_next:** fechar o vértice DEVICE sem trocar APK/commit; depois integrar #346 sem regressão.
