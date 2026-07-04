# Refatoração segura de zips e arquivos do `Livro/`

Este documento define uma refatoração conservadora para separar texto, pesquisa, protótipos e pacotes compactados.

## Problema

O diretório `Livro/` mistura capítulos, whitepapers, metodologia, relatórios, scripts, protótipos, textos brutos, zips, tarballs e snapshots de sessão.

Essa mistura dificulta saber:

1. o que é fonte ativa;
2. o que é arquivo histórico;
3. o que pode ser executado ou testado com segurança.

## Cadeia de decisão

```text
ARQUIVO_LOCAL → TIPO → RISCO → VALIDAÇÃO → DESTINO
```

Um arquivo não deve ser promovido apenas pelo nome. A promoção precisa de evidência local, comando de teste e saída verificável.

## Classes operacionais

| Classe | Exemplos | Destino recomendado | Regra |
| --- | --- | --- | --- |
| Capítulo/documento narrativo | `Capítulo *.md`, `Introducao.md` | `01_capitulos/` | mover só após revisar links internos |
| Pesquisa/whitepaper | `RAFAELIA_WHITEPAPER.md`, `paper.md`, `METHODOLOGY.md`, `REPRODUCIBILITY.md` | `02_pesquisa/` | manter como pesquisa, não como runtime |
| Compilador/protótipo | `falas.sh`, `fala.sh`, `raf_c_to_asm_root_optimizer.py` | `03_compiladores/` ou caminho atual | mover somente se comandos e docs forem atualizados |
| Texto bruto | `.txt` sem estrutura canônica | `04_raw/` | transformar em `.md` curado antes de promover |
| Snapshot de sessão | `RAFAELIA_SESSION_COMPLETE.zip` | `_archives/sessions/` | validar integridade e listar conteúdo |
| Bundle versionado | `rafaelia_bundle_v*.tar.gz` | `_archives/bundles/` | nunca extrair direto no repo |
| Relatório de varredura | saída do script de refatoração | `_archives/reports/` | gerar por execução local |

## Movimentos seguros inicialmente declarados

```text
Livro/RAFAELIA_SESSION_COMPLETE.zip  → Livro/_archives/sessions/RAFAELIA_SESSION_COMPLETE.zip
Livro/rafaelia_bundle_v4.tar.gz      → Livro/_archives/bundles/rafaelia_bundle_v4.tar.gz
Livro/rafaelia_bundle_v5.tar.gz      → Livro/_archives/bundles/rafaelia_bundle_v5.tar.gz
Livro/rafaelia_bundle_v6.tar.gz      → Livro/_archives/bundles/rafaelia_bundle_v6.tar.gz
```

Arquivos como `falas.sh` e `compiladorlowFala.txt` não devem ser movidos automaticamente neste primeiro passe, porque existem comandos que apontam para `Livro/falas.sh`.

## Fluxo recomendado

### 1. Inventariar

```bash
bash scripts/refactor_livro_archives.sh --check
```

### 2. Validar compactados

```bash
unzip -t Livro/RAFAELIA_SESSION_COMPLETE.zip
unzip -l Livro/RAFAELIA_SESSION_COMPLETE.zip

tar -tzf Livro/rafaelia_bundle_v4.tar.gz
tar -tzf Livro/rafaelia_bundle_v5.tar.gz
tar -tzf Livro/rafaelia_bundle_v6.tar.gz
```

### 3. Aplicar movimentos históricos

```bash
bash scripts/refactor_livro_archives.sh --apply
```

### 4. Revisar diff

```bash
git status --short
git diff --stat
git diff -- Livro scripts/refactor_livro_archives.sh
```

### 5. Atualizar referências

Se algum arquivo ativo for movido depois, atualizar antes:

- comandos no README;
- referências em docs;
- scripts que usam caminho fixo;
- CI/workflows;
- índices de rastreabilidade.

## Critério de bloqueio

Não mover automaticamente quando:

- arquivo for chamado por script existente;
- arquivo tiver nome usado em documentação;
- conteúdo for binário sem checksum;
- pacote tiver estrutura desconhecida;
- houver risco de sobrescrever caminho já existente;
- o arquivo for grande e não houver evidência de que é somente histórico.

Nesses casos, classificar, registrar e não promover.

## Resultado esperado

```text
Livro/
├── README.md
├── REFATORACAO_ARQUIVOS.md
├── _archives/
│   ├── sessions/
│   ├── bundles/
│   └── reports/
├── 01_capitulos/
├── 02_pesquisa/
├── 03_compiladores/
└── 04_raw/
```

O objetivo é separar memória histórica, pesquisa conceitual, protótipo executável, fonte ativa, pacote compactado e lacuna assumida.
