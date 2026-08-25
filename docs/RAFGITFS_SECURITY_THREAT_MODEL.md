# RafGitFS V1 — Threat Model

Status: `SOURCE_REVIEWED / EXTERNAL_REVIEW=TOKEN_VAZIO / CLAIM_ALLOWED=false`

## Ativos protegidos

- token de autenticação GitHub;
- conteúdo privado de repositórios;
- arquivos do workspace local;
- integridade de branches e commits;
- aprovações e hashes de plano;
- recibos operacionais;
- histórico de conflitos;
- armazenamento e disponibilidade do celular.

## Fronteiras de confiança

```text
usuário Android
↕ confirmação explícita
Compose / ViewModel
↕ plano hash-bound
Governed Sync Engine
↕ runtime security gate
GitHub read/write API
↕ HTTPS + token interceptor
GitHub repository
```

Persistência local:

```text
Room = índice, jobs, conflitos e recibos
filesDir = cache e workspace privados
GitHub = autoridade versionada remota
```

## Ameaças e controles

| Ameaça | Impacto | Controle V1 | Residual |
|---|---|---|---|
| token gravado no banco | exposição de credencial | contratos e gates proíbem segredos no Room | inspeção dinâmica `TOKEN_VAZIO` |
| path traversal | sobrescrita fora do workspace | caminho normalizado + `canonicalPath` + bloqueio `.git` | fuzz Android `TOKEN_VAZIO` |
| escrita direta em branch protegida | dano ao repositório | apenas branch `rafgitfs/*` | regras do servidor precisam ser verificadas |
| force-push | reescrita de histórico | DTO e writer fixam `force=false` | interceptação dinâmica `TOKEN_VAZIO` |
| branch-base mudou após o plano | commit sobre evidência obsoleta | SHA-base relido antes de criar branch | corrida posterior tratada pelo push não-force |
| alteração local e remota simultânea | perda silenciosa | comparação de três vias + conflito persistente | resolução humana necessária |
| plano alterado depois da aprovação | execução diferente do aprovado | `requestId + workspaceId + planHash` canônicos | armazenamento de aprovação assinado futuro |
| PR publicada sem revisão | mudança prematura | PR aberta em `DRAFT` | regras de merge do GitHub externas |
| conteúdo staged adulterado | commit de bytes diferentes | SHA-256 local revalidado antes do blob | memória/runtime comprometidos fora do escopo |
| blob remoto divergente | corrupção | hash Git recalculado | SHA-256 repos Git futuro suportado |
| repetição parcial | múltiplas branches/PRs | marcos persistentes idempotentes | limpeza de branches abandonadas é manual |
| exclusão remota acidental | perda | nenhum endpoint DELETE | exclusão manual fora do app permanece possível |
| log com segredo | vazamento | sanitização + buffer limitado | logs de terceiros fora do escopo |
| cache excede espaço | indisponibilidade | orçamento, expiração e LRU pinned-safe | pressão global do Android |
| supply chain Gradle | código malicioso | versões fixadas e CI prevista | SBOM/assinatura `TOKEN_VAZIO` |

## Controles canônicos SEC-001…011

```text
SEC-CLAIM-001      claimAllowed=false
SEC-SECRET-002     credenciais fora do Room
SEC-STORE-003      workspace privado
SEC-PLAN-004       planHash SHA-256 conhecido
SEC-APPROVAL-005   aprovação exata
SEC-CONFLICT-006   zero conflito não resolvido
SEC-STAGE-007      staged file verificado
SEC-BRANCH-008     somente rafgitfs/*
SEC-BASE-009       ref-base explícita
SEC-FORCE-010      force=false
SEC-PR-011         draft=true
```

## Fora do escopo V1

- proteção contra aparelho root comprometido;
- hardware-backed keystore e atestação;
- assinatura criptográfica das aprovações;
- merge automático;
- deleção remota;
- administração de branch protection;
- recuperação de conta GitHub;
- sincronização concorrente multi-dispositivo;
- garantia formal do backend GitHub.

## Gates de falsificabilidade

Uma reivindicação de segurança é rejeitada se qualquer teste demonstrar:

- endpoint `DELETE` ou merge na API dedicada;
- `force=true`;
- PR não-draft;
- branch fora de `rafgitfs/*`;
- `claimAllowed=true`;
- persistência de segredo;
- workspace fora de `filesDir`;
- execução com conflito;
- alteração de workspace sem alteração do planHash;
- aprovação que não corresponda ao hash exato.

## Estado final

```yaml
security_design: IMPLEMENTED_SOURCE
runtime_security_gate: IMPLEMENTED_SOURCE
unit_and_adversarial_tests: IMPLEMENTED_SOURCE
instrumented_private_storage_test: IMPLEMENTED_SOURCE
external_penetration_test: TOKEN_VAZIO
android_runtime_receipt: TOKEN_VAZIO
certification_claim: false
claim_allowed: false
```
