# Resumo e diff — RAFAE Toro7

## Resumo executivo

- Criado núcleo macro-only `rafea_toro7_q16.h` para mapa toroidal de 7 dimensões em Q16.16.
- Removida a necessidade de heap: toda memória vem do chamador ou de constantes locais.
- Mantida abertura linear dos sete eixos: não há escrita do eixo posterior sem confirmação dos anteriores.
- Incluída tabela completa de 42 atratores e sinalização explícita do atrator 22 como VOID.
- Documentadas invariantes, prova de término por `gcd` e condições de falsificação.

## Diff lógico

```diff
+ IaCCOMPLIER/rafea_toro7_q16.h
+   núcleo autocontido em C por macros
+   Q16.16 inteiro, sem ponto flutuante
+   sem alocação dinâmica de heap
+   percurso toroidal 6x7 com 42 passos
+   tabela de 42 atratores
+   VOID #22 sinalizado, não corrigido silenciosamente
+
+ IaCCOMPLIER/README_RAFAE_REFATORACAO.md
+   documentação completa do contrato
+   explicação do que foi entendido
+   invariantes e falsificação
+   exemplo mínimo de uso
+
+ IaCCOMPLIER/RAFAE_RESUMO_E_DIFF.md
+   resumo solicitado
+   diff lógico da refatoração
```

## Conferência rápida

- Pasta tocada: somente `IaCCOMPLIER/`.
- Dependências externas: nenhuma além de cabeçalhos C padrão de tipos inteiros.
- Estado global mutável: nenhum.
- Política de VOID: visível por bit de erro.
