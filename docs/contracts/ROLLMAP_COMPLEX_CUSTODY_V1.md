# RAFAELIA Ω | Roll-map de redes estruturais e custódia V1

**Classe:** contrato de execução do RafGitTools; **estado:** IMPLEMENTED_UNTESTED remoto; **claim_allowed:** false.
**Autoridade:** RafGitTools possui o indexador local e os validadores; RafPolimata possui interpretação tipada/codec de rotas; Mapa continua sendo a autoridade de roteamento federado. Google Drive preserva a custódia de origem. Este documento nao move ou modifica fonte externa.

## 1. Unidade de observação

Toda entrada de diretório, arquivo, ponteiro, arquivo vazio, arquivo compactado ou membro virtual de contêiner pode produzir um nó. **A unidade de descoberta não é um diretório específico**: o escopo é a árvore de profundidade variável, percorrida sob um orçamento declarado e com continuação/paginação quando o provedor o exigir.

Modelo mínimo, separando atributos observados e interpretações:

```text
NODE: stable_provider_id | source | parent_id | relative_path | raw_name
      display_name | kind | size_bytes[0|positive|TOKEN_VAZIO]
      mime | provider_revision | observed_at | privacy_class
      digest_kind | digest_value[TOKEN_VAZIO] | evidence_ref
EDGE: from | relation_type | to | evidence | status | scope
```

- A semântica de uma busca pode resultar da **composição de várias entradas**: filhos, descendentes, ancestrais, irmãos, nomes repetidos, diferentes níveis, partes ordenadas de hash ou caminhos alternativos.
- `AND` e `OR` atuam sobre conjuntos de nós; `HAS_CHILD(...)` e `HAS_DESC(...)` fazem **junção estrutural**, e cláusulas independentes podem ser satisfeitas por filhos distintos.
- O mesmo conteúdo pode ter várias aparições; aparição != conteúdo != identidade == autoria; links semânticos são tipados, com origem, justificativa e eventual falsificador.
- Metadados de provedor (ID, parent, MIME, tamanho, revisão) são separados de conteúdo lido. Um nome significativo é um *hint*; não credencial nem garantia de integridade.
- Um arquivo 0 byte é evidência de objeto observado, não valor booleano zero, nem prova de uma operação concluída. Tamanho desconhecido continua TOKEN_VAZIO.

## 2. Exemplos de consultas estruturais

```sh
python3 tools/rollmap/rafaelia_rollmap_v2.py build \
  --root "$HOME/storage/shared/RAFAELIA" \
  --out /tmp/rollmap-derived.sqlite
python3 tools/rollmap/rafaelia_rollmap_v2.py query \
  --db /tmp/rollmap-derived.sqlite \
  --expr 'HAS_DESC(name:*manifest*) AND HAS_DESC(name:*receipt*)' \
  --expand -1 --direction both
python3 tools/rollmap/rafaelia_rollmap_v2.py query \
  --db /tmp/rollmap-derived.sqlite \
  --expr '(path:*Ω* OR path:*Γ*) AND NOT size=0'
```

`--expand -1` alcança todo o **grafo local já indexado**; não concede novas permissões no Drive, não resolve cursores remotos e não valida a árvore mundial. Consultas sem leitura de bytes são rápidas, mas evidência derivada de hash exige acesso legítimo ao conteúdo e verificação.

## 3. Semântica das rotas multidimensionais

Uma rota pode ser encontrada por:
- `TREE/PATH`: pai, filho, descendente, antepassado, irmão, contêiner interno, referência cruzada;
- `BYTES`: 0, tamanho positivo ou desconhecido; cabeçalho permitido; hash completo ou fragmento;
- `TEMPORAL`: observed_at, source_time, predecessor, sucessor, snapshot;
- `SYMBOL/UNICODE`: nome byte-exato preservado + chave NFC/casefold para pesquisa; colisões e normalização conflitante não são resolvidas por suposição;
- `SEMANTIC`: `ALIAS_CANDIDATE`, `DERIVED_FROM_DECLARED`, `ANALOGY_CANDIDATE`, `AUTHORITY_EVIDENCED`, sempre com proveniência por aresta.

Para fragmentos separados ao longo de subdiretórios, **não é válido reconstruir um SHA-256 ou inferir igualdade por permutação sem esquema de fragmentação, ordem, largura e bytes validados**. Contratos futuros podem emitir `HASH_PIECE_CANDIDATE` com ordem/versionamento; no motor inicial só há busca segura de subcadeias hexadecimais >=8 caracteres. A presença de um hash em nome/cabeçalho não comprova o conteúdo referenciado.

## 4. Custódia e rollback

Drive é **fonte remota com ACL**, não diretório POSIX. O indexador opera sobre dados legitimamente materializados e uma amostra de proveniência; nunca acessa credenciais de terceiro nem escreve nos produtores.

Operações futuras de escrita exigem:
```text
INTENT -> SOURCE@revision -> ACL/approval -> writer lease/fencing
 -> snapshot/predecessor -> typed gates -> operation -> readback
 -> producer receipt -> consumer receipt -> Mapa edge -> Drive checkpoint
```

Se faltar um gate: `STOPWALK_BLOCKED + TOKEN_VAZIO(reason)`; preservar checkpoint anterior. Rollback é sucessor append-only, não apagamento. Hash de diretório = digest de um **manifesto ordenado de observação**, não raiz Merkle autenticada do provedor. Assinatura, testemunha independente e garantias transacionais distribuídas continuam fora do escopo.

## 5. Limites e falsificadores

- Os nomes não devem conter tokens, contas privadas, localização/PII, hashes correlacionáveis sensíveis ou payload de corpus em branches públicas.
- `UTF-8` e símbolos são suportados em metadados e consultas; portabilidade depende do provider e do filesystem, com separador/escape/normalização seguros.
- ZIP/TAR são indexados sem extração e com orçamento de bytes; symlinks não são seguidos; caminhos `..` de contêiner são rejeitados; profundidade operacional pode ser limitada por orçamento e dispositivo.
- Verificação de hashes baseada em bytes reais é distinta de hashes reportados, fragmentos, SHA de objeto Git e IDs.
- O índice SQLite é derivado/descartável. **Não é o log de custódia**, nem substitui fonte, recibos ou snapshots.

## 6. Rota do contrato

Código: `tools/rollmap/rafaelia_rollmap_v2.py`.
Testes sintéticos: `tools/rollmap/test_rollmap_contract.py`.
RafPolimata publica o contrato complementar de tipos/interpretacao em `contracts/rollmap-typed-path-v1.json` e `docs/ROLLMAP_TYPED_PATH_CONTRACT_V1.md`; este vínculo é proposto e só será considerado integrado após producer/consumer exact-HEAD e readback.

**R3:** F_ok=artefato isolado e tests sintéticos locais; F_gap=adapter paginado do Drive, provas distribuídas e runtime Android TOKEN_VAZIO; F_next=validar compatibilidade na HEAD exata dos dois produtores antes de qualquer promoção.
