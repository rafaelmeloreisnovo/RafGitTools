# Livro — índice operacional e refatoração de arquivos

Este diretório é tratado como **área de livro, pesquisa, sessões e arquivos históricos**. Ele não deve ser promovido automaticamente para fonte principal do app Android, compilador ou runtime sem triagem, validação e comparação de diffs.

## Estado operacional

`Livro/` contém materiais de natureza diferente:

- capítulos e textos de pesquisa;
- whitepapers, metodologia e reprodutibilidade;
- scripts e protótipos ligados ao fluxo `FALA → FONEMA → TOKEN → AST → BYTECODE → ASM → VM → OUTPUT`;
- zips, tarballs e bundles de sessão;
- arquivos brutos `.txt` vindos de exploração, conversa, experimento ou consolidação.

A regra deste diretório é simples:

```text
arquivo solto não vira fonte de verdade por nome bonito
zip não vira release por estar completo
texto histórico não vira implementação sem teste
gap declarado vale mais que claim inventado
TOKEN_VAZIO é saída válida quando não houver evidência local
```

## Layout recomendado

A refatoração segura deve convergir para esta organização:

```text
Livro/
├── README.md
├── REFATORACAO_ARQUIVOS.md
├── _archives/
│   ├── sessions/      # snapshots de sessão, zips completos, pacotes recebidos
│   ├── bundles/       # rafaelia_bundle_v*.tar.gz e pacotes versionados
│   └── reports/       # relatórios gerados por varredura/refatoração
├── 01_capitulos/      # capítulos e narrativa estruturada
├── 02_pesquisa/       # whitepapers, metodologia, reprodutibilidade e análises
├── 03_compiladores/   # scripts/protótipos que podem ser executados ou testados
└── 04_raw/            # textos brutos aguardando curadoria
```

Este layout é uma **direção de curadoria**, não autorização para mover tudo sem diff. Arquivos ativos usados por scripts, documentação ou CI devem permanecer no caminho atual até que referências internas sejam atualizadas.

## Regra para zips e tarballs

Todo `.zip`, `.tar.gz`, `.tgz`, `.7z` ou pacote compactado deve passar por este fluxo antes de extração ou promoção:

```text
1. verificar integridade
2. listar conteúdo sem extrair no workspace principal
3. extrair somente em diretório temporário
4. comparar com arquivos já existentes
5. gerar relatório
6. só então promover algo para fonte ativa
```

Comandos seguros:

```bash
unzip -t arquivo.zip
unzip -l arquivo.zip
tar -tzf arquivo.tar.gz
```

Nunca extrair pacote histórico diretamente sobre o repositório sem branch, diff e rollback.

## Script de apoio

Use o script de refatoração em modo de checagem:

```bash
bash scripts/refactor_livro_archives.sh --check
```

Para aplicar apenas os movimentos seguros já declarados no script:

```bash
bash scripts/refactor_livro_archives.sh --apply
```

O modo `--apply` move somente itens conhecidos e tratados como arquivo histórico. Ele não promove conteúdo compactado, não extrai zip e não sobrescreve arquivos existentes.

## Fonte ativa versus arquivo histórico

| Classe | Tratamento |
| --- | --- |
| Script executável | validar com `bash -n`, `--check` ou teste equivalente antes de mover |
| Markdown técnico | revisar links internos antes de renomear |
| Texto bruto | classificar como `04_raw` ou converter em documento curado |
| Zip/tarball | manter em `_archives`, validar integridade e listar conteúdo |
| Protótipo `.py`, `.c`, `.sh` | manter como experimento até ter comando, teste e saída auditável |

## Critério de promoção

Um arquivo só sai de histórico/experimental para ativo quando tiver:

- função clara;
- comando de validação;
- dependências declaradas;
- ausência de `TODO`, `STUB` e `PLACEHOLDER` críticos;
- saída reproduzível;
- documentação apontando para o caminho novo.

Sem isso, fica como contexto, snapshot ou `TOKEN_VAZIO` operacional.
