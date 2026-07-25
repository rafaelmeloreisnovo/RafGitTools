# Excelência Operacional — RafGitTools

Este diretório define as melhores práticas do plano de controle RAFAELIA.

O RafGitTools não deve executar processamento pesado diretamente. Sua função canônica é:

```text
intenção
-> capacidade declarada
-> autorização
-> job limitado
-> acompanhamento por eventos
-> evidência
-> decisão humana ou automática permitida
```

## Invariante do plano de controle

```text
identidade + contrato + fronteira + autorização
+ evento + evidência + retorno + reversibilidade
```

Se uma ação não possui executor identificado, limites, política, evidência e rollback, ela não deve ser apresentada como botão operacional.

## Responsabilidades

- catálogo de módulos e capacidades;
- gates de governança;
- geração de jobs tipados;
- seleção de fonte sem expor credenciais;
- observação de progresso por `run_id`;
- apresentação de `PASS`, `PASS_LIMITED`, `FAIL`, `BLOCKED` e `TOKEN_VAZIO`;
- ligação entre resultado, artefato e evidência;
- autorização humana para operações mutáveis.

## Fronteira com RafPolimata

```text
RafGitTools governa e roteia.
RafPolimata processa, estrutura e prova.
```

A integração deve ocorrer por contrato estável, nunca por dependência em detalhes internos.

## Documentos

- `PLANO_DE_CONTROLE.md`
- `RUNBOOK_EXCELENCIA_OPERACIONAL.md`
- `CONTRATO_RAFGITTOOLS_RAFPOLIMATA.md`

## Regra de ouro

> Um botão só é real quando existe capacidade observada, gate aprovado, executor disponível, retorno estruturado e evidência vinculada.
