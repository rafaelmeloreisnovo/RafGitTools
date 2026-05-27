# RAFGITTOOLS_SECURITY_AUTH_MAP

- Status: ATIVO
- Última atualização: 2026-05-27

## Fluxo oficial de token

1. Entrada: `AuthViewModel.authenticateWithPat` / `startDeviceCodeLogin`.
2. Persistência: `AuthRepository.savePat` (criptografia via `SecurityManager`).
3. Cache em memória: `AuthTokenCache`.
4. Injeção HTTP: `data.auth.AuthInterceptor` adiciona `Authorization` quando token presente.
5. Limpeza: `AuthRepository.clearAuthState` + limpeza de cache no `AuthViewModel.logout`.

## Duplicidade/dead code

- `data/network/AuthInterceptor.kt` (deprecated) removido nesta iteração para evitar caminho paralelo e confusão.

## Riscos

- Garantir testes para “sem token não injeta header” e “com token injeta corretamente”.
- Garantir que logs não exponham Authorization/token.
