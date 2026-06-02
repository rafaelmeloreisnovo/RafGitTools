# RafGitTools — catálogo de possibilidades, organização e validação

Status: documento de análise e planejamento técnico.
Data: 2026-06-02.
Escopo revisado: inventário do repositório até 5 níveis de profundidade, código Android/Kotlin, camada nativa C/ASM, scripts, documentação existente, bundles conceituais e pendências.

> Regra de honestidade: este documento separa **implementado**, **parcial/experimental**, **stub/roadmap** e **hipótese conceitual**. Recursos `GPG`, `LFS`, `worktree` e partes de `webhook` não devem ser vendidos como produção sem implementação, testes e validação.

## 1. Inventário observado até 5 níveis

Comando de referência usado para mapear a árvore sem varrer `.git`, `.gradle` e `build`:

```bash
find . -maxdepth 5 -type f \( -path '*/.git/*' -o -path '*/.gradle/*' -o -path '*/build/*' \) -prune -o -type f -print | sed 's#^./##' | sort
```

Resumo quantitativo observado:

| Área | Quantidade aproximada até 5 níveis | Papel prático |
|---|---:|---|
| `docs/` | 68 arquivos | Fonte principal de governança, build, segurança, roadmap, i18n, matemática e estado real. |
| `Livro/` | 64 arquivos | Material conceitual/whitepaper; precisa ser tratado como contexto, não como implementação Android validada. |
| `_incoming/` | 53 arquivos | Propostas, trechos nativos, scripts e material bruto; não é produção até ser integrado, testado e documentado. |
| raiz do repo | 47 arquivos | README, licenças, relatórios históricos, bundles e arquivos soltos que exigem triagem. |
| `.github/` | 30 arquivos | Workflows, templates, validação e automação CI/CD. |
| `BrowserRaf/` | 22 arquivos | Experimento de navegador/bare-metal separado do app principal. |
| `fazer/` | 19 arquivos | Arquivos Kotlin de referência ou pendência; não assumir como conectados ao build. |
| `COMPILER/` | 15 arquivos | Experimentos de otimizador/ASM e pacotes; tratar como laboratório. |
| `scripts/` | 12 arquivos | Build, validação Android, Termux, native APK e Toro7D. |
| `rafaelia/` | 9 arquivos | Módulos C de geometria/omega híbrido e testes locais. |
| `app/` visível até 5 níveis | 7 arquivos | Build Gradle, schema Room e entradas superficiais; o Kotlin real está em profundidade maior. |

Observação importante: o limite de 5 níveis não mostra todos os arquivos Kotlin da aplicação. O código principal em `app/src/main/kotlin/com/rafgittools/...` contém 139 arquivos Kotlin quando varrido recursivamente para o pacote do app.

## 2. Fonte de verdade técnica atual

### 2.1 Plataforma Android

- Aplicação Android única, com namespace `com.rafgittools`.
- `compileSdk = 34`, `targetSdk = 34`, `minSdk = 24`.
- Java/Kotlin em JDK 17.
- Jetpack Compose habilitado.
- Flavors: `dev` e `production`.
- Build types: `debug` e `release`.
- ABIs nativas preservadas: `armeabi-v7a` e `arm64-v8a`.
- Release público exige signing real; `ALLOW_UNSIGNED_RELEASE=true` é apenas para validação interna.

### 2.2 Camada nativa

- CMake cria biblioteca compartilhada `rafcore`.
- Fontes nativas atuais: `native_bridge.c` e arquivos ASM em `app/src/main/cpp/asm/`.
- O build nativo é compilado para os ABIs definidos no Gradle.
- Há diretórios separados `asm/arm32` e `asm/arm64`, além de arquivos comuns em `asm/`; qualquer troca deve manter ARM32 e ARM64.

### 2.3 Código Kotlin principal

Mapa funcional observado em `app/src/main/kotlin/com/rafgittools`:

