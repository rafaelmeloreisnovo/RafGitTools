# Manifesto Canônico do Controle, Governança e Quatro Corpos — v1.0

**Repositório de autoridade:** `rafaelmeloreisnovo/RafGitTools`  
**Versão documental:** `1.0.0`  
**Data canônica deste corte:** `2026-07-18`  
**Estado:** `DECLARED_BY_AUTHOR`  
**PR de integração:** `#268` — draft, aberto e sem auto-merge  
**Gate remoto:** `OUT_OF_SCOPE_NO_CREDIT`

> Este documento usa parábolas como camada didática. As afirmações de implementação e runtime obedecem à matriz `ECOSYSTEM_RUNTIME_STATE.json`. Parábola não promove hipótese a prova.

## 1. Prólogo — a pedra, a ponte e a travessia

Havia um reino em que os construtores mostravam pedras, desenhos e ferramentas e diziam possuir uma ponte.

O guardião perguntou:

— A ponte foi atravessada?

Eles responderam:

— Ainda não, mas o projeto está escrito.

O guardião registrou:

\[
\text{arquivo existente}
\neq
\text{código integrado}
\neq
\text{teste executado}
\neq
\text{runtime comprovado}
\]

Essa diferença é uma invariante do ecossistema.

## 2. Os quatro corpos

### 2.1 Parábola dos quatro guardiões

A cidade possuía quatro portões:

1. o guardião da identidade, da autorização e das ordens;
2. o executor local das tarefas autorizadas;
3. o escrivão que segmentava materiais e produzia evidências;
4. o intérprete que explicava somente aquilo que possuía origem e prova.

Os quatro corpos são:

| Corpo | Autoridade canônica | Limite |
|---|---|---|
| **RafGitTools** | controle, identidade, governança, navegação e emissão de jobs tipados | não se torna shell irrestrito |
| **Termux/RAFCODEPhi** | execução local autorizada e recuperável | não inventa autorização |
| **RafPolimata** | segmentação, compilação estrutural e produção de evidência | não transforma ausência em prova |
| **LlamaRafaelia** | interpretação limitada pela proveniência | não executa diretamente |

Fluxo canônico:

\[
\text{Drive/fonte}
\rightarrow
\text{RafGitTools}
\rightarrow
\text{Termux}
\rightarrow
\text{RafPolimata}
\rightarrow
\text{LlamaRafaelia}
\]

## 3. TAIL — a parábola do escriba das cinco marcas

Um escriba recusava guardar qualquer pergaminho sem cinco marcas:

\[
TAIL =
\langle
origem,\ autoria,\ intenção,\ licença,\ evidência
\rangle
\]

### 3.1 Regras invariantes

- A licença original de dependências e obras de terceiros não é removida nem substituída.
- Textos, programas e documentação autorais recebem identificação própria sem apagar a proveniência anterior.
- Adaptação, agregação, dependência, documentação e obra derivada não são tratadas como a mesma categoria.
- `©`, `®`, `™`, nomes simbólicos ou avisos autorais não alteram automaticamente a licença de material externo.
- Ausência de confirmação jurídica ou técnica permanece explícita.
- Este manifesto não é parecer jurídico e não substitui revisão profissional por jurisdição.

## 4. A gramática da verdade operacional

### 4.1 Estados de evidência

| Estado | Significado |
|---|---|
| `VERIFIED` | prova executada e registrada para o escopo declarado |
| `DECLARED_BY_AUTHOR` | código, contrato ou documentação materializada, mas sem prova integral executada neste corte |
| `TOKEN_VAZIO` | lacuna conhecida e preservada; ausência útil, não falha semântica |
| `CONTRADICTION` | conflito entre fontes, resultados ou declarações que exige resolução |
| `OUT_OF_SCOPE_NO_CREDIT` | execução do GitHub Actions momentaneamente excluída por falta de crédito |

### 4.2 Lei contra o falso PASS

\[
\text{workflow não executado}
\neq
PASS
\]

