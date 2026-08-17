# Uncertainty, Urgency, Ethics & License by Design — V1

## Invariante

A obra só avança quando a lacuna muda por evidência, não por redação:

```text
incerteza -> autoridade -> providência -> teste -> receipt -> decisão
```

`TOKEN_VAZIO` é um estado útil e auditável. Ele não é zero, falso, `null`, PASS, FAIL nem autorização de claim.

## Três trilhos que não se confundem

1. **Operacional:** execução, CI, hashes, receipts e rollback.
2. **Científico:** dados, likelihood, incerteza, falsificador e replicação.
3. **Direitos/ética:** licença, finalidade, minimização, revisão humana e limites de uso.

Um trilho verde não compensa um trilho ausente.

## Licença sem fricção falsa

A redução de fricção não consiste em presumir permissões. Consiste em classificar cedo:

```text
publicly accessible != public domain
publicly accessible != redistribution permission
publicly accessible != training permission
repository license != automatic third-party dataset license
```

Enquanto não houver grant verificável, uso redistributivo, treino e uso comercial permanecem `false`. Referência por identificador/URL pode continuar quando não copia o payload e a finalidade é governança.

## Ética por design

Toda utilização governada declara finalidade e minimização. Itens de risco alto exigem revisão humana. Ações mutantes exigem caminho de rollback antes da execução. Inferência silenciosa é proibida.

A parábola pode ensinar e indexar, mas não mede:

```text
TOKEN_VAZIO  -> tijolo ainda não encontrado
receipt      -> assinatura da etapa
falsificador -> esquadro que pode reprovar
rollback     -> retorno ao último ponto seguro
```

`parable_evidence_effect = NONE`.

## Microciclos — evidência corrente

A janela observada em 2026-08-17 contém quatro runs consecutivos da `main`, todos `EXECUTED_READ_ONLY`, `latest_four_count=4` e `claim_allowed=false`. A cadeia `previous_entry_sha256 -> entry_sha256` é contínua.

Essa evidência fecha a antiga ausência de navegabilidade 4/4 **neste snapshot**. Não prova ciência, runtime físico ou permanência futura do serviço.

## Urgências científicas preservadas

P0 continua contendo, entre outros:

- joint multi-probe Bayes;
- replicação independente;
- execução física Android/Termux;
- reprodução DESI DR2 official joint/cross-block.

O Dependency Graph deixou de ser vazio genérico: há evidência parcial e escopada em B0/B1, enquanto dependências semânticas externas ao bloco e cinco consumidores executáveis continuam abertos.

## Anti-regressão

O contrato contém 30 invariantes. A validação falha se houver promoção de claim, quebra da cadeia dos microciclos, retirada de um invariante, permissão de licença inferida de acesso público, uso de alto risco sem revisão humana ou evidência positiva sem referências.

## Autoridade

Este contrato vive no `RafGitTools` porque governa evidência, incerteza, direitos e promoção. O `Mapa` continua autoridade do catálogo/ciclo e o RLL continua autoridade dos claims científicos. Não há replicação indiscriminada de lógica entre repositórios.
