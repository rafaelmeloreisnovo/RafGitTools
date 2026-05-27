# RAFGITTOOLS_CURRENT_STATE

- Status: ATIVO (auditoria técnica em andamento)
- Última atualização: 2026-05-27
- Escopo: auth/home/docs + validação de build/teste neste ambiente.

## Atualizações desta execução

1. `AuthViewModelTest` alinhado ao construtor atual de `AuthViewModel` com mocks de `OAuthDeviceFlowManager` e `GhCliAuthImporter`.
2. Teste trocado de `AuthUiState.SuccessOffline` para `AuthUiState.Offline`.
3. Teste trocado de `importFromGhCli()` para `importGhCliToken()`.
4. `AuthScreen` agora renderiza UI real de `AuthUiState.DeviceCodePending` (exibe `userCode` e `verificationUri`) e status de polling.
5. `HomeViewModel` agora respeita `authRepository.isOfflineMode()` e libera fluxo local/offline sem depender de autenticação online.
6. Documentação atualizada marcando SSH e OAUTH_WEB como PARCIAL/ROADMAP.
7. Matrizes documentais expandidas e preenchidas (`CODE_REALITY`, `UI_NAVIGATION`, `GIT_OPERATIONS`, `SECURITY_AUTH`, `ROADMAP_TRUE`).

## Resultado de validação (solicitado)

- `./scripts/gradlew_with_java17.sh testDevDebugUnitTest` => **FALHOU** por ausência de SDK Android (`local.properties`/`ANDROID_HOME`).
- `./scripts/gradlew_with_java17.sh assembleDevDebug` => **FALHOU** pelo mesmo motivo de SDK não configurado.

## Risco aberto

Sem SDK Android configurado no ambiente atual, não há comprovação de compilação/testes locais para esta rodada.
