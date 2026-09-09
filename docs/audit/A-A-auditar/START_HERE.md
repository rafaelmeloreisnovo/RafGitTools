# A-A AUDITAR — START HERE — RAFAELIA

**Modo:** desenvolvimento; não treinamento de modelo  
**Persistência:** append-only operacional  
**Governança:** `SOURCE != ARTEFATO != EXECUCAO != EVIDENCIA != CLAIM`  
**Lacuna:** `TOKEN_VAZIO != 0`

## START HERE — boot / cadeia de custódia

1. Ler livro-memória central.
2. Ler `A-A auditar`.
3. Filtrar pelo tema atual.
4. Enumerar pendências relevantes.
5. Mostrar só 3–5 palavras.
6. Sugerir entrada na rodada.
7. Abrir fonte antes de agir.
8. Registrar source + hash/ref.
9. Separar artefato de execução.
10. Executar somente escopo autorizado.
11. Registrar evidência observável.
12. Promover claim só com prova.
13. Append receipt de resultado.
14. Manter rollback navegável.

## Regra de apresentação

Ao iniciar tema relacionado, mostrar **somente** pendências não auditadas aplicáveis, uma por linha, com rótulo de 3–5 palavras. A lista é sugestão pré-rodada; não constitui execução nem evidência.

`Start here` = executar este boot antes da rodada.

## Fila inicial — não auditado

- `AA-0001` — BrowserRaf layout gráfico
- `AA-0002` — BrowserRaf TLS completo
- `AA-0003` — BrowserRaf replay runtime
- `AA-0004` — BrowserRaf cadeia custódia
- `AA-0005` — ASCII fluxo sem tamanho
- `AA-0006` — Renderer geometria variável

`AA-0005` e `AA-0006` permanecem **HIPOTESE_NAO_AUDITADA** até teste/arquitetura observável.

## BrowserRaf — fonte observada

- repo: `rafaelmeloreisnovo/RafGitTools`
- path: `BrowserRaf/internal/`
- cadeia: `termux-app-rafacodephi/master/Browser.sh -> RafGitTools/BrowserRaf/internal/`
- componentes: `br_sys.h`, `br_dns.h`, `br_http.h`, `br_tls.h`, `br_html.h`, `br_main.c`, `br_start.S`, `Makefile`
- observado: syscalls Linux diretas; ARM32 `svc #0`; `-nostdlib`; `-fno-builtin`; `_start` próprio
- pipeline observado: `URL -> DNS -> TCP -> HTTP -> HTML -> text render`
- layout HTML gráfico: `TOKEN_VAZIO`
- HTTPS/TLS 1.3 completo: `TOKEN_VAZIO`

## Próximo passo único BrowserRaf

**Auditar renderer gráfico sem reescrever transporte.**

## Invariante de espaço lógico

“Sem tamanho” significa **sem extensão global fixa no espaço lógico**, não memória física infinita. A implementação deve manter recursos físicos finitos por janelas/chunks/páginas e permitir que o domínio lógico cresça por composição:

`ESPACO_LOGICO = uniao(chunk_k), k >= 0`

com `MEMORIA_ATIVA(t)` sempre limitada pelo dispositivo.

## Append rule

Nunca apagar uma entrada histórica. Mudança de estado deve entrar como novo receipt, apontando para o ID anterior e preservando proveniência/rollback.
