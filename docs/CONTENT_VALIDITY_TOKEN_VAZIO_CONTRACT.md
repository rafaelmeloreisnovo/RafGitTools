# Contrato de Validade de Conteúdo, Pesos, Tensores e TOKEN_VAZIO

Status: `METHOD_DEFINED`  
Autoridade canônica: `rafaelmeloreisnovo/RafGitTools`  
Contrato legível por máquina: `configs/content_validity_contract.json`

## 1. Problema

Um sistema pode preservar palavras e ainda perder o conteúdo. Isso acontece quando a
tokenização elimina negações, separa unidades de valores, mistura fontes, remove
condições, trata metáfora como fato ou converte ausência em zero.

Este contrato define uma cadeia mínima para que cada unidade de conteúdo seja
classificada sem fabricação:

```text
fonte
→ token semântico
→ âncora
→ classificação
→ janela
→ tensor de sustentação
→ contradição/falsificador
→ estado de validade
→ custódia
```

Ele não afirma que todo token será validado. Ele garante que o sistema consiga
distinguir o que está sustentado, limitado, contradito, bloqueado ou ausente.

## 2. TOKEN_VAZIO é útil e válido

`TOKEN_VAZIO` é um estado epistemológico válido para uma informação necessária que
está ausente, não demonstrada ou deliberadamente não inferida.

```text
TOKEN_VAZIO ≠ 0
TOKEN_VAZIO ≠ false
TOKEN_VAZIO ≠ null genérico
TOKEN_VAZIO ≠ falha de parser
TOKEN_VAZIO ≠ sucesso
TOKEN_VAZIO ≠ prova de inexistência
```

Um registro `TOKEN_VAZIO` é válido quando possui:

- campo afetado;
- razão concreta;
- owner;
- próxima ação;
- critério de saída.

Portanto, há duas perguntas separadas:

1. o registro da ausência é válido?
2. o conteúdo ausente já pode ser promovido?

A primeira pode ser `sim`; a segunda permanece `não`.

## 3. Token semântico

Token não significa apenas fragmento produzido por um tokenizer de modelo. Aqui ele
é uma unidade mínima de significado preservável:

- definição;
- claim;
- condição;
- negação;
- exceção;
- metáfora;
- fórmula;
- unidade;
- parâmetro;
- resultado;
- método;
- lacuna.

Cada token registra texto bruto, texto normalizado, fonte, domínio, uso pretendido,
evidência e características semânticas.

A normalização deve preservar explicitamente:

```text
negação
modalidade
condições
exceções
números
unidades
```

Uma frase como “o resultado não demonstra superioridade” não pode ser comprimida
para “resultado demonstra superioridade”. A negação é uma invariante do conteúdo.

## 4. Janela de tokenização

Uma janela é um recipiente operacional, não uma fonte de verdade.

Cada janela declara:

- fontes;
- tokens em ordem;
- máximo de tokens;
- overlap;
- contagem observada;
- política de truncamento;
- ponte explícita quando mistura fontes.

Não existe tamanho universal de janela neste contrato:

```text
default_max_tokens = TOKEN_VAZIO
default_overlap_tokens = TOKEN_VAZIO
```

Esses valores pertencem ao perfil de runtime: tokenizer, modelo, contexto, memória e
objetivo. Inventar um número genérico seria uma falsa calibração.

## 5. Tensor de sustentação

O suporte é representado por um tensor esparso:

\[
T_{iwd}
\]

onde:

- \(i\): token;
- \(w\): janela;
- \(d\): dimensão de sustentação.

As oito dimensões são:

| ID | Dimensão | Pergunta |
|---|---|---|
| D01 | Proveniência | De onde veio e qual é sua custódia? |
| D02 | Fidelidade semântica | O significado e suas restrições foram preservados? |
| D03 | Adequação de domínio | Onde vale e onde não vale? |
| D04 | Consistência formal | Lógica, dimensões e derivação são coerentes? |
| D05 | Sustentação empírica | Existem dados, incerteza e falsificador? |
| D06 | Reprodutibilidade | Há comando, versão, ambiente, teste e artefato? |
| D07 | Validade temporal | Quando foi observado e quando expira? |
| D08 | Governança | Há autoridade, privacidade, gate e rollback? |

Um peso numérico pertence ao intervalo \([0,1]\) e exige evidência referenciada.
Ele não é probabilidade de verdade, relevância aprendida ou opinião do modelo.

