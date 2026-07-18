# RafGitTools — Relatório de Status

**Data:** 2026-07-18  
**Estado geral:** 🟡 Cliente GitHub/Git funcional avançado; build/device permanecem sem prova nesta atividade  
**Matriz executável:** `ECOSYSTEM_RUNTIME_STATE.json`  
**Validador sem Actions:** `python3 scripts/validate_runtime_truth.py`

## Regra de evidência

```text
arquivo existente
≠ código integrado
≠ teste executado
≠ APK gerado
≠ runtime em aparelho
```

GitHub Actions está marcado como `OUT_OF_SCOPE_NO_CREDIT`. Nenhuma ausência de
workflow é convertida em PASS ou FAIL de implementação.

## Semântica de status

- **IMPLEMENTED:** código integrado, com contrato concreto.
- **TESTS_ADDED:** testes presentes; execução deve ser registrada separadamente.
- **PARTIAL_ADVANCED:** implementação extensa, sem matriz end-to-end completa.
- **ADAPTER_IMPLEMENTED:** wrapper existe, mas depende de processo/ferramenta externa.
- **STUB_TYPED:** interface existe e retorna estado explícito de não implementação.
- **TOKEN_VAZIO:** falta build, device, log, ferramenta ou artefato verificável.
- **BOUNDED_EXECUTOR:** executor limitado, não terminal PTY/VT100.

## Classificação técnica atual

| Componente | Status | Evidência/limite |
|---|---|---|
| Android + Compose + Hilt + Room | `IMPLEMENTED` | Código integrado; APK não produzido nesta atividade |
| Login PAT | `IMPLEMENTED + TESTS_ADDED` | Armazenamento seguro e fluxo de identidade presentes |
| OAuth Device Flow | `IMPLEMENTED / CONFIG_REQUIRED` | Exige Client ID público |
| Importação `gh` / Termux | `IMPLEMENTED + TESTS_ADDED` | Runtime externo ainda precisa de prova no device |
| SSH | `PARTIAL` | Matriz real de chaves/servidores pendente |
| API GitHub | `PARTIAL_ADVANCED` | Endpoints e repositórios presentes |
| Git local via JGit | `PARTIAL_ADVANCED` | Conflitos/rede/credenciais pedem regressão |
| UI GitHub/Git | `PARTIAL_ADVANCED` | Telas não equivalem a fluxo end-to-end |
| Fila offline | `DURABLE_STORAGE_AVAILABLE` | Codec de operação e WorkManager ainda pendentes |
| GPG | `ADAPTER_IMPLEMENTED / TOKEN_VAZIO_RUNTIME` | Requer binário autorizado e teste real |
| LFS | `ADAPTER_IMPLEMENTED / TOKEN_VAZIO_RUNTIME` | Requer `git-lfs` e repositório real |
| Worktree | `ADAPTER_IMPLEMENTED / TOKEN_VAZIO_RUNTIME` | Requer filesystem/device |
| Bisect | `ADAPTER_IMPLEMENTED / TOKEN_VAZIO_RUNTIME` | Requer fixture de regressão |
| Webhooks | `STUB` | Sem implementação funcional comprovada |
| Multi-provider | `STUB_TYPED` | Não confunde não implementado com lista vazia |
| Terminal | `BOUNDED_EXECUTOR` | Leitura/diagnóstico; escrita deve usar job tipado |
| APK verificável | `TOKEN_VAZIO` | Sem APK/SHA/device nesta atividade |

## Correções deste corte

1. `TerminalEmulator` drena saída concorrentemente e rejeita comandos/argumentos perigosos.
2. `OfflineQueue` possui fronteira durável e rollback quando a gravação falha.
3. `AtomicFileQueueStorage` grava registros limitados, executa `fsync` e publica por rename.
4. Multi-provider usa estados tipados: `NotConfigured`, `NotImplemented`, erros e sucesso.
5. `fazer/` foi classificado como legado superado, não como implementação pendente.
6. `job.v1` e a matriz de runtime foram materializados em `contracts/` e na raiz.

## Fonte de verdade

1. código integrado em `app/src/`;
2. testes em `app/src/test/`;
3. contratos em `contracts/`;
4. `ECOSYSTEM_RUNTIME_STATE.json`;
5. `docs/RAFGITTOOLS_CURRENT_STATE.md`;
6. este relatório;
7. roadmaps e documentos históricos.

## Próximo gate local

```sh
python3 scripts/validate_runtime_truth.py
```

O build Android e a prova de aparelho devem ser executados localmente quando o
SDK/device estiver disponível e registrados com comando, stdout/stderr, APK,
SHA-256 e identificação da ABI. Até lá permanecem `TOKEN_VAZIO`.

## Retroalimentar[3]

- **F_ok:** falhas estruturais foram convertidas em código, testes e contratos.
- **F_gap:** APK, device, WorkManager e transporte Termux ainda não foram comprovados.
- **F_next:** executar a validação local e ligar `job.v1` ao runtime autorizado sem shell genérico.
