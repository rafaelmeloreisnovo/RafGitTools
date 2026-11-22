# RAFAELIA — Compatibilidade de contratos federados (27/10/2026)

> Auditoria de contratos JSON Schema de **fonte**, não execução de consumidores. Os seis nomes de família não demonstram interfaces equivalentes. Documento mantido no produtor do middleware RafGitTools, sem modificar esquema algum ou transferir dados entre repositórios.

## Cabeçalhos de origem fixos

- `rafaelmeloreisnovo/RafGitTools` @ `0c6bbb32acda3a729652e1fb2559a5976dd0a86c` (Git tree não truncada).
- `rafaelmeloreisnovo/termux-app-rafacodephi` @ `a7f7d60d04d1b445bbf24e173b494a4466bd4922` (Git tree não truncada).
- `rafaelmeloreisnovo/llamaRafaelia` @ `89d6fc0ced59b172c6af6060ccfb11fa244e16be` (Git tree não truncada).

## Matriz de compatibilidade dos campos obrigatórios

| Nome | RafGitTools | Termux App | llamaRafaelia | Situação |
|---|---|---|---|---|
| `context_bundle` | `schema,bundle_id,chunks,created_at` | `bundle_id,chunk_refs,assembled_at,working_directory,active_repos` | `schema,bundle_id,conversation_chunks,generated_at` | `REQUIRES_ADAPTER` |
| `tool_request` | `schema,request_id,intent_id,capability,payload` | `request_id,intent_id,command,args,working_directory,capability,read_only` | `schema,request_id,intent_id,capability,args` | `REQUIRES_ADAPTER` |
| `execution_plan` | `schema,plan_id,intent_id,steps,compiled` | `plan_id,intent_id,gate_decision,steps,created_at` | `schema,plan_id,intent_id,created_at,commands` | `REQUIRES_ADAPTER` |
| `execution_result` | `schema,intent_id,executed_command,args,working_directory,started_at,ended_at,exit_code,stdout_truncated,stderr_truncated,stdout_sha256,stderr_sha256,artifacts,final_state,rollback_available,source_chunk_refs` | `result_id,intent_id,executed_command,args,working_directory,started_at,ended_at,exit_code,stdout_truncated,stderr_truncated,stdout_sha256,stderr_sha256,artifacts,final_state,rollback_available,source_chunk_refs` | `schema,intent_id,executed_command,args,working_directory,started_at,ended_at,exit_code,stdout_truncated,stderr_truncated,stdout_sha256,stderr_sha256,artifacts,final_state,rollback_available,source_chunk_refs` | `CONDITIONAL_NO_PROOF` |
| `intent_ir` | `schema,intent_id,action,target,inputs,constraints,evidence_refs,requested_capabilities,risk,execution_gate` | `schema,intent_id,action,target,inputs,constraints,evidence_refs,requested_capabilities,risk,execution_gate` | `schema,intent_id,action,target,inputs,constraints,evidence_refs,requested_capabilities,risk,execution_gate` | `REQUIRED_MATCH_ONLY` |
| `conversation_chunk` | `schema,chunk_id,source,content` | `chunk_id,source_repo,sequence_index,content,created_at` | `schema,chunk_id,conversation_id,content,created_at` | `REQUIRES_ADAPTER` |

## Identidades Git individuais dos esquemas consultados

| Família | RafGitTools blob | Termux App blob | llamaRafaelia blob |
|---|---|---|---|
| `context_bundle` | `38857bb6564fccb98648e1de8de1684f9090f3b7` | `b184d03cd01d0bddcf322c85b9be1b7d0662a08d` | `3af8c7990a5db018d2f17610db1e4bab4ddbaa82` |
| `tool_request` | `4e610eb38213a46e066a937e936095e0d9bca23f` | `4cc8779353b106ad4c5c95a42154f5f16ab00f39` | `1875da1646b03592e35ef57021f34e9449686f2b` |
| `execution_plan` | `f073b87389f3ab3852a60611e66fa97e4ad86606` | `b2ca1b2635c34193827baadff6407614540e56d5` | `835bd4e5eb6ed54a125dba66b1768bdec1a6976e` |
| `execution_result` | `935cc2b2a7ebb7ed0701a5501492eda4135cdff9` | `cb4ddbfc0d6daa05d376c9e3c938d55be2957b84` | `c00a0c286729ddcce288747f67057776235e3bda` |
| `intent_ir` | `137bbebf124c332a3a760ac312753fc5f32ca2bc` | `c4458596d3b1abe34807e8696c70b9a5d1d61483` | `13ee7ac3da597635b56d992f1ddb162a6095fa8b` |
| `conversation_chunk` | `afaee0b79b5f441e4c70791221585a2f438f12e5` | `91715df6fddba1d702aeaa17c53278a786388850` | `d2d535d656268aca1d95d6b9aae6bade3422d554` |

