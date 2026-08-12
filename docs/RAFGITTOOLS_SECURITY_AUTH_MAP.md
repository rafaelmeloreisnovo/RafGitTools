# RAFGITTOOLS_SECURITY_AUTH_MAP

- Status: **ATIVO / IMPLEMENTED_ADVANCED**
- Atualização observada: **2026-08-12**
- Regra: fonte integrada != runtime comprovado; `TOKEN_VAZIO` permanece para execução não observada.

## Mapa de autenticação e segurança

| Método | Entrada UI | Persistência | Cache memória | Estado / limite |
|---|---|---|---|---|
| PAT | AuthScreen > PatLoginForm | `AuthRepository.savePat` + Android Keystore/AES-GCM | `AuthTokenCache.token` | IMPLEMENTED; `/user` é validado antes da persistência |
| OAUTH_DEVICE | AuthScreen > `startDeviceCodeLogin` | token autorizado protegido em `AuthRepository`; método persistido separadamente | `AuthTokenCache.token` | IMPLEMENTED / `GITHUB_CLIENT_ID` CONFIG_REQUIRED |
| GH_CLI_IMPORT | AuthScreen > `importGhCliToken` | token importado passa pela mesma validação remota antes de ser salvo | `AuthTokenCache.token` | IMPLEMENTED; disponibilidade de `gh` dentro do processo Android = TOKEN_VAZIO_RUNTIME |
| OFFLINE | AuthScreen > `continueOffline` | `AuthRepository.setOfflineMode(true)` | token nulo | IMPLEMENTED; não é sessão GitHub |
| SSH_KEY | AuthScreen > `authenticateWithSshKey` | método + chave local | token nulo | PARTIAL/LOCAL; transporte JGit existe, matriz real de chaves/servidores ainda requer device regression |
| OAUTH_WEB | AuthScreen > `startOAuthWebLogin` | usa o Device Flow existente com método distinto | `AuthTokenCache.token` | IMPLEMENTED / CONFIG_REQUIRED; não implica client secret no APK |

## GitHub privado por HTTPS

O caminho de UI para GitHub privado deve construir credenciais Git como:

```text
username = login GitHub (fallback não secreto: x-access-token)
password = PAT/OAuth token
```

O token **não** deve entrar no remote URL nem no campo de username.

Rotas cobertas pela correção dedicada:

- clone (`AddRepositoryViewModel`);
- pull/push/fetch (`RepositoryDetailViewModel`);
- testes de regressão de construção da credencial.

A execução física autenticada continua sendo evidência separada e só pode sair de `TOKEN_VAZIO` com transcript/receipt real.

## Controles ativos

1. Token persistido criptografado via `SecurityManager`/Android Keystore.
2. Token validado contra o GitHub antes da criação da sessão PAT/gh-import.
3. Cache em memória isolado por `AuthTokenCache`.
4. Logout limpa estado persistido e cache de autenticação.
5. Interceptor HTTP redige `Authorization`/`Proxy-Authorization` em logs.
6. `GhCliAuthImporter` usa hostname explícito, exit code fail-closed e não converte stderr em token.
7. `scripts/rafgittools_private_auth_check.sh` prova acesso ao repositório privado sem imprimir token.

## Lacunas reais restantes

| Gap | Estado | Fecha com |
|---|---|---|
| build/test Android do head revisado | TOKEN_VAZIO / BLOCKED_INFRA remoto | readiness gate local com JDK17 + SDK |
| clone/pull/push/fetch privado em aparelho | TOKEN_VAZIO | repo descartável/privado + transcript + resultado |
| OAuth Device Flow configurado | CONFIG_REQUIRED | Client ID público real + fluxo autorizado |
| `gh` executável dentro do processo Android | TOKEN_VAZIO_RUNTIME | smoke explícito; PAT permanece fallback primário |
| SSH remoto | TOKEN_VAZIO_RUNTIME | matriz de chave/host/clone/push em aparelho |
| force-with-lease privado | GAP_TRACKED | corrigir destination ref + credentialed `lsRemote` com testes dedicados |

Nenhuma dessas ausências deve ser promovida a PASS por documentação.
