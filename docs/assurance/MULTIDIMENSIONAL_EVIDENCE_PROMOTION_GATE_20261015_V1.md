# RafGitTools — Gate de promoção para observação multidimensional (V1)

**Estado:** `DESIGN_ONLY / DRAFT_NO_PROMOTE / claim_allowed=false`  
**Data:** 2026-10-15  
**Autoridade:** RafGitTools — Git/CI/revisão/custódia; não altera física, equações ou conteúdo de fontes externas.

## Não duplicar o produtor

Receber de Mapa somente IDs, proveniência, tipo de relação, pointer imutável, previsão/falsificador e `required_gate`. Papers governa hipótese/risco, Matem-tica- as equações, ChipQuantum experimento determinístico, RafPolimata carrier; o adaptador GitOps **não torna uma hipótese verdadeira** quando um workflow fica verde.

## Unidade de promoção em CI

`{repo,branch,commit_sha,file_blob_sha,producer_authority,source_context,evidence_scope,observer_id,clock_context,relation_type,gate_id,gate_result,previous_receipt,review_ref,provider_enforcement,rollback_ref}`.

Não aceitar `source!=artifact!=execution!=evidence!=claim` como uma única categoria. `token_vazio`, `missing_observation`, `not_applicable`, `failed`, `blocked`, `untested`, `passed` são estados diferentes.

## Algoritmo de decisão proposta (não implementado como CI neste PR)

1. Confirmar repo, branch e SHA atual **exatamente iguais** aos do resultado solicitado.
2. Ler CI completo e testes dedicados relevantes, mas preservar dívida legada separadamente.
3. Conferir license root/proveniência e arquivo(s) novos; não presumir que `LicenseRef` seja licença oficial.
4. Associar cada claim a falsificador, evidência e escopo; simulação, fonte, device e review não herdam PASS entre si.
5. Conferir `reviewed_before_merge` e proteção/enforcement do provedor; `403` significa `TOKEN_VAZIO_AUTHORITY`, não `PASS`.
6. Bloquear se um hard falsifier falhou, se a latência passou deadline crítico sem exceção tipada, ou se a suposta fonte de entropia é apenas pseudorruído, cache ou arquivo lógico.
7. Exigir `rollback_ref` testável e emitir receipt append-only; ausência de evidence registra `HOLD`, nunca retorna o zero da máquina como ausência.
8. PR com fator bloqueante conserva título `NÃO PROMOVER`; jamais executar merge automático.

## Falsificadores transversais

`SAME_NAME != SAME_SEMANTICS`; `SAME_SHA != INDEPENDENT_REPLICATION`; `BLOCK_DETECTED != RECOVERED`; `VARIABLE_BITS != CRYPTO_TRNG`; `LOW_LATENCY != BETTER_DECISION`; `MAPPED_ADDRESS != PHYSICAL_CELL`; `CI_OTHER_SHA != CI_EXACT_HEAD`.

## Critério de sucesso

Só declarar redução de risco quando uma mutação negativa falhar como esperado **e** baseline/HEAD equivalentes passarem no mesmo escopo. O ledger deve guardar divergências, anomalias, regressões, falhas e sucessores; não remover histórico. Este arquivo é uma especificação de consumidor para futura automação, **não a automação**.

**R3:** F_ok=roteamento e regras de gate; F_gap=workflow executável/teste de enforcement e acesso provider; F_next=integrar no verificador canônico existente com fixture negativa e comprovante SHA-bound.
