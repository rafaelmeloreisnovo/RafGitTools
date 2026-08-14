# RAFGITTOOLS_ROADMAP_TRUE

- Status: **ATIVO — fonte de verdade de sequência operacional**
- Última atualização: **2026-08-14**
- Regra: `SOURCE != TESTED != BUILD_VERIFIED != DEVICE_VERIFIED != RELEASE`
- Roadmap histórico de 288 features: referência de planejamento; **não** substitui a realidade observada no código.

## Estado já alcançado

### SOURCE

- P33: **33/33 SOURCE_FUNCTIONAL**.
- Android/Compose/Hilt/Room: implementação avançada.
- Git local/JGit: implementação avançada, incluindo shallow/single-branch/submodules, amend, pull-rebase, force-with-lease, stash, reflog, blame, config e interactive staging.
- Auth: PAT, OAuth Device Flow, gh CLI import, SSH, biometria, multi-account e lifecycle capability-aware de tokens.
- Multi-provider: adapters GitLab, Bitbucket, Gitea/Forgejo e Azure DevOps presentes em fonte.
- Offline: fila, persistência Room, storage atômico e worker presentes.
- Terminal: bounded executor implementado; PTY/VT100 completo permanece fora do estado entregue.
- LFS/worktree/bisect/GPG: implementação/adapters presentes, com runtime externo ainda gated.
- JNI/RAFAELIA e camada nativa Android integradas ao build.
- Bridge local/LLM e extensão Kiwi existem em fonte, com runtime real ainda não fechado.

### TESTED + BUILD_VERIFIED

Checkpoint comprovado para o commit:

`bbdb556a59c06a23cc2f6df6ba0ae7c98466a4fa`

Workflow `Android Client Build` run `31821491676`: **PASS**.

Passaram:

- custody/structural tests;
- authentication unit tests;
- full dev unit tests;
- Android lint;
- `assembleDevDebug`;
- verificação do APK;
- build receipt;
- upload do artifact.

APK comprovado:

- `app-dev-debug.apk`;
- 24,672,130 bytes;
- SHA-256 `115b9cb1e71f53f16b2648924a09549b8e5e0b9e453280cab2e7f183a411ebf6`;
- ZIP CRC PASS;
- `armeabi-v7a` PRESENT;
- `arm64-v8a` PRESENT.

## Gate 0 — coerência documental/CI

Estado: **EM FECHAMENTO**.

Fechado nesta trilha:

- `CURRENT_STATE`, `STATUS_REPORT`, runtime matrix, P33, Code Reality Matrix e índice reconciliados;
- evidence append-only do build materializada;
- alvo hardcoded `issues/236/comments` removido do CI;
- comentário de build agora é ligado a `github.event.pull_request.number`;
- teste `tests/test_workflow_pr_binding.py` impede regressão para PR/issue hardcoded.

Gate para fechar:

- CI do head atual deve executar e passar os novos testes/workflows.

## Gate 1 — DEVICE físico

Estado: **TOKEN_VAZIO_PHYSICAL_DEVICE_REQUIRED**.

Exigir na mesma cadeia:

1. commit escolhido;
2. APK produzido para esse commit;
3. SHA-256 do APK;
4. instalação física;
5. launch;
6. runtime receipt;
7. revalidação commit + SHA-256;
8. `triangle_closure=PASS`.

Nenhum APK histórico autoriza o DEVICE de um head novo.

## Gate 2 — fixtures reais críticas

Após DEVICE básico:

- PAT/401/reautenticação em Android;
- GitHub App Device Flow refresh real quando configurado;
- clone/fetch/pull/push em remote descartável;
- force-with-lease com remote privado controlado;
- SSH com chave/servidor real;
- interactive staging em fixture física;
- offline enqueue -> restart -> recovery;
- GitLab/Bitbucket/Gitea-Forgejo/Azure DevOps com credenciais descartáveis.

## Gate 3 — runtimes externos

Fechar isoladamente:

- `git-lfs` + remote fixture;
- worktree filesystem matrix;
- bisect regression fixture;
- GPG binário + assinatura/verificação;
- Kiwi unpacked + loopback bridge;
- modelo local/GGUF;
- LLaMA JNI somente após dependência externa pinada e reproduzível.

## Gate 4 — release

Estado: **BLOCKED_BY_DEVICE_AND_RELEASE_GATES**.

Exigir antes de promover release:

- DEVICE PASS do commit candidato;
- regressão dos fluxos críticos;
- assinatura oficial configurada;
- artefato release + SHA/receipt;
- política de distribuição definida;
- `release_allowed=true` emitido por gate explícito, nunca por inferência.

## Prioridade real

```text
P0  CI/head coerente
P1  DEVICE físico do APK exato
P2  Git/Auth/Offline fixtures reais
P3  providers e runtimes externos
P4  release/signing/distribuição
P5  expansões (PTY completo, IA local avançada, extras do roadmap)
```

Não adicionar nova feature de baixa prioridade se ela deslocar P0/P1 sem justificativa.

## Fonte de verdade

1. `app/src/`;
2. `app/src/test/` + `tests/`;
3. `.github/workflows/`;
4. `docs/RAFGITTOOLS_CODE_REALITY_MATRIX.md`;
5. `docs/RAFGITTOOLS_CURRENT_STATE.md`;
6. `ECOSYSTEM_RUNTIME_STATE.json`;
7. `data/evidence/` + `docs/canonical/`;
8. este roadmap operacional;
9. roadmap histórico por último.

## Retroalimentar[3]

- **F_ok:** fonte e BUILD já ultrapassaram amplamente o roadmap antigo.
- **F_gap:** CI do novo head, DEVICE e fixtures reais ainda delimitam o claim.
- **F_next:** passar o novo gate CI e produzir o primeiro runtime receipt físico commit-bound.
