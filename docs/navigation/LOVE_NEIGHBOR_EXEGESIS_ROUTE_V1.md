# LOVE_NEIGHBOR_EXEGESIS_ROUTE_V1

## Função

Rota gradual para leitura humano/IA de textos do Novo Testamento quando a intenção cruza ética, linguagem, história e hipótese teológica.

Esta rota não define doutrina e não transforma interpretação em fato histórico.

```text
TEXT != VARIANT != HISTORY != THEOLOGY != ETHICAL_APPLICATION
SOURCE != INTERPRETATION != CLAIM
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

## Gate anti-fusão para reconstrução humano / IA

Antes de relacionar dois elementos, registrar:

```text
ENTITY -> OBSERVED_ACTION_OR_TITLE -> SOURCE -> CLASS -> EVIDENCE_BOUNDARY
```

Invariantes:

```text
CHARACTER != ROLE != ACTION != TITLE != INTERPRETATION
JUDAS != BARABBAS != PETER != YESHUA
DENIAL_COUNT != STRUCTURAL_NODE_COUNT
NARRATIVE_CORRESPONDENCE != PROOF_OF_INTENT
TRANSLATION != ORIGINAL_WORDING
```

Falha fechada:

```text
missing_source
OR entity_conflation
OR title_conflation
OR translation_promoted_as_original
OR interpretation_promoted_as_history
=> ROUTE_STATE = BLOCKED
=> VALUE = TOKEN_VAZIO
```

Objetivo: permitir reconstrução rápida sem depender de memória implícita e impedir que compressão de contexto transforme associação em identidade.

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

Não fundir com Barabbas:

```text
JUDAS_MONEY_BOX_AND_THEFT != BARABBAS_PRISONER_INSURRECTION_KILLING
```

## Caso: Pedro, reconhecimento, negação e restauração

Mateus 16:13-17 preserva um nó anterior de reconhecimento/confissão; a negação de Pedro na narrativa da paixão é tripla.

Quando uma reconstrução usar o número `4`, tipar explicitamente a intenção estrutural:

```text
PETER_RECOGNITION_NODE = 1
PETER_DENIAL_COUNT = 3
PETER_STRUCTURAL_GROUP = 1 + 3
PETER_DENIAL_COUNT != 4
```

O grupo `1 + 3` é um mapa interpretativo de nós narrativos separados; não afirma quatro negações nem adjacência imediata entre os episódios.

João 21:15-17 preserva três perguntas de Jesus a Pedro sobre amor, acompanhadas por três incumbências pastorais. A correspondência pode ser registrada sem promovê-la a prova de uma fórmula oculta:

```text
PETER_DENIAL_COUNT = 3
PETER_LOVE_QUESTIONS = 3
PETER_RESTORATION_CORRESPONDENCE = TEXTUAL_NARRATIVE_PATTERN
HIDDEN_NUMERICAL_DOCTRINE = TOKEN_VAZIO
```

O ensino sobre perdoar "setenta e sete" ou "setenta vezes sete" aparece em Mateus 18:21-22 e é outro episódio.

```text
FORGIVENESS_7_TO_77_OR_70x7 = SEPARATE_TEACHING
```

A aproximação entre reconhecimento, falha, restauração e perdão pode ser teológica, desde que os episódios não sejam fundidos.

## Caso: Barabbas

`Barabbas` é normalmente explicado a partir do aramaico `bar-abba`, "filho do pai".

Algumas testemunhas textuais de Mateus 27:16-17 preservam a leitura `Jesus Barabbas`; a leitura é conhecida na crítica textual, mas sua originalidade permanece discutida.

Portanto:

```text
BARABBAS_SON_OF_FATHER = LANGUAGE_SUPPORTED
JESUS_BARABBAS_READING = TEXTUAL_VARIANT
JESUS_BARABBAS_ORIGINAL = DISPUTED
```

Marcos 15:7 e Lucas 23:19 associam Barabbas a prisão, insurreição e homicídio. Esse conjunto pertence a Barabbas e não deve ser transferido para Judas por compressão de contexto.

```text
BARABBAS_PRISONER = TEXT_SUPPORTED
BARABBAS_INSURRECTION_KILLING = TEXT_SUPPORTED
JUDAS_IS_BARABBAS = FALSE_NARRATIVE_CONFLATION
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

## Pacote mínimo de reconstrução

Para cada associação preservada, um humano ou IA deve conseguir recuperar sem varrer todo o corpus:

```text
ID
ENTITY
OBSERVATION
SOURCE_MIN
CLASS
EVIDENCE_BOUNDARY
PREDECESSOR_OR_CONTEXT
F_GAP
F_NEXT
```

Regras de qualidade operacional:

```text
one_observation -> one_classification
one_claim -> source_or_TOKEN_VAZIO
correction -> successor_not_silent_rewrite
exact_source_boundary -> mandatory_before_promotion
```

As normas, métodos de qualidade e engenharia podem orientar disciplina de trabalho, mas este documento não declara certificação ISO, IEEE, RFC, Six Sigma, ICT ou equivalente.

## Fontes mínimas iniciais

- Marcos 12:29-31 — amor a Deus e ao próximo.
- Mateus 16:13-17 — pergunta sobre o Filho do Homem e confissão de Pedro.
- Lucas 22:47-48 — Judas, beijo e `παραδίδως`.
- João 12:6; 13:21-26 — bolsa comum e anúncio da entrega.
- relatos da paixão — três negações de Pedro.
- João 21:15-17 — três perguntas sobre amor e incumbências a Pedro.
- Mateus 18:21-22 — sete / setenta e sete / setenta vezes sete.
- Marcos 15:7; Lucas 23:19 — Barabbas, prisão, insurreição e homicídio.
- Mateus 27:3-17 — Judas, moedas e Barabbas.
- Atos 1:15-20 — tradição lucana sobre o fim de Judas.

## Fronteira

A rota admite leitura espiritual forte, inclusive a ideia de um Verbo atemporal, desde que:

1. a gramática permaneça gramática;
2. a história permaneça história;
3. a variante permaneça variante;
4. a hipótese seja identificada como hipótese;
5. a aplicação ética possa atravessar o tempo sem reescrever a fonte;
6. números usados como estrutura sejam separados de contagens literais do texto;
7. personagens não sejam fundidos por proximidade semântica, tradução ou compressão de contexto.

## Gate de promoção

```text
PASS only if:
  entity_is_unambiguous
  AND source_min_present
  AND class_present
  AND evidence_boundary_present
  AND no_category_fusion

otherwise:
  PENDING | BLOCKED | TOKEN_VAZIO
```

Nenhum `PASS` desta rota equivale a prova histórica total, doutrina, certificação acadêmica ou verdade metafísica; significa apenas que a associação documental passou os limites declarados desta rota.

## R3

`F_ok`: eixo ético "amar o próximo como a si mesmo" possui apoio textual direto; beijo/entrega, Pedro, Judas e Barabbas podem ser relacionados sem fundir suas categorias; o nó estrutural de Pedro `1 + 3` e a correspondência `3 negações <-> 3 perguntas de amor` ficam agora tipados para evitar perda por compressão.

`F_gap`: intenção salvífica de Judas, estatuto romano específico para libertação pascal, leitura atemporal como morfologia verbal e qualquer doutrina numérica oculta não possuem apoio textual/histórico suficiente.

`F_next`: ampliar somente quando uma nova fonte textual, variante crítica ou evidência histórica específica reduzir um `TOKEN_VAZIO`; não por associação nominal, aritmética ou simbólica isolada.
