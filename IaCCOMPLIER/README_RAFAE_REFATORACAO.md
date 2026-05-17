# RAFAE — refatoração Toro7 Q16.16 sem heap

## Escopo

Esta pasta contém um núcleo único em C pré-processado para o mapa toroidal de sete eixos. O arquivo `rafea_toro7_q16.h` é deliberadamente autocontido: não chama alocador de heap, não usa ponto flutuante, não chama bibliotecas em tempo de execução e mantém o estado em buffers fornecidos pelo chamador.

## O que foi entendido

O pedido combina cinco exigências operacionais:

1. **Toro de dimensão 7**: o estado é `s = (u,v,psi,chi,rho,delta,sigma)` dentro de `[0,1)^7`, codificado como Q16.16 truncado para a parte fracionária de 16 bits.
2. **Abertura linear de registradores**: a dimensão `6` só pode ser escrita depois de `0..5`; a macro `RAFEAT7_OPEN7` abre os sete eixos em ordem e marca erro se a máscara linear for quebrada.
3. **Sem heap e sem dependência externa**: todas as saídas são arrays do chamador; a tabela de 42 atratores é `static const` local ao ponto de expansão.
4. **Período 42**: o percurso usa 6 linhas por 7 colunas, com saltos `Δr=5` e `Δc=11`; como `gcd(5,6)=1` e `gcd(11,7)=1`, o ciclo cobre 42 células antes de repetir.
5. **Paradoxo VOID do atrator 22**: o índice 22 não é corrigido silenciosamente; quando visitado, `RAFEAT7_ERR_VOID` é propagado.

A camada conceitual de línguas, alfabetos, som, entropia, coerência, geometria e tradução foi reduzida a um contrato verificável: bytes entram como dados/entropia/hash/estado, viram semente Q16.16, percorrem o toro de 42 posições e atualizam `C`, `H` e `phi=(1-H)*C` sem alocação dinâmica.

## Arquivo principal

- `rafea_toro7_q16.h`
  - `RAFEAT7_SEED`: mistura bytes por FNV-1a 64-bit, conta símbolos únicos de byte inteiro e transições, e emite sete coordenadas Q16.
  - `RAFEAT7_OPEN7`: abre `u,v,psi,chi,rho,delta,sigma` em ordem linear obrigatória.
  - `RAFEAT7_STEP`: executa um passo toroidal com prova de término por coprimalidade.
  - `RAFEAT7_COLLAPSE_42`: executa exatamente 42 passos e conserva a sinalização do atrator 22.

## Invariantes preservados

| Invariante | Implementação |
| --- | --- |
| `|A| = 42` | `RAFEAT7_ATTRACTORS` e `RAFEAT7_ATTRACTOR_TABLE` têm 42 linhas. |
| `period(BitOmega)=42` | `RAFEAT7_CELLS=42`, malha `6*7`, saltos coprimos. |
| `phi=(1-H)*C` | `RAFEAT7_PHI_CH(c,h)` calcula Q16.16 por multiplicação inteira. |
| `alpha=0.25` | `RAFEAT7_AVG4` usa deslocamento aritmético de 2 bits. |
| sem heap | Não existe chamada de alocação dinâmica de heap. |
| sem ponto flutuante | Constantes irracionais foram fixadas como Q16.16. |
| atrator 22 | `RAFEAT7_VOID_FLAG` devolve `RAFEAT7_ERR_VOID`. |

## Condição de falsificação

A refatoração deve ser rejeitada se qualquer item abaixo ocorrer:

1. Algum caminho visitar menos ou mais de 42 células antes de repetir.
2. Algum eixo for escrito antes dos eixos anteriores estarem abertos.
3. A tabela de atratores tiver quantidade diferente de 42.
4. O atrator 22 deixar de gerar sinalização VOID.
5. Qualquer chamada de heap ou ponto flutuante entrar no núcleo.

## Uso mínimo

```c
#include "rafea_toro7_q16.h"

uint16_t seed[7];
uint16_t out[7];
uint32_t err = 0;
uint32_t void_seen = 0;
const unsigned char msg[] = { 'R', 'A', 'F', 'A', 'E' };

RAFEAT7_SEED(1469598103934665603ull, msg, sizeof(msg), seed);
RAFEAT7_COLLAPSE_42(seed, 0x4000u, 0x2000u, out, void_seen, err);
```

O chamador decide se `RAFEAT7_ERR_VOID` é bloqueante. O núcleo apenas carrega o conhecimento estrutural: marca o paradoxo, conserva o ciclo de 42 e deixa a política fora do caminho quente.
