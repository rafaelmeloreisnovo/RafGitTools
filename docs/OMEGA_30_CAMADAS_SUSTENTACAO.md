# Ω — 30 camadas de sustentação

Status: `METHOD_DEFINED`  
Representação canônica: [`/workflow-master-index.json`](../workflow-master-index.json)

## Princípio

As camadas abaixo não são trinta metáforas independentes. Elas formam uma malha
de sustentação. Uma claim pode atravessar várias camadas, mas não pode usar uma
camada como substituta de outra: semântica não substitui teste; hash não
substitui validade científica; execução não substitui ética.

A cobertura de uma sessão é representada por:

\[
\Gamma = \{s_1, s_2, \ldots, s_{30}\}
\]

Cada \(s_i\) recebe um estado explícito:

`PASS`, `PARTIAL`, `FAIL`, `NOT_APPLICABLE` ou `TOKEN_VAZIO`.

`NOT_APPLICABLE` exige justificativa de domínio. `TOKEN_VAZIO` exige razão da
ausência. Nenhum dos dois equivale a `PASS`.

## S01 — Origem semântica

**Categoria:** `semantic`

**Função:** Preservar a formulação original antes de qualquer normalização.

**Evidência mínima:** trecho de sessão, documento ou issue com autoria e data.

**Falha explícita:** `TOKEN_VAZIO`.

**Gate de promoção:** origem localizada e referenciada.

## S02 — Vocabulário canônico

**Categoria:** `semantic`

**Função:** Definir termos, símbolos e sinônimos sem trocar significado silenciosamente.

**Evidência mínima:** glossário versionado e exemplos de uso.

**Falha explícita:** `CONTRADICTION`.

**Gate de promoção:** termo possui definição, domínio e aliases.

## S03 — Atomicidade de claim

**Categoria:** `semantic`

**Função:** Quebrar afirmações compostas em unidades verificáveis.

**Evidência mínima:** ledger com identificadores únicos.

**Falha explícita:** `BLOCKED`.

**Gate de promoção:** cada claim pode ser testado ou classificado isoladamente.

## S04 — Estado epistemológico

**Categoria:** `semantic`

**Função:** Distinguir evidência, método, hipótese e símbolo.

**Evidência mínima:** estado explícito por claim.

**Falha explícita:** `TOKEN_VAZIO`.

**Gate de promoção:** nenhuma promoção ocorre sem evidência compatível.

## S05 — Fronteira de domínio

**Categoria:** `semantic`

**Função:** Declarar onde uma relação vale e onde deixa de valer.

**Evidência mínima:** domínio, pré-condições e contraexemplos.

**Falha explícita:** `OUT_OF_DOMAIN`.

**Gate de promoção:** limites e exclusões estão registrados.

## S06 — Consistência dimensional

**Categoria:** `formal`

**Função:** Impedir equações fisicamente incompatíveis.

**Evidência mínima:** unidades, dimensões e checagem simbólica.

**Falha explícita:** `FAIL`.

**Gate de promoção:** todas as somas e igualdades preservam dimensões.

## S07 — Derivação matemática

**Categoria:** `formal`

**Função:** Registrar passos entre premissas e conclusão.

**Evidência mínima:** derivação reproduzível ou referência formal.

**Falha explícita:** `TOKEN_VAZIO`.

**Gate de promoção:** cada transformação possui justificativa.

## S08 — Invariante geométrica

**Categoria:** `formal`

**Função:** Identificar quantidades preservadas sob transformações declaradas.

**Evidência mínima:** definição da transformação e teste analítico ou numérico.

**Falha explícita:** `PARTIAL`.

**Gate de promoção:** invariante passa em casos válidos e falha nos inválidos.

## S09 — Falsificador explícito

**Categoria:** `formal`

**Função:** Definir observação capaz de enfraquecer ou rejeitar a hipótese.

**Evidência mínima:** critério de rejeição antes do teste.

