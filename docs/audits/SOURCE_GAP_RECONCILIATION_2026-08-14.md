# Source Gap Reconciliation — 2026-08-14

Status: `PREEXISTING_SOURCE_GAP_BASELINE / SEMANTIC_TRIAGE_REQUIRED / claim_allowed=false`

## 1. Por que este documento existe

O workflow `Source Gap Audit` é útil para localizar `NotImplementedError`, `TOKEN_VAZIO`, placeholders e outros sinais lexicais, mas esses sinais não equivalem automaticamente a uma função morta ou a um stub real.

A documentação precisa distinguir:

```text
sinal lexical
!= caminho executável
!= integração de UI
!= runtime observado
!= feature completamente fechada
```

## 2. Comparação base → PR de navegação

### `main`

- commit: `49e1e56600fe0d424d0cdd180e3c386ea232d47c`;
- Source Gap Audit run: `31831916581`;
- resultado: `failure`;
- scanner self-test: PASS;
- scan: `files=257 blockers=82 warnings=34 allowlisted=0`;
- `tree_sha256=60ef440e4e05f1d908a563360371177005dd673fa3eebf3b0bf2e15d3e524d3c`.

### PR #352 / Repository View

- head: `d5e223c88d33cab7dc8a326f170dff39afe68fa5`;
- Source Gap Audit run: `31858085711`;
- resultado: `failure`;
- scanner self-test: PASS;
- scan: `files=257 blockers=82 warnings=34 allowlisted=0`;
- `tree_sha256=60ef440e4e05f1d908a563360371177005dd673fa3eebf3b0bf2e15d3e524d3c`.

Conclusão limitada: o novo Repository View **não alterou o conjunto compilado examinado pelo Source Gap Audit** e não introduziu os 82 blockers/34 warnings. O baseline vermelho é pré-existente nesse corte.

## 3. Falso positivo semântico importante: modo compatível/stub por parâmetro

Três managers demonstram por que o scanner lexical não pode ser usado sozinho para rebaixar toda uma feature a `stub`.

### Bisect

`BisectManager` implementa chamadas reais a `git bisect` quando recebe `repoPath` não vazio. O `NotImplementedError` aparece deliberadamente quando o parâmetro default vazio é usado, para manter compatibilidade com callers/testes antigos.

Classificação correta:

```text
real command path = IMPLEMENTED_SOURCE
empty repoPath compatibility path = EXPLICIT_STUB_SIGNAL
runtime/device proof = TOKEN_VAZIO_RUNTIME
```

### Git LFS

`LfsManager` executa `git lfs install/track/fetch/pull/push/...` quando há repositório e binário disponíveis. Alguns métodos mantêm `NotImplementedError` somente no caminho `repoPath=""`.

Classificação correta:

```text
real command path = IMPLEMENTED_SOURCE_EXTERNAL_BINARY_GATED
empty repoPath compatibility path = EXPLICIT_STUB_SIGNAL
runtime with real git-lfs = TOKEN_VAZIO_RUNTIME
```

### Worktree

`WorktreeManager` implementa `git worktree add/list/remove/prune/lock/unlock`, mas preserva `NotImplementedError` em operações cujo `repoPath` default não foi fornecido.

Classificação correta:

```text
real command path = IMPLEMENTED_SOURCE
empty repoPath compatibility path = EXPLICIT_STUB_SIGNAL
filesystem/device matrix = TOKEN_VAZIO_RUNTIME
```

## 4. Blockers que permanecem reais candidatos de investigação

O scanner também encontrou muitos `TOKEN_VAZIO_SOURCE` e sinais em áreas como:

- compliance/privacy;
- `TermuxHealthProbe` e `ToolRouter`;
- `rafgitfs/assurance`;
- `rafgitfs/data/model/policy`;
- `rafgitfs/sync/write`;
- UI/view-models de RafGitFs.

Esses itens **não são promovidos nem descartados por regex**. Precisam de triagem semântica por símbolo/call-site/teste:

```text
arquivo
→ símbolo
→ caller
→ caminho alcançável
→ teste
→ execução
→ documentação
→ estado
```

Até essa triagem, o estado correto é `TOKEN_VAZIO_SEMANTIC_TRIAGE`.

## 5. Consequência para a documentação

As palavras `implemented`, `partial`, `stub` e `runtime-gated` devem ser atribuídas à **capacidade/caminho**, não à mera ocorrência de uma palavra no arquivo.

Por isso:

- não ocultar o gate vermelho;
- não afirmar que 82 features estão quebradas;
- não afirmar que os 82 blockers são irrelevantes;
- registrar a contagem como sinal lexical reproduzível;
- fazer o Repository View apontar caminhos e subdiretórios;
- fazer a reconciliação código→docs fechar o significado com evidência adicional.

## 6. Integração com Repository View

O `Repository View V1` cobre a dimensão que o Source Gap Audit não cobre:

```text
Source Gap Audit = conteúdo lexical de fonte compilada
Repository View  = topologia completa de caminhos rastreados + navegabilidade
CODE_TO_DOC_MAP  = relação semântica curada
BUILD/TEST/CI    = execução observada
```

Essas quatro camadas devem ser usadas em conjunto.

## 7. F_ok / F_gap / F_next

**F_ok**
- baseline `main` comparado com PR #352;
- fingerprint lexical idêntico;
- regressão do PR descartada nesse eixo;
- três falsos positivos semânticos importantes explicados.

**F_gap**
- 82 blockers e 34 warnings ainda exigem classificação por símbolo/call-site;
- `TOKEN_VAZIO_SOURCE` não foi convertido artificialmente em PASS;
- runtime real de Bisect/LFS/Worktree permanece aberto.

**F_next**
- cruzar saída do Source Gap Audit com `repository-map/INDEX.yml`;
- gerar ledger `path → symbol → caller → docs → test → state`;
- corrigir primeiro divergências P0 entre documentação e caminhos realmente alcançáveis.
