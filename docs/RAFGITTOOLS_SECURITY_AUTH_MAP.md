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

## Ciclo de vida da credencial

O contrato real de P33-25 é **invalidação + reautenticação**, não um refresh-token fictício:

```text
GitHub 2xx/3xx               -> credencial permanece
GitHub 401 com token enviado -> cache em memória zerado imediatamente
                               + AuthRepository.clearAuthState() no mesmo ciclo
                               + próxima ação = reautenticar
GitHub 403 + X-RateLimit-Remaining=0
                             -> RateLimited(resetEpoch?); credencial preservada
GitHub 403 sem prova de rate limit
                             -> Forbidden; credencial preservada
```

A fonte compilada não aceita `clientSecret` nem expõe `refreshOAuthToken(...)`. O fluxo OAuth App atual recebe um access token pelo Device Flow, mas não modela `refresh_token`; portanto não existe base de evidência para afirmar refresh automático.

O HTTP 401 também não é rebatizado artificialmente como “expired” ou “revoked”: ele prova que a credencial anexada foi rejeitada, e isso é suficiente para fail-closed.

## GitHub privado por HTTPS

O caminho de UI para GitHub privado constrói credenciais Git como:

```text
username = login GitHub (fallback não secreto: x-access-token)
password = PAT/OAuth token
```

A camada JGit também normaliza `Credentials.Token` para:

```text
username = x-access-token
password = token
```

Assim chamadas diretas/avançadas — clone normal, shallow, single-branch, submodules, pull, pull-rebase, fetch, push e force-with-lease — não recaem no contrato antigo `username=token/password=vazio`.

O token **não** deve entrar no remote URL nem no campo de username.

A execução física autenticada continua sendo evidência separada e só pode sair de `TOKEN_VAZIO` com transcript/receipt real.

## Force-with-lease

O caminho de fonte implementa dois gates complementares:

1. `lsRemote` autenticado lê o OID remoto esperado e falha cedo quando o lease já está stale;
2. `RefLeaseSpec(refs/heads/<branch>, expectedOldObjectId)` permanece no push como gate atômico, cobrindo mudança remota entre preflight e mutation.

Invariantes:

- o lease protege o **destination ref**, nunca o refspec `src:dst` completo;
- HTTPS e SSH são aplicados também ao `lsRemote`;
- OID esperado deve ter 40 dígitos hexadecimais;
- stale/invalid lease nunca cai para force-push incondicional;
- status diferente de `OK`/`UP_TO_DATE` é falha.

Testes locais de JGit cobrem lease correspondente, stale sem mutação e OID inválido. A execução contra GitHub privado real permanece `TOKEN_VAZIO_RUNTIME` até fixture autenticada.

## Controles ativos

1. Token persistido criptografado via `SecurityManager`/Android Keystore.
2. Token validado contra o GitHub antes da criação da sessão PAT/gh-import.
3. Cache em memória isolado por `AuthTokenCache`.
4. `AuthInterceptor` injeta `TokenRefreshManager`; 401 invalida cache em memória no mesmo request.
5. `TokenRefreshManager` tenta limpar persistência em 401 e preserva credencial em 403 rate-limit/forbidden.
6. Logout limpa estado persistido e cache de autenticação.
7. Interceptor HTTP de logging redige `Authorization`/`Proxy-Authorization`.
8. `GhCliAuthImporter` usa hostname explícito, exit code fail-closed e não converte stderr em token.
9. `scripts/rafgittools_private_auth_check.sh` prova acesso ao repositório privado sem imprimir token.
10. `scripts/validate_runtime_truth.py` rejeita regressões de token-as-username, destination-ref lease e refresh-token/client-secret stub.

## Lacunas reais restantes

| Gap | Estado | Fecha com |
|---|---|---|
| build/test Android do head revisado | TOKEN_VAZIO / BLOCKED_INFRA_BILLING remoto | readiness gate local com JDK17 + SDK ou resolver billing/spending do Actions |
| clone/pull/push/fetch privado em aparelho | TOKEN_VAZIO_RUNTIME | repo descartável/privado + transcript + resultado |
| force-with-lease contra GitHub privado real | TOKEN_VAZIO_RUNTIME | fixture privada descartável + lease positivo/stale + receipt |
| 401 lifecycle em aparelho | TOKEN_VAZIO_RUNTIME | token descartável/revogado + comprovar cache/persistência/reauth |
| OAuth Device Flow configurado | CONFIG_REQUIRED | Client ID público real + fluxo autorizado |
| `gh` executável dentro do processo Android | TOKEN_VAZIO_RUNTIME | smoke explícito; PAT permanece fallback primário |
| SSH remoto | TOKEN_VAZIO_RUNTIME | matriz de chave/host/clone/push em aparelho |

Nenhuma dessas ausências deve ser promovida a PASS por documentação.
