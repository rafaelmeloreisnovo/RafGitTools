# LOVE_NEIGHBOR_EXEGESIS_ROUTE_V1

## Função

Rota gradual para leitura humano/IA de textos do Novo Testamento quando a intenção cruza ética, linguagem, história e hipótese teológica.

Esta rota não define doutrina e não transforma interpretação em fato histórico.

```text
TEXT != VARIANT != HISTORY != THEOLOGY != ETHICAL_APPLICATION
TOKEN_VAZIO != 0
```

## Entrada ética

Ponto textual de maior estabilidade para esta rota:

```text
AMAR_A_DEUS -> AMAR_O_PROXIMO_COMO_A_SI_MESMO -> ATO
```

Referências: Marcos 12:29-31; Levítico 19:18; paralelos em Mateus 22:37-40 e Lucas 10:25-37.

## Gradiente de leitura

A navegação deve seguir esta ordem e parar quando a evidência acabar:

1. `TEXT` — o que a testemunha textual efetivamente contém.
2. `LANGUAGE` — forma grega/hebraica/aramaica observável e sua gramática.
3. `VARIANT` — variantes manuscritas relevantes e grau de incerteza.
4. `HISTORY` — o que possui ou não possui apoio histórico externo.
5. `THEOLOGY` — interpretação de significado, explicitamente marcada como interpretação.
6. `ETHICAL_APPLICATION` — aplicação ao presente, sem retroprojetá-la como fato histórico.

## Caso: Judas, beijo e entrega

### TEXT

Lucas 22:48 contém a pergunta de Jesus a Judas sobre entregar/trair o Filho do Homem com um beijo.

### LANGUAGE

O verbo grego em Lucas 22:48 é `παραδίδως` (`paradidōs`), presente indicativo ativo, segunda pessoa do singular: semanticamente, "estás entregando/traindo?".

Portanto:

```text
GRAMMAR_CLAIM = PRESENT_ACTION
TIMELESS_BEFORE_NOW_ALWAYS = THEOLOGICAL_INTERPRETATION
```

A leitura `ANTES -> AGORA -> SEMPRE` pode funcionar como estrutura espiritual/metafísica da rota, mas não deve ser apresentada como conjugação literal do verbo grego.

### CONTEXT

Os evangelhos também preservam a antecipação de que "um de vocês" o entregaria; João 13:21-26 liga a identificação narrativa a Judas.

João 12:6 apresenta Judas como responsável pela bolsa comum e o caracteriza como ladrão. Mateus 27:3-5 associa Judas às trinta moedas, arrependimento e enforcamento.

Não há no texto canônico uma afirmação direta de que Judas agiu com a intenção secreta de salvar Jesus ou "salvar uma vida em todos". Essa hipótese deve permanecer:

```text
JUDAS_SALVATION_MOTIVE = TOKEN_VAZIO_TEXTUAL_SUPPORT
CLASS = THEOLOGICAL_HYPOTHESIS
```

## Caso: Pedro, negação e perdão

A negação de Pedro é narrada como tripla. O ensino sobre perdoar "setenta e sete" ou "setenta vezes sete" aparece em Mateus 18:21-22 e é outro episódio.

Assim:

```text
PETER_DENIAL_COUNT = 3
FORGIVENESS_7_TO_77_OR_70x7 = SEPARATE_TEACHING
```

A aproximação entre as duas passagens pode ser teológica — falha humana seguida de restauração e perdão — mas não deve fundir os eventos.

## Caso: Barabbas

`Barabbas` é normalmente explicado a partir do aramaico `bar-abba`, "filho do pai".

Algumas testemunhas textuais de Mateus 27:16-17 preservam a leitura `Jesus Barabbas`; a leitura é conhecida na crítica textual, mas sua originalidade permanece discutida.

Portanto:

```text
BARABBAS_SON_OF_FATHER = LANGUAGE_SUPPORTED
JESUS_BARABBAS_READING = TEXTUAL_VARIANT
JESUS_BARABBAS_ORIGINAL = DISPUTED
```

A multidão, na narrativa, escolhe a libertação de uma pessoa; o texto não diz que ela "escolheu um nome" como categoria jurídica.

## Caso: costume de libertação

Os evangelhos descrevem uma libertação de prisioneiro ligada à festa, mas não há atestação externa direta e inequívoca de um costume pascal judeano exatamente como narrado. A historicidade e a forma administrativa desse costume são discutidas.

Não promover:

```text
PASSOVER_RELEASE = LAW_OF_CAESAR
```

Sem fonte jurídica específica, esse estado é:

```text
ROMAN_LEGAL_BASIS = TOKEN_VAZIO_SPECIFIC_STATUTE
HISTORICAL_CUSTOM = DISPUTED
```

## Modelo temporal

Para navegação humana, calendários e eventos continuam lineares:

```text
BEFORE -> NOW -> AFTER
```

Para a camada teológica/metafórica, uma constante ética pode ser representada separadamente:

```text
LOVE_NEIGHBOR_AS_SELF = ETHICAL_INVARIANT
```

Essa constante não altera data, gramática ou sequência histórica; ela organiza a aplicação moral através do tempo.

## Rota canônica

```text
INTENT
  -> TEXT
  -> LANGUAGE
  -> VARIANT
  -> HISTORY
  -> THEOLOGY
  -> ETHICAL_APPLICATION
  -> RECEIPT
```

Se uma etapa não tiver fonte suficiente:

```text
ROUTE_STATE = BLOCKED
VALUE = TOKEN_VAZIO
```

## Fontes mínimas iniciais

- Marcos 12:29-31 — amor a Deus e ao próximo.
- Lucas 22:47-48 — Judas, beijo e `παραδίδως`.
- João 12:6; 13:21-26 — bolsa comum e anúncio da entrega.
- Mateus 18:21-22 — sete / setenta e sete / setenta vezes sete.
- Mateus 27:3-17 — Judas, moedas e Barabbas.
- Atos 1:15-20 — tradição lucana sobre o fim de Judas.

## Fronteira

A rota admite leitura espiritual forte, inclusive a ideia de um Verbo atemporal, desde que:

1. a gramática permaneça gramática;
2. a história permaneça história;
3. a variante permaneça variante;
4. a hipótese seja identificada como hipótese;
5. a aplicação ética possa atravessar o tempo sem reescrever a fonte.

## R3

`F_ok`: eixo ético "amar o próximo como a si mesmo" possui apoio textual direto; beijo/entrega, Pedro, Judas e Barabbas podem ser relacionados sem fundir suas categorias.

`F_gap`: intenção salvífica de Judas, estatuto romano específico para libertação pascal e leitura atemporal como morfologia verbal não possuem apoio textual/histórico suficiente.

`F_next`: ampliar somente por fonte textual, crítica textual ou evidência histórica específica; não por associação nominal isolada.