## 6. Regra não compensatória

A cobertura de um token é:

\[
C_i=\frac{|K_i|}{|R_i|}
\]

onde \(R_i\) é o conjunto de dimensões obrigatórias e \(K_i\) o subconjunto
conhecido.

A qualidade mínima é:

\[
Q_i=\min_{d\in R_i} T_{iwd}
\]

somente quando todas as dimensões obrigatórias são conhecidas. Caso contrário:

\[
Q_i=\mathrm{TOKEN\_VAZIO}
\]

A média não é usada para promoção. Um excelente hash não compensa falta de fonte;
um texto elegante não compensa ausência de teste; alta reprodutibilidade não
compensa uma equação dimensionalmente inválida.

## 7. Estados de promoção

```text
RAW
→ ANCHORED
→ CLASSIFIED
→ WEIGHTED
→ WINDOWED
→ CROSS_CHECKED
→ VALID_LIMITED
→ VALID
```

Estados laterais preservados:

```text
TOKEN_VAZIO
CONTRADICTION
BLOCKED
REJECTED
```

`VALID` significa válido para o uso e domínio declarados, no intervalo temporal
registrado. Não significa verdade universal.

`VALID_LIMITED` exige todas as dimensões obrigatórias, mas mantém limites explícitos
de domínio, dataset, ambiente ou generalização.

## 8. Regras por tipo de conteúdo

### Claim

Exige fonte, fidelidade semântica, domínio, formalização, evidência empírica,
reprodutibilidade, validade temporal e governança.

### Fórmula

Exige origem, símbolos, unidades, derivação, domínio, implementação ou teste e
limites de aplicação.

### Resultado numérico

Exige entrada, comando, configuração, seed quando aplicável, ambiente, tolerância,
saída, hash e baseline.

### Metáfora

Pode ser `VALID` como recurso didático. Não pode, por esse motivo, ser promovida a
claim física.

### Lacuna

Pode ser um `TOKEN_VAZIO` perfeitamente válido, desde que a ausência seja rastreável
e possua critério de fechamento.

## 9. Relação com as 30 camadas Ω

O contrato não substitui as trinta camadas; ele fornece uma projeção token/janela:

| Tensor | Camadas relacionadas |
|---|---|
| D01 | S01, S11, S12, S13 |
| D02 | S02, S03, S04, S29 |
| D03 | S05, S30 |
| D04 | S06–S10 |
| D05 | S09–S11, S24 |
| D06 | S14–S18, S24, S25 |
| D07 | S12, S27 |
| D08 | S19–S23, S26–S28 |

A relação é de rastreabilidade, não de equivalência matemática.

## 10. Heurísticas de observação

1. **Preservar antes de resumir.** O trecho original entra antes da compressão.
2. **Quebrar sem amputar.** Claims atômicas conservam condições e negações.
3. **Não misturar fontes silenciosamente.** Toda janela cross-source exige ponte.
4. **Separar importância de validade.** Um token pode ser importante e continuar
   `TOKEN_VAZIO`.
5. **Separar peso de verdade.** Peso mede sustentação ligada a evidência.
6. **Não calcular sobre ausência.** `TOKEN_VAZIO` nunca entra como zero em média,
   produto, softmax ou score.
7. **Usar o mínimo obrigatório.** Promoção depende do elo mais fraco aplicável.
8. **Preservar contradição.** Conflito aberto bloqueia `VALID`.
9. **Declarar o relógio.** Conteúdo atual pode se tornar obsoleto.
10. **Fechar com próxima ação.** Ausência sem owner e critério de saída é apenas
    abandono, não governança.

## 11. Uso

```bash
python3 scripts/content_validity_contract.py validate-contract \
  configs/content_validity_contract.json

python3 scripts/content_validity_contract.py validate-manifest \
  configs/content_validity_contract.json \
  examples/content_validity.example.json

python3 scripts/content_validity_contract.py summarize \
  configs/content_validity_contract.json \
  examples/content_validity.example.json
```

## 12. Limites

O contrato prova estrutura e rastreabilidade. Ele não prova automaticamente:

- correção científica;
- completude universal das dimensões;
- qualidade de um tokenizer específico;
- valor ótimo de janela ou overlap;
- validade externa de pesos;
- ausência de vieses no conteúdo de origem.

Esses pontos permanecem sujeitos a testes, revisão e novos `TOKEN_VAZIO`.
