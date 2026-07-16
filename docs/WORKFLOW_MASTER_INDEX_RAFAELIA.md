# WORKFLOW MASTER INDEX — RAFAELIA / Ω

Status: `METHOD_DEFINED`  
Autoridade de integração: `rafaelmeloreisnovo/RafGitTools`  
Fonte legível por máquina: [`/workflow-master-index.json`](../workflow-master-index.json)

## 1. Finalidade

Este índice não tenta transformar todas as ideias em uma única teoria. Ele cria uma
**tábua de navegação verificável** entre linguagem simbólica, matemática formal,
software, experimentos, evidência, governança e aplicação.

A invariante operacional adotada é:

\[
C_v = S_p + P_r + E_a
\]

onde:

- \(S_p\): símbolo preservado sem troca silenciosa de significado;
- \(P_r\): prova ou sustentação registrada;
- \(E_a\): execução auditável.

A revisão seguinte não substitui a anterior. Ela calcula um delta:

\[
A_{n+1} = A_n + \Delta_{fontes} + \Delta_{commits} +
\Delta_{evidências} + \Delta_{contradições}
\]

## 2. Ciclo longitudinal

| Símbolo | Estado | Saída mínima |
|---|---|---|
| ψ | intenção | pergunta, escopo e critério de término |
| χ | observação | fontes, commits, arquivos, testes e ambiente |
| ρ | ruído | lacunas, contradições, duplicações e riscos |
| Δ | transformação | claims, modelos, algoritmos e experimentos |
| Σ | custódia | hashes, commits, ledgers e rastreabilidade |
| Ω | fechamento provisório | estado, limites, resultado e próximo falsificador |
| ψ′ | reabertura | delta incorporado à próxima passagem |

O ciclo é reentrante:

\[
ψ \rightarrow χ \rightarrow ρ \rightarrow Δ \rightarrow Σ \rightarrow Ω \rightarrow ψ'
\]

Nenhuma passagem deve apagar uma contradição útil ou promover hipótese sem gate.

## 3. Módulos do ecossistema

| ID | Módulo | Responsabilidade | Repositório canônico declarado |
|---|---|---|---|
| M01 | RAFAELIA simbólica | linguagem, ética e compressão conceitual | `TOKEN_VAZIO` |
| M02 | RLL científico | modelos falsificáveis, likelihood e comparação cosmológica | `instituto-Rafael/relativity-living-light` |
| M03 | Omega Kernel | núcleo determinístico, Q16, invariantes e checkpoints | `rafaelmeloreisnovo/ZIPRAF_OMEGA_FULL` |
| M04 | Omega Governance | políticas, ledger, runtime-lock e promoção | `rafaelmeloreisnovo/RafGitTools` |
| M05 | Termux RAFCODEΦ | runtime Android local e transporte tipado | `rafaelmeloreisnovo/termux-app-rafacodephi` |
| M06 | Vectras VM Android | QEMU, Proot e ciclo de VM no Android | `rafaelmeloreisnovo/Vectras-VM-Android` |
| M07 | ChipQuantum Geometria | papers, projeções, √3/2 e experimentos formais | `rafaelmeloreisnovo/ChipQuantum` |
| M08 | RAFAELIA Engine | indexação, memória, coerência e drift | `TOKEN_VAZIO` |
| M09 | Android Middleware IA | interface controlada entre apps e modelos | `rafaelmeloreisnovo/llamaRafaelia` |
| M10 | CompiladorLowFala | fala, bytecode e VM simbólica experimental | `TOKEN_VAZIO` |
| M11 | Bare-metal Edge | registradores, sensores e execução de borda | `TOKEN_VAZIO` |
| M12 | Corpus e memória | fontes, chunks, matrizes e custódia de dados | `rafaelmeloreisnovo/CONVERSATIONS_CHUNKS_PRIVATE` |
| M13 | GitHub Agents e ResearchOps | orquestração, claims, PRs e artefatos | `rafaelmeloreisnovo/RafGitTools` |

`TOKEN_VAZIO` significa que a autoridade canônica ainda não foi demonstrada pelo
índice. Não significa inexistência do trabalho.

## 4. Quatro níveis que não podem ser confundidos

| Nível | Conteúdo | Promoção necessária |
|---|---|---|
| Evidência | dado, commit, log, hash, teste, resultado | identidade e cadeia de custódia |
| Método | algoritmo, schema, arquitetura, equação operacional | implementação ou derivação |
| Hipótese | relação proposta e previsão | falsificador e comparação |
| Símbolo | metáfora, parábola e compressão conceitual | definição antes de transferência de domínio |

## 5. Contrato de sessão

Cada passagem deve gerar um JSON compatível com
[`schemas/rafaelia_workflow_session.schema.json`](../schemas/rafaelia_workflow_session.schema.json)
e validado pelo parser canônico:

```bash
python3 scripts/workflow_session_contract.py validate-index workflow-master-index.json
python3 scripts/workflow_session_contract.py validate-session \
  workflow-master-index.json examples/workflow_session.example.json
python3 scripts/workflow_session_contract.py summarize \
  workflow-master-index.json examples/workflow_session.example.json
```

O contrato exige:

1. origem e escopo;
2. âncoras semânticas;
3. claims atômicos;
4. estado epistemológico;
5. evidências e testes referenciados por ID;
6. invariantes;
7. contradições;
8. `TOKEN_VAZIO` com razão;
9. as 30 camadas de sustentação;
10. uma próxima ação concreta.

## 6. Regra de promoção

```text
SYMBOL
→ DEFINED
→ METHOD_DEFINED
→ TESTED_LOCAL
→ EVIDENCE_LINKED
→ REPLICATED
→ RELEASE_CANDIDATE
```

A seta não é automática. Cada transição depende das camadas aplicáveis e do seu
gate de promoção.

## 7. Relação com o runtime-lock

O `workflow-master-index.json` organiza significado, claims e sustentação.
O `runtime-lock.json` fixa a proveniência das fontes do runtime.
Nenhum deles substitui o outro:

```text
workflow index = o que significa e como se sustenta
runtime lock   = quais fontes exatas foram usadas
artifact hash  = qual produto exato foi gerado
test report    = o que foi exercitado
```

## 8. Política de automação

Esta implementação **não cria um novo workflow YAML**. O gate canônico é o
script [`scripts/validate_rafaelia_workflow.sh`](../scripts/validate_rafaelia_workflow.sh),
que pode ser chamado pelo orquestrador existente, localmente, no Termux ou em CI.

## 9. Resultado esperado

A saída de cada revisão deve permitir reconstruir:

```text
sessão
→ termo
→ claim
→ domínio
→ evidência
→ teste
→ arquivo
→ commit
→ artefato
→ estado
→ próximo falsificador
```

Essa cadeia é a invariante de custódia do workflow.
