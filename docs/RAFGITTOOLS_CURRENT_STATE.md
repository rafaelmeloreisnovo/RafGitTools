# RAFGITTOOLS_CURRENT_STATE

- Status: **ATIVO — código integrado, validação local pendente**
- Estado observado: **2026-07-18**
- Fonte de verdade: código em `app/src/`, testes, contratos e `ECOSYSTEM_RUNTIME_STATE.json`
- GitHub Actions: **OUT_OF_SCOPE_NO_CREDIT** nesta atividade; ausência de run não é PASS nem FAIL de código

## Núcleo Android/Git/GitHub

| Componente | Estado | Limite atual |
|---|---|---|
| Android / Compose / Hilt / Room | `IMPLEMENTED` | APK e device smoke não executados nesta atividade |
| PAT + armazenamento seguro | `IMPLEMENTED` | Resultado end-to-end depende de execução local/device |
| OAuth Device Flow | `IMPLEMENTED / CONFIG_REQUIRED` | Exige Client ID público configurado |
| API GitHub | `PARTIAL_ADVANCED` | Falta matriz end-to-end completa |
| Git local via JGit | `PARTIAL_ADVANCED` | Rede, credenciais e conflitos precisam de regressão real |
| SSH | `PARTIAL` | Depende de ambiente/chaves e teste em device |
| GPG | `ADAPTER_IMPLEMENTED / RUNTIME_TOKEN_VAZIO` | Wrapper exige binário `gpg` acessível pelo processo autorizado |
| Git LFS | `ADAPTER_IMPLEMENTED / RUNTIME_TOKEN_VAZIO` | Wrapper exige `git-lfs` e repositório real |
| Worktree | `ADAPTER_IMPLEMENTED / RUNTIME_TOKEN_VAZIO` | Falta matriz de filesystem/device |
| Bisect | `ADAPTER_IMPLEMENTED / RUNTIME_TOKEN_VAZIO` | Falta cenário regressivo controlado |
| Webhooks | `STUB` | Sem implementação funcional comprovada |
| Terminal | `BOUNDED_EXECUTOR` | Não é PTY/VT100 e não aceita Git gravável |
| Multi-provider | `STUB_TYPED` | Estados `NOT_CONFIGURED` e `NOT_IMPLEMENTED` são distintos de lista vazia |

## Correções estruturais deste corte

### Executor limitado

`TerminalEmulator.kt` agora:

- drena stdout/stderr enquanto o processo executa, evitando bloqueio por pipe cheio;
- rejeita aspas abertas, escapes incompletos, NUL e múltiplas linhas;
- permite apenas subcomandos Git de leitura;
- rejeita ações `find` que executem comandos ou removam/escrevam arquivos;
- continua explicitamente fora do escopo de PTY/VT100.

Operações graváveis devem seguir:

```text
RafGitTools
→ job.v1 tipado
→ GovernanceGate
→ runtime Termux autorizado
→ resultado estruturado
```

### Fila offline

`OfflineQueue` recebeu fronteira de armazenamento durável. A implementação
`AtomicFileQueueStorage` usa:

- registros binários length-prefixed;
- limites de quantidade e tamanho;
- `fsync` antes da publicação;
- arquivo temporário no mesmo diretório;
- rollback da mutação quando a persistência falha.

Ainda falta conectar um codec de `OfflineOperation` e WorkManager. Portanto,
**fila durável disponível** não significa **sincronização de produção concluída**.

### Multi-plataforma

GitLab, Bitbucket, Gitea e Azure DevOps não retornam mais `emptyList()` como se
a integração estivesse concluída. As consultas tipadas distinguem:

- `Success`;
- `NotConfigured`;
- `NotImplemented`;
- `AuthenticationError`;
- `NetworkError`.

## Diretório `fazer/`

Os 17 arquivos de `fazer/` são rascunhos históricos **não compilados**. A auditoria
do PR #267 concluiu que foram superados pelas implementações mais completas em
`app/src/`. Eles **não estão pendentes de integração** e não podem ser usados como
fonte de verdade.

Próxima limpeza dedicada:

```text
fazer/
→ archive/legacy-drafts/fazer-2026-07/
ou remoção após comparação final de hashes/diffs
```

Nenhuma funcionalidade deve ser contabilizada duas vezes por existir em
`app/src/` e em `fazer/`.

## Contratos locais

- `contracts/job-v1.schema.json`: handoff tipado e limitado para o runtime;
- `contracts/ecosystem-runtime-state.schema.json`: estados e evidências;
- `ECOSYSTEM_RUNTIME_STATE.json`: matriz material desta revisão;
- `scripts/validate_runtime_truth.py`: validação stdlib, sem GitHub Actions.

## Evidência e limitações

- Mudanças de código e testes estão presentes na branch de auditoria.
- GitHub Actions não foi executado por falta momentânea de créditos.
- APK, device smoke ARM32/ARM64 e integração Termux permanecem `TOKEN_VAZIO` até
  existirem comando, stdout/stderr, hash e resultado de aparelho.

## Retroalimentar[3]

- **F_ok:** inconsistências conhecidas foram transformadas em código, contratos e testes locais.
- **F_gap:** build Android, device runtime, WorkManager e ponte Termux não foram comprovados.
- **F_next:** executar o validador local; depois, em ambiente Android disponível, compilar APK e registrar hashes/logs sem depender de Actions.