**Falha explícita:** `UNFALSIFIABLE`.

**Gate de promoção:** ao menos um resultado possível rejeita a claim.

## S10 — Baseline comparável

**Categoria:** `formal`

**Função:** Comparar com método simples, clássico ou estado da arte apropriado.

**Evidência mínima:** baseline, mesma entrada e mesma métrica.

**Falha explícita:** `NO_BASELINE`.

**Gate de promoção:** ganho ou perda é mensurável sob condições iguais.

## S11 — Proveniência de dados

**Categoria:** `evidence`

**Função:** Rastrear fonte, revisão, tamanho, hash e transformações.

**Evidência mínima:** manifesto de entrada e cadeia de custódia.

**Falha explícita:** `TOKEN_VAZIO`.

**Gate de promoção:** entrada é identificável sem depender do nome do arquivo.

## S12 — Lock de fonte

**Categoria:** `evidence`

**Função:** Fixar commits e versões usados na execução.

**Evidência mínima:** runtime-lock ou equivalente validado.

**Falha explícita:** `SOURCE_FLOATING`.

**Gate de promoção:** dependências apontam para SHAs concretos.

## S13 — Hash de artefato

**Categoria:** `evidence`

**Função:** Provar identidade dos produtos gerados.

**Evidência mínima:** SHA-256 e tamanho por artefato.

**Falha explícita:** `ARTIFACT_UNVERIFIED`.

**Gate de promoção:** hash concreto no gate de promoção.

## S14 — Execução determinística

**Categoria:** `execution`

**Função:** Obter mesma saída para mesma entrada e configuração declarada.

**Evidência mínima:** vetores repetidos e hashes iguais.

**Falha explícita:** `NON_DETERMINISTIC`.

**Gate de promoção:** repetições concordam dentro da tolerância.

## S15 — Recorrência e ponto fixo

**Categoria:** `execution`

**Função:** Caracterizar evolução por ciclos, atratores e condições de parada.

**Evidência mínima:** traço de iterações e critério de convergência.

**Falha explícita:** `DIVERGENT`.

**Gate de promoção:** ponto fixo ou ausência dele é demonstrado.

## S16 — Tolerância numérica

**Categoria:** `execution`

**Função:** Separar igualdade exata de aproximação finita.

**Evidência mínima:** erro absoluto/relativo, escala e limite.

**Falha explícita:** `UNBOUNDED_ERROR`.

**Gate de promoção:** tolerância é justificada antes da execução.

## S17 — Plataforma e ABI

**Categoria:** `execution`

**Função:** Explicitar arquitetura, compilador, flags e ambiente.

**Evidência mínima:** manifesto de plataforma e smoke test.

**Falha explícita:** `PLATFORM_UNKNOWN`.

**Gate de promoção:** alvos declarados foram exercitados.

## S18 — Limites de recursos

**Categoria:** `execution`

**Função:** Controlar memória, armazenamento, expansão, tempo e ciclos.

**Evidência mínima:** limites configurados e medidos.

**Falha explícita:** `RESOURCE_UNBOUNDED`.

**Gate de promoção:** execução permanece dentro dos limites.

## S19 — Governança de ação

**Categoria:** `governance`

**Função:** Autorizar somente operações tipadas e permitidas.

**Evidência mínima:** policy, GovernanceGate e decisão auditável.

**Falha explícita:** `DENIED`.

**Gate de promoção:** ação possui identidade, escopo e decisão.

## S20 — Privacidade e consentimento

**Categoria:** `governance`

**Função:** Proteger credenciais, corpus e dados pessoais.

**Evidência mínima:** base legal/consentimento, minimização e isolamento.

**Falha explícita:** `PRIVACY_BLOCKED`.

**Gate de promoção:** nenhum dado sensível vaza para log, prompt ou artefato.

## S21 — Proteção infantil

**Categoria:** `governance`

**Função:** Bloquear usos que exponham ou prejudiquem crianças.

