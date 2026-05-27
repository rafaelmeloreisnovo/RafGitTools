# RAFGITTOOLS_SECURITY_AUTH_MAP

- Status: ATIVO
- Última atualização: 2026-05-27

## Mapa de autenticação e segurança

| Método | Entrada UI | Persistência | Cache memória | Risco atual | Maturidade |
|---|---|---|---|---|---|
| PAT | AuthScreen > PatLoginForm | AuthRepository.savePat (SecurityManager) | AuthTokenCache.token | vazamento em logs/acidentes de UX | ATIVO |
| OAUTH_DEVICE | AuthScreen > startDeviceCodeLogin | token final via authenticateWithPat | AuthTokenCache.token | polling/retry e UX de timeout | ATIVO |
| GH_CLI_IMPORT | AuthScreen > importGhCliToken | token importado passa por authenticateWithPat | AuthTokenCache.token | dependência de gh instalado | ATIVO |
| OFFLINE | AuthScreen > continueOffline | AuthRepository.setOfflineMode(true) | token nulo | acesso a features indevidas online | ATIVO |
| SSH | AuthScreen > authenticateWithSshKey | AuthMethod.SSH_KEY + offline local + validação de chave | token nulo | sem chave cadastrada impede login | REAL_ATIVO_LOCAL |
| OAUTH_WEB | AuthScreen > startOAuthWebLogin | Device flow com método OAUTH_WEB persistido | AuthTokenCache.token | depende de GITHUB_CLIENT_ID | REAL_ATIVO |

## Controles ativos

1. Token armazenado criptografado por `SecurityManager`.
2. Cache em memória isolado por `AuthTokenCache`.
3. Logout limpa estado persistido e memória (`clearAuthState` + cache).
4. Modo offline não injeta token e permite fluxo local da Home.

## Lacunas prioritárias

- Testes de interceptação sem token/com token.
- Política de mascaramento de erro para evitar leak de credenciais.
- Implementar SSH real (agent/chave) e OAuth Web completo.
