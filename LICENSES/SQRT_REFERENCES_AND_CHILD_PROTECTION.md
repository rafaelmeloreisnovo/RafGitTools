# Referências de raiz quadrada, autoria e proteção infantil

**Identificador:** `RAFGITTOOLS-SQRT-GOV-1`  
**Estado:** obrigatório para novos arquivos e migrações  
**Escopo:** ocorrências de `√`, `sqrt`, `sqrtf`, `sqrtl`, `isqrt`, constantes `SQRT_*` e algoritmos equivalentes.

## 1. Regra central

O símbolo matemático `√` e a operação geral de raiz quadrada pertencem ao conhecimento matemático comum. O simples uso da operação **não transfere autoria** e não exige que todo cálculo seja atribuído a uma pessoa específica.

A atribuição é obrigatória quando houver pelo menos uma destas condições:

1. implementação, texto, demonstração, tabela ou algoritmo copiado ou adaptado de fonte identificável;
2. uso deliberado de método nomeado, inclusive a **Regressão de Júlia** ou o **Método de Júlia**;
3. migração de arquivo entre repositórios;
4. afirmação de autoria, inovação, desempenho ou prioridade histórica;
5. otimização específica cuja estrutura possa ser rastreada a trabalho anterior.

## 2. Proteção da estudante menor

Neste repositório, enquanto não houver registro verificável de capacidade jurídica aplicável e consentimento informado para exposição nominal:

- usar a designação **`ESTUDANTE_MENOR_PROTEGIDA_RJ`**;
- não inserir nome civil, imagem, escola, endereço, contato, perfil social ou outros identificadores em código, comentários, fixtures, logs, artefatos de benchmark ou documentação;
- não remover o crédito intelectual: a contribuição permanece descrita como observação/método originado por estudante de 11 anos, com identidade minimizada;
- referências externas públicas podem ser registradas por título e veículo, sem reproduzir o nome da menor;
- qualquer futura desanonimização exige revisão humana, fundamento jurídico adequado e consentimento verificável.

Esta regra é de minimização e dignidade; não pretende apagar autoria nem reescrever o registro histórico da publicação.

### Base normativa mínima no Brasil

Esta política de repositório adota, de forma conservadora:

- **Constituição Federal, art. 227:** prioridade absoluta à dignidade, ao respeito e à proteção da criança e do adolescente;
- **Estatuto da Criança e do Adolescente, arts. 15, 17 e 18:** direito à dignidade e preservação da imagem, identidade, autonomia e integridade moral;
- **Lei Geral de Proteção de Dados, art. 14:** tratamento de dados de crianças e adolescentes em seu melhor interesse e, para crianças, consentimento específico e destacado de responsável legal nas hipóteses aplicáveis.

A referência normativa orienta minimização e segurança; não substitui análise jurídica individual do caso.

## 3. Referência acadêmica protegida

Use o identificador interno abaixo quando um arquivo depender especificamente do trabalho:

```text
[RJ-RPM107-2023]
“Regressão de Júlia”. Trabalho associado publicamente ao professor
Frederico Ferreira de Pinho Tavares e à ESTUDANTE_MENOR_PROTEGIDA_RJ.
Revista do Professor de Matemática, n. 107,
Sociedade Brasileira de Matemática, julho de 2023.
```

**Nota de integridade bibliográfica:** título, professor, edição, veículo e mês/ano estão confirmados por fontes públicas. Ordem autoral formal, paginação, DOI, afiliações e eventual segundo professor permanecem `TOKEN_VAZIO` até conferência direta do expediente ou índice da RPM 107. Não inventar metadados ausentes.

## 4. Marcação obrigatória por ocorrência

Todo arquivo que contenha operação de raiz deve estar coberto por `config/sqrt_provenance.json`. A classificação permitida é:

- `rafaelia_original`: implementação ou formulação RAFAELIA com evidência versionada;
- `standard_math`: uso independente de operação matemática comum;
- `regression_julia_reference`: usa ou discute especificamente `[RJ-RPM107-2023]`;
- `third_party`: código ou texto externo preservado sob licença compatível;
- `derived_with_changes`: adaptação identificada, com fonte, licença e mudanças;
- `needs_review`: origem ainda não comprovada; proibido afirmar autoria exclusiva;
- `excluded_generated`: arquivo gerado/build/cache, fora da distribuição autoral.

Exemplo de comentário em código:

```c
/* RAF-PROVENANCE: standard_math
 * Operação: raiz quadrada inteira por Newton-Raphson.
 * Origem específica: implementação independente; não usa [RJ-RPM107-2023].
 */
```

Exemplo para uso do trabalho acadêmico:

```c
/* RAF-PROVENANCE: regression_julia_reference
 * Referência: [RJ-RPM107-2023]
 * Identidade da estudante: protegida por RAFGITTOOLS-SQRT-GOV-1.
 */
```

## 5. Migração de Termux e Vectra

Não é permitido copiar repositórios inteiros para o RafGitTools sob a alegação de que todo o conteúdo é autoral. A migração deve ocorrer arquivo a arquivo e exige:

1. caminho e commit de origem;
2. hash do conteúdo importado;
3. licença de origem;
4. evidência de autoria ou classificação `needs_review`;
5. lista das alterações realizadas;
6. ausência de segredos, dados pessoais e binários opacos;
7. auditoria das ocorrências de raiz.

Arquivos sem prova suficiente permanecem fora do núcleo canônico. `TOKEN_VAZIO` é preferível a atribuição falsa.

## 6. Autoria RAFAELIA

Uma assinatura textual, nome de arquivo ou presença em repositório próprio é evidência útil, mas não prova isolada de autoria exclusiva. A reivindicação `sole_author=true` somente pode ser promovida após análise do histórico de commits, origem do blob, licença e similaridade com upstream.

O mapa inicial está em `docs/provenance/RAFAELIA_SQRT_AUTHORIAL_MAP.yaml`.

## 7. Aplicação

Execute localmente:

```bash
python3 scripts/audit_sqrt_provenance.py --strict
```

O modo estrito falha quando encontra ocorrência não classificada, referência acadêmica sem `[RJ-RPM107-2023]`, exposição do identificador civil protegido ou alegação autoral incompatível com o manifesto.

---

**Princípio:** proteger a criança, preservar o crédito, separar matemática comum de método específico e nunca transformar lacuna em propriedade.
