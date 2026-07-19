# Runtime Truth — validação local — 2026-07-18

## Escopo

Este corte executa procedimentos da auditoria RafGitTools ↔ RafPolimata sem
GitHub Actions. A conta está momentaneamente sem crédito de Actions.

```text
workflow ausente
≠ PASS
≠ FAIL de código
```

## Implementações

- fila offline com storage durável opcional e rollback de persistência;
- storage binário bounded com `fsync` e publicação por rename;
- executor de comandos com drenagem concorrente de output;
- rejeição de aspas abertas, escapes incompletos, comandos múltiplos e Git gravável;
- provedores externos com estados tipados, sem `emptyList()` ambíguo;
- `job.v1` tipado para handoff autorizado;
- matriz `ECOSYSTEM_RUNTIME_STATE.json`;
- correção da contradição documental sobre `fazer/`;
- testes unitários adicionados para fila, terminal e providers.

## Validação disponível

```sh
python3 scripts/validate_runtime_truth.py
```

O validador usa somente Python stdlib e verifica contratos JSON, semântica dos
estados e invariantes de fonte. O build Android, APK e aparelho não foram
executados nesta atividade e permanecem `TOKEN_VAZIO`.

## Limites preservados

- `TerminalEmulator` continua não sendo PTY/VT100;
- operações Git graváveis devem atravessar `job.v1` + GovernanceGate + runtime autorizado;
- persistência da fila não significa que WorkManager e codec de operação estejam integrados;
- wrappers GPG/LFS/Worktree/Bisect continuam dependentes de runtime externo comprovável;
- GitLab, Bitbucket, Gitea e Azure DevOps continuam sem adapters HTTP reais.

## Próxima promoção

Qualquer promoção para `VERIFIED` precisa registrar comando, commit, versão de
ferramenta, stdout/stderr, hashes e, para Android, ABI/device/install/smoke.
