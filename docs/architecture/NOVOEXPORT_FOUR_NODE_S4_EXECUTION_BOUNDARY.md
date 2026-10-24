# NOVOexport × RafGitTools — execução coordenada S4 (sucessor, sem promoção)

Estado: `SOURCE_IMPLEMENTED_UNTESTED_EXACT_HEAD`, sem execução física de Android nem resync bilateral automático.
Este documento complementa `NOVOEXPORT_FOUR_NODE_HANDOFF_V1.md`; não altera o AGENTS.md, a autenticação ou a fonte original.

## Contratos presentes

- Produto privado `CONVERSATIONS_CHUNKS_PRIVATE`: importador de snapshots com PIN de blob Git, SQLite derivado e suíte de falsificadores sintéticos.
- Consumidor público `RafGitTools`: `contracts/novoexport-four-node-handoff.v1.schema.json`, `scripts/novoexport_handoff_gate.py` e `tests/test_novoexport_handoff_gate.py`.
- O celular continua responsável pelo futuro canário real (árvore SAF selecionada); `NovoexportSafInventory` e `NovoexportLibraryCatalogComposer` fornecem a fonte de metadados sem abrir bytes.
- Drive continua domínio de custódia documental e receipts; não é um segundo executor GitHub.

## Entrada mínima do consumidor

RafGitTools valida um envelope `PLAN_ONLY`, vinculado a SHA exato do produtor e do executor, e aprova apenas a estrutura de uma proposta. Nenhum resultado do validador executa uma ação nem concede direito de escrita.

```sh
python3 scripts/novoexport_handoff_gate.py /caminho/seguro/para/plano.json
python3 -m unittest tests.test_novoexport_handoff_gate -v
```

O JSON do plano permanece no espaço privado apropriado, sem incluir URI `content://`, tokens, conteúdo das conversas ou dados sensíveis. O consumidor público não recebe o inventário privado de nomes.

## CI observado, não extrapolado

A tentativa de orquestração no produtor para o HEAD anterior `6d29dc3a9c518168541f5d7122f7430f872be0ff` retornou RUN #40049216304 `failure`, job sem steps e logs indisponíveis no conector. A nova execução da suíte do produtor precisa ser inspecionada individualmente em sua revisão exata. Não atribuir falha ao código sem logs.
A existência de `tests/test_novoexport_handoff_gate.py` não substitui executar/validar testes no HEAD desta branch.
Nenhum CI de Android ou teste físico aqui foi observado.

## Transição permitida

```text
SOURCE_METADATA
→ IMMUTABLE_SNAPSHOT_PIN
→ CANDIDATE_NAME_EDGES
→ PLAN_ONLY
→ HUMAN_APPROVAL_OF_EXACT_PRIVATE_DESTINATION
→ ONE_FILE_ANDROID_CANARY
→ PROVIDER_READBACK
→ DRIVE_RECEIPT
→ R3
```

`SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM`.
`TOKEN_VAZIO != 0`. `claim_allowed=false`.
Nenhuma PR mesclada, nenhum secret recuperado, nenhuma publicação de dados de corpus pelo consumidor público.