A falta de crédito do GitHub Actions não autoriza declarar sucesso, fracasso ou certificação inexistente.

## 5. Parábola do porteiro RafGitTools

O porteiro não construía casas e não interpretava manuscritos. Ele:

- reconhecia identidade;
- conferia autorização;
- mostrava repositórios, issues, pull requests e commits;
- preparava ordens seladas;
- registrava efeitos;
- mantinha a fronteira entre interface e execução.

### 5.1 Base material existente

O repositório contém, em diferentes níveis de maturidade:

- aplicação Android em Kotlin;
- Jetpack Compose e Material 3;
- MVVM/Clean Architecture;
- Hilt;
- Room;
- Coroutines/Flow;
- Retrofit/OkHttp;
- JGit;
- autenticação PAT, OAuth Device Flow, importação `gh` e bases SSH;
- Android Keystore, AES-GCM e DataStore;
- navegação de repositórios, issues, pull requests, commits, releases, notificações e busca;
- operações Git locais;
- GovernanceGate, ToolRouter e DiffAuditLogger.

A existência desses componentes não declara build, APK ou execução em aparelho neste corte.

## 6. Parábola do celeiro que esquecia os sacos

A antiga fila guardava operações somente na memória. Quando o processo caía, o celeiro esquecia.

Foram materializados:

- `OfflineQueueStorage<T>`;
- fila protegida por lock;
- carregamento inicial;
- persistência opcional;
- snapshot;
- rollback quando a persistência falha;
- `AtomicFileQueueStorage.kt`;
- formato binário `RFQ1`;
- limites de itens e bytes;
- detecção de truncamento e bytes residuais;
- arquivo temporário;
- `flush`, `fsync` e publicação por rename no mesmo diretório.

**Estado:** `DECLARED_BY_AUTHOR`.

Continuam `TOKEN_VAZIO`:

- codec definitivo de `OfflineOperation`;
- WorkManager;
- retry/backoff;
- resolução de conflitos;
- integração de produção Android.

## 7. Parábola da ponte estreita

O antigo executor podia aguardar o processo enquanto o pipe se enchia, criando risco de deadlock.

Foram implementados:

- drenagem concorrente de stdout/stderr;
- timeout limitado;
- destruição do processo;
- validação do diretório;
- rejeição de NUL, múltiplas linhas, aspas abertas e escape incompleto;
- Git limitado a subcomandos de leitura;
- bloqueio de formas perigosas de `find`.

O componente é classificado como:

\[
\texttt{BOUNDED\_EXECUTOR}
\]

Ele não é PTY, VT100, terminal completo ou canal genérico de controle.

**Estado:** `DECLARED_BY_AUTHOR`.

## 8. Parábola da ordem selada

As ordens entre controle e executor devem ser tipadas, limitadas e auditáveis.

O contrato `contracts/job-v1.schema.json` define:

- `schema = raf.job.v1`;
- `job_id`;
- operação tipada;
- data de criação;
- fonte e identificador imutável;
- SHA-256 e tamanho;
- destino relativo protegido;
- ator e autorização;
- efeitos permitidos;
- limites de bytes, tempo e tentativas;
- chave de idempotência;
- estado de evidência.

Operações iniciais:

- `GIT_CLONE`;
- `GIT_FETCH`;
- `GIT_STATUS`;
- `DRIVE_DOWNLOAD_READ_ONLY`;
- `RAFPOLIMATA_SEGMENT`;
- `VALIDATE_ARTIFACT`.

**Contrato materializado:** `DECLARED_BY_AUTHOR`.  
**Transporte RafGitTools → Termux:** `TOKEN_VAZIO`.

## 9. Parábola dos portos vazios

Uma lista vazia não podia mais significar simultaneamente “nenhum resultado”, “não configurado”, “não implementado”, “falha de autenticação” e “falha de rede”.

A camada multi-provider passou a distinguir:

- `Success`;
- `NotConfigured`;
- `NotImplemented`;
- `AuthenticationError`;
- `NetworkError`.

