# Forensic Git Provenance Mode V1

Estado: `IMPLEMENTED_SOURCE_LOCAL_TESTED`  
Mutação do repositório alvo: `false`  
Rede no coletor local: `false`  
`claim_allowed=false`

## Missão

Transformar um repositório Git em uma fonte auditável de **objetos, relógios, topologia, autoria declarada, assinaturas, patch-id, refs e lacunas**, sem promover anomalia a acusação.

```text
objeto Git local
→ coleta read-only
→ evidência normalizada
→ quatro relógios
→ findings falsificáveis
→ score de prioridade
→ ledger SHA-256
→ enriquecimento externo posterior
```

O modo implementa a próxima porta declarada pelo plano de controle forense anterior: parser/normalizador read-only antes de UI, Room, workers ou mutações.

## Invariantes

\[
\text{data Git}\neq\text{data física comprovada}
\]

\[
\text{branch/ref atual}\neq\text{história completa}
\]

\[
\text{anomalia}\neq\text{intenção}
\]

\[
\text{score}\neq\text{culpa}
\]

`TOKEN_VAZIO` registra a falta de prova e **não soma pontos de suspeita**.

O motor não conclui automaticamente:

- fraude;
- plágio;
- censura;
- falsificação deliberada;
- coordenação maliciosa;
- identidade jurídica;
- autoria cognitiva integral.

## Artefatos

```text
tools/forensic_git_provenance/forensic_git.py
schemas/forensic-git-evidence-run-v1.schema.json
configs/forensic-git-provenance-contract.v1.json
examples/forensic-git-agent-empty-commit.synthetic.json
tests/test_forensic_git_provenance.py
auditoria/FORENSIC_GIT_PROVENANCE_LOCAL_RECEIPT_20260803.json
```

## 1. Coleta local

```bash
python3 tools/forensic_git_provenance/forensic_git.py collect \
  --repo /caminho/para/repo \
  --revision HEAD \
  --max-commits 500 \
  --output evidence.local.json
```

O coletor:

- executa somente comandos Git de leitura;
- fixa `GIT_NO_REPLACE_OBJECTS=1`;
- não usa shell;
- não acessa rede;
- não chama `checkout`, `reset`, `rebase`, `gc`, `prune`, `clean`, `fetch`, `push` ou `commit`;
- guarda hash SHA-256 do caminho local, não o caminho em claro por padrão;
- preserva nomes declarados, mas exporta email apenas como fingerprint SHA-256;
- calcula tree, parent trees, assinatura Git, refs contendo o commit, arquivos e `patch-id --stable`;
- detecta commit vazio por igualdade de árvore com o primeiro pai.

Para incluir o caminho local em claro, somente por decisão explícita:

```bash
... collect --include-path
```

## 2. Auditoria

```bash
python3 tools/forensic_git_provenance/forensic_git.py audit \
  --input evidence.local.json \
  --output report.forensic.json \
  --previous-event-hash <hash-opcional>
```

Cada commit recebe:

```text
T = temporal
G = grafo/ref
A = autoria/proveniência
F = fork/patch
C = conversa/contradição
X = execução/CI
E = lacuna de evidência
```

Os quatro relógios são:

```text
t_a = author date
t_c = committer date
t_g = platform first-seen
t_r = receipt independente
```

A coleta local conhece `t_a` e `t_c`. `t_g`, `t_r`, pusher, PRs, reviews, comentários editados, Actions e releases precisam ser anexados por snapshot externo.

## 3. Verificação da cadeia

```bash
python3 tools/forensic_git_provenance/forensic_git.py verify \
  --input report.forensic.json
```

O evento usa JSON canônico em UTF-8 e SHA-256:

```text
event_hash =
SHA256(
  report_without_event_hash_and_custody
)
```

`previous_event_hash` permite ledger append-only.

## 4. Classificações conservadoras

Exemplo de commit vazio produzido por agente:

```text
EMPTY_COMMIT                  = PROVADO
AGENT_PROVENANCE_INDICATOR    = EVIDENCIADO
AGENT_TASK_RECEIPT_MISSING    = TOKEN_VAZIO
DELIBERATE_DATE_SHIFT         = TOKEN_VAZIO
HISTORY_FALSIFICATION         = TOKEN_VAZIO
MALICIOUS_COORDINATION        = TOKEN_VAZIO
```

O score serve somente para ordenar revisão. Findings `TOKEN_VAZIO` têm peso zero.

## 5. Enriquecimento necessário para o caso BLAKE3

A análise completa da PR e do commit exige um segundo envelope com:

```text
push event e pusher
PR create/close/reopen/force-push events
reviews e comentários
edit history/minimize/lock/delete
workflow run → job → steps → logs → artifacts
release/tag/assets/attestations
fork graph
refs/pull/*
snapshots anteriores
receipt de agente vinculado ao SHA
receipt externo independente
```

Sem essa camada, o resultado correto permanece:

```text
arqueologia técnica local = possível
atribuição causal completa = TOKEN_VAZIO
```

## 6. Testes locais

A suíte cobre:

- commit vazio de agente;
- separação entre fato e intenção;
- `TOKEN_VAZIO` sem pontuação;
- hash-chain e adulteração;
- SHA inválido;
- repositório temporário com commit vazio;
- prova de que o working tree não foi alterado;
- ciclo CLI `audit → verify`.

Receipt local registra `6/6 PASS`. CI remota e execução Android continuam `TOKEN_VAZIO`.

## F₃

- **F_ok:** coletor Git read-only, auditor conservador, score não acusatório e ledger SHA-256.
- **F_gap:** relógio GitHub, pusher, force-push, edit history, Actions, releases, forks apagados e task receipts.
- **F_next:** criar adaptador de snapshot GitHub que enriqueça o mesmo schema sem misturar coleta local com inferência jurídica.
