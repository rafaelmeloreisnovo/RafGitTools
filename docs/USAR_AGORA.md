# USAR AGORA (DEV/INTERNO)

## Objetivo
Guia curto para compilar e validar o RafGitTools como APK interno (sem Play Store).

## Pré-requisitos
- JDK 17
- Android SDK (API 34 + Build Tools 34.0.0)
- Linux/macOS/WSL com `bash`

## Passo a passo
```bash
./scripts/prepare_local_properties.sh
./scripts/gradlew_with_java17.sh --version
./scripts/gradlew_with_java17.sh assembleDevDebug
./scripts/gradlew_with_java17.sh testDevDebugUnitTest
```

Para instalar em dispositivo conectado por ADB:
```bash
./scripts/gradlew_with_java17.sh installDevDebug
```

## Release interno sem assinatura de loja
```bash
ALLOW_UNSIGNED_RELEASE=true ./scripts/gradlew_with_java17.sh assembleProductionRelease
```

## Onde ficam os APKs
- Debug dev: `app/build/outputs/apk/dev/debug/`
- Release production: `app/build/outputs/apk/production/release/`

## Escopo de uso recomendado
- **Pode usar agora:** validação interna/dev, smoke tests funcionais, testes estruturais.
- **Antes de release pública:** assinatura oficial, CI verde recente, validação de OAuth/token e revisão de recursos ainda em stub.
