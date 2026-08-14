# RafGitFS — Pós-V1: Fechamento das Evidências

As oito ondas de implementação encerram o escopo funcional de fonte. As próximas ondas não devem aumentar funcionalidades antes de fechar provas.

## Onda 9 — Build reproduzível

```text
checkout SHA exato
→ JDK/Gradle fixados
→ gates Python
→ KSP/Room/Compose compile
→ testes JVM
→ APK debug
→ checksums e artefatos
```

Saída obrigatória: logs com etapas, artifact SHA-256 e manifesto de ambiente.

## Onda 10 — Android físico

Dispositivos prioritários:

- Moto E7, Android 10, armeabi-v7a;
- Realme Note 50, Android 14/15, arm64-v8a.

Cenários:

- criação e migração do banco;
- navegação por repositórios;
- arquivos UTF-8 e binários;
- cache sob pressão;
- pin offline e reinício;
- acessibilidade com TalkBack;
- workspace, stage e undo;
- conflito de SHA-base;
- PR draft em repositório de teste;
- rollback por commit.

## Onda 11 — Segurança e supply chain

- SBOM;
- verificação de dependências;
- assinatura de APK;
- segredo em memória/logs;
- interceptação de requests para provar `force=false` e `draft=true`;
- fuzz de caminhos;
- teste de corrupção de cache/workspace;
- revisão independente;
- política de retenção e limpeza de branches `rafgitfs/*` abandonadas.

## Onda 12 — Desempenho e release

Medir p50/p95/p99 para:

```text
listagem local da árvore
pesquisa Room
refresh incremental
leitura de cache
verificação SHA
plano de 1k/10k arquivos
commit de múltiplos blobs
uso de memória
consumo de bateria
```

Nenhum limite provisório vira claim sem ambiente, amostra e recibo.

## Ordem operacional

```text
P0 corrigir ZERO_STEP_NO_LOGS
P0 compilar HEAD exato
P0 executar migração e testes
P1 Android físico ARM32/ARM64
P1 PR draft end-to-end
P1 rollback end-to-end
P1 assinatura e SBOM
P2 performance e energia
P2 revisão externa
P3 release candidate
```

## Regra de congelamento

Até concluir P0 e P1:

- não adicionar novos provedores;
- não adicionar merge automático;
- não adicionar exclusão remota;
- não liberar escrita direta;
- não promover `claim_allowed`;
- não confundir fonte implementada com execução observada.
