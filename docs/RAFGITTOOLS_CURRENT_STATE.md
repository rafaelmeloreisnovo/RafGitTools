# RAFGITTOOLS_CURRENT_STATE

- Status: ATIVO (auditoria técnica em andamento)
- Última atualização: 2026-05-27
- Escopo: auth/home/docs + validação de build/teste neste ambiente.

## Atualizações desta execução

1. `AuthViewModelTest` atualizado para novo construtor com `SshKeyManager` (além de `OAuthDeviceFlowManager` e `GhCliAuthImporter`).
2. Implementação real de `OAUTH_WEB`: novo `startOAuthWebLogin()` usando fluxo OAuth Device existente, com persistência de método `AuthMethod.OAUTH_WEB` quando autorizado.
3. Implementação real de `SSH` local: `authenticateWithSshKey()` valida presença de chaves via `SshKeyManager`; se houver chave, ativa modo offline/local e persiste `AuthMethod.SSH_KEY`.
4. `AuthScreen` ganhou botão de `OAuth Web (browser)` e ação para abrir `verificationUri` no navegador durante `DeviceCodePending`.
5. `HomeViewModel` permanece com suporte a modo offline via `authRepository.isOfflineMode()`.
6. Matrizes de documentação ajustadas para refletir SSH/OAUTH_WEB em estado real atual.

## Resultado de validação (solicitado)

- `./scripts/gradlew_with_java17.sh testDevDebugUnitTest` => **FALHOU** por ausência de SDK Android (`local.properties`/`ANDROID_HOME`).
- `./scripts/gradlew_with_java17.sh assembleDevDebug` => **FALHOU** pelo mesmo motivo de SDK não configurado.

## Risco aberto

Sem SDK Android configurado no ambiente atual, não há comprovação de compilação/testes locais para esta rodada.
