# RafGitTools — Current Source State — 2026-05-26

> **Documento histórico / SUPERSEDED.**  
> A fonte atualizada está em [`CURRENT_SOURCE_STATE_2026-07-10.md`](CURRENT_SOURCE_STATE_2026-07-10.md).

Este snapshot foi preservado para rastreabilidade. Ele não deve ser usado como estado técnico atual porque o código avançou principalmente em:

- autenticação GitHub;
- escolha de métodos de login;
- validação PAT e OAuth;
- importação `gh` / Termux;
- testes do fluxo de credenciais;
- contrato de build e artefato APK.

## Estado registrado em 2026-05-26

Naquela data, a base Kotlin/JGit/GitHub API estava funcional/parcial, o build nativo cobria `armeabi-v7a` e `arm64-v8a`, e a camada RAFAELIA/Termux ARM32 permanecia experimental.

A fonte de verdade então identificada já era:

- código-fonte em `app/`;
- testes em `app/src/test` e `app/src/androidTest`;
- `app/build.gradle`;
- CMake e ASM em `app/src/main/cpp`;
- scripts de validação;
- workflows CI.

As lacunas históricas incluíam cobertura parcial, stubs de GPG/LFS/Worktree/Webhooks, terminal não pronto e divergência entre documentação e código.

## Regra de leitura

```text
snapshot de maio
≠
estado atual de julho
```

Consulte:

- `docs/CURRENT_SOURCE_STATE_2026-07-10.md`;
- `docs/STATUS_REPORT.md`;
- `.github/workflows/android-client-build.yml`;
- código e testes da branch/revisão vigente.
