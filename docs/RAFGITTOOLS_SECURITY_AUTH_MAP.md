# RAFGITTOOLS_SECURITY_AUTH_MAP

- Status: **ATIVO / IMPLEMENTED_ADVANCED**
- Atualização observada: **2026-08-12**
- Regra: fonte integrada != runtime comprovado; `TOKEN_VAZIO` permanece para execução não observada.

## Mapa de autenticação e segurança

| Método | Persistência | Estado / limite |
|---|---|---|
| PAT | access token cifrado via Android Keystore/AES-GCM | IMPLEMENTED; não possui refresh-token no contrato atual |
| OAuth App Device Flow | access token cifrado | IMPLEMENTED / `GITHUB_CLIENT_ID` CONFIG_REQUIRED; sem refresh-token quando o provedor não o retorna |
| GitHub App Device Flow com expiring user tokens | access + refresh token cifrados separadamente + expiries | IMPLEMENTED / RUNTIME_GATED; refresh rotativo sem client secret |
| GH_CLI_IMPORT | token importado passa pela mesma validação remota antes de persistir | IMPLEMENTED; `gh` dentro do processo Android = TOKEN_VAZIO_RUNTIME |
| OFFLINE | flag local; token nulo | IMPLEMENTED; não é sessão GitHub |
| SSH_KEY | método + chave local | PARTIAL/RUNTIME_GATED; transporte existe, matriz servidor/chave real pendente |

## P33-25 — ciclo de vida capability-aware

O código não presume que toda credencial pode ser renovada. A decisão é baseada no artefato persistido:

```text
PAT / OAuth App sem refresh_token
  401 -> tentativa de refresh falha por ausência da capacidade
      -> clearAuthState()
      -> AuthTokenCache = null
      -> reautenticação necessária

GitHub App Device Flow + refresh_token
  401 -> recoveryMutex
      -> refreshMutex
      -> se outro request já rotacionou o access token: reutiliza o novo token
      -> senão POST /login/oauth/access_token
           client_id
           grant_type=refresh_token
           refresh_token
           NÃO envia client_secret
      -> salva access + novo refresh token/expiries atomicamente
      -> atualiza AuthTokenCache
      -> fecha resposta 401 original
      -> repete a requisição exatamente uma vez
      -> segundo 401: invalidateSession(), sem segundo refresh

403 + X-RateLimit-Remaining=0 -> RateLimited; credencial preservada
outro 403                     -> Forbidden; credencial preservada
```

### Invariantes da rotação

- refresh token possui alias Keystore próprio (`github_refresh_token`);
- `savePat()` remove qualquer refresh token/expiry de sessão anterior;
- `clearAuthState()` remove access + refresh + expiries;
- refresh token expirado falha antes da chamada de rede;
- refresh tokens rotativos são consumidos sob `Mutex`;
- refresh + decisão + eventual invalidação usam `recoveryMutex`, evitando um `clearAuthState()` atrasado apagar sessão recém-rotacionada;
- dois 401 concorrentes para o mesmo access token não consomem o mesmo refresh token duas vezes;
- resposta de refresh sem novo `refresh_token` limpa a capacidade futura em vez de reutilizar token rotativo antigo;
- endpoint OAuth usa cliente Retrofit/OkHttp separado do cliente GitHub API autenticado;
- nenhum `client_secret` é aceito, armazenado ou enviado pelo fluxo Android;
- retry HTTP é limitado a uma tentativa.

O HTTP 401 não é rotulado artificialmente como “expired” ou “revoked”; ele prova apenas que a credencial anexada foi rejeitada. A capacidade de refresh é determinada pela presença válida do refresh token persistido.

## GitHub privado por HTTPS

```text
UI GitHub HTTPS:
username = login GitHub (fallback: x-access-token)
password = PAT/OAuth access token

JGit Credentials.Token:
username = x-access-token
password = token
```

O token nunca deve entrar no remote URL ou ser usado como username. O mapeamento vale para clone normal/shallow/single-branch/submodules, pull, pull-rebase, fetch, push e force-with-lease.

## Force-with-lease

- `lsRemote` autenticado faz o preflight;
- `RefLeaseSpec(refs/heads/<branch>, expectedOldObjectId)` protege o destination ref;
- OID esperado = 40 hex;
- stale/invalid lease nunca cai para force-push incondicional;
- status remoto diferente de `OK`/`UP_TO_DATE` = falha.

## Controles ativos

1. Access/refresh tokens cifrados via Android Keystore com aliases separados.
2. PAT/importado é validado contra `/user` antes da sessão.
3. `AuthTokenCache` contém a cópia em memória usada no interceptor.
4. `AuthInterceptor` executa lifecycle 401/403 e retry único.
5. `TokenRefreshManager` serializa recuperação e invalidação.
6. `OAuthDeviceFlowManager` serializa consumo de refresh token rotativo e coalesce 401 concorrente.
7. OAuth transport é separado do Retrofit autenticado da API.
8. Logout/clear remove access, refresh e expiries.
9. Logging HTTP redige `Authorization`/`Proxy-Authorization`.
10. `GhCliAuthImporter` usa hostname explícito, exit code fail-closed e não converte stderr em token.
11. `scripts/rafgittools_private_auth_check.sh` verifica acesso privado sem expor token.
12. `scripts/validate_runtime_truth.py` rejeita regressão de client-secret, stub de refresh, ausência de locks, retry infinito e token-as-username.

## Lacunas reais restantes

| Gap | Estado | Fecha com |
|---|---|---|
| build/test Android do head | TOKEN_VAZIO / BLOCKED_INFRA_BILLING | readiness gate local ou resolver billing/spending do Actions |
| PAT/OAuth lifecycle em aparelho | TOKEN_VAZIO_RUNTIME | credencial descartável/revogada + receipt |
| GitHub App refresh real | CONFIG_REQUIRED / TOKEN_VAZIO_RUNTIME | Client ID de GitHub App com expiring user tokens + Device Flow + rotação observada |
| clone/pull/push/fetch privado | TOKEN_VAZIO_RUNTIME | fixture privada descartável + transcript |
| force-with-lease privado | TOKEN_VAZIO_RUNTIME | fixture positiva/stale + receipt |
| `gh` dentro do processo Android | TOKEN_VAZIO_RUNTIME | smoke explícito; PAT continua fallback |
| SSH remoto | TOKEN_VAZIO_RUNTIME | chave/host/clone/push reais |

Nenhuma ausência de execução é promovida a PASS por documentação.