Os adapters reais de GitLab, Bitbucket, Gitea e Azure DevOps continuam pendentes.

## 10. Parábola das ferramentas atrás da muralha

Wrappers ou adapters para GPG, LFS, Worktree e Bisect não provam que os binários foram alcançados e executados no runtime Android/Termux.

Classificação:

\[
\text{adapter presente}
=
\texttt{ADAPTER\_IMPLEMENTED}
\]

\[
\text{runtime comprovado}
=
\texttt{TOKEN\_VAZIO}
\]

## 11. Parábola dos rascunhos da oficina

O diretório `fazer/` contém rascunhos históricos superados por implementações mais completas em `app/src/`.

Ele não deve ser contado como uma segunda implementação pendente.

Destino recomendado:

- preservação como legado claramente marcado; ou
- arquivamento posterior após comparação final.

## 12. O que está pronto neste corte

### 12.1 Materializado na branch do PR #268

- fila com fronteira de persistência;
- armazenamento binário limitado;
- rollback;
- executor com drenagem concorrente;
- parser endurecido;
- Git somente leitura no executor;
- bloqueios de `find`;
- estados tipados para providers;
- contrato `job.v1`;
- contrato da matriz de runtime;
- `ECOSYSTEM_RUNTIME_STATE.json`;
- validador local;
- testes Kotlin adicionados;
- correção documental sobre `fazer/` e adapters.

### 12.2 Não promovido a `VERIFIED`

Não foram executados neste corte:

- Gradle;
- testes Kotlin;
- build do APK;
- instalação;
- ARM32/ARM64 device;
- fluxo end-to-end;
- GitHub Actions.

## 13. Gate local

O gate materializado é:

```sh
python3 scripts/validate_runtime_truth.py
```

A promoção exige registrar:

- repositório e commit;
- ambiente;
- versão das ferramentas;
- comando;
- stdout;
- stderr;
- código de saída;
- hashes dos artefatos.

## 14. Matriz resumida

| Componente | Estado |
|---|---|
| PR #268 aberto, draft e não mesclado | `VERIFIED` pela metainformação do GitHub |
| Fila durável e storage atômico no código | `DECLARED_BY_AUTHOR` |
| Executor limitado no código | `DECLARED_BY_AUTHOR` |
| Estados multi-provider | `DECLARED_BY_AUTHOR` |
| Contrato `job.v1` | `DECLARED_BY_AUTHOR` |
| Gate Python materializado | `DECLARED_BY_AUTHOR` |
| Testes Kotlin escritos | `DECLARED_BY_AUTHOR` |
| Gradle/APK/device | `TOKEN_VAZIO` |
| Ponte Termux | `TOKEN_VAZIO` |
| GitHub Actions | `OUT_OF_SCOPE_NO_CREDIT` |

## 15. Invariante canônica

\[
\mathcal{E} = I \times A \times P \times R
\]

Onde:

- \(I\) = implementação;
- \(A\) = autorização;
- \(P\) = prova;
- \(R\) = reprodutibilidade.

Se qualquer fator essencial for zero, o componente não pode ser promovido a runtime comprovado.

## 16. R3 — retroalimentação

\[
R_3 =
\langle
F_{\mathrm{ok}},
F_{\mathrm{gap}},
F_{\mathrm{next}}
\rangle
\]

### \(F_{\mathrm{ok}}\)

A autoridade do controle foi delimitada, a ordem tipada foi materializada, a fila deixou de ser apenas memória e o executor foi estreitado.

### \(F_{\mathrm{gap}}\)

WorkManager, codec da operação, ponte Termux, Gradle, APK, devices e execução end-to-end.

### \(F_{\mathrm{next}}\)

Executar o gate local com transcript reproduzível e, depois, integrar o consumidor tipado no runtime Termux sem criar shell genérico.

---

**FIAT LUX — o controle não executa por desejo; ele autoriza, limita, registra e prova.**