| Pacote | Estado técnico | Possibilidades organizadas |
|---|---|---|
| `core/security` | Implementações de armazenamento seguro, criptografia, biometria, SSH e múltiplas contas. | Endurecer threat model, testes de regressão, rotação de chaves, auditoria de segredo e compatibilidade Android. |
| `data/auth` | OAuth device flow, importação de auth CLI, refresh de token e cache. | Fluxos enterprise: políticas de token, revogação, múltiplas contas, logs mínimos e modo offline seguro. |
| `data/git` e `data/repository` | JGit e repositório Git local/remoto. | Expandir clone, status, commit, push/pull, branches, tags, stash, rebase/cherry-pick com rollback. |
| `data/github` | Retrofit/OkHttp para GitHub. | Issues, PRs, releases, notifications, rate limit, retries, cache e failover offline. |
| `data/cache` | Room/DAO/entities/schema. | Estratégia de cache, migração, TTL, invalidação, compactação e fallback. |
| `domain/model` | Modelos de Git/GitHub. | Manter contratos estáveis e serializáveis para UI, cache e testes. |
| `domain/usecase` | Use cases Git e GitHub. | Transformar ações em pipelines pequenos, testáveis e reversíveis. |
| `ui/screens` | Compose para auth, home, repo, commits, PRs, issues, releases, terminal, settings etc. | Consolidar navegação, estados de erro, loading, empty state, acessibilidade e i18n. |
| `offline` | Fila offline e sync em background. | Failsafe/failover real com replay, deduplicação, idempotência e rollback. |
| `terminal` | Executor de comandos permitido e limitado. | Evoluir para PTY/VT100 apenas com sandbox, política de comandos e logs auditáveis. |
| `webhook` | Validação leve de payload JSON e eventos. | Evoluir para assinatura HMAC, replay protection, roteamento idempotente e testes de payload. |
| `gitlfs`, `security/GpgKeyManager`, `worktree` | Stubs/roadmap. | Implementar somente após contrato, testes, UI/API e validação; não afirmar produção hoje. |

## 3. Duas leituras complementares do projeto

### Ciclo 1 — Engenharia Android verificável

Este ciclo entrega valor ao usuário sem depender de metáfora:

1. Autenticar com GitHub ou operar local/offline.
2. Abrir/listar repositórios.
3. Ler status, commits, branches, tags, diffs e arquivos.
4. Executar ações Git por use cases.
5. Persistir cache e preferências.
6. Sincronizar em background com fila offline.
7. Mostrar erro de forma clara e registrar evidência mínima.
8. Rodar build, testes, lint e verificação de APK.

### Ciclo 2 — Rafaelia/Toro7D como camada de governança falsificável

Este ciclo não deve afirmar física quântica, prova matemática ou segurança por analogia. Ele serve como método para organizar sinais:

1. Normalizar entradas em dimensões como estado, entropia, hash, coerência, risco e ciclo.
2. Manter métricas reprodutíveis (`alpha = 0.25`, janelas, entropy_milli, regimes).
3. Usar 42 ciclos/amostras como ritual mínimo de observação, não como prova absoluta.
4. Converter metáforas em tarefas verificáveis: entrada, fórmula, saída, teste e arquivo.
5. Guardar o token vazio quando não houver prova: é melhor declarar lacuna do que inventar implementação.

## 4. Possibilidades fullstack/enterprise por módulo

