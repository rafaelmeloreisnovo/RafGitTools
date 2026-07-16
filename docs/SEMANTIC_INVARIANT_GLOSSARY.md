# Glossário semântico de invariantes — RAFAELIA / Ω

Status: `SEMANTIC_CONTRACT`

Este glossário preserva o sentido dos símbolos usados no workflow. Ele não
converte símbolos em leis físicas e não atribui validade científica por
semelhança verbal.

## Símbolos do ciclo

| Símbolo | Nome operacional | Significado no workflow |
|---|---|---|
| ψ | intenção | pergunta, desejo, hipótese inicial ou problema |
| χ | observação | leitura de fontes e aquisição de evidência |
| ρ | ruído | erro, ambiguidade, contradição, latência ou lacuna |
| Δ | transformação | mudança controlada do estado anterior |
| Σ | custódia | soma versionada, memória e rastreabilidade |
| Ω | fechamento provisório | síntese limitada pelas evidências atuais |
| ψ′ | reabertura | novo ciclo incorporando o delta anterior |

## Termos canônicos

### Invariante

Propriedade que permanece preservada sob uma família **declarada** de
transformações. Sem declarar a transformação, “invariante” é apenas uma
intenção sem domínio.

### Invariante geométrica

Relação preservada por transformações geométricas especificadas. Pode ser
exata, aproximada ou restrita a uma classe de objetos. A palavra não autoriza
transferência automática para física, biologia ou cognição.

### Multidimensional

Estrutura com múltiplas coordenadas, variáveis, domínios ou camadas. Não
significa, por si só, dimensão física adicional.

### Fractal

Padrão com recorrência ou auto-semelhança em escalas declaradas. Uso metafórico
de “fractal” deve ser marcado como símbolo; uso matemático requer definição da
transformação de escala e medida.

### Ômega / Ω

No workflow, representa fechamento provisório com custódia e limite explícito.
Não significa verdade final. O próximo estado é sempre \(ψ′\).

### Antiderivada do projeto

Metáfora operacional para o acúmulo rastreável dos deltas de cada revisão:

\[
K_n = K_0 + \sum_{i=1}^{n}\Delta_i
\]

Não é uma integral física; é um modelo de memória versionada.

### TOKEN_VAZIO

Valor epistemológico para informação ausente, não demonstrada ou
intencionalmente não inferida. Não é zero, `null` genérico, sucesso, negação ou
prova de inexistência.

### Claim

Afirmação atômica capaz de receber domínio, estado, evidência, teste,
contradição e histórico de promoção.

### Evidência

Objeto observável e referenciável: arquivo, commit, dado, log, derivação,
execução, medição ou publicação. Evidência pode sustentar uma claim sem
demonstrá-la universalmente.

### Método

Procedimento definido e repetível. `METHOD_DEFINED` não equivale a
`EVIDENCE_LINKED`.

### Hipótese

Relação proposta que admite ao menos um falsificador. Sem falsificador,
permanece declaração exploratória.

### Determinismo tecnológico

Mesma entrada, configuração, versão e ambiente declarado produzem a mesma
saída, ou saída dentro de tolerância previamente fixada. Determinismo local não
prova validade externa.

### Coerência

Ausência de conflito interno sob o conjunto de regras adotado. Um sistema pode
ser coerente e ainda estar empiricamente errado.

### Transdisciplinar

Transferência controlada de estrutura entre domínios. Deve declarar:

```text
origem → propriedade preservada → propriedade alterada → hipótese → teste
```

Sem isso, a transferência permanece analogia.

### Pedaço semântico

Unidade mínima de significado preservada antes da formalização: termo,
relação, negação, condição, exceção ou metáfora. Pedaços semânticos recebem
âncoras estáveis para não desaparecerem na normalização.

## Regra de não promoção silenciosa

```text
símbolo ≠ hipótese
hipótese ≠ método
método ≠ execução
execução ≠ evidência externa
evidência ≠ replicação
replicação ≠ verdade universal
```