**Evidência mínima:** regra explícita, teste de negação e trilha de auditoria.

**Falha explícita:** `HARD_DENY`.

**Gate de promoção:** qualquer cenário infantil inseguro é rejeitado.

## S22 — Confissão de ausência

**Categoria:** `governance`

**Função:** Usar TOKEN_VAZIO em vez de inventar valores.

**Evidência mínima:** campo vazio classificado com razão.

**Falha explícita:** `FABRICATION`.

**Gate de promoção:** ausência é visível e não convertida em zero.

## S23 — Ledger de contradições

**Categoria:** `governance`

**Função:** Preservar evidências conflitantes sem apagamento oportunista.

**Evidência mínima:** registros dos lados, escopo e estado.

**Falha explícita:** `CONTRADICTION`.

**Gate de promoção:** conflito permanece rastreável até resolução.

## S24 — Reprodutibilidade

**Categoria:** `evidence`

**Função:** Permitir repetição por terceiro com entradas e comandos conhecidos.

**Evidência mínima:** comandos, seeds, versões e artefatos.

**Falha explícita:** `NOT_REPRODUCIBLE`.

**Gate de promoção:** terceiro consegue repetir o resultado.

## S25 — Testes por camada

**Categoria:** `evidence`

**Função:** Testar unidade, contrato, integração, regressão e casos inválidos.

**Evidência mínima:** suíte executável e relatório.

**Falha explícita:** `TEST_GAP`.

**Gate de promoção:** claims executáveis têm testes correspondentes.

## S26 — Gate de promoção

**Categoria:** `evolution`

**Função:** Separar rascunho, método, teste local, evidência e release.

**Evidência mínima:** critérios de entrada e saída por estado.

**Falha explícita:** `PROMOTION_BLOCKED`.

**Gate de promoção:** estado superior exige todos os gates anteriores.

## S27 — Observabilidade

**Categoria:** `evolution`

**Função:** Registrar o que ocorreu sem expor segredos.

**Evidência mínima:** logs estruturados, métricas e IDs correlacionáveis.

**Falha explícita:** `UNOBSERVABLE`.

**Gate de promoção:** falha aponta estágio e ação de recuperação.

## S28 — Checkpoint e rollback

**Categoria:** `evolution`

**Função:** Retomar ou desfazer operações sem corromper a origem.

**Evidência mínima:** checkpoint, hash, journal e teste de recuperação.

**Falha explícita:** `ROLLBACK_UNPROVEN`.

**Gate de promoção:** estado anterior é restaurável e verificável.

## S29 — Rastreabilidade semântica→código

**Categoria:** `evolution`

**Função:** Ligar símbolo, claim, arquivo, teste, commit e artefato.

**Evidência mínima:** matriz de rastreabilidade com IDs estáveis.

**Falha explícita:** `TRACE_GAP`.

**Gate de promoção:** cada promoção aponta sua cadeia de sustentação.

## S30 — Fronteira transdisciplinar

**Categoria:** `evolution`

**Função:** Transferir conceitos entre áreas sem promover metáfora a lei.

**Evidência mínima:** mapeamento origem→analogia→hipótese→teste por domínio.

**Falha explícita:** `CATEGORY_ERROR`.

**Gate de promoção:** aplicação declara o que é preservado e o que muda.

## Matriz de fechamento

Uma sessão pode ser considerada fechada provisoriamente quando:

1. todas as 30 camadas estão presentes no contrato;
2. nenhuma camada aplicável está omitida;
3. `FAIL` bloqueia promoção;
4. `TOKEN_VAZIO` permanece visível;
5. claims `VERIFIED` apontam para evidências válidas;
6. claims verificadas por teste apontam para teste `PASS`;
7. a próxima ação é única e concreta.

A suficiência universal das 30 camadas ainda é uma hipótese metodológica. O
contrato prova cobertura estrutural, não prova que nenhuma camada futura será
necessária.