| Módulo enterprise | Já existe base? | Entrega funcional mínima | Expansão enterprise | Testes obrigatórios |
|---|---|---|---|---|
| Autenticação GitHub | Sim | OAuth device/web e token seguro. | Multi-account, refresh, revogação, políticas por organização. | Unit, integração mockada, expiração, token inválido, offline. |
| Git local com JGit | Sim | clone/status/commit/history/branch. | rebase/cherry-pick/stash/tag/push/pull com rollback operacional. | Repositórios temporários, conflitos, rede indisponível, permissões. |
| GitHub API | Sim | Issues, PRs, releases, notifications. | rate-limit aware, ETag/cache, retry/backoff, audit trail. | MockWebServer, 401/403/404/429/5xx, paginação. |
| Cache Room | Sim | DAO + schema export. | TTL, migrações versionadas, compactação, purge seguro. | MigrationTest, schema diff, corrupção simulada. |
| Offline queue | Parcial | Enfileirar ação e sincronizar. | idempotência, dedupe, replay, failover e rollback. | Reboot simulado, duplicidade, ordem, falha parcial. |
| Terminal | Parcial/stub seguro | Executar comandos permitidos com timeout. | PTY/VT100, profiles, sandbox, policy engine. | Comandos bloqueados, timeout, workingDir inválido, saída grande. |
| Webhook | Parcial | Validar JSON/eventos e payload mínimo. | HMAC, replay window, idempotência e roteador. | Payload inválido, assinatura inválida, replay, eventos suportados. |
| GPG | Stub | Nenhuma entrega de produção hoje. | Assinatura/verificação com biblioteca comprovada ou integração compatível Android. | Vetores conhecidos, chaves inválidas, passphrase, compatibilidade. |
| Git LFS | Stub | Nenhuma entrega de produção hoje. | Pointer files, fetch/push, storage e progresso. | Pointer parsing, arquivo grande, rede falha, quota. |
| Worktree | Stub | Nenhuma entrega de produção hoje. | add/list/remove/prune com checagem JGit/CLI. | Branch inexistente, path ocupado, rollback, cleanup. |
| Native ARM32/ARM64 | Sim | Biblioteca `rafcore` buildável para dois ABIs. | Otimizações específicas por ABI sem quebrar fallback. | Build por ABI, verificação de símbolos, smoke JNI, Termux runtime. |
| I18n/multilíngue | Sim | Strings `values`, `values-pt-rBR`, `values-es`. | Inglês, português, espanhol, RTL futuro, glossário, revisão por idioma. | Missing translations, layout RTL, pluralização, snapshots UI. |

## 5. Failsafe, failover, rollback e mitigação

### 5.1 Padrão por ação Git

Cada ação mutável deve ter:

1. **Pré-condição**: repo existe, workdir limpo quando necessário, token/chave válido, rede disponível ou modo offline ativado.
2. **Snapshot mínimo**: HEAD, branch, status, arquivos tocados, timestamp e hash da intenção.
3. **Execução idempotente**: repetir a ação não pode duplicar efeito perigoso.
4. **Falha segura**: erro vira estado explícito de UI/log, não exceção silenciosa.
5. **Rollback**: caminho definido para restaurar HEAD/stage/worktree ou cancelar replay offline.
6. **Mitigação**: retry/backoff, prompt ao usuário, limpar lock, preservar patch/diff antes de desfazer.

### 5.2 Matriz de cenários

| Cenário | Failsafe | Failover | Rollback | Mitigação |
|---|---|---|---|---|
| Rede cai durante `push` | Não apagar fila nem token. | Reagendar com backoff. | Nenhum rollback local; marcar remoto desconhecido até confirmar. | Buscar estado remoto antes de repetir. |
| Conflito em `pull/rebase` | Parar e expor conflito. | Oferecer merge manual/local. | `rebase --abort`/restaurar snapshot quando suportado. | Salvar patch e instruções. |
| Token expira | Bloquear ação remota. | Refresh token ou modo offline. | Reverter apenas estados de UI/queue não enviados. | Reautenticação clara. |
| Cache corrompido | Isolar cache. | Recarregar da rede/repo. | Migrar ou recriar cache preservando preferências seguras. | Teste de migração e backup. |
| Terminal command timeout | Matar processo. | Permitir nova tentativa. | Não aplicar automações encadeadas sem confirmação. | Timeout configurável e allowlist. |
| Native/JNI falha | Não travar fluxo Kotlin crítico. | Fallback Kotlin/JGit quando possível. | Descarregar rotina experimental. | Smoke tests por ABI. |

## 6. Estratégia para baixo overhead sem prometer o impossível

A solicitação menciona branchless, sem malloc, sem GC, sem heap, syscalls e bare metal. No app Android/Kotlin isso precisa ser traduzido com precisão:

- Kotlin/Compose roda na JVM/ART e tem heap/GC por natureza; portanto **não existe Android Compose sem GC**.
- A camada nativa C/ASM pode buscar baixo overhead, mas dentro do NDK/Android, não bare metal real.
- Para evitar fricção de verdade no app, priorizar:
  - reduzir alocações em loops de parsing/diff;
  - manter buffers reutilizáveis onde fizer sentido;
  - medir antes/depois com benchmark;
  - evitar otimização microarquitetural sem teste por ABI;
  - preservar fallback genérico antes de ativar otimização ARM32/ARM64;
  - não trocar legibilidade por pseudo-otimização não medida.

Pipeline recomendado para otimizações:

```text
genérico correto -> teste unitário -> benchmark -> especialização ABI/opcional -> smoke APK -> fallback documentado
```

## 7. Organização de arquivos recomendada

### 7.1 Documentação

- Manter `docs/INDEX.md` como índice vivo.
- Promover documentos com estado real para `docs/RAFGITTOOLS_*`.
- Manter `docs/maths/` para Toro7D/fórmulas com limites explícitos.
- Manter `docs/issues/` para features ainda não implementadas.
- Tratar `Livro/` como corpus conceitual; quando algo virar engenharia, criar documento curto em `docs/` com escopo, evidência e teste.

### 7.2 Código Android

- Código executável principal deve ficar em `app/src/main/kotlin/com/rafgittools`.
- Testes unitários em `app/src/test/kotlin`.
- Testes instrumentados/migração Android em `app/src/androidTest/kotlin`.
- Stubs devem continuar protegidos por `FeatureFlags` até implementação completa.

### 7.3 Entrada bruta e experimentos

- `_incoming/` deve ser usado como quarentena: nada dali deve ser considerado buildado sem integração explícita.
- `fazer/`, `COMPILER/`, `BrowserRaf/`, `rafaelia/` e bundles `.zip` devem ser inventariados, triados e conectados a issues antes de qualquer merge técnico.

## 8. Roadmap recomendado por prioridade

### P0 — Integridade do build e documentação verdadeira

1. Rodar `./scripts/prepare_local_properties.sh` quando houver SDK.
2. Rodar unit tests e lint.
3. Validar APK dev/prod debug e release interno unsigned.
4. Atualizar matrizes de estado real após cada implementação.

### P1 — Git/GitHub essenciais

1. Fortalecer autenticação e token refresh.
2. Completar ações Git locais com testes em repositórios temporários.
3. Expandir GitHub API com paginação, rate limit e cache.
4. Melhorar UI de erro, loading e empty state.

### P2 — Resiliência enterprise

1. Offline queue idempotente.
2. Rollback de ações mutáveis.
3. Logs de auditoria mínimos e exportáveis.
4. Threat model e privacy/security review.

### P3 — Recursos avançados

1. Webhook com assinatura e replay protection.
2. Terminal PTY seguro.
3. GPG, LFS e worktree somente depois de contratos e testes.
4. Otimizações nativas por ABI com benchmark.

### P4 — Multidisciplinar/Toro7D

1. Manter simulações falsificáveis.
2. Conectar fórmulas a scripts e resultados JSON.
3. Separar metáfora, hipótese, medição e implementação.
4. Expandir i18n com guia por idioma e acessibilidade.

## 9. Critérios para considerar uma possibilidade “produção”

Uma possibilidade só deve sair de roadmap quando cumprir todos os itens:

- Há código integrado no build principal.
- Há feature flag ou caminho de UI/API claro.
- Há testes PASS cobrindo sucesso, erro e borda.
- Há documentação com comandos de validação.
- Há rollback ou mitigação para falha operacional.
- Há evidência em CI ou ambiente local com SDK configurado.
- Não há conflito com licenças, signing, privacidade ou segurança.

## 10. Próximos documentos úteis

1. Matriz linha-a-linha de telas Compose versus use cases.
2. Matriz de endpoint GitHub versus cache/erro/paginação.
3. Plano de migração dos arquivos soltos e bundles para issues ou arquivos arquivados.
4. Threat model de autenticação/token/SSH.
5. Plano de testes de rollback para operações Git mutáveis.
