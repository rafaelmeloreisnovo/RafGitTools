# Mapa código → documentação

Este mapa conecta módulos de código aos documentos que descrevem comportamento, arquitetura e status.

> **Importante:** este arquivo é uma curadoria semântica manual. Ele não deve mais ser tratado como inventário completo da árvore. Para presença estrutural recursiva, subdiretórios, arquivos soltos e candidatos de dívida documental, use `docs/navigation/REPOSITORY_VIEW_USAGE.md` e gere `docs/repository-map/INDEX.yml` com `scripts/navigation/repository_view.py`.

## Git core (`data/git`, `domain/usecase/git`)

| Arquivo(s)-chave do código | Documento(s) relacionados | Status atual |
|---|---|---|
| `app/src/main/kotlin/com/rafgittools/data/git/JGitService.kt` | `docs/ARCHITECTURE.md`, `docs/STATUS_REPORT.md`, `docs/ROADMAP.md` | implementado |
| `app/src/main/kotlin/com/rafgittools/domain/usecase/git/CloneRepositoryUseCase.kt` | `docs/ARCHITECTURE.md`, `docs/FEATURE_MATRIX.md` | implementado |
| `app/src/main/kotlin/com/rafgittools/domain/usecase/git/GetRepositoryStatusUseCase.kt` | `docs/ARCHITECTURE.md`, `docs/STATUS_REPORT.md` | implementado |
| `app/src/main/kotlin/com/rafgittools/domain/usecase/git/AdvancedCloneUseCases.kt` | `docs/ROADMAP.md`, `docs/STATUS_REPORT.md` | parcial |
| `app/src/main/kotlin/com/rafgittools/domain/usecase/git/RebaseAndCherryPickUseCases.kt`, `app/src/main/kotlin/com/rafgittools/domain/usecase/git/StashUseCases.kt`, `app/src/main/kotlin/com/rafgittools/domain/usecase/git/TagUseCases.kt` | `docs/STATUS_REPORT.md`, `docs/FEATURE_MATRIX.md` | parcial |

## GitHub API (`data/github`)

| Arquivo(s)-chave do código | Documento(s) relacionados | Status atual |
|---|---|---|
| `app/src/main/kotlin/com/rafgittools/data/github/GithubApiService.kt` | `docs/ARCHITECTURE.md`, `docs/STATUS_REPORT.md`, `docs/ROADMAP.md` | implementado |
| `app/src/main/kotlin/com/rafgittools/data/github/GithubRepository.kt` | `docs/ARCHITECTURE.md`, `docs/FEATURE_MATRIX.md`, `docs/STATUS_REPORT.md` | parcial |

## Segurança (`core/security`, `security`)

| Arquivo(s)-chave do código | Documento(s) relacionados | Status atual |
|---|---|---|
| `app/src/main/kotlin/com/rafgittools/core/security/SecurityManager.kt`, `app/src/main/kotlin/com/rafgittools/core/security/SecureStorage.kt` | `docs/SECURITY.md`, `docs/PRIVACY.md`, `docs/SECURITY_DEPLOYMENT_GUIDE.md` | implementado |
| `app/src/main/kotlin/com/rafgittools/core/security/SshKeyManager.kt`, `app/src/main/kotlin/com/rafgittools/core/security/SshSessionFactory.kt` | `docs/ROADMAP.md`, `docs/STATUS_REPORT.md`, `docs/SECURITY.md` | parcial |
| `app/src/main/kotlin/com/rafgittools/security/GpgKeyManager.kt` | `docs/issues/feature-gpg-implementation.md`, `docs/ROADMAP.md`, `docs/STATUS_REPORT.md` | stub |

## UI (`ui/screens/*`)

| Arquivo(s)-chave do código | Documento(s) relacionados | Status atual |
|---|---|---|
| `app/src/main/kotlin/com/rafgittools/ui/screens/home/HomeScreen.kt`, `app/src/main/kotlin/com/rafgittools/ui/screens/repository/RepositoryListScreen.kt` | `docs/NAVIGATION_MAP.md`, `docs/ARCHITECTURE.md` | implementado |
| `app/src/main/kotlin/com/rafgittools/ui/screens/issues/*`, `app/src/main/kotlin/com/rafgittools/ui/screens/pullrequests/*`, `app/src/main/kotlin/com/rafgittools/ui/screens/releases/*` | `docs/NAVIGATION_MAP.md`, `docs/STATUS_REPORT.md`, `docs/FEATURE_MATRIX.md` | parcial |
| `app/src/main/kotlin/com/rafgittools/ui/screens/terminal/TerminalScreen.kt`, `app/src/main/kotlin/com/rafgittools/ui/screens/terminal/TerminalViewModel.kt` | `docs/ROADMAP.md`, `docs/STATUS_REPORT.md` | parcial |

## Camada recursiva de descoberta

A relação acima responde **o que significa** parte do código. O Repository View responde **o que existe fisicamente** no repositório.

Fluxo recomendado:

```text
árvore Git rastreada
  → repository_view.py
  → docs/repository-map/INDEX.yml
  → FILES_*.yml + DIRECTORIES_*.yml
  → candidatos source_without_local_index / root_loose_file_candidates
  → revisão humana
  → atualização deste CODE_TO_DOC_MAP e dos documentos de domínio
```

Regras:

- `INDEX.yml` é índice estrutural, não prova semântica.
- Arquivo cuja finalidade não possa ser classificada fica `semantic_role_state: TOKEN_VAZIO`.
- Diretório de código sem README/INDEX local é **candidato** de dívida documental, não erro automático.
- Arquivos na raiz fora dos anchors usuais são listados como candidatos a arquivo solto para revisão.
- O mapa é shardado para permanecer utilizável em árvores muito grandes.
- `scripts/federation/streaming_inventory.py` permanece responsável por identidade/bytes/SHA-256; `repository_view.py` cobre topologia e navegabilidade.

## Critério de status

- **implementado**: já existe código funcional no módulo e integração principal no app.
- **parcial**: funcionalidade disponível, mas ainda com lacunas registradas no roadmap/status.
- **stub**: arquivo apenas com placeholder/`NotImplementedError`.
- **TOKEN_VAZIO**: não há evidência suficiente para atribuir papel/estado com segurança.