Todos os arquivos acima têm caminho `docs/contracts/<família>.schema.json` nos respectivos HEADs. A identidade e `required` são observados no JSON; validadores de JSON Schema e round-trip real **não foram executados**. Mesmo `REQUIRED_MATCH_ONLY` não prova compatibilidade de `type`, `additionalProperties`, `$ref`, `enum`, campos opcionais, normalização de Unicode ou governança.

## Recomendações e prioridades

1. **P0 — rotas que executam ações:** `tool_request` e `execution_plan` devem declarar explicitamente direção e perfil de adaptação, sem promoção automática. No Termux App o request exige `command,args,working_directory,read_only`, que não existem como campos obrigatórios equivalentes em RafGitTools.
2. **P0 — fronteira de informação:** `context_bundle` usa `chunks`, `chunk_refs` e `conversation_chunks` respectivamente. Não copiar dados privados entre formas; selecionar referências, minimização e consentimento.
3. **P1 — resultado:** Termux App exige `result_id` em vez de `schema`, e inclui campos adicionais de recibos. É preciso definir mapeamento e campos que não podem ser inferidos.
4. **P1 — hipótese próxima:** `intent_ir` tem required iguais, mas ainda precisa validar formatos completos e casos negativos.
5. **P1 — identidade:** `conversation_chunk` tem chaves de origem e timestamps distintas. Serializar com proveniência explícita, sem inventar chunks.

## Contratos de governança e hardware relacionados

- RafGitTools `contracts/documentation_no_regression.v1.json`: valida apenas arquivos Markdown modificados; não declara saneada toda a dívida pré-existente.
- RafGitTools `contracts/MAIN_PROVIDER_ENFORCEMENT_PLAN_20260927.v4.json`: stage1 contratual tem `human_review_required=false`, `minimum_approving_reviews=0`; stage2 registra `TOKEN_VAZIO_INDEPENDENT_REVIEWER`. Isso é **declaração de plano**, não leitura das regras reais no provedor. Não promover sem revisão independente.
- `rafaelia-human-ai-adapter.v1.json` em Termux App, llamaRafaelia, GAIA_phi e Rafaelia_Private: mesmo `schema=raf.human-ai.adapter.v1`, mas `role` e efeitos permitidos deliberadamente diferem. Não igualar permissões.
- Vectras `docs/contracts/VECTRAS_TERMUX_IPC_V3.json` e Termux App `docs/contracts/VECTRAS_TERMUX_PROVIDER_V3.json` formam um par consumidor/provedor. A origem aponta para baselines históricos com `refresh_required`, não para prova física atual.
- llamaRafaelia `contracts/raf_ethica.h` descreve `uint64_t layers[7]` com comentário `7 x 64 = 42 bits`, aritmeticamente incorreto; documentação deve corrigir a afirmação sem presumir alteração de layout ou contagem de estados.
- GAIA_phi `schemas/context-geometric-invariant-v1.schema.json`: ausência de evidência implica rejeição operacional, não falsidade científica.

## Aceitação e rollback

Exigir par `producer_schema_sha / consumer_schema_sha / adapter_sha`, fixture positiva e negativa, falha fechada, avaliação de dados sensíveis, logs minimizados, execução em HEAD exato e recibo independente antes de declarar interoperabilidade. Modelo de revisão humana e rollback preservado.

**F_ok:** 18 esquemas inspecionados e seis famílias comparadas pelos campos `required`; **F_gap:** sem testes de validação nem consumidores executados; **F_next:** implantar teste de compatibilidade bidirecional e versões de adapter sem mudar legado. `claim_allowed=false` / `PROMOTION=HOLD`.
